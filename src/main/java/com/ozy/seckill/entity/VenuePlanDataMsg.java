package com.ozy.seckill.entity;

import java.util.List;

public class VenuePlanDataMsg {
    private String msg;
    private int code;
    private List<VenuePlan> venuePlanData;

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public List<VenuePlan> getVenuePlanData() {
        return venuePlanData;
    }

    public void setVenuePlanData(List<VenuePlan> venuePlanData) {
        this.venuePlanData = venuePlanData;
    }
}
