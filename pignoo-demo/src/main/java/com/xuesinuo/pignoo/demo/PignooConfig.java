package com.xuesinuo.pignoo.demo;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.xuesinuo.pignoo.Gru;
import com.xuesinuo.pignoo.Pignoo.DatabaseEngine;

@Configuration
public class PignooConfig {
    @Bean
    Gru gru(DataSource dataSource) {
        return new Gru(DatabaseEngine.MySQL, dataSource);
    }
}
