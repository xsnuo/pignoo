package com.xuesinuo.pignoo.test.core.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.xuesinuo.pignoo.core.Pignoo;
import com.xuesinuo.pignoo.core.PignooSorter.SMode;
import com.xuesinuo.pignoo.core.implement.BasePignoo;
import com.xuesinuo.pignoo.test.fixture.Plain;

/**
 * 遍历器集成测试：真实MySQL上验证keyset分页遍历的完整性、顺序、边界与remove语义
 * <p>
 * Iterator integration test for the keyset pagination iterator
 */
@SpringBootTest
public class IteratorTest {

    public final DataSource dataSource;

    public IteratorTest(@Autowired DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 清空夹具表并按顺序造n条数据
     *
     * @return 自增主键，按写入顺序（即升序）排列
     */
    private List<Long> seed(Pignoo pignoo, int n) {
        var writer = pignoo.writer(Plain.class);
        writer.removeAll();
        List<Long> ids = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            Plain plain = new Plain();
            plain.setName("N" + i);
            plain.setQty(i);
            ids.add(writer.add(plain).getId());
        }
        return ids;
    }

    /** 把遍历器走完，收集主键 */
    private List<Long> drain(Iterator<Plain> iterator) {
        List<Long> ids = new ArrayList<>();
        while (iterator.hasNext()) {
            ids.add(iterator.next().getId());
        }
        return ids;
    }

    @Test
    public void forEachVisitsEveryRowExactlyOnce() {
        try (var pignoo = new BasePignoo(this.dataSource)) {
            List<Long> ids = this.seed(pignoo, 10);
            var reader = pignoo.reader(Plain.class);
            reader.setIteratorStep(3); // 步长明显小于总数，强制翻多页

            List<Long> visited = new ArrayList<>();
            List<String> names = new ArrayList<>();
            for (Plain plain : reader) {
                visited.add(plain.getId());
                names.add(plain.getName());
            }

            assertThat(visited).as("for-each应当不重不漏地遍历全部数据").containsExactlyElementsOf(ids);
            assertThat(names).as("for-each遍历到的内容应当与写入内容一致")
                    .containsExactly("N1", "N2", "N3", "N4", "N5", "N6", "N7", "N8", "N9", "N10");
        }
    }

    @Test
    public void iteratorSortModes() {
        try (var pignoo = new BasePignoo(this.dataSource)) {
            List<Long> ids = this.seed(pignoo, 7);
            var reader = pignoo.reader(Plain.class);

            List<Long> asc = this.drain(reader.iterator(2, SMode.MIN_FIRST));
            assertThat(asc).as("MIN_FIRST应当按主键升序遍历").containsExactlyElementsOf(ids);

            List<Long> desc = this.drain(reader.iterator(2, SMode.MAX_FIRST));
            List<Long> expectedDesc = new ArrayList<>(ids);
            Collections.reverse(expectedDesc);
            assertThat(desc).as("MAX_FIRST应当按主键降序遍历").containsExactlyElementsOf(expectedDesc);
        }
    }

    @Test
    public void iteratorOffsetAndLimit() {
        try (var pignoo = new BasePignoo(this.dataSource)) {
            List<Long> ids = this.seed(pignoo, 10);
            var reader = pignoo.reader(Plain.class);

            List<Long> asc = this.drain(reader.iterator(3, SMode.MIN_FIRST, 2, 5));
            assertThat(asc).as("升序遍历时offset应当跳过前2条、limit应当只取5条")
                    .containsExactly(ids.get(2), ids.get(3), ids.get(4), ids.get(5), ids.get(6));

            List<Long> desc = this.drain(reader.iterator(3, SMode.MAX_FIRST, 2, 4));
            assertThat(desc).as("降序遍历时offset与limit同样生效")
                    .containsExactly(ids.get(7), ids.get(6), ids.get(5), ids.get(4));

            List<Long> zeroLimit = this.drain(reader.iterator(3, SMode.MIN_FIRST, 0, 0));
            assertThat(zeroLimit).as("limit为0时应当一条都不返回").isEmpty();
        }
    }

    @Test
    public void iteratorHandlesPageBoundaries() {
        try (var pignoo = new BasePignoo(this.dataSource)) {
            int step = 3;
            int[] counts = { 0, 1, 2, 3, 4, 5, 6, 7, 9, 12 }; // 覆盖恰好整除step、step+1等边界
            for (int count : counts) {
                List<Long> ids = this.seed(pignoo, count);
                Iterator<Plain> iterator = pignoo.reader(Plain.class).iterator(step, SMode.MIN_FIRST);
                if (count == 0) {
                    assertThat(iterator.hasNext()).as("空表的hasNext()应当为false").isFalse();
                }
                List<Long> visited = this.drain(iterator);
                assertThat(visited).as("数据量为" + count + "时应当不重不漏地遍历完").isEqualTo(ids);
                assertThatThrownBy(iterator::next)
                        .as("数据量为" + count + "时，遍历结束后再调用next()应当抛NoSuchElementException")
                        .isInstanceOf(NoSuchElementException.class);
            }
        }
    }

