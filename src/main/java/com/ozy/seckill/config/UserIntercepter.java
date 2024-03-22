package com.ozy.seckill.config;

import com.ozy.seckill.entity.WebCount;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class UserIntercepter implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String ipAddress = request.getRemoteAddr();
        //每天统计UV，统一IP在同一天内的访问记一次UV
        if(!WebCount.ips.contains(ipAddress)){
            WebCount.userCount++;
            WebCount.ips.add(ipAddress);
        }
        return true;
    }

}
