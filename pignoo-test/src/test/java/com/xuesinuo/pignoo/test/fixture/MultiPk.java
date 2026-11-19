package com.xuesinuo.pignoo.test.fixture;

import com.xuesinuo.pignoo.core.annotation.Column;

import lombok.Data;

/**
 * 坏实体：标了两个主键，解析时应当失败
 */
@Data
public class MultiPk {
    @Column(primaryKey = Column.PrimaryKey.AUTO)
    private Long id;
    @Column(primaryKey = Column.PrimaryKey.AUTO)
    private Long otherKey;
}
