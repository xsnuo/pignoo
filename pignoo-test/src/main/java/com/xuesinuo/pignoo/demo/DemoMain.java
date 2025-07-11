package com.xuesinuo.pignoo.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.xuesinuo.pignoo.annotation.Column;
import com.xuesinuo.pignoo.annotation.PrimaryKey;
import com.xuesinuo.pignoo.annotation.Table;

import lombok.Data;

@SpringBootApplication
public class DemoMain {

    @Table("pig")
    @Data
    public static class Pig {
        @PrimaryKey(auto = true)
        @Column("id")
        private Long id;

        @Column("name")
        private String name;

        @Column("kg")
        private Double kg;
    }

    public static void main(String[] args) {
        SpringApplication.run(DemoMain.class, args);
    }
}
