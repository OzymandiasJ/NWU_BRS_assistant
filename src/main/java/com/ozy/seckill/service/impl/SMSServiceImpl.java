package com.ozy.seckill.service.impl;

import com.aliyun.auth.credentials.Credential;
import com.aliyun.auth.credentials.provider.StaticCredentialProvider;
import com.aliyun.sdk.service.dysmsapi20170525.AsyncClient;
import com.aliyun.sdk.service.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.sdk.service.dysmsapi20170525.models.SendSmsResponse;
import com.google.gson.Gson;
import com.ozy.seckill.service.SMSService;
import darabonba.core.client.ClientOverrideConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
@Service
public class SMSServiceImpl implements SMSService {
    @Value("${AccessKeyId}")
    private String AccessKeyId;
    @Value("${AccessKeySecret}")
    private String AccessKeySecret;
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());
    static int numberOfTasks=50;

    static ExecutorService executor = Executors.newFixedThreadPool(numberOfTasks);

    @Override
    public void sendSuccessMessage(String phoneNum, String msg) {
//        System.out.println(phoneNum+"_"+msg);
        executor.submit(new SendSms_succ_Runable(phoneNum, msg));
    }
    @Override
    public void sendFailMessage(String phoneNum, String msg) {
//        System.out.println(phoneNum+"_"+msg);
        executor.submit(new SendSms_fail_Runable(phoneNum, msg));
    }


    class SendSms_succ_Runable implements Runnable{
        private final String phoneNum;
        private final String msg;

        public SendSms_succ_Runable(String phoneNum, String msg) {
            this.phoneNum = phoneNum;
            this.msg = msg;
        }
        @Override
        public void run() {
            try{
                StaticCredentialProvider provider = StaticCredentialProvider.create(Credential.builder()
                        // Please ensure that the environment variables ALIBABA_CLOUD_ACCESS_KEY_ID and ALIBABA_CLOUD_ACCESS_KEY_SECRET are set.
                        .accessKeyId(AccessKeyId)
                        .accessKeySecret(AccessKeySecret)
                        //.securityToken(System.getenv("ALIBABA_CLOUD_SECURITY_TOKEN")) // use STS token
                        .build());

                // Configure the Client
                AsyncClient client = AsyncClient.builder()
                        .region("cn-hangzhou") // Region ID
                        //.httpClient(httpClient) // Use the configured HttpClient, otherwise use the default HttpClient (Apache HttpClient)
                        .credentialsProvider(provider)
                        //.serviceConfiguration(Configuration.create()) // Service-level configuration
                        // Client-level configuration rewrite, can set Endpoint, Http request parameters, etc.
                        .overrideConfiguration(
                                ClientOverrideConfiguration.create()
                                        // Endpoint 请参考 https://api.aliyun.com/product/Dysmsapi
                                        .setEndpointOverride("dysmsapi.aliyuncs.com")
                                //.setConnectTimeout(Duration.ofSeconds(30))
                        )
                        .build();

                // Parameter settings for API request
                SendSmsRequest sendSmsRequest = SendSmsRequest.builder()
                        .phoneNumbers(phoneNum)
                        .signName("云上智栈")
                        .templateCode("SMS_464355686")
                        // Request-level configuration rewrite, can set Http request parameters, etc.
                        // .requestConfiguration(RequestConfiguration.create().setHttpHeaders(new HttpHeaders()))
                        .build();
                // Asynchronously get the return value of the API request
                CompletableFuture<SendSmsResponse> response = client.sendSms(sendSmsRequest);
                // Synchronously get the return value of the API request
                SendSmsResponse resp = null;
                try {
                    resp = response.get();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("预约成功短信发送成功，手机号："+phoneNum);
                System.out.println("短信响应："+new Gson().toJson(resp));
                // Asynchronous processing of return values
                /*response.thenAccept(resp -> {
                    System.out.println(new Gson().toJson(resp));
                }).exceptionally(throwable -> { // Handling exceptions
                    System.out.println(throwable.getMessage());
                    return null;
                });*/

                // Finally, close the client
                client.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
    class SendSms_fail_Runable implements Runnable{
        private final String phoneNum;
        private final String msg;

        public SendSms_fail_Runable(String phoneNum, String msg) {
            this.phoneNum = phoneNum;
            this.msg = msg;
        }
        @Override
        public void run() {
            try{
                StaticCredentialProvider provider = StaticCredentialProvider.create(Credential.builder()
                        // Please ensure that the environment variables ALIBABA_CLOUD_ACCESS_KEY_ID and ALIBABA_CLOUD_ACCESS_KEY_SECRET are set.
                        .accessKeyId("LTAI5t91ENo8M65mdCCiria9")
                        .accessKeySecret("FHbwpCuUMjoQDrgr9anRjpOtGaxSVV")
                        //.securityToken(System.getenv("ALIBABA_CLOUD_SECURITY_TOKEN")) // use STS token
                        .build());

                // Configure the Client
                AsyncClient client = AsyncClient.builder()
                        .region("cn-hangzhou") // Region ID
                        //.httpClient(httpClient) // Use the configured HttpClient, otherwise use the default HttpClient (Apache HttpClient)
                        .credentialsProvider(provider)
                        //.serviceConfiguration(Configuration.create()) // Service-level configuration
                        // Client-level configuration rewrite, can set Endpoint, Http request parameters, etc.
                        .overrideConfiguration(
                                ClientOverrideConfiguration.create()
                                        // Endpoint 请参考 https://api.aliyun.com/product/Dysmsapi
                                        .setEndpointOverride("dysmsapi.aliyuncs.com")
                                //.setConnectTimeout(Duration.ofSeconds(30))
                        )
                        .build();

                // Parameter settings for API request
                SendSmsRequest sendSmsRequest = SendSmsRequest.builder()
                        .signName("云上智栈")
                        .templateCode("SMS_464355687")
                        .phoneNumbers(phoneNum)
                        .templateParam("{\"failReason\":\""+msg+"\"}")
                        // Request-level configuration rewrite, can set Http request parameters, etc.
                        // .requestConfiguration(RequestConfiguration.create().setHttpHeaders(new HttpHeaders()))
                        .build();

                // Asynchronously get the return value of the API request
                CompletableFuture<SendSmsResponse> response = client.sendSms(sendSmsRequest);

                // Synchronously get the return value of the API request
                SendSmsResponse resp = null;
                try {
                    resp = response.get();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("预约失败短信发送成功，手机号："+phoneNum);
                System.out.println("短信响应："+new Gson().toJson(resp));
                // Asynchronous processing of return values
                /*response.thenAccept(resp -> {
                    System.out.println(new Gson().toJson(resp));
                }).exceptionally(throwable -> { // Handling exceptions
                    System.out.println(throwable.getMessage());
                    return null;
                });*/

                // Finally, close the client
                client.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
