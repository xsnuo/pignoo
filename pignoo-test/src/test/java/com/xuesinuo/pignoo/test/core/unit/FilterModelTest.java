package com.xuesinuo.pignoo.test.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.xuesinuo.pignoo.core.EntityFunction;
import com.xuesinuo.pignoo.core.PignooFilter;
import com.xuesinuo.pignoo.core.PignooFilter.FMode;
import com.xuesinuo.pignoo.core.PignooFilter.XOR;
import com.xuesinuo.pignoo.core.exception.MapperException;
import com.xuesinuo.pignoo.test.fixture.Plain;

/**
 * PignooFilter的纯模型行为：不生成SQL，只看构造出来的结构
 * <p>
 * Pure model behaviours of PignooFilter: no SQL generation, only the built structure
 */
public class FilterModelTest {

    /**
     * 10个筛选条件的名称与参数个数
     */
    @Test
    public void fModeMetadata() {
        assertEquals(10, FMode.values().length, "筛选条件的数量应为10");

        assertEquals("==", FMode.EQ.getName(), "EQ的名称应为==");
        assertEquals("!=", FMode.NE.getName(), "NE的名称应为!=");
        assertEquals(">", FMode.GT.getName(), "GT的名称应为>");
        assertEquals("<", FMode.LT.getName(), "LT的名称应为<");
        assertEquals(">=", FMode.GE.getName(), "GE的名称应为>=");
        assertEquals("<=", FMode.LE.getName(), "LE的名称应为<=");
        assertEquals("like", FMode.LIKE.getName(), "LIKE的名称应为like");
        assertEquals("not like", FMode.NOT_LIKE.getName(), "NOT_LIKE的名称应为not like");
        assertEquals("in", FMode.IN.getName(), "IN的名称应为in");
        assertEquals("not in", FMode.NOT_IN.getName(), "NOT_IN的名称应为not in");

        for (FMode mode : FMode.values()) {
            if (mode == FMode.IN || mode == FMode.NOT_IN) {
                assertEquals(0, mode.getMinCount(), mode + "的参数最少个数应为0");
                assertEquals(Integer.MAX_VALUE, mode.getMaxCount(), mode + "的参数最多个数应不限");
            } else {
                assertEquals(1, mode.getMinCount(), mode + "的参数最少个数应为1");
                assertEquals(1, mode.getMaxCount(), mode + "的参数最多个数应为1");
            }
        }
    }

    /**
     * 名称映射成枚举：大小写不敏感、允许首尾空格、带空格的名称
     */
    @Test
    public void getFModeByName() {
        assertSame(FMode.EQ, FMode.getFMode("=="), "==应映射到EQ");
        assertSame(FMode.NE, FMode.getFMode("!="), "!=应映射到NE");
        assertSame(FMode.GE, FMode.getFMode(">="), ">=应映射到GE");
        assertSame(FMode.LIKE, FMode.getFMode("like"), "like应映射到LIKE");
        assertSame(FMode.LIKE, FMode.getFMode("LIKE"), "名称映射应大小写不敏感");
        assertSame(FMode.LIKE, FMode.getFMode("LiKe"), "名称映射应大小写不敏感");
        assertSame(FMode.NOT_LIKE, FMode.getFMode("NOT LIKE"), "带空格的名称应能映射");
        assertSame(FMode.NOT_IN, FMode.getFMode("Not In"), "带空格的名称应能映射");
        assertSame(FMode.IN, FMode.getFMode("  in  "), "首尾空格应被忽略");
    }

    /**
     * 非法名称与null都抛MapperException
     */
    @Test
    public void getFModeInvalid() {
        MapperException unknown = assertThrows(MapperException.class, () -> FMode.getFMode("xxx"), "非法名称应抛出MapperException");
        assertTrue(unknown.getMessage().contains("Invalid FMode"), "异常信息应指出筛选条件非法，实际为：" + unknown.getMessage());

        assertThrows(MapperException.class, () -> FMode.getFMode(""), "空串应抛出MapperException");
        assertThrows(MapperException.class, () -> FMode.getFMode("   "), "空白串应抛出MapperException");
        assertThrows(MapperException.class, () -> FMode.getFMode("EQ"), "映射用的是name属性而不是枚举名，EQ应被视为非法名称");
        assertThrows(MapperException.class, () -> FMode.getFMode("notin"), "少了空格的notin应被视为非法名称");

        MapperException nullName = assertThrows(MapperException.class, () -> FMode.getFMode(null), "null应抛出MapperException");
        assertTrue(nullName.getMessage().contains("null"), "异常信息应指出名称为null，实际为：" + nullName.getMessage());
    }

    /**
     * build出来的基本结构
     */
    @Test
    public void buildStructure() {
        EntityFunction<Plain, ?> nameGetter = Plain::getName;
        PignooFilter<Plain> filter = PignooFilter.build(nameGetter, FMode.EQ, "abc");
        assertSame(nameGetter, filter.getField(), "field应原样保留传入的getter方法引用");
        assertSame(FMode.EQ, filter.getMode(), "mode应原样保留");
        assertEquals(List.of("abc"), filter.getValues(), "values应保留传入的值");
        assertSame(XOR.AND, filter.getXor(), "build出来的条件默认是AND");
        assertTrue(filter.getOtherPignooFilterList().isEmpty(), "单条件不应携带子条件");
    }

    /**
     * 用字符串指定筛选条件
     */
    @Test
    public void buildWithStringMode() {
        PignooFilter<Plain> filter = PignooFilter.<Plain>build(Plain::getName, "NOT LIKE", "%a%");
        assertSame(FMode.NOT_LIKE, filter.getMode(), "字符串重载应先把名称映射成枚举");
        assertEquals(List.of("%a%"), filter.getValues(), "字符串重载不应影响值的处理");
        assertThrows(MapperException.class, () -> PignooFilter.<Plain>build(Plain::getName, "xxx", "%a%"), "字符串重载遇到非法名称应抛出MapperException");
    }

    /**
     * 空条件：只有一个空的子条件列表，其余都是null
     */
    @Test
    public void buildEmpty() {
        PignooFilter<Plain> filter = PignooFilter.build(Plain.class);
        assertNull(filter.getField(), "空条件的field应为null");
        assertNull(filter.getMode(), "空条件的mode应为null");
        assertNull(filter.getValues(), "空条件的values应为null");
        assertNull(filter.getXor(), "空条件的xor应为null");
        assertTrue(filter.getOtherPignooFilterList().isEmpty(), "空条件的子条件列表应为空且非null");
    }

    /**
     * and拼接：返回的是一个新的容器条件，自身不带字段，只挂两个子条件
     */
    @Test
    public void andStructure() {
        PignooFilter<Plain> first = PignooFilter.<Plain>build(Plain::getName, FMode.EQ, "a");
        PignooFilter<Plain> second = PignooFilter.<Plain>build(Plain::getCode, FMode.EQ, "b");
        PignooFilter<Plain> combined = first.and(second);

        assertNotSame(first, combined, "and应返回新的容器条件而不是修改自身");
        assertNull(combined.getField(), "容器条件自身不带字段");
        assertNull(combined.getMode(), "容器条件自身不带筛选条件");
        assertSame(XOR.AND, combined.getXor(), "and拼接出来的容器应为AND");
        assertEquals(2, combined.getOtherPignooFilterList().size(), "容器条件应挂两个子条件");
        assertSame(first, combined.getOtherPignooFilterList().get(0), "第一个子条件应是原条件本身");
        assertSame(second, combined.getOtherPignooFilterList().get(1), "第二个子条件应是传入的条件本身");

        PignooFilter<Plain> chained = first.and(Plain::getCode, FMode.GT, 1);
        assertSame(XOR.AND, chained.getXor(), "and拼接出来的容器应为AND");
        assertSame(first, chained.getOtherPignooFilterList().get(0), "第一个子条件应是原条件本身");
        PignooFilter<Plain> child = chained.getOtherPignooFilterList().get(1);
        assertSame(FMode.GT, child.getMode(), "新建的子条件应保留传入的筛选条件");
        assertEquals(List.of(1), child.getValues(), "新建的子条件应保留传入的值");
        assertSame(XOR.AND, child.getXor(), "新建的子条件自身是AND");
    }

    /**
     * or拼接：容器是OR，但新建的子条件自身仍被标成AND
     */
    @Test
    public void orStructure() {
        PignooFilter<Plain> first = PignooFilter.<Plain>build(Plain::getName, FMode.EQ, "a");
        PignooFilter<Plain> second = PignooFilter.<Plain>build(Plain::getCode, FMode.EQ, "b");
        PignooFilter<Plain> combined = first.or(second);

        assertSame(XOR.OR, combined.getXor(), "or拼接出来的容器应为OR");
        assertEquals(2, combined.getOtherPignooFilterList().size(), "容器条件应挂两个子条件");
        assertSame(first, combined.getOtherPignooFilterList().get(0), "第一个子条件应是原条件本身");
        assertSame(second, combined.getOtherPignooFilterList().get(1), "第二个子条件应是传入的条件本身");

        PignooFilter<Plain> chained = first.or(Plain::getCode, FMode.LT, 9);
        assertSame(XOR.OR, chained.getXor(), "or拼接出来的容器应为OR");
        PignooFilter<Plain> child = chained.getOtherPignooFilterList().get(1);
        assertSame(XOR.AND, child.getXor(), "or重载新建的子条件自身仍被标成AND，OR体现在容器上");
    }

