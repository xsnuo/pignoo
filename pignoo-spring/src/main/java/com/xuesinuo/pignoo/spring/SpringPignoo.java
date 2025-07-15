package com.xuesinuo.pignoo.spring;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.xuesinuo.pignoo.core.Pignoo;
import com.xuesinuo.pignoo.core.PignooConfig;
import com.xuesinuo.pignoo.core.PignooList;
import com.xuesinuo.pignoo.core.implement.MySqlPignooList;

/**
 * 基于Spring数据源管理的Pignoo实现
 * <p>
 * Pignoo implementation based on Spring data source management
 * <p>
 * 使用方法：构建一个SpringPignoo类型的SpringBean，在需要的地方直接注入Pignoo接口即可
 * <p>
 * Usage: build a SpringPignoo type SpringBean, and directly inject the Pignoo interface where needed
 * 
 * @author xuesinuo
 * @since 0.1.0
 */
public class SpringPignoo implements Pignoo {

    private final DatabaseEngine engine;// 数据库引擎

    private final DataSource dataSource;// 数据库连接

    /**
     * 
     * @param dataSource 数据源
     *                   <p>
     *                   Data source
     * @param config     配置
     *                   <p>
     *                   Configuration
     */
    public SpringPignoo(DataSource dataSource, PignooConfig config) {
        DatabaseEngine engine = config.getEngine();
        if (engine == null) {
            try (Connection conn = dataSource.getConnection()) {
                engine = DatabaseEngine.getDatabaseEngineByConnection(conn);
                conn.commit();
            } catch (SQLException e) {
                throw new RuntimeException("Read database engine failed", e);
            }
        }
        if (engine == null) {
            throw new RuntimeException("Unknow database engine");
        }
        this.engine = engine;
        this.dataSource = dataSource;
    }

    @Override
    public <E> PignooList<E> getList(Class<E> c) {
        switch (engine) {
        case MySQL:
            return new MySqlPignooList<E>(this, DataSourceUtils.getConnection(dataSource), TransactionSynchronizationManager.isActualTransactionActive(), c);
        }
        throw new RuntimeException("Unknow database engine");
    }

    @Override
    public void close() {
        throw new RuntimeException("Can not close spring-pignoo");
    }

    @Override
    public boolean hasClosed() {
        return false;
    }
}
