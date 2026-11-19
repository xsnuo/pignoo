package com.xuesinuo.pignoo.test.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import com.xuesinuo.pignoo.core.config.DatabaseEngine;

/**
 * 数据库引擎的名称识别
 * <p>
 * Recognizing the database engine by name
 */
public class DatabaseEngineTest {

    /**
     * 引擎名称全小写
     */
    @Test
    public void engineName() {
        assertEquals(1, DatabaseEngine.values().length, "当前只支持一种数据库引擎");
        assertEquals("mysql", DatabaseEngine.MySQL.getName(), "引擎名称应为全小写的mysql");
    }

    /**
     * 名称识别大小写不敏感
     */
    @Test
    public void byNameIgnoreCase() {
        assertSame(DatabaseEngine.MySQL, DatabaseEngine.getDatabaseEngineByName("mysql"), "mysql应识别为MySQL");
        assertSame(DatabaseEngine.MySQL, DatabaseEngine.getDatabaseEngineByName("MySQL"), "名称识别应大小写不敏感");
        assertSame(DatabaseEngine.MySQL, DatabaseEngine.getDatabaseEngineByName("MYSQL"), "名称识别应大小写不敏感");
    }

    /**
     * 只要包含引擎名称的子串就算命中，用于直接对接数据库产品名
     */
    @Test
    public void byNameContains() {
        assertSame(DatabaseEngine.MySQL, DatabaseEngine.getDatabaseEngineByName("MySQL Community Server - GPL"), "包含引擎名称的产品名应能识别");
        assertSame(DatabaseEngine.MySQL, DatabaseEngine.getDatabaseEngineByName("  mysql  "), "名称不做trim，但包含关系仍然成立");
        assertSame(DatabaseEngine.MySQL, DatabaseEngine.getDatabaseEngineByName("xxmysqlxx"), "只要包含引擎名称的子串就算命中");
    }

    /**
     * null、空串与未知名称都返回null
     */
    @Test
    public void byNameUnknown() {
        assertNull(DatabaseEngine.getDatabaseEngineByName(null), "名称为null应返回null");
        assertNull(DatabaseEngine.getDatabaseEngineByName(""), "名称为空串应返回null");
        assertNull(DatabaseEngine.getDatabaseEngineByName("   "), "名称为空白串应返回null");
        assertNull(DatabaseEngine.getDatabaseEngineByName("PostgreSQL"), "未知的数据库产品名应返回null");
        assertNull(DatabaseEngine.getDatabaseEngineByName("my sql"), "名称对不上应返回null");
    }
}
