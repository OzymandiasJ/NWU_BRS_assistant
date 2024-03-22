package com.ozy.seckill.service;

import com.ozy.seckill.entity.Result;

public interface SuperSeckillService {

    /**
     * vip服务，提供自旋任务+多线程功能
     * @param token
     * @param startTime
     * @param endTime
     * @param notTwoHour
     * @param firstTwoHour
     * @param username
     * @return
     */
    public Result spinKill(String token, String startTime, String endTime, String accept_startTime, String accept_endTime, String notTwoHour, String firstTwoHour, String username,String userNum,String phoneNum);
}
