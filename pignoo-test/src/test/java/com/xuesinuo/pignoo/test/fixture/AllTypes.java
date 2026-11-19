package com.xuesinuo.pignoo.test.fixture;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.Date;

import com.xuesinuo.pignoo.core.annotation.Table;

import lombok.Data;

/**
 * 覆盖SimpleJdbcSqlExecuter支持的全部类型
 */
@Table("fx_all_types")
@Data
public class AllTypes {

    public static enum Enums {
        A, B, C
    }

    private Long id;

    private byte nbyte;
    private short nshort;
    private int ninteger;
    private long nlong;
    private float nfloat;
    private double ndouble;
    private char nchar;
    private boolean nboolean;

    private Byte xbyte;
    private Short xshort;
    private Integer xinteger;
    private Long xlong;
    private Float xfloat;
    private Double xdouble;
    private Character xchar;
    private Boolean xboolean;

    private String string;
    private BigInteger bigInteger;
    private BigDecimal bigDecimal;

    private Date date;
    private LocalDate localDate;
    private LocalTime localTime;
    private LocalDateTime localDateTime;
    private Instant instant;
    private ZonedDateTime zonedDateTime;
    private OffsetTime offsetTime;

    private byte[] bytes;
    private Enums enums;
}
