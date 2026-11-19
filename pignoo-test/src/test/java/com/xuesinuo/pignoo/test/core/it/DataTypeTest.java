package com.xuesinuo.pignoo.test.core.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.xuesinuo.pignoo.core.exception.PignooRuntimeException;
import com.xuesinuo.pignoo.core.implement.BasePignoo;
import com.xuesinuo.pignoo.test.fixture.AllTypes;

/**
 * 类型往返集成测试：真实MySQL上验证SimpleJdbcSqlExecuter的参数绑定与结果读取
 * <p>
 * Data type round-trip integration test against a real MySQL
 */
@SpringBootTest
public class DataTypeTest {

    public final DataSource dataSource;

    public DataTypeTest(@Autowired DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** 清空本类使用的夹具表 */
    private void clearTable() {
        try (var pignoo = new BasePignoo(this.dataSource)) {
            pignoo.writer(AllTypes.class).removeAll();
        }
    }

    /** 绕开Pignoo，直接用JDBC读一列的字符串值 */
    private String readColumnAsString(String column, Long id) throws Exception {
        try (Connection conn = this.dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement("SELECT `" + column + "` FROM `fx_all_types` WHERE `id`=?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    /** 绕开Pignoo，直接用JDBC写一列的字符串值 */
    private void writeColumnAsString(String column, Long id, String value) throws Exception {
        try (Connection conn = this.dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement("UPDATE `fx_all_types` SET `" + column + "`=? WHERE `id`=?")) {
            ps.setString(1, value);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    /** 八原生类型给一组哨兵值，避免出现'\0'之类不便断言的默认值 */
    private void fillPrimitives(AllTypes object) {
        object.setNbyte((byte) 12);
        object.setNshort((short) 13);
        object.setNinteger(14);
        object.setNlong(15L);
        object.setNfloat(16.5f);
        object.setNdouble(17.25);
        object.setNchar('P');
        object.setNboolean(true);
    }

    @Test
    public void primitiveAndWrapperRoundTrip() {
        this.clearTable();
        try (var pignoo = new BasePignoo(this.dataSource)) {
            AllTypes object = new AllTypes();
            this.fillPrimitives(object);
            object.setXbyte((byte) 22);
            object.setXshort((short) 23);
            object.setXinteger(24);
            object.setXlong(25L);
            object.setXfloat(26.5f);
            object.setXdouble(27.25);
            object.setXchar('N');
            object.setXboolean(false);

            Long id = pignoo.writer(AllTypes.class).add(object).getId();
            AllTypes back = pignoo.reader(AllTypes.class).filter(AllTypes::getId, "==", id).getAny();

            assertThat(back).as("按主键应当能查回刚写入的行").isNotNull();
            assertThat(back.getNbyte()).as("byte往返应当一致").isEqualTo((byte) 12);
            assertThat(back.getNshort()).as("short往返应当一致").isEqualTo((short) 13);
            assertThat(back.getNinteger()).as("int往返应当一致").isEqualTo(14);
            assertThat(back.getNlong()).as("long往返应当一致").isEqualTo(15L);
            assertThat(back.getNfloat()).as("float往返应当一致").isEqualTo(16.5f);
            assertThat(back.getNdouble()).as("double往返应当一致").isEqualTo(17.25);
            assertThat(back.getNchar()).as("char往返应当一致").isEqualTo('P');
            assertThat(back.isNboolean()).as("boolean往返应当一致").isTrue();

            assertThat(back.getXbyte()).as("Byte往返应当一致").isEqualTo((byte) 22);
            assertThat(back.getXshort()).as("Short往返应当一致").isEqualTo((short) 23);
            assertThat(back.getXinteger()).as("Integer往返应当一致").isEqualTo(24);
            assertThat(back.getXlong()).as("Long往返应当一致").isEqualTo(25L);
            assertThat(back.getXfloat()).as("Float往返应当一致").isEqualTo(26.5f);
            assertThat(back.getXdouble()).as("Double往返应当一致").isEqualTo(27.25);
            assertThat(back.getXchar()).as("Character往返应当一致").isEqualTo('N');
            assertThat(back.getXboolean()).as("Boolean往返应当一致").isFalse();
        }
    }

    @Test
    public void textNumberAndBinaryRoundTrip() {
        this.clearTable();
        try (var pignoo = new BasePignoo(this.dataSource)) {
            AllTypes object = new AllTypes();
            this.fillPrimitives(object);
            object.setString("Pignoo-字符串");
            object.setBigInteger(BigInteger.valueOf(9007199254740993L));
            object.setBigDecimal(new BigDecimal("33.200000000000000000000000000001"));
            object.setBytes(new byte[] { 1, 2, 3, -4, -5, 0, 127 });
            object.setEnums(AllTypes.Enums.B);

            Long id = pignoo.writer(AllTypes.class).add(object).getId();
            AllTypes back = pignoo.reader(AllTypes.class).filter(AllTypes::getId, "==", id).getAny();

            assertThat(back.getString()).as("String往返应当一致").isEqualTo("Pignoo-字符串");
            assertThat(back.getBigInteger()).as("BigInteger往返应当一致").isEqualTo(BigInteger.valueOf(9007199254740993L));
            assertThat(back.getBigDecimal()).as("BigDecimal往返应当数值相等").isEqualByComparingTo(new BigDecimal("33.200000000000000000000000000001"));
            assertThat(back.getBytes()).as("byte[]往返应当逐字节一致").isEqualTo(new byte[] { 1, 2, 3, -4, -5, 0, 127 });
            assertThat(back.getEnums()).as("枚举往返应当一致").isEqualTo(AllTypes.Enums.B);
        }
    }

    @Test
    public void dateAndTimeRoundTrip() {
        this.clearTable();
        try (var pignoo = new BasePignoo(this.dataSource)) {
            // 建表脚本里的时间列都没有小数秒，写入前先截断到秒
            Date date = new Date(System.currentTimeMillis() / 1000L * 1000L);
            LocalDate localDate = LocalDate.now();
            LocalTime localTime = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
            LocalDateTime localDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
            Instant instant = Instant.now().truncatedTo(ChronoUnit.SECONDS);
            ZonedDateTime zonedDateTime = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);
            OffsetTime offsetTime = OffsetTime.now().truncatedTo(ChronoUnit.SECONDS);

            AllTypes object = new AllTypes();
            this.fillPrimitives(object);
            object.setDate(date);
            object.setLocalDate(localDate);
            object.setLocalTime(localTime);
            object.setLocalDateTime(localDateTime);
            object.setInstant(instant);
            object.setZonedDateTime(zonedDateTime);
            object.setOffsetTime(offsetTime);

            Long id = pignoo.writer(AllTypes.class).add(object).getId();
            AllTypes back = pignoo.reader(AllTypes.class).filter(AllTypes::getId, "==", id).getAny();

            assertThat(back.getDate()).as("java.util.Date不应为空").isNotNull();
            assertThat(back.getDate().getTime()).as("java.util.Date往返应当毫秒一致").isEqualTo(date.getTime());
            assertThat(back.getLocalDate()).as("LocalDate往返应当一致").isEqualTo(localDate);
            assertThat(back.getLocalTime()).as("LocalTime往返应当一致").isEqualTo(localTime);
            assertThat(back.getLocalDateTime()).as("LocalDateTime往返应当一致").isEqualTo(localDateTime);
            assertThat(back.getInstant()).as("Instant往返应当一致").isEqualTo(instant);
            assertThat(back.getZonedDateTime()).as("ZonedDateTime不应为空").isNotNull();
            assertThat(back.getZonedDateTime().compareTo(zonedDateTime)).as("ZonedDateTime往返应当一致").isZero();
            assertThat(back.getOffsetTime()).as("OffsetTime不应为空").isNotNull();
            assertThat(back.getOffsetTime().compareTo(offsetTime)).as("OffsetTime往返应当一致").isZero();
        }
    }

    @Test
    public void insertWithoutOptionalColumnsKeepsNull() {
        this.clearTable();
        try (var pignoo = new BasePignoo(this.dataSource)) {
            AllTypes object = new AllTypes();
            this.fillPrimitives(object); // 原生类型不可能为null，只能给值

            Long id = pignoo.writer(AllTypes.class).add(object).getId();
            AllTypes back = pignoo.reader(AllTypes.class).filter(AllTypes::getId, "==", id).getAny();

            assertThat(back).as("按主键应当能查回刚写入的行").isNotNull();
            assertThat(back.getXbyte()).as("未写入的Byte列应当读回null").isNull();
            assertThat(back.getXshort()).as("未写入的Short列应当读回null").isNull();
            assertThat(back.getXinteger()).as("未写入的Integer列应当读回null").isNull();
            assertThat(back.getXlong()).as("未写入的Long列应当读回null").isNull();
            assertThat(back.getXfloat()).as("未写入的Float列应当读回null").isNull();
            assertThat(back.getXdouble()).as("未写入的Double列应当读回null").isNull();
            assertThat(back.getXchar()).as("未写入的Character列应当读回null").isNull();
            assertThat(back.getXboolean()).as("未写入的Boolean列应当读回null").isNull();
            assertThat(back.getString()).as("未写入的String列应当读回null").isNull();
            assertThat(back.getBigInteger()).as("未写入的BigInteger列应当读回null").isNull();
            assertThat(back.getBigDecimal()).as("未写入的BigDecimal列应当读回null").isNull();
            assertThat(back.getDate()).as("未写入的Date列应当读回null").isNull();
            assertThat(back.getLocalDate()).as("未写入的LocalDate列应当读回null").isNull();
            assertThat(back.getLocalTime()).as("未写入的LocalTime列应当读回null").isNull();
            assertThat(back.getLocalDateTime()).as("未写入的LocalDateTime列应当读回null").isNull();
            assertThat(back.getInstant()).as("未写入的Instant列应当读回null").isNull();
            assertThat(back.getZonedDateTime()).as("未写入的ZonedDateTime列应当读回null").isNull();
            assertThat(back.getOffsetTime()).as("未写入的OffsetTime列应当读回null").isNull();
            assertThat(back.getBytes()).as("未写入的byte[]列应当读回null").isNull();
            assertThat(back.getEnums()).as("未写入的枚举列应当读回null").isNull();

            assertThat(back.getNbyte()).as("原生类型不参与null，应当读回写入值").isEqualTo((byte) 12);
            assertThat(back.getNchar()).as("原生类型不参与null，应当读回写入值").isEqualTo('P');
            assertThat(back.isNboolean()).as("原生类型不参与null，应当读回写入值").isTrue();
        }
    }

    @Test
    public void replaceByIdWritesNullExplicitly() {
        this.clearTable();
        try (var pignoo = new BasePignoo(this.dataSource)) {
            AllTypes object = new AllTypes();
            this.fillPrimitives(object);
            object.setXinteger(24);
            object.setString("先写一个非null值");
            object.setBigDecimal(new BigDecimal("1.5"));
            object.setBytes(new byte[] { 9 });
            object.setEnums(AllTypes.Enums.C);
            object.setLocalDate(LocalDate.now());

            var writer = pignoo.writer(AllTypes.class);
            Long id = writer.add(object).getId();

            AllTypes blank = new AllTypes();
            blank.setId(id);
            this.fillPrimitives(blank); // 其余可空列全部保持null
            long rows = writer.replaceById(blank);
            assertThat(rows).as("replaceById应当命中1行").isEqualTo(1L);

            AllTypes back = pignoo.reader(AllTypes.class).filter(AllTypes::getId, "==", id).getAny();
            assertThat(back.getXinteger()).as("显式写入null后Integer列应当读回null").isNull();
            assertThat(back.getString()).as("显式写入null后String列应当读回null").isNull();
            assertThat(back.getBigDecimal()).as("显式写入null后BigDecimal列应当读回null").isNull();
            assertThat(back.getBytes()).as("显式写入null后byte[]列应当读回null").isNull();
            assertThat(back.getEnums()).as("显式写入null后枚举列应当读回null").isNull();
            assertThat(back.getLocalDate()).as("显式写入null后LocalDate列应当读回null").isNull();
        }
    }

    @Test
    public void enumIsStoredByName() throws Exception {
        this.clearTable();
        try (var pignoo = new BasePignoo(this.dataSource)) {
            AllTypes object = new AllTypes();
            this.fillPrimitives(object);
            object.setEnums(AllTypes.Enums.C);
            Long id = pignoo.writer(AllTypes.class).add(object).getId();

            assertThat(this.readColumnAsString("enums", id)).as("枚举应当按名字存成字符串").isEqualTo("C");

            AllTypes back = pignoo.reader(AllTypes.class).filter(AllTypes::getId, "==", id).getAny();
            assertThat(back.getEnums()).as("枚举应当按名字还原").isEqualTo(AllTypes.Enums.C);
        }
    }

    @Test
    public void illegalEnumNameInDatabaseThrows() throws Exception {
        this.clearTable();
        Long id;
        try (var pignoo = new BasePignoo(this.dataSource)) {
            AllTypes object = new AllTypes();
            this.fillPrimitives(object);
            object.setEnums(AllTypes.Enums.A);
            id = pignoo.writer(AllTypes.class).add(object).getId();
        }
        this.writeColumnAsString("enums", id, "X"); // 数据库里出现了枚举中不存在的名字
        try (var pignoo = new BasePignoo(this.dataSource)) {
            var reader = pignoo.reader(AllTypes.class).filter(AllTypes::getId, "==", id);
            assertThatThrownBy(() -> reader.getAny())
                    .as("数据库里存了非法枚举名时，读取应当抛异常而不是静默降级")
                    .isInstanceOf(PignooRuntimeException.class)
                    .hasRootCauseInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    public void charIsBoundAsStringAndReadBack() throws Exception {
        this.clearTable();
        try (var pignoo = new BasePignoo(this.dataSource)) {
            var writer = pignoo.writer(AllTypes.class);
            var reader = pignoo.reader(AllTypes.class);
            char[] samples = { 'P', 'z', '9', '_' };
            for (char sample : samples) {
                AllTypes object = new AllTypes();
                this.fillPrimitives(object);
                object.setNchar(sample);
                object.setXchar(sample);
                Long id = writer.add(object).getId();

                assertThat(this.readColumnAsString("nchar", id)).as("char会被绑定成长度为1的字符串写入").isEqualTo(String.valueOf(sample));
                assertThat(this.readColumnAsString("xchar", id)).as("Character会被绑定成长度为1的字符串写入").isEqualTo(String.valueOf(sample));

                AllTypes back = reader.copyReader().filter(AllTypes::getId, "==", id).getAny();
                assertThat(back.getNchar()).as("char应当从字符串取首字符还原").isEqualTo(sample);
                assertThat(back.getXchar()).as("Character应当从字符串取首字符还原").isEqualTo(sample);
            }
        }
    }

    @Test
    public void numericBoundaryValues() {
        this.clearTable();
        try (var pignoo = new BasePignoo(this.dataSource)) {
            var writer = pignoo.writer(AllTypes.class);

            AllTypes min = new AllTypes();
            this.fillPrimitives(min);
            min.setNbyte(Byte.MIN_VALUE);
            min.setNshort(Short.MIN_VALUE);
            min.setNinteger(Integer.MIN_VALUE);
            min.setNlong(Long.MIN_VALUE);
            min.setXbyte(Byte.MIN_VALUE);
            min.setXshort(Short.MIN_VALUE);
            min.setXinteger(Integer.MIN_VALUE);
            min.setXlong(Long.MIN_VALUE);
            min.setBigInteger(BigInteger.valueOf(Long.MIN_VALUE));
            min.setBigDecimal(new BigDecimal("-12345678901234567890.123456789012345678901234567890"));
            Long minId = writer.add(min).getId();

            AllTypes max = new AllTypes();
            this.fillPrimitives(max);
            max.setNbyte(Byte.MAX_VALUE);
            max.setNshort(Short.MAX_VALUE);
            max.setNinteger(Integer.MAX_VALUE);
            max.setNlong(Long.MAX_VALUE);
            max.setXbyte(Byte.MAX_VALUE);
            max.setXshort(Short.MAX_VALUE);
            max.setXinteger(Integer.MAX_VALUE);
            max.setXlong(Long.MAX_VALUE);
            max.setBigInteger(BigInteger.valueOf(Long.MAX_VALUE));
            max.setBigDecimal(new BigDecimal("12345678901234567890.123456789012345678901234567890"));
            Long maxId = writer.add(max).getId();

            var reader = pignoo.reader(AllTypes.class);
            AllTypes minBack = reader.copyReader().filter(AllTypes::getId, "==", minId).getAny();
            AllTypes maxBack = reader.copyReader().filter(AllTypes::getId, "==", maxId).getAny();

            assertThat(minBack.getNbyte()).as("byte最小值往返应当一致").isEqualTo(Byte.MIN_VALUE);
            assertThat(minBack.getNshort()).as("short最小值往返应当一致").isEqualTo(Short.MIN_VALUE);
            assertThat(minBack.getNinteger()).as("int最小值往返应当一致").isEqualTo(Integer.MIN_VALUE);
            assertThat(minBack.getNlong()).as("long最小值往返应当一致").isEqualTo(Long.MIN_VALUE);
            assertThat(minBack.getXbyte()).as("Byte最小值往返应当一致").isEqualTo(Byte.MIN_VALUE);
            assertThat(minBack.getXshort()).as("Short最小值往返应当一致").isEqualTo(Short.MIN_VALUE);
            assertThat(minBack.getXinteger()).as("Integer最小值往返应当一致").isEqualTo(Integer.MIN_VALUE);
            assertThat(minBack.getXlong()).as("Long最小值往返应当一致").isEqualTo(Long.MIN_VALUE);
            assertThat(minBack.getBigInteger()).as("BigInteger最小值往返应当一致").isEqualTo(BigInteger.valueOf(Long.MIN_VALUE));
            assertThat(minBack.getBigDecimal()).as("BigDecimal负向大数往返应当数值相等")
                    .isEqualByComparingTo(new BigDecimal("-12345678901234567890.123456789012345678901234567890"));

            assertThat(maxBack.getNbyte()).as("byte最大值往返应当一致").isEqualTo(Byte.MAX_VALUE);
            assertThat(maxBack.getNshort()).as("short最大值往返应当一致").isEqualTo(Short.MAX_VALUE);
            assertThat(maxBack.getNinteger()).as("int最大值往返应当一致").isEqualTo(Integer.MAX_VALUE);
            assertThat(maxBack.getNlong()).as("long最大值往返应当一致").isEqualTo(Long.MAX_VALUE);
            assertThat(maxBack.getXbyte()).as("Byte最大值往返应当一致").isEqualTo(Byte.MAX_VALUE);
            assertThat(maxBack.getXshort()).as("Short最大值往返应当一致").isEqualTo(Short.MAX_VALUE);
            assertThat(maxBack.getXinteger()).as("Integer最大值往返应当一致").isEqualTo(Integer.MAX_VALUE);
            assertThat(maxBack.getXlong()).as("Long最大值往返应当一致").isEqualTo(Long.MAX_VALUE);
            assertThat(maxBack.getBigInteger()).as("BigInteger最大值往返应当一致").isEqualTo(BigInteger.valueOf(Long.MAX_VALUE));
            assertThat(maxBack.getBigDecimal()).as("BigDecimal正向大数往返应当数值相等")
                    .isEqualByComparingTo(new BigDecimal("12345678901234567890.123456789012345678901234567890"));
        }
    }

    @Test
    public void emptyStringAndEmptyBytes() {
        this.clearTable();
        try (var pignoo = new BasePignoo(this.dataSource)) {
            AllTypes object = new AllTypes();
            this.fillPrimitives(object);
            object.setString("");
            object.setBytes(new byte[0]);

            Long id = pignoo.writer(AllTypes.class).add(object).getId();
            AllTypes back = pignoo.reader(AllTypes.class).filter(AllTypes::getId, "==", id).getAny();

            assertThat(back.getString()).as("空字符串应当读回空字符串而不是null").isEmpty();
            assertThat(back.getBytes()).as("空数组应当读回空数组而不是null").isNotNull();
            assertThat(back.getBytes()).as("空数组应当读回空数组").isEmpty();
        }
    }
}
