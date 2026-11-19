package com.xuesinuo.pignoo.test.autodatabase.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.xuesinuo.pignoo.autodatabase.TypeMapper;
import com.xuesinuo.pignoo.autodatabase.impl.DatabaseChecker4MySql;
import com.xuesinuo.pignoo.core.annotation.Column;

/**
 * 默认类型映射器的纯单元用例：不连库、不启 Spring
 * <p>
 * Pure unit cases for the default type mapper: no database, no Spring context
 */
public class TypeMapperTest {

    private static final TypeMapper MAPPER = DatabaseChecker4MySql.defaultTypeMapper;

    /** 默认映射器不读取 Field，故这里统一传 null */
    private static String map(Class<?> javaType) {
        return MAPPER.javaTypeToSqlType(javaType, 0, null);
    }

    private static String map(Class<?> javaType, int scale) {
        return MAPPER.javaTypeToSqlType(javaType, scale, null);
    }

    public static enum Colors {
        RED, GREEN, BLUE
    }

    @Test
    public void primitives() {
        assertThat(map(long.class)).as("long 应映射为 bigint").isEqualTo("bigint");
        assertThat(map(int.class)).as("int 应映射为 int").isEqualTo("int");
        assertThat(map(short.class)).as("short 应映射为 smallint").isEqualTo("smallint");
        assertThat(map(byte.class)).as("byte 应映射为 tinyint").isEqualTo("tinyint");
        assertThat(map(double.class)).as("double 应映射为 double").isEqualTo("double");
        assertThat(map(float.class)).as("float 应映射为 float").isEqualTo("float");
        assertThat(map(boolean.class)).as("boolean 应映射为 tinyint(1)").isEqualTo("tinyint(1)");
        assertThat(map(char.class)).as("char 应映射为 char(1)").isEqualTo("char(1)");
    }

    @Test
    public void boxedTypes() {
        assertThat(map(Long.class)).as("Long 应映射为 bigint").isEqualTo("bigint");
        assertThat(map(Integer.class)).as("Integer 应映射为 int").isEqualTo("int");
        assertThat(map(Short.class)).as("Short 应映射为 smallint").isEqualTo("smallint");
        assertThat(map(Byte.class)).as("Byte 应映射为 tinyint").isEqualTo("tinyint");
        assertThat(map(Double.class)).as("Double 应映射为 double").isEqualTo("double");
        assertThat(map(Float.class)).as("Float 应映射为 float").isEqualTo("float");
        assertThat(map(Boolean.class)).as("Boolean 应映射为 tinyint(1)").isEqualTo("tinyint(1)");
        assertThat(map(Character.class)).as("Character 应映射为 char(1)").isEqualTo("char(1)");
    }

    @Test
    public void numbers() {
        assertThat(map(BigInteger.class)).as("BigInteger 应映射为 decimal(64,0)").isEqualTo("decimal(64,0)");
        assertThat(map(BigDecimal.class)).as("BigDecimal 默认 scale 应映射为 decimal(32,8)").isEqualTo("decimal(32,8)");
    }

    @Test
    public void dateAndTime() {
        assertThat(map(Date.class)).as("java.util.Date 应映射为 datetime").isEqualTo("datetime");
        assertThat(map(Instant.class)).as("Instant 应映射为 timestamp").isEqualTo("timestamp");
        assertThat(map(LocalDate.class)).as("LocalDate 应映射为 date").isEqualTo("date");
        assertThat(map(LocalTime.class)).as("LocalTime 应映射为 time").isEqualTo("time");
        assertThat(map(LocalDateTime.class)).as("LocalDateTime 应映射为 datetime").isEqualTo("datetime");
        assertThat(map(ZonedDateTime.class)).as("ZonedDateTime 应映射为 timestamp").isEqualTo("timestamp");
        assertThat(map(OffsetDateTime.class)).as("OffsetDateTime 应映射为 timestamp").isEqualTo("timestamp");
        assertThat(map(OffsetTime.class)).as("OffsetTime 应映射为 time").isEqualTo("time");
        assertThat(map(java.sql.Date.class)).as("java.sql.Date 是 java.util.Date 的子类，应先命中 datetime").isEqualTo("datetime");
    }

