package com.xuesinuo.pignoo.test.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.xuesinuo.pignoo.core.PignooConfig;
import com.xuesinuo.pignoo.core.annotation.Table;
import com.xuesinuo.pignoo.core.config.NamingMode;
import com.xuesinuo.pignoo.core.entity.EntityMapper;

import lombok.Data;

/**
 * 驼峰转下划线的边界行为：通过表名与列名的解析结果间接验证
 * <p>
 * Boundary behaviours of camel-to-underline, verified indirectly through the resolved table name and column names
 * <p>
 * 规则以代码现状为准：首字符只做小写、不加下划线；其后每个大写字符都加一个下划线并转小写；原有下划线原样保留
 */
public class NamingAlgoTest {

    private static PignooConfig camel() {
        return new PignooConfig();
    }

    private static PignooConfig same() {
        PignooConfig config = new PignooConfig();
        config.setNamingMode(NamingMode.SAME);
        return config;
    }

    /** 多段驼峰 */
    @Data
    public static class UserAccountInfo {
        private Long id;
    }

    /** 连续大写 */
    @Data
    public static class HTTPServer {
        private Long id;
    }

    /** 全大写的两字母 */
    @Data
    public static class ID {
        private Long id;
    }

    /** 名字里本来就有下划线 */
    @Data
    public static class User_Account {
        private Long id;
    }

    /** 单字符 */
    @Data
    public static class A {
        private Long id;
    }

    /** 驼峰中夹数字 */
    @Data
    public static class Order2Item {
        private Long id;
    }

    /** 各种属性名的列名推导 */
    @Table("fx_naming")
    @Data
    public static class NamingColumns {
        private Long id;
        private String userName;
        private String URL;
        private String legacy_code;
        private String aB;
        private String name2Code;
    }

    /**
     * 多段驼峰与夹数字的驼峰
     */
    @Test
    public void tableNameMultiSegmentCamel() {
        assertEquals("user_account_info", EntityMapper.build(UserAccountInfo.class, camel()).tableName(), "多段驼峰的每个大写字母前都应插入下划线");
        assertEquals("order2_item", EntityMapper.build(Order2Item.class, camel()).tableName(), "数字不影响下划线插入，只有大写字母才插入下划线");
    }

    /**
     * 连续大写：每个大写字母都各自插入一个下划线，不做缩写词识别
     */
    @Test
    public void tableNameContinuousUpperCase() {
        assertEquals("h_t_t_p_server", EntityMapper.build(HTTPServer.class, camel()).tableName(), "连续大写不会被识别成缩写，每个大写字母都会各自插入下划线");
        assertEquals("i_d", EntityMapper.build(ID.class, camel()).tableName(), "全大写的类名同样是逐字母插入下划线");
    }

    /**
     * 首字母大写只转小写、不插入下划线；单字符类名直接转小写
     */
    @Test
    public void tableNameFirstCharAndSingleChar() {
        assertEquals("a", EntityMapper.build(A.class, camel()).tableName(), "首字符只做小写，不插入下划线");
        assertEquals("user_account_info", EntityMapper.build(UserAccountInfo.class, camel()).tableName(), "首字符只做小写，不插入下划线");
    }

    /**
     * 名字里本来就有下划线：原样保留，其后的大写字母仍会再插入一个下划线，于是出现双下划线
     */
    @Test
    public void tableNameAlreadyContainsUnderline() {
        assertEquals("user__account", EntityMapper.build(User_Account.class, camel()).tableName(), "已有的下划线原样保留，其后的大写字母仍会再插入一个下划线");
    }

    /**
     * SAME模式下不做任何转换
     */
    @Test
    public void tableNameSameMode() {
        assertEquals("HTTPServer", EntityMapper.build(HTTPServer.class, same()).tableName(), "SAME模式下表名等于类名原样");
        assertEquals("User_Account", EntityMapper.build(User_Account.class, same()).tableName(), "SAME模式下表名等于类名原样");
        assertEquals("ID", EntityMapper.build(ID.class, same()).tableName(), "SAME模式下表名等于类名原样");
    }

    /**
     * 列名推导与表名共用同一套规则
     */
    @Test
    public void columnNameCamelToSnake() {
        List<String> columns = EntityMapper.build(NamingColumns.class, camel()).columns();
        assertEquals(List.of("id", "user_name", "u_r_l", "legacy_code", "a_b", "name2_code"), columns, "列名推导应与表名共用同一套驼峰转下划线规则");
    }

    /**
     * SAME模式下列名等于属性名原样
     */
    @Test
    public void columnNameSameMode() {
        EntityMapper<NamingColumns> mapper = EntityMapper.build(NamingColumns.class, same());
        assertEquals("fx_naming", mapper.tableName(), "@Table显式指定的表名不受命名规则影响");
        assertEquals(List.of("id", "userName", "URL", "legacy_code", "aB", "name2Code"), mapper.columns(), "SAME模式下列名等于属性名原样");
    }
}
