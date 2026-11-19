package com.xuesinuo.pignoo.test.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import com.xuesinuo.pignoo.core.EntityFunction;
import com.xuesinuo.pignoo.core.PignooSorter;
import com.xuesinuo.pignoo.core.PignooSorter.SMode;
import com.xuesinuo.pignoo.test.fixture.Plain;

/**
 * PignooSorter的纯模型行为：链式结构与拷贝
 * <p>
 * Pure model behaviours of PignooSorter: the chained structure and the copy
 */
public class SorterModelTest {

    /**
     * 排序方式只有两个取值
     */
    @Test
    public void sModeValues() {
        assertEquals(2, SMode.values().length, "排序方式应只有两个取值");
        assertSame(SMode.MIN_FIRST, SMode.valueOf("MIN_FIRST"), "小的在前应为MIN_FIRST");
        assertSame(SMode.MAX_FIRST, SMode.valueOf("MAX_FIRST"), "大的在前应为MAX_FIRST");
    }

    /**
     * build出来的基本结构
     */
    @Test
    public void buildStructure() {
        EntityFunction<Plain, ?> nameGetter = Plain::getName;
        PignooSorter<Plain> sorter = PignooSorter.build(nameGetter, SMode.MIN_FIRST);
        assertSame(nameGetter, sorter.getField(), "field应原样保留传入的getter方法引用");
        assertSame(SMode.MIN_FIRST, sorter.getMode(), "mode应原样保留");
        assertNull(sorter.getOtherPignooSorter(), "单级排序不应挂上一级排序");
    }

    /**
     * then链式：返回的是新的排序器，并反向指回上一级
     */
    @Test
    public void thenChain() {
        PignooSorter<Plain> first = PignooSorter.build(Plain::getName, SMode.MIN_FIRST);
        PignooSorter<Plain> second = first.then(Plain::getId, SMode.MAX_FIRST);

        assertNotSame(first, second, "then应返回新的排序器而不是修改自身");
        assertSame(SMode.MAX_FIRST, second.getMode(), "新排序器应保留传入的排序方式");
        assertSame(first, second.getOtherPignooSorter(), "新排序器应反向指回上一级排序器");
        assertNull(first.getOtherPignooSorter(), "上一级排序器不应被改写");

        PignooSorter<Plain> third = second.then(Plain::getCode, SMode.MIN_FIRST);
        assertSame(second, third.getOtherPignooSorter(), "三级链应逐级反向串起来");
        assertSame(first, third.getOtherPignooSorter().getOtherPignooSorter(), "三级链应逐级反向串起来");
    }

    /**
     * then(PignooSorter)重载：直接改写传入的排序器并把它返回
     */
    @Test
    public void thenWithSorter() {
        PignooSorter<Plain> first = PignooSorter.build(Plain::getName, SMode.MIN_FIRST);
        PignooSorter<Plain> next = PignooSorter.build(Plain::getId, SMode.MAX_FIRST);
        PignooSorter<Plain> result = first.then(next);

        assertSame(next, result, "then(PignooSorter)返回的就是传入的排序器本身");
        assertSame(first, next.getOtherPignooSorter(), "传入的排序器会被直接改写，指回上一级");
        assertNull(first.getOtherPignooSorter(), "上一级排序器不应被改写");
    }

    /**
     * copy是深拷贝：整条链逐级拷贝
     */
    @Test
    public void copyDeep() {
        EntityFunction<Plain, ?> nameGetter = Plain::getName;
        PignooSorter<Plain> first = PignooSorter.build(nameGetter, SMode.MIN_FIRST);
        PignooSorter<Plain> second = first.then(Plain::getId, SMode.MAX_FIRST);
        PignooSorter<Plain> copied = PignooSorter.copy(second);

        assertNotSame(second, copied, "copy应返回新实例");
        assertSame(second.getField(), copied.getField(), "field是方法引用，按引用复制");
        assertSame(SMode.MAX_FIRST, copied.getMode(), "mode应被复制");
        assertNotSame(first, copied.getOtherPignooSorter(), "上一级排序器也应被拷贝成新实例");
        assertSame(nameGetter, copied.getOtherPignooSorter().getField(), "拷贝后的上一级排序器内容应一致");
        assertSame(SMode.MIN_FIRST, copied.getOtherPignooSorter().getMode(), "拷贝后的上一级排序器内容应一致");
        assertNull(copied.getOtherPignooSorter().getOtherPignooSorter(), "链的末端拷贝后仍是末端");
    }

    /**
     * copy(null)返回null
     */
    @Test
    public void copyNull() {
        assertNull(PignooSorter.copy(null), "copy(null)应返回null");
    }
}
