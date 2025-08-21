package com.xuesinuo.pignoo.core.config;

/**
 * 名称的解析方式：在未标记注解或注解中未指定名称的情况下生效
 * <p>
 * The parsing method of the name: effective when the annotation is not marked or the name is not specified in the annotation.
 * 
 * @author xuesinuo
 * @version 0.2.0
 */
public enum NamingMode {
    /**
     * 类名等于表名，属性名等于列名
     * <p>
     * The class name is equal to the table name, and the attribute name is equal to the column name.
     */
    SAME,

    /**
     * 【默认】Java使用驼峰命名，数据库使用蛇形命名（下划线命名）
     * <p>
     * [Default] Java uses camel naming, and the database uses snake naming (underscore naming).
     * <p>
     * 这要求JavaBean的命名必须要规范，否则可能出现解析错误
     * <p>
     * This requires that the naming of JavaBean must be standardized, otherwise parsing errors may occur.
     */
    CAMEL_TO_SNAKE;
}