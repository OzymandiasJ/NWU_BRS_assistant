package com.ozy.seckill.entity;

import org.springframework.http.ResponseEntity;

public class TaskHandlerCallableResult {
    private VenuePlan plan;
    private int taskId;
    private ResponseEntity<String> responseEntity;

    public TaskHandlerCallableResult(VenuePlan plan, int taskId, ResponseEntity<String> responseEntity) {
        this.plan = plan;
        this.taskId = taskId;
        this.responseEntity = responseEntity;
    }

    public VenuePlan getPlan() {
        return plan;
    }

    public void setPlan(VenuePlan plan) {
        this.plan = plan;
    }

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public ResponseEntity<String> getResponseEntity() {
        return responseEntity;
    }

    public void setResponseEntity(ResponseEntity<String> responseEntity) {
        this.responseEntity = responseEntity;
    }
}
