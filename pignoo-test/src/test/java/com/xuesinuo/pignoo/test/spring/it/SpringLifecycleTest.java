package com.xuesinuo.pignoo.test.spring.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.xuesinuo.pignoo.core.Pignoo;
import com.xuesinuo.pignoo.core.PignooConfig;
import com.xuesinuo.pignoo.core.config.DatabaseEngine;
import com.xuesinuo.pignoo.core.exception.DataSourceException;
import com.xuesinuo.pignoo.core.exception.PignooRuntimeException;
import com.xuesinuo.pignoo.spring.implement.SpringPignoo;

import lombok.Data;

/**
 * SpringPignoo生命周期集成测试：构造校验、引擎探测与关闭语义
 * <p>
 * SpringPignoo lifecycle integration test
 * <p>
 * 关闭语义一律用自己new出来的独立实例来验证，容器里共享的Pignoo Bean绝不能关。
 */
@SpringBootTest
public class SpringLifecycleTest {

    public final DataSource dataSource;
    public final Pignoo sharedPignoo;

    public SpringLifecycleTest(@Autowired DataSource dataSource, @Autowired Pignoo sharedPignoo) {
        this.dataSource = dataSource;
        this.sharedPignoo = sharedPignoo;
    }

    @Data
    public static class SpringTest01 {
        private Long id;
        private String name;
        private String code;
    }

    /** 清空本类使用的表 */
    private void clearTable() {
        try (SpringPignoo pignoo = new SpringPignoo(this.dataSource)) {
            pignoo.writer(SpringTest01.class).removeAll();
        }
    }

    private static SpringTest01 newRow(String name, String code) {
        SpringTest01 row = new SpringTest01();
        row.setName(name);
        row.setCode(code);
        return row;
    }

    /** 伪造一个只会回答数据库产品名的DataSource，用来构造引擎探测失败的场景 */
    private static DataSource fakeDataSource(String productName) {
        ClassLoader loader = SpringLifecycleTest.class.getClassLoader();
        DatabaseMetaData metaData = (DatabaseMetaData) Proxy.newProxyInstance(loader, new Class<?>[] { DatabaseMetaData.class },
                answer("getDatabaseProductName", productName));
        Connection connection = (Connection) Proxy.newProxyInstance(loader, new Class<?>[] { Connection.class },
                answer("getMetaData", metaData));
        return (DataSource) Proxy.newProxyInstance(loader, new Class<?>[] { DataSource.class },
                answer("getConnection", connection));
    }

    /** 只应答一个方法，其余方法一律给零值 */
    private static InvocationHandler answer(String methodName, Object result) {
        return (proxy, method, args) -> {
            if (methodName.equals(method.getName())) {
                return result;
            }
            switch (method.getName()) {
            case "equals":
                return proxy == args[0];
            case "hashCode":
                return System.identityHashCode(proxy);
            case "toString":
                return "FakeDataSource";
            default:
                break;
            }
            Class<?> returnType = method.getReturnType();
            if (void.class.equals(returnType)) {
                return null;
            }
            return Array.get(Array.newInstance(returnType, 1), 0); // 借一个数组元素拿该返回类型的零值
        };
    }

    @Test
    public void newInstanceDetectsEngineAndWorks() {
        this.clearTable();
        SpringPignoo pignoo = new SpringPignoo(this.dataSource);
        try {
            assertThat(pignoo.closed()).as("新建的SpringPignoo不应处于关闭状态").isFalse();

            SpringTest01 added = pignoo.writer(SpringTest01.class).add(newRow("自动探测引擎", "E1"));
            assertThat(added.getId()).as("自动探测数据库引擎之后writer应当能正常写入").isNotNull();

            SpringTest01 back = pignoo.reader(SpringTest01.class).filter(SpringTest01::getId, "==", added.getId()).getAny();
            assertThat(back).as("自动探测数据库引擎之后reader应当能读回数据").isNotNull();
            assertThat(back.getName()).as("读回的内容应当与写入一致").isEqualTo("自动探测引擎");
        } finally {
            pignoo.close();
        }
    }

