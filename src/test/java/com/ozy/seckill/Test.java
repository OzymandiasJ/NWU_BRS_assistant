package com.ozy.seckill;

import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@SpringBootTest
public class Test {
    @org.junit.jupiter.api.Test
    public void test(){
        java.time.LocalDateTime currentTime = java.time.LocalDateTime.now();
        System.out.println(currentTime);
        java.time.LocalDateTime next30s0msTime = currentTime.plusSeconds(30 - (currentTime.getSecond() % 30)).withNano(0);
        long milliseconds = java.time.Duration.between(currentTime, next30s0msTime).toMillis();
        System.out.println(milliseconds);
    }
}
