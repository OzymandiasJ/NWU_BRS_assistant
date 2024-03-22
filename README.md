# NWU羽毛球馆预约助手
已开源，代码进攻学习交流使用，相关第三方API已隐去
直接使用网址：https://nwuserv.top

- 有没有会做前端vue开发的一起合作，联系我邮箱，非常感谢


众所周知西大羽毛球馆预约系统是一坨答辩，本人作为一名羽毛球爱好者深受土豆服务器的迫害，每次从12点半开始等一直要等到接近1点页面才能加载出来，这时候已经基本上所有场次被抢光了
所以开发了这个系统，用于帮助用户快速抢馆，开发完后试用了下效果很不错，2s完成预约

前端页面：
![image](https://github.com/Oyzmandias/NWU_BRS_assistant/assets/69197910/f84c5a37-ed4a-4088-925f-6bcc25588d22)

![image](https://github.com/Oyzmandias/NWU_BRS_assistant/assets/69197910/8a5eaa87-023e-4f59-b923-3d1f4eed24f1)


# 系统使用说明：
- 1，本应用由一个编程和羽毛球爱好者开发，用于西北大学羽毛球馆自动抢馆，解决手机微信端载入速度极慢的问题，不对成功率负责，场次数量是固定的，狼多肉少必然导致大部分人抢不到
- 2，推荐电脑访问,推荐使用Edge或者Chrome浏览器，移动设备可以点击页脚链接去博客看使用说明，网页之所以设计黑色是因为防止大家凌晨吃闪
- 3，做了多线程并发下打散目标场次的算法优化，降低多线程抢同一场次产生的碰撞率，尽量提高成功率，此外，不同用户的预约任务提交顺序不影响成功率
- 4，应用不存储您的账号密码，只存储身份令牌用于自动登录，请放心使用

打开网站，点击登录，会自动跳到NWU统一认证页面，登录完成后携带用户token返回，会显示当前登录用户信息，然后选择你期望的场次范围，比如开始时间17，结束时间22，就会寻找下午5点到21：30之间的所有可预约场次，如果勾选不接受2小时，会自动排除2小时的可用场次信息

点击开始抢购按钮，等待一下会看到预约结果


# 项目Overview
## 项目名
NWU场馆预约助手
## 项目解决痛点
解决了微信网页端抢馆因为静态资源加载慢导致网页一直刷新进不去，球馆难以预约到的问题
## 项目提供的功能
- 使用统一认证接口自动登录
- 选择期望时间范围直接抢馆
- 选择期望时间范围，提前提交任务预约抢馆
## 项目技术栈
- 后端使用springboot，前端vue渲染数据，使用restful风格接口实现前后端交互
- 使用nginx实现端口转发，使用NAT实现内网穿透对外提供服务
- 不使用数据库持久化，免得被人说保存用户数据，只定期在本地写入网站PV、UV、点赞量统计数据
## 软件流程图
[图片]
## 技术详解
- 1，前端
前端使用html+css+js开发，总代码量没仔细统计，可能大约2k行把，大部分是css代码行数比较多
- 2，接口
规定接口如下：
1. 获取当前用户：
url
http://nwuserv.top/api/seckill/getUser
参数
用户token
注意事项
token不能为空
2. 提交任务
url
 https://nwuserv.top/api/seckill/submitTask

方式：get

参数
```
                params:{
                    "token": this.token ,//用户token
                    "startTime":this.startTime,//期望开始时间
                    "endTime":this.endTime,//期望结束时间
                    "accept_startTime":this.accept_startTime,//能接受的最早开始时间
                    "accept_endTime":this.accept_endTime,//能接受的最晚结束时间
                    "notTwoHour":this.notTwoHour,//不接受两小时的场次
                    "firstTwoHour":this.firstTwoHour,//优先两小时场次
                    "username":this.username,//用户名
                    "userNum":this.userNum,//学号
                    "spinMod":this.spinMode//，模式，true为自旋任务模式，false为直抢模式
                }
```
返回
返回任务提交的结果
注意
请确保提交时学号姓名token不为空
3. 撤销任务
url： https://nwuserv.top/api/seckill/cancelTask

方式：get

参数：userNum学号

4. pv+1
url
 https://nwuserv.top/api/visit
方式:get
参数:无
返回:无
5. 用户点赞
url: https://nwuserv.top/api/like
方式:get
参数:username用户名
6. 查看当前提交任务的人员列表
url： https://nwuserv.top/api/admin/getSpinTaskResults
方式：get
参数：adminUserNum=管理员学号

7. 获取结果集合
url: https://nwuserv.top/api/admin/getSpinTaskResults
方式：get
参数：adminUserNum=管理员学号
9. PVUV点赞数获取
url:https://nwuserv.top/api/admin/getWebCountInfo
方式：get
参数：adminUserNum=管理员学号

3，后端
使用Controller-Service-DAO架构，MainController为中央控制器，因为接口较少就只是用一个controller了，Service负责处理业务逻辑，而本项目因为不想存储用户信息就没有设计DAO层进行持久化

后端如下：
![image](https://github.com/Oyzmandias/NWU_BRS_assistant/assets/69197910/0fe5c22b-d634-4473-b719-9acb809e113d)


后端首先配置了三个拦截器，按照层级如下：
LikesCountInterceptor
接受用户点赞请求
registry.addInterceptor(new LikesCountInterceptor()).addPathPatterns(
        "/like"
).order(0);
RequestInterceptor
用户统计pv，统计到了之后直接返回false
registry.addInterceptor(new RequestInterceptor()).addPathPatterns(
        "/visit"
).order(1);
UserIntercepter
拦截所有请求,统计uv
registry.addInterceptor(new UserIntercepter()).addPathPatterns(
        "/seckill/getUser",
        "/seckill/submitTask"
).order(2);

然后中央控制器MainControlelr，需要调用三个service，使用依赖注入方式注入，下面三个service介绍如下：
1. UserService
用户service，方法如下：
```
@Service
public interface UserService {
    /**
     * 用户登录方法
     * @param token
     * @return
     */
    Result getUser(String token);

    /**
     * 获取用户当前任务状态或者结果
     * @param userNum
     * @return
     */
    Result gotResult(String userNum);

    /**
     * 撤销任务
     * @param userNum 
     * @return
     */
    Result cancelTask(String userNum);
}
```
2. SuperSeckillService
```
@Service
public interface UserService {
    /**
     * 用户登录方法
     * @param token
     * @return
     */
    Result getUser(String token);

    /**
     * 获取用户当前任务状态或者结果
     * @param userNum
     * @return
     */
    Result gotResult(String userNum);

    /**
     * 撤销任务
     * @param userNum 
     * @return
     */
    Result cancelTask(String userNum);
}
```
3. NormalSeckillService
```
public interface NormalSeckillService {
    /**
     * 通用单用户单线程秒杀抢馆方法，一个用户抢馆时候必须用单线程
     * 如果使用上面的多线程会造成一个用户同时抢到多个场馆的情况
     * @param token 
     * @param startTime
     * @param endTime
     * @param accept_startTime
     * @param accept_endTime
     * @param notTwoHour
     * @param firstTwoHour
     * @param username
     * @param userNum
     * @param threadTaskIndex
     * @return
     */
    Result singleThreadSeckill(String token, String startTime, String endTime, String accept_startTime, String accept_endTime, String notTwoHour, String firstTwoHour, String username,String userNum,Integer threadTaskIndex);
}

```
