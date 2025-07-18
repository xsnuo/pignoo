package com.xuesinuo.pignoo.demo.table;

import java.math.BigDecimal;

import com.xuesinuo.pignoo.core.annotation.PrimaryKey;

import lombok.Data;

@Data
public class Pig {
    @PrimaryKey(auto = true)
    private Long id;

    private String name;

    private Integer age;

    private String color;

    private BigDecimal weiGht;
}
