package com.ozy.seckill.entity;

import org.springframework.web.client.RestTemplate;

/**
 * 多线程发起抢购任务
 */
public class VenuePlanTask {
    private String token;
    private VenuePlan plan;

    public VenuePlanTask(String token, VenuePlan plan) {
        this.token = token;
        this.plan = plan;
    }
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public VenuePlan getPlan() {
        return plan;
    }

    public void setPlan(VenuePlan plan) {
        this.plan = plan;
    }
}
