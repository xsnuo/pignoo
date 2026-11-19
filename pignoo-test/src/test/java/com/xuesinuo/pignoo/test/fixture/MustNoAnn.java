package com.xuesinuo.pignoo.test.fixture;

import lombok.Data;

/**
 * 没有@Table也没有@Link，MUST模式下应当解析失败
 */
@Data
public class MustNoAnn {
    private Long id;
    private String name;
}
