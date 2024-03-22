package com.ozy.seckill.service.impl;

import cn.hutool.json.JSONUtil;
import com.ozy.seckill.entity.*;
import com.ozy.seckill.service.NormalSeckillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class NormalSeckillServiceImpl implements NormalSeckillService {
    @Autowired
    RestTemplate restTemplate;
    static boolean failure_503=false;
    @Value("${getVenuePlanChildUrl}")
    String getVenuePlanChildUrl;
    @Value("${saveReservationRecordEntityUrl}")
    String saveReservationRecordEntityUrl;
    static int MAX_504_GatewayTimeout_tryCount=3;

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());
    public Result singleThreadSeckill(String token,
                                      String startTime,
                                      String endTime,
                                      String accept_startTime,
                                      String accept_endTime,
                                      String notTwoHour,
                                      String firstTwoHour,
                                      String username,
                                      String userNum
    ){
        //1,获取所有的场次信息
        // 创建请求头部信息
        String date= LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String url_venue=getVenuePlanChildUrl + date;
        LOG.info(username+userNum+"发起请求："+url_venue);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Token",token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        int retryCount = 0;
        HttpEntity<String> requestEntity=new HttpEntity<>(null, headers);;
        String responseData=null;
        // 发送 GET 请求并获取响应数据
        while (retryCount < MAX_504_GatewayTimeout_tryCount) {
            try {
//                TODO:环境切换块
                ResponseEntity<String> response = restTemplate.exchange(url_venue, HttpMethod.GET, requestEntity, String.class);
                responseData= response.getBody();
                //TODO:环境切换块

                //TODO:环境切换块
//                responseData="{\"msg\":\"success\",\"code\":0,\"venuePlanData\":[{\"useStatusName\":\"可预约\",\"sort\":\"1\",\"addUserStatus\":\"1\",\"useStatus\":0,\"checkStatus\":true,\"price\":\"13.00元\",\"disable\":0,\"venueId\":10,\"name\":\"羽毛球场1\",\"id\":222042,\"time\":\"09:00~10:00\",\"venuePlanId\":383,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"1\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":10,\"name\":\"羽毛球场1\",\"id\":222084,\"time\":\"10:00~11:00\",\"venuePlanId\":390,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"1\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":10,\"name\":\"羽毛球场1\",\"id\":222126,\"time\":\"11:00~12:00\",\"venuePlanId\":397,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"1\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":10,\"name\":\"羽毛球场1\",\"id\":222164,\"time\":\"14:00~15:00\",\"venuePlanId\":404,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"1\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":10,\"name\":\"羽毛球场1\",\"id\":222214,\"time\":\"15:00~16:00\",\"venuePlanId\":470,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"1\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":10,\"name\":\"羽毛球场1\",\"id\":222261,\"time\":\"16:00~17:00\",\"venuePlanId\":411,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"1\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":10,\"name\":\"羽毛球场1\",\"id\":222303,\"time\":\"17:00~18:00\",\"venuePlanId\":418,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"2\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":11,\"name\":\"羽毛球场2\",\"id\":222043,\"time\":\"09:00~10:00\",\"venuePlanId\":384,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"2\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":11,\"name\":\"羽毛球场2\",\"id\":222085,\"time\":\"10:00~11:00\",\"venuePlanId\":391,\"campusInfoId\":null},{\"useStatusName\":\"可预约\",\"sort\":\"2\",\"addUserStatus\":\"1\",\"useStatus\":0,\"checkStatus\":true,\"price\":\"13.00元\",\"disable\":0,\"venueId\":11,\"name\":\"羽毛球场2\",\"id\":222127,\"time\":\"11:00~12:00\",\"venuePlanId\":398,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"2\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":11,\"name\":\"羽毛球场2\",\"id\":222165,\"time\":\"14:00~15:00\",\"venuePlanId\":405,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"2\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":11,\"name\":\"羽毛球场2\",\"id\":222215,\"time\":\"15:00~16:00\",\"venuePlanId\":471,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"2\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":11,\"name\":\"羽毛球场2\",\"id\":222262,\"time\":\"16:00~17:00\",\"venuePlanId\":412,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"3\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":12,\"name\":\"羽毛球场3\",\"id\":222044,\"time\":\"09:00~10:00\",\"venuePlanId\":385,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"3\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":12,\"name\":\"羽毛球场3\",\"id\":222166,\"time\":\"14:00~15:00\",\"venuePlanId\":406,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"3\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":12,\"name\":\"羽毛球场3\",\"id\":222216,\"time\":\"15:00~16:00\",\"venuePlanId\":472,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"3\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":12,\"name\":\"羽毛球场3\",\"id\":222263,\"time\":\"16:00~17:00\",\"venuePlanId\":413,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"3\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":12,\"name\":\"羽毛球场3\",\"id\":222305,\"time\":\"17:00~18:00\",\"venuePlanId\":420,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"4\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":13,\"name\":\"羽毛球场4\",\"id\":222070,\"time\":\"09:00~10:00\",\"venuePlanId\":482,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"4\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":13,\"name\":\"羽毛球场4\",\"id\":222176,\"time\":\"14:00~15:00\",\"venuePlanId\":485,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"4\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":13,\"name\":\"羽毛球场4\",\"id\":222217,\"time\":\"15:00~16:00\",\"venuePlanId\":473,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"5\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":14,\"name\":\"羽毛球场5\",\"id\":222045,\"time\":\"09:00~10:00\",\"venuePlanId\":386,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"5\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":14,\"name\":\"羽毛球场5\",\"id\":222087,\"time\":\"10:00~11:00\",\"venuePlanId\":393,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"5\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":14,\"name\":\"羽毛球场5\",\"id\":222129,\"time\":\"11:00~12:00\",\"venuePlanId\":400,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"5\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":14,\"name\":\"羽毛球场5\",\"id\":222306,\"time\":\"17:00~18:00\",\"venuePlanId\":421,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"5\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":14,\"name\":\"羽毛球场5\",\"id\":222387,\"time\":\"19:00~20:00\",\"venuePlanId\":435,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"5\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"19.50元\",\"disable\":1,\"venueId\":14,\"name\":\"羽毛球场5\",\"id\":222426,\"time\":\"20:00~21:30\",\"venuePlanId\":442,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"6\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":15,\"name\":\"羽毛球场6\",\"id\":222046,\"time\":\"09:00~10:00\",\"venuePlanId\":387,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"6\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":15,\"name\":\"羽毛球场6\",\"id\":222088,\"time\":\"10:00~11:00\",\"venuePlanId\":394,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"6\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":15,\"name\":\"羽毛球场6\",\"id\":222130,\"time\":\"11:00~12:00\",\"venuePlanId\":401,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"6\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":15,\"name\":\"羽毛球场6\",\"id\":222168,\"time\":\"14:00~15:00\",\"venuePlanId\":408,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"6\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":15,\"name\":\"羽毛球场6\",\"id\":222219,\"time\":\"15:00~16:00\",\"venuePlanId\":475,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"6\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":15,\"name\":\"羽毛球场6\",\"id\":222388,\"time\":\"19:00~20:00\",\"venuePlanId\":436,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"6\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"19.50元\",\"disable\":1,\"venueId\":15,\"name\":\"羽毛球场6\",\"id\":222427,\"time\":\"20:00~21:30\",\"venuePlanId\":443,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"7\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":16,\"name\":\"羽毛球场7\",\"id\":222047,\"time\":\"09:00~10:00\",\"venuePlanId\":388,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"7\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":16,\"name\":\"羽毛球场7\",\"id\":222089,\"time\":\"10:00~11:00\",\"venuePlanId\":395,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"7\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":16,\"name\":\"羽毛球场7\",\"id\":222131,\"time\":\"11:00~12:00\",\"venuePlanId\":402,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"7\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":16,\"name\":\"羽毛球场7\",\"id\":222266,\"time\":\"16:00~17:00\",\"venuePlanId\":416,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"7\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":16,\"name\":\"羽毛球场7\",\"id\":222308,\"time\":\"17:00~18:00\",\"venuePlanId\":423,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"7\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":16,\"name\":\"羽毛球场7\",\"id\":222349,\"time\":\"18:00~19:00\",\"venuePlanId\":430,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"7\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":16,\"name\":\"羽毛球场7\",\"id\":222389,\"time\":\"19:00~20:00\",\"venuePlanId\":437,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"7\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"19.50元\",\"disable\":1,\"venueId\":16,\"name\":\"羽毛球场7\",\"id\":222429,\"time\":\"20:00~21:30\",\"venuePlanId\":444,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"8\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":18,\"name\":\"羽毛球场8\",\"id\":222048,\"time\":\"09:00~10:00\",\"venuePlanId\":389,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"8\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":18,\"name\":\"羽毛球场8\",\"id\":222090,\"time\":\"10:00~11:00\",\"venuePlanId\":396,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"8\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":18,\"name\":\"羽毛球场8\",\"id\":222132,\"time\":\"11:00~12:00\",\"venuePlanId\":403,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"8\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":18,\"name\":\"羽毛球场8\",\"id\":222267,\"time\":\"16:00~17:00\",\"venuePlanId\":417,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"8\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":18,\"name\":\"羽毛球场8\",\"id\":222309,\"time\":\"17:00~18:00\",\"venuePlanId\":424,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"8\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":18,\"name\":\"羽毛球场8\",\"id\":222350,\"time\":\"18:00~19:00\",\"venuePlanId\":431,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"8\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":18,\"name\":\"羽毛球场8\",\"id\":222390,\"time\":\"19:00~20:00\",\"venuePlanId\":438,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"8\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"19.50元\",\"disable\":1,\"venueId\":18,\"name\":\"羽毛球场8\",\"id\":222430,\"time\":\"20:00~21:30\",\"venuePlanId\":445,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"9\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"19.50元\",\"disable\":1,\"venueId\":19,\"name\":\"羽毛球场9\",\"id\":222058,\"time\":\"09:00~10:30\",\"venuePlanId\":446,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"9\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"19.50元\",\"disable\":1,\"venueId\":19,\"name\":\"羽毛球场9\",\"id\":222110,\"time\":\"10:30~12:00\",\"venuePlanId\":450,\"campusInfoId\":null},{\"useStatusName\":\"可预约\",\"sort\":\"9\",\"addUserStatus\":\"1\",\"useStatus\":0,\"checkStatus\":true,\"price\":\"26.00元\",\"disable\":0,\"venueId\":19,\"name\":\"羽毛球场9\",\"id\":222269,\"time\":\"16:00~18:00\",\"venuePlanId\":458,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"9\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"26.00元\",\"disable\":1,\"venueId\":19,\"name\":\"羽毛球场9\",\"id\":222352,\"time\":\"18:00~20:00\",\"venuePlanId\":462,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"9\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"19.50元\",\"disable\":1,\"venueId\":19,\"name\":\"羽毛球场9\",\"id\":222431,\"time\":\"20:00~21:30\",\"venuePlanId\":466,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"10\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"19.50元\",\"disable\":1,\"venueId\":20,\"name\":\"羽毛球场10\",\"id\":222060,\"time\":\"09:00~10:30\",\"venuePlanId\":447,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"10\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"19.50元\",\"disable\":1,\"venueId\":20,\"name\":\"羽毛球场10\",\"id\":222111,\"time\":\"10:30~12:00\",\"venuePlanId\":451,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"10\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"26.00元\",\"disable\":1,\"venueId\":20,\"name\":\"羽毛球场10\",\"id\":222270,\"time\":\"16:00~18:00\",\"venuePlanId\":459,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"10\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"26.00元\",\"disable\":1,\"venueId\":20,\"name\":\"羽毛球场10\",\"id\":222353,\"time\":\"18:00~20:00\",\"venuePlanId\":463,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"10\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"19.50元\",\"disable\":1,\"venueId\":20,\"name\":\"羽毛球场10\",\"id\":222432,\"time\":\"20:00~21:30\",\"venuePlanId\":467,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"11\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"19.50元\",\"disable\":1,\"venueId\":21,\"name\":\"羽毛球场11\",\"id\":222062,\"time\":\"09:00~10:30\",\"venuePlanId\":448,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"11\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"19.50元\",\"disable\":1,\"venueId\":21,\"name\":\"羽毛球场11\",\"id\":222112,\"time\":\"10:30~12:00\",\"venuePlanId\":452,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"11\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"26.00元\",\"disable\":1,\"venueId\":21,\"name\":\"羽毛球场11\",\"id\":222271,\"time\":\"16:00~18:00\",\"venuePlanId\":460,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"11\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"26.00元\",\"disable\":1,\"venueId\":21,\"name\":\"羽毛球场11\",\"id\":222354,\"time\":\"18:00~20:00\",\"venuePlanId\":464,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"11\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"19.50元\",\"disable\":1,\"venueId\":21,\"name\":\"羽毛球场11\",\"id\":222433,\"time\":\"20:00~21:30\",\"venuePlanId\":468,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"12\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"19.50元\",\"disable\":1,\"venueId\":22,\"name\":\"羽毛球场12\",\"id\":222064,\"time\":\"09:00~10:30\",\"venuePlanId\":449,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"12\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"19.50元\",\"disable\":1,\"venueId\":22,\"name\":\"羽毛球场12\",\"id\":222113,\"time\":\"10:30~12:00\",\"venuePlanId\":453,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"12\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"26.00元\",\"disable\":1,\"venueId\":22,\"name\":\"羽毛球场12\",\"id\":222272,\"time\":\"16:00~18:00\",\"venuePlanId\":461,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"12\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":22,\"name\":\"羽毛球场12\",\"id\":222333,\"time\":\"18:00~19:00\",\"venuePlanId\":599,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"12\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":22,\"name\":\"羽毛球场12\",\"id\":222392,\"time\":\"19:00~20:00\",\"venuePlanId\":465,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"12\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"19.50元\",\"disable\":1,\"venueId\":22,\"name\":\"羽毛球场12\",\"id\":222434,\"time\":\"20:00~21:30\",\"venuePlanId\":469,\"campusInfoId\":null}]}\n";
                //TODO:环境切换块
                break; // 如果成功获取响应，跳出循环
            } catch (HttpServerErrorException.GatewayTimeout e) {
                // 捕获 504 错误
                retryCount++;
                if (retryCount < MAX_504_GatewayTimeout_tryCount) {
                    // 添加延迟
                    try {
                        Thread.sleep(10000); // 等待10秒后重试
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    // 达到最大重试次数，处理重试失败的情况
                    // ...
                    failure_503=true;
                    LOG.warn(username+userNum+"失败：目标服务器宕机，已重试3次，达到最大重试次数");
                    return Result.ok("失败：目标服务器宕机，已重试3次，达到最大重试次数");
                }
            }
        }
        //打印响应数据

        LOG.info(username+userNum+"获取到了responseData");
        //解析json数据
        VenuePlanDataMsg venuePlanData = JSONUtil.toBean(responseData, VenuePlanDataMsg.class);
        List<VenuePlan> planData = venuePlanData.getVenuePlanData();
        ResponseEntity<String> responseEntity=null;
        ResultDTO resultDTO = null;
        long taskStartTime = System.currentTimeMillis();
        int statusCode=0;
        int count=0;
        Body body=null;
        boolean obeyCondition=true;
        for (int k = 0; k < 2; k++) {
            for (VenuePlan plan : planData) {
                //对于每个plan,首先判断useStatus值是不是0，不是的话说明不可预约下一个
                Integer useStatus = plan.getUseStatus();
                if(useStatus!=0){
                    continue;
                }

                //useStatus值为0，判断time是不是在给定时间范围内，不是的话下一个
                String time = plan.getTime();
                Integer plan_beginTime=Integer.valueOf(time.split("~")[0].split(":")[0]);
                Integer plan_endTime=Integer.valueOf(time.split("~")[1].split(":")[0]);
                //非绝对限制条件，第二轮可以不满足这几个条件
                if(obeyCondition){
                    if(plan_beginTime<Integer.parseInt(startTime)||plan_endTime>Integer.parseInt(endTime))
                        continue;
                    //优先两小时
                    if(Boolean.parseBoolean(firstTwoHour)&&!plan.getPrice().equals("26.00元")){
                        continue;//不是的话下一个
                    }
                }
                //绝对限制条件，不能动,第二轮仍然要满足这几个条件
                if(plan_beginTime<Integer.parseInt(accept_startTime)||plan_endTime>Integer.parseInt(accept_endTime))
                    continue;
                if(Boolean.parseBoolean(notTwoHour)&&plan.getPrice().equals("26.00元")){
                    continue;//不是的话下一个
                }

                //上述条件都满足的话，所有条件都满足了，该plan可用
                count++;
                //发起请求，获取结果
                //TODO:线上环境代码
                responseEntity=taskHandler(token,plan,username,userNum);
                statusCode = responseEntity.getStatusCodeValue();
                String responseBody = responseEntity.getBody();
                //TODO:线上环境代码

                //TODO:测试环境代码1
//                responseEntity=new ResponseEntity<>(HttpStatus.ACCEPTED);
//                statusCode = 200;
//                String responseBody = "{\"msg\":\"success\",\"code\":0,\"payurl\":\"https://cwcwx.nwu.edu.cn/zhifu/payAccept.aspx?prePayId=f6724cba0d143e*******d5cad7e856c08e3ea2956f1f\"}";
                //TODO:测试环境代码1

                //模拟测试数据提前刷新成可预约但实际预约不了
                //TODO:测试环境代码2
//                responseEntity=new ResponseEntity<>(HttpStatus.ACCEPTED);
//                statusCode = 200;
//                String responseBody = "{\"msg\":\"场地已被抢走，请您下次早点预约！\",\"code\":500}";
                //TODO:测试环境代码2
                if(responseEntity==null){
                    return Result.ok("目标服务器宕机！！！已经达到最大重试次数"+MAX_504_GatewayTimeout_tryCount+"，此用户任务失败，已返回\"");
                }

                body = JSONUtil.toBean(responseBody, Body.class);
                if(statusCode==200&&body.getMsg().equals("success")){
                    //封装DTO
                    LOG.info(username+userNum+"请求结果:预定成功！！！场次："+plan.getTime()+" 场地："+plan.getName());
                    long taskEndTime = System.currentTimeMillis();
                    long elapsedTimeMillis = taskEndTime - taskStartTime;
                    resultDTO=new ResultDTO(true,body,plan.getName(),plan.getTime(),plan.getPrice(),body.getPayurl(),count, (int) (elapsedTimeMillis/1000));
                    return Result.ok(resultDTO);
                } else if (statusCode==200&&body.getCode()==500&&body.getMsg().equals("您有未支付的场地预约信息，请先支付！")) {
                    LOG.info(username+userNum+"请求结果:您有未支付的场地预约信息，请先支付！");
                    return Result.ok("失败：您有未支付的场地预约信息，请先支付！");
                }else if (statusCode==200&&body.getCode()==500&&body.getMsg().equals("您有未使用的场地预约信息，请消费完后再进行预约！")) {
                    LOG.info(username+userNum+"失败：您有未使用的场地预约信息，请消费完后再进行预约！");
                    return Result.ok("失败：您有未使用的场地预约信息，请消费完后再进行预约！");
                } else {//对不起，场地被抢走
//                    long taskEndTime = System.currentTimeMillis();
//                    long elapsedTimeMillis = taskEndTime - taskStartTime;
//                    resultDTO=new ResultDTO(false,body,null,null,null,0, (int)(elapsedTimeMillis/1000));
                }
            }
            if(resultDTO!=null){
                break;
            }else {
                LOG.info("非绝对条件下无匹配场次，取消非绝对条件重新查询");
                //obeyCondition修改为false，重新来一次循环
                obeyCondition=false;
            }
        }
        //没有可预约的场次
        LOG.info(username+userNum+"请求结果:没有选定时间范围内有效场次，可以尝试扩大时间范围再次尝试，或者可能暂未放场");
        return Result.fail("没有选定时间范围内有效场次，可以尝试扩大时间范围再次尝试，或者可能暂未放场");
    }
    public ResponseEntity<String> taskHandler(String token,VenuePlan plan,String username,String userNum) {
        HttpHeaders headers = new HttpHeaders();
        //创建header
        headers.set("Token", token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Params params = new Params(plan.getId(),plan.getVenueId(),plan.getName(),plan.getVenuePlanId(),1,null);
        HttpEntity<String> requestEntity = new HttpEntity<>(JSONUtil.toJsonStr(params), headers);
        // 发送 GET 请求并获取响应数据
        String url = saveReservationRecordEntityUrl;
        int retryCount = 0;
        ResponseEntity<String> responseEntity = null;
        while (retryCount < MAX_504_GatewayTimeout_tryCount) {
            try {
                LOG.info(username+userNum+"发起一次抢馆请求：场次："+plan.getTime()+" 场地："+plan.getName()+"  url:"+url);
                responseEntity = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
                // 处理响应
                String body = responseEntity.getBody();
                LOG.info(username+userNum+"请求响应body:"+body);
                break; // 如果成功获取响应，跳出循环
            } catch (HttpServerErrorException.GatewayTimeout e) {
                // 捕获 504 错误
                LOG.warn(userNum+username+"：目标服务器宕机！！！发生504_GatewayTimeout错误！！！60s后重试");
                retryCount++;
                if (retryCount < MAX_504_GatewayTimeout_tryCount) {
                    // 添加延迟
                    try {
                        Thread.sleep(30000); // 等待30秒后重试
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    LOG.warn(userNum+username+"：目标服务器宕机！！！已经达到最大重试次数"+MAX_504_GatewayTimeout_tryCount+"，此用户任务失败，已返回");
                    // 达到最大重试次数，处理重试失败的情况
                    responseEntity = null;
                }
            }
        }
        return responseEntity;

    }
}
