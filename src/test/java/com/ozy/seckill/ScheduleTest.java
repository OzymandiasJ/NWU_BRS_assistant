package com.ozy.seckill;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
@SpringBootTest
public class ScheduleTest {
//    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
//    // 表示每天的午夜 0 点触发任务执行。
////    @Scheduled(cron = "0 0 0 * * ?")
//    @Scheduled(cron = "0 21 15 * * ?",zone = "Asia/Shanghai")
//    @Test
//    public void executeDailyTask() {
//        LocalDateTime now = LocalDateTime.now();
//        System.out.println("Scheduled task executed at " + dateTimeFormatter.format(now));
//        // 在这里添加您的任务逻辑
//    }
}
