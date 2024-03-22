package com.ozy.seckill.entity;

public class Body {
    public Body() {
    }

    String msg;
    int code;
    String payurl;

    public Body(String msg, int code, String payurl) {
        this.msg = msg;
        this.code = code;
        this.payurl = payurl;
    }

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

    public String getPayurl() {
        return payurl;
    }

    public void setPayurl(String payurl) {
        this.payurl = payurl;
    }

    @Override
    public String toString() {
        return "Body{" +
                "msg='" + msg + '\'' +
                ", code=" + code +
                ", payurl='" + payurl + '\'' +
                '}';
    }
}
