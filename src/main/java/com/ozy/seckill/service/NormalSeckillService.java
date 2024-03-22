package com.ozy.seckill.service;

import com.ozy.seckill.entity.Result;

public interface NormalSeckillService {
    /**
     * 通用单用户单线程秒杀抢馆方法
     * @param token
     * @param startTime
     * @param endTime
     * @param accept_startTime
     * @param accept_endTime
     * @param notTwoHour
     * @param firstTwoHour
     * @param username
     * @param userNum
     * @return
     */
    Result singleThreadSeckill(String token, String startTime, String endTime, String accept_startTime, String accept_endTime, String notTwoHour, String firstTwoHour, String username,String userNum);
}