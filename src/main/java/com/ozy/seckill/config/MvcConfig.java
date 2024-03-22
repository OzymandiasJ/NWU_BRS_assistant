package com.ozy.seckill.config;


import org.apache.catalina.User;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //统计pv
        registry.addInterceptor(new RequestInterceptor()).addPathPatterns(
                "/visit"
        ).order(1);
        //拦截所有请求,统计uv
        registry.addInterceptor(new UserIntercepter()).addPathPatterns(
                "/seckill/getUser",
                "/seckill/submitTask"
        ).order(2);
        registry.addInterceptor(new LikesCountInterceptor()).addPathPatterns(
                "/like"
        ).order(0);
    }
}
