package com.xuesinuo.pignoo.core.entity;

import java.util.LinkedHashMap;
import java.util.Map;

public class SqlParam {
    public int index = 0;
    public Map<Integer, Object> params = new LinkedHashMap<>();

    public String next(Object value) {
        params.put(index++, value);
        return "?";
    }
}
