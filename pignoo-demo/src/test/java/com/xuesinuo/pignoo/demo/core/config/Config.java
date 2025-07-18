package com.xuesinuo.pignoo.demo.core.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.xuesinuo.pignoo.core.Gru;

@Configuration
public class Config {
    @Bean
    Gru gru(@Autowired DataSource dataSource) {
        return new Gru(dataSource);
    }
}
