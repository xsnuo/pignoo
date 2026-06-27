package com.xuesinuo.pignoo.test.core;

import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.xuesinuo.pignoo.core.Gru;
import com.xuesinuo.pignoo.core.PignooConfig;
import com.xuesinuo.pignoo.core.PignooFilter;
import com.xuesinuo.pignoo.core.PignooSorter.SMode;
import com.xuesinuo.pignoo.core.exception.MapperException;
import com.xuesinuo.pignoo.core.implement.BasePignoo;

import lombok.Data;

/**
 * 历史缺陷回归用例（B2 空 filter / B3 空事务 / B7 无无参构造）
 * <p>
 * Regression cases for fixed bugs (B2 empty filter / B3 empty transaction / B7 missing no-arg constructor)
 */
@SpringBootTest
public class Test06_Regression {

    public final DataSource dataSource;
    public final Gru gru;
    public PignooConfig config = new PignooConfig();

    public Test06_Regression(@Autowired DataSource dataSource) {
        this.dataSource = dataSource;
        this.gru = new Gru(dataSource);
    }

    private static String RandomCode(int length) {
        return UUID.randomUUID().toString().toUpperCase().replace("-", "").substring(0, length);
    }

    @Data
    public static class Test04 {
        private Long id;
        private String name;
        private String code;
    }

    /**
     * 无无参构造的实体：仅有有参构造，JVM 不再生成默认无参构造。
     * 不使用 Lombok，避免 @Data/@RequiredArgsConstructor 反而补出无参构造。
     * <p>
     * Entity without a no-arg constructor: only a parameterized constructor is declared,
     * so the JVM does not synthesize a default no-arg constructor.
     */
    public static class NoArgEntity {
        private Long id;
        private String name;

        public NoArgEntity(Long id) {
            this.id = id;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    /**
     * B2：空 filter（非 null 但不含任何条件）。旧代码会无条件拼出 "WHERE "（或主键分支的多余 "AND "），
     * 触发 SQL 语法错误；修复后空 filter 等价于无 filter（整表）。
     * <p>
     * B2: an empty filter (non-null but holding no condition) used to emit a dangling "WHERE "
     * (or a dangling "AND " in the by-id branch) and break the SQL; now it behaves as no filter.
     */
    @Test
    public void emptyFilter() {
        try (BasePignoo pignoo = new BasePignoo(this.dataSource, this.config)) {
            pignoo.writer(Test04.class).removeAll();
            for (int i = 0; i < 3; i++) {
                Test04 t = new Test04();
                t.setName(RandomCode(4));
                t.setCode(RandomCode(5));
                pignoo.writer(Test04.class).add(t);
            }

            // reader 端：空 filter 应等价于整表查询
            long size = pignoo.reader(Test04.class).filter(new PignooFilter<Test04>()).size();
            if (size != 3L) {
                throw new RuntimeException("B2 空 filter 断言失败：reader.size() 期望 3，实际 " + size);
            }
            Test04 any = pignoo.reader(Test04.class).filter(f -> f).getAny();
            if (any == null) {
                throw new RuntimeException("B2 空 filter 断言失败：reader.getAny() 不应为空");
            }

            // writer 端：空 filter 的 mixById 走 "WHERE 主键=? AND ..." 分支
            Test04 first = pignoo.reader(Test04.class).sort(Test04::getId, SMode.MAX_FIRST).getFirst();
            Test04 mix = new Test04();
            mix.setId(first.getId());
            mix.setName("MIXED");
            long mixRow = pignoo.writer(Test04.class).filter(new PignooFilter<Test04>()).mixById(mix);
            if (mixRow != 1L) {
                throw new RuntimeException("B2 空 filter 断言失败：mixById 影响行数期望 1，实际 " + mixRow);
            }

            // writer 端：空 filter 的 mixAll 走 "WHERE ..." 分支（整表更新）
            Test04 mixAll = new Test04();
            mixAll.setCode("ALLCODE");
            long mixAllRow = pignoo.writer(Test04.class).filter(new PignooFilter<Test04>()).mixAll(mixAll);
            if (mixAllRow != 3L) {
                throw new RuntimeException("B2 空 filter 断言失败：mixAll 影响行数期望 3，实际 " + mixAllRow);
            }

            // writer 端：空 filter 的 removeAll（整表删除）
            long removeRow = pignoo.writer(Test04.class).filter(f -> f).removeAll();
            if (removeRow != 3L) {
                throw new RuntimeException("B2 空 filter 断言失败：removeAll 影响行数期望 3，实际 " + removeRow);
            }
        }
    }

    /**
     * B3：空事务（事务体内不访问数据库，连接始终为 null）。旧代码 rollback()/commit() 会对 null 连接 NPE，
     * 并可能泄漏连接；修复后回滚应原样抛出业务异常、正常结束应安静提交、且不泄漏连接。
     * <p>
     * B3: an empty transaction (no DB access, connection stays null). Rollback/commit used to NPE
     * on the null connection and could leak it.
     */
    @Test
    public void emptyTransaction() {
        // 1) 空事务回滚：不碰数据库直接抛异常，应原样抛出业务异常而非 NPE
        boolean caught = false;
        try {
            gru.runTransaction(pignoo -> {
                if (pignoo != null) { // 不执行任何数据库操作，直接触发回滚
                    throw new RuntimeException("空事务回滚");
                }
            });
        } catch (RuntimeException e) {
            caught = true;
            if (!"空事务回滚".equals(e.getMessage())) {
                throw new RuntimeException("B3 空事务回滚断言失败：期望原始业务异常，实际为 " + e);
            }
        }
        if (!caught) {
            throw new RuntimeException("B3 空事务回滚断言失败：未捕获到预期异常");
        }

        // 2) 空事务正常结束：事务体什么都不做，提交路径不应 NPE
        gru.runTransaction(pignoo -> {});

        // 3) 连接未泄漏：空事务后仍能正常写入并读回（块状 lambda 明确匹配 Consumer 重载，避免歧义）
        String code = RandomCode(6);
        gru.runTransaction(pignoo -> {
            Test04 t = new Test04();
            t.setName(RandomCode(4));
            t.setCode(code);
            pignoo.writer(Test04.class).add(t);
        });
        gru.run(pignoo -> {
            long n = pignoo.reader(Test04.class).filter(Test04::getCode, "==", code).size();
            if (n != 1L) {
                throw new RuntimeException("B3 连接泄漏断言失败：空事务后写入未生效，期望 1，实际 " + n);
            }
        });
    }

    /**
     * B7：实体缺少无参构造。旧代码 printStackTrace 后吞成 null / NPE；修复后应抛出 MapperException。
     * <p>
     * B7: an entity missing a no-arg constructor. The old code swallowed it into null/NPE;
     * now a MapperException is thrown.
     */
    @Test
    public void missingNoArgConstructor() {
        try (BasePignoo pignoo = new BasePignoo(this.dataSource, this.config)) {
            boolean caught = false;
            try {
                pignoo.reader(NoArgEntity.class).size();
            } catch (MapperException e) {
                caught = true;
                if (e.getMessage() == null || !e.getMessage().contains("constructor")) {
                    throw new RuntimeException("B7 断言失败：期望无无参构造相关的 MapperException，实际为 " + e.getMessage());
                }
            }
            if (!caught) {
                throw new RuntimeException("B7 断言失败：无无参构造实体未抛出 MapperException");
            }
        }
    }
}
