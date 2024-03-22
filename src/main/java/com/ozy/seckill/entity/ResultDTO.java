package com.ozy.seckill.entity;

public class ResultDTO {
    boolean success;
    Body body;
    String name;
    String time;
    String price;
    int requestNum;
    int seconds;
    String payurl;


    public ResultDTO(boolean success, Body body, String name, String time, String price, String payurl, int requestNum, int seconds) {
        this.success = success;
        this.body = body;
        this.name = name;
        this.time = time;
        this.price = price;
        this.requestNum = requestNum;
        this.seconds = seconds;
        this.payurl=payurl;
    }


    public String getPayurl() {
        return payurl;
    }

    public void setPayurl(String payurl) {
        this.payurl = payurl;
    }
    public boolean getSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Body getBody() {
        return body;
    }

    public void setBody(Body body) {
        this.body = body;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public int getRequestNum() {
        return requestNum;
    }

    public void setRequestNum(int requestNum) {
        this.requestNum = requestNum;
    }

    public int getSeconds() {
        return seconds;
    }

    public void setSeconds(int seconds) {
        this.seconds = seconds;
    }
}
