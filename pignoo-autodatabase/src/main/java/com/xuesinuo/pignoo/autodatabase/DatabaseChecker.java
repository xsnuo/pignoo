package com.xuesinuo.pignoo.autodatabase;

import com.xuesinuo.pignoo.autodatabase.entity.DatabaseCheckResult;
import com.xuesinuo.pignoo.core.entity.EntityMapper;

/**
 * 数据库检查器：用于检查数据库是否正确映射了实体类
 * <p>
 * Database Checker: Used to check whether the database correctly maps the entity class
 * 
 * @author xuesinuo
 * @since 0.3.0
 * @version 0.3.0
 */
public interface DatabaseChecker {
    /**
     * 检查数据库是否正确映射了实体类
     * <p>
     * Check whether the database correctly maps the entity class
     * 
     * @param entityMapper 实体映射器
     *                     <p>
     *                     Entity Mapper
     * @return 检查结果
     *         <p>
     *         Check Result
     */
    public DatabaseCheckResult check(EntityMapper<?> entityMapper);

    /**
     * java类型转（唯一的）sql类型
     * <p>
     * Java type to (unique) sql type
     * <p>
     * Java实体属性比数据库多时，使用此映射关系自动映射到数据库
     * <p>
     * Java entity attributes are more than the database, use this mapping relationship to automatically map to the database
     * 
     * @param javaType Java类型
     *                 <p>
     *                 Java type
     * @return sql类型
     *         <p>
     *         sql type
     */
    public String javaType2SqlType(Class<?> javaType);

    /**
     * 验证Java类型与SQL类型是否可以映射
     * <p>
     * Verify whether Java type and SQL type can be mapped
     * 
     * @param javaType Java类型
     *                 <p>
     *                 Java type
     * @param sqlType  SQL类型
     *                 <p>
     *                 SQL type
     * @return 能否映射
     *         <p>
     *         Can it be mapped
     */
    public boolean javaType2SqlTypeOk(Class<?> javaType, String sqlType);
}
