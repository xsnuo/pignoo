package com.xuesinuo.pignoo.test.autodatabase.test01entity.addcolumn;

import java.math.BigDecimal;

import com.xuesinuo.pignoo.core.annotation.Column;
import com.xuesinuo.pignoo.core.annotation.Table;

import lombok.Data;

@Table
@Data
public class AutodatabaseTest01 {
    private Long id;
    private String name;
    private String code;
    @Column(scale = Column.PresetScale.SMALL)
    private String key;
    @Column(scale = Column.PresetScale.SMALL)
    private BigDecimal value;
}
