package com.xuesinuo.pignoo.test.core.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.xuesinuo.pignoo.core.implement.BasePignoo;
import com.xuesinuo.pignoo.test.fixture.BoolNaming;

/**
 * boolean字段的is前缀命名规则集成测试：在真实往返中钉住getter/setter与列名的对应关系
 * <p>
 * Integration test for the boolean "is" prefix naming rules
 */
@SpringBootTest
public class BoolNamingTest {

    public final DataSource dataSource;

    public BoolNamingTest(@Autowired DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** 清空本类使用的夹具表 */
    private void clearTable() {
        try (var pignoo = new BasePignoo(this.dataSource)) {
            pignoo.writer(BoolNaming.class).removeAll();
        }
    }

    /** 绕开Pignoo，直接用JDBC读一列的布尔值，用来钉住列名映射 */
    private boolean readColumn(String column, Long id) throws Exception {
        try (Connection conn = this.dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement("SELECT `" + column + "` FROM `fx_bool` WHERE `id`=?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).as("按主键应当能查到fx_bool中的行").isTrue();
                return rs.getBoolean(1);
            }
        }
    }

    @Test
    public void allTrueRoundTrip() {
        this.clearTable();
        try (var pignoo = new BasePignoo(this.dataSource)) {
            BoolNaming object = new BoolNaming();
            object.setAdmin(true);
            object.setActive(true);
            object.setIsadmin(true);
            object.set_admin(true);
            object.setIs(true);

            Long id = pignoo.writer(BoolNaming.class).add(object).getId();
            BoolNaming back = pignoo.reader(BoolNaming.class).filter(BoolNaming::getId, "==", id).getAny();

            assertThat(back).as("按主键应当能查回刚写入的行").isNotNull();
            assertThat(back.isAdmin()).as("admin字段写true应当读回true").isTrue();
            assertThat(back.isActive()).as("isActive字段写true应当读回true").isTrue();
            assertThat(back.isIsadmin()).as("isadmin字段写true应当读回true").isTrue();
            assertThat(back.is_admin()).as("is_admin字段写true应当读回true").isTrue();
            assertThat(back.isIs()).as("is字段写true应当读回true").isTrue();
        }
    }

    @Test
    public void allFalseRoundTrip() {
        this.clearTable();
        try (var pignoo = new BasePignoo(this.dataSource)) {
            BoolNaming object = new BoolNaming(); // 五个boolean都保持默认的false

            Long id = pignoo.writer(BoolNaming.class).add(object).getId();
            BoolNaming back = pignoo.reader(BoolNaming.class).filter(BoolNaming::getId, "==", id).getAny();

            assertThat(back).as("按主键应当能查回刚写入的行").isNotNull();
            assertThat(back.isAdmin()).as("admin字段写false应当读回false").isFalse();
            assertThat(back.isActive()).as("isActive字段写false应当读回false").isFalse();
            assertThat(back.isIsadmin()).as("isadmin字段写false应当读回false").isFalse();
            assertThat(back.is_admin()).as("is_admin字段写false应当读回false").isFalse();
            assertThat(back.isIs()).as("is字段写false应当读回false").isFalse();
        }
    }

    @Test
    public void eachFieldMapsToItsOwnColumn() throws Exception {
        this.clearTable();
        Long id;
        try (var pignoo = new BasePignoo(this.dataSource)) {
            BoolNaming object = new BoolNaming();
            object.setAdmin(true);
            object.setActive(false);
            object.setIsadmin(true);
            object.set_admin(false);
            object.setIs(true);

            id = pignoo.writer(BoolNaming.class).add(object).getId();
            BoolNaming back = pignoo.reader(BoolNaming.class).filter(BoolNaming::getId, "==", id).getAny();

            assertThat(back.isAdmin()).as("admin字段应当独立往返").isTrue();
            assertThat(back.isActive()).as("isActive字段应当独立往返").isFalse();
            assertThat(back.isIsadmin()).as("isadmin字段应当独立往返").isTrue();
            assertThat(back.is_admin()).as("is_admin字段应当独立往返").isFalse();
            assertThat(back.isIs()).as("is字段应当独立往返").isTrue();
        }
        assertThat(this.readColumn("admin", id)).as("admin字段应当映射到admin列").isTrue();
        assertThat(this.readColumn("is_active", id)).as("isActive字段应当映射到is_active列").isFalse();
        assertThat(this.readColumn("isadmin", id)).as("isadmin字段应当映射到isadmin列").isTrue();
        assertThat(this.readColumn("is_admin", id)).as("is_admin字段应当映射到is_admin列").isFalse();
        assertThat(this.readColumn("is", id)).as("is字段应当映射到is列").isTrue();
    }

    @Test
    public void settersWriteBackThroughProxy() throws Exception {
        this.clearTable();
        try (var pignoo = new BasePignoo(this.dataSource)) {
            BoolNaming saved = pignoo.writer(BoolNaming.class).add(new BoolNaming());
            Long id = saved.getId();

            saved.setAdmin(true);
            saved.setActive(true);
            saved.setIsadmin(true);
            saved.set_admin(true);
            saved.setIs(true);

            assertThat(this.readColumn("admin", id)).as("setAdmin应当回写到admin列").isTrue();
            assertThat(this.readColumn("is_active", id)).as("setActive应当回写到is_active列").isTrue();
            assertThat(this.readColumn("isadmin", id)).as("setIsadmin应当回写到isadmin列").isTrue();
            assertThat(this.readColumn("is_admin", id)).as("set_admin应当回写到is_admin列").isTrue();
            assertThat(this.readColumn("is", id)).as("setIs应当回写到is列").isTrue();
        }
    }
}
