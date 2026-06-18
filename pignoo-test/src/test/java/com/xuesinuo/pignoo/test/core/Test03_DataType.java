package com.xuesinuo.pignoo.test.core;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.xuesinuo.pignoo.core.implement.BasePignoo;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
public class Test03_DataType {

    public final DataSource dataSource;

    public Test03_DataType(@Autowired DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public static enum Enums {
        A, B, C
    }

    @Data
    public static class Test03DataType {
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

    @Test
    public void dataType() {
        try (var pignoo = new BasePignoo(this.dataSource)) {
            Test03DataType object = new Test03DataType();
            object.setNbyte((byte) 12);
            object.setNshort((short) 13);
            object.setNinteger(14);
            object.setNlong(15L);
            object.setNfloat(16.0f);
            object.setNdouble(17.0);
            object.setNchar('P');
            object.setNboolean(true);

            object.setXbyte((byte) 22);
            object.setXshort((short) 23);
            object.setXinteger(24);
            object.setXlong(25L);
            object.setXfloat(26.0f);
            object.setXdouble(27.0);
            object.setXchar('N');
            object.setXboolean(false);

            object.setString("Pignoo");
            object.setBigInteger(BigInteger.valueOf(32));
            object.setBigDecimal(new BigDecimal("33.2"));

            object.setDate(new Date(System.currentTimeMillis() / 1000L * 1000L));
            object.setLocalDate(LocalDate.now());
            object.setLocalTime(LocalTime.now().truncatedTo(ChronoUnit.SECONDS));
            object.setLocalDateTime(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
            object.setInstant(Instant.now().truncatedTo(ChronoUnit.SECONDS));
            object.setZonedDateTime(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS));
            object.setOffsetTime(OffsetTime.now().truncatedTo(ChronoUnit.SECONDS));

            object.setBytes(new byte[] { 1, 2, 3, 4, 5 });
            object.setEnums(Enums.B);

            var writer = pignoo.writer(Test03DataType.class);
            writer.removeAll();

            Test03DataType objectInDatabase = writer.add(object);
            if (objectInDatabase.getId() == null
                    || objectInDatabase.getXbyte() == null
                    || objectInDatabase.getXshort() == null
                    || objectInDatabase.getXinteger() == null
                    || objectInDatabase.getXlong() == null
                    || objectInDatabase.getXfloat() == null
                    || objectInDatabase.getXdouble() == null
                    || objectInDatabase.getXchar() == null
                    || objectInDatabase.getXboolean() == null
                    || objectInDatabase.getString() == null
                    || objectInDatabase.getBigInteger() == null
                    || objectInDatabase.getBigDecimal() == null
                    || objectInDatabase.getDate() == null
                    || objectInDatabase.getLocalDate() == null
                    || objectInDatabase.getLocalTime() == null
                    || objectInDatabase.getLocalDateTime() == null
                    || objectInDatabase.getInstant() == null
                    || objectInDatabase.getZonedDateTime() == null
                    || objectInDatabase.getOffsetTime() == null
                    || objectInDatabase.getBytes() == null
                    || objectInDatabase.getEnums() == null) {
                throw new RuntimeException("DataType error 1");
            }
            if (object.getNbyte() != objectInDatabase.getNbyte()
                    || object.getNshort() != objectInDatabase.getNshort()
                    || object.getNinteger() != objectInDatabase.getNinteger()
                    || object.getNlong() != objectInDatabase.getNlong()
                    || object.getNfloat() != objectInDatabase.getNfloat()
                    || object.getNdouble() != objectInDatabase.getNdouble()
                    || object.getNchar() != objectInDatabase.getNchar()
                    || object.isNboolean() != objectInDatabase.isNboolean()
                    || !object.getXbyte().equals(objectInDatabase.getXbyte())
                    || !object.getXshort().equals(objectInDatabase.getXshort())
                    || !object.getXinteger().equals(objectInDatabase.getXinteger())
                    || !object.getXlong().equals(objectInDatabase.getXlong())
                    || !object.getXfloat().equals(objectInDatabase.getXfloat())
                    || !object.getXdouble().equals(objectInDatabase.getXdouble())
                    || !object.getXchar().equals(objectInDatabase.getXchar())
                    || !object.getXboolean().equals(objectInDatabase.getXboolean())
                    || !object.getString().equals(objectInDatabase.getString())
                    || object.getBigInteger().compareTo(objectInDatabase.getBigInteger()) != 0
                    || object.getBigDecimal().compareTo(objectInDatabase.getBigDecimal()) != 0
                    || object.getDate().compareTo(objectInDatabase.getDate()) != 0
                    || object.getLocalDate().compareTo(objectInDatabase.getLocalDate()) != 0
                    || object.getLocalTime().compareTo(objectInDatabase.getLocalTime()) != 0
                    || object.getLocalDateTime().compareTo(objectInDatabase.getLocalDateTime()) != 0
                    || object.getInstant().compareTo(objectInDatabase.getInstant()) != 0
                    || object.getZonedDateTime().compareTo(objectInDatabase.getZonedDateTime()) != 0
                    || object.getOffsetTime().compareTo(objectInDatabase.getOffsetTime()) != 0
                    || !Arrays.equals(object.getBytes(), objectInDatabase.getBytes())
                    || object.getEnums().compareTo(objectInDatabase.getEnums()) != 0) {
                throw new RuntimeException("DataType error 2");
            }
        }
    }

    @Data
    public static class Test03Setter1 {
        private Long id;
        private boolean admin;
        private boolean isadmin;
        private boolean is_admin;
        private boolean is;
    }

    @Data
    public static class Test03Setter2 {
        private Long id;
        private boolean isAdmin;
    }

    @Test
    public void setter() {
        try (var pignoo = new BasePignoo(this.dataSource)) {
            var writer = pignoo.writer(Test03Setter1.class);
            var reader = pignoo.reader(Test03Setter1.class);
            writer.removeAll();
            var object = new Test03Setter1();
            object = writer.add(object);
            object.setAdmin(true);
            object.setIsadmin(true);
            object.set_admin(true);
            object.setIs(true);
            object = reader.getAny();
            if (object == null || object.isAdmin() != true || object.isIsadmin() != true || object.is_admin() != true || object.isIs() != true) {
                throw new RuntimeException("Setter error 1");
            }
        }
        try (var pignoo = new BasePignoo(this.dataSource)) {
            var writer = pignoo.writer(Test03Setter2.class);
            var reader = pignoo.reader(Test03Setter2.class);
            writer.removeAll();
            var object = new Test03Setter2();
            object = writer.add(object);
            object.setAdmin(true);
            object = reader.getAny();
            if (object == null || object.isAdmin() != true) {
                throw new RuntimeException("Setter error 1");
            }
        }
    }
}