    /**
     * 数组与集合的摊平
     */
    @Test
    public void flattenArrayAndCollection() {
        assertEquals(List.of(1, 2, 3), PignooFilter.<Plain>build(Plain::getQty, FMode.IN, (Object) new Object[] { 1, 2, 3 }).getValues(), "Object数组应被摊平");
        assertEquals(List.of(1, 2, 3), PignooFilter.<Plain>build(Plain::getQty, FMode.IN, List.of(1, 2, 3)).getValues(), "List应被摊平");
        assertEquals(List.of(1, 2, 3), PignooFilter.<Plain>build(Plain::getQty, FMode.IN, new LinkedHashSet<>(List.of(1, 2, 3))).getValues(), "Set应被摊平");
        assertEquals(List.of(1, 2, 3), PignooFilter.<Plain>build(Plain::getQty, FMode.IN, 1, 2, 3).getValues(), "多个散值应逐个进入values");
        assertEquals(List.of("ab"), PignooFilter.<Plain>build(Plain::getName, FMode.IN, "ab").getValues(), "字符串不是集合也不是数组，应整体作为一个值");
        assertTrue(PignooFilter.<Plain>build(Plain::getQty, FMode.IN, List.of()).getValues().isEmpty(), "空集合摊平后values应为空");
    }

