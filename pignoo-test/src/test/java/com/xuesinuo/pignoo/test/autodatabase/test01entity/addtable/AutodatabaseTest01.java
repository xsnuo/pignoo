package com.xuesinuo.pignoo.test.autodatabase.test01entity.addtable;

import com.xuesinuo.pignoo.core.annotation.Table;

import lombok.Data;

@Table("autodatabase_test01")
@Data
public class AutodatabaseTest01 {
    private Long id;
    private String name;
}
