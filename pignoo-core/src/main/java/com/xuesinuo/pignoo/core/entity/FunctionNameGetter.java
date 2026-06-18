package com.xuesinuo.pignoo.core.entity;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

import com.xuesinuo.pignoo.core.EntityFunction;
import com.xuesinuo.pignoo.core.exception.MapperException;

/**
 * getter方法与属性名映射器
 * <p>
 * getter method and property name mapper
 * <p>
 * 用于通过getter方法引用解析出方法名，基于{@link java.lang.invoke.SerializedLambda}，不执行方法本身
 * <p>
 * Resolve the method name from the getter method reference, based on {@link java.lang.invoke.SerializedLambda}, without invoking the method itself
 *
 * @param <E> JavaBean Type
 * @author xuesinuo
 * @since 0.1.0
 * @version 1.1.8
 */
public class FunctionNameGetter<E> {

    /**
     * lambda类型与方法名的缓存
     * <p>
     * Cache of lambda type and method name
     */
    private final ConcurrentHashMap<Class<?>, String> cache = new ConcurrentHashMap<>();

    /**
     * 获取方法名
     * <p>
     * Get Method Name
     *
     * @param fun getter方法引用
     *            <p>
     *            getter method reference
     * @return 方法名
     *         <p>
     *         Method Name
     */
    public String getFunctionName(EntityFunction<E, ?> fun) {
        return cache.computeIfAbsent(fun.getClass(), c -> resolveFunctionName(fun));
    }

    /**
     * 通过SerializedLambda解析方法引用对应的方法名
     * <p>
     * Resolve the method name of the method reference by SerializedLambda
     *
     * @param fun getter方法引用
     *            <p>
     *            getter method reference
     * @return 方法名
     *         <p>
     *         Method Name
     */
    private static String resolveFunctionName(EntityFunction<?, ?> fun) {
        try {
            Method writeReplace = fun.getClass().getDeclaredMethod("writeReplace");
            writeReplace.setAccessible(true);
            SerializedLambda serializedLambda = (SerializedLambda) writeReplace.invoke(fun);
            return serializedLambda.getImplMethodName();
        } catch (Exception e) {
            throw new MapperException("Resolve function name failed", e);
        }
    }
}
