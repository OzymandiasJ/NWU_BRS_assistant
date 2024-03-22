package com.ozy.seckill.config;

import com.ozy.seckill.entity.WebCount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LikesCountInterceptor implements HandlerInterceptor {
    private  final Logger LOG = LoggerFactory.getLogger(this.getClass());
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        WebCount.likesCount++;
        String username = request.getParameter("username");
        LOG.info("收到了一个来自用户<"+username+">的点赞，开心(^_^)");
        return false;
    }

}