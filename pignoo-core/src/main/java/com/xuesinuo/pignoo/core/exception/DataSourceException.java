package com.xuesinuo.pignoo.core.exception;

/**
 * 数据源异常
 * <p>
 * DataSource exception
 * 
 * @author xuesinuo
 * @since 1.0.0
 * @version 1.0.0
 */
public class DataSourceException extends RuntimeException {
    public DataSourceException(Exception e) {
        super(e);
    }

    public DataSourceException(String string) {
        super(string);
    }

    public DataSourceException(String string, Exception e) {
        super(string, e);
    }
}
