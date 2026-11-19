package com.xuesinuo.pignoo.test.core.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.xuesinuo.pignoo.core.PignooConfig;
import com.xuesinuo.pignoo.core.PignooSorter;
import com.xuesinuo.pignoo.core.PignooSorter.SMode;
import com.xuesinuo.pignoo.core.PignooWriter;
import com.xuesinuo.pignoo.core.implement.BasePignoo;
import com.xuesinuo.pignoo.test.fixture.Plain;

/**
 * 排序语义：升降序、多级排序的拼接顺序、NULL 值的位置，以及 sorter 对 getFirst / getAny 的影响
 * <p>
 * Sorter semantics: asc/desc, multi-level ordering, NULL placement and the effect on getFirst / getAny
 */
@SpringBootTest
public class SorterTest {

    public final DataSource dataSource;
    public PignooConfig config = new PignooConfig();

    public SorterTest(@Autowired DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private static Plain addPlain(PignooWriter<Plain> writer, String name, String code, Integer qty) {
        Plain plain = new Plain();
        plain.setName(name);
        plain.setCode(code);
        plain.setQty(qty);
        plain.setMoney(new BigDecimal(qty));
        return writer.add(plain);
    }

    private static List<Integer> qtyOf(List<Plain> list) {
        return list.stream().map(Plain::getQty).toList();
    }

    private static List<String> nameOf(List<Plain> list) {
        return list.stream().map(Plain::getName).toList();
    }

    /**
     * MIN_FIRST 为升序，MAX_FIRST 为降序
     */
    @Test
    public void minFirstAndMaxFirst() {
        try (BasePignoo pignoo = new BasePignoo(this.dataSource, this.config)) {
            var writer = pignoo.writer(Plain.class);
            writer.removeAll();
            for (int i = 1; i <= 5; i++) {
                addPlain(writer, "N" + i, "C" + i, i);
            }

            assertThat(qtyOf(pignoo.reader(Plain.class).sort(Plain::getQty, SMode.MIN_FIRST).getAll()))
                    .as("MIN_FIRST 应为升序").containsExactly(1, 2, 3, 4, 5);
            assertThat(qtyOf(pignoo.reader(Plain.class).sort(Plain::getQty, SMode.MAX_FIRST).getAll()))
                    .as("MAX_FIRST 应为降序").containsExactly(5, 4, 3, 2, 1);
            assertThat(qtyOf(pignoo.reader(Plain.class).sort(PignooSorter.build(Plain::getQty, SMode.MAX_FIRST)).getAll()))
                    .as("传入 PignooSorter 与传字段的写法等价").containsExactly(5, 4, 3, 2, 1);
            assertThat(qtyOf(pignoo.reader(Plain.class).sort(Plain::getQty, SMode.MIN_FIRST).get(1, 2)))
                    .as("排序应作用于分页查询").containsExactly(2, 3);
            assertThat(qtyOf(pignoo.writer(Plain.class).sort(Plain::getQty, SMode.MAX_FIRST).getAll()))
                    .as("writer 侧的排序语义应与 reader 一致").containsExactly(5, 4, 3, 2, 1);
        }
    }

    /**
     * 多级排序：后调用的 sort 会成为主排序键（ORDER BY 是反向拼接的）
     */
    @Test
    public void multiLevelSortPutsTheLastSortFirst() {
        try (BasePignoo pignoo = new BasePignoo(this.dataSource, this.config)) {
            var writer = pignoo.writer(Plain.class);
            writer.removeAll();
            addPlain(writer, "N1", "G1", 1);
            addPlain(writer, "N2", "G1", 2);
            addPlain(writer, "N3", "G2", 3);
            addPlain(writer, "N4", "G2", 4);

            // 单级排序：code 降序时，G2 的两行一定排在前面
            assertThat(pignoo.reader(Plain.class).sort(Plain::getCode, SMode.MAX_FIRST).getAll().get(0).getCode())
                    .as("单级排序按 code 降序时，第一行应属于 G2").isEqualTo("G2");

            // 两级排序：后调用的 sort(qty) 才是主排序键，所以整体按 qty 升序
            assertThat(qtyOf(pignoo.reader(Plain.class).sort(Plain::getCode, SMode.MAX_FIRST).sort(Plain::getQty, SMode.MIN_FIRST).getAll()))
                    .as("sort(code).sort(qty) 中后调用的 sort(qty) 是主排序键，结果应按 qty 升序").containsExactly(1, 2, 3, 4);

            assertThat(qtyOf(pignoo.reader(Plain.class).sort(PignooSorter.<Plain>build(Plain::getCode, SMode.MAX_FIRST).then(Plain::getQty, SMode.MIN_FIRST)).getAll()))
                    .as("PignooSorter.then(字段) 与连续 sort 的拼接顺序一致").containsExactly(1, 2, 3, 4);

            assertThat(qtyOf(pignoo.reader(Plain.class)
                    .sort(PignooSorter.<Plain>build(Plain::getCode, SMode.MAX_FIRST).then(PignooSorter.build(Plain::getQty, SMode.MIN_FIRST)))
                    .getAll()))
                    .as("PignooSorter.then(排序器) 与连续 sort 的拼接顺序一致").containsExactly(1, 2, 3, 4);

            // 反过来：先 qty 后 code，则 code 是主排序键
            var byCodeFirst = pignoo.reader(Plain.class).sort(Plain::getQty, SMode.MIN_FIRST).sort(Plain::getCode, SMode.MAX_FIRST).getAll();
            assertThat(qtyOf(byCodeFirst)).as("sort(qty).sort(code) 中后调用的 sort(code) 是主排序键，G2 组应整体在前").containsExactly(3, 4, 1, 2);
        }
    }

    /**
     * 含 NULL 值列的排序位置：MySQL 中 NULL 在升序排最前、降序排最后
     */
    @Test
    public void nullValueOrdering() {
        try (BasePignoo pignoo = new BasePignoo(this.dataSource, this.config)) {
            var writer = pignoo.writer(Plain.class);
            writer.removeAll();
            addPlain(writer, "B", "C1", 1);
            addPlain(writer, "A", "C2", 2);
            addPlain(writer, null, "C3", 3);
            addPlain(writer, "C", "C4", 4);
            addPlain(writer, null, "C5", 5);

            assertThat(nameOf(pignoo.reader(Plain.class).sort(Plain::getName, SMode.MIN_FIRST).getAll()))
                    .as("升序时 NULL 排在最前").containsExactly(null, null, "A", "B", "C");
            assertThat(nameOf(pignoo.reader(Plain.class).sort(Plain::getName, SMode.MAX_FIRST).getAll()))
                    .as("降序时 NULL 排在最后").containsExactly("C", "B", "A", null, null);

            // 用 qty 作为第二排序键，把两行 NULL 的顺序也钉死（qty 是后调用的 sort，是主排序键，故先按 name 排的效果需反过来写）
            assertThat(qtyOf(pignoo.reader(Plain.class).sort(Plain::getQty, SMode.MIN_FIRST).sort(Plain::getName, SMode.MIN_FIRST).getAll()))
                    .as("name 升序为主、qty 升序为次：两行 NULL 按 qty 升序排在最前").containsExactly(3, 5, 2, 1, 4);
        }
    }

    /**
     * getFirst 受 sorter 影响，getAny 忽略 sorter
     */
    @Test
    public void getFirstIsSortedAndGetAnyIsNot() {
        try (BasePignoo pignoo = new BasePignoo(this.dataSource, this.config)) {
            var writer = pignoo.writer(Plain.class);
            writer.removeAll();
            for (int i = 1; i <= 5; i++) {
                addPlain(writer, "N" + i, "C" + i, i);
            }

            assertThat(pignoo.reader(Plain.class).sort(Plain::getQty, SMode.MAX_FIRST).getFirst().getQty())
                    .as("getFirst 受 sorter 影响：降序应取到最大值").isEqualTo(5);
            assertThat(pignoo.reader(Plain.class).sort(Plain::getQty, SMode.MIN_FIRST).getFirst().getQty())
                    .as("getFirst 受 sorter 影响：升序应取到最小值").isEqualTo(1);
            assertThat(pignoo.reader(Plain.class).filter(Plain::getQty, "<=", 3).sort(Plain::getQty, SMode.MAX_FIRST).getFirst().getQty())
                    .as("getFirst 同时受 filter 与 sorter 影响").isEqualTo(3);

            Integer anyWithMaxFirst = pignoo.reader(Plain.class).sort(Plain::getQty, SMode.MAX_FIRST).getAny().getQty();
            Integer anyWithMinFirst = pignoo.reader(Plain.class).sort(Plain::getQty, SMode.MIN_FIRST).getAny().getQty();
            Integer anyWithoutSorter = pignoo.reader(Plain.class).getAny().getQty();
            assertThat(anyWithMaxFirst).as("getAny 忽略 sorter：升降序应取到同一行").isEqualTo(anyWithMinFirst);
            assertThat(anyWithMaxFirst).as("getAny 忽略 sorter：与不带 sorter 的结果一致").isEqualTo(anyWithoutSorter);
            assertThat(pignoo.reader(Plain.class).filter(Plain::getQty, "==", 4).sort(Plain::getQty, SMode.MAX_FIRST).getAny().getQty())
                    .as("getAny 忽略 sorter，但仍受 filter 影响").isEqualTo(4);
        }
    }
}
