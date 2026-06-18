package com.xuesinuo.pignoo.test.autodatabase.test01entity.deletecolumn;

import java.math.BigDecimal;

import com.xuesinuo.pignoo.core.annotation.Column;

import lombok.Data;

@Data
public class AutodatabaseTest01 {
    private Long id;
    private String name;
    @Column(scale = Column.PresetScale.LARGE)
    private String key;
    @Column(scale = Column.PresetScale.LARGE)
    private BigDecimal value;
}
