package com.xuesinuo.pignoo.test.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Field;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.xuesinuo.pignoo.core.EntityFunction;
import com.xuesinuo.pignoo.core.PignooConfig;
import com.xuesinuo.pignoo.core.annotation.Column;
import com.xuesinuo.pignoo.core.annotation.Table;
import com.xuesinuo.pignoo.core.entity.EntityMapper;
import com.xuesinuo.pignoo.core.entity.FunctionNameGetter;
import com.xuesinuo.pignoo.test.fixture.AllTypes;
import com.xuesinuo.pignoo.test.fixture.BoolNaming;
import com.xuesinuo.pignoo.test.fixture.Plain;

import lombok.Data;

/**
 * getter方法引用到列名的解析：基于SerializedLambda，不执行方法本身
 * <p>
 * Resolving a getter method reference into a column name: based on SerializedLambda, without invoking the method itself
 */
public class FunctionNameTest {

    private static PignooConfig config() {
        return new PignooConfig();
    }

    /** 有被忽略字段的实体，用于验证解析不到列时返回null */
    @Table("fx_function")
    @Data
    public static class Ignored {
        private Long id;
        @Column(ignore = true)
        private String secret;
    }

    @SuppressWarnings("unchecked")
    private static Map<Class<?>, String> readCache(FunctionNameGetter<?> functionNameGetter) {
        try {
            Field field = FunctionNameGetter.class.getDeclaredField("cache");
            field.setAccessible(true);
            return (Map<Class<?>, String>) field.get(functionNameGetter);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("读取FunctionNameGetter内部缓存失败，缓存字段可能已改名", e);
        }
    }

    /**
     * 引用类型的getter方法引用
     */
    @Test
    public void columnByGetter() {
        EntityMapper<Plain> mapper = EntityMapper.build(Plain.class, config());
        assertEquals("id", mapper.getColumnByFunction(Plain::getId), "getId应解析到id列");
        assertEquals("name", mapper.getColumnByFunction(Plain::getName), "getName应解析到name列");
        assertEquals("code", mapper.getColumnByFunction(Plain::getCode), "getCode应解析到code列");
        assertEquals("money", mapper.getColumnByFunction(Plain::getMoney), "getMoney应解析到money列");
        assertEquals("qty", mapper.getColumnByFunction(Plain::getQty), "getQty应解析到qty列");
    }

    /**
     * 基本类型的getter方法引用：返回值需要装箱，解析走的仍是SerializedLambda，不应报错或解析不到
     */
    @Test
    public void columnByPrimitiveGetter() {
        EntityMapper<AllTypes> mapper = EntityMapper.build(AllTypes.class, config());
        assertEquals("nbyte", mapper.getColumnByFunction(AllTypes::getNbyte), "getNbyte应解析到nbyte列");
        assertEquals("nshort", mapper.getColumnByFunction(AllTypes::getNshort), "getNshort应解析到nshort列");
        assertEquals("ninteger", mapper.getColumnByFunction(AllTypes::getNinteger), "getNinteger应解析到ninteger列");
        assertEquals("nlong", mapper.getColumnByFunction(AllTypes::getNlong), "getNlong应解析到nlong列");
        assertEquals("nfloat", mapper.getColumnByFunction(AllTypes::getNfloat), "getNfloat应解析到nfloat列");
        assertEquals("ndouble", mapper.getColumnByFunction(AllTypes::getNdouble), "getNdouble应解析到ndouble列");
        assertEquals("nchar", mapper.getColumnByFunction(AllTypes::getNchar), "getNchar应解析到nchar列");
        assertEquals("nboolean", mapper.getColumnByFunction(AllTypes::isNboolean), "isNboolean应解析到nboolean列");
    }

    /**
     * 解析不到对应列时返回null：不在映射范围内的方法、被ignore的字段
     */
    @Test
    public void columnByFunctionNotMapped() {
        EntityMapper<Plain> mapper = EntityMapper.build(Plain.class, config());
        assertNull(mapper.getColumnByFunction(Plain::toString), "不是映射列的getter应返回null");
        EntityMapper<Ignored> ignored = EntityMapper.build(Ignored.class, config());
        assertEquals("id", ignored.getColumnByFunction(Ignored::getId), "未被忽略的字段应正常解析");
        assertNull(ignored.getColumnByFunction(Ignored::getSecret), "被@Column(ignore)忽略的字段应返回null");
    }

    /**
     * 直接使用FunctionNameGetter解析方法名
     */
    @Test
    public void functionName() {
        FunctionNameGetter<Plain> plainGetter = new FunctionNameGetter<>();
        assertEquals("getName", plainGetter.getFunctionName(Plain::getName), "应解析出方法引用的真实方法名");
        assertEquals("getId", plainGetter.getFunctionName(Plain::getId), "应解析出方法引用的真实方法名");
        FunctionNameGetter<BoolNaming> boolGetter = new FunctionNameGetter<>();
        assertEquals("is_admin", boolGetter.getFunctionName(BoolNaming::is_admin), "非常规命名的getter也应原样解析出方法名");
    }

    /**
     * 同一个lambda重复解析应命中缓存
     */
    @Test
    public void functionNameCache() {
        FunctionNameGetter<Plain> functionNameGetter = new FunctionNameGetter<>();
        assertEquals(0, readCache(functionNameGetter).size(), "新建的解析器缓存应为空");

        EntityFunction<Plain, ?> nameGetter = Plain::getName;
        assertEquals("getName", functionNameGetter.getFunctionName(nameGetter), "首次解析应返回方法名");
        assertEquals(1, readCache(functionNameGetter).size(), "首次解析后应写入一条缓存");
        assertEquals("getName", functionNameGetter.getFunctionName(nameGetter), "重复解析应返回同样的方法名");
        assertEquals("getName", functionNameGetter.getFunctionName(nameGetter), "重复解析应返回同样的方法名");
        assertEquals(1, readCache(functionNameGetter).size(), "同一个lambda重复解析应命中缓存，缓存条目数不应增长");

        EntityFunction<Plain, ?> codeGetter = Plain::getCode;
        assertEquals("getCode", functionNameGetter.getFunctionName(codeGetter), "另一个方法引用应解析出自己的方法名");
        assertEquals(2, readCache(functionNameGetter).size(), "不同的lambda应各自占一条缓存");
    }
}
