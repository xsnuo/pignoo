package com.xuesinuo.pignoo.core.entity;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import com.xuesinuo.pignoo.core.PignooConfig;

/**
 * 实体映射器，启动包很类信息、getter方法与属性名的映射器
 * <p>
 * Entity Mapper, Map ClassInfo, Getter Method and Property Name
 *
 * @author xuesinuo
 * @since 0.1.0
 * @version 0.1.0
 */
public class EntityMapper<E> {

    private ClassInfo<E> classInfo;
    private FunctionNameGetter<E> functionNameGetter;
    private static final ConcurrentHashMap<Class<?>, EntityMapper<?>> cache = new ConcurrentHashMap<>();

    private EntityMapper(Class<E> c, PignooConfig config) {
        this.classInfo = new ClassInfo<>(c, config);
        this.functionNameGetter = new FunctionNameGetter<>(c);
    }

    /**
     * 实体解析器构造函数
     * <p>
     * Entity Parser Constructor
     *
     * @param c      要解析的类型
     *               <p>
     *               Type to be parsed
     * @param config 配置
     *               <p>
     *               Config
     * @param <E>    要解析的类型
     *               <p>
     *               Type to be parsed
     * @return 类型解析器
     *         <p>
     *         Type Parser
     */
    @SuppressWarnings("unchecked")
    public static <E> EntityMapper<E> build(Class<E> c, PignooConfig config) {
        EntityMapper<E> mapper = (EntityMapper<E>) cache.get(c);
        if (mapper == null) {
            mapper = new EntityMapper<>(c, config);
            cache.put(c, mapper);
        }
        return mapper;
    }

    /**
     * 实体解析器构造函数
     * <p>
     * Entity Parser Constructor
     *
     * @param c   要解析的类型
     *            <p>
     *            Type to be parsed
     * @param <E> 要解析的类型
     *            <p>
     *            Type to be parsed
     * @return 类型解析器
     *         <p>
     *         Type Parser
     */
    @SuppressWarnings("unchecked")
    public static <E> EntityMapper<E> build(Class<E> c) {
        EntityMapper<E> mapper = (EntityMapper<E>) cache.get(c);
        if (mapper == null) {
            throw new RuntimeException("EntityMapper not found for " + c.getName());
        }
        return mapper;
    }

    /**
     * 表名
     * <p>
     * table name
     *
     * @return 表名
     *         <p>
     *         table name
     */
    public String tableName() {
        return classInfo.tableName;
    }

    /**
     * 构造一个实体
     * <p>
     * Build an entity
     *
     * @return 新的实体
     *         <p>
     *         New entity
     */
    public E buildEntity() {
        try {
            return classInfo.constructor.newInstance();
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 全部列名
     * <p>
     * all column names
     *
     * @return 列名集合
     *         <p>
     *         column names
     */
    public List<String> columns() {
        return classInfo.columns;
    }

    /**
     * 全部JavaBean属性
     * <p>
     * all JavaBean properties
     *
     * @return 属性集合
     *         <p>
     *         properties
     */
    public List<Field> fields() {
        return classInfo.fields;
    }

    /**
     * getters
     *
     * @return getters
     */
    public List<Method> getters() {
        return classInfo.getters;
    }

    /**
     * setters
     *
     * @return setters
     */
    public List<Method> setters() {
        return classInfo.setters;
    }

    /**
     * setter方法名
     * <p>
     * setter method names
     *
     * @return setter方法名集合
     *         <p>
     *         setter method names
     */
    public List<String> setterNames() {
        return classInfo.setterNames;
    }

    /**
     * 主键的列名
     * <p>
     * primary key column name
     *
     * @return 主键的列名
     *         <p>
     *         primary key column name
     */
    public String primaryKeyColumn() {
        return classInfo.primaryKeyColumn;
    }

    /**
     * 主键对应属性的getter
     * <p>
     * primary key getter
     *
     * @return 主键对应属性的getter
     *         <p>
     *         primary key getter
     */
    public Method primaryKeyGetter() {
        return classInfo.primaryKeyGetter;
    }

    /**
     * 是否为自增主键
     * <p>
     * is auto increment primary key
     *
     * @return 是否为自增主键
     *         <p>
     *         is auto increment primary key
     */
    public boolean autoPrimaryKey() {
        return classInfo.autoPrimaryKey;
    }

    /**
     * 根据JavaBean的getter function获取对应的列名
     *
     * @param function getter方法
     *                 <p>
     *                 getter function
     * @return 对应的列名
     *         <p>
     *         column name
     */
    public String getColumnByFunction(Function<E, ?> function) {
        String functionName = functionNameGetter.getFunctionName(function);
        int index = classInfo.getterNames.indexOf(functionName);
        if (index >= 0) {
            return classInfo.columns.get(index);
        }
        return null;
    }
}
