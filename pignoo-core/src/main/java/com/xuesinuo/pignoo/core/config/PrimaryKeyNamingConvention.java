package com.xuesinuo.pignoo.core.config;

import com.xuesinuo.pignoo.core.annotation.PrimaryKey;

/**
 * 主键命名规则
 * <p>
 * PrimaryKey Naming Convention
 * <p>
 * 设置一个全局的主键命名规则，当没有{@link PrimaryKey}注解时，会按照这个规则查找主键
 * <p>
 * Set a global primary key naming rule, when there is no {@link PrimaryKey} annotation, it will find the primary key according to this rule
 * 
 * @author xuesinuo
 * @since 0.2.0
 */
@FunctionalInterface
public interface PrimaryKeyNamingConvention {
    /**
     * 主键命名规则
     * <p>
     * PrimaryKey Naming Convention
     * 
     * @param tableName 表名
     * @param className 类名
     * @return 主键名
     */
    String naming(String tableName, String className);

    /**
     * 默认主键叫“id”
     * <p>
     * Default PrimaryKey is "id"
     */
    public static PrimaryKeyNamingConvention DEFAULT = (tableName, className) -> "id";
}