    @Test
    public void explicitEngineIsAcceptedAndConfigIsCopied() {
        this.clearTable();
        PignooConfig config = new PignooConfig();
        config.setEngine(DatabaseEngine.MySQL);
        try (SpringPignoo pignoo = new SpringPignoo(this.dataSource, config)) {
            assertThat(pignoo.writer(SpringTest01.class).add(newRow("显式指定引擎", "E2")).getId())
                    .as("显式指定的引擎与实际一致时应当被接受并正常工作").isNotNull();
        }

        PignooConfig blank = new PignooConfig();
        try (SpringPignoo pignoo = new SpringPignoo(this.dataSource, blank)) {
            assertThat(pignoo.reader(SpringTest01.class).size()).as("未指定引擎时应当探测出引擎并正常工作").isEqualTo(1L);
        }
        assertThat(blank.getEngine()).as("构造时应当复制配置，不应把探测到的引擎回写进调用方传入的PignooConfig").isNull();
    }

    @Test
    public void constructorRejectsNullDataSource() {
        assertThatThrownBy(() -> new SpringPignoo(null)).as("数据源为null时应当抛DataSourceException")
                .isInstanceOf(DataSourceException.class)
                .hasMessage("Unknown dataSource");
    }

    @Test
    public void constructorRejectsUnrecognizedEngine() {
        assertThatThrownBy(() -> new SpringPignoo(fakeDataSource("PostgreSQL")))
                .as("数据源报出的产品名不是已支持的引擎时应当抛DataSourceException")
                .isInstanceOf(DataSourceException.class)
                .hasMessage("Unknown database engine");

        // 注意：DatabaseEngine目前只有MySQL一个取值，"探测到的引擎非空且与配置指定的引擎不同"这一条无法构造，
        // 指定MySQL配上非MySQL数据源，也是先在引擎探测这一步就被拒掉
        PignooConfig config = new PignooConfig();
        config.setEngine(DatabaseEngine.MySQL);
        assertThatThrownBy(() -> new SpringPignoo(fakeDataSource("PostgreSQL"), config))
                .as("显式指定MySQL但数据源不是MySQL时，会先在引擎探测阶段被拒绝")
                .isInstanceOf(DataSourceException.class)
                .hasMessage("Unknown database engine");
    }

    @Test
    public void closeSemantics() {
        this.clearTable();
        SpringPignoo pignoo = new SpringPignoo(this.dataSource);
        assertThat(pignoo.closed()).as("新建的SpringPignoo不应处于关闭状态").isFalse();
        pignoo.writer(SpringTest01.class).add(newRow("关闭前写入", "L1"));

        pignoo.close();
        assertThat(pignoo.closed()).as("close()之后closed()应当返回true").isTrue();
        assertThatThrownBy(() -> pignoo.writer(SpringTest01.class))
                .as("SpringPignoo关闭后取writer就直接抛异常，不像BasePignoo那样推迟到执行SQL时")
                .isInstanceOf(PignooRuntimeException.class)
                .hasMessage("Pignoo-Spring has closed!");
        assertThatThrownBy(() -> pignoo.reader(SpringTest01.class))
                .as("SpringPignoo关闭后取reader同样直接抛异常")
                .isInstanceOf(PignooRuntimeException.class)
                .hasMessage("Pignoo-Spring has closed!");
        assertThatCode(pignoo::close).as("重复调用close()不应抛异常").doesNotThrowAnyException();
        assertThatCode(pignoo::closeSpringTransaction).as("没有事务时调用closeSpringTransaction()不应抛异常")
                .doesNotThrowAnyException();
        assertThat(pignoo.closed()).as("重复close()之后closed()仍然是true").isTrue();

        try (SpringPignoo check = new SpringPignoo(this.dataSource)) {
            assertThat(check.reader(SpringTest01.class).size()).as("非事务写入是自动提交语义，close()不影响已写入的数据").isEqualTo(1L);
        }
    }

    @Test
    public void closingOwnInstanceDoesNotAffectSharedBean() {
        this.clearTable();
        SpringPignoo independent = new SpringPignoo(this.dataSource);
        independent.writer(SpringTest01.class).add(newRow("独立实例", "S1"));
        independent.close();

        assertThat(this.sharedPignoo.closed()).as("关闭自己new出来的实例不应当影响容器里共享的Pignoo Bean").isFalse();
        assertThat(this.sharedPignoo.reader(SpringTest01.class).size()).as("共享的Pignoo Bean应当仍然能正常读").isEqualTo(1L);
        assertThat(this.sharedPignoo.writer(SpringTest01.class).add(newRow("共享Bean", "S2")).getId())
                .as("共享的Pignoo Bean应当仍然能正常写").isNotNull();
    }
}
