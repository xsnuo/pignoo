package com.xuesinuo.pignoo.test.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.xuesinuo.pignoo.core.PignooConfig;
import com.xuesinuo.pignoo.core.annotation.Column;
import com.xuesinuo.pignoo.core.annotation.Link;
import com.xuesinuo.pignoo.core.annotation.Table;
import com.xuesinuo.pignoo.core.config.AnnotationMode;
import com.xuesinuo.pignoo.core.config.NamingMode;
import com.xuesinuo.pignoo.core.entity.EntityMapper;
import com.xuesinuo.pignoo.test.fixture.LinkProjection;
import com.xuesinuo.pignoo.test.fixture.MustTable;
import com.xuesinuo.pignoo.test.fixture.PkAuto;
import com.xuesinuo.pignoo.test.fixture.PkNonAuto;
import com.xuesinuo.pignoo.test.fixture.Plain;

import lombok.Data;

/**
 * 实体映射规则的纯单元测试：表名、列名、主键、注解模式、@Link 与映射器缓存
 * <p>
 * Pure unit tests for entity mapping rules: table name, column name, primary key, annotation mode, @Link and mapper cache
 */
public class MappingTest {

    private static PignooConfig config() {
        return new PignooConfig();
    }

    private static List<String> fieldNames(EntityMapper<?> mapper) {
        return mapper.fields().stream().map(Field::getName).toList();
    }

    /** 无@Table：表名由命名规则推导 */
    @Data
    public static class UserAccount {
        private Long id;
        private String userName;
    }

    /** @Table不填值：等价于没写表名，仍由命名规则推导 */
    @Table
    @Data
    public static class BlankTable {
        private Long id;
    }

    /** 列名的三种来源：注解指定、注解为空按规则推导、注解忽略 */
    @Table("fx_column")
    @Data
    public static class ColumnOverride {
        private Long id;
        @Column("user_name_2")
        private String userName;
        @Column(ignore = true)
        private String secret;
        @Column
        private String orderNo;
    }

    /** 只有一半字段标了@Column，用于对比MIX与MUST */
    @Table("fx_must_mix")
    @Data
    public static class MustPartial {
        @Column
        private Long id;
        private String name;
    }

    /** @Link到Plain，且多出一个Plain没有的列 */
    @Link(Plain.class)
    @Data
    public static class LinkExtra {
        private Long id;
        private String name;
        private String notInPlain;
    }

    /**
     * @Table显式指定的表名原样使用
     */
    @Test
    public void tableNameFromAnnotation() {
        assertEquals("fx_plain", EntityMapper.build(Plain.class, config()).tableName(), "@Table显式指定的表名应原样使用");
        assertEquals("fx_must", EntityMapper.build(MustTable.class, config()).tableName(), "@Table显式指定的表名应原样使用");
        assertEquals("fx_pk_auto", EntityMapper.build(PkAuto.class, config()).tableName(), "@Table显式指定的表名应原样使用");
    }

    /**
     * 没有表名时，SAME取类名原样，CAMEL_TO_SNAKE取驼峰转下划线
     */
    @Test
    public void tableNameByNamingMode() {
        PignooConfig camel = config();
        assertEquals("user_account", EntityMapper.build(UserAccount.class, camel).tableName(), "CAMEL_TO_SNAKE模式下表名应为类名的下划线形式");
        assertEquals("blank_table", EntityMapper.build(BlankTable.class, camel).tableName(), "@Table不填值时表名仍按命名规则推导");

        PignooConfig same = config();
        same.setNamingMode(NamingMode.SAME);
        assertEquals("UserAccount", EntityMapper.build(UserAccount.class, same).tableName(), "SAME模式下表名应等于类名原样");
        assertEquals("BlankTable", EntityMapper.build(BlankTable.class, same).tableName(), "SAME模式下表名应等于类名原样");
        assertEquals("fx_plain", EntityMapper.build(Plain.class, same).tableName(), "@Table显式指定的表名不受命名规则影响");
    }

    /**
     * @Link实体继承目标实体的表名
     */
    @Test
    public void tableNameFromLink() {
        assertEquals("fx_plain", EntityMapper.build(LinkProjection.class, config()).tableName(), "@Link实体应继承目标实体的表名");
        assertEquals("fx_plain", EntityMapper.build(LinkExtra.class, config()).tableName(), "@Link实体应继承目标实体的表名");
    }

