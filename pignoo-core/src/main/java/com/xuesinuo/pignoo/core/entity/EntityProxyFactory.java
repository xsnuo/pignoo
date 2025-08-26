package com.xuesinuo.pignoo.core.entity;

import java.lang.reflect.Field;
import java.util.List;

import com.xuesinuo.pignoo.core.exception.PignooRuntimeException;

import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;

/**
 * 查询结果实体的代理的创建器
 * <p>
 * 每个查询结果Entity都会经过此工厂创建代理，代理调用setter方法时会同步更新数据库
 *
 * @param <E> JavaBean Type
 * @author xuesinuo
 * @since 0.1.0
 * @version 0.1.0
 */
@Slf4j
public class EntityProxyFactory<E> {
    private Class<? extends E> porxyClass;
    private Field proxyField;
    private EntityMapper<E> mapper;

    /**
     * 在代理执行setter时，拦截并执行的update操作
     * <p>
     * Update Operation Executed When Proxy Executes Setter
     * 
     * @author xuesinuo
     * @since 0.1.0
     * @version 0.1.0
     */
    @FunctionalInterface
    public static interface Updater {
        void run(int index, Object setterArg, Object obj);
    }

    /**
     * 代理工厂构造器：每个类型建造一座工厂
     * <p>
     * Proxy Factory Constructor: Each type builds a factory
     *
     * @param c       实体类型
     *                <p>
     *                Entity Type
     * @param mapper  实体映射器
     *                <p>
     *                Entity Mapper
     * @param updater 在代理执行setter时，拦截并执行的update操作
     *                <p>
     *                Update Operation Executed When Proxy Executes Setter
     */
    public EntityProxyFactory(Class<E> c, EntityMapper<E> mapper, Updater updater) {
        this.mapper = mapper;
        try {
            this.porxyClass = new ByteBuddy()
                    .subclass(c)
                    .defineField("$proxy", c, java.lang.reflect.Modifier.PRIVATE)
                    .method(ElementMatchers.not(ElementMatchers.isDeclaredBy(Object.class)))
                    .intercept(InvocationHandlerAdapter.of((proxy, method, args) -> {
                        String methodName = method.getName();
                        int index = mapper.setterNames().indexOf(methodName);
                        if (index >= 0 && method.getParameterCount() == 1) {
                            updater.run(index, args[0], proxy);
                        }
                        return method.invoke(this.proxyField.get(proxy), args);
                    }))
                    .make()
                    .load(c.getClassLoader())
                    .getLoaded();
            this.proxyField = porxyClass.getDeclaredField("$proxy");
            this.proxyField.setAccessible(true);
        } catch (Exception e) {
            throw new PignooRuntimeException("Pignoo create proxy-factory error", e);
        }
    }

    /**
     * 构建一个JavaBean的代理
     * <p>
     * Build a JavaBean Proxy
     *
     * @param entity 原始JavaBean
     *               <p>
     *               Original JavaBean
     * @return 代理
     *         <p>
     *         Proxy
     */
    public E build(E entity) {
        if (entity == null) {
            return null;
        }
        E proxy = null;
        try {
            proxy = (E) porxyClass.getDeclaredConstructor().newInstance();
            proxyField.set(proxy, entity);
        } catch (Exception e) {
            throw new PignooRuntimeException("Pignoo create proxy error", e);
        }
        for (Field field : mapper.fields()) {
            try {
                Object value = field.get(entity);
                field.set(proxy, value);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return proxy;
    }

    /**
     * 对List中的每个元素构建代理
     * <p>
     * Build a proxy for each element in the List
     *
     * @param list JavaBean列表
     *             <p>
     *             JavaBean List
     * @return 代理列表
     *         <p>
     *         Proxy List
     */
    public List<E> build(List<E> list) {
        if (list == null) {
            return null;
        }
        return list.stream().map(e -> build(e)).toList();
    }
}
