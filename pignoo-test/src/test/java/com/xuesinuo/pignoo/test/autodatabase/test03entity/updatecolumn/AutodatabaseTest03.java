package com.xuesinuo.pignoo.test.autodatabase.test03entity.updatecolumn;

import com.xuesinuo.pignoo.core.annotation.Column;

import lombok.Data;

@Data
public class AutodatabaseTest03 {
    private Long id;
    @Column
    private String str1;
    @Column(scale = 9999)
    private String str2;
    @Column(scale = 255)
    private String str3;
    @Column(scale = -3)
    private String str4;
    @Column(scale = 10)
    private String strNumber;
    @Column
    private String number;
}
