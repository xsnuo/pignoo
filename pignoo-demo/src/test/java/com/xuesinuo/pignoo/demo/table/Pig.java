package com.xuesinuo.pignoo.demo.table;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class Pig {
    private Long id;

    private String name;

    private Integer age;

    private String color;

    private BigDecimal weiGht;
}
