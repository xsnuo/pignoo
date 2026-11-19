package com.xuesinuo.pignoo.test.spring.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.UnexpectedRollbackException;

import com.xuesinuo.pignoo.core.Pignoo;

import lombok.Data;

/**
 * SpringPignoo事务集成测试：提交、回滚、非事务自动提交、事务隔离与多线程ThreadLocal隔离
 * <p>
 * SpringPignoo transaction integration test
 */
@SpringBootTest
public class SpringTxTest {

    public final Pignoo pignoo;
    public final SpringTxTool tool;

    public SpringTxTest(@Autowired Pignoo pignoo, @Autowired SpringTxTool tool) {
        this.pignoo = pignoo;
        this.tool = tool;
    }

    @Data
    public static class SpringTest01 {
        private Long id;
        private String name;
        private String code;
    }

    /** 业务异常：用来触发Spring事务回滚 */
    public static class BizException extends RuntimeException {
        public BizException(String message) {
            super(message);
        }
    }

    /** 清空本类使用的表：走非事务路径，写完即提交 */
    private void clearTable() {
        this.pignoo.writer(SpringTest01.class).removeAll();
    }

    private static SpringTest01 newRow(String name, String code) {
        SpringTest01 row = new SpringTest01();
        row.setName(name);
        row.setCode(code);
        return row;
    }

    /** 在另一个线程（无Spring事务、另一条连接）上读一次总条数 */
    private long countOutside(ExecutorService pool) {
        Callable<Long> task = () -> this.pignoo.reader(SpringTest01.class).size();
        try {
            return pool.submit(task).get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("事务外读取失败或超时", e);
        }
    }

    /** 带超时的闭锁等待：超时直接失败，不允许无限等待 */
    private static void awaitLatch(CountDownLatch latch, String message) {
        if (!awaitQuietly(latch)) {
            throw new IllegalStateException(message);
        }
    }

