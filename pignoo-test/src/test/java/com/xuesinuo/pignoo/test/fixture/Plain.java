package com.xuesinuo.pignoo.test.fixture;

import java.math.BigDecimal;

import com.xuesinuo.pignoo.core.annotation.Table;

import lombok.Data;

/**
 * 最普通的实体：主键靠默认约定的id
 */
@Table("fx_plain")
@Data
public class Plain {
    private Long id;
    private String name;
    private String code;
    private BigDecimal money;
    private Integer qty;
}
