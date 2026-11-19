package com.xuesinuo.pignoo.test.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.xuesinuo.pignoo.core.entity.SqlParam;

/**
 * SQL参数拼接：占位符与参数下标
 * <p>
 * SQL parameter collecting: the placeholder and the parameter index
 */
public class SqlParamTest {

    /**
     * 新建的参数表是空的
     */
    @Test
    public void emptyAtBeginning() {
        assertTrue(new SqlParam().getParams().isEmpty(), "新建的参数表应为空");
    }

    /**
     * next返回占位符
     */
    @Test
    public void nextReturnsPlaceholder() {
        SqlParam sqlParam = new SqlParam();
        assertEquals("?", sqlParam.next("a"), "next应返回占位符?");
        assertEquals("?", sqlParam.next(1), "next应返回占位符?");
        assertEquals("?", sqlParam.next(null), "next对null值同样返回占位符?");
    }

    /**
     * 参数按调用顺序从0开始编号
     */
    @Test
    public void paramsIndexedByCallOrder() {
        SqlParam sqlParam = new SqlParam();
        sqlParam.next("a");
        sqlParam.next(2);
        sqlParam.next(null);
        sqlParam.next(true);

        assertEquals(4, sqlParam.getParams().size(), "参数个数应等于next的调用次数");
        assertEquals("a", sqlParam.getParams().get(0), "参数下标应从0开始");
        assertEquals(2, sqlParam.getParams().get(1), "参数应按调用顺序编号");
        assertNull(sqlParam.getParams().get(2), "null值应原样存入参数表");
        assertEquals(true, sqlParam.getParams().get(3), "参数应按调用顺序编号");
    }

    /**
     * 参数表保持插入顺序
     */
    @Test
    public void paramsKeepInsertionOrder() {
        SqlParam sqlParam = new SqlParam();
        for (int i = 0; i < 5; i++) {
            sqlParam.next("v" + i);
        }
        assertEquals(List.of(0, 1, 2, 3, 4), new ArrayList<>(sqlParam.getParams().keySet()), "参数表应保持插入顺序");
        assertEquals(List.of("v0", "v1", "v2", "v3", "v4"), new ArrayList<>(sqlParam.getParams().values()), "参数表应保持插入顺序");
    }

    /**
     * 多个参数表之间互不干扰
     */
    @Test
    public void independentBetweenInstances() {
        SqlParam one = new SqlParam();
        SqlParam another = new SqlParam();
        one.next("a");
        one.next("b");
        another.next("c");

        assertEquals(2, one.getParams().size(), "参数下标是实例级的，不应共享");
        assertEquals(1, another.getParams().size(), "参数下标是实例级的，不应共享");
        assertEquals("c", another.getParams().get(0), "新参数表的下标应从0重新开始");
    }
}
