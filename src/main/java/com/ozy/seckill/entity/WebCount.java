package com.ozy.seckill.entity;

import lombok.Data;
import java.io.Serializable;
import java.util.HashSet;

/**
 * 项目统计信息类，全部是静态成员变量，项目启动后调用DataLoader从文件ApplicationDatas.txt文件中加载数据，每小时的40分时候持久化写入文件一次
 */
@Data
public class WebCount implements Serializable {
    public static int visitCount=0;//PV
    public static int userCount=0;//每日UV
    public static int likesCount =0;//点赞数
    public static int useCount =0;//系统总使用次数
    public static int taskCount =0;//预约任务数
    public static int taskSuccessCount =0;//预约任务成功数
    public static int taskFailCount =0;//预约任务失败数

    public static HashSet<String> ips =new HashSet<>();//存储统计UV的ip池

    public WebCount() {
    }

}
