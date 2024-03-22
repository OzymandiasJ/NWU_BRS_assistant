package com.ozy.seckill.controller;

import cn.hutool.json.JSONUtil;
import com.ozy.seckill.entity.*;
import com.ozy.seckill.service.NormalSeckillService;
import com.ozy.seckill.service.SuperSeckillService;
import com.ozy.seckill.service.UserService;
import com.ozy.seckill.service.impl.SuperSeckillServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;

@Controller
@Slf4j
@RequestMapping("/seckill")
public class MainController {
    @Value("${adminUser}")
    private ArrayList<String> adminUsers;
    @Autowired
    SuperSeckillService superSeckillService;
    @Autowired
    NormalSeckillService normalSeckillService;
    @Autowired
    UserService userService;
    private  final Logger LOG = LoggerFactory.getLogger(this.getClass());


    @RequestMapping("/getLikeCount")
    @ResponseBody
    public Result getLikeCount(){
        return Result.ok(WebCount.likesCount);
    }

    //统计信息接口
    @RequestMapping("/getStatistic")
    @ResponseBody
    public Result getStatistic(){
        HashMap<String, Object> statusMap = new HashMap<>();
        statusMap.put("visitCount", WebCount.visitCount);
        statusMap.put("userCount", WebCount.userCount);
        statusMap.put("likesCount", WebCount.likesCount);
        statusMap.put("useCount", WebCount.useCount);
        statusMap.put("taskCount", WebCount.taskCount);
        statusMap.put("taskSuccessCount", WebCount.taskSuccessCount);
        statusMap.put("taskFailCount", WebCount.taskFailCount);
//        String jsonStr = JSONUtil.toJsonStr(statusMap);
        return Result.ok(statusMap);
    }



    @RequestMapping("/getUser")
    @ResponseBody
    public Result getUser(@RequestParam("token") String userNum){
        return userService.getUser(userNum);
    }
    @RequestMapping("/getResult")
    @ResponseBody
    public Result getResult(@RequestParam("userNum") String userNum){
        return userService.getResult(userNum);
    }
    @RequestMapping("/gotResult")
    @ResponseBody
    public Result gotResult(@RequestParam("userNum") String userNum,@RequestParam("username") String username){
        return userService.gotResult(userNum,username);
    }
    @RequestMapping("getPayUrl")
    public String getPayUrl(@RequestParam("userNum") String userNum) {
        return userService.getPayUrl(userNum);
    }


    /**
     * 撤销用户的任务
     * @param userNum
     * @return
     */
    @RequestMapping("/cancelTask")
    @ResponseBody
    public Result cancelTask(@RequestParam("userNum") String userNum){
        return userService.cancelTask(userNum);
    }
    /**
     * @param token
     * @param startTime 场次时间范围
     * @param endTime
     * @param notTwoHour 只要一小时，不要1.5小时的场次
     * @return
     */
    @RequestMapping("/submitTask")
    @ResponseBody
    public Result submitTask(
            @RequestParam("token") String token,
            @RequestParam("startTime") String startTime,
            @RequestParam("endTime") String endTime,
            @RequestParam("accept_startTime") String accept_startTime,
            @RequestParam("accept_endTime") String accept_endTime,
            @RequestParam("notTwoHour") String notTwoHour,
            @RequestParam("firstTwoHour") String firstTwoHour,
            @RequestParam("username") String username,
            @RequestParam("userNum") String userNum,
            @RequestParam("phoneNum") String phoneNum,
            @RequestParam("spinMod") String spinMod
    ){
        //应对异常情况，尤其是学号姓名token缺失
        if((username==null||username.equals(""))||
                (userNum==null||userNum.equals(""))||
                (token==null||token.equals(""))||
                (spinMod==null||spinMod.equals(""))){
            //出现异常
            return Result.fail("出现异常:可能是姓名学号或者token缺失，请刷新页面重试，确保右上角已经登录，然后重新提交请求");
        }
//        System.out.println(token);
        //鉴权
        if(spinMod.equals("true")){
            LOG.info(username+userNum+"发来请求，自旋模式,期望场次开始时间："+startTime+" 结束时间："+endTime+" 可接受最早开始时间："+accept_startTime+" 可接受最晚结束时间："+accept_endTime+" 优先2小时："+firstTwoHour+" 是否不接受两小时："+notTwoHour);
            WebCount.useCount++;
            return superSeckillService.spinKill(token,startTime,endTime,accept_startTime,accept_endTime,notTwoHour,firstTwoHour,username,userNum,phoneNum);
        }else {
            LOG.info(username+userNum+"请求，直抢模式,期望场次开始时间："+startTime+" 结束时间："+endTime+" 可接受最早开始时间："+accept_startTime+" 可接受最晚结束时间："+accept_endTime+" 优先2小时："+firstTwoHour+" 是否不接受两小时："+notTwoHour);
            //然后，一个用户不能发多个预约请求
            if(SuperSeckillServiceImpl.isContainsUserNum(userNum)){
                LOG.info(username+userNum+"尝试多次多个任务，已拒绝");
                return Result.fail("失败:一个用户一天只允许预约一次!");
            }
            WebCount.useCount++;
            return normalSeckillService.singleThreadSeckill(token,startTime,endTime,accept_startTime,accept_endTime,notTwoHour,firstTwoHour,username,userNum);
        }
    }




}
