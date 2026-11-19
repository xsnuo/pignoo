package com.xuesinuo.pignoo.test.core.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.xuesinuo.pignoo.core.PignooConfig;
import com.xuesinuo.pignoo.core.PignooWriter;
import com.xuesinuo.pignoo.core.exception.MapperException;
import com.xuesinuo.pignoo.core.implement.BasePignoo;
import com.xuesinuo.pignoo.test.fixture.PkNonAuto;
import com.xuesinuo.pignoo.test.fixture.Plain;

/**
 * containsId / containsIds 的语义：含主键为 null、集合含 null、主键重复等边界
 * <p>
 * Semantics of containsId / containsIds, including null primary keys, null elements and duplicated keys
 */
@SpringBootTest
public class ContainsTest {

    /** 显式的 null 值，避免可变参数把裸 null 当成空数组 */
    public static final Object NULL = null;

    public final DataSource dataSource;
    public PignooConfig config = new PignooConfig();

    public ContainsTest(@Autowired DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private static Plain addPlain(PignooWriter<Plain> writer, String name, Integer qty) {
        Plain plain = new Plain();
        plain.setName(name);
        plain.setQty(qty);
        plain.setMoney(new BigDecimal(qty));
        return writer.add(plain);
    }

    private static Plain withId(Long id) {
        Plain plain = new Plain();
        plain.setId(id);
        return plain;
    }

    /**
     * containsId：存在为 true、不存在为 false、传 null 实体为 false、主键为 null 抛异常
     */
    @Test
    public void containsId() {
        try (BasePignoo pignoo = new BasePignoo(this.dataSource, this.config)) {
            var writer = pignoo.writer(Plain.class);
            writer.removeAll();
            Plain p1 = addPlain(writer, "N1", 1);
            Plain p2 = addPlain(writer, "N2", 2);
            var reader = pignoo.reader(Plain.class);

            assertThat(reader.containsId(p1)).as("已存在的主键应为 true").isTrue();
            assertThat(reader.containsId(withId(p2.getId()))).as("只带主键的实体也能判断，应为 true").isTrue();
            assertThat(reader.containsId(withId(p2.getId() + 1000L))).as("不存在的主键应为 false").isFalse();
            assertThat(reader.containsId(null)).as("传 null 实体应为 false").isFalse();

            assertThatThrownBy(() -> reader.containsId(new Plain()))
                    .as("主键为 null 时 containsId 应抛 MapperException").isInstanceOf(MapperException.class);
        }
    }

    /**
     * containsIds：全部存在才为 true；集合为 null/空、含 null 元素、含 null 主键、主键重复都为 false
     */
    @Test
    public void containsIds() {
        try (BasePignoo pignoo = new BasePignoo(this.dataSource, this.config)) {
            var writer = pignoo.writer(Plain.class);
            writer.removeAll();
            Plain p1 = addPlain(writer, "N1", 1);
            Plain p2 = addPlain(writer, "N2", 2);
            Plain p3 = addPlain(writer, "N3", 3);
            var reader = pignoo.reader(Plain.class);

            assertThat(reader.containsIds(List.of(p1, p2, p3))).as("全部存在时应为 true").isTrue();
            assertThat(reader.containsIds(List.of(p1))).as("单个存在时应为 true").isTrue();
            assertThat(reader.containsIds(List.of(p1, withId(p3.getId() + 1000L)))).as("含不存在的主键时应为 false").isFalse();
            assertThat(reader.containsIds(null)).as("集合为 null 时应为 false").isFalse();
            assertThat(reader.containsIds(List.of())).as("集合为空时应为 false").isFalse();

            List<Plain> withNullElement = new ArrayList<>(List.of(p1, p2));
            withNullElement.add(null);
            assertThat(reader.containsIds(withNullElement)).as("集合含 null 元素时会因数量对不上而为 false").isFalse();

            List<Plain> withNullPrimaryKey = new ArrayList<>(List.of(p1, p2));
            withNullPrimaryKey.add(new Plain());
            assertThat(reader.containsIds(withNullPrimaryKey)).as("集合含主键为 null 的元素时返回 false，不抛异常").isFalse();

            assertThat(reader.containsIds(List.of(p1, p1)))
                    .as("集合含重复主键时，去重后数量与集合大小对不上，即使数据存在也返回 false").isFalse();
        }
    }

    /**
     * containsId / containsIds 与 filter 组合：filter 排除该行时应为 false
     */
    @Test
    public void containsCombinedWithFilter() {
        try (BasePignoo pignoo = new BasePignoo(this.dataSource, this.config)) {
            var writer = pignoo.writer(Plain.class);
            writer.removeAll();
            Plain p1 = addPlain(writer, "N1", 1);
            Plain p2 = addPlain(writer, "N2", 2);

            assertThat(pignoo.reader(Plain.class).filter(Plain::getQty, "==", 1).containsId(p1)).as("filter 命中该行时应为 true").isTrue();
            assertThat(pignoo.reader(Plain.class).filter(Plain::getQty, "==", 2).containsId(p1)).as("filter 排除该行时应为 false").isFalse();

            assertThat(pignoo.reader(Plain.class).filter(Plain::getQty, ">=", 1).containsIds(List.of(p1, p2))).as("filter 命中全部行时应为 true").isTrue();
            assertThat(pignoo.reader(Plain.class).filter(Plain::getQty, ">=", 2).containsIds(List.of(p1, p2))).as("filter 排除其中一行时应为 false").isFalse();
            assertThat(pignoo.reader(Plain.class).filter(Plain::getName, "==", NULL).containsIds(List.of(p1, p2))).as("filter 排除全部行时应为 false").isFalse();

            assertThat(pignoo.writer(Plain.class).containsId(p1)).as("writer 侧的 containsId 语义应与 reader 一致").isTrue();
        }
    }

    /**
     * 非自增（字符串）主键的 containsId / containsIds
     */
    @Test
    public void containsOnNonAutoPrimaryKey() {
        try (BasePignoo pignoo = new BasePignoo(this.dataSource, this.config)) {
            var writer = pignoo.writer(PkNonAuto.class);
            writer.removeAll();
            PkNonAuto first = new PkNonAuto();
            first.setBizKey("BK-1");
            first.setName("B1");
            writer.add(first);
            PkNonAuto second = new PkNonAuto();
            second.setBizKey("BK-2");
            second.setName("B2");
            writer.add(second);

            PkNonAuto missing = new PkNonAuto();
            missing.setBizKey("BK-404");
            var reader = pignoo.reader(PkNonAuto.class);

            assertThat(reader.containsId(first)).as("字符串主键存在时应为 true").isTrue();
            assertThat(reader.containsId(missing)).as("字符串主键不存在时应为 false").isFalse();
            assertThat(reader.containsIds(List.of(first, second))).as("字符串主键全部存在时应为 true").isTrue();
            assertThat(reader.containsIds(List.of(first, missing))).as("字符串主键含不存在的值时应为 false").isFalse();
            assertThatThrownBy(() -> reader.containsId(new PkNonAuto()))
                    .as("字符串主键为 null 时 containsId 应抛 MapperException").isInstanceOf(MapperException.class);
        }
    }
}
