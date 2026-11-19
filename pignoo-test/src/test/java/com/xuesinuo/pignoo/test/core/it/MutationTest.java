package com.xuesinuo.pignoo.test.core.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.xuesinuo.pignoo.core.PignooConfig;
import com.xuesinuo.pignoo.core.PignooSorter.SMode;
import com.xuesinuo.pignoo.core.PignooWriter;
import com.xuesinuo.pignoo.core.exception.MapperException;
import com.xuesinuo.pignoo.core.implement.BasePignoo;
import com.xuesinuo.pignoo.test.fixture.Plain;

/**
 * 修改与删除的语义：mix（跳过 null）与 replace（把 null 写成 NULL）的对照，以及 removeAll 的整表语义
 * <p>
 * Mutation semantics: mix (skips null fields) versus replace (writes NULL), and the whole-table removeAll
 */
@SpringBootTest
public class MutationTest {

    /** 显式的 null 值，避免可变参数把裸 null 当成空数组 */
    public static final Object NULL = null;

    public final DataSource dataSource;
    public PignooConfig config = new PignooConfig();

    public MutationTest(@Autowired DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private static Plain addPlain(PignooWriter<Plain> writer, String name, String code, Integer qty, BigDecimal money) {
        Plain plain = new Plain();
        plain.setName(name);
        plain.setCode(code);
        plain.setQty(qty);
        plain.setMoney(money);
        return writer.add(plain);
    }

    /**
     * mixById 跳过 null 字段（不动原值）；replaceById 把 null 写成 NULL（清空原值）
     */
    @Test
    public void mixByIdVersusReplaceById() {
        try (BasePignoo pignoo = new BasePignoo(this.dataSource, this.config)) {
            var writer = pignoo.writer(Plain.class);
            writer.removeAll();
            Plain saved = addPlain(writer, "N", "C", 1, new BigDecimal("10"));
            Long id = saved.getId();

            Plain mix = new Plain();
            mix.setId(id);
            mix.setCode("C-MIX");
            assertThat(pignoo.writer(Plain.class).mixById(mix)).as("mixById 应影响 1 行").isEqualTo(1L);

            Plain afterMix = pignoo.reader(Plain.class).filter(Plain::getId, "==", id).getAny();
            assertThat(afterMix.getCode()).as("mixById 应更新非 null 字段").isEqualTo("C-MIX");
            assertThat(afterMix.getName()).as("mixById 跳过 null 字段，name 应保持原值").isEqualTo("N");
            assertThat(afterMix.getQty()).as("mixById 跳过 null 字段，qty 应保持原值").isEqualTo(1);
            assertThat(afterMix.getMoney()).as("mixById 跳过 null 字段，money 应保持原值").isEqualByComparingTo(new BigDecimal("10"));

            Plain replace = new Plain();
            replace.setId(id);
            replace.setCode("C-REPLACE");
            assertThat(pignoo.writer(Plain.class).replaceById(replace)).as("replaceById 应影响 1 行").isEqualTo(1L);

            Plain afterReplace = pignoo.reader(Plain.class).filter(Plain::getId, "==", id).getAny();
            assertThat(afterReplace.getCode()).as("replaceById 应更新非 null 字段").isEqualTo("C-REPLACE");
            assertThat(afterReplace.getName()).as("replaceById 把 null 字段写成 NULL，name 应被清空").isNull();
            assertThat(afterReplace.getQty()).as("replaceById 把 null 字段写成 NULL，qty 应被清空").isNull();
            assertThat(afterReplace.getMoney()).as("replaceById 把 null 字段写成 NULL，money 应被清空").isNull();
            assertThat(afterReplace.getId()).as("主键不在被更新的字段中").isEqualTo(id);
        }
    }

    /**
     * mixAll / replaceAll 作用于 filter 命中的所有行，null 字段的处理与 by-id 版本一致
     */
    @Test
    public void mixAllVersusReplaceAll() {
        try (BasePignoo pignoo = new BasePignoo(this.dataSource, this.config)) {
            var writer = pignoo.writer(Plain.class);
            writer.removeAll();
            for (int i = 1; i <= 3; i++) {
                addPlain(writer, "N" + i, "C" + i, i, new BigDecimal(i));
            }

            Plain mix = new Plain();
            mix.setCode("C-MIX");
            long mixRow = pignoo.writer(Plain.class).filter(Plain::getQty, ">=", 2).mixAll(mix);
            assertThat(mixRow).as("mixAll 应影响 filter 命中的 2 行").isEqualTo(2L);
            assertThat(pignoo.reader(Plain.class).filter(Plain::getCode, "==", "C-MIX").size()).as("2 行的 code 应被更新").isEqualTo(2L);
            assertThat(pignoo.reader(Plain.class).filter(Plain::getName, "!=", NULL).size()).as("mixAll 跳过 null 字段，3 行的 name 都还在").isEqualTo(3L);
            assertThat(pignoo.reader(Plain.class).filter(Plain::getCode, "==", "C1").size()).as("未命中 filter 的行不应被改动").isEqualTo(1L);

            Plain replace = new Plain();
            replace.setCode("C-REPLACE");
            long replaceRow = pignoo.writer(Plain.class).filter(Plain::getQty, ">=", 2).replaceAll(replace);
            assertThat(replaceRow).as("replaceAll 应影响 filter 命中的 2 行").isEqualTo(2L);
            assertThat(pignoo.reader(Plain.class).filter(Plain::getCode, "==", "C-REPLACE").size()).as("2 行的 code 应被更新").isEqualTo(2L);
            assertThat(pignoo.reader(Plain.class).filter(Plain::getName, "==", NULL).size()).as("replaceAll 把 null 字段写成 NULL，2 行的 name 应被清空").isEqualTo(2L);
            assertThat(pignoo.reader(Plain.class).filter(Plain::getQty, "==", NULL).size()).as("replaceAll 把 null 字段写成 NULL，2 行的 qty 应被清空").isEqualTo(2L);

            Plain survivor = pignoo.reader(Plain.class).filter(Plain::getQty, "==", 1).getAny();
            assertThat(survivor).as("未命中 filter 的行应完好保留").isNotNull();
            assertThat(survivor.getName()).as("未命中 filter 的行 name 不应被清空").isEqualTo("N1");

            Plain mixAllWithoutFilter = new Plain();
            mixAllWithoutFilter.setName("N-ALL");
            assertThat(pignoo.writer(Plain.class).mixAll(mixAllWithoutFilter)).as("无 filter 的 mixAll 是整表更新").isEqualTo(3L);
            assertThat(pignoo.reader(Plain.class).filter(Plain::getName, "==", "N-ALL").size()).as("整表更新后 3 行的 name 都应变化").isEqualTo(3L);
        }
    }

    /**
     * 主键为 null 时，by-id 系列方法一律抛 MapperException
     */
    @Test
    public void byIdMethodsRejectNullPrimaryKey() {
        try (BasePignoo pignoo = new BasePignoo(this.dataSource, this.config)) {
            var writer = pignoo.writer(Plain.class);
            writer.removeAll();
            addPlain(writer, "N", "C", 1, new BigDecimal("10"));

            Plain noId = new Plain();
            noId.setName("NEW-NAME");
            assertThatThrownBy(() -> writer.mixById(noId)).as("主键为 null 的 mixById 应抛 MapperException").isInstanceOf(MapperException.class);
            assertThatThrownBy(() -> writer.replaceById(noId)).as("主键为 null 的 replaceById 应抛 MapperException").isInstanceOf(MapperException.class);
            assertThatThrownBy(() -> writer.removeById(noId)).as("主键为 null 的 removeById 应抛 MapperException").isInstanceOf(MapperException.class);

            assertThat(writer.size()).as("抛异常的修改不应改动数据").isEqualTo(1L);
            assertThat(pignoo.reader(Plain.class).getAny().getName()).as("抛异常的修改不应改动数据").isEqualTo("N");
        }
    }

    /**
     * 没有任何可更新字段（非主键字段全为 null）时，mix 系列返回 0
     */
    @Test
    public void mixWithoutUpdatableFieldReturnsZero() {
        try (BasePignoo pignoo = new BasePignoo(this.dataSource, this.config)) {
            var writer = pignoo.writer(Plain.class);
            writer.removeAll();
            Plain saved = addPlain(writer, "N", "C", 1, new BigDecimal("10"));

            Plain onlyId = new Plain();
            onlyId.setId(saved.getId());
            assertThat(pignoo.writer(Plain.class).mixById(onlyId)).as("没有可更新字段时 mixById 应返回 0").isZero();
            assertThat(pignoo.writer(Plain.class).mixAll(new Plain())).as("没有可更新字段时 mixAll 应返回 0").isZero();

            Plain unchanged = pignoo.reader(Plain.class).filter(Plain::getId, "==", saved.getId()).getAny();
            assertThat(unchanged.getName()).as("没有可更新字段时不应改动数据").isEqualTo("N");
            assertThat(unchanged.getCode()).as("没有可更新字段时不应改动数据").isEqualTo("C");
        }
    }

    /**
     * removeById 按主键删除；removeAll 在没有 filter 时是整表删除（设计上的"脚枪"）
     */
    @Test
    public void removeByIdAndRemoveAll() {
        try (BasePignoo pignoo = new BasePignoo(this.dataSource, this.config)) {
            var writer = pignoo.writer(Plain.class);
            writer.removeAll();
            Plain p1 = addPlain(writer, "N1", "C1", 1, new BigDecimal("10"));
            addPlain(writer, "N2", "C2", 2, new BigDecimal("20"));
            Plain p3 = addPlain(writer, "N3", "C3", 3, new BigDecimal("30"));

            assertThat(writer.removeById(p3)).as("removeById 应删除 1 行").isEqualTo(1L);
            assertThat(writer.size()).as("删除后应剩 2 行").isEqualTo(2L);
            assertThat(pignoo.reader(Plain.class).filter(Plain::getId, "==", p3.getId()).size()).as("被删的行应查不到").isZero();

            assertThat(writer.removeById(p3)).as("重复删除同一主键应返回 0").isZero();

            assertThat(pignoo.writer(Plain.class).filter(Plain::getQty, "==", 2).removeAll()).as("带 filter 的 removeAll 只删命中行").isEqualTo(1L);
            assertThat(writer.size()).as("删除后应剩 1 行").isEqualTo(1L);
            assertThat(pignoo.reader(Plain.class).filter(Plain::getId, "==", p1.getId()).size()).as("未命中 filter 的行应保留").isEqualTo(1L);

            addPlain(writer, "N4", "C4", 4, new BigDecimal("40"));
            addPlain(writer, "N5", "C5", 5, new BigDecimal("50"));
            assertThat(writer.removeAll()).as("无 filter 的 removeAll 是整表删除").isEqualTo(3L);
            assertThat(writer.size()).as("整表删除后应为空表").isZero();
        }
    }

    /**
     * pollFirst 取出并删除排序后的第一条，pollAny 忽略 sorter；空表返回 null
     */
    @Test
    public void pollFirstAndPollAny() {
        try (BasePignoo pignoo = new BasePignoo(this.dataSource, this.config)) {
            var writer = pignoo.writer(Plain.class);
            writer.removeAll();
            for (int i = 1; i <= 3; i++) {
                addPlain(writer, "N" + i, "C" + i, i, new BigDecimal(i));
            }

            Plain polled = pignoo.writer(Plain.class).sort(Plain::getQty, SMode.MAX_FIRST).pollFirst();
            assertThat(polled).as("pollFirst 应取出一条数据").isNotNull();
            assertThat(polled.getQty()).as("pollFirst 受 sorter 影响，倒序时取出最大的一条").isEqualTo(3);
            assertThat(pignoo.reader(Plain.class).size()).as("pollFirst 应同时删除取出的行").isEqualTo(2L);
            assertThat(pignoo.reader(Plain.class).filter(Plain::getQty, "==", 3).size()).as("被取出的行应已删除").isZero();

            Plain any = pignoo.writer(Plain.class).pollAny();
            assertThat(any).as("pollAny 应取出一条数据").isNotNull();
            assertThat(pignoo.reader(Plain.class).size()).as("pollAny 应同时删除取出的行").isEqualTo(1L);
            assertThat(pignoo.reader(Plain.class).filter(Plain::getId, "==", any.getId()).size()).as("被取出的行应已删除").isZero();

            pignoo.writer(Plain.class).removeAll();
            assertThat(pignoo.writer(Plain.class).pollFirst()).as("空表的 pollFirst 应返回 null").isNull();
            assertThat(pignoo.writer(Plain.class).pollAny()).as("空表的 pollAny 应返回 null").isNull();
        }
    }

    /**
     * by-id 系列与 filter 组合：条件是"主键 AND 过滤器"，filter 不命中时不生效
     */
    @Test
    public void byIdMethodsCombinedWithFilter() {
        try (BasePignoo pignoo = new BasePignoo(this.dataSource, this.config)) {
            var writer = pignoo.writer(Plain.class);
            writer.removeAll();
            Plain p1 = addPlain(writer, "N1", "C1", 1, new BigDecimal("10"));
            Plain p2 = addPlain(writer, "N2", "C2", 2, new BigDecimal("20"));

            Plain mix = new Plain();
            mix.setId(p2.getId());
            mix.setName("N2-MIX");
            assertThat(pignoo.writer(Plain.class).filter(Plain::getQty, "==", 99).mixById(mix)).as("主键命中但 filter 不命中时 mixById 返回 0").isZero();
            assertThat(pignoo.reader(Plain.class).filter(Plain::getId, "==", p2.getId()).getAny().getName()).as("filter 不命中时不应改动数据").isEqualTo("N2");

            assertThat(pignoo.writer(Plain.class).filter(Plain::getQty, "==", 2).mixById(mix)).as("主键与 filter 都命中时 mixById 影响 1 行").isEqualTo(1L);
            assertThat(pignoo.reader(Plain.class).filter(Plain::getId, "==", p2.getId()).getAny().getName()).as("filter 命中时应改动数据").isEqualTo("N2-MIX");

            assertThat(pignoo.writer(Plain.class).filter(Plain::getQty, "==", 2).removeById(p1)).as("主键命中但 filter 不命中时 removeById 返回 0").isZero();
            assertThat(pignoo.reader(Plain.class).size()).as("filter 不命中时不应删除").isEqualTo(2L);

            assertThat(pignoo.writer(Plain.class).filter(Plain::getQty, "==", 1).removeById(p1)).as("主键与 filter 都命中时 removeById 影响 1 行").isEqualTo(1L);
            assertThat(pignoo.reader(Plain.class).size()).as("filter 命中时应删除").isEqualTo(1L);
        }
    }
}
