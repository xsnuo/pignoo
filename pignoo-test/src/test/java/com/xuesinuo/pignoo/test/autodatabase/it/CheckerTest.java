package com.xuesinuo.pignoo.test.autodatabase.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.xuesinuo.pignoo.autodatabase.TypeMapper;
import com.xuesinuo.pignoo.autodatabase.entity.DatabaseCheckResult;
import com.xuesinuo.pignoo.autodatabase.impl.DatabaseChecker4MySql;
import com.xuesinuo.pignoo.core.PignooConfig;
import com.xuesinuo.pignoo.core.annotation.Column;
import com.xuesinuo.pignoo.core.annotation.Table;
import com.xuesinuo.pignoo.core.entity.EntityMapper;

import lombok.Data;

/**
 * {@link DatabaseChecker4MySql} 的检查结果用例：只产出建议 SQL，不执行任何 DDL
 * <p>
 * Cases for {@link DatabaseChecker4MySql}: it only produces advise SQL, never executes DDL
 * <p>
 * 用例自建 {@code ad_chk_} 前缀的临时表，每个方法结束后统一清理
 * <p>
 * Every case builds its own {@code ad_chk_} prefixed temp table, dropped after each method
 */
@SpringBootTest
public class CheckerTest {

    public final DataSource dataSource;

    public CheckerTest(@Autowired DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @AfterEach
    public void dropTempTables() throws SQLException {
        List<String> tables = new ArrayList<>();
        try (var con = dataSource.getConnection();
                var ps = con.prepareStatement("SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME LIKE 'ad_chk_%'");
                var rs = ps.executeQuery()) {
            while (rs.next()) {
                tables.add(rs.getString(1));
            }
        }
        for (String table : tables) {
            ddl("DROP TABLE IF EXISTS `" + table + "`");
        }
    }

    private void ddl(String... sqls) throws SQLException {
        try (var con = dataSource.getConnection()) {
            con.setAutoCommit(false);
            for (String sql : sqls) {
                con.createStatement().execute(sql);
            }
            con.commit();
        }
    }

    private DatabaseCheckResult check(Class<?> entity, boolean strictColumnType) {
        return check(entity, (javaType, scale, field) -> null, strictColumnType);
    }

    private DatabaseCheckResult check(Class<?> entity, TypeMapper typeMapper, boolean strictColumnType) {
        DatabaseChecker4MySql checker = new DatabaseChecker4MySql(dataSource, typeMapper, strictColumnType);
        return checker.check(EntityMapper.build(entity, new PignooConfig()));
    }

    @Data
    @Table("ad_chk_add_table")
    public static class AdChkAddTable {
        private Long id;
        private String name;
        private Integer qty;
    }

    @Data
    @Table("ad_chk_add_table_pk")
    public static class AdChkAddTablePk {
        @Column(primaryKey = Column.PrimaryKey.NON_AUTO)
        private String bizKey;
        private String name;
    }

    @Data
    @Table("ad_chk_ok")
    public static class AdChkOk {
        private Long id;
        private String name;
        private Integer qty;
    }

    @Data
    @Table("ad_chk_add_col")
    public static class AdChkAddCol {
        private Long id;
        private String name;
        private Integer qty;
    }

    @Data
    @Table("ad_chk_drop_col")
    public static class AdChkDropCol {
        private Long id;
        private String name;
    }

    @Data
    @Table("ad_chk_mod_col")
    public static class AdChkModCol {
        private Long id;
        private String name;
        private Integer qty;
    }

    @Data
    @Table("ad_chk_strict")
    public static class AdChkStrict {
        private Long id;
        private String name;
    }

    @Data
    @Table("ad_chk_pk_missing")
    public static class AdChkPkMissing {
        private Long id;
        private String name;
        private String code;
    }

    @Data
    @Table("ad_chk_pk_auto")
    public static class AdChkPkAuto {
        private Long id;
        private String name;
    }

    @Data
    @Table("ad_chk_pk_extra")
    public static class AdChkPkExtra {
        private Long id;
        private String name;
    }

    @Data
    @Table("ad_chk_pk_type")
    public static class AdChkPkType {
        private Long id;
        private String name;
    }

    @Data
    @Table("ad_chk_unmapped")
    public static class AdChkUnmapped {
        private Long id;
        private String name;
        private Object payload;
    }

    @Data
    @Table("ad_chk_unmapped_exist")
    public static class AdChkUnmappedExist {
        private Long id;
        private String name;
        private Object payload;
    }

    @Data
    @Table("ad_chk_mapper")
    public static class AdChkMapper {
        private Long id;
        /** 自定义 mapper 按字段名接管：char(20) */
        private String orderNumber;
        /** 自定义 mapper 按 scale 接管：varchar(64) */
        @Column(scale = 64)
        private String memo;
        /** 自定义 mapper 不接管，回退默认：varchar(255) */
        private String name;
        /** 自定义 mapper 不接管，回退默认：int */
        private Integer qty;
    }

    @Data
    @Table("ad_chk_mapper_alter")
    public static class AdChkMapperAlter {
        private Long id;
        /** 表里是 varchar(255)，自定义 mapper 接管为 char(20)：触发改列 */
        private String orderNumber;
        /** 表里是 varchar(255)，自定义 mapper 不接管回退默认 int：触发改列 */
        private Integer qty;
        /** 表里没有这一列，自定义 mapper 接管为 char(20)：触发加列 */
        private String serialNumber;
    }

    @Data
    @Table("ad_chk_mapper_blank")
    public static class AdChkMapperBlank {
        private Long id;
        private String name;
    }

    /**
     * 自定义类型映射器：只接管一部分 String 字段，其余一律返回 null 交回默认映射
     * <p>
     * Custom type mapper: it only takes over part of the String fields, everything else returns null and
     * falls back to the default mapper
     */
    private static final TypeMapper CUSTOM_MAPPER = (javaType, scale, field) -> {
        if (String.class.isAssignableFrom(javaType)) {
            if (field.getName().toLowerCase().endsWith("number") && scale == 0) {
                return "char(20)";
            }
            if (scale > 0 && scale <= 255) {
                return "varchar(" + scale + ")";
            }
        }
        return null;
    };

    @Test
    public void tableMissing() throws SQLException {
        ddl("DROP TABLE IF EXISTS `ad_chk_add_table`");

        DatabaseCheckResult result = check(AdChkAddTable.class, false);

        assertThat(result.getAdvise2AddTable()).as("表不存在时应产出且只产出一条建表语句").hasSize(1);
        String sql = result.getAdvise2AddTable().get(0);
        assertThat(sql).as("建表语句应针对实体对应的表").contains("CREATE TABLE `ad_chk_add_table`");
        assertThat(sql).as("建表语句应包含自增主键").contains("`id` bigint NOT NULL AUTO_INCREMENT");
        assertThat(sql).as("建表语句的非主键列应带 DEFAULT NULL").contains("`name` varchar(255) DEFAULT NULL");
        assertThat(sql).as("建表语句的非主键列应带 DEFAULT NULL").contains("`qty` int DEFAULT NULL");
        assertThat(sql).as("建表语句应声明主键").contains("PRIMARY KEY (`id`)");
        assertThat(result.getAdvise2AddColumn()).as("表不存在时不应产出加列建议").isEmpty();
        assertThat(result.getAdvise2UpdateColumn()).as("表不存在时不应产出改列建议").isEmpty();
        assertThat(result.getAdvise2RemoveColumn()).as("表不存在时不应产出删列建议").isEmpty();
        assertThat(result.getOtherMessage()).as("表不存在且类型均可映射时不应有其它问题").isEmpty();
    }

    @Test
    public void tableMissingWithNonAutoPrimaryKey() throws SQLException {
        ddl("DROP TABLE IF EXISTS `ad_chk_add_table_pk`");

        DatabaseCheckResult result = check(AdChkAddTablePk.class, false);

        assertThat(result.getAdvise2AddTable()).as("表不存在时应产出且只产出一条建表语句").hasSize(1);
        String sql = result.getAdvise2AddTable().get(0);
        assertThat(sql).as("非自增主键应建成 NOT NULL 且不带自增").contains("`biz_key` varchar(255) NOT NULL");
        assertThat(sql).as("非自增主键的建表语句不应出现 AUTO_INCREMENT").doesNotContain("AUTO_INCREMENT");
        assertThat(sql).as("建表语句应声明主键").contains("PRIMARY KEY (`biz_key`)");
        assertThat(result.getOtherMessage()).as("非自增主键建表不应有其它问题").isEmpty();
    }

    @Test
    public void tableMatched() throws SQLException {
        ddl("DROP TABLE IF EXISTS `ad_chk_ok`", """
                CREATE TABLE `ad_chk_ok` (
                  `id` bigint NOT NULL AUTO_INCREMENT,
                  `name` varchar(255) NULL,
                  `qty` int NULL,
                  PRIMARY KEY (`id`)
                )
                """);

        DatabaseCheckResult result = check(AdChkOk.class, false);

        assertThat(result.getAdvise2AddTable()).as("表结构完全一致时不应产出建表建议").isEmpty();
        assertThat(result.getAdvise2AddColumn()).as("表结构完全一致时不应产出加列建议").isEmpty();
        assertThat(result.getAdvise2UpdateColumn()).as("表结构完全一致时不应产出改列建议").isEmpty();
        assertThat(result.getAdvise2RemoveColumn()).as("表结构完全一致时不应产出删列建议").isEmpty();
        assertThat(result.getOtherMessage()).as("表结构完全一致时不应有其它问题").isEmpty();
    }

    @Test
    public void columnMissing() throws SQLException {
        ddl("DROP TABLE IF EXISTS `ad_chk_add_col`", """
                CREATE TABLE `ad_chk_add_col` (
                  `id` bigint NOT NULL AUTO_INCREMENT,
                  `name` varchar(255) NULL,
                  PRIMARY KEY (`id`)
                )
                """);

        DatabaseCheckResult result = check(AdChkAddCol.class, false);

        assertThat(result.getAdvise2AddColumn()).as("表中缺少 qty 列时应产出且只产出一条加列建议").hasSize(1);
        assertThat(result.getAdvise2AddColumn().get(0)).as("加列建议应为 ADD COLUMN 且类型正确")
                .contains("ALTER TABLE `ad_chk_add_col` ADD COLUMN `qty` int");
        assertThat(result.getAdvise2AddTable()).as("表已存在时不应产出建表建议").isEmpty();
        assertThat(result.getAdvise2UpdateColumn()).as("只缺列时不应产出改列建议").isEmpty();
        assertThat(result.getAdvise2RemoveColumn()).as("只缺列时不应产出删列建议").isEmpty();
        assertThat(result.getOtherMessage()).as("只缺列时不应有其它问题").isEmpty();
    }

    @Test
    public void columnRedundant() throws SQLException {
        ddl("DROP TABLE IF EXISTS `ad_chk_drop_col`", """
                CREATE TABLE `ad_chk_drop_col` (
                  `id` bigint NOT NULL AUTO_INCREMENT,
                  `name` varchar(255) NULL,
                  `legacy_col` varchar(255) NULL,
                  PRIMARY KEY (`id`)
                )
                """);

        DatabaseCheckResult result = check(AdChkDropCol.class, false);

        assertThat(result.getAdvise2RemoveColumn()).as("表中多出实体没有的列时应产出且只产出一条删列建议").hasSize(1);
        assertThat(result.getAdvise2RemoveColumn().get(0)).as("删列建议应为 DROP COLUMN")
                .contains("ALTER TABLE `ad_chk_drop_col` DROP COLUMN `legacy_col`");
        assertThat(result.getAdvise2AddTable()).as("表已存在时不应产出建表建议").isEmpty();
        assertThat(result.getAdvise2AddColumn()).as("只多列时不应产出加列建议").isEmpty();
        assertThat(result.getAdvise2UpdateColumn()).as("只多列时不应产出改列建议").isEmpty();
        assertThat(result.getOtherMessage()).as("只多列时不应有其它问题").isEmpty();
    }

    @Test
    public void columnTypeMismatch() throws SQLException {
        ddl("DROP TABLE IF EXISTS `ad_chk_mod_col`", """
                CREATE TABLE `ad_chk_mod_col` (
                  `id` bigint NOT NULL AUTO_INCREMENT,
                  `name` varchar(255) NULL,
                  `qty` varchar(255) NULL,
                  PRIMARY KEY (`id`)
                )
                """);

        DatabaseCheckResult result = check(AdChkModCol.class, false);

        assertThat(result.getAdvise2UpdateColumn()).as("Integer 对 varchar 属于不兼容类型，应产出且只产出一条改列建议").hasSize(1);
        assertThat(result.getAdvise2UpdateColumn().get(0)).as("改列建议应为 MODIFY COLUMN 且改为 int")
                .contains("ALTER TABLE `ad_chk_mod_col` MODIFY COLUMN `qty` int");
        assertThat(result.getAdvise2UpdateColumn().get(0)).as("原列可空，改列建议应保持可空").contains("`qty` int NULL");
        assertThat(result.getAdvise2AddTable()).as("表已存在时不应产出建表建议").isEmpty();
        assertThat(result.getAdvise2AddColumn()).as("只有类型不匹配时不应产出加列建议").isEmpty();
        assertThat(result.getAdvise2RemoveColumn()).as("只有类型不匹配时不应产出删列建议").isEmpty();
        assertThat(result.getOtherMessage()).as("非主键列类型不匹配可自动修正，不应有其它问题").isEmpty();
    }

    @Test
    public void strictColumnTypeVsLoose() throws SQLException {
        ddl("DROP TABLE IF EXISTS `ad_chk_strict`", """
                CREATE TABLE `ad_chk_strict` (
                  `id` bigint NOT NULL AUTO_INCREMENT,
                  `name` varchar(100) NULL,
                  PRIMARY KEY (`id`)
                )
                """);

        DatabaseCheckResult loose = check(AdChkStrict.class, false);
        assertThat(loose.getAdvise2UpdateColumn()).as("宽松模式下 String 对 varchar(100) 视为兼容，不应产出改列建议").isEmpty();
        assertThat(loose.getOtherMessage()).as("宽松模式下不应有其它问题").isEmpty();

        DatabaseCheckResult strict = check(AdChkStrict.class, true);
        assertThat(strict.getAdvise2UpdateColumn()).as("严格模式下 varchar(100) 与默认的 varchar(255) 不一致，应产出一条改列建议").hasSize(1);
        assertThat(strict.getAdvise2UpdateColumn().get(0)).as("严格模式应把 varchar(100) 改为 varchar(255)")
                .contains("ALTER TABLE `ad_chk_strict` MODIFY COLUMN `name` varchar(255)");
        assertThat(strict.getOtherMessage()).as("严格模式下 bigint 主键仍应完全一致，不应有其它问题").isEmpty();
    }

    @Test
    public void primaryKeyNotInTable() throws SQLException {
        ddl("DROP TABLE IF EXISTS `ad_chk_pk_missing`", """
                CREATE TABLE `ad_chk_pk_missing` (
                  `name` varchar(255) NULL,
                  `code` varchar(255) NULL
                )
                """);

        DatabaseCheckResult result = check(AdChkPkMissing.class, false);

        assertThat(result.getOtherMessage()).as("实体主键在表中不存在时应累积一条错误信息").hasSize(1);
        assertThat(result.getOtherMessage().get(0)).as("错误信息应指出主键不在表中")
                .contains("Primary-Key 'id'").contains("not in table: ad_chk_pk_missing");
        assertThat(result.getAdvise2AddColumn()).as("缺失的主键不能靠加列自动修复，不应产出加列建议").isEmpty();
    }

    @Test
    public void primaryKeyAutoMismatch() throws SQLException {
        ddl("DROP TABLE IF EXISTS `ad_chk_pk_auto`", """
                CREATE TABLE `ad_chk_pk_auto` (
                  `id` bigint NOT NULL,
                  `name` varchar(255) NULL,
                  PRIMARY KEY (`id`)
                )
                """);

        DatabaseCheckResult result = check(AdChkPkAuto.class, false);

        assertThat(result.getOtherMessage()).as("实体按自增主键映射、表却非自增时应累积一条错误信息").hasSize(1);
        assertThat(result.getOtherMessage().get(0)).as("错误信息应指出主键自增设置不一致")
                .contains("Primary-Key auto setting is error").contains("ad_chk_pk_auto");
        assertThat(result.getAdvise2UpdateColumn()).as("主键自增设置不一致不能靠改列自动修复，不应产出改列建议").isEmpty();
    }

    @Test
    public void primaryKeyRedundantInTable() throws SQLException {
        ddl("DROP TABLE IF EXISTS `ad_chk_pk_extra`", """
                CREATE TABLE `ad_chk_pk_extra` (
                  `id` bigint NOT NULL AUTO_INCREMENT,
                  `other_id` bigint NOT NULL,
                  `name` varchar(255) NULL,
                  PRIMARY KEY (`id`, `other_id`)
                )
                """);

        DatabaseCheckResult result = check(AdChkPkExtra.class, false);

        assertThat(result.getOtherMessage()).as("表中多出的列属于主键时应累积一条错误信息").hasSize(1);
        assertThat(result.getOtherMessage().get(0)).as("错误信息应指出该主键列不在实体中")
                .contains("Primary-Key 'other_id'").contains("in table: ad_chk_pk_extra");
        assertThat(result.getAdvise2RemoveColumn()).as("多出的主键列不应产出删列建议").isEmpty();
    }

    @Test
    public void primaryKeyTypeMismatch() throws SQLException {
        ddl("DROP TABLE IF EXISTS `ad_chk_pk_type`", """
                CREATE TABLE `ad_chk_pk_type` (
                  `id` varchar(64) NOT NULL,
                  `name` varchar(255) NULL,
                  PRIMARY KEY (`id`)
                )
                """);

        DatabaseCheckResult result = check(AdChkPkType.class, false);

        assertThat(result.getOtherMessage()).as("主键类型不匹配时应累积错误信息")
                .anySatisfy(msg -> assertThat(msg).as("错误信息应指出主键类型映射错误").contains("Primary-Key type mapping error"));
        assertThat(result.getAdvise2UpdateColumn()).as("主键类型不匹配不能靠改列自动修复，不应产出改列建议").isEmpty();
    }

    @Test
    public void unmappableTypeWhenTableMissing() throws SQLException {
        ddl("DROP TABLE IF EXISTS `ad_chk_unmapped`");

        DatabaseCheckResult result = check(AdChkUnmapped.class, false);

        assertThat(result.getOtherMessage()).as("存在无法映射的字段时应累积一条错误信息").hasSize(1);
        assertThat(result.getOtherMessage().get(0)).as("错误信息应指出字段无法映射为 SQL 类型")
                .contains("'payload'").contains("can't be mapped to a sql type.");
        assertThat(result.getAdvise2AddTable()).as("存在无法映射的字段时应放弃建表，不产出建表建议").isEmpty();
    }

    @Test
    public void unmappableTypeWhenTableExists() throws SQLException {
        ddl("DROP TABLE IF EXISTS `ad_chk_unmapped_exist`", """
                CREATE TABLE `ad_chk_unmapped_exist` (
                  `id` bigint NOT NULL AUTO_INCREMENT,
                  `name` varchar(255) NULL,
                  PRIMARY KEY (`id`)
                )
                """);

        DatabaseCheckResult result = check(AdChkUnmappedExist.class, false);

        assertThat(result.getOtherMessage()).as("表已存在时，无法映射的字段同样应累积一条错误信息").hasSize(1);
        assertThat(result.getOtherMessage().get(0)).as("错误信息应指出字段无法映射为 SQL 类型")
                .contains("'payload'").contains("can't be mapped to a sql type.");
        assertThat(result.getAdvise2AddColumn()).as("无法映射的字段不应产出加列建议").isEmpty();
    }

    @Test
    public void customTypeMapperAppliesOnAddTable() throws SQLException {
        ddl("DROP TABLE IF EXISTS `ad_chk_mapper`");

        DatabaseCheckResult result = check(AdChkMapper.class, CUSTOM_MAPPER, false);

        assertThat(result.getAdvise2AddTable()).as("表不存在时应产出且只产出一条建表语句").hasSize(1);
        String sql = result.getAdvise2AddTable().get(0);
        assertThat(sql).as("自定义 mapper 按字段名给出的类型应体现在建表语句里").contains("`order_number` char(20) DEFAULT NULL");
        assertThat(sql).as("自定义 mapper 按 scale 给出的类型应体现在建表语句里").contains("`memo` varchar(64) DEFAULT NULL");
        assertThat(sql).as("自定义 mapper 对该 String 字段返回 null，应回退到默认映射").contains("`name` varchar(255) DEFAULT NULL");
        assertThat(sql).as("自定义 mapper 对 Integer 返回 null，应回退到默认映射").contains("`qty` int DEFAULT NULL");
        assertThat(sql).as("自定义 mapper 对主键返回 null，主键同样应回退到默认映射").contains("`id` bigint NOT NULL AUTO_INCREMENT");
        assertThat(result.getOtherMessage()).as("自定义映射与默认映射合起来覆盖了全部字段，不应有其它问题").isEmpty();
    }

    @Test
    public void customTypeMapperAppliesOnAlterAdvise() throws SQLException {
        ddl("DROP TABLE IF EXISTS `ad_chk_mapper_alter`", """
                CREATE TABLE `ad_chk_mapper_alter` (
                  `id` bigint NOT NULL AUTO_INCREMENT,
                  `order_number` varchar(255) NULL,
                  `qty` varchar(255) NULL,
                  PRIMARY KEY (`id`)
                )
                """);

        DatabaseCheckResult result = check(AdChkMapperAlter.class, CUSTOM_MAPPER, true);

        assertThat(result.getAdvise2AddColumn()).as("表中缺少 serial_number 列时应产出且只产出一条加列建议").hasSize(1);
        assertThat(result.getAdvise2AddColumn().get(0)).as("加列建议应采用自定义 mapper 给出的类型")
                .contains("ALTER TABLE `ad_chk_mapper_alter` ADD COLUMN `serial_number` char(20)");
        assertThat(result.getAdvise2UpdateColumn()).as("order_number 与 qty 两列类型都与实体不符，应产出两条改列建议").hasSize(2);
        assertThat(result.getAdvise2UpdateColumn()).as("改列建议应把 order_number 改成自定义 mapper 给出的类型")
                .anySatisfy(advise -> assertThat(advise).as("改列建议应把 order_number 改成 char(20)")
                        .contains("MODIFY COLUMN `order_number` char(20)"));
        assertThat(result.getAdvise2UpdateColumn()).as("自定义 mapper 对 Integer 返回 null，改列建议应回退到默认映射")
                .anySatisfy(advise -> assertThat(advise).as("改列建议应把 qty 改成默认映射的 int")
                        .contains("MODIFY COLUMN `qty` int"));
        assertThat(result.getAdvise2AddTable()).as("表已存在时不应产出建表建议").isEmpty();
        assertThat(result.getAdvise2RemoveColumn()).as("表中没有实体之外的列，不应产出删列建议").isEmpty();
        assertThat(result.getOtherMessage()).as("主键类型与表完全一致，不应有其它问题").isEmpty();
    }

    @Test
    public void customTypeMapperReturningBlankFallsBackToDefault() throws SQLException {
        ddl("DROP TABLE IF EXISTS `ad_chk_mapper_blank`");

        DatabaseCheckResult result = check(AdChkMapperBlank.class,
                (javaType, scale, field) -> String.class.isAssignableFrom(javaType) ? "   " : null, false);

        assertThat(result.getAdvise2AddTable()).as("表不存在时应产出且只产出一条建表语句").hasSize(1);
        assertThat(result.getAdvise2AddTable().get(0)).as("自定义 mapper 返回空白串与返回 null 等价，都应回退到默认映射")
                .contains("`name` varchar(255) DEFAULT NULL");
        assertThat(result.getOtherMessage()).as("回退后类型可映射，不应被当成无法映射的字段").isEmpty();
    }
}
