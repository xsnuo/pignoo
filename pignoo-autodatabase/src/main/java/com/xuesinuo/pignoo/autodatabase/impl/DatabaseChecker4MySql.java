package com.xuesinuo.pignoo.autodatabase.impl;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.sql.DataSource;

import com.xuesinuo.pignoo.autodatabase.DatabaseChecker;
import com.xuesinuo.pignoo.autodatabase.entity.DatabaseCheckResult;
import com.xuesinuo.pignoo.core.SqlExecuter;
import com.xuesinuo.pignoo.core.annotation.Column;
import com.xuesinuo.pignoo.core.annotation.PrimaryKey;
import com.xuesinuo.pignoo.core.annotation.Table;
import com.xuesinuo.pignoo.core.entity.EntityMapper;
import com.xuesinuo.pignoo.core.implement.SimpleJdbcSqlExecuter;

import lombok.Data;

public class DatabaseChecker4MySql implements DatabaseChecker {
    /**
     * SQL执行器
     * <p>
     * SQL Executer
     */
    protected static final SqlExecuter sqlExecuter = SimpleJdbcSqlExecuter.getInstance();

    private final DataSource dataSource;

    public DatabaseChecker4MySql(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Data
    @Table("PIGNOO_AUTO_DATABASE_CHECKER") // This table is not existed
    private static class MySqlColumnInfo {
        @Column("column_name")
        @PrimaryKey(auto = false)
        private String columnName;
        @Column("column_type")
        private String columnType;
        @Column("pk")
        private Boolean pk;
        @Column("auto")
        private Boolean auto;
    }

    @Override
    public DatabaseCheckResult check(EntityMapper<?> entityMapper) {
        DatabaseCheckResult result = new DatabaseCheckResult();
        Connection conn = null;
        Boolean autoCommit = null;
        try {
            conn = dataSource.getConnection();
            autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(true);
            final Connection c = conn;
            // 是否选中了数据库
            String database = sqlExecuter.selectColumn(() -> c, (x) -> {}, "SELECT DATABASE()", new HashMap<>(), String.class);
            if (database == null || database.isBlank()) {
                throw new RuntimeException("Connection must have a database selected.");
            }
            // 表是否存在
            String tableName = entityMapper.tableName();
            Integer hasTable = sqlExecuter.selectColumn(() -> c, (x) -> {},
                    "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='" + database + "' AND TABLE_NAME='" + tableName + "'",
                    new HashMap<>(), Integer.class);
            if (hasTable == null || hasTable == 0) {// 表不存在：创建表
                result.setState(DatabaseCheckResult.ResultState.ERROR);
                StringBuilder sql = new StringBuilder();
                String pkColumn = entityMapper.primaryKeyColumn();
                String pkType = javaType2MySqlType(entityMapper.primaryKeyField().getType());
                String pkAuto = entityMapper.autoPrimaryKey() ? "AUTO_INCREMENT" : "";
                sql.append("CREATE TABLE `" + tableName + "` ( ");
                sql.append("`" + pkColumn + "` " + pkType + " NOT NULL " + pkAuto + ", ");
                for (int i = 0; i < entityMapper.columns().size(); i++) {
                    if (entityMapper.columns().get(i).equals(entityMapper.primaryKeyColumn())) {
                        continue;
                    }
                    String column = entityMapper.columns().get(i);
                    String columnType = javaType2MySqlType(entityMapper.fields().get(i).getType());
                    sql.append("`" + column + "` " + columnType + " DEFAULT NULL, ");
                }
                sql.append("PRIMARY KEY (`" + pkColumn + "`) ");
                sql.append(") ");
                result.setAdvise2AddTable(sql.toString());
            } else {// 表存在：检查字段
                String sql = """
                        SELECT
                            c.COLUMN_NAME AS `column_name`,
                            c.DATA_TYPE AS `column_type`,
                            CASE
                                WHEN k.COLUMN_NAME IS NOT NULL THEN 1
                                ELSE 0
                            END AS `pk`,
                            CASE
                                WHEN c.EXTRA = 'auto_increment' THEN 1
                                ELSE 0
                            END AS `auto`
                        FROM
                            information_schema.COLUMNS c
                        LEFT JOIN
                            information_schema.KEY_COLUMN_USAGE k
                            ON c.TABLE_SCHEMA = k.TABLE_SCHEMA
                            AND c.TABLE_NAME = k.TABLE_NAME
                            AND c.COLUMN_NAME = k.COLUMN_NAME
                            AND k.CONSTRAINT_NAME = 'PRIMARY'
                        WHERE
                            c.TABLE_SCHEMA = '__database_name__'
                            AND c.TABLE_NAME = '__table_name__'
                        ORDER BY
                            c.ORDINAL_POSITION;
                        """;
                sql.replaceAll("__database_name__", database);
                sql.replaceAll("__table_name__", tableName);
                List<MySqlColumnInfo> columnInfosInDatabase = sqlExecuter.selectList(() -> c, (x) -> {}, sql, new HashMap<>(), MySqlColumnInfo.class);
                List<String> columnNamesInDatabase = columnInfosInDatabase.stream().map(x -> x.getColumnName()).toList();
                for (int i = 0; i < entityMapper.columns().size(); i++) {// 数据库中缺少字段：添加
                    String column = entityMapper.columns().get(i);
                    String columnType = javaType2MySqlType(entityMapper.fields().get(i).getType());
                    if (columnNamesInDatabase.contains(column)) {
                        continue;
                    }
                    if (entityMapper.primaryKeyColumn().equals(column)) {
                        result.getOtherMessage().add("Primary-Key '" + column + "' in + " + entityMapper.getType().getName() + ", to table: " + tableName);
                        continue;
                    }
                    sql = "ALTER TABLE `" + tableName + "` ADD COLUMN `" + column + "` " + columnType + " NULL ";
                    result.getAdvise2AddColumn().add(sql);
                }
                for (int i = 0; i < columnInfosInDatabase.size(); i++) {// 数据库中多余字段：删除
                    String column = columnNamesInDatabase.get(i);
                    if (entityMapper.columns().contains(columnNamesInDatabase.get(i))) {
                        continue;
                    }
                    if (columnInfosInDatabase.get(i).getPk()) {
                        result.getOtherMessage().add("Primary-Key '" + column + "' not found in + " + entityMapper.getType().getName() + ", from table: " + tableName);
                        continue;
                    }
                    sql = "ALTER TABLE `" + tableName + "` DROP COLUMN `" + column + "` ";
                    result.getAdvise2RemoveColumn().add(sql);
                }
                for (int i = 0; i < entityMapper.columns().size(); i++) {// 数据库中字段类型不匹配：修改
                    String column = entityMapper.columns().get(i);
                    if (!columnNamesInDatabase.contains(column)) {
                        continue;
                    }
                    MySqlColumnInfo ciid = columnInfosInDatabase.stream().filter(cid -> cid.getColumnName().equals(column)).findFirst().get();
                    if (entityMapper.primaryKeyColumn().equals(column) || ciid.getPk()) {
                        result.getOtherMessage().add("Primary-Key type mapping error with + " + entityMapper.getType().getName() + " and table: " + tableName);
                    }
                    // TODO
                }
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    if (autoCommit != null) {
                        conn.setAutoCommit(autoCommit);
                    }
                    conn.close();
                } catch (SQLException e) {
                    // TODO Auto-generated catch block
                }
            }
        }
        return result;
    }

    public String javaType2MySqlType(Class<?> javaType) {
        // 基本数据类型
        if (Long.class.isAssignableFrom(javaType) || long.class.equals(javaType)) {
            return "bigint";
        }
        if (Integer.class.isAssignableFrom(javaType) || int.class.equals(javaType)) {
            return "int";
        }
        if (Short.class.isAssignableFrom(javaType) || short.class.equals(javaType)) {
            return "smallint";
        }
        if (Byte.class.isAssignableFrom(javaType) || byte.class.equals(javaType)) {
            return "tinyint";
        }
        if (Double.class.isAssignableFrom(javaType) || double.class.equals(javaType)) {
            return "double";
        }
        if (Float.class.isAssignableFrom(javaType) || float.class.equals(javaType)) {
            return "float";
        }
        if (Boolean.class.isAssignableFrom(javaType) || boolean.class.equals(javaType)) {
            return "tinyint(1)";
        }
        if (Character.class.isAssignableFrom(javaType) || char.class.equals(javaType)) {
            return "char(1)";
        }
        // 字符串
        if (String.class.isAssignableFrom(javaType)) {
            return "varchar(128)";
        }
        // 日期时间
        if (java.util.Date.class.isAssignableFrom(javaType)) {
            if (java.sql.Timestamp.class.isAssignableFrom(javaType)) {
                return "datetime";
            }
            return "date";
        }
        if (Instant.class.isAssignableFrom(javaType)) {
            return "timestamp";
        }
        if (LocalDate.class.isAssignableFrom(javaType)) {
            return "date";
        }
        if (LocalTime.class.isAssignableFrom(javaType)) {
            return "time";
        }
        if (LocalDateTime.class.isAssignableFrom(javaType)) {
            return "datetime";
        }
        if (ZonedDateTime.class.isAssignableFrom(javaType) || OffsetDateTime.class.isAssignableFrom(javaType)) {
            return "timestamp";
        }
        if (OffsetTime.class.isAssignableFrom(javaType)) {
            return "time";
        }
        // 数字
        if (java.math.BigDecimal.class.isAssignableFrom(javaType)) {
            return "decimal(32,4)";
        }
        if (BigInteger.class.isAssignableFrom(javaType)) {
            return "decimal(64,0)";
        }
        // 二进制
        if (byte[].class.isAssignableFrom(javaType)) {
            return "blob";
        }
        return null;
    }

    public boolean javaAndMySqlTypeOk(Class<?> javaType, String mysqlType) {
        if (Long.class.isAssignableFrom(javaType) || long.class.equals(javaType)) {
            if (Arrays.asList("bigint").contains(mysqlType)) {
                return true;
            }
            return false;
        }
        // TODO
        return false;
    }
}
