package com.xuesinuo.pignoo.autodatabase.exception;

/**
 * 扫描过程异常
 * <p>
 * ScanException
 * 
 * @author xuesinuo
 * @since 1.0.0
 * @version 1.0.0
 */
public class ScanException extends RuntimeException {
    public ScanException(Exception e) {
        super(e);
    }

    public ScanException(String string) {
        super(string);
    }

    public ScanException(String string, Exception e) {
        super(string, e);
    }
}