    @Test
    public void removeSemantics() {
        try (var pignoo = new BasePignoo(this.dataSource)) {
            this.seed(pignoo, 5);

            Iterator<Plain> readOnly = pignoo.reader(Plain.class).iterator(2, SMode.MIN_FIRST);
            assertThatThrownBy(readOnly::remove)
                    .as("只读reader的遍历器不支持remove()")
                    .isInstanceOf(UnsupportedOperationException.class);
            readOnly.next();
            assertThatThrownBy(readOnly::remove)
                    .as("只读reader的遍历器即使调用过next()也不支持remove()")
                    .isInstanceOf(UnsupportedOperationException.class);

            Iterator<Plain> writable = pignoo.writer(Plain.class).iterator(2, SMode.MIN_FIRST);
            assertThatThrownBy(writable::remove)
                    .as("writer的遍历器未调用next()就remove()应当抛IllegalStateException")
                    .isInstanceOf(IllegalStateException.class);

            assertThat(pignoo.reader(Plain.class).size()).as("失败的remove不应删除任何数据").isEqualTo(5L);
        }
    }

    @Test
    public void writerIteratorRemoveDeletesRows() {
        try (var pignoo = new BasePignoo(this.dataSource)) {
            this.seed(pignoo, 5);

            Iterator<Plain> iterator = pignoo.writer(Plain.class).iterator(2, SMode.MIN_FIRST);
            int removed = 0;
            while (iterator.hasNext()) {
                iterator.next();
                iterator.remove();
                removed++;
            }

            assertThat(removed).as("writer的遍历器应当能遍历并删除全部5条").isEqualTo(5);
            assertThat(pignoo.reader(Plain.class).size()).as("全部remove之后表里不应剩下数据").isEqualTo(0L);
        }
    }

    @Test
    public void iteratorCombinesWithFilter() {
        try (var pignoo = new BasePignoo(this.dataSource)) {
            List<Long> ids = this.seed(pignoo, 10);
            var reader = pignoo.reader(Plain.class).filter(Plain::getQty, ">=", 6);

            List<Long> visited = this.drain(reader.iterator(2, SMode.MIN_FIRST));
            assertThat(visited).as("游标条件与过滤条件应当同时生效")
                    .containsExactly(ids.get(5), ids.get(6), ids.get(7), ids.get(8), ids.get(9));

            List<Long> limited = this.drain(reader.iterator(2, SMode.MAX_FIRST, 1, 2));
            assertThat(limited).as("过滤条件与offset、limit应当叠加生效")
                    .containsExactly(ids.get(8), ids.get(7));
        }
    }

    @Test
    public void iteratorSettings() {
        try (var pignoo = new BasePignoo(this.dataSource)) {
            List<Long> ids = this.seed(pignoo, 5);
            var reader = pignoo.reader(Plain.class);

            assertThat(reader.getIteratorStep()).as("遍历器默认步长应当是100").isEqualTo(100);
            assertThat(reader.getIteratorSortMode()).as("遍历器默认排序方式应当是MIN_FIRST").isEqualTo(SMode.MIN_FIRST);

            reader.setIteratorStep(2);
            assertThat(reader.getIteratorStep()).as("setIteratorStep之后getIteratorStep应当返回新值").isEqualTo(2);

            reader.setIteratorSortMode(SMode.MAX_FIRST);
            assertThat(reader.getIteratorSortMode()).as("setIteratorSortMode之后getIteratorSortMode应当返回新值").isEqualTo(SMode.MAX_FIRST);

            assertThatThrownBy(() -> reader.setIteratorSortMode(null))
                    .as("setIteratorSortMode(null)应当抛IllegalArgumentException")
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(reader.getIteratorSortMode()).as("设置null失败后不应改变原有排序方式").isEqualTo(SMode.MAX_FIRST);

            List<Long> expectedDesc = new ArrayList<>(ids);
            Collections.reverse(expectedDesc);
            assertThat(this.drain(reader.iterator())).as("无参iterator()应当使用已配置的步长与排序方式").containsExactlyElementsOf(expectedDesc);
            assertThat(this.drain(reader.iterator(2, null))).as("排序方式传null时应当回退到已配置的排序方式").containsExactlyElementsOf(expectedDesc);
        }
    }
}
