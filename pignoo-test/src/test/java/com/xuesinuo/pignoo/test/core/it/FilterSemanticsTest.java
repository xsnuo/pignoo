package com.xuesinuo.pignoo.test.core.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.xuesinuo.pignoo.core.Pignoo;
import com.xuesinuo.pignoo.core.PignooConfig;
import com.xuesinuo.pignoo.core.PignooFilter;
import com.xuesinuo.pignoo.core.PignooFilter.FMode;
import com.xuesinuo.pignoo.core.exception.MapperException;
import com.xuesinuo.pignoo.core.implement.BasePignoo;
import com.xuesinuo.pignoo.test.fixture.Plain;

/**
 * 过滤器语义：各 FMode 的可观察结果、NULL 的特殊处理、值集合摊平、复合与空过滤器
 * <p>
 * Filter semantics: observable result of every FMode, the special NULL handling, value flattening,
 * composite filters and the empty filter
 */
@SpringBootTest
public class FilterSemanticsTest {

    /** 显式的 null 值，避免可变参数把裸 null 当成空数组 */
    public static final Object NULL = null;

    public final DataSource dataSource;
    public PignooConfig config = new PignooConfig();

    public FilterSemanticsTest(@Autowired DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 造 9 行基准数据：qty 为 1..9；qty 能被 3 整除的 3 行，其 name / code / money 为 NULL
     */
    private void prepare(Pignoo pignoo) {
        var writer = pignoo.writer(Plain.class);
        writer.removeAll();
        for (int i = 1; i <= 9; i++) {
            Plain plain = new Plain();
            plain.setQty(i);
            if (i % 3 != 0) {
                plain.setName(i + "-NAME");
                plain.setCode(i + "-CODE");
                plain.setMoney(new BigDecimal(i));
            }
            writer.add(plain);
        }
    }

    /**
     * 每个 FMode 在不含 NULL 场景下的基本语义
     */
    @Test
    public void basicModes() {
        try (BasePignoo pignoo = new BasePignoo(this.dataSource, this.config)) {
            prepare(pignoo);
            var reader = pignoo.reader(Plain.class);
            assertThat(reader.size()).as("基准数据应为 9 行").isEqualTo(9L);

            assertThat(reader.copyReader().filter(Plain::getQty, FMode.EQ, 5).size()).as("EQ 应命中 1 行").isEqualTo(1L);
            assertThat(reader.copyReader().filter(Plain::getQty, FMode.NE, 5).size()).as("NE 应命中除该值外的 8 行").isEqualTo(8L);
            assertThat(reader.copyReader().filter(Plain::getQty, FMode.GT, 5).size()).as("GT 应命中 6..9 共 4 行").isEqualTo(4L);
            assertThat(reader.copyReader().filter(Plain::getQty, FMode.LT, 5).size()).as("LT 应命中 1..4 共 4 行").isEqualTo(4L);
            assertThat(reader.copyReader().filter(Plain::getQty, FMode.GE, 5).size()).as("GE 应命中 5..9 共 5 行").isEqualTo(5L);
            assertThat(reader.copyReader().filter(Plain::getQty, FMode.LE, 5).size()).as("LE 应命中 1..5 共 5 行").isEqualTo(5L);
            assertThat(reader.copyReader().filter(Plain::getName, FMode.LIKE, "1-%").size()).as("LIKE 应命中 1 行").isEqualTo(1L);
            assertThat(reader.copyReader().filter(Plain::getQty, FMode.IN, 1, 2, 3).size()).as("IN 应命中 3 行").isEqualTo(3L);
            assertThat(reader.copyReader().filter(Plain::getQty, FMode.NOT_IN, 1, 2, 3).size()).as("NOT_IN 应命中其余 6 行").isEqualTo(6L);

            // 字符串写法与枚举写法等价，且大小写不敏感
            assertThat(reader.copyReader().filter(Plain::getQty, "==", 5).size()).as("字符串写法的 == 与 EQ 等价").isEqualTo(1L);
            assertThat(reader.copyReader().filter(Plain::getQty, ">=", 5).size()).as("字符串写法的 >= 与 GE 等价").isEqualTo(5L);
            assertThat(reader.copyReader().filter(Plain::getName, "LIKE", "1-%").size()).as("过滤条件名不区分大小写").isEqualTo(1L);
            assertThat(reader.copyReader().filter(Plain::getQty, "not in", 1, 2, 3).size()).as("字符串写法的 not in 与 NOT_IN 等价").isEqualTo(6L);

            assertThat(reader.copyReader().filter(Plain::getQty, FMode.EQ, 5).getAll()).as("getAll 也应受 filter 影响").hasSize(1);
            assertThat(reader.copyReader().filter(Plain::getQty, FMode.EQ, 5).getAll().get(0).getQty()).as("命中的应是 qty=5 的那一行").isEqualTo(5);
            assertThat(reader.copyReader().filter(Plain::getQty, FMode.EQ, 99).getAll()).as("无命中时 getAll 应为空列表").isEmpty();
            assertThat(reader.copyReader().filter(Plain::getQty, FMode.EQ, 99).getFirst()).as("无命中时 getFirst 应为 null").isNull();
        }
    }

    /**
     * NULL 的特殊处理：这是本框架最容易与直觉不符的部分
     */
    @Test
    public void nullSemantics() {
        try (BasePignoo pignoo = new BasePignoo(this.dataSource, this.config)) {
            prepare(pignoo);
            var reader = pignoo.reader(Plain.class);

            assertThat(reader.copyReader().filter(Plain::getName, FMode.EQ, NULL).size()).as("EQ null 等价于 IS NULL，应命中 3 行").isEqualTo(3L);
            assertThat(reader.copyReader().filter(Plain::getName, FMode.NE, NULL).size()).as("NE null 等价于 IS NOT NULL，应命中 6 行").isEqualTo(6L);

            assertThat(reader.copyReader().filter(Plain::getName, FMode.NE, "1-NAME").size())
                    .as("NE 不含 null 时会额外拼上 OR IS NULL，3 行 NULL 也算命中，共 8 行").isEqualTo(8L);
            assertThat(reader.copyReader().filter(Plain::getName, FMode.NOT_LIKE, "%-NAME").size())
                    .as("NOT_LIKE 会额外拼上 OR IS NULL，非 NULL 的 6 行全被排除后只剩 3 行 NULL").isEqualTo(3L);

            assertThat(reader.copyReader().filter(Plain::getName, FMode.IN, NULL).size()).as("IN 只含 null 时等价于 IS NULL，应命中 3 行").isEqualTo(3L);
            assertThat(reader.copyReader().filter(Plain::getName, FMode.IN, NULL, "1-NAME").size())
                    .as("IN 含 null 时拼成 (IN (?) OR IS NULL)，应命中 3+1 共 4 行").isEqualTo(4L);

            assertThat(reader.copyReader().filter(Plain::getName, FMode.NOT_IN, NULL).size())
                    .as("NOT_IN 只含 null（单值分支）等价于 IS NOT NULL，应命中 6 行").isEqualTo(6L);
            assertThat(reader.copyReader().filter(Plain::getName, FMode.NOT_IN, NULL, "1-NAME").size())
                    .as("NOT_IN 含 null 且多值（多值分支）只拼 NOT IN (?)，NULL 行被 SQL 三值逻辑排除，应命中 5 行").isEqualTo(5L);
            assertThat(reader.copyReader().filter(Plain::getName, FMode.NOT_IN, "1-NAME", "2-NAME").size())
                    .as("NOT_IN 不含 null 时会额外拼上 OR IS NULL，NULL 行反而被包含，应命中 7 行").isEqualTo(7L);

            // 与 writer 侧一致
            assertThat(pignoo.writer(Plain.class).filter(Plain::getName, FMode.EQ, NULL).size()).as("writer 侧的 EQ null 语义应与 reader 一致").isEqualTo(3L);
        }
    }

    /**
     * 空值集合：IN 空集匹配 0 行，NOT_IN 空集匹配全部
     */
    @Test
    public void emptyValueCollection() {
        try (BasePignoo pignoo = new BasePignoo(this.dataSource, this.config)) {
            prepare(pignoo);
            var reader = pignoo.reader(Plain.class);

            assertThat(reader.copyReader().filter(Plain::getName, FMode.IN, Arrays.asList()).size()).as("IN 空集合应匹配 0 行").isZero();
            assertThat(reader.copyReader().filter(Plain::getName, FMode.NOT_IN, Arrays.asList()).size()).as("NOT_IN 空集合应匹配全部 9 行").isEqualTo(9L);
            assertThat(reader.copyReader().filter(Plain::getName, FMode.IN).size()).as("IN 不传任何值应匹配 0 行").isZero();
            assertThat(reader.copyReader().filter(Plain::getName, FMode.NOT_IN).size()).as("NOT_IN 不传任何值应匹配全部 9 行").isEqualTo(9L);
            assertThat(reader.copyReader().filter(Plain::getName, FMode.IN, Arrays.asList(), new Byte[] {}).size()).as("多个空集合摊平后仍是空集").isZero();
        }
    }

    /**
     * 参数不合法：GT/LT/GE/LE/LIKE/NOT_LIKE 不接受 null，参数个数越界也会报错
     */
    @Test
    public void illegalArguments() {
        try (BasePignoo pignoo = new BasePignoo(this.dataSource, this.config)) {
            prepare(pignoo);
            var reader = pignoo.reader(Plain.class);

            assertThatThrownBy(() -> reader.copyReader().filter(Plain::getName, FMode.GT, NULL).size())
                    .as("GT 传 null 应抛 MapperException").isInstanceOf(MapperException.class);
            assertThatThrownBy(() -> reader.copyReader().filter(Plain::getName, FMode.LT, NULL).size())
                    .as("LT 传 null 应抛 MapperException").isInstanceOf(MapperException.class);
            assertThatThrownBy(() -> reader.copyReader().filter(Plain::getName, FMode.GE, NULL).size())
                    .as("GE 传 null 应抛 MapperException").isInstanceOf(MapperException.class);
            assertThatThrownBy(() -> reader.copyReader().filter(Plain::getName, FMode.LE, NULL).size())
                    .as("LE 传 null 应抛 MapperException").isInstanceOf(MapperException.class);
            assertThatThrownBy(() -> reader.copyReader().filter(Plain::getName, FMode.LIKE, NULL).size())
                    .as("LIKE 传 null 应抛 MapperException").isInstanceOf(MapperException.class);
            assertThatThrownBy(() -> reader.copyReader().filter(Plain::getName, FMode.NOT_LIKE, NULL).size())
                    .as("NOT_LIKE 传 null 应抛 MapperException").isInstanceOf(MapperException.class);

            assertThatThrownBy(() -> reader.copyReader().filter(Plain::getQty, FMode.EQ, 1, 2).size())
                    .as("EQ 传 2 个值超出参数上限，应抛 MapperException").isInstanceOf(MapperException.class);
            assertThatThrownBy(() -> reader.copyReader().filter(Plain::getQty, FMode.NE, 1, 2).size())
                    .as("NE 传 2 个值超出参数上限，应抛 MapperException").isInstanceOf(MapperException.class);
            assertThatThrownBy(() -> reader.copyReader().filter(Plain::getQty, FMode.EQ).size())
                    .as("EQ 不传值低于参数下限，应抛 MapperException").isInstanceOf(MapperException.class);
            assertThatThrownBy(() -> reader.copyReader().filter(Plain::getQty, "无此条件", 1).size())
                    .as("未知的过滤条件名应抛 MapperException").isInstanceOf(MapperException.class);

            assertThat(reader.size()).as("抛异常的查询不应影响数据").isEqualTo(9L);
        }
    }

    /**
     * 值的摊平：集合、对象数组、原始类型数组、可变参数与多层嵌套的结果一致
     */
    @Test
    public void valueFlattening() {
        try (BasePignoo pignoo = new BasePignoo(this.dataSource, this.config)) {
            prepare(pignoo);
            var reader = pignoo.reader(Plain.class);

            assertThat(reader.copyReader().filter(Plain::getQty, FMode.IN, List.of(1, 2, 3)).size()).as("IN 传 List 应命中 3 行").isEqualTo(3L);
            assertThat(reader.copyReader().filter(Plain::getQty, FMode.IN, (Object) new Integer[] { 1, 2, 3 }).size()).as("IN 传 Object[] 应命中 3 行").isEqualTo(3L);
            assertThat(reader.copyReader().filter(Plain::getQty, FMode.IN, new int[] { 1, 2, 3 }).size()).as("IN 传 int[] 应命中 3 行").isEqualTo(3L);
            assertThat(reader.copyReader().filter(Plain::getQty, FMode.IN, 1, 2, 3).size()).as("IN 传可变参数应命中 3 行").isEqualTo(3L);
            assertThat(reader.copyReader().filter(Plain::getQty, FMode.IN, List.of(List.of(1, 2), new int[] { 3 })).size()).as("IN 传多层嵌套的集合与数组应摊平后命中 3 行").isEqualTo(3L);

            assertThat(reader.copyReader().filter(Plain::getQty, FMode.NOT_IN, List.of(1, 2, 3)).size()).as("NOT_IN 传 List 应命中 6 行").isEqualTo(6L);
            assertThat(reader.copyReader().filter(Plain::getQty, FMode.NOT_IN, new int[] { 1, 2, 3 }).size()).as("NOT_IN 传 int[] 应命中 6 行").isEqualTo(6L);
        }
    }

    /**
     * 复合过滤：and / or 嵌套、构建器写法、条件式 filter
     */
    @Test
    public void compositeFilters() {
        try (BasePignoo pignoo = new BasePignoo(this.dataSource, this.config)) {
            prepare(pignoo);
            var reader = pignoo.reader(Plain.class);

            PignooFilter<Plain> filterA = PignooFilter.<Plain>build(Plain::getQty, FMode.EQ, 1).or(Plain::getQty, FMode.EQ, 2);
            PignooFilter<Plain> filterB = PignooFilter.<Plain>build(Plain::getQty, FMode.EQ, 2).or(Plain::getQty, FMode.EQ, 3);
            assertThat(reader.copyReader().filter(filterA).size()).as("(qty=1 or qty=2) 应命中 2 行").isEqualTo(2L);
            assertThat(reader.copyReader().filter(filterA.and(filterB)).size()).as("(qty=1 or qty=2) and (qty=2 or qty=3) 应只命中 qty=2 这 1 行").isEqualTo(1L);
            assertThat(reader.copyReader().filter(filterA.or(filterB)).size()).as("(qty=1 or qty=2) or (qty=2 or qty=3) 应命中 3 行").isEqualTo(3L);

            assertThat(reader.copyReader().filter(f -> f.or(Plain::getQty, "==", 1).or(Plain::getQty, "==", 2)).size())
                    .as("构建器写法的 or 应命中 2 行").isEqualTo(2L);
            assertThat(reader.copyReader().filter(f -> f.and(Plain::getQty, FMode.GE, 3).and(Plain::getQty, FMode.LE, 5)).size())
                    .as("构建器写法的 and 应命中 3 行").isEqualTo(3L);
            assertThat(reader.copyReader().filter(f -> f.or(Plain::getName, FMode.LIKE, "1-%").or(Plain::getName, FMode.EQ, NULL)).size())
                    .as("or 中混用 LIKE 与 EQ null，应命中 1+3 共 4 行").isEqualTo(4L);

            assertThat(reader.copyReader().filter(Plain::getQty, FMode.GE, 3).filter(Plain::getQty, FMode.LE, 5).size())
                    .as("连续调用 filter 是 AND 关系，应命中 3 行").isEqualTo(3L);

            assertThat(reader.copyReader().filter(true, Plain::getQty, FMode.EQ, 1).size()).as("条件为 true 时过滤生效").isEqualTo(1L);
            assertThat(reader.copyReader().filter(false, Plain::getQty, FMode.EQ, 1).size()).as("条件为 false 时过滤不生效").isEqualTo(9L);
            assertThat(reader.copyReader().filter((Boolean) null, Plain::getQty, FMode.EQ, 1).size()).as("条件为 null 时过滤不生效").isEqualTo(9L);
            assertThat(reader.copyReader().filter(true, Plain::getQty, "==", 1).size()).as("条件式 filter 的字符串写法同样生效").isEqualTo(1L);
            assertThat(reader.copyReader().filter(false, Plain::getQty, "==", 1).size()).as("条件式 filter 的字符串写法同样可关闭").isEqualTo(9L);
        }
    }

    /**
     * 空过滤器等价于无过滤器：不能拼出残缺 SQL，reader 与 writer 两侧都要成立
     */
    @Test
    public void emptyFilterEqualsNoFilter() {
        try (BasePignoo pignoo = new BasePignoo(this.dataSource, this.config)) {
            prepare(pignoo);

            assertThat(pignoo.reader(Plain.class).filter(PignooFilter.build(Plain.class)).size()).as("PignooFilter.build(Class) 是空过滤器，应查出全部 9 行").isEqualTo(9L);
            assertThat(pignoo.reader(Plain.class).filter(new PignooFilter<Plain>()).size()).as("new PignooFilter 是空过滤器，应查出全部 9 行").isEqualTo(9L);
            assertThat(pignoo.reader(Plain.class).filter(f -> f).getAll()).as("filter(f -> f) 是空过滤器，应查出全部 9 行").hasSize(9);
            assertThat(pignoo.reader(Plain.class).filter(f -> null).size()).as("构建器返回 null 时应等价于没有过滤器").isEqualTo(9L);
            assertThat(pignoo.reader(Plain.class).filter(f -> f).getAny()).as("空过滤器的 getAny 应能取到数据").isNotNull();
            assertThat(pignoo.reader(Plain.class).filter(f -> f).getFirst()).as("空过滤器的 getFirst 应能取到数据").isNotNull();
            assertThat(pignoo.reader(Plain.class).filter(f -> f).max(Plain::getQty, Integer.class)).as("空过滤器的聚合应作用于全表").isEqualTo(9);

            // 空过滤器 + 非空条件：只剩非空条件生效
            assertThat(pignoo.reader(Plain.class).filter(new PignooFilter<Plain>()).filter(Plain::getQty, FMode.EQ, 1).size())
                    .as("空过滤器不应影响其后追加的条件").isEqualTo(1L);

            Plain mix = new Plain();
            mix.setCode("C-EMPTY-FILTER");
            assertThat(pignoo.writer(Plain.class).filter(PignooFilter.build(Plain.class)).mixAll(mix)).as("writer 侧空过滤器的 mixAll 是整表更新").isEqualTo(9L);
            assertThat(pignoo.reader(Plain.class).filter(Plain::getCode, FMode.EQ, "C-EMPTY-FILTER").size()).as("整表更新后 9 行的 code 都应变化").isEqualTo(9L);
            assertThat(pignoo.writer(Plain.class).filter(f -> f).removeAll()).as("writer 侧空过滤器的 removeAll 是整表删除").isEqualTo(9L);
            assertThat(pignoo.reader(Plain.class).size()).as("整表删除后应为空表").isZero();
        }
    }
}
