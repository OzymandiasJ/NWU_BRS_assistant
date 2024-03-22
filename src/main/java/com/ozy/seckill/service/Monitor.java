package com.ozy.seckill.service;

import com.ozy.seckill.entity.Result;
import org.springframework.web.bind.annotation.RequestParam;

public interface Monitor {

    Result getWebCountInfo(String adminUserNum);

    Result getSpinTaskUserNames(String adminUserNum);
    Result getSpinTaskResults(String adminUserNum);
}
