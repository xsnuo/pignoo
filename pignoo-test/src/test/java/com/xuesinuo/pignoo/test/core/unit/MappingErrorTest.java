package com.xuesinuo.pignoo.test.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.xuesinuo.pignoo.core.PignooConfig;
import com.xuesinuo.pignoo.core.annotation.Column;
import com.xuesinuo.pignoo.core.annotation.Table;
import com.xuesinuo.pignoo.core.config.AnnotationMode;
import com.xuesinuo.pignoo.core.entity.EntityMapper;
import com.xuesinuo.pignoo.core.exception.MapperException;
import com.xuesinuo.pignoo.test.fixture.MultiPk;
import com.xuesinuo.pignoo.test.fixture.MustNoAnn;
import com.xuesinuo.pignoo.test.fixture.NoArgCtorMissing;
import com.xuesinuo.pignoo.test.fixture.Plain;

import lombok.Data;

/**
 * 实体解析的异常路径：断言异常类型与提示信息
 * <p>
 * Failure paths of entity parsing: assert the exception type and the message
 */
public class MappingErrorTest {

    private static PignooConfig config() {
        return new PignooConfig();
    }

    /** 既没有名为id的列，也没有主键注解 */
    @Data
    public static class NoPk {
        private String name;
        private String code;
    }

    /** 唯一可能当主键的列被显式声明为不是主键 */
    @Data
    public static class PkMarkedNot {
        @Column(primaryKey = Column.PrimaryKey.NOT)
        private Long id;
        private String name;
    }

    /** 唯一可能当主键的列被忽略 */
    @Data
    public static class PkIgnored {
        @Column(ignore = true)
        private Long id;
        private String name;
    }

    /** 有属性但没有getter */
    @Table("fx_no_getter")
    public static class NoGetter {
        private Long id;

        public void setId(Long id) {
            this.id = id;
        }
    }

    /** 有getter但没有setter */
    @Table("fx_no_setter")
    public static class NoSetter {
        private Long id;

        public Long getId() {
            return this.id;
        }
    }

    /**
     * 缺少无参构造
     */
    @Test
    public void noArgConstructorMissing() {
        MapperException exception = assertThrows(MapperException.class, () -> EntityMapper.build(NoArgCtorMissing.class, config()), "缺少无参构造的实体应解析失败");
        assertTrue(exception.getMessage().contains("no-arg constructor"), "异常信息应指出缺少无参构造，实际为：" + exception.getMessage());
        assertTrue(exception.getMessage().contains(NoArgCtorMissing.class.getName()), "异常信息应指出出错的实体，实际为：" + exception.getMessage());
    }

    /**
     * 声明了多个主键
     */
    @Test
    public void multiplePrimaryKey() {
        MapperException exception = assertThrows(MapperException.class, () -> EntityMapper.build(MultiPk.class, config()), "声明了多个主键的实体应解析失败");
        assertTrue(exception.getMessage().contains("more than one @PrimaryKey"), "异常信息应指出主键不止一个，实际为：" + exception.getMessage());
    }

    /**
     * MUST模式下缺少@Table或@Link
     */
    @Test
    public void mustModeMissingTableAnnotation() {
        PignooConfig must = config();
        must.setAnnotationMode(AnnotationMode.MUST);
        MapperException exception = assertThrows(MapperException.class, () -> EntityMapper.build(MustNoAnn.class, must), "MUST模式下没有@Table也没有@Link的实体应解析失败");
        assertTrue(exception.getMessage().contains("missing @Table or @Link"), "异常信息应指出缺少@Table或@Link，实际为：" + exception.getMessage());

        assertEquals("must_no_ann", EntityMapper.build(MustNoAnn.class, config()).tableName(), "同一个实体在MIX模式下应能正常解析");
    }

    /**
     * 找不到主键：没有符合约定的列
     */
    @Test
    public void primaryKeyNotFound() {
        MapperException exception = assertThrows(MapperException.class, () -> EntityMapper.build(NoPk.class, config()), "找不到主键的实体应解析失败");
        assertTrue(exception.getMessage().contains("PrimaryKey not found"), "异常信息应指出找不到主键，实际为：" + exception.getMessage());
    }

    /**
     * 找不到主键：符合约定的列被声明为不是主键，或者被忽略
     */
    @Test
    public void primaryKeyExcludedByAnnotation() {
        MapperException markedNot = assertThrows(MapperException.class, () -> EntityMapper.build(PkMarkedNot.class, config()), "约定主键被声明为PrimaryKey.NOT时应解析失败");
        assertTrue(markedNot.getMessage().contains("PrimaryKey not found"), "异常信息应指出找不到主键，实际为：" + markedNot.getMessage());

        MapperException ignored = assertThrows(MapperException.class, () -> EntityMapper.build(PkIgnored.class, config()), "约定主键被ignore时应解析失败");
        assertTrue(ignored.getMessage().contains("PrimaryKey not found"), "异常信息应指出找不到主键，实际为：" + ignored.getMessage());
    }

    /**
     * 找不到主键：自定义的主键命名规则给不出可用的列名
     */
    @Test
    public void primaryKeyNamingConventionGivesNothing() {
        PignooConfig blank = config();
        blank.setPrimaryKeyNamingConvention((tableName, className, c, fields) -> "  ");
        MapperException blankName = assertThrows(MapperException.class, () -> EntityMapper.build(Plain.class, blank), "主键命名规则返回空白时应解析失败");
        assertTrue(blankName.getMessage().contains("PrimaryKey not found"), "异常信息应指出找不到主键，实际为：" + blankName.getMessage());

        PignooConfig unknown = config();
        unknown.setPrimaryKeyNamingConvention((tableName, className, c, fields) -> "no_such_column");
        MapperException unknownName = assertThrows(MapperException.class, () -> EntityMapper.build(Plain.class, unknown), "主键命名规则指向不存在的列时应解析失败");
        assertTrue(unknownName.getMessage().contains("PrimaryKey not found"), "异常信息应指出找不到主键，实际为：" + unknownName.getMessage());
    }

    /**
     * 属性缺少getter或setter
     */
    @Test
    public void accessorMissing() {
        MapperException noGetter = assertThrows(MapperException.class, () -> EntityMapper.build(NoGetter.class, config()), "属性没有getter时应解析失败");
        assertTrue(noGetter.getMessage().contains("read getter failed"), "异常信息应指出读取getter失败，实际为：" + noGetter.getMessage());

        MapperException noSetter = assertThrows(MapperException.class, () -> EntityMapper.build(NoSetter.class, config()), "属性没有setter时应解析失败");
        assertTrue(noSetter.getMessage().contains("read setter failed"), "异常信息应指出读取setter失败，实际为：" + noSetter.getMessage());
    }
}
