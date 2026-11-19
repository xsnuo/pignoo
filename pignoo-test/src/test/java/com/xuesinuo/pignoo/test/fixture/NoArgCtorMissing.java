package com.xuesinuo.pignoo.test.fixture;

import lombok.Data;

/**
 * 坏实体：只有带参构造，没有无参构造，解析时应当失败
 */
@Data
public class NoArgCtorMissing {
    private Long id;
    private String name;

    public NoArgCtorMissing(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}
