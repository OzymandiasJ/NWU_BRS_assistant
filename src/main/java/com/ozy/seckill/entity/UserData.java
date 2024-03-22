package com.ozy.seckill.entity;

public class UserData {
    private String userNum;
    private String sex;
    private String mobile;
    private int id;
    private boolean isVip;
    private String userType;
    private String type;
    private String email;
    private String username;
    private SpinKillCallableResult spinKillCallableResult;

    public UserData(String userNum, String sex, String mobile, int id, boolean isVip, String userType, String type, String email, String username, SpinKillCallableResult spinKillCallableResult) {
        this.userNum = userNum;
        this.sex = sex;
        this.mobile = mobile;
        this.id = id;
        this.isVip = isVip;
        this.userType = userType;
        this.type = type;
        this.email = email;
        this.username = username;
        this.spinKillCallableResult = spinKillCallableResult;
    }
}