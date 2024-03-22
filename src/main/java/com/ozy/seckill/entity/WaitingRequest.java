package com.ozy.seckill.entity;

public class WaitingRequest {
    private String token;
    private String url;
    private String startTime;
    private String endTime;
    private String notTwoHour;
    private String firstTwoHour;
    private String username;
    private String userNum;

    public WaitingRequest(String token, String url, String startTime, String endTime, String notTwoHour, String firstTwoHour, String username, String userNum) {
        this.token = token;
        this.url = url;
        this.startTime = startTime;
        this.endTime = endTime;
        this.notTwoHour = notTwoHour;
        this.firstTwoHour = firstTwoHour;
        this.username = username;
        this.userNum = userNum;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getNotTwoHour() {
        return notTwoHour;
    }

    public void setNotTwoHour(String notTwoHour) {
        this.notTwoHour = notTwoHour;
    }

    public String getFirstTwoHour() {
        return firstTwoHour;
    }

    public void setFirstTwoHour(String firstTwoHour) {
        this.firstTwoHour = firstTwoHour;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserNum() {
        return userNum;
    }

    public void setUserNum(String userNum) {
        this.userNum = userNum;
    }

    @Override
    public String toString() {
        return "WaitingRequest{" +
                "token='" + token + '\'' +
                ", url='" + url + '\'' +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                ", notTwoHour='" + notTwoHour + '\'' +
                ", firstTwoHour='" + firstTwoHour + '\'' +
                ", username='" + username + '\'' +
                ", userNum='" + userNum + '\'' +
                '}';
    }
}
