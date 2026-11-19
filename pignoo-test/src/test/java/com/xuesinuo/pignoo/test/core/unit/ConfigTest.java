package com.xuesinuo.pignoo.test.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.xuesinuo.pignoo.core.PignooConfig;
import com.xuesinuo.pignoo.core.config.AnnotationMode;
import com.xuesinuo.pignoo.core.config.DatabaseEngine;
import com.xuesinuo.pignoo.core.config.NamingMode;
import com.xuesinuo.pignoo.core.config.PrimaryKeyNamingConvention;

/**
 * PignooConfig的默认值、复制与Logo打印
 * <p>
 * Default values, copy and logo printing of PignooConfig
 */
public class ConfigTest {

    /**
     * 各项默认值
     */
    @Test
    public void defaults() {
        PignooConfig config = new PignooConfig();
        assertNull(config.getEngine(), "数据库引擎默认不配置，留给运行时从连接读取");
        assertSame(AnnotationMode.MIX, config.getAnnotationMode(), "注解模式默认为MIX");
        assertSame(NamingMode.CAMEL_TO_SNAKE, config.getNamingMode(), "命名模式默认为CAMEL_TO_SNAKE");
        assertSame(PrimaryKeyNamingConvention.DEFAULT, config.getPrimaryKeyNamingConvention(), "主键命名规则默认为内置的DEFAULT");
        assertEquals("id", config.getPrimaryKeyNamingConvention().naming("any_table", "Any", PignooConfig.class, PignooConfig.class.getDeclaredFields()), "内置主键命名规则应恒返回id");
        assertTrue(config.getAutoPrimaryKey(), "主键默认按自增处理");
        assertTrue(config.getOpenSetterProxy(), "setter代理默认开启");
    }

    /**
     * copy逐项复制配置
     */
    @Test
    public void copyAllFields() {
        PrimaryKeyNamingConvention convention = (tableName, className, c, fields) -> "biz_key";
        PignooConfig config = new PignooConfig();
        config.setEngine(DatabaseEngine.MySQL);
        config.setAnnotationMode(AnnotationMode.MUST);
        config.setNamingMode(NamingMode.SAME);
        config.setPrimaryKeyNamingConvention(convention);
        config.setAutoPrimaryKey(false);
        config.setOpenSetterProxy(false);

        PignooConfig copied = config.copy();
        assertNotSame(config, copied, "copy应返回新实例");
        assertSame(DatabaseEngine.MySQL, copied.getEngine(), "engine应被复制");
        assertSame(AnnotationMode.MUST, copied.getAnnotationMode(), "annotationMode应被复制");
        assertSame(NamingMode.SAME, copied.getNamingMode(), "namingMode应被复制");
        assertSame(convention, copied.getPrimaryKeyNamingConvention(), "primaryKeyNamingConvention应按引用复制");
        assertFalse(copied.getAutoPrimaryKey(), "autoPrimaryKey应被复制");
        assertFalse(copied.getOpenSetterProxy(), "openSetterProxy应被复制");
        assertEquals(config, copied, "复制后的配置应与原配置值相等");
    }

    /**
     * copy出来的配置与原配置互不影响
     */
    @Test
    public void copyIsIndependent() {
        PignooConfig config = new PignooConfig();
        PignooConfig copied = config.copy();

        copied.setNamingMode(NamingMode.SAME);
        copied.setAutoPrimaryKey(false);
        assertSame(NamingMode.CAMEL_TO_SNAKE, config.getNamingMode(), "改副本不应影响原配置");
        assertTrue(config.getAutoPrimaryKey(), "改副本不应影响原配置");

        config.setAnnotationMode(AnnotationMode.MUST);
        assertSame(AnnotationMode.MIX, copied.getAnnotationMode(), "改原配置不应影响副本");
    }

    /**
     * 配置按值比较：EntityMapper的缓存以此为key
     */
    @Test
    public void equalsByValue() {
        PignooConfig one = new PignooConfig();
        PignooConfig another = new PignooConfig();
        assertEquals(one, another, "两个默认配置应当值相等");
        assertEquals(one.hashCode(), another.hashCode(), "值相等的配置应有相同的hashCode");

        another.setNamingMode(NamingMode.SAME);
        assertNotSame(one, another, "用例前提：这里应是两个不同的配置对象");
        assertFalse(one.equals(another), "任一配置项不同，两个配置就不相等");
    }

    /**
     * Logo只打印一次：showLogo是全局静态开关，第一个配置对象打印后即关闭
     */
    @Test
    public void showLogoOnlyOnce() {
        PrintStream originOut = System.out;
        String firstOutput;
        String secondOutput;
        try {
            ByteArrayOutputStream first = new ByteArrayOutputStream();
            PignooConfig.showLogo = true;
            System.setOut(new PrintStream(first, true, StandardCharsets.UTF_8));
            new PignooConfig();

            ByteArrayOutputStream second = new ByteArrayOutputStream();
            System.setOut(new PrintStream(second, true, StandardCharsets.UTF_8));
            new PignooConfig();

            firstOutput = first.toString(StandardCharsets.UTF_8);
            secondOutput = second.toString(StandardCharsets.UTF_8);
        } finally {
            System.setOut(originOut);
            // Logo在本用例中已经消费掉，这里不还原成true，避免后续用例再打印一次
            PignooConfig.showLogo = false;
        }
        assertTrue(firstOutput.contains("pignoo"), "showLogo为true时，第一个配置对象应打印Logo，实际输出为：" + firstOutput);
        assertEquals("", secondOutput, "Logo只应打印一次，第二个配置对象不应再打印，实际输出为：" + secondOutput);
        assertFalse(PignooConfig.showLogo, "打印过Logo后，全局开关应被关闭");
    }
}
