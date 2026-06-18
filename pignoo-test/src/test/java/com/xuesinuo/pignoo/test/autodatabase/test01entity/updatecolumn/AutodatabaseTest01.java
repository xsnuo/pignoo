package com.xuesinuo.pignoo.test.autodatabase.test01entity.updatecolumn;

import java.math.BigDecimal;

import com.xuesinuo.pignoo.core.annotation.Column;

import lombok.Data;

@Data
public class AutodatabaseTest01 {
    private Long id;
    private String name;
    private Long code;
    @Column(scale = Column.PresetScale.MEDIUM)
    private String key;
    @Column(scale = Column.PresetScale.MEDIUM)
    private BigDecimal value;
}
