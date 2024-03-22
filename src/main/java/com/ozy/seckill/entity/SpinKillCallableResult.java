package com.ozy.seckill.entity;

public class SpinKillCallableResult {
    @Override
    public String toString() {
        return "SpinKillCallableResult{" +
                "result=" + result +
                ", userNum='" + userNum + '\'' +
                ", phoneNum='" + phoneNum + '\'' +
                '}';
    }

    private Result result;
    private String userNum;
    private String phoneNum;

    public SpinKillCallableResult(Result result, String userNum, String phoneNum) {
        this.result = result;
        this.userNum = userNum;
        this.phoneNum=phoneNum;
    }

    public String getPhoneNum() {
        return phoneNum;
    }

    public void setPhoneNum(String phoneNum) {
        this.phoneNum = phoneNum;
    }

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    public String getUserNum() {
        return userNum;
    }

    public void setUserNum(String userNum) {
        this.userNum = userNum;
    }
}