    /**
     * @Column.value覆盖列名，ignore=true的字段被排除
     */
    @Test
    public void columnNameOverrideAndIgnore() {
        EntityMapper<ColumnOverride> mapper = EntityMapper.build(ColumnOverride.class, config());
        assertEquals(List.of("id", "user_name_2", "order_no"), mapper.columns(), "@Column.value应覆盖列名，@Column不填值时仍按命名规则推导");
        assertEquals(List.of("id", "userName", "orderNo"), fieldNames(mapper), "ignore=true的字段不应出现在属性列表中");
        assertFalse(mapper.columns().contains("secret"), "ignore=true的字段不应出现在列表中");
        assertEquals(List.of("setId", "setUserName", "setOrderNo"), mapper.setterNames(), "被忽略的字段，其setter也应同步排除");
    }

    /**
     * 列名推导：SAME取属性名原样，CAMEL_TO_SNAKE取驼峰转下划线
     */
    @Test
    public void columnNameByNamingMode() {
        PignooConfig same = config();
        same.setNamingMode(NamingMode.SAME);
        assertEquals(List.of("id", "user_name_2", "orderNo"), EntityMapper.build(ColumnOverride.class, same).columns(), "SAME模式下列名等于属性名，但@Column.value仍然优先");
        assertEquals(List.of("id", "userName"), EntityMapper.build(UserAccount.class, same).columns(), "SAME模式下列名应等于属性名原样");
        assertEquals(List.of("id", "user_name"), EntityMapper.build(UserAccount.class, config()).columns(), "CAMEL_TO_SNAKE模式下列名应为属性名的下划线形式");
    }

    /**
     * @Column(primaryKey)显式声明的主键
     */
    @Test
    public void primaryKeyFromAnnotation() {
        EntityMapper<PkAuto> auto = EntityMapper.build(PkAuto.class, config());
        assertEquals("auto_key", auto.primaryKeyColumn(), "主键列名应由主键字段名推导");
        assertEquals("autoKey", auto.primaryKeyField().getName(), "主键属性应为标注了@Column(primaryKey)的字段");
        assertTrue(auto.autoPrimaryKey(), "PrimaryKey.AUTO应被识别为自增主键");

        EntityMapper<PkNonAuto> nonAuto = EntityMapper.build(PkNonAuto.class, config());
        assertEquals("biz_key", nonAuto.primaryKeyColumn(), "主键列名应由主键字段名推导");
        assertEquals("bizKey", nonAuto.primaryKeyField().getName(), "主键属性应为标注了@Column(primaryKey)的字段");
        assertFalse(nonAuto.autoPrimaryKey(), "PrimaryKey.NON_AUTO应被识别为非自增主键");
    }

    /**
     * 没有主键注解时，按默认约定找名为id的列
     */
    @Test
    public void primaryKeyByConvention() {
        EntityMapper<Plain> mapper = EntityMapper.build(Plain.class, config());
        assertEquals("id", mapper.primaryKeyColumn(), "没有主键注解时应按默认约定找到id列");
        assertEquals("id", mapper.primaryKeyField().getName(), "约定主键应对应到id属性");
        assertTrue(mapper.autoPrimaryKey(), "约定主键的自增性取自配置，默认为true");
    }

    /**
     * 配置项autoPrimaryKey只影响约定主键，不影响注解声明的主键
     */
    @Test
    public void autoPrimaryKeyConfigOnlyAffectsConventionPrimaryKey() {
        PignooConfig off = config();
        off.setAutoPrimaryKey(false);
        assertFalse(EntityMapper.build(Plain.class, off).autoPrimaryKey(), "配置autoPrimaryKey=false应作用于约定主键");
        assertTrue(EntityMapper.build(PkAuto.class, off).autoPrimaryKey(), "注解声明的AUTO主键不应被配置覆盖");

        PignooConfig on = config();
        on.setAutoPrimaryKey(true);
        assertFalse(EntityMapper.build(PkNonAuto.class, on).autoPrimaryKey(), "注解声明的NON_AUTO主键不应被配置覆盖");

        PignooConfig unset = config();
        unset.setAutoPrimaryKey(null);
        assertTrue(EntityMapper.build(Plain.class, unset).autoPrimaryKey(), "配置autoPrimaryKey为null时，约定主键按自增处理");
    }

    /**
     * 自定义主键命名规则；规则为null时回落到默认的id
     */
    @Test
    public void primaryKeyNamingConvention() {
        PignooConfig custom = config();
        custom.setPrimaryKeyNamingConvention((tableName, className, c, fields) -> "code");
        EntityMapper<Plain> mapper = EntityMapper.build(Plain.class, custom);
        assertEquals("code", mapper.primaryKeyColumn(), "主键列应由自定义命名规则决定");
        assertEquals("code", mapper.primaryKeyField().getName(), "主键属性应跟随自定义命名规则");

        PignooConfig none = config();
        none.setPrimaryKeyNamingConvention(null);
        assertEquals("id", EntityMapper.build(Plain.class, none).primaryKeyColumn(), "主键命名规则为null时应回落到默认的id");
    }

