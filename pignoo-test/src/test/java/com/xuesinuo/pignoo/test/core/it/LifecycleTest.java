package com.xuesinuo.pignoo.test.core.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.xuesinuo.pignoo.core.Gru;
import com.xuesinuo.pignoo.core.PignooConfig;
import com.xuesinuo.pignoo.core.config.DatabaseEngine;
import com.xuesinuo.pignoo.core.exception.PignooRuntimeException;
import com.xuesinuo.pignoo.core.implement.BasePignoo;
import com.xuesinuo.pignoo.core.implement.TransactionPignoo;
import com.xuesinuo.pignoo.test.fixture.Plain;

/**
 * 连接与关闭语义集成测试：BasePignoo、TransactionPignoo与Gru的生命周期
 * <p>
 * Integration test for connection and close semantics
 */
@SpringBootTest
public class LifecycleTest {

    public final DataSource dataSource;

    public LifecycleTest(@Autowired DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** 引擎预置好的配置：这样TransactionPignoo构造时不会去取连接 */
    private PignooConfig configWithEngine() {
        PignooConfig config = new PignooConfig();
        config.setEngine(DatabaseEngine.MySQL);
        return config;
    }

    /** 清空本类使用的夹具表 */
    private void clearTable() {
        try (var pignoo = new BasePignoo(this.dataSource)) {
            pignoo.writer(Plain.class).removeAll();
        }
    }

    @Test
    public void basePignooCloseSemantics() {
        this.clearTable();
        BasePignoo pignoo = new BasePignoo(this.dataSource);
        assertThat(pignoo.closed()).as("新建的BasePignoo不应处于关闭状态").isFalse();
        assertThat(pignoo.reader(Plain.class).size()).as("关闭前查询应当正常工作").isEqualTo(0L);

        pignoo.close();
        assertThat(pignoo.closed()).as("close()之后closed()应当返回true").isTrue();

        var reader = pignoo.reader(Plain.class);
        var writer = pignoo.writer(Plain.class);
        assertThat(reader).as("close之后仍然能拿到reader，异常推迟到真正执行SQL时才抛").isNotNull();
        assertThat(writer).as("close之后仍然能拿到writer，异常推迟到真正执行SQL时才抛").isNotNull();
        assertThatThrownBy(() -> reader.getAll())
                .as("close之后执行查询应当抛PignooRuntimeException")
                .isInstanceOf(PignooRuntimeException.class);
        assertThatThrownBy(() -> writer.removeAll())
                .as("close之后执行写入应当抛PignooRuntimeException")
                .isInstanceOf(PignooRuntimeException.class);
    }

    @Test
    public void basePignooCloseIsIdempotent() {
        BasePignoo pignoo = new BasePignoo(this.dataSource);
        pignoo.close();
        assertThatCode(pignoo::close).as("重复调用BasePignoo.close()不应抛异常").doesNotThrowAnyException();
        assertThat(pignoo.closed()).as("重复close之后closed()仍然是true").isTrue();
    }

    @Test
    public void transactionPignooCloseSemantics() {
        this.clearTable();
        TransactionPignoo pignoo = new TransactionPignoo(this.dataSource);
        assertThat(pignoo.closed()).as("新建的TransactionPignoo不应处于关闭状态").isFalse();

        Plain plain = new Plain();
        plain.setName("事务内写入");
        pignoo.writer(Plain.class).add(plain);

        pignoo.close();
        assertThat(pignoo.closed()).as("close()之后closed()应当返回true").isTrue();
        assertThatCode(pignoo::close).as("重复调用TransactionPignoo.close()不应抛异常").doesNotThrowAnyException();
        assertThatCode(pignoo::rollback).as("close之后调用rollback()不应抛异常").doesNotThrowAnyException();

        try (var check = new BasePignoo(this.dataSource)) {
            assertThat(check.reader(Plain.class).size()).as("TransactionPignoo正常close应当提交事务").isEqualTo(1L);
        }
    }

    @Test
    public void transactionPignooRollbackIsIdempotent() {
        this.clearTable();
        try (TransactionPignoo pignoo = new TransactionPignoo(this.dataSource, this.configWithEngine())) {
            Plain plain = new Plain();
            plain.setName("将被回滚");
            pignoo.writer(Plain.class).add(plain);
            pignoo.rollback();
            assertThatCode(pignoo::rollback).as("重复调用rollback()应当是幂等的").doesNotThrowAnyException();
        }

        try (var check = new BasePignoo(this.dataSource)) {
            assertThat(check.reader(Plain.class).size()).as("回滚之后事务内的写入不应可见").isEqualTo(0L);
        }
    }

    @Test
    public void emptyTransactionClosesCleanly() {
        // 引擎预置：整个事务过程一条连接都不会取
        TransactionPignoo lazy = new TransactionPignoo(this.dataSource, this.configWithEngine());
        assertThatCode(lazy::close).as("空事务体直接close()不应抛异常").doesNotThrowAnyException();
        assertThatCode(lazy::rollback).as("空事务体close()之后rollback()不应抛异常").doesNotThrowAnyException();

        TransactionPignoo lazyRollbackFirst = new TransactionPignoo(this.dataSource, this.configWithEngine());
        assertThatCode(lazyRollbackFirst::rollback).as("空事务体直接rollback()不应抛异常").doesNotThrowAnyException();
        assertThatCode(lazyRollbackFirst::close).as("空事务体rollback()之后close()不应抛异常").doesNotThrowAnyException();

        // 不预置引擎：构造时就已经取到了连接，空事务体照样要能正常收尾
        TransactionPignoo eager = new TransactionPignoo(this.dataSource);
        assertThatCode(eager::close).as("构造时已取连接的空事务体close()不应抛异常").doesNotThrowAnyException();
        assertThat(eager.closed()).as("空事务体close()之后closed()应当返回true").isTrue();
    }

    @Test
    public void gruRunOverloads() {
        Gru gru = new Gru(this.dataSource);

        gru.run(pignoo -> {
            pignoo.writer(Plain.class).removeAll();
        });

        Long id = gru.run(pignoo -> {
            Plain plain = new Plain();
            plain.setName("Gru");
            return pignoo.writer(Plain.class).add(plain).getId();
        });
        assertThat(id).as("Gru.run(Function)应当把业务返回值透传出来").isNotNull();

        String name = gru.runTransaction(pignoo -> {
            return pignoo.reader(Plain.class).filter(Plain::getId, "==", id).getAny().getName();
        });
        assertThat(name).as("Gru.runTransaction(Function)应当把业务返回值透传出来").isEqualTo("Gru");

        boolean[] executed = { false };
        gru.runTransaction(pignoo -> {
            Plain plain = new Plain();
            plain.setName("Gru2");
            pignoo.writer(Plain.class).add(plain);
            executed[0] = true;
        });
        assertThat(executed[0]).as("Gru.runTransaction(Consumer)应当执行事务体").isTrue();

        long size = gru.run(pignoo -> {
            return pignoo.reader(Plain.class).size();
        });
        assertThat(size).as("四个重载执行完之后应当有两条数据").isEqualTo(2L);
    }
}
