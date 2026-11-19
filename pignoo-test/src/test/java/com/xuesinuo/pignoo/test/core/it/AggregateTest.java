package com.xuesinuo.pignoo.test.core.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.xuesinuo.pignoo.core.Pignoo;
import com.xuesinuo.pignoo.core.PignooConfig;
import com.xuesinuo.pignoo.core.PignooWriter;
import com.xuesinuo.pignoo.core.implement.BasePignoo;
import com.xuesinuo.pignoo.test.fixture.Plain;

/**
 * 聚合函数语义：max / min / sum / avg / countDistinct 及其 NullAs（COALESCE）变体
 * <p>
 * Aggregate semantics: max / min / sum / avg / countDistinct and their NullAs (COALESCE) variants
 */
@SpringBootTest
public class AggregateTest {

    public final DataSource dataSource;
    public PignooConfig config = new PignooConfig();

    public AggregateTest(@Autowired DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private static Plain addPlain(PignooWriter<Plain> writer, String name, Integer qty, BigDecimal money) {
        Plain plain = new Plain();
        plain.setName(name);
        plain.setQty(qty);
        plain.setMoney(money);
        return writer.add(plain);
    }

    /**
     * 造 4 行数据：money 为 10 / 20 / 30 / NULL，name 为 A / B / C / NULL，qty 为 1..4
     */
    private void prepare(Pignoo pignoo) {
        var writer = pignoo.writer(Plain.class);
        writer.removeAll();
        addPlain(writer, "A", 1, new BigDecimal("10"));
        addPlain(writer, "B", 2, new BigDecimal("20"));
        addPlain(writer, "C", 3, new BigDecimal("30"));
        addPlain(writer, null, 4, null);
    }

    /**
     * max / min / sum / avg 的基本语义：NULL 值不参与计算
     */
    @Test
    public void maxMinSumAvg() {
        try (BasePignoo pignoo = new BasePignoo(this.dataSource, this.config)) {
            prepare(pignoo);
            var reader = pignoo.reader(Plain.class);

            assertThat(reader.max(Plain::getMoney, BigDecimal.class)).as("max 应为 30").isEqualByComparingTo(new BigDecimal("30"));
            assertThat(reader.min(Plain::getMoney, BigDecimal.class)).as("min 应为 10").isEqualByComparingTo(new BigDecimal("10"));
            assertThat(reader.sum(Plain::getMoney, BigDecimal.class)).as("sum 应为 60").isEqualByComparingTo(new BigDecimal("60"));
            assertThat(reader.avg(Plain::getMoney, BigDecimal.class)).as("avg 只按 3 行非 NULL 计算，应为 20").isEqualByComparingTo(new BigDecimal("20"));

            assertThat(reader.max(Plain::getQty, Integer.class)).as("整型列的 max 应为 4").isEqualTo(4);
            assertThat(reader.min(Plain::getQty, Integer.class)).as("整型列的 min 应为 1").isEqualTo(1);
            assertThat(reader.max(Plain::getName, String.class)).as("字符串列的 max 应为 C").isEqualTo("C");
            assertThat(reader.min(Plain::getName, String.class)).as("字符串列的 min 应为 A").isEqualTo("A");

            assertThat(pignoo.writer(Plain.class).sum(Plain::getMoney, BigDecimal.class)).as("writer 侧的 sum 语义应与 reader 一致").isEqualByComparingTo(new BigDecimal("60"));
            assertThat(pignoo.writer(Plain.class).avg(Plain::getMoney, BigDecimal.class)).as("writer 侧的 avg 语义应与 reader 一致").isEqualByComparingTo(new BigDecimal("20"));
        }
    }

    /**
     * NullAs 变体：把 NULL 按给定值参与计算（COALESCE 语义）
     */
    @Test
    public void nullAsVariants() {
        try (BasePignoo pignoo = new BasePignoo(this.dataSource, this.config)) {
            prepare(pignoo);
            var reader = pignoo.reader(Plain.class);

            assertThat(reader.maxNullAs(Plain::getMoney, BigDecimal.class, new BigDecimal("100")))
                    .as("maxNullAs 把 NULL 当成 100 后，最大值应为 100").isEqualByComparingTo(new BigDecimal("100"));
            assertThat(reader.minNullAs(Plain::getMoney, BigDecimal.class, new BigDecimal("5")))
                    .as("minNullAs 把 NULL 当成 5 后，最小值应为 5").isEqualByComparingTo(new BigDecimal("5"));
            assertThat(reader.sumNullAs(Plain::getMoney, BigDecimal.class, new BigDecimal("5")))
                    .as("sumNullAs 把 NULL 当成 5 后，合计应为 65").isEqualByComparingTo(new BigDecimal("65"));
            assertThat(reader.avgNullAs(Plain::getMoney, BigDecimal.class, BigDecimal.ZERO))
                    .as("avgNullAs 把 NULL 当成 0 后按 4 行计算，应为 15").isEqualByComparingTo(new BigDecimal("15"));

            assertThat(reader.maxNullAs(Plain::getName, String.class, "Z")).as("字符串列的 maxNullAs 应为 Z").isEqualTo("Z");
            assertThat(reader.minNullAs(Plain::getName, String.class, "0")).as("字符串列的 minNullAs 应为 0").isEqualTo("0");

            assertThat(reader.maxNullAs(Plain::getQty, Integer.class, 99)).as("列中没有 NULL 时 NullAs 不改变结果").isEqualTo(4);
        }
    }

    /**
     * 空表：max / min / sum / avg 返回 null，countDistinct 返回 0
     */
    @Test
    public void aggregateOnEmptyTable() {
        try (BasePignoo pignoo = new BasePignoo(this.dataSource, this.config)) {
            pignoo.writer(Plain.class).removeAll();
            var reader = pignoo.reader(Plain.class);

            assertThat(reader.max(Plain::getMoney, BigDecimal.class)).as("空表的 max 应为 null").isNull();
            assertThat(reader.min(Plain::getMoney, BigDecimal.class)).as("空表的 min 应为 null").isNull();
            assertThat(reader.sum(Plain::getMoney, BigDecimal.class)).as("空表的 sum 应为 null").isNull();
            assertThat(reader.avg(Plain::getMoney, BigDecimal.class)).as("空表的 avg 应为 null").isNull();
            assertThat(reader.maxNullAs(Plain::getMoney, BigDecimal.class, new BigDecimal("1"))).as("空表的 maxNullAs 应为 null").isNull();
            assertThat(reader.sumNullAs(Plain::getMoney, BigDecimal.class, new BigDecimal("1"))).as("空表的 sumNullAs 应为 null").isNull();
            assertThat(reader.countDistinct(Plain::getMoney)).as("空表的 countDistinct 应为 0").isZero();
            assertThat(reader.countDistinctNullAs(Plain::getMoney, new BigDecimal("1"))).as("空表的 countDistinctNullAs 应为 0").isZero();
        }
    }

    /**
     * 整列全为 NULL：max / min / sum / avg 返回 null，countDistinct 返回 0
     */
    @Test
    public void aggregateOnAllNullColumn() {
        try (BasePignoo pignoo = new BasePignoo(this.dataSource, this.config)) {
            var writer = pignoo.writer(Plain.class);
            writer.removeAll();
            for (int i = 1; i <= 3; i++) {
                addPlain(writer, "N" + i, i, null);
            }
            var reader = pignoo.reader(Plain.class);

            assertThat(reader.size()).as("表内应有 3 行").isEqualTo(3L);
            assertThat(reader.max(Plain::getMoney, BigDecimal.class)).as("整列全 NULL 时 max 应为 null").isNull();
            assertThat(reader.min(Plain::getMoney, BigDecimal.class)).as("整列全 NULL 时 min 应为 null").isNull();
            assertThat(reader.sum(Plain::getMoney, BigDecimal.class)).as("整列全 NULL 时 sum 应为 null").isNull();
            assertThat(reader.avg(Plain::getMoney, BigDecimal.class)).as("整列全 NULL 时 avg 应为 null").isNull();
            assertThat(reader.countDistinct(Plain::getMoney)).as("整列全 NULL 时 countDistinct 应为 0").isZero();

            assertThat(reader.sumNullAs(Plain::getMoney, BigDecimal.class, new BigDecimal("5")))
                    .as("整列全 NULL 时 sumNullAs 应为 5*3=15").isEqualByComparingTo(new BigDecimal("15"));
            assertThat(reader.avgNullAs(Plain::getMoney, BigDecimal.class, new BigDecimal("5")))
                    .as("整列全 NULL 时 avgNullAs 应为 5").isEqualByComparingTo(new BigDecimal("5"));
            assertThat(reader.countDistinctNullAs(Plain::getMoney, new BigDecimal("5")))
                    .as("整列全 NULL 时 countDistinctNullAs 应为 1").isEqualTo(1L);
        }
    }

    /**
     * countDistinct 不计 NULL，countDistinctNullAs 把 NULL 计成给定值
     */
    @Test
    public void countDistinctAndNullAs() {
        try (BasePignoo pignoo = new BasePignoo(this.dataSource, this.config)) {
            var writer = pignoo.writer(Plain.class);
            writer.removeAll();
            addPlain(writer, "A", 1, new BigDecimal("10"));
            addPlain(writer, "A", 2, new BigDecimal("10"));
            addPlain(writer, "B", 3, new BigDecimal("20"));
            addPlain(writer, null, 4, null);
            var reader = pignoo.reader(Plain.class);

            assertThat(reader.size()).as("表内应有 4 行").isEqualTo(4L);
            assertThat(reader.countDistinct(Plain::getName)).as("countDistinct 去重且不计 NULL，应为 2").isEqualTo(2L);
            assertThat(reader.countDistinctNullAs(Plain::getName, "Z")).as("countDistinctNullAs 把 NULL 计成 Z，应为 3").isEqualTo(3L);
            assertThat(reader.countDistinctNullAs(Plain::getName, "A")).as("nullAs 与已有值重复时会被一起去重，应为 2").isEqualTo(2L);
            assertThat(reader.countDistinct(Plain::getQty)).as("qty 各不相同，countDistinct 应为 4").isEqualTo(4L);
        }
    }

    /**
     * maxNullAs / minNullAs 的 nullAs 参数不允许为 null
     */
    @Test
    public void nullAsParamCanNotBeNull() {
        try (BasePignoo pignoo = new BasePignoo(this.dataSource, this.config)) {
            prepare(pignoo);
            var reader = pignoo.reader(Plain.class);

            assertThatThrownBy(() -> reader.maxNullAs(Plain::getMoney, BigDecimal.class, null))
                    .as("maxNullAs 的 nullAs 为 null 时应抛 NullPointerException").isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> reader.minNullAs(Plain::getMoney, BigDecimal.class, null))
                    .as("minNullAs 的 nullAs 为 null 时应抛 NullPointerException").isInstanceOf(NullPointerException.class);
        }
    }

