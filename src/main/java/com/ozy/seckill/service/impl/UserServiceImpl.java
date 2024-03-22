package com.ozy.seckill.service.impl;

import cn.hutool.json.JSONUtil;
import com.ozy.seckill.entity.*;
import com.ozy.seckill.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;

@Service
public class UserServiceImpl implements UserService {
    @Value("${adminUser}")
    private ArrayList<String> adminUsers;


    // 创建 RestTemplate 实例
    @Autowired
    RestTemplate restTemplate;
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());
    @Override
    public Result getUser(String token) {
        //拿着token去请求，得到数据，封装，返回前端
        // 创建请求头部信息
        HttpHeaders headers = new HttpHeaders();
        headers.set("Token",token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        // 创建请求实体
        HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);
        String url="getUser的api地址";
        //TODO: 代码仅供学习交流，已隐去该地址


        // 发送 GET 请求并获取响应数据
        //TODO:生产环境代码
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
        String responseData = response.getBody();
        //TODO:生产环境代码

        //TODO:测试环境代码
//        String responseData = "{\"msg\":\"success\",\"code\":0,\"user\":{\"userNum\":\"xxxxxx\",\"sex\":\"男\",\"mobile\":\"18580164887\",\"id\":31619,\"userType\":\"本（专）科生\",\"type\":\"0\",\"email\":null,\"username\":\"肖杰\"}}";
//        String responseData = "{\"msg\":\"1234567890\",\"code\":0,\"user\":{\"userNum\":\"xxxxxx\",\"sex\":\"男\",\"mobile\":\"18580164887\",\"id\":31619,\"userType\":\"本（专）科生\",\"type\":\"0\",\"email\":null,\"username\":\"肖杰\"}}";
        //TODO:测试环境代码

        //获取学号
        int leftIndex=responseData.indexOf("userNum")+10;
        int rightIndex=responseData.indexOf("\"",leftIndex);
        String userNum=responseData.substring(leftIndex,rightIndex);
        //获取姓名
        leftIndex=responseData.indexOf("username")+11;
        rightIndex=responseData.indexOf("\"",leftIndex);
        String username=responseData.substring(leftIndex,rightIndex);
        //TODO:这里需要拼接isVip字段，使用正则表达式获取学号
        String insertJson="";
        if(userNum.contains("请重新登录")){
            return Result.fail("token过期，请退出重新登陆");
        }
        if(adminUsers.contains(userNum)){
            LOG.info("管理员登录："+userNum+username);
//            LOG.info("responseData:  "+responseData);
        }else {
            LOG.info("普通用户登录："+userNum+username);
//            LOG.info("responseData:  "+responseData);
        }
        //插入数据
        //这里出问题了，不应该是+11，要看学号长度,已解决，使用userNum.length()来代入学号长度
        String left=responseData.substring(0,(responseData.indexOf(userNum)+(userNum.length()+1)));
        String right=responseData.substring((responseData.indexOf(userNum)+(userNum.length()+1)),responseData.length());
        //还要查看用户当前预约任务信息
        if(SuperSeckillServiceImpl.isContainsUserNum(userNum)){
            //有等待任务,去看看有没有结果
            if(SuperSeckillServiceImpl.spinKillCallableResultsMap.containsKey(userNum)){
                //有结果了，拿出来
                SpinKillCallableResult spinKillCallableResult = SuperSeckillServiceImpl.spinKillCallableResultsMap.get(userNum);
                Result result = spinKillCallableResult.getResult();
                if(result.getSuccess()==false){
                    //失败了
                    insertJson+=",\"taskStatusCode\":"+2;////0没有，1正在等待，2失败了，3成功
                }else {
                    if(result.getData()==null){
                        //有未使用或者未支付的场地
                        insertJson+=",\"taskStatusCode\":"+2;
                    }else {
                        //成功了
                        insertJson+=",\"taskStatusCode\":"+3;
                    }
                }
                String resultJson = JSONUtil.toJsonStr(result);
                insertJson+=",\"taskResult\":"+resultJson;
            }else {
                //还没结果
                insertJson+=",\"taskStatusCode\":"+1;
                insertJson+=",\"taskResult\":null";
            }
        }else {
            insertJson+=",\"taskStatusCode\":"+0;
            insertJson+=",\"taskResult\":null";
        }
        responseData=left+insertJson+right;

//        LOG.info(left);
//        LOG.info(insertJson);
//        LOG.info(right);
//        LOG.info(responseData);
        return Result.ok(true,responseData);
    }

    /**
     * 周期性请求获取结果
     * @param userNum
     */
    @Override
    public Result getResult(String userNum) {
        LOG.info(userNum+"发来周期性请求获取结果");
        //从spinKillCallableResultsMap中查询，没有的话返回null，有的话返回去
        if(SuperSeckillServiceImpl.isContainsUserNum(userNum)){
            //有等待任务,去看看有没有结果
            if(SuperSeckillServiceImpl.spinKillCallableResultsMap.containsKey(userNum)){
                //有结果了，拿出来
                SpinKillCallableResult spinKillCallableResult = SuperSeckillServiceImpl.spinKillCallableResultsMap.get(userNum);
                Result result = spinKillCallableResult.getResult();
                return Result.ok(result);
            }else {
                //还没结果
                return Result.fail("任务还未完成");
            }
        }else {
            //能发来这个请求应该是一定有任务吧
            return Result.fail("该用户没有预约任务");
        }

    }
    /**
     * 用户已经确认拿到结果，从结果集移除
     * @param userNum
     */
    @Override
    public Result gotResult(String userNum,String username) {
        //用户获取了结果，就可以从结果集移除了
        SuperSeckillServiceImpl.spinKillCallableResultsMap.remove(userNum);
        LOG.info(userNum+username+"已获取结果，从用户结果map中删除，结果Map剩余Map数："+SuperSeckillServiceImpl.spinKillCallableResultsMap.size());
        return Result.ok();
    }

    @Override
    public Result cancelTask(String userNum) {
        boolean hasCanceled=false;
        //首先看这个用户是不是哨兵线程用户
        if(SuperSeckillServiceImpl.sentinel!=null&&SuperSeckillServiceImpl.sentinel.getUserNum().equals(userNum)){
            //是的话删除哨兵。并将callables中第一个任务作为哨兵，从原来的线程集中删除
            String username=SuperSeckillServiceImpl.sentinel.getUsername();
            SuperSeckillServiceImpl.sentinel=null;
            hasCanceled=true;
            if(SuperSeckillServiceImpl.spinKillCallables.size()>0){
                SuperSeckillServiceImpl.sentinel=SuperSeckillServiceImpl.spinKillCallables.get(0);
                SuperSeckillServiceImpl.spinKillCallables.remove(0);//别忘了从Callables中删除这个线程
                LOG.info("哨兵线程"+username+userNum+"撤销了预约任务，现在由线程池任务"+SuperSeckillServiceImpl.sentinel.getUsername()+SuperSeckillServiceImpl.sentinel.getUserNum()+"作为哨兵");
            }else {
                LOG.info("哨兵线程"+username+userNum+"撤销了预约任务，现在哨兵和线程集都为空");
            }
        }else {
            for (SuperSeckillServiceImpl.SpinKillCallable spinKillCallable : SuperSeckillServiceImpl.spinKillCallables) {
                if(spinKillCallable.getUserNum().equals(userNum)){
                    //找到了，删除
                    SuperSeckillServiceImpl.spinKillCallables.remove(spinKillCallable);
                    LOG.info("线程池线程"+spinKillCallable.getUsername()+spinKillCallable.getUserNum()+"撤销了预约任务");
                    hasCanceled=true;
                    break;
                }
            }
        }
        if(hasCanceled){
            WebCount.taskCount--;
            return Result.ok("撤销任务成功");
        }else {
            return Result.fail("撤销任务失败，请刷新查看是否任务已经有了结果");
        }
    }

    @Override
    public String getPayUrl(String userNum) {
//        if(!SuperSeckillServiceImpl.payUrlMap.containsKey(userNum)){
//            return "";
//        }
        return "redirect:"+SuperSeckillServiceImpl.payUrlMap.get(userNum);
    }

}