    /**
     * MIX模式：没有@Column的字段也入列
     */
    @Test
    public void annotationModeMix() {
        assertEquals(List.of("id", "name", "code", "money", "qty"), EntityMapper.build(Plain.class, config()).columns(), "MIX模式下没有@Column的字段也应映射为列");
        assertEquals(List.of("id", "name"), EntityMapper.build(MustPartial.class, config()).columns(), "MIX模式下标注与未标注的字段都应映射为列");
    }

    /**
     * MUST模式：没有@Column的字段被忽略
     */
    @Test
    public void annotationModeMust() {
        PignooConfig must = config();
        must.setAnnotationMode(AnnotationMode.MUST);
        EntityMapper<MustTable> mustTable = EntityMapper.build(MustTable.class, must);
        assertEquals("fx_must", mustTable.tableName(), "MUST模式下有@Table的实体应正常解析");
        assertEquals(List.of("id", "name"), mustTable.columns(), "MUST模式下标注了@Column的字段应正常映射");
        assertEquals("id", mustTable.primaryKeyColumn(), "MUST模式下仍可按约定找到主键");
        assertEquals(List.of("id"), EntityMapper.build(MustPartial.class, must).columns(), "MUST模式下没有@Column的字段应被忽略");
    }

    /**
     * @Link实体只保留目标实体中存在的列
     */
    @Test
    public void linkColumnIntersection() {
        EntityMapper<LinkProjection> projection = EntityMapper.build(LinkProjection.class, config());
        assertEquals(List.of("id", "name"), projection.columns(), "@Link实体的列应是自身列与目标实体列的交集");
        assertEquals("id", projection.primaryKeyColumn(), "@Link实体的主键仍按自身规则解析");

        EntityMapper<LinkExtra> extra = EntityMapper.build(LinkExtra.class, config());
        assertEquals(List.of("id", "name"), extra.columns(), "目标实体中不存在的列应被剔除");
        assertEquals(List.of("id", "name"), fieldNames(extra), "被剔除的列，其属性也应同步剔除");
        assertEquals(List.of("setId", "setName"), extra.setterNames(), "被剔除的列，其setter也应同步剔除");
    }

    /**
     * 配置项为null时回落到默认值，且解析过程不修改调用方的配置
     */
    @Test
    public void nullConfigItemsFallBackToDefault() {
        PignooConfig config = config();
        config.setAnnotationMode(null);
        config.setNamingMode(null);
        EntityMapper<UserAccount> mapper = EntityMapper.build(UserAccount.class, config);
        assertEquals("user_account", mapper.tableName(), "命名模式为null时应回落到CAMEL_TO_SNAKE");
        assertEquals(List.of("id", "user_name"), mapper.columns(), "注解模式为null时应回落到MIX");
        assertNull(config.getAnnotationMode(), "解析过程不应修改调用方传入的配置");
        assertNull(config.getNamingMode(), "解析过程不应修改调用方传入的配置");
    }

    /**
     * 同一实体+等值配置命中缓存，返回同一个映射器实例
     */
    @Test
    public void mapperCache() {
        PignooConfig one = config();
        PignooConfig another = config();
        assertNotSame(one, another, "用例前提：这里应是两个不同的配置对象");
        assertEquals(one, another, "两个默认配置应当值相等（映射器缓存以配置的值作为key）");
        assertSame(EntityMapper.build(Plain.class, one), EntityMapper.build(Plain.class, another), "同一实体+等值配置应命中缓存，返回同一个映射器");

        PignooConfig same = config();
        same.setNamingMode(NamingMode.SAME);
        assertNotSame(EntityMapper.build(Plain.class, one), EntityMapper.build(Plain.class, same), "配置不同应生成不同的映射器");
        assertNotSame(EntityMapper.build(Plain.class, one), EntityMapper.build(MustTable.class, one), "实体不同应生成不同的映射器");
    }

    /**
     * buildEntity每次构造新实例，getType返回原始类型
     */
    @Test
    public void buildEntityAndType() {
        EntityMapper<Plain> mapper = EntityMapper.build(Plain.class, config());
        assertSame(Plain.class, mapper.getType(), "getType应返回映射的实体类型");
        Plain first = mapper.buildEntity();
        Plain second = mapper.buildEntity();
        assertNotNull(first, "buildEntity不应返回null");
        assertNotSame(first, second, "buildEntity每次都应构造新实例");
        assertNull(first.getId(), "buildEntity应使用无参构造，属性保持默认值");
    }
}
