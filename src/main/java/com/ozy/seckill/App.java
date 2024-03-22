package com.ozy.seckill;

import com.ozy.seckill.config.DataLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Hello world!
 *
 */
@SpringBootApplication
@EnableScheduling
public class App {
    @Value("${devMode}")


    public static void main(String[] args) {

        DataLoader.loadDataFromFile(); // 加载数据
        SpringApplication.run(App.class, args);
    }
}

