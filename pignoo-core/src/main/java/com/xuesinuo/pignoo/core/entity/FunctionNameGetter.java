package com.xuesinuo.pignoo.core.entity;

import java.util.function.Function;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;

/**
 * getter方法与属性名映射器
 * <p>
 * 用于通过getter方法获取属性名
 *
 * @param <E> JavaBean Type
 * @author xuesinuo
 * @since 0.1.0
 * @version 1.0.0
 */
public class FunctionNameGetter<E> {
    private static class NamePicker {
        private String name;
    }

    private E proxy;
    private NamePicker namePicker;

    /**
     * 构造方法
     * <p>
     * Constructor
     *
     * @param c 实体类型
     *          <p>
     *          Entity Type
     */
    public FunctionNameGetter(Class<E> c) {
        this.namePicker = new NamePicker();
        try {
            this.proxy = (E) new ByteBuddy()
                    .subclass(c)
                    .method(ElementMatchers.any())
                    .intercept(InvocationHandlerAdapter.of((proxy, method, args) -> {
                        String functionName = method.getName();
                        namePicker.name = functionName;
                        return null;
                    }))
                    .make()
                    .load(c.getClassLoader())
                    .getLoaded()
                    .getDeclaredConstructor()
                    .newInstance();
        } catch (Exception e) {
            throw new RuntimeException("创建代理对象失败", e);
        }
    }

    /**
     * 获取方法名
     * <p>
     * Get Method Name
     *
     * @param fun 方法
     *            <p>
     *            Method
     * @return 方法名
     *         <p>
     *         Method Name
     */
    public String getFunctionName(Function<E, ?> fun) {
        String functionName;
        synchronized (this) {
            fun.apply(proxy);
            functionName = this.namePicker.name;
        }
        return functionName;
    }
}