package com.xuesinuo.pignoo.core.exception;

/**
 * Pignoo内部运行异常
 * <p>
 * RuntimeException in Pignoo
 * 
 * @author xuesinuo
 * @since 1.0.0
 * @version 1.0.0
 */
public class PignooRuntimeException extends RuntimeException {
    public PignooRuntimeException(Exception e) {
        super(e);
    }

    public PignooRuntimeException(String string) {
        super(string);
    }

    public PignooRuntimeException(String string, Exception e) {
        super(string, e);
    }
}
