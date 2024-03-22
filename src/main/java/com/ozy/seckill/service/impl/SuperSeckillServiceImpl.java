package com.ozy.seckill.service.impl;

import cn.hutool.json.JSONUtil;
import com.ozy.seckill.entity.*;
import com.ozy.seckill.service.SMSService;
import com.ozy.seckill.service.SuperSeckillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Service
public class SuperSeckillServiceImpl implements SuperSeckillService {
    @Value("${adminUser}")
    private String[] adminUser;
    @Value("${devMode}")
    private String devMode;
    @Value("${getVenuePlanChildUrl}")
    String getVenuePlanChildUrl;
    @Value("${saveReservationRecordEntityUrl}")
    String saveReservationRecordEntityUrl;

    // 创建 RestTemplate 实例
    @Autowired
    RestTemplate restTemplate;


    @Autowired
    ScheduleService scheduleServiceTest;

    //定义用户任务结果，Result Map<userNum,Result>结合
    static HashMap<String,Result> userResultMap=new HashMap<>();

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());
    //最大支持100个线程
    static int numberOfTasks = 100;
    static int MAX_504_GatewayTimeout_tryCount=20;
    static boolean failure_503=false;
    static int Sentinel_MAX_504_GatewayTimeout_tryCount=60;

    // 创建线程池，非静态，不存在线程安全问题，多个用户之间也不冲突
    static ExecutorService executor = Executors.newFixedThreadPool(numberOfTasks);

    private class TaskHandlerCallable implements Callable<TaskHandlerCallableResult> {
        private VenuePlan plan;
        private int taskId;
        private VenuePlanTask venuePlanTask;
        private String username;
        private String userNum;
        public TaskHandlerCallable(VenuePlanTask venuePlanTask, int taskId,VenuePlan plan,String username,String userNum) {
            this.venuePlanTask = venuePlanTask;
            this.taskId=taskId;
            this.plan=plan;
            this.username=username;
            this.userNum=userNum;
        }
        @Override
        public TaskHandlerCallableResult call() throws Exception {
            String token = venuePlanTask.getToken();
            VenuePlan plan = venuePlanTask.getPlan();

            HttpHeaders headers = new HttpHeaders();
            //创建header
            headers.set("Token", token);
            headers.setContentType(MediaType.APPLICATION_JSON);
            Params params = new Params(plan.getId(),plan.getVenueId(),plan.getName(),plan.getVenuePlanId(),1,null);
            LOG.info(username+userNum+"发起一次抢馆请求："+plan.getName()+plan.getTime());
            HttpEntity<String> requestEntity = new HttpEntity<>(JSONUtil.toJsonStr(params), headers);
            // 发送 GET 请求并获取响应数据
            String url = saveReservationRecordEntityUrl;
            ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            String body = responseEntity.getBody();
            LOG.info(username+userNum+"的请求响应body:"+body);
            TaskHandlerCallableResult result=new TaskHandlerCallableResult(plan,taskId,responseEntity);
            return result;
        }
    }

    //线程结果集
    static Map<String,SpinKillCallableResult> spinKillCallableResultsMap =new HashMap<>();
    //线程池运行结果
    static List<Future<SpinKillCallableResult>> spinKillCallableFutures = new ArrayList<>();
    //哨兵线程
    static SpinKillCallable sentinel=null;
    static List<SpinKillCallable> spinKillCallables=new ArrayList<>();
    //为了加速00:30的查询，哨兵保存的场次列表结果
    static List<VenuePlan> sentinelSavedPlanData =null;
    /**
     * vip服务，提供自旋任务功能
     * @param token
     * @param startTime
     * @param endTime
     * @param notTwoHour
     * @param firstTwoHour
     * @param username
     * @return
     */
    public Result spinKill(String token, String startTime, String endTime,  String accept_startTime, String accept_endTime,String notTwoHour, String firstTwoHour, String username ,String userNum,String phoneNum){

        //00:30-08:00不能提交请求
        if(devMode.equals("false")&& LocalTime.now().isAfter(LocalTime.of(0, 30))&&LocalTime.now().isBefore(LocalTime.of(8, 00))){
            LOG.info(username+userNum+"尝试在"+LocalTime.now()+"提交预约任务，已拒绝");
            return Result.fail("失败:00:30——8:00之间不支持预约任务，仅支持直抢任务，请8:00之后重试");
        }
        //然后，一个用户不能发多个预约请求
        if(isContainsUserNum(userNum)){
            LOG.info(username+userNum+"尝试多次多个任务，已拒绝");
            return Result.fail("失败:一个用户一天只允许预约一次!");
        }
        SpinKillCallable spinKillCallable = new SpinKillCallable(token,startTime,endTime,accept_startTime,accept_endTime,notTwoHour,firstTwoHour,username,userNum,phoneNum);
        spinKillCallables.add(spinKillCallable);
        LOG.info(username+userNum+"的预约任务已提交");
        return Result.fail("成功：预约任务已提交，预约结果会通过短信下发到您的手机号"+phoneNum+"(如需修改手机号请先取消任务然后前往NWU场馆预约系统修改)，也可以回来本页面查询结果，如果预约成功请及时前往NWU场馆预约系统支付，超时5分钟未支付将释放场次");
    }
    public static boolean isContainsUserNum(String userNum){
        for (SuperSeckillServiceImpl.SpinKillCallable spinKillCallable : SuperSeckillServiceImpl.spinKillCallables) {
            if(spinKillCallable.getUserNum().equals(userNum)){
                return true;
            }
        }
        for (Map.Entry<String, SpinKillCallableResult> entry : SuperSeckillServiceImpl.spinKillCallableResultsMap.entrySet()) {
            if(entry.getKey().equals(userNum)){
                return true;
            }
        }
        return false;
    }
    class SpinKillCallable implements Callable<SpinKillCallableResult>{

        private String token;
        private String startTime;
        private String endTime;
        private String accept_startTime;
        private String accept_endTime;
        private String notTwoHour;
        private String firstTwoHour;
        private String username;
        private String userNum;
        private String phoneNum;
        private boolean isSentinel;

        public String getPhoneNum() {
            return phoneNum;
        }

        public void setPhoneNum(String phoneNum) {
            this.phoneNum = phoneNum;
        }

        public String getUserNum() {
            return userNum;
        }

        public String getUsername() {
            return username;
        }

        public String getThreadUserInfo() {
            return userNum+username;
        }

        public boolean isSentinel() {
            return isSentinel;
        }

        public void setSentinel(boolean sentinel) {
            isSentinel = sentinel;
        }

        public SpinKillCallable(String token, String startTime, String endTime, String accept_startTime, String accept_endTime, String notTwoHour, String firstTwoHour, String username, String userNum, String phoneNum) {
            this.token = token;
            this.startTime = startTime;
            this.endTime = endTime;
            this.accept_startTime = accept_startTime;
            this.accept_endTime = accept_endTime;
            this.notTwoHour = notTwoHour;
            this.firstTwoHour = firstTwoHour;
            this.username = username;
            this.userNum = userNum;
            this.phoneNum=phoneNum;
        }

        @Override
        public SpinKillCallableResult call() throws Exception {
            Result result= singleThreadSeckill(token,startTime,endTime,accept_startTime,accept_endTime,notTwoHour,firstTwoHour,username,userNum,phoneNum);
            SpinKillCallableResult spinKillCallableResult=new SpinKillCallableResult(result,userNum,phoneNum);
            return spinKillCallableResult;
        }
    }




    /**
     * 重构，将任务模式和直抢模式的normalSeckillService下的singleThreadSeckill方法分开
     */

    /**
     * 回避map，记录当前key为userNum的用户已经回避的次数
     * key:学号
     * value：已经退避的次数
     */
    static ConcurrentHashMap<String,Integer> avoidCountMap=null;
    static HashMap<String,String> payUrlMap=new HashMap<>();
    static boolean obeyConditionHaveChangedTag =false;
    static boolean Tag_0030 =false;
    static Object lock=new Object();
    /**
     * key：场次信息，只有两个值，一个是startTime,一个是endTime
     * value，list集合，存放目标场次为key的用户学号信息
     */
    static ConcurrentHashMap<String,List<String>> collideMap=null;
    @Autowired
    SMSService smsService;
    public Result singleThreadSeckill(String token,
                                      String startTime,
                                      String endTime,
                                      String accept_startTime,
                                      String accept_endTime,
                                      String notTwoHour,
                                      String firstTwoHour,
                                      String username,
                                      String userNum,
                                      String phoneNum
    ){
        List<VenuePlan> planData=null;
        //让哨兵获取查询数据
        if(!Tag_0030||sentinelSavedPlanData==null){
            //(如果当前是普通任务)或者(当前是哨兵任务且当前哨兵查询到没有可用场次信息)就直接请求
            //1,获取所有的场次信息
            // 创建请求头部信息
            String date= LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String url_venue=getVenuePlanChildUrl + date;
            LOG.info(username+userNum+"发起请求："+url_venue);
            HttpHeaders headers = new HttpHeaders();
            headers.set("Token",token);
            headers.setContentType(MediaType.APPLICATION_JSON);
            // 创建请求实体
            int retryCount = 0;
            HttpEntity<String> requestEntity=new HttpEntity<>(null, headers);;
            String responseData=null;
            while (retryCount < Sentinel_MAX_504_GatewayTimeout_tryCount) {
                try {
                    //TODO:环境切换块
                    ResponseEntity<String> response = restTemplate.exchange(url_venue, HttpMethod.GET, requestEntity, String.class);
                    responseData= response.getBody();
                    //TODO:环境切换块

                    //TODO:环境切换块——有可用场次
//                    responseData="{\"msg\":\"success\",\"code\":0,\"venuePlanData\":[{\"useStatusName\":\"可预约\",\"sort\":\"1\",\"addUserStatus\":\"1\",\"useStatus\":0,\"checkStatus\":true,\"price\":\"13.00元\",\"disable\":0,\"venueId\":10,\"name\":\"羽毛球场1\",\"id\":222042,\"time\":\"09:00~10:00\",\"venuePlanId\":383,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"1\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":10,\"name\":\"羽毛球场1\",\"id\":222084,\"time\":\"10:00~11:00\",\"venuePlanId\":390,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"1\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":10,\"name\":\"羽毛球场1\",\"id\":222126,\"time\":\"11:00~12:00\",\"venuePlanId\":397,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"1\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":10,\"name\":\"羽毛球场1\",\"id\":222164,\"time\":\"14:00~15:00\",\"venuePlanId\":404,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"1\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":10,\"name\":\"羽毛球场1\",\"id\":222214,\"time\":\"15:00~16:00\",\"venuePlanId\":470,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"1\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":10,\"name\":\"羽毛球场1\",\"id\":222261,\"time\":\"16:00~17:00\",\"venuePlanId\":411,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"1\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":10,\"name\":\"羽毛球场1\",\"id\":222303,\"time\":\"17:00~18:00\",\"venuePlanId\":418,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"2\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":11,\"name\":\"羽毛球场2\",\"id\":222043,\"time\":\"09:00~10:00\",\"venuePlanId\":384,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"2\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":11,\"name\":\"羽毛球场2\",\"id\":222085,\"time\":\"10:00~11:00\",\"venuePlanId\":391,\"campusInfoId\":null},{\"useStatusName\":\"可预约\",\"sort\":\"2\",\"addUserStatus\":\"1\",\"useStatus\":0,\"checkStatus\":true,\"price\":\"13.00元\",\"disable\":0,\"venueId\":11,\"name\":\"羽毛球场2\",\"id\":222127,\"time\":\"11:00~12:00\",\"venuePlanId\":398,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"2\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":11,\"name\":\"羽毛球场2\",\"id\":222165,\"time\":\"14:00~15:00\",\"venuePlanId\":405,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"2\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":11,\"name\":\"羽毛球场2\",\"id\":222215,\"time\":\"15:00~16:00\",\"venuePlanId\":471,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"2\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":11,\"name\":\"羽毛球场2\",\"id\":222262,\"time\":\"16:00~17:00\",\"venuePlanId\":412,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"3\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":12,\"name\":\"羽毛球场3\",\"id\":222044,\"time\":\"09:00~10:00\",\"venuePlanId\":385,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"3\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":12,\"name\":\"羽毛球场3\",\"id\":222166,\"time\":\"14:00~15:00\",\"venuePlanId\":406,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"3\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":12,\"name\":\"羽毛球场3\",\"id\":222216,\"time\":\"15:00~16:00\",\"venuePlanId\":472,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"3\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":12,\"name\":\"羽毛球场3\",\"id\":222263,\"time\":\"16:00~17:00\",\"venuePlanId\":413,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"3\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":12,\"name\":\"羽毛球场3\",\"id\":222305,\"time\":\"17:00~18:00\",\"venuePlanId\":420,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"4\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":13,\"name\":\"羽毛球场4\",\"id\":222070,\"time\":\"09:00~10:00\",\"venuePlanId\":482,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"4\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":13,\"name\":\"羽毛球场4\",\"id\":222176,\"time\":\"14:00~15:00\",\"venuePlanId\":485,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"4\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":13,\"name\":\"羽毛球场4\",\"id\":222217,\"time\":\"15:00~16:00\",\"venuePlanId\":473,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"5\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":14,\"name\":\"羽毛球场5\",\"id\":222045,\"time\":\"09:00~10:00\",\"venuePlanId\":386,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"5\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":14,\"name\":\"羽毛球场5\",\"id\":222087,\"time\":\"10:00~11:00\",\"venuePlanId\":393,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"5\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":14,\"name\":\"羽毛球场5\",\"id\":222129,\"time\":\"11:00~12:00\",\"venuePlanId\":400,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"5\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":14,\"name\":\"羽毛球场5\",\"id\":222306,\"time\":\"17:00~18:00\",\"venuePlanId\":421,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"5\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":14,\"name\":\"羽毛球场5\",\"id\":222387,\"time\":\"19:00~20:00\",\"venuePlanId\":435,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"5\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"19.50元\",\"disable\":1,\"venueId\":14,\"name\":\"羽毛球场5\",\"id\":222426,\"time\":\"20:00~21:30\",\"venuePlanId\":442,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"6\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":15,\"name\":\"羽毛球场6\",\"id\":222046,\"time\":\"09:00~10:00\",\"venuePlanId\":387,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"6\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":15,\"name\":\"羽毛球场6\",\"id\":222088,\"time\":\"10:00~11:00\",\"venuePlanId\":394,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"6\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":15,\"name\":\"羽毛球场6\",\"id\":222130,\"time\":\"11:00~12:00\",\"venuePlanId\":401,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"6\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":15,\"name\":\"羽毛球场6\",\"id\":222168,\"time\":\"14:00~15:00\",\"venuePlanId\":408,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"6\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":15,\"name\":\"羽毛球场6\",\"id\":222219,\"time\":\"15:00~16:00\",\"venuePlanId\":475,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"6\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":15,\"name\":\"羽毛球场6\",\"id\":222388,\"time\":\"19:00~20:00\",\"venuePlanId\":436,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"6\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"19.50元\",\"disable\":1,\"venueId\":15,\"name\":\"羽毛球场6\",\"id\":222427,\"time\":\"20:00~21:30\",\"venuePlanId\":443,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"7\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":16,\"name\":\"羽毛球场7\",\"id\":222047,\"time\":\"09:00~10:00\",\"venuePlanId\":388,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"7\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":16,\"name\":\"羽毛球场7\",\"id\":222089,\"time\":\"10:00~11:00\",\"venuePlanId\":395,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"7\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":16,\"name\":\"羽毛球场7\",\"id\":222131,\"time\":\"11:00~12:00\",\"venuePlanId\":402,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"7\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":16,\"name\":\"羽毛球场7\",\"id\":222266,\"time\":\"16:00~17:00\",\"venuePlanId\":416,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"7\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":16,\"name\":\"羽毛球场7\",\"id\":222308,\"time\":\"17:00~18:00\",\"venuePlanId\":423,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"7\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":16,\"name\":\"羽毛球场7\",\"id\":222349,\"time\":\"18:00~19:00\",\"venuePlanId\":430,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"7\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":16,\"name\":\"羽毛球场7\",\"id\":222389,\"time\":\"19:00~20:00\",\"venuePlanId\":437,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"7\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"19.50元\",\"disable\":1,\"venueId\":16,\"name\":\"羽毛球场7\",\"id\":222429,\"time\":\"20:00~21:30\",\"venuePlanId\":444,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"8\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":18,\"name\":\"羽毛球场8\",\"id\":222048,\"time\":\"09:00~10:00\",\"venuePlanId\":389,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"8\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":18,\"name\":\"羽毛球场8\",\"id\":222090,\"time\":\"10:00~11:00\",\"venuePlanId\":396,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"8\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":18,\"name\":\"羽毛球场8\",\"id\":222132,\"time\":\"11:00~12:00\",\"venuePlanId\":403,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"8\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":18,\"name\":\"羽毛球场8\",\"id\":222267,\"time\":\"16:00~17:00\",\"venuePlanId\":417,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"8\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":18,\"name\":\"羽毛球场8\",\"id\":222309,\"time\":\"17:00~18:00\",\"venuePlanId\":424,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"8\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":18,\"name\":\"羽毛球场8\",\"id\":222350,\"time\":\"18:00~19:00\",\"venuePlanId\":431,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"8\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":18,\"name\":\"羽毛球场8\",\"id\":222390,\"time\":\"19:00~20:00\",\"venuePlanId\":438,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"8\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"19.50元\",\"disable\":1,\"venueId\":18,\"name\":\"羽毛球场8\",\"id\":222430,\"time\":\"20:00~21:30\",\"venuePlanId\":445,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"9\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"19.50元\",\"disable\":1,\"venueId\":19,\"name\":\"羽毛球场9\",\"id\":222058,\"time\":\"09:00~10:30\",\"venuePlanId\":446,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"9\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"19.50元\",\"disable\":1,\"venueId\":19,\"name\":\"羽毛球场9\",\"id\":222110,\"time\":\"10:30~12:00\",\"venuePlanId\":450,\"campusInfoId\":null},{\"useStatusName\":\"可预约\",\"sort\":\"9\",\"addUserStatus\":\"1\",\"useStatus\":0,\"checkStatus\":true,\"price\":\"26.00元\",\"disable\":0,\"venueId\":19,\"name\":\"羽毛球场9\",\"id\":222269,\"time\":\"16:00~18:00\",\"venuePlanId\":458,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"9\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"26.00元\",\"disable\":1,\"venueId\":19,\"name\":\"羽毛球场9\",\"id\":222352,\"time\":\"18:00~20:00\",\"venuePlanId\":462,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"9\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"19.50元\",\"disable\":1,\"venueId\":19,\"name\":\"羽毛球场9\",\"id\":222431,\"time\":\"20:00~21:30\",\"venuePlanId\":466,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"10\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"19.50元\",\"disable\":1,\"venueId\":20,\"name\":\"羽毛球场10\",\"id\":222060,\"time\":\"09:00~10:30\",\"venuePlanId\":447,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"10\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"19.50元\",\"disable\":1,\"venueId\":20,\"name\":\"羽毛球场10\",\"id\":222111,\"time\":\"10:30~12:00\",\"venuePlanId\":451,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"10\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"26.00元\",\"disable\":1,\"venueId\":20,\"name\":\"羽毛球场10\",\"id\":222270,\"time\":\"16:00~18:00\",\"venuePlanId\":459,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"10\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"26.00元\",\"disable\":1,\"venueId\":20,\"name\":\"羽毛球场10\",\"id\":222353,\"time\":\"18:00~20:00\",\"venuePlanId\":463,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"10\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"19.50元\",\"disable\":1,\"venueId\":20,\"name\":\"羽毛球场10\",\"id\":222432,\"time\":\"20:00~21:30\",\"venuePlanId\":467,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"11\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"19.50元\",\"disable\":1,\"venueId\":21,\"name\":\"羽毛球场11\",\"id\":222062,\"time\":\"09:00~10:30\",\"venuePlanId\":448,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"11\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"19.50元\",\"disable\":1,\"venueId\":21,\"name\":\"羽毛球场11\",\"id\":222112,\"time\":\"10:30~12:00\",\"venuePlanId\":452,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"11\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"26.00元\",\"disable\":1,\"venueId\":21,\"name\":\"羽毛球场11\",\"id\":222271,\"time\":\"16:00~18:00\",\"venuePlanId\":460,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"11\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"26.00元\",\"disable\":1,\"venueId\":21,\"name\":\"羽毛球场11\",\"id\":222354,\"time\":\"18:00~20:00\",\"venuePlanId\":464,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"11\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"19.50元\",\"disable\":1,\"venueId\":21,\"name\":\"羽毛球场11\",\"id\":222433,\"time\":\"20:00~21:30\",\"venuePlanId\":468,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"12\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"19.50元\",\"disable\":1,\"venueId\":22,\"name\":\"羽毛球场12\",\"id\":222064,\"time\":\"09:00~10:30\",\"venuePlanId\":449,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"12\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"19.50元\",\"disable\":1,\"venueId\":22,\"name\":\"羽毛球场12\",\"id\":222113,\"time\":\"10:30~12:00\",\"venuePlanId\":453,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"12\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"26.00元\",\"disable\":1,\"venueId\":22,\"name\":\"羽毛球场12\",\"id\":222272,\"time\":\"16:00~18:00\",\"venuePlanId\":461,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"12\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":22,\"name\":\"羽毛球场12\",\"id\":222333,\"time\":\"18:00~19:00\",\"venuePlanId\":599,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"12\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":22,\"name\":\"羽毛球场12\",\"id\":222392,\"time\":\"19:00~20:00\",\"venuePlanId\":465,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"12\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"19.50元\",\"disable\":1,\"venueId\":22,\"name\":\"羽毛球场12\",\"id\":222434,\"time\":\"20:00~21:30\",\"venuePlanId\":469,\"campusInfoId\":null}]}\n";
                    //TODO:环境切换块

                    //TODO:环境切换块——无可用场次
//                    responseData="{\"msg\":\"success\",\"code\":0,\"venuePlanData\":[{\"useStatusName\":\"已过时\",\"sort\":\"1\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":10,\"name\":\"羽毛球场1\",\"id\":222042,\"time\":\"09:00~10:00\",\"venuePlanId\":383,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"1\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":10,\"name\":\"羽毛球场1\",\"id\":222084,\"time\":\"10:00~11:00\",\"venuePlanId\":390,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"1\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":10,\"name\":\"羽毛球场1\",\"id\":222126,\"time\":\"11:00~12:00\",\"venuePlanId\":397,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"1\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":10,\"name\":\"羽毛球场1\",\"id\":222164,\"time\":\"14:00~15:00\",\"venuePlanId\":404,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"1\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":10,\"name\":\"羽毛球场1\",\"id\":222214,\"time\":\"15:00~16:00\",\"venuePlanId\":470,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"1\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":10,\"name\":\"羽毛球场1\",\"id\":222261,\"time\":\"16:00~17:00\",\"venuePlanId\":411,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"1\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":10,\"name\":\"羽毛球场1\",\"id\":222303,\"time\":\"17:00~18:00\",\"venuePlanId\":418,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"2\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":11,\"name\":\"羽毛球场2\",\"id\":222043,\"time\":\"09:00~10:00\",\"venuePlanId\":384,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"2\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":11,\"name\":\"羽毛球场2\",\"id\":222085,\"time\":\"10:00~11:00\",\"venuePlanId\":391,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"2\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":11,\"name\":\"羽毛球场2\",\"id\":222127,\"time\":\"11:00~12:00\",\"venuePlanId\":398,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"2\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":11,\"name\":\"羽毛球场2\",\"id\":222165,\"time\":\"14:00~15:00\",\"venuePlanId\":405,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"2\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":11,\"name\":\"羽毛球场2\",\"id\":222215,\"time\":\"15:00~16:00\",\"venuePlanId\":471,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"2\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":11,\"name\":\"羽毛球场2\",\"id\":222262,\"time\":\"16:00~17:00\",\"venuePlanId\":412,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"3\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":12,\"name\":\"羽毛球场3\",\"id\":222044,\"time\":\"09:00~10:00\",\"venuePlanId\":385,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"3\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":12,\"name\":\"羽毛球场3\",\"id\":222166,\"time\":\"14:00~15:00\",\"venuePlanId\":406,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"3\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":12,\"name\":\"羽毛球场3\",\"id\":222216,\"time\":\"15:00~16:00\",\"venuePlanId\":472,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"3\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":12,\"name\":\"羽毛球场3\",\"id\":222263,\"time\":\"16:00~17:00\",\"venuePlanId\":413,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"3\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":12,\"name\":\"羽毛球场3\",\"id\":222305,\"time\":\"17:00~18:00\",\"venuePlanId\":420,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"4\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":13,\"name\":\"羽毛球场4\",\"id\":222070,\"time\":\"09:00~10:00\",\"venuePlanId\":482,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"4\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":13,\"name\":\"羽毛球场4\",\"id\":222176,\"time\":\"14:00~15:00\",\"venuePlanId\":485,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"4\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":13,\"name\":\"羽毛球场4\",\"id\":222217,\"time\":\"15:00~16:00\",\"venuePlanId\":473,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"5\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":14,\"name\":\"羽毛球场5\",\"id\":222045,\"time\":\"09:00~10:00\",\"venuePlanId\":386,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"5\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":14,\"name\":\"羽毛球场5\",\"id\":222087,\"time\":\"10:00~11:00\",\"venuePlanId\":393,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"5\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":14,\"name\":\"羽毛球场5\",\"id\":222129,\"time\":\"11:00~12:00\",\"venuePlanId\":400,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"5\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":14,\"name\":\"羽毛球场5\",\"id\":222306,\"time\":\"17:00~18:00\",\"venuePlanId\":421,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"5\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":14,\"name\":\"羽毛球场5\",\"id\":222387,\"time\":\"19:00~20:00\",\"venuePlanId\":435,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"5\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"19.50元\",\"disable\":1,\"venueId\":14,\"name\":\"羽毛球场5\",\"id\":222426,\"time\":\"20:00~21:30\",\"venuePlanId\":442,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"6\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":15,\"name\":\"羽毛球场6\",\"id\":222046,\"time\":\"09:00~10:00\",\"venuePlanId\":387,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"6\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":15,\"name\":\"羽毛球场6\",\"id\":222088,\"time\":\"10:00~11:00\",\"venuePlanId\":394,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"6\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":15,\"name\":\"羽毛球场6\",\"id\":222130,\"time\":\"11:00~12:00\",\"venuePlanId\":401,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"6\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":15,\"name\":\"羽毛球场6\",\"id\":222168,\"time\":\"14:00~15:00\",\"venuePlanId\":408,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"6\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":15,\"name\":\"羽毛球场6\",\"id\":222219,\"time\":\"15:00~16:00\",\"venuePlanId\":475,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"6\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":15,\"name\":\"羽毛球场6\",\"id\":222388,\"time\":\"19:00~20:00\",\"venuePlanId\":436,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"6\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"19.50元\",\"disable\":1,\"venueId\":15,\"name\":\"羽毛球场6\",\"id\":222427,\"time\":\"20:00~21:30\",\"venuePlanId\":443,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"7\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":16,\"name\":\"羽毛球场7\",\"id\":222047,\"time\":\"09:00~10:00\",\"venuePlanId\":388,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"7\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":16,\"name\":\"羽毛球场7\",\"id\":222089,\"time\":\"10:00~11:00\",\"venuePlanId\":395,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"7\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":16,\"name\":\"羽毛球场7\",\"id\":222131,\"time\":\"11:00~12:00\",\"venuePlanId\":402,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"7\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":16,\"name\":\"羽毛球场7\",\"id\":222266,\"time\":\"16:00~17:00\",\"venuePlanId\":416,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"7\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":16,\"name\":\"羽毛球场7\",\"id\":222308,\"time\":\"17:00~18:00\",\"venuePlanId\":423,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"7\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":16,\"name\":\"羽毛球场7\",\"id\":222349,\"time\":\"18:00~19:00\",\"venuePlanId\":430,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"7\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":16,\"name\":\"羽毛球场7\",\"id\":222389,\"time\":\"19:00~20:00\",\"venuePlanId\":437,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"7\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"19.50元\",\"disable\":1,\"venueId\":16,\"name\":\"羽毛球场7\",\"id\":222429,\"time\":\"20:00~21:30\",\"venuePlanId\":444,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"8\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":18,\"name\":\"羽毛球场8\",\"id\":222048,\"time\":\"09:00~10:00\",\"venuePlanId\":389,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"8\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":18,\"name\":\"羽毛球场8\",\"id\":222090,\"time\":\"10:00~11:00\",\"venuePlanId\":396,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"8\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":18,\"name\":\"羽毛球场8\",\"id\":222132,\"time\":\"11:00~12:00\",\"venuePlanId\":403,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"8\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":18,\"name\":\"羽毛球场8\",\"id\":222267,\"time\":\"16:00~17:00\",\"venuePlanId\":417,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"8\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":18,\"name\":\"羽毛球场8\",\"id\":222309,\"time\":\"17:00~18:00\",\"venuePlanId\":424,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"8\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":18,\"name\":\"羽毛球场8\",\"id\":222350,\"time\":\"18:00~19:00\",\"venuePlanId\":431,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"8\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":18,\"name\":\"羽毛球场8\",\"id\":222390,\"time\":\"19:00~20:00\",\"venuePlanId\":438,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"8\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"19.50元\",\"disable\":1,\"venueId\":18,\"name\":\"羽毛球场8\",\"id\":222430,\"time\":\"20:00~21:30\",\"venuePlanId\":445,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"9\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"19.50元\",\"disable\":1,\"venueId\":19,\"name\":\"羽毛球场9\",\"id\":222058,\"time\":\"09:00~10:30\",\"venuePlanId\":446,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"9\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"19.50元\",\"disable\":1,\"venueId\":19,\"name\":\"羽毛球场9\",\"id\":222110,\"time\":\"10:30~12:00\",\"venuePlanId\":450,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"9\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"26.00元\",\"disable\":1,\"venueId\":19,\"name\":\"羽毛球场9\",\"id\":222269,\"time\":\"16:00~18:00\",\"venuePlanId\":458,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"9\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"26.00元\",\"disable\":1,\"venueId\":19,\"name\":\"羽毛球场9\",\"id\":222352,\"time\":\"18:00~20:00\",\"venuePlanId\":462,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"9\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"19.50元\",\"disable\":1,\"venueId\":19,\"name\":\"羽毛球场9\",\"id\":222431,\"time\":\"20:00~21:30\",\"venuePlanId\":466,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"10\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"19.50元\",\"disable\":1,\"venueId\":20,\"name\":\"羽毛球场10\",\"id\":222060,\"time\":\"09:00~10:30\",\"venuePlanId\":447,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"10\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"19.50元\",\"disable\":1,\"venueId\":20,\"name\":\"羽毛球场10\",\"id\":222111,\"time\":\"10:30~12:00\",\"venuePlanId\":451,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"10\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"26.00元\",\"disable\":1,\"venueId\":20,\"name\":\"羽毛球场10\",\"id\":222270,\"time\":\"16:00~18:00\",\"venuePlanId\":459,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"10\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"26.00元\",\"disable\":1,\"venueId\":20,\"name\":\"羽毛球场10\",\"id\":222353,\"time\":\"18:00~20:00\",\"venuePlanId\":463,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"10\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"19.50元\",\"disable\":1,\"venueId\":20,\"name\":\"羽毛球场10\",\"id\":222432,\"time\":\"20:00~21:30\",\"venuePlanId\":467,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"11\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"19.50元\",\"disable\":1,\"venueId\":21,\"name\":\"羽毛球场11\",\"id\":222062,\"time\":\"09:00~10:30\",\"venuePlanId\":448,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"11\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"19.50元\",\"disable\":1,\"venueId\":21,\"name\":\"羽毛球场11\",\"id\":222112,\"time\":\"10:30~12:00\",\"venuePlanId\":452,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"11\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"26.00元\",\"disable\":1,\"venueId\":21,\"name\":\"羽毛球场11\",\"id\":222271,\"time\":\"16:00~18:00\",\"venuePlanId\":460,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"11\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"26.00元\",\"disable\":1,\"venueId\":21,\"name\":\"羽毛球场11\",\"id\":222354,\"time\":\"18:00~20:00\",\"venuePlanId\":464,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"11\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"19.50元\",\"disable\":1,\"venueId\":21,\"name\":\"羽毛球场11\",\"id\":222433,\"time\":\"20:00~21:30\",\"venuePlanId\":468,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"12\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"19.50元\",\"disable\":1,\"venueId\":22,\"name\":\"羽毛球场12\",\"id\":222064,\"time\":\"09:00~10:30\",\"venuePlanId\":449,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"12\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"19.50元\",\"disable\":1,\"venueId\":22,\"name\":\"羽毛球场12\",\"id\":222113,\"time\":\"10:30~12:00\",\"venuePlanId\":453,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"12\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"26.00元\",\"disable\":1,\"venueId\":22,\"name\":\"羽毛球场12\",\"id\":222272,\"time\":\"16:00~18:00\",\"venuePlanId\":461,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"12\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":22,\"name\":\"羽毛球场12\",\"id\":222333,\"time\":\"18:00~19:00\",\"venuePlanId\":599,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"12\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"13.00元\",\"disable\":1,\"venueId\":22,\"name\":\"羽毛球场12\",\"id\":222392,\"time\":\"19:00~20:00\",\"venuePlanId\":465,\"campusInfoId\":null},{\"useStatusName\":\"已过时\",\"sort\":\"12\",\"addUserStatus\":\"1\",\"useStatus\":2,\"checkStatus\":false,\"price\":\"19.50元\",\"disable\":1,\"venueId\":22,\"name\":\"羽毛球场12\",\"id\":222434,\"time\":\"20:00~21:30\",\"venuePlanId\":469,\"campusInfoId\":null}]}\n";
                    //TODO:环境切换块
                    break; // 如果成功获取响应，跳出循环
                } catch (HttpServerErrorException.GatewayTimeout e) {
                    // 捕获 504 错误
                    retryCount++;
                    if (retryCount < Sentinel_MAX_504_GatewayTimeout_tryCount) {
                        // 添加延迟
                        try {
                            Thread.sleep(30000); // 等待30秒后重试
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                    } else {
                        // 达到最大重试次数，处理重试失败的情况
                        // ...
                        failure_503=true;
                        LOG.info(username+userNum+"失败：目标服务器宕机，已重试20次，达到最大重试次数，如需继续预约请使用直抢模式");
                        return Result.ok("失败：目标服务器宕机，已重试20次，达到最大重试次数，如需继续预约请使用直抢模式");
                    }
                }
            }
            // 发送 GET 请求并获取响应数据
            //打印响应数据

            LOG.info(username+userNum+"获取的responseData："+responseData);
            if(responseData.contains("token失效")){//可能会出现这种情况
                LOG.info(username+userNum+"的token失效");
                //获取手机号
                for (SpinKillCallable spinKillCallable : SuperSeckillServiceImpl.spinKillCallables) {
                    if(spinKillCallable.getUserNum().equals(userNum)){
                        //TODO 清除callable
                        SuperSeckillServiceImpl.spinKillCallables.remove(spinKillCallable);
                        //直接发送短信
                        LOG.info(username+userNum+"token失效，发送告知短信");
                        smsService.sendFailMessage(phoneNum,"token失效，请重新登陆重新提交任务，或者重新登录后使用直抢模式");
                        break;
                    }
                }

                return Result.fail("token失效,请重新登陆重新提交任务，或者使用直抢模式");
            }
            //解析json数据
            VenuePlanDataMsg venuePlanData = JSONUtil.toBean(responseData, VenuePlanDataMsg.class);
            planData = venuePlanData.getVenuePlanData();
            if(planData!=null){
                sentinelSavedPlanData=planData;
                LOG.info(username+userNum+"查询到了可用场次，已经保存");
            }else {
                LOG.info(username+userNum+"发生错误：planData为空");
                smsService.sendFailMessage(phoneNum,"发生错误，请重新登陆重新提交任务，或者使用直抢模式");
                return Result.fail("token失效,请重新登陆重新提交任务，或者使用直抢模式");
            }
        }else {
            //否则使用哨兵的结果,但是要自旋等待哨兵拿到结果
            while(sentinelSavedPlanData==null){
                try {
                    if(failure_503){
                        LOG.info(username+userNum+"失败：目标服务器宕机，已重试20次，达到最大重试次数，如需继续预约请使用直抢模式");
                        return Result.ok("失败：目标服务器宕机，已重试20次，达到最大重试次数，如需继续预约请使用直抢模式");
                    }
                    //每隔200ms检查一次
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            LOG.info(username+userNum+"普通线程使用哨兵保存的结果");
            planData = sentinelSavedPlanData;
        }
        /**
         * 优化：避免内部碰撞，使用一个碰撞数组collideMap：
         * HashMap<List<Integer>,List<String>> collideMap=new HashMap<>();
         * key：场次信息，只有两个值，一个是startTime,一个是endTime
         * value，list集合，存放目标场次为key的用户学号信息
         * 进入循环后，先将目标场次放入map中，如果为空则创建，index就是加入的次序
         * 当查询到一个可用场次时，首先看该场次下该用户的下标index，偏移index下，为0不偏移
         * 由于hashmap是线程不安全的，所以使用ConcurrentHashMap，支持线程安全的并发操作
         */

        ResponseEntity<String> responseEntity=null;
        ResultDTO resultDTO = null;
        long taskStartTime = System.currentTimeMillis();
        int statusCode=0;
        int count=0;
        Body body=null;
        boolean obeyCondition=true;

        for (int k = 0; k < 2; k++) {
            String key_begin;
            String key_end;
            if(obeyCondition){
                key_begin=startTime;
                key_end=endTime;
            }else {
                key_begin=accept_startTime;
                key_end=accept_endTime;
            }
            /**
             * 回避map，记录当前key为userNum的用户已经回避的次数
             * key:学号
             * value：已经回避的次数
             */

            if(!obeyConditionHaveChangedTag){
                synchronized (lock){
                    avoidCountMap=new ConcurrentHashMap<>();
                    collideMap=new ConcurrentHashMap<>();
                    obeyConditionHaveChangedTag=true;
                }
            }


            avoidCountMap.put(userNum,0);
            /**
             * key：场次信息，只有两个值，一个是startTime,一个是endTime
             * value，list集合，存放目标场次为key的用户学号信息
             */
            if(collideMap.get(key_begin+"_"+key_end)==null){
                List<String> list=new ArrayList<>();
                list.add(userNum);
                collideMap.put(key_begin+"_"+key_end,list);
            }else {
                collideMap.get(key_begin+"_"+key_end).add(userNum);
            }
            for (VenuePlan plan : planData) {//error:planData为空？ 可能原因没保存
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

                //TODO:减少碰撞
                if(avoidCountMap.get(userNum)<collideMap.get(key_begin+"_"+key_end).indexOf(userNum)){
                    //如果当前用户回避次数小于用户的index值，比如当前用户index值为1，那么需要回避1次
                    avoidCountMap.put(userNum,avoidCountMap.get(userNum)+1);
                    LOG.info("用户"+userNum+username+"进行一次退避");
                    continue;
                }
                //上述条件都满足的话，所有条件都满足了，该plan可用
                count++;
                //发起请求，获取结果
                //TODO:线上环境代码
                responseEntity=taskHandler(token,plan,username,userNum);
                statusCode = responseEntity.getStatusCodeValue();
                String responseBody = responseEntity.getBody();
                //TODO:线上环境代码

                //TODO:测试环境代码1——预约失败
//                responseEntity=new ResponseEntity<>(HttpStatus.ACCEPTED);
//                statusCode = 200;
//                String responseBody = "{\"msg\":\"场地已被抢走，请您下次早点预约！\",\"code\":500}";
                //TODO:测试环境代码1

                //TODO:测试环境代码2——预约成功
//                responseEntity=new ResponseEntity<>(HttpStatus.ACCEPTED);
//                statusCode = 200;
//                String responseBody = "{\"msg\":\"success\",\"code\":0,\"payurl\":\"https://cwcwx.nwu.edu.cn/zhifu/payAccept.aspx?prePayId=f6724cba0d143e*******d5cad7e856c08e3ea2956f1f\"}";
                //TODO:测试环境代码2

                //TODO:测试环境代码3——模拟测试数据提前刷新成可预约但实际预约不了
//                statusCode = 200;
//                String responseBody = "{\"msg\":\"场地已被抢走，请您下次早点预约！\",\"code\":500}";
                //TODO:测试环境代码3
                if(responseEntity==null){
                    return Result.ok("目标服务器宕机！！！已经达到最大重试次数"+MAX_504_GatewayTimeout_tryCount+"，此用户任务失败，已返回\"");
                }
                body = JSONUtil.toBean(responseBody, Body.class);
                if(statusCode==200&&body.getMsg().equals("success")){
                    //封装DTO
                    LOG.info(username+userNum+"请求结果:预定成功！！！场次："+plan.getTime()+" 场地："+plan.getName());
                    long taskEndTime = System.currentTimeMillis();
                    long elapsedTimeMillis = taskEndTime - taskStartTime;
                    payUrlMap.put(userNum,body.getPayurl());
                    resultDTO=new ResultDTO(true,body,plan.getName(),plan.getTime(),plan.getPrice(),body.getPayurl(),count, (int) (elapsedTimeMillis/1000));
                    return Result.ok(resultDTO);
                } else if (statusCode==200&&body.getCode()==500&&body.getMsg().equals("您有未支付的场地预约信息，请先支付！")) {
                    LOG.info(username+userNum+"请求结果:您有未支付的场地预约信息，请先支付！");
                    return Result.ok("失败：您有未支付的场地预约信息，请先支付！");
                }else if (statusCode==200&&body.getCode()==500&&body.getMsg().equals("您有未使用的场地预约信息，请消费完后再进行预约！")) {
                    LOG.info(username+userNum+"失败：您有未使用的场地预约信息，请消费完后再进行预约！");
                    return Result.ok("失败：您有未使用的场地预约信息，请消费完后再进行预约！");
                } else {//对不起，场地被抢走

                }
            }
            if(resultDTO!=null){
                break;
            }else {
                if(k<1){
                    LOG.info(userNum+username+"非绝对条件下无匹配场次，取消非绝对条件重新查询");
                    //obeyCondition修改为false，重新来一次循环
                    obeyCondition=false;
                }
            }
        }
        //没有可预约的场次
        LOG.info(username+userNum+"请求结果:没有选定时间范围内有效场次，可以尝试扩大时间范围再次尝试");
        return Result.fail("没有选定时间范围内有效场次，可以尝试扩大时间范围再次尝试");
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
//                LOG.info(username+userNum+"的请求响应body:"+body);
                LOG.info(username+userNum+"请求响应body:"+body);
                break; // 如果成功获取响应，跳出循环
            } catch (HttpServerErrorException.GatewayTimeout e) {
                // 捕获 504 错误
                LOG.warn(userNum+username+"：目标服务器宕机！！！发生504_GatewayTimeout错误！！！60s后重试");
                retryCount++;
                if (retryCount < MAX_504_GatewayTimeout_tryCount) {
                    // 添加延迟
                    try {
                        Thread.sleep(60000); // 等待60秒后重试
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

