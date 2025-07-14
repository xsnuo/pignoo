package com.xuesinuo.pignoo.demo.core.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.xuesinuo.pignoo.core.Gru;
import com.xuesinuo.pignoo.core.PignooConfig;
import com.xuesinuo.pignoo.core.Pignoo.DatabaseEngine;

@Configuration
public class Config {
    @Bean
    Gru gru(@Autowired DataSource dataSource) {
        PignooConfig pignooConfig = new PignooConfig();
        pignooConfig.setEngine(DatabaseEngine.MySQL);
        return new Gru(dataSource, pignooConfig);
    }
}
