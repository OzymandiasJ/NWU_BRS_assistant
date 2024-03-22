package com.ozy.seckill.config;

import com.ozy.seckill.entity.WebCount;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RequestInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        WebCount.visitCount++;
//        int cacheExpirationSeconds = 2 * 60 * 60;
//        response.setHeader("Cache-Control", "max-age=" + cacheExpirationSeconds);
        return false;
    }

}
