package com.ozy.seckill.service.impl;

import cn.hutool.json.JSONUtil;
import com.ozy.seckill.entity.Result;
import com.ozy.seckill.entity.WebCount;
import com.ozy.seckill.service.Monitor;
import com.ozy.seckill.service.NormalSeckillService;
import com.ozy.seckill.service.SuperSeckillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;

@Service
public class MonitorImpl implements Monitor {

    @Override
    public Result getWebCountInfo(String adminUserNum) {
        ArrayList<String> list = new ArrayList<>();
        list.add("PV:"+WebCount.visitCount);
        list.add("UV:"+WebCount.userCount);
        list.add("点赞数："+WebCount.likesCount);
        return Result.ok(true,list);
    }

    @Override
    public Result getSpinTaskUserNames(String adminUserNum) {
        HashMap<String,String> users=new HashMap<>();
        for (SuperSeckillServiceImpl.SpinKillCallable spinKillCallable : SuperSeckillServiceImpl.spinKillCallables) {
            users.put(spinKillCallable.getUsername(),spinKillCallable.getUserNum());
        }
        return Result.ok(users);
    }

    @Override
    public Result getSpinTaskResults(String adminUserNum) {
        return Result.ok(SuperSeckillServiceImpl.spinKillCallableResultsMap);
    }

}
