package com.ozy.seckill.config;

import com.ozy.seckill.entity.WebCount;
import org.springframework.core.io.ClassPathResource;

import java.io.*;

public class DataLoader {
    public static void loadDataFromFile() {
        try {
            File file = new File("ApplicationDatas.data");
            BufferedReader reader = new BufferedReader(new FileReader(file));
            WebCount.visitCount = Integer.parseInt(reader.readLine());
            System.out.println("PV:"+WebCount.visitCount);

            WebCount.userCount  = Integer.parseInt(reader.readLine());
            System.out.println("UV:"+WebCount.userCount);

            WebCount.likesCount = Integer.parseInt(reader.readLine());
            System.out.println("点赞数:"+WebCount.likesCount);

            WebCount.useCount = Integer.parseInt(reader.readLine());
            System.out.println("系统总使用次数:"+WebCount.useCount);

            WebCount.taskCount = Integer.parseInt(reader.readLine());
            System.out.println("总提交任务数:"+WebCount.taskCount);

            WebCount.taskSuccessCount = Integer.parseInt(reader.readLine());
            System.out.println("预约任务成功数:"+WebCount.taskSuccessCount);

            WebCount.taskFailCount = Integer.parseInt(reader.readLine());
            System.out.println("预约任务失败数:"+WebCount.taskFailCount);

            System.out.println("成功率:"+String.format("%.2f",1.0*WebCount.taskSuccessCount/WebCount.taskCount));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}