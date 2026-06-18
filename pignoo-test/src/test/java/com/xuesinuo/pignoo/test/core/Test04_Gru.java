package com.xuesinuo.pignoo.test.core;

import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.xuesinuo.pignoo.core.Gru;
import com.xuesinuo.pignoo.core.PignooSorter.SMode;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
public class Test04_Gru {

    public final Gru gru;
    public final DataSource dataSource;

    public Test04_Gru(@Autowired DataSource dataSource) {
        this.dataSource = dataSource;
        this.gru = new Gru(dataSource);
    }

    private static String RandomCode(int length) {
        return UUID.randomUUID().toString().toUpperCase().replaceAll("-", "").substring(0, length);
    }

    @Data
    public static class Test04 {
        private Long id;
        private String name;
        private String code;
    }

    @Test
    public void submit() {
        Test04 dataInJava = gru.runTransaction(pignoo -> {
            Test04 dataInDb = new Test04();
            dataInDb.setName(RandomCode(4));
            dataInDb.setCode(RandomCode(5));
            dataInDb = pignoo.writer(Test04.class).add(dataInDb);
            dataInDb.setCode(RandomCode(6));

            var writer = pignoo.writer(Test04.class).sort(Test04::getId, SMode.MAX_FIRST);
            writer.getFirst();
            writer.getAny();

            return dataInDb;
        });
        gru.run(pignoo -> {
            if (!pignoo.reader(Test04.class).sort(Test04::getId, SMode.MAX_FIRST).getFirst().getId().equals(dataInJava.getId())) {
                throw new RuntimeException("submit error 1");
            }
        });
        dataInJava.setName(RandomCode(7));
        gru.run(pignoo -> {
            if (pignoo.reader(Test04.class).filter(Test04::getId, "==", dataInJava.getId()).getFirst().getCode().length() != 6) {
                throw new RuntimeException("submit error 2");
            }
        });
    }

    @Test
    public void rollback() {
        Long[] rollbackId = new Long[1];
        try {
            gru.runTransaction(pignoo -> {
                Test04 test04 = new Test04();
                test04.setName(RandomCode(4));
                test04.setCode(RandomCode(5));
                rollbackId[0] = pignoo.writer(Test04.class).add(test04).getId();
                if (rollbackId[0] >= Long.MIN_VALUE) { // 一定触发回滚
                    throw new RuntimeException("rollback");
                }
            });
        } catch (Exception e) { // gru外catch不影响回滚
            if (e.getMessage().equals("rollback")) {
                gru.run(pignoo -> {
                    Test04 t4 = pignoo.reader(Test04.class).sort(Test04::getId, SMode.MAX_FIRST).getFirst();
                    if (t4 != null && t4.getId().equals(rollbackId[0])) {
                        throw new RuntimeException("Rollback error");
                    }
                });
            } else {
                throw e;
            }
        }
    }

    @Test
    public void breakRun() {
        Long[] rollbackId = new Long[1];
        try {
            gru.run(pignoo -> {
                Test04 test04 = new Test04();
                test04.setName(RandomCode(4));
                test04.setCode(RandomCode(5));
                test04 = pignoo.writer(Test04.class).add(test04);
                rollbackId[0] = test04.getId();
                test04.setCode(RandomCode(6));
                if (rollbackId[0] >= Long.MIN_VALUE) {
                    throw new RuntimeException("break run");
                }
            });
        } catch (Exception e) {
            if (e.getMessage().equals("break run")) {
                gru.run(pignoo -> {
                    Test04 t4 = pignoo.reader(Test04.class).sort(Test04::getId, SMode.MAX_FIRST).getFirst();
                    if (t4 == null || !t4.getId().equals(rollbackId[0]) || t4.getCode().length() != 6) {
                        throw new RuntimeException("Break run error");
                    }
                });
            } else {
                throw e;
            }
        }
    }

    private static volatile int step = 0;

    @Test
    public void forUpdate() {
        gru.run(pignoo -> {
            pignoo.writer(Test04.class).removeAll();
            Test04 t1 = new Test04();
            t1.setId(1L);
            t1.setName(RandomCode(1));
            pignoo.writer(Test04.class).add(t1);
        });
        Long[] times = new Long[7];
        Thread thread1 = new Thread(() -> {
            gru.runTransaction(pignoo -> {
                for (;;) {
                    if (step == 0) {
                        break;
                    }
                }
                times[0] = System.currentTimeMillis();
                Test04 t1 = pignoo.writer(Test04.class).filter(Test04::getId, "==", 1L).getFirst();
                times[1] = System.currentTimeMillis();
                step = step + 1;
                for (;;) {
                    if (step == 2) {
                        break;
                    }
                }
                times[4] = System.currentTimeMillis();
                t1.setName(RandomCode(3));
                times[5] = System.currentTimeMillis();
            });
            step = step + 1;
        });
        Thread thread2Timeout = new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}
            step = step + 1;
        });
        Thread thread2 = new Thread(() -> {
            gru.runTransaction(pignoo -> {
                for (;;) {
                    if (step == 1) {
                        break;
                    }
                }
                Test04 t1 = new Test04();
                t1.setId(1L);
                t1.setName(RandomCode(2));
                times[2] = System.currentTimeMillis();
                thread2Timeout.start(); // 延迟，大概率保证本县城的写入操作能提交到数据库，触发锁定
                times[3] = System.currentTimeMillis();
                pignoo.writer(Test04.class).mixById(t1); // 线程1释放锁后才会执行此操作
                times[6] = System.currentTimeMillis();
            });
            step = step + 1;
        });
        thread2.start();
        thread1.start();
        for (;;) {
            if (step == 4) { // 确保两个事务都提交了，再进行下一步验证
                break;
            }
        }
        gru.run(pignoo -> {
            if (pignoo.reader(Test04.class).filter(Test04::getId, "==", 1L).getFirst().getName().length() != 2) {
                throw new RuntimeException("For-update error 1");
            }
        });
        Long time = 0L;
        for (Long t : times) {
            if (t < time) {
                throw new RuntimeException("For-update error 2");
            }
            time = t;
        }
    }
}
