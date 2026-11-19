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
import com.xuesinuo.pignoo.test.fixture.PkAuto;
import com.xuesinuo.pignoo.test.fixture.PkNonAuto;
import com.xuesinuo.pignoo.test.fixture.Plain;

/**
 * 增删查的基础语义（连真实 MySQL）
 * <p>
 * Basic add/select semantics against a real MySQL
 */
@SpringBootTest
public class CrudTest {

    public final DataSource dataSource;
    public PignooConfig config = new PignooConfig();

    public CrudTest(@Autowired DataSource dataSource) {
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
     * 自增主键（默认约定的 id）新增：主键由数据库生成，返回的是按主键反查出来的新实体
     */
    @Test
    public void addWithAutoPrimaryKey() {
        try (BasePignoo pignoo = new BasePignoo(this.dataSource, this.config)) {
            var writer = pignoo.writer(Plain.class);
            writer.removeAll();

            Plain plain = new Plain();
            plain.setName("N1");
            plain.setMoney(new BigDecimal("1.5"));
            Plain added = writer.add(plain);

            assertThat(added).as("add 应返回新增后的实体").isNotNull();
            assertThat(added.getId()).as("自增主键应由数据库生成").isNotNull();
            assertThat(added.getId()).as("自增主键应为正数").isGreaterThan(0L);
            assertThat(added.getName()).as("新增的 name 应被写入").isEqualTo("N1");
            assertThat(plain.getId()).as("add 不回填入参对象的主键，返回的是另一个实体").isNull();
            // money 列定义为 decimal(32,8)：入参 1.5 只有 1 位小数，反查回来会补足到 8 位小数
            assertThat(added.getMoney()).as("money 的值应为 1.5").isEqualByComparingTo(new BigDecimal("1.5"));
            assertThat(added.getMoney().scale()).as("add 返回的是按主键从数据库反查的结果，小数位应为列定义的 8 位").isEqualTo(8);
            assertThat(added.getQty()).as("未赋值的字段在库中为 NULL").isNull();
            assertThat(writer.size()).as("新增后表内应有 1 行").isEqualTo(1L);
        }
    }

    /**
     * 显式标注、且不叫 id 的自增主键，同样能正常新增
     */
    @Test
    public void addWithNonIdAutoPrimaryKey() {
        try (BasePignoo pignoo = new BasePignoo(this.dataSource, this.config)) {
            var writer = pignoo.writer(PkAuto.class);
            writer.removeAll();

            PkAuto pkAuto = new PkAuto();
            pkAuto.setName("A1");
            PkAuto added = writer.add(pkAuto);

            assertThat(added).as("add 应返回新增后的实体").isNotNull();
            assertThat(added.getAutoKey()).as("非 id 命名的自增主键也应由数据库生成").isNotNull();
            assertThat(added.getName()).as("新增的 name 应被写入").isEqualTo("A1");

            PkAuto read = pignoo.reader(PkAuto.class).filter(PkAuto::getAutoKey, "==", added.getAutoKey()).getAny();
            assertThat(read).as("应能按自增主键查回刚新增的行").isNotNull();
            assertThat(read.getName()).as("查回的 name 应与新增一致").isEqualTo("A1");
        }
    }

    /**
     * 非自增主键：必须手工赋主键，主键为 null 时抛 MapperException 且不落库
     */
    @Test
    public void addWithNonAutoPrimaryKey() {
        try (BasePignoo pignoo = new BasePignoo(this.dataSource, this.config)) {
            var writer = pignoo.writer(PkNonAuto.class);
            writer.removeAll();

            PkNonAuto withKey = new PkNonAuto();
            withKey.setBizKey("BK-1");
            withKey.setName("B1");
            PkNonAuto added = writer.add(withKey);
            assertThat(added).as("手工赋主键后应能正常新增").isNotNull();
            assertThat(added.getBizKey()).as("非自增主键应原样保留调用方给定的值").isEqualTo("BK-1");
            assertThat(added.getName()).as("新增的 name 应被写入").isEqualTo("B1");

            PkNonAuto withoutKey = new PkNonAuto();
            withoutKey.setName("B2");
            assertThatThrownBy(() -> writer.add(withoutKey))
                    .as("非自增主键为 null 时新增应抛 MapperException")
                    .isInstanceOf(MapperException.class);

            assertThat(writer.size()).as("主键为 null 的新增不应落库").isEqualTo(1L);
        }
    }

    /**
     * getFirst 受 sorter 影响，getAny 忽略 sorter；getAll / get(offset,limit) / size 的基本行为
     */
    @Test
    public void selectFirstAnyAllAndSize() {
        try (BasePignoo pignoo = new BasePignoo(this.dataSource, this.config)) {
            var writer = pignoo.writer(Plain.class);
            writer.removeAll();
            Plain p1 = addPlain(writer, "N1", "C1", 1, new BigDecimal("10"));
            Plain p2 = addPlain(writer, "N2", "C2", 2, new BigDecimal("20"));
            Plain p3 = addPlain(writer, "N3", "C3", 3, new BigDecimal("30"));

            assertThat(pignoo.reader(Plain.class).size()).as("表内应有 3 行").isEqualTo(3L);
            assertThat(pignoo.reader(Plain.class).getAll()).as("getAll 应返回全部 3 行").hasSize(3);

            Plain maxFirst = pignoo.reader(Plain.class).sort(Plain::getQty, SMode.MAX_FIRST).getFirst();
            assertThat(maxFirst.getId()).as("getFirst 受 sorter 影响：倒序时应取到最后一行").isEqualTo(p3.getId());
            Plain minFirst = pignoo.reader(Plain.class).sort(Plain::getQty, SMode.MIN_FIRST).getFirst();
            assertThat(minFirst.getId()).as("getFirst 受 sorter 影响：正序时应取到第一行").isEqualTo(p1.getId());

            Long anyWithMaxSorter = pignoo.reader(Plain.class).sort(Plain::getQty, SMode.MAX_FIRST).getAny().getId();
            Long anyWithMinSorter = pignoo.reader(Plain.class).sort(Plain::getQty, SMode.MIN_FIRST).getAny().getId();
            Long anyWithoutSorter = pignoo.reader(Plain.class).getAny().getId();
            assertThat(anyWithMaxSorter).as("getAny 忽略 sorter：正反序应取到同一行").isEqualTo(anyWithMinSorter);
            assertThat(anyWithMaxSorter).as("getAny 忽略 sorter：与不带 sorter 的结果一致").isEqualTo(anyWithoutSorter);

            var page = pignoo.reader(Plain.class).sort(Plain::getQty, SMode.MIN_FIRST).get(1, 1);
            assertThat(page).as("get(1,1) 应只返回 1 行").hasSize(1);
            assertThat(page.get(0).getId()).as("get(1,1) 应跳过第 1 行取第 2 行").isEqualTo(p2.getId());

            var page2 = pignoo.reader(Plain.class).sort(Plain::getQty, SMode.MIN_FIRST).get(1, 10);
            assertThat(page2).as("limit 超出剩余行数时只返回剩余行").hasSize(2);
        }
    }

    /**
     * 空表：getFirst / getAny 返回 null，getAll 返回空列表，size 为 0
     */
    @Test
    public void selectOnEmptyTable() {
        try (BasePignoo pignoo = new BasePignoo(this.dataSource, this.config)) {
            pignoo.writer(Plain.class).removeAll();

            var reader = pignoo.reader(Plain.class).sort(Plain::getId, SMode.MIN_FIRST);
            assertThat(reader.getFirst()).as("空表的 getFirst 应为 null").isNull();
            assertThat(reader.getAny()).as("空表的 getAny 应为 null").isNull();
            assertThat(reader.getAll()).as("空表的 getAll 应为空列表").isEmpty();
            assertThat(reader.get(0, 10)).as("空表的 get(offset,limit) 应为空列表").isEmpty();
            assertThat(reader.size()).as("空表的 size 应为 0").isZero();
        }
    }

    /**
     * copyReader / copyWriter 保留已设置的 filter 与 sorter，且副本与原对象互不影响
     */
    @Test
    public void copyKeepsFilterAndSorter() {
        try (BasePignoo pignoo = new BasePignoo(this.dataSource, this.config)) {
            var writer = pignoo.writer(Plain.class);
            writer.removeAll();
            for (int i = 1; i <= 5; i++) {
                addPlain(writer, "N" + i, "C" + i, i, new BigDecimal(i));
            }

            var reader = pignoo.reader(Plain.class).filter(Plain::getQty, ">=", 3).sort(Plain::getQty, SMode.MIN_FIRST);
            assertThat(reader.size()).as("原 reader 命中 3 行").isEqualTo(3L);

            var copiedReader = reader.copyReader();
            assertThat(copiedReader.size()).as("copyReader 应保留已设置的 filter").isEqualTo(3L);
            assertThat(copiedReader.getFirst().getQty()).as("copyReader 应保留已设置的 sorter").isEqualTo(3);

            copiedReader.filter(Plain::getQty, "<=", 4);
            assertThat(copiedReader.size()).as("副本追加条件后应命中 2 行").isEqualTo(2L);
            assertThat(reader.size()).as("在副本上追加条件不应影响原 reader").isEqualTo(3L);

            var copiedWriter = reader.copyWriter();
            assertThat(copiedWriter.size()).as("copyWriter 应保留已设置的 filter").isEqualTo(3L);
            assertThat(copiedWriter.getFirst().getQty()).as("copyWriter 应保留已设置的 sorter").isEqualTo(3);
            assertThat(copiedWriter.removeAll()).as("copyWriter 保留 filter，removeAll 只删命中的 3 行").isEqualTo(3L);
            assertThat(pignoo.reader(Plain.class).size()).as("未命中 filter 的 2 行应保留").isEqualTo(2L);
        }
    }

    /**
     * isReadOnly：reader 只读，writer 可写；copy 出来的对象按目标类型决定
     */
    @Test
    public void readOnlyFlag() {
        try (BasePignoo pignoo = new BasePignoo(this.dataSource, this.config)) {
            assertThat(pignoo.reader(Plain.class).isReadOnly()).as("reader 应为只读").isTrue();
            assertThat(pignoo.writer(Plain.class).isReadOnly()).as("writer 不应为只读").isFalse();
            assertThat(pignoo.reader(Plain.class).copyWriter().isReadOnly()).as("reader.copyWriter 得到的是可写对象").isFalse();
            assertThat(pignoo.writer(Plain.class).copyReader().isReadOnly()).as("writer.copyReader 得到的是只读对象").isTrue();
        }
    }
}