    /**
     * 多层嵌套的摊平
     */
    @Test
    public void flattenNested() {
        Object nested = new Object[] { 1, new Object[] { 2, List.of(3, 4) }, List.of(List.of(5), 6), new int[] { 7, 8 } };
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8), PignooFilter.<Plain>build(Plain::getQty, FMode.IN, nested).getValues(), "数组与集合的多层嵌套应被递归摊平");
        assertEquals(List.of(1, 2), PignooFilter.<Plain>build(Plain::getQty, FMode.IN, List.of(List.of(List.of(1, 2)))).getValues(), "多层集合嵌套应被递归摊平");
    }

    /**
     * 八种基本类型数组的摊平（装箱后进入values）
     */
    @Test
    public void flattenPrimitiveArrays() {
        assertEquals(List.of((byte) 1, (byte) 2), PignooFilter.<Plain>build(Plain::getQty, FMode.IN, new byte[] { 1, 2 }).getValues(), "byte数组应被摊平并装箱");
        assertEquals(List.of((short) 1, (short) 2), PignooFilter.<Plain>build(Plain::getQty, FMode.IN, new short[] { 1, 2 }).getValues(), "short数组应被摊平并装箱");
        assertEquals(List.of(1, 2), PignooFilter.<Plain>build(Plain::getQty, FMode.IN, new int[] { 1, 2 }).getValues(), "int数组应被摊平并装箱");
        assertEquals(List.of(1L, 2L), PignooFilter.<Plain>build(Plain::getQty, FMode.IN, new long[] { 1L, 2L }).getValues(), "long数组应被摊平并装箱");
        assertEquals(List.of(1.5F, 2.5F), PignooFilter.<Plain>build(Plain::getQty, FMode.IN, new float[] { 1.5F, 2.5F }).getValues(), "float数组应被摊平并装箱");
        assertEquals(List.of(1.5D, 2.5D), PignooFilter.<Plain>build(Plain::getQty, FMode.IN, new double[] { 1.5D, 2.5D }).getValues(), "double数组应被摊平并装箱");
        assertEquals(List.of('a', 'b'), PignooFilter.<Plain>build(Plain::getQty, FMode.IN, new char[] { 'a', 'b' }).getValues(), "char数组应被摊平并装箱");
        assertEquals(List.of(true, false), PignooFilter.<Plain>build(Plain::getQty, FMode.IN, new boolean[] { true, false }).getValues(), "boolean数组应被摊平并装箱");
    }

    /**
     * null值被保留
     */
    @Test
    public void flattenKeepsNull() {
        assertEquals(Arrays.asList("a", null, "b"), PignooFilter.<Plain>build(Plain::getName, FMode.IN, "a", null, "b").getValues(), "散值中的null应被保留");
        assertEquals(Arrays.asList("a", null), PignooFilter.<Plain>build(Plain::getName, FMode.IN, Arrays.asList("a", null)).getValues(), "集合中的null应被保留");
        assertEquals(Arrays.asList((Object) null), PignooFilter.<Plain>build(Plain::getName, FMode.EQ, (Object) null).getValues(), "单个null值应被保留");
        assertEquals(Arrays.asList((Object) null), PignooFilter.<Plain>build(Plain::getName, FMode.EQ, (Object[]) null).getValues(), "整个可变参数为null时应当作一个null值");
    }

    /**
     * and/or重载的摊平行为与build一致
     */
    @Test
    public void flattenSameForAndOr() {
        PignooFilter<Plain> base = PignooFilter.<Plain>build(Plain::getName, FMode.EQ, "a");
        Object nested = new Object[] { 1, List.of(2, 3), new int[] { 4 } };

        PignooFilter<Plain> andChild = base.and(Plain::getQty, FMode.IN, nested).getOtherPignooFilterList().get(1);
        assertEquals(List.of(1, 2, 3, 4), andChild.getValues(), "and重载应与build用同一套摊平逻辑");

        PignooFilter<Plain> orChild = base.or(Plain::getQty, FMode.IN, nested).getOtherPignooFilterList().get(1);
        assertEquals(List.of(1, 2, 3, 4), orChild.getValues(), "or重载应与build用同一套摊平逻辑");

        assertEquals(Arrays.asList("a", null), base.and(Plain::getName, FMode.IN, "a", null).getOtherPignooFilterList().get(1).getValues(), "and重载同样保留null");
        assertEquals(Arrays.asList("a", null), base.or(Plain::getName, FMode.IN, "a", null).getOtherPignooFilterList().get(1).getValues(), "or重载同样保留null");
    }

    /**
     * copy是深拷贝：子条件也被逐层拷贝，改原件不影响副本
     */
    @Test
    public void copyDeep() {
        EntityFunction<Plain, ?> nameGetter = Plain::getName;
        PignooFilter<Plain> leaf = PignooFilter.build(nameGetter, FMode.IN, "a", "b");
        PignooFilter<Plain> root = leaf.or(Plain::getCode, FMode.EQ, "c");
        PignooFilter<Plain> copied = PignooFilter.copy(root);

        assertNotSame(root, copied, "copy应返回新实例");
        assertSame(XOR.OR, copied.getXor(), "xor应被复制");
        assertEquals(2, copied.getOtherPignooFilterList().size(), "子条件个数应一致");
        assertNotSame(root.getOtherPignooFilterList(), copied.getOtherPignooFilterList(), "子条件列表应是新的列表");
        assertNotSame(root.getOtherPignooFilterList().get(0), copied.getOtherPignooFilterList().get(0), "子条件应被逐个拷贝");
        assertNotSame(root.getOtherPignooFilterList().get(1), copied.getOtherPignooFilterList().get(1), "子条件应被逐个拷贝");

        PignooFilter<Plain> copiedLeaf = copied.getOtherPignooFilterList().get(0);
        assertSame(nameGetter, copiedLeaf.getField(), "field是方法引用，按引用复制");
        assertSame(FMode.IN, copiedLeaf.getMode(), "mode应被复制");
        assertEquals(List.of("a", "b"), copiedLeaf.getValues(), "values内容应一致");

        leaf.getValues().add("z");
        assertEquals(List.of("a", "b", "z"), leaf.getValues(), "用例前提：原件的values是可变集合");
        assertEquals(List.of("a", "b"), copiedLeaf.getValues(), "改原件的values不应影响副本");
    }

    /**
     * copy的边界：null直接返回null；副本的values是不可变集合；原件的values含null时会失败
     */
    @Test
    public void copyEdgeCases() {
        assertNull(PignooFilter.copy(null), "copy(null)应返回null");

        PignooFilter<Plain> empty = PignooFilter.copy(PignooFilter.build(Plain.class));
        assertNull(empty.getValues(), "原件values为null时，副本的values仍为null");
        assertTrue(empty.getOtherPignooFilterList().isEmpty(), "空条件拷贝后子条件列表仍为空");

        PignooFilter<Plain> copied = PignooFilter.copy(PignooFilter.<Plain>build(Plain::getName, FMode.IN, "a"));
        assertThrows(UnsupportedOperationException.class, () -> copied.getValues().add("b"), "副本的values是不可变集合");

        PignooFilter<Plain> withNull = PignooFilter.<Plain>build(Plain::getName, FMode.IN, "a", null);
        assertThrows(NullPointerException.class, () -> PignooFilter.copy(withNull), "当前实现用List.copyOf拷贝values，含null的条件拷贝时会抛NullPointerException");
    }

    /**
     * values按传入顺序排列
     */
    @Test
    public void valuesKeepOrder() {
        PignooFilter<Plain> filter = PignooFilter.<Plain>build(Plain::getQty, FMode.IN, 3, List.of(1, 2), new int[] { 9 });
        assertEquals(List.of(3, 1, 2, 9), new ArrayList<>(filter.getValues()), "摊平后的值应保持传入顺序");
    }
}
