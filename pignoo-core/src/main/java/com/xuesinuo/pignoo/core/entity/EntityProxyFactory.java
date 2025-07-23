package com.xuesinuo.pignoo.core.entity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

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
public class EntityProxyFactory<E> {
    private Enhancer enhancer;
    private EntityMapper<E> mapper;
    /**
     * 正在构建代理时的对象，会调用set方法赋值内容，以此标记此工厂正在构建的代理，正在构建中的代理不执行代理逻辑
     */
    private Object building = null;

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
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(c);
        enhancer.setCallback(new MethodInterceptor() {
            @Override
            public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
                String methodName = method.getName();
                int index = mapper.setterNames().indexOf(methodName);
                if (index >= 0 && method.getParameterCount() == 1 && obj != building) {
                    updater.run(index, args[0], obj);
                }
                return proxy.invokeSuper(obj, args);
            }
        });
        this.enhancer = enhancer;
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
    @SuppressWarnings("unchecked")
    public synchronized E build(E entity) {
        if (entity == null) {
            return null;
        }
        E proxy = (E) enhancer.create();
        building = proxy;
        for (int i = 0; i < mapper.columns().size(); i++) {
            try {
                if (mapper.setters().get(i) != null && mapper.getters().get(i) != null) {
                    mapper.setters().get(i).invoke(proxy, mapper.getters().get(i).invoke(entity));
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        building = null;
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
