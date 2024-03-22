package com.ozy.seckill.controller;

import com.ozy.seckill.entity.Body;
import com.ozy.seckill.entity.Result;
import com.ozy.seckill.entity.ResultDTO;
import com.ozy.seckill.entity.WebCount;
import com.ozy.seckill.service.Monitor;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;

@Controller
@Slf4j
@RequestMapping("/admin")
public class AdminController {
    @Value("${adminUser}")
    private ArrayList<String> adminUser;
    @Autowired
    Monitor monitor;

    @Autowired
    UserService userService;
    private  final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @RequestMapping("/getSpinTaskUsers")
    @ResponseBody
    public Result getSpinTaskUsers(@RequestParam("adminUserNum") String adminUserNum) {
        if(adminUser.contains(adminUserNum)){
            return monitor.getSpinTaskUserNames(adminUserNum);
        }else
            return Result.fail("失败，您无权操作!!!");
    }
    @RequestMapping("/getSpinTaskResults")
    @ResponseBody
    public Result getSpinTaskResults(@RequestParam("adminUserNum") String adminUserNum) {
        if(adminUser.contains(adminUserNum)){
            return monitor.getSpinTaskResults(adminUserNum);
        }else
            return Result.fail("失败，您无权操作!!!");
    }
    @RequestMapping("/getWebCountInfo")
    @ResponseBody
    public Result getWebCountInfo(@RequestParam("adminUserNum") String adminUserNum) {
        if(adminUser.contains(adminUserNum)){
            return monitor.getWebCountInfo(adminUserNum);
        }else
            return Result.fail("失败，您无权操作!!!");
    }
}
