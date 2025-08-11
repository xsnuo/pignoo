package com.xuesinuo.pignoo.core.exception;

/**
 * 映射异常
 * <p>
 * Mapper exception
 * 
 * @author xuesinuo
 * @since 1.0.0
 * @version 1.0.0
 */
public class MapperException extends RuntimeException {
    public MapperException(Exception e) {
        super(e);
    }

    public MapperException(String string) {
        super(string);
    }

    public MapperException(String string, Exception e) {
        super(string, e);
    }
}
