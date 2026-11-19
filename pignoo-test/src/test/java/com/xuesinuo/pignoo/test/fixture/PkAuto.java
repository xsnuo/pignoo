package com.xuesinuo.pignoo.test.fixture;

import com.xuesinuo.pignoo.core.annotation.Column;
import com.xuesinuo.pignoo.core.annotation.Table;

import lombok.Data;

/**
 * 显式标注的自增主键，且主键字段不叫id
 */
@Table("fx_pk_auto")
@Data
public class PkAuto {
    @Column(primaryKey = Column.PrimaryKey.AUTO)
    private Long autoKey;
    private String name;
}