    /**
     * 聚合与 filter 组合：聚合只作用于 filter 命中的行
     */
    @Test
    public void aggregateWithFilter() {
        try (BasePignoo pignoo = new BasePignoo(this.dataSource, this.config)) {
            prepare(pignoo);
            var reader = pignoo.reader(Plain.class).filter(Plain::getQty, ">=", 2);

            assertThat(reader.size()).as("filter 应命中 3 行").isEqualTo(3L);
            assertThat(reader.max(Plain::getMoney, BigDecimal.class)).as("命中行的 max 应为 30").isEqualByComparingTo(new BigDecimal("30"));
            assertThat(reader.min(Plain::getMoney, BigDecimal.class)).as("命中行的 min 应为 20").isEqualByComparingTo(new BigDecimal("20"));
            assertThat(reader.sum(Plain::getMoney, BigDecimal.class)).as("命中行的 sum 应为 50").isEqualByComparingTo(new BigDecimal("50"));
            assertThat(reader.avg(Plain::getMoney, BigDecimal.class)).as("命中行中只有 2 行非 NULL，avg 应为 25").isEqualByComparingTo(new BigDecimal("25"));
            assertThat(reader.sumNullAs(Plain::getMoney, BigDecimal.class, new BigDecimal("5"))).as("命中行的 sumNullAs 应为 55").isEqualByComparingTo(new BigDecimal("55"));
            assertThat(reader.countDistinct(Plain::getName)).as("命中行中非 NULL 的 name 有 2 个").isEqualTo(2L);

            var noneMatch = pignoo.reader(Plain.class).filter(Plain::getQty, ">", 100);
            assertThat(noneMatch.sum(Plain::getMoney, BigDecimal.class)).as("filter 无命中时 sum 应为 null").isNull();
            assertThat(noneMatch.countDistinct(Plain::getName)).as("filter 无命中时 countDistinct 应为 0").isZero();
        }
    }
}
