package com.xuesinuo.pignoo.test.core.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.xuesinuo.pignoo.core.PignooConfig;
import com.xuesinuo.pignoo.core.annotation.Column;
import com.xuesinuo.pignoo.core.config.NamingMode;
import com.xuesinuo.pignoo.core.implement.BasePignoo;
import com.xuesinuo.pignoo.test.fixture.LinkProjection;
import com.xuesinuo.pignoo.test.fixture.Plain;

import lombok.Data;

/**
 * 投影（Link注解）与命名模式集成测试
 * <p>
 * Integration test for the Link projection and the naming modes
 */
@SpringBootTest
public class LinkAndNamingTest {

    public final DataSource dataSource;

    public LinkAndNamingTest(@Autowired DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** CAMEL_TO_SNAKE模式下：类名映射到test02_for_mix_camel，属性名映射到蛇形列名 */
    @Data
    public static class Test02ForMixCamel {
        @Column(primaryKey = Column.PrimaryKey.AUTO)
        private Long test02ForMixCamelId;
        private String nameForMixCamel;
    }

    /** SAME模式下：类名等于表名，属性名等于列名 */
    @Data
    public static class Test02ForMastSame {
        private Long id;
        private String nameForMastSame;
    }

    /** 造一条完整的Plain，返回它的主键 */
    private Long seedPlain(BasePignoo pignoo) {
        var writer = pignoo.writer(Plain.class);
        writer.removeAll();
        Plain plain = new Plain();
        plain.setName("原始名字");
        plain.setCode("CODE-1");
        plain.setMoney(new BigDecimal("12.34"));
        plain.setQty(7);
        return writer.add(plain).getId();
    }

    @Test
    public void linkProjectionReadsSharedTable() {
        try (var pignoo = new BasePignoo(this.dataSource)) {
            Long id = this.seedPlain(pignoo);

            LinkProjection projection = pignoo.reader(LinkProjection.class).filter(LinkProjection::getId, "==", id).getAny();
            assertThat(projection).as("@Link投影应当与Plain共用fx_plain表").isNotNull();
            assertThat(projection.getId()).as("投影读到的主键应当与Plain写入的一致").isEqualTo(id);
            assertThat(projection.getName()).as("投影读到的name应当与Plain写入的一致").isEqualTo("原始名字");
        }
    }

    @Test
    public void linkProjectionInsertLeavesOtherColumnsNull() {
        try (var pignoo = new BasePignoo(this.dataSource)) {
            var plainWriter = pignoo.writer(Plain.class);
            plainWriter.removeAll();

            LinkProjection projection = new LinkProjection();
            projection.setName("只写投影列");
            Long id = pignoo.writer(LinkProjection.class).add(projection).getId();

            Plain back = pignoo.reader(Plain.class).filter(Plain::getId, "==", id).getAny();
            assertThat(back).as("用投影写入的行应当能用Plain读出来").isNotNull();
            assertThat(back.getName()).as("投影拥有的列应当被写入").isEqualTo("只写投影列");
            assertThat(back.getCode()).as("投影不拥有的列不应被写入").isNull();
            assertThat(back.getMoney()).as("投影不拥有的列不应被写入").isNull();
            assertThat(back.getQty()).as("投影不拥有的列不应被写入").isNull();
        }
    }

    @Test
    public void linkProjectionUpdateTouchesOnlyItsOwnColumns() {
        try (var pignoo = new BasePignoo(this.dataSource)) {
            Long id = this.seedPlain(pignoo);

            LinkProjection projection = new LinkProjection();
            projection.setId(id);
            projection.setName("投影改名");
            long rows = pignoo.writer(LinkProjection.class).replaceById(projection);
            assertThat(rows).as("按主键更新投影应当命中1行").isEqualTo(1L);

            Plain back = pignoo.reader(Plain.class).filter(Plain::getId, "==", id).getAny();
            assertThat(back.getName()).as("投影拥有的列应当被更新").isEqualTo("投影改名");
            assertThat(back.getCode()).as("投影不拥有的列不应被更新").isEqualTo("CODE-1");
            assertThat(back.getMoney()).as("投影不拥有的列不应被更新").isEqualByComparingTo(new BigDecimal("12.34"));
            assertThat(back.getQty()).as("投影不拥有的列不应被更新").isEqualTo(7);
        }
    }

    @Test
    public void linkProjectionSetterUpdatesOnlyItsOwnColumn() {
        try (var pignoo = new BasePignoo(this.dataSource)) {
            Long id = this.seedPlain(pignoo);

            LinkProjection projection = pignoo.writer(LinkProjection.class).filter(LinkProjection::getId, "==", id).getAny();
            projection.setName("投影代理改名");

            Plain back = pignoo.reader(Plain.class).filter(Plain::getId, "==", id).getAny();
            assertThat(back.getName()).as("投影的setter代理应当回写name列").isEqualTo("投影代理改名");
            assertThat(back.getCode()).as("投影的setter代理不应影响其他列").isEqualTo("CODE-1");
            assertThat(back.getQty()).as("投影的setter代理不应影响其他列").isEqualTo(7);
        }
    }

    @Test
    public void camelToSnakeNaming() {
        PignooConfig config = new PignooConfig();
        config.setNamingMode(NamingMode.CAMEL_TO_SNAKE);
        try (var pignoo = new BasePignoo(this.dataSource, config)) {
            var writer = pignoo.writer(Test02ForMixCamel.class);
            writer.removeAll();

            Test02ForMixCamel row = new Test02ForMixCamel();
            row.setNameForMixCamel("驼峰转蛇形");
            Long id = writer.add(row).getTest02ForMixCamelId();
            assertThat(id).as("CAMEL_TO_SNAKE模式下应当映射到test02_for_mix_camel表并拿到自增主键").isNotNull();

            Test02ForMixCamel back = pignoo.reader(Test02ForMixCamel.class)
                    .filter(Test02ForMixCamel::getTest02ForMixCamelId, "==", id).getAny();
            assertThat(back).as("CAMEL_TO_SNAKE模式下应当能按蛇形主键列查回数据").isNotNull();
            assertThat(back.getNameForMixCamel()).as("nameForMixCamel应当映射到name_for_mix_camel列并往返一致").isEqualTo("驼峰转蛇形");
        }
    }

    @Test
    public void sameNaming() {
        PignooConfig config = new PignooConfig();
        config.setNamingMode(NamingMode.SAME);
        config.setAutoPrimaryKey(false); // Test02ForMastSame的主键不是自增列
        try (var pignoo = new BasePignoo(this.dataSource, config)) {
            var writer = pignoo.writer(Test02ForMastSame.class);
            writer.removeAll();

            Test02ForMastSame row = new Test02ForMastSame();
            row.setId(101L);
            row.setNameForMastSame("原样命名");
            writer.add(row);

            Test02ForMastSame back = pignoo.reader(Test02ForMastSame.class)
                    .filter(Test02ForMastSame::getId, "==", 101L).getAny();
            assertThat(back).as("SAME模式下应当映射到Test02ForMastSame表").isNotNull();
            assertThat(back.getNameForMastSame()).as("nameForMastSame应当原样映射到nameForMastSame列并往返一致").isEqualTo("原样命名");
        }
    }
}
