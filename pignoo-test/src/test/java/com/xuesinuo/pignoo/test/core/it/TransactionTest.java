package com.xuesinuo.pignoo.test.core.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.xuesinuo.pignoo.core.Gru;
import com.xuesinuo.pignoo.test.fixture.Plain;

/**
 * 事务语义集成测试：提交、回滚、异常传播与FOR UPDATE行锁
 * <p>
 * Transaction semantics integration test
 */
@SpringBootTest
public class TransactionTest {

    public final DataSource dataSource;
    public final Gru gru;

    public TransactionTest(@Autowired DataSource dataSource) {
        this.dataSource = dataSource;
        this.gru = new Gru(dataSource);
    }

    /** 业务异常：用来验证事务体抛出的异常是否原样向外传播 */
    public static class BizException extends RuntimeException {
        public BizException(String message) {
            super(message);
        }
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** 清空本类使用的夹具表 */
    private void clearTable() {
        this.gru.run(pignoo -> {
            pignoo.writer(Plain.class).removeAll();
        });
    }

    @Test
    public void transactionCommitsOnNormalReturn() {
        this.clearTable();
        Long id = this.gru.runTransaction(pignoo -> {
            Plain plain = new Plain();
            plain.setName("已提交");
            return pignoo.writer(Plain.class).add(plain).getId();
        });

        this.gru.run(pignoo -> {
            Plain back = pignoo.reader(Plain.class).filter(Plain::getId, "==", id).getAny();
            assertThat(back).as("事务正常结束应当提交，事务外可见").isNotNull();
            assertThat(back.getName()).as("事务提交后写入的内容应当可见").isEqualTo("已提交");
        });
    }

    @Test
    public void transactionRollsBackAndRethrowsOriginalException() {
        this.clearTable();
        assertThatThrownBy(() -> this.gru.runTransaction(pignoo -> {
            Plain plain = new Plain();
            plain.setName("将被回滚");
            pignoo.writer(Plain.class).add(plain);
            if (plain.getName() != null) {
                throw new BizException("业务失败");
            }
        })).as("事务体抛出的业务异常应当原样向外传播，不应被包装成其他异常")
                .isInstanceOf(BizException.class)
                .hasMessage("业务失败");

        this.gru.run(pignoo -> {
            assertThat(pignoo.reader(Plain.class).size()).as("事务体抛异常应当回滚，数据不可见").isEqualTo(0L);
        });
    }

    @Test
    public void emptyTransactionRethrowsOriginalException() {
        assertThatThrownBy(() -> this.gru.runTransaction(pignoo -> {
            if (pignoo != null) {
                throw new BizException("空事务体失败");
            }
        })).as("空事务体（未取过连接）抛异常时应当原样传播，不应变成NullPointerException")
                .isInstanceOf(BizException.class)
                .hasMessage("空事务体失败");
    }

    @Test
    public void nonTransactionalRunKeepsWritesOnException() {
        this.clearTable();
        Long[] holder = new Long[1];
        assertThatThrownBy(() -> this.gru.run(pignoo -> {
            Plain plain = new Plain();
            plain.setName("自动提交");
            holder[0] = pignoo.writer(Plain.class).add(plain).getId();
            if (holder[0] != null) {
                throw new BizException("中途失败");
            }
        })).as("非事务run中途抛出的异常同样应当原样传播")
                .isInstanceOf(BizException.class)
                .hasMessage("中途失败");

        this.gru.run(pignoo -> {
            Plain back = pignoo.reader(Plain.class).filter(Plain::getId, "==", holder[0]).getAny();
            assertThat(back).as("非事务run是自动提交语义，中途抛异常时已执行的写入仍然生效").isNotNull();
            assertThat(back.getName()).as("非事务run中已写入的内容应当保留").isEqualTo("自动提交");
        });
    }

    @Test
    public void forUpdateBlocksConcurrentUpdate() throws Exception {
        this.clearTable();
        Long id = this.gru.run(pignoo -> {
            Plain plain = new Plain();
            plain.setName("锁");
            return pignoo.writer(Plain.class).add(plain).getId();
        });

        long holdMillis = 800L;
        CountDownLatch locked = new CountDownLatch(1);
        // 0:线程1准备释放行锁的时刻 1:线程2发起更新的时刻 2:线程2完成更新的时刻
        long[] times = new long[3];
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> holder = pool.submit(() -> {
                this.gru.runTransaction(pignoo -> {
                    pignoo.writer(Plain.class).filter(Plain::getId, "==", id).getFirst(); // SELECT ... FOR UPDATE
                    locked.countDown();
                    sleepQuietly(holdMillis);
                    times[0] = System.nanoTime();
                });
            });
            assertThat(locked.await(20, TimeUnit.SECONDS)).as("线程1应当在20秒内拿到行锁").isTrue();

            Future<?> updater = pool.submit(() -> {
                this.gru.runTransaction(pignoo -> {
                    Plain plain = new Plain();
                    plain.setId(id);
                    plain.setName("后来者");
                    var writer = pignoo.writer(Plain.class);
                    times[1] = System.nanoTime();
                    writer.mixById(plain);
                    times[2] = System.nanoTime();
                });
            });

            holder.get(60, TimeUnit.SECONDS); // 超时保护：不允许挂起
            updater.get(60, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
            pool.awaitTermination(10, TimeUnit.SECONDS);
        }

        assertThat(times[2]).as("线程2的更新必须等到线程1释放行锁之后才能完成").isGreaterThan(times[0]);
        long blockedMillis = (times[2] - times[1]) / 1000000L;
        assertThat(blockedMillis).as("线程2的更新应当被行锁真正阻塞住，实际只等了" + blockedMillis + "毫秒")
                .isGreaterThanOrEqualTo(holdMillis / 2);

        this.gru.run(pignoo -> {
            Plain back = pignoo.reader(Plain.class).filter(Plain::getId, "==", id).getAny();
            assertThat(back.getName()).as("两个事务都提交后应当是线程2写入的值").isEqualTo("后来者");
        });
    }
}
