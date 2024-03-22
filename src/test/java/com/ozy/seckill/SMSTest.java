package com.ozy.seckill;

import com.aliyun.auth.credentials.Credential;
import com.aliyun.auth.credentials.provider.StaticCredentialProvider;
import com.aliyun.sdk.service.dysmsapi20170525.AsyncClient;
import com.aliyun.sdk.service.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.sdk.service.dysmsapi20170525.models.SendSmsResponse;
import com.google.gson.Gson;
import com.ozy.seckill.service.SMSService;
import com.ozy.seckill.service.impl.SMSServiceImpl;
import darabonba.core.client.ClientOverrideConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
public class SMSTest {
    @Autowired
    SMSService smsService;
    static int numberOfTasks=50;

    @Test
    public void testSms(){
        String phoneNum = "18580164887";
        String succ_msg = "https://cwcwx.nwu.edu.cn/zhifu/payAccept.aspx?prePayId=98b7e3c9de66e85e1e9efead112876cc5b816a01c137765c45346740f5f91b5e";
        String fail_msg = "没有选定时间范围内有效场次，可以尝试扩大时间范围再次尝试";


//        smsService.sendSuccessMessage(phoneNum, succ_msg);
        smsService.sendFailMessage(phoneNum, fail_msg);
//        executor.submit(new SendSms_fail_Runable(phoneNum, fail_msg));
    }

}
