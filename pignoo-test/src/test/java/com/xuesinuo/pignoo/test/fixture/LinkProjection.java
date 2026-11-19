package com.xuesinuo.pignoo.test.fixture;

import com.xuesinuo.pignoo.core.annotation.Link;

import lombok.Data;

/**
 * Plain的投影，只关心其中两列，共用fx_plain表
 */
@Link(Plain.class)
@Data
public class LinkProjection {
    private Long id;
    private String name;
}
