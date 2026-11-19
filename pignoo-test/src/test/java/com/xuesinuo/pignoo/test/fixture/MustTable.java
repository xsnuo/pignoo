package com.xuesinuo.pignoo.test.fixture;

import com.xuesinuo.pignoo.core.annotation.Column;
import com.xuesinuo.pignoo.core.annotation.Table;

import lombok.Data;

/**
 * MUST模式下可用的实体：类上有@Table，字段上有@Column
 */
@Table("fx_must")
@Data
public class MustTable {
    @Column
    private Long id;
    @Column
    private String name;
}
