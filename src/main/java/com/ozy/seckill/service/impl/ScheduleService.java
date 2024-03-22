package com.ozy.seckill.service.impl;

import com.ozy.seckill.entity.Result;
import com.ozy.seckill.entity.ResultDTO;
import com.ozy.seckill.entity.SpinKillCallableResult;
import com.ozy.seckill.entity.WebCount;
import com.ozy.seckill.service.SMSService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

@Component
public class ScheduleService {
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());
    private LocalTime startTime;
    @Autowired
    SMSService smsService;
    @Value("${HeartbeatTime}")
    private String HeartbeatTimeStr;
    //每小时保存下=一次数据，顺便输出当前程序任务池信息
    @Scheduled(cron = "0 40 * * * ?",zone = "Asia/Shanghai")
    public void saveData(){
        try {
            File file = new File("ApplicationDatas.data");
            BufferedWriter writer = new BufferedWriter(new FileWriter(file, false));
            writer.write(WebCount.visitCount + "\n");
            writer.write(WebCount.userCount +"\n");
            writer.write(WebCount.likesCount +"\n");
            writer.write(WebCount.useCount + "\n");
            writer.write(WebCount.taskCount +"\n");
            writer.write(WebCount.taskSuccessCount +"\n");
            writer.write(WebCount.taskFailCount +"\n");
            writer.close();
            LOG.info("每小时数据持久化任务完成，写入文件成功");
            LOG.info("当前任务总数："+SuperSeckillServiceImpl.spinKillCallables.size());
        } catch (IOException e) {
            LOG.error("写入文件时发生错误：" + e.getMessage());
        }
    }


    @Scheduled(cron = "0 0 0 * * ?",zone = "Asia/Shanghai")
