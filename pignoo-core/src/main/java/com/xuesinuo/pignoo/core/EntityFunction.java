package com.xuesinuo.pignoo.core;

import java.io.Serializable;
import java.util.function.Function;

/**
 * 可序列化的{@link java.util.function.Function}
 * <p>
 * Serializable {@link java.util.function.Function}
 * <p>
 * Pignoo通过它承载实体的getter方法引用（如{@code Entity::getId}），并借助{@link java.lang.invoke.SerializedLambda}解析出对应的字段名
 * <p>
 * Pignoo uses it to carry the entity's getter method reference (such as {@code Entity::getId}), and resolves the corresponding field name by {@link java.lang.invoke.SerializedLambda}
 *
 * @param <T> 入参类型
 *            <p>
 *            Input type
 * @param <R> 返回类型
 *            <p>
 *            Return type
 * @author xuesinuo
 * @since 1.1.8
 * @version 1.1.8
 */
@FunctionalInterface
public interface EntityFunction<T, R> extends Function<T, R>, Serializable {
}
