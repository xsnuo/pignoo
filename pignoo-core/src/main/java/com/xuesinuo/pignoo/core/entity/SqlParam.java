package com.xuesinuo.pignoo.core.entity;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SQL参数拼接工具
 *
 * @author xuesinuo
 * @since 0.2.3
 * @version 0.2.3
 */
public class SqlParam {
    private int index = 0;
    private final Map<Integer, Object> params = new LinkedHashMap<>();

    /**
     * 拼接下一个参数，并返回一个占位符
     *
     * @param value 拼接的参数
     *              <p>
     *              the value
     * @return 占位符
     *         <p>
     *         placeholder
     */
    public String next(Object value) {
        params.put(index++, value);
        return "?";
    }

    /**
     * 获取已拼接的参数
     * <p>
     * Get the collected parameters
     *
     * @return 参数表
     *         <p>
     *         parameters
     */
    public Map<Integer, Object> getParams() {
        return params;
    }
}
