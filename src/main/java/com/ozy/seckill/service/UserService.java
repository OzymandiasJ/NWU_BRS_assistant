package com.ozy.seckill.service;

import com.ozy.seckill.entity.Result;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Service
public interface UserService {
    /**
     * 用户登录方法
     * @param token
     * @return
     */
    Result getUser(String token);

    Result getResult(String userNum);

    /**
     * 获取用户当前任务状态或者结果
     * @param userNum
     * @return
     */
    Result gotResult(String userNum,String username);

    /**
     * 撤销任务
     * @param userNum
     * @return
     */
    Result cancelTask(String userNum);

    /**
     * 根据用户名查payUrl
     * @param userNum
     * @return
     */
    public String getPayUrl(String userNum);
}