    @Test
    public void stringBinaryAndEnum() {
        assertThat(map(String.class)).as("String 默认 scale 应映射为 varchar(255)").isEqualTo("varchar(255)");
        assertThat(map(byte[].class)).as("byte[] 应映射为 blob").isEqualTo("blob");
        assertThat(map(Colors.class)).as("枚举应映射为 varchar(255)").isEqualTo("varchar(255)");
    }

    @Test
    public void unsupportedTypes() {
        assertThat(map(Object.class)).as("Object 无法映射，应返回 null").isNull();
        assertThat(map(List.class)).as("List 无法映射，应返回 null").isNull();
        assertThat(map(UUID.class)).as("UUID 无法映射，应返回 null").isNull();
        assertThat(map(int[].class)).as("int[] 无法映射，应返回 null").isNull();
        assertThat(map(String[].class)).as("String[] 无法映射，应返回 null").isNull();
    }

    @Test
    public void stringScale() {
        assertThat(map(String.class, Column.PresetScale.SMALL)).as("String + SMALL 应映射为 varchar(255)").isEqualTo("varchar(255)");
        assertThat(map(String.class, Column.PresetScale.MEDIUM)).as("String + MEDIUM 应映射为 text").isEqualTo("text");
        assertThat(map(String.class, Column.PresetScale.LARGE)).as("String + LARGE 应映射为 longtext").isEqualTo("longtext");
        assertThat(map(String.class, 0)).as("String + scale=0 应回落到 varchar(255)").isEqualTo("varchar(255)");
        assertThat(map(String.class, -3)).as("String + 负数 scale 应回落到 varchar(255)").isEqualTo("varchar(255)");
        assertThat(map(String.class, 9999)).as("String + 超出预设档位的 scale 应回落到 varchar(255)").isEqualTo("varchar(255)");
    }

    @Test
    public void bigDecimalScale() {
        assertThat(map(BigDecimal.class, Column.PresetScale.SMALL)).as("BigDecimal + SMALL 应映射为 decimal(16,4)").isEqualTo("decimal(16,4)");
        assertThat(map(BigDecimal.class, Column.PresetScale.MEDIUM)).as("BigDecimal + MEDIUM 应映射为 decimal(32,8)").isEqualTo("decimal(32,8)");
        assertThat(map(BigDecimal.class, Column.PresetScale.LARGE)).as("BigDecimal + LARGE 应映射为 decimal(64,16)").isEqualTo("decimal(64,16)");
        assertThat(map(BigDecimal.class, 0)).as("BigDecimal + scale=0 应回落到 decimal(32,8)").isEqualTo("decimal(32,8)");
        assertThat(map(BigDecimal.class, -3)).as("BigDecimal + 负数 scale 应回落到 decimal(32,8)").isEqualTo("decimal(32,8)");
        assertThat(map(BigDecimal.class, 9999)).as("BigDecimal + 超出预设档位的 scale 应回落到 decimal(32,8)").isEqualTo("decimal(32,8)");
    }

    @Test
    public void scaleOnlyAffectsStringAndBigDecimal() {
        assertThat(map(Long.class, Column.PresetScale.LARGE)).as("scale 不应影响 Long 的映射结果").isEqualTo("bigint");
        assertThat(map(LocalDateTime.class, Column.PresetScale.LARGE)).as("scale 不应影响 LocalDateTime 的映射结果").isEqualTo("datetime");
        assertThat(map(BigInteger.class, Column.PresetScale.LARGE)).as("scale 不应影响 BigInteger 的映射结果").isEqualTo("decimal(64,0)");
        assertThat(map(Colors.class, Column.PresetScale.LARGE)).as("scale 不应影响枚举的映射结果").isEqualTo("varchar(255)");
    }
}
