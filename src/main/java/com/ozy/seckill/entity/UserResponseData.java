package com.ozy.seckill.entity;

public class UserResponseData {
    private String msg;
    private int code;
    private UserData user;
    public static class UserData {
        private String userNum;
        private String sex;
        private String mobile;
        private int id;
        private boolean isVip;
        private int taskStatus;//0没有，1正在等待，2失败了，3成功
        private SpinKillCallableResult taskResult;
        private String userType;
        private String type;
        private String email;
        private String username;

        public UserData(String userNum, boolean isVip,int taskStatus,SpinKillCallableResult taskResult,String sex, String mobile, int id, String userType, String type, String email, String username) {
            this.userNum = userNum;
            this.isVip = isVip;
            this.taskStatus=taskStatus;
            this.taskResult=taskResult;
            this.sex = sex;
            this.mobile = mobile;
            this.id = id;
            this.userType = userType;
            this.type = type;
            this.email = email;
            this.username = username;
        }
    }

    public UserResponseData(String msg, int code, UserData user) {
        this.msg = msg;
        this.code = code;
        this.user = user;
    }
}

