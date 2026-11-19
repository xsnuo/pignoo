package com.xuesinuo.pignoo.test.fixture;

import com.xuesinuo.pignoo.core.annotation.Column;
import com.xuesinuo.pignoo.core.annotation.Table;

import lombok.Data;

/**
 * 非自增主键，主键值由调用方给定
 */
@Table("fx_pk_non_auto")
@Data
public class PkNonAuto {
    @Column(primaryKey = Column.PrimaryKey.NON_AUTO)
    private String bizKey;
    private String name;
}
