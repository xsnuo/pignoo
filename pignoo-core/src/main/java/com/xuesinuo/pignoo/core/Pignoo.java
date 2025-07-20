package com.xuesinuo.pignoo.core;

/**
 * Pignoo - 小黄人语的“无聊”。《卑鄙的我3》中小黄人们高呼“Pignoo”抗议格活过于无聊
 * <p>
 * Pignoo - The "boring" in the Minions's Language. The Minions call "Pignoo" to protest the boring life in "Despicable Me 3"
 * <p>
 * Pignoo也是本框架的名称，Pignoo类是本框架的核心类
 * <p>
 * Pignoo is also the name of this framework, and the Pignoo class is the core class of this framework
 * <p>
 * 一个Pignoo实例，就是一个从DataSource中获取的Connection封装，在此基础上，Pignoo有着自己独特的数据操作方式
 * <p>
 * A Pignoo instance is a Connection encapsulated from a DataSource, on top of which, Pignoo has its own unique data operation way
 * <p>
 * 如果使用Pignoo来管理事务的提交、回滚、关闭或返还连接，请使用{@link Gru}构建Pignoo实例
 * <p>
 * If you use Pignoo to manage the commit, rollback, close or return the connection, please use {@link Gru} to build Pignoo instances
 * <p>
 * Pignoo是一个需要关闭的({@link AutoCloseable})，在关闭Pignoo时执行的是JDBC事务的提交或回滚
 * <p>
 * Pignoo is a need to close ({@link AutoCloseable}), and when closing Pignoo, the execution is the commit or rollback of the JDBC transaction
 * <p>
 * 如果使用例如Spring + Pignoo的其他方式构建，则可能不需要关闭Pignoo，关闭连接、提交事务等操作都交给外部框架管理了
 * <p>
 * If you use other ways such as Spring + Pignoo to build, you may not need to close Pignoo, and the operations such as closing the connection, committing the transaction, etc. are managed by the
 * external framework
 * 
 * @author xuesinuo
 * @since 0.1.0
 */
public interface Pignoo extends AutoCloseable {

    /**
     * 获取一个PignooList实例，这是Pignoo的最核心用法
     * <p>
     * Get a PignooList instance, which is the most core usage of Pignoo
     * 
     * @param <E> 实体类型
     *            <p>
     *            Entity type
     * @param c   实体类型
     *            <p>
     *            Entity type
     * @return PignooList
     */
    public <E> PignooList<E> getList(Class<E> c);

    /**
     * 是否已经关闭退出Pignoo作用域
     * <p>
     * Whether to close and exit the Pignoo scope
     */
    public boolean closed();
}