//    @Scheduled(cron = "0 37 * * * ?",zone = "Asia/Shanghai")
    private void executeAllThreadAndSentinelWithOption() throws InterruptedException, ExecutionException {
        WebCount.ips.clear();//UV的跨度为1天，即每日UV
        //清空之前的结果池
        LOG.info("清空头一天的信息~~~");
        LOG.info("现在是00:00，所有线程加上哨兵并发执行executeAllThreadAndSentinelWithOption,当前线程池线程数量："+SuperSeckillServiceImpl.spinKillCallables.size());
        SuperSeckillServiceImpl.spinKillCallableResultsMap.clear();
        SuperSeckillServiceImpl.userResultMap.clear();
        SuperSeckillServiceImpl.Tag_0030 =false;
        SuperSeckillServiceImpl.failure_503=false;
        SuperSeckillServiceImpl.obeyConditionHaveChangedTag=false;
        SuperSeckillServiceImpl.avoidCountMap=new ConcurrentHashMap<>();
        SuperSeckillServiceImpl.collideMap=new ConcurrentHashMap<>();
        HashMap<String,SuperSeckillServiceImpl.SpinKillCallable> spinKillCallableMap=new HashMap<>();
        HashMap<String,SuperSeckillServiceImpl.SpinKillCallable> retryMap=new HashMap<>();
        for (SuperSeckillServiceImpl.SpinKillCallable spinKillCallable : SuperSeckillServiceImpl.spinKillCallables) {
            Future<SpinKillCallableResult> future = SuperSeckillServiceImpl.executor.submit(spinKillCallable);
            SuperSeckillServiceImpl.spinKillCallableFutures.add(future);
            spinKillCallableMap.put(spinKillCallable.getUserNum(),spinKillCallable);
        }

        //判断是否有成功的
        boolean haveSuccess=false;
        Map<String,SpinKillCallableResult> tmpResultsMap =new HashMap<>();

        for (Future<SpinKillCallableResult> spinKillCallableFuture : SuperSeckillServiceImpl.spinKillCallableFutures) {
            SpinKillCallableResult spinKillCallableResult=null;
            try{
                spinKillCallableResult=spinKillCallableFuture.get();
                if(!spinKillCallableResult.getResult().getSuccess()&&spinKillCallableResult.getResult().getErrorMsg().startsWith("token失效")){//token失效了，线程内部已经处理了，直接continue即可
                    SuperSeckillServiceImpl.spinKillCallableResultsMap.put(spinKillCallableResult.getUserNum(),spinKillCallableResult);
                    tmpResultsMap.put(spinKillCallableResult.getUserNum(),spinKillCallableResult);
                    continue;
                }
                tmpResultsMap.put(spinKillCallableResult.getUserNum(),spinKillCallableResult);
                if(spinKillCallableResult.getResult().getSuccess()){
                    haveSuccess=true;
                }else {
                    retryMap.put(spinKillCallableResult.getUserNum(),spinKillCallableMap.get(spinKillCallableResult.getUserNum()));
                }
            } catch (Exception e) {
                LOG.error("线程"+spinKillCallableResult.getUserNum()+spinKillCallableResult+"捕获到异常！！！");
                e.printStackTrace();
            }
        }

        if(haveSuccess){
            //有成功的，说明此时放场，重试失败的
            if (retryMap.size()>0){
                SuperSeckillServiceImpl.Tag_0030 =true;
                SuperSeckillServiceImpl.spinKillCallableFutures.clear();
                for (Map.Entry<String,SuperSeckillServiceImpl.SpinKillCallable> entry:retryMap.entrySet()){
                    LOG.info("现在有成功的线程，但是线程"+entry.getValue().getUserNum()+entry.getValue().getUsername()+"失败了，可能是因为放场不准时，现在进行重试");
                    Future<SpinKillCallableResult> future = SuperSeckillServiceImpl.executor.submit(entry.getValue());
                    SuperSeckillServiceImpl.spinKillCallableFutures.add(future);
                }
                for (Future<SpinKillCallableResult> spinKillCallableFuture : SuperSeckillServiceImpl.spinKillCallableFutures) {
                    try{
                        SpinKillCallableResult spinKillCallableResult=spinKillCallableFuture.get();
                        if(!spinKillCallableResult.getResult().getSuccess()&&spinKillCallableResult.getResult().getErrorMsg().startsWith("token失效")){
                            SuperSeckillServiceImpl.spinKillCallableResultsMap.put(spinKillCallableResult.getUserNum(),spinKillCallableResult);
                            tmpResultsMap.put(spinKillCallableResult.getUserNum(),spinKillCallableResult);
                            WebCount.taskCount--;
                            continue;
                        }
                        //直接覆盖
                        tmpResultsMap.put(spinKillCallableResult.getUserNum(),spinKillCallableResult);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
                SuperSeckillServiceImpl.Tag_0030 =false;
            }

            //保存结果
            SuperSeckillServiceImpl.spinKillCallableResultsMap=tmpResultsMap;
            //发送短信
            for (Map.Entry<String,SpinKillCallableResult> entry:SuperSeckillServiceImpl.spinKillCallableResultsMap.entrySet()){
                if(entry.getValue().getResult().getData()!=null){
                    WebCount.taskCount++;
                    WebCount.taskSuccessCount++;
                    smsService.sendSuccessMessage(entry.getValue().getPhoneNum(),null);
                }else {
                    WebCount.taskCount++;
                    WebCount.taskFailCount++;
                    smsService.sendFailMessage(entry.getValue().getPhoneNum(),entry.getValue().getResult().getErrorMsg());
                }
                LOG.info(entry.getKey()+":已发送通知短信");
            }
            //清空线程集
            SuperSeckillServiceImpl.spinKillCallables.clear();
            //清空Futures集
            SuperSeckillServiceImpl.spinKillCallableFutures.clear();
            LOG.info("有线程预约场次成功，说明今天是00:00放场，已保存结果，哨兵任务完成准备下线，当前线程池所有任务已执行");
            LOG.info("哨兵已下线，线程池所有任务全部执行完毕，结果map剩余未取数："+SuperSeckillServiceImpl.spinKillCallableResultsMap.size());
        }else {
            //没有成功的，说明非00:00放场，不保存信息
            LOG.info("没有成功的，说明非00:00放场或者没有任务提交，不保存信息");
            SuperSeckillServiceImpl.spinKillCallableFutures.clear();
            SuperSeckillServiceImpl.avoidCountMap=new ConcurrentHashMap<>();
            SuperSeckillServiceImpl.collideMap=new ConcurrentHashMap<>();
        }
    }


    /**
     * 哨兵从00:05开始周期性执行任务
     */
    @Scheduled(cron = "0 5 0 * * ?",zone = "Asia/Shanghai")
//    @Scheduled(cron = "0 38 * * * ?",zone = "Asia/Shanghai")
    private void HeartbeatTask() throws ExecutionException, InterruptedException {
        LOG.info("哨兵任务执行ing~~~");
        startTime = LocalTime.now();
        Integer HeartbeatTime=Integer.valueOf(HeartbeatTimeStr);
        LOG.info("心跳时间(s):"+HeartbeatTime);
        // 哨兵结束时间为当天的00:25
        LocalTime targetTime = LocalTime.of(0, 25);
        Duration duration = Duration.between(startTime, targetTime);
        int maxTryTime = (int) duration.toMinutes(); // 转换为分钟数
        System.out.println("maxTryTime:"+maxTryTime);
        Result sentinelResult =null;
        SpinKillCallableResult sentinelSpinKillCallableResult=null;
        LocalDateTime currentTime=null;
        //选出哨兵
        SuperSeckillServiceImpl.Tag_0030 =false;

        int sentinelIndex=0;
        if(SuperSeckillServiceImpl.spinKillCallables.size()>0){
            SuperSeckillServiceImpl.sentinel=SuperSeckillServiceImpl.spinKillCallables.get(sentinelIndex);
            LOG.info(SuperSeckillServiceImpl.sentinel.getUsername()+SuperSeckillServiceImpl.sentinel.getUserNum()+"作为哨兵线程执行任务");
        }
        while (true){
            //TODO:开始哨兵计划逻辑
            if(SuperSeckillServiceImpl.sentinel!=null){
                FutureTask<SpinKillCallableResult> sentinelFuture=new FutureTask<>(SuperSeckillServiceImpl.sentinel);
                SuperSeckillServiceImpl.avoidCountMap.clear();
                SuperSeckillServiceImpl.collideMap.clear();
                Thread sentinelThreat=new Thread(sentinelFuture);
                sentinelThreat.start();
                //阻塞主线程，获取结果
                try {
                    sentinelSpinKillCallableResult = sentinelFuture.get();
                    if(!sentinelSpinKillCallableResult.getResult().getSuccess()&&sentinelSpinKillCallableResult.getResult().getErrorMsg().startsWith("token失效")){
                        //token过期,更换哨兵
                        WebCount.taskCount--;
                        LOG.info(SuperSeckillServiceImpl.sentinel.getUsername()+SuperSeckillServiceImpl.sentinel.getUserNum()+"哨兵的token失效，更换新的哨兵");
                        sentinelIndex++;
                        if(sentinelIndex==SuperSeckillServiceImpl.spinKillCallables.size()){
                            //没有哨兵可用了
                            LOG.info("没有哨兵可用了");
                            SuperSeckillServiceImpl.sentinel=null;
                        }else{
                            SuperSeckillServiceImpl.sentinel=SuperSeckillServiceImpl.spinKillCallables.get(sentinelIndex);
                            LOG.info(SuperSeckillServiceImpl.sentinel.getUsername()+SuperSeckillServiceImpl.sentinel.getUserNum()+"成为新的哨兵");
                        }
                        continue;
                    }
                    sentinelResult = sentinelSpinKillCallableResult.getResult();
                    if(sentinelResult!=null&&sentinelResult.getSuccess()){//成功了
                        LOG.info("现在是非00:00和00:30时间,发现哨兵线程成功预约，现在执行所有线程池线程");
                        //保存哨兵线程结果
                        SuperSeckillServiceImpl.spinKillCallableResultsMap.put(sentinelSpinKillCallableResult.getUserNum(), sentinelSpinKillCallableResult);
                        Object data = sentinelSpinKillCallableResult.getResult().getData();
                        if(data!=null){
                            WebCount.taskCount++;
                            WebCount.taskSuccessCount++;
                            smsService.sendSuccessMessage(sentinelSpinKillCallableResult.getPhoneNum(),null);
                        }else {
                            WebCount.taskCount++;
                            WebCount.taskFailCount++;
                            smsService.sendFailMessage(sentinelSpinKillCallableResult.getPhoneNum(),sentinelResult.getErrorMsg());
                        }
                        LOG.info(sentinelSpinKillCallableResult.getUserNum()+":发送通知短信");
                        //移除哨兵
                        SuperSeckillServiceImpl.spinKillCallables.remove(0);
                        SuperSeckillServiceImpl.sentinel=null;
                        //执行其他所有线程
                        SuperSeckillServiceImpl.avoidCountMap.clear();
                        SuperSeckillServiceImpl.collideMap.clear();
                        executeAllThread();
                    }else {
                        //否则自旋
                        LOG.info("哨兵失败，继续自旋");
                    }
                } catch (Exception e) {
                    LOG.error("哨兵线程捕获到异常！！！");
                    e.printStackTrace();
                }

            }else {//没有哨兵，线程池一定是空
                //不要直接结束等待，万一期间新增了任务
                LOG.info("没有哨兵，线程池为空，等待任务中");
            }
            try {
                //计算离下一个心跳时间的时间
                currentTime=LocalDateTime.now();
                java.time.LocalDateTime nextIntegerTime = currentTime.plusSeconds( Integer.parseInt(HeartbeatTimeStr)- (currentTime.getSecond() % Integer.parseInt(HeartbeatTimeStr))).withNano(0);
                long milliseconds = java.time.Duration.between(currentTime, nextIntegerTime).toMillis();
                LOG.info("等待时间:"+(int)milliseconds/1000+"s,自旋线程数："+SuperSeckillServiceImpl.spinKillCallables.size());
                LOG.info("-------------------------------------------------------------------------");
                Thread.sleep(milliseconds);
            } catch (InterruptedException e) {
                LOG.warn("线程睡眠被异常打断，原因详见日志");
                throw new RuntimeException(e);
            }
            currentTime = LocalDateTime.now();
            long elapsedTimeMinutes = ChronoUnit.MINUTES.between(startTime, currentTime);
            if (elapsedTimeMinutes >= maxTryTime) {
                // 如果执行时间超过了maxTryTime分钟，停止自旋
                LOG.warn("今日哨兵任务执行完毕");
                return;
            }
        }
    }

    @Scheduled(cron = "0 30 0 * * ?",zone = "Asia/Shanghai")
//    @Scheduled(cron = "0 58 * * * ?",zone = "Asia/Shanghai")
    private void executeAllThread() {
        LOG.info("现在是00:30，所有线程加上哨兵并发执行executeAllThread,线程池线程数量："+SuperSeckillServiceImpl.spinKillCallables.size());
        //concurrentTag用于通知哨兵也不要去请求可用场次信息了，直接使用00:30之前查到的数据，这样也能提高哨兵线程的性能
        SuperSeckillServiceImpl.Tag_0030 =true;
//        SuperSeckillServiceImpl.Tag_0030 =false;
        HashMap<String,SuperSeckillServiceImpl.SpinKillCallable> spinKillCallableMap=new HashMap<>();

        //将所有的线程加入线程池执行
        for (SuperSeckillServiceImpl.SpinKillCallable spinKillCallable : SuperSeckillServiceImpl.spinKillCallables) {
            LOG.info("开始执行线程："+spinKillCallable.getUserNum()+spinKillCallable.getUsername());
            Future<SpinKillCallableResult> future = SuperSeckillServiceImpl.executor.submit(spinKillCallable);
            SuperSeckillServiceImpl.spinKillCallableFutures.add(future);
            spinKillCallableMap.put(spinKillCallable.getUserNum(),spinKillCallable);
        }
        for (Future<SpinKillCallableResult> spinKillCallableFuture : SuperSeckillServiceImpl.spinKillCallableFutures) {
            SpinKillCallableResult spinKillCallableResult= null;
            try {
                spinKillCallableResult = spinKillCallableFuture.get();
                if(!spinKillCallableResult.getResult().getSuccess()&&spinKillCallableResult.getResult().getErrorMsg().startsWith("token失效")){
                    SuperSeckillServiceImpl.spinKillCallableResultsMap.put(spinKillCallableResult.getUserNum(),spinKillCallableResult);
                    continue;
                }
            } catch (Exception e) {
                LOG.error("线程"+spinKillCallableResult.getUserNum()+"捕获到异常！！！");
                e.printStackTrace();
            }

            //发送短信
            if(spinKillCallableResult.getResult().getData()!=null){
                ResultDTO resultDTO=(ResultDTO) spinKillCallableResult.getResult().getData();
                WebCount.taskCount++;
                WebCount.taskSuccessCount++;
                smsService.sendSuccessMessage(spinKillCallableResult.getPhoneNum(),resultDTO.getTime());
            }else {
                //重试下
                SuperSeckillServiceImpl.SpinKillCallable retrySpinKillCallable=spinKillCallableMap.get(spinKillCallableResult.getUserNum());
                LOG.info("线程"+retrySpinKillCallable.getUserNum()+retrySpinKillCallable.getUsername()+"失败，进行重试");
                LOG.info("线程"+retrySpinKillCallable.getUserNum()+retrySpinKillCallable.getUsername()+"开始重试~~~");
                Future<SpinKillCallableResult> retryFuture = SuperSeckillServiceImpl.executor.submit(retrySpinKillCallable);
                SpinKillCallableResult retrySpinKillCallableResult=null;
                try {
                    retrySpinKillCallableResult = retryFuture.get();
                    if(!spinKillCallableResult.getResult().getSuccess()&&spinKillCallableResult.getResult().getErrorMsg().startsWith("token失效")){
                        SuperSeckillServiceImpl.spinKillCallableResultsMap.put(spinKillCallableResult.getUserNum(),spinKillCallableResult);
                        WebCount.userCount--;
                        continue;
                    }
                } catch (Exception e) {
                    LOG.error("线程"+spinKillCallableResult.getUserNum()+spinKillCallableResult+"捕获到异常！！！");
                    e.printStackTrace();
                }
                if(retrySpinKillCallableResult.getResult().getData()!=null){
                    LOG.info("线程"+retrySpinKillCallable.getUserNum()+retrySpinKillCallable.getUsername()+"重试结果：成功");
                    ResultDTO resultDTO=(ResultDTO) retrySpinKillCallableResult.getResult().getData();
                    WebCount.taskCount++;
                    WebCount.taskSuccessCount++;
                    smsService.sendSuccessMessage(retrySpinKillCallableResult.getPhoneNum(),resultDTO.getTime());
                }else {
                    LOG.info("线程"+retrySpinKillCallable.getUserNum()+retrySpinKillCallable.getUsername()+"重试结果：失败~~~");
                    WebCount.taskCount++;
                    WebCount.taskFailCount++;
                    smsService.sendFailMessage(retrySpinKillCallableResult.getPhoneNum(),retrySpinKillCallableResult.getResult().getErrorMsg());
                }
                spinKillCallableResult=retrySpinKillCallableResult;
            }
            LOG.info(spinKillCallableResult.getUserNum()+":已发送通知短信");
            //然后把结果放到结果集中去
            SuperSeckillServiceImpl.spinKillCallableResultsMap.put(spinKillCallableResult.getUserNum(),spinKillCallableResult);
        }

        //清空线程集
        SuperSeckillServiceImpl.spinKillCallables.clear();
        //清空Futures集
        SuperSeckillServiceImpl.spinKillCallableFutures.clear();
        //别忘了，删除哨兵
        SuperSeckillServiceImpl.sentinel=null;
        LOG.info("今日所有任务完成，线程池所有任务全部执行完毕，结果map剩余未取数："+SuperSeckillServiceImpl.spinKillCallableResultsMap.size());
        //当线程池所有线程唤醒并全部拿到结果后，别忘了删除哨兵保存的结果
        SuperSeckillServiceImpl.sentinelSavedPlanData=null;
    }
}