    /** 带超时的闭锁等待，超时或被中断返回false */
    private static boolean awaitQuietly(CountDownLatch latch) {
        try {
            return latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static void shutdown(ExecutorService pool) {
        pool.shutdownNow();
        try {
            pool.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    public void transactionalMethodRollsBackOnException() {
        this.clearTable();
        assertThatThrownBy(() -> this.tool.run(pignoo -> {
            pignoo.writer(SpringTest01.class).add(newRow("将被回滚", "R1"));
            throw new BizException("业务失败");
        })).as("@Transactional方法体抛出的业务异常应当原样向外传播")
                .isInstanceOf(BizException.class)
                .hasMessage("业务失败");

        assertThat(this.pignoo.reader(SpringTest01.class).size())
                .as("@Transactional方法抛异常应当由Spring回滚，事务外查不到数据").isEqualTo(0L);
    }

    @Test
    public void transactionalMethodCommitsOnNormalReturn() {
        this.clearTable();
        Long id = this.tool.call(pignoo -> pignoo.writer(SpringTest01.class).add(newRow("已提交", "C1")).getId());
        assertThat(id).as("事务内新增应当拿得到自增主键").isNotNull();

        SpringTest01 back = this.pignoo.reader(SpringTest01.class).filter(SpringTest01::getId, "==", id).getAny();
        assertThat(back).as("@Transactional方法正常结束应当提交，事务外可见").isNotNull();
        assertThat(back.getName()).as("提交后写入的内容应当完整").isEqualTo("已提交");
        assertThat(back.getCode()).as("提交后写入的内容应当完整").isEqualTo("C1");
    }

    @Test
    public void nonTransactionalWriteIsVisibleImmediately() {
        this.clearTable();
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            SpringTest01 added = this.pignoo.writer(SpringTest01.class).add(newRow("非事务写入", "N1"));
            assertThat(added.getId()).as("非事务写入应当拿得到自增主键").isNotNull();

            SpringTest01 back = this.pignoo.reader(SpringTest01.class).filter(SpringTest01::getId, "==", added.getId()).getAny();
            assertThat(back).as("非事务写入在同一线程内应当立即可见").isNotNull();
            assertThat(this.countOutside(pool)).as("非事务写入是自动提交语义，其它线程另一条连接也应当立即看到").isEqualTo(1L);
        } finally {
            shutdown(pool);
        }
    }

    @Test
    public void transactionalWriteIsIsolatedUntilCommit() {
        this.clearTable();
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            long[] seenOutside = new long[1];
            Long id = this.tool.call(pignoo -> {
                SpringTest01 added = pignoo.writer(SpringTest01.class).add(newRow("未提交", "U1"));
                assertThat(pignoo.reader(SpringTest01.class).size())
                        .as("同一事务内应当能看到自己刚写入的数据（事务内连接复用）").isEqualTo(1L);
                seenOutside[0] = this.countOutside(pool);
                return added.getId();
            });

            assertThat(seenOutside[0]).as("事务提交前，事务外的连接不应当看到未提交的数据").isEqualTo(0L);
            SpringTest01 back = this.pignoo.reader(SpringTest01.class).filter(SpringTest01::getId, "==", id).getAny();
            assertThat(back).as("事务提交后事务外应当看得到").isNotNull();
            assertThat(this.countOutside(pool)).as("事务提交后其它线程也应当看得到").isEqualTo(1L);
        } finally {
            shutdown(pool);
        }
    }

    @Test
    public void parallelTransactionsAreIsolatedPerThread() {
        this.clearTable();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch bothWritten = new CountDownLatch(2);
            CountDownLatch release = new CountDownLatch(1);

            Future<?> committer = pool.submit(() -> this.tool.run(pignoo -> {
                pignoo.writer(SpringTest01.class).add(newRow("线程A提交", "A1"));
                bothWritten.countDown();
                awaitLatch(release, "线程A等待放行超时");
            }));
            Future<?> rollbacker = pool.submit(() -> this.tool.run(pignoo -> {
                pignoo.writer(SpringTest01.class).add(newRow("线程B回滚", "B1"));
                bothWritten.countDown();
                awaitLatch(release, "线程B等待放行超时");
                throw new BizException("线程B失败");
            }));

            assertThat(awaitQuietly(bothWritten)).as("两个线程应当在30秒内各自在自己的事务里写入完成").isTrue();
            release.countDown(); // 两个事务同时在飞，此时才各自收尾

            assertThatCode(() -> committer.get(60, TimeUnit.SECONDS)).as("线程A的事务应当正常提交，不抛异常")
                    .doesNotThrowAnyException();
            assertThatThrownBy(() -> rollbacker.get(60, TimeUnit.SECONDS)).as("线程B的事务应当因业务异常而回滚")
                    .hasRootCauseInstanceOf(BizException.class);
        } finally {
            shutdown(pool);
        }

        List<SpringTest01> all = this.pignoo.reader(SpringTest01.class).getAll();
        assertThat(all).as("两个线程各自独立事务，线程B回滚不应当影响线程A提交").hasSize(1);
        assertThat(all.get(0).getName()).as("留下的应当是线程A提交的数据").isEqualTo("线程A提交");
    }

    @Test
    public void pignooBeanStillWorksAfterTransactionCompleted() {
        this.clearTable();
        this.tool.run(pignoo -> pignoo.writer(SpringTest01.class).add(newRow("第一次事务", "T1")));

        assertThat(this.pignoo.closed()).as("事务结束不应当把容器里的共享Pignoo Bean也关掉").isFalse();
        assertThat(this.pignoo.writer(SpringTest01.class).add(newRow("事务之后", "P1")).getId())
                .as("事务结束后同一个Pignoo Bean的非事务写入应当照常工作").isNotNull();

        // 事务结束时closeSpringTransaction会关闭并移除ThreadLocal里的事务Pignoo，
        // 若没移除干净，同一线程的第二个事务会拿到已关闭的实例而失败
        this.tool.run(pignoo -> pignoo.writer(SpringTest01.class).add(newRow("第二次事务", "T2")));
        assertThat(this.pignoo.reader(SpringTest01.class).size())
                .as("同一个Pignoo Bean在事务、非事务之间反复切换应当都能正常写入").isEqualTo(3L);
    }

    @Test
    public void requiresNewInnerCommitsWhenOuterRollsBack() {
        this.clearTable();
        assertThatThrownBy(() -> this.tool.runNested((pignoo, inner) -> {
            pignoo.writer(SpringTest01.class).add(newRow("外层将被回滚", "O1"));
            inner.requiresNew(innerPignoo -> innerPignoo.writer(SpringTest01.class).add(newRow("内层独立提交", "I1")));
            throw new BizException("外层失败");
        })).as("外层事务方法抛出的业务异常应当原样向外传播")
                .isInstanceOf(BizException.class)
                .hasMessage("外层失败");

        List<SpringTest01> all = this.pignoo.reader(SpringTest01.class).getAll();
        assertThat(all).as("内层REQUIRES_NEW是独立事务，外层回滚只应当撤销外层自己写入的那一条").hasSize(1);
        assertThat(all.get(0).getName()).as("留下的应当是内层独立事务提交的数据").isEqualTo("内层独立提交");
        assertThat(all.get(0).getCode()).as("留下的应当是内层独立事务提交的数据").isEqualTo("I1");
    }

    @Test
    public void requiredInnerExceptionRollsBackWholeTransaction() {
        this.clearTable();
        assertThatThrownBy(() -> this.tool.runNested((pignoo, inner) -> {
            pignoo.writer(SpringTest01.class).add(newRow("外层写入", "O2"));
            inner.required(innerPignoo -> {
                innerPignoo.writer(SpringTest01.class).add(newRow("内层写入", "I2"));
                throw new BizException("内层失败");
            });
        })).as("内层事务方法抛出的业务异常应当穿过外层原样向外传播")
                .isInstanceOf(BizException.class)
                .hasMessage("内层失败");

        assertThat(this.pignoo.reader(SpringTest01.class).size())
                .as("内层REQUIRED与外层是同一个事务，内层抛异常应当把外层写入一并回滚").isEqualTo(0L);
    }

    @Test
    public void requiredInnerRollbackOnlyBlocksOuterCommit() {
        this.clearTable();
        assertThatThrownBy(() -> this.tool.runNested((pignoo, inner) -> {
            pignoo.writer(SpringTest01.class).add(newRow("外层写入", "O3"));
            try {
                inner.required(innerPignoo -> {
                    innerPignoo.writer(SpringTest01.class).add(newRow("内层写入", "I3"));
                    throw new BizException("内层失败");
                });
            } catch (BizException e) {
                // 外层吞掉内层异常，但内层已经把共享的事务标记为只回滚
            }
        })).as("内层REQUIRED已把共享事务标记为只回滚，外层即使吞掉异常，提交时也应当失败")
                .isInstanceOf(UnexpectedRollbackException.class);

        assertThat(this.pignoo.reader(SpringTest01.class).size())
                .as("外层吞掉内层异常也救不回来，外层自己的写入同样不可见").isEqualTo(0L);
    }
}
