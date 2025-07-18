package com.xuesinuo.pignoo.core.config;

import com.xuesinuo.pignoo.core.annotation.Column;
import com.xuesinuo.pignoo.core.annotation.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 注解使用方式
 * <p>
 * The annotation's mode.
 * 
 * @author xuesinuo
 * @version 0.2.0
 */
@AllArgsConstructor
@Getter
public enum AnnotationMode {
    /**
     * 必须正确使用注解，不使用{@link Table}注解的表类报错，不使用{@link Column}注解的列报错将忽略
     * <p>
     * Must use the annotation correctly, the class that does not use the {@link Table} annotation reports an error, the column that does not use the {@link Column} annotation will be ignored.
     */
    MUST,

    /**
     * 【默认】如果标记了注解，则按照注解解析。如果未标记注解，则按照类名、属性名解析
     * <p>
     * [Default] If the annotation is marked, it is parsed according to the annotation. If the annotation is not marked, it is parsed according to the class name and attribute name.f the annotation is
     * not marked, it is parsed according to the class name and attribute name.
     */
    MIX;

    /**
     * 混用注解时，名称的解析方式
     * <p>
     * When using mixed annotations, the parsing method of the name.
     * 
     * @author xuesinuo
     * @version 0.2.0
     */
    public static enum AnnotationMixMode {
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
}
