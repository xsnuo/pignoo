package com.xuesinuo.pignoo.core.config;

import java.sql.Connection;
import java.sql.SQLException;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 数据库引擎
 * <p>
 * Database engine
 * 
 * @author xuesinuo
 * @version 0.1.0
 */
@AllArgsConstructor
@Getter
public enum DatabaseEngine {
    MySQL("mysql");

    /**
     * 数据库引擎名称，全小写
     * <p>
     * Database engine name, all lowercase
     */
    private String name;

    /**
     * 根据名称获取数据库引擎
     * <p>
     * Get database engine by name
     */
    public static DatabaseEngine getDatabaseEngineByName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        for (DatabaseEngine engine : DatabaseEngine.values()) {
            if (name.toLowerCase().contains(engine.getName())) {
                return engine;
            }
        }
        return null;
    }

    /**
     * 根据数据库连接获取数据库引擎
     * <p>
     * Get database engine by connection
     */
    public static DatabaseEngine getDatabaseEngineByConnection(Connection conn) throws SQLException {
        if (conn == null || conn.getMetaData() == null) {
            return null;
        }
        return getDatabaseEngineByName(conn.getMetaData().getDatabaseProductName());
    }
}
