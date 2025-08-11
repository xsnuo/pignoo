package com.xuesinuo.pignoo.core.exception;

import java.sql.SQLException;

/**
 * SQL执行异常
 * <p>
 * SQL execute exception
 * 
 * @author xuesinuo
 * @since 1.0.0
 * @version 1.0.0
 */
public class SqlExecuteException extends RuntimeException {
    public SqlExecuteException(SQLException e) {
        super(e);
    }

    public SqlExecuteException(String string) {
        super(string);
    }

    public SqlExecuteException(String string, SQLException e) {
        super(string, e);
    }
}
