package com.xuesinuo.pignoo.test.autodatabase.test03entity.addtable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.Date;

import com.xuesinuo.pignoo.core.annotation.Column;
import com.xuesinuo.pignoo.test.autodatabase.Test03_Mapper.Enums;

import lombok.Data;

@Data
public class AutodatabaseTest03 {
    private Long id;
    @Column
    private String str1;
    @Column(scale = -3)
    private String str2;
    @Column(scale = 10)
    private String str3;
    @Column(scale = 9999)
    private String str4;
    @Column(scale = 255)
    private String strNumber;

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
