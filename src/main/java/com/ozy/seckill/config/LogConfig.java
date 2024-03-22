package com.ozy.seckill.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LogConfig {
    @Value("${devMode}")
    private String devMode;
    private static final Logger LOG = LoggerFactory.getLogger(LogConfig.class);

    @Bean
    public void logMethod() {
        LOG.info("==========log configed==========");
        if(devMode.equals("true")){
            LOG.info("项目启动模式为：dev");
        }else{
            LOG.info("项目启动模式为：product");
        }
    }
}
