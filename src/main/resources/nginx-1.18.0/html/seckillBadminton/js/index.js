new Vue({
    el: "#app",
    data: {
      domain:'https://nwuserv.top',
      currentTime: '',
      statusMsg:"还未开始抢，点击查看使用说明",
      allUserMsgs:[
                "最近更新：2024.1.9，修复部分bug，优化登录状态存储"
                ,"预约任务模式下最晚00:30左右会发送短信通知预约结果，无论成功或失败，因为NWU预约系统一般会在00:00和00:30放场，偶尔随机时间放场，所以不需要取消任务频繁使用直抢模式"
                ,"请注意，如果预约成功请及时支付，不要卡时间支付，比如00:30:00预约成功，如果在00:34:59之前点击支付按钮进行支付，但是00:35:00之后才输完密码，会发生吞钱，即付款成功但是判定支付超时导致场次预约失败,而且不会退款"
                ,"欢迎点赞和github点star，如有任何问题欢迎带截图联系邮箱或者github提交Issues"
    ],
      informBtnDivMsg:"",
      optionBtnDivMsg:"",
      intervalId:null,
      token: null,
      username:null,
      userNum:null,
      phoneNum:null,
      isVip:true,
      logout:false,
      notTwoHour:false,
      firstTwoHour:false,
      startTime:17,
      endTime:18,
      accept_startTime:17,
      accept_endTime:19,
      startHours: Array.from({ length: 12 }, (_, i) => i + 9),
      endHours: Array.from({ length: 2 }, (_, i) => i + 17),
      accept_startHours: Array.from({ length: 12 }, (_, i) => i + 9),
      accept_endHours: Array.from({ length: 6}, (_, i) => i+16),
      isFinish:false,
      issuccess:true,
      spinMode:false,
      taskStatusCode:1,
      resultInfo:{
            payurl:"",
            name:"",
            time:"",
            price:"",
            requestNum:0,
            seconds:0
      },
      likesCount:999,//点赞数
      taskCount:999,//总提交任务数
      taskSuccessCount:999,//成功任务数
      isLikedToday:false
    },
    created() {
        setInterval(() => {
            this.currentTime = new Date().toLocaleTimeString();
        }, 1000);
      },
    methods: {
        redirectToPage() {
            window.location.href = "https://cgzx.nwu.edu.cn/#/pages/index/index";
        },
        toInstall1(){            
            window.open("https://microsoftedge.microsoft.com/addons/detail/%E7%AF%A1%E6%94%B9%E7%8C%B4/iikmkjmpaadaobahmlepeloendndfphd", "_blank");
            // window.location.href = "https://greasyfork.org/zh-CN/scripts/481369-nwu%E8%87%AA%E5%8A%A8%E6%8A%A2%E9%A6%86%E8%8E%B7%E5%8F%96token";
        },
        toInstall2(){
            window.open("https://greasyfork.org/zh-CN/scripts/482749-nwu%E8%87%AA%E5%8A%A8%E6%8A%A2%E9%A6%86%E8%8E%B7%E5%8F%96token", "_blank");
        },
        toLogout(){
            //清除登录信息
            this.username=null;
            this.userNum=null;
            this.token=null;
            this.logout=true;
            //清除本地存储的token
            localStorage.removeItem("token")
            localStorage.removeItem("tokenStorageTime")
            console.log("退出登录！")
            window.open("https://cgzx.nwu.edu.cn/#/pages/index/index?logout=true","_blank")
        },
        toBlog(){
            window.open("https://ozyblog.top/archives/453","_blank");
        },
        toGithub(){
            window.open("https://github.com/Oyzmandias/NWU_BRS_assistant");
        },
        toMail(){
            window.open("https://mail.google.com/mail/");
        },
        likeApp(){
            axios.get(this.domain+"/api/like?username="+this.username)
            console.log("感谢您的点赞")
            this.isLikedToday=true;
            this.likesCount=this.likesCount+1
            // 获取当前日期
            var currentDate = new Date();
            var day = currentDate.getDate();
            //k:用户名，value：上次点赞的日期
            localStorage.setItem(this.userNum,day)
        },
        displayLoaderDiv(){
            console.log("displayLoaderDiv")
            const element = this.$refs.loader_div; // 获取元素的引用
            element.style.display = 'flex'; 
        },
        closeLoaderDiv(){
            const element = this.$refs.loader_div; // 获取元素的引用
            element.style.display = 'none'; 
        },
        displayInformBtndiv(informBtnDivMsg){
            document.documentElement.style.overflowY = 'hidden'; 
            this.informBtnDivMsg=informBtnDivMsg;
            const element = this.$refs.informBtn_div; 
            element.style.display = 'flex'; 
        },
        closeInformBtnDiv(){
            document.body.style.overflow = '';
            // 获取元素的引用,关闭确认预约结果信息确认蒙版
            const element = this.$refs.informBtn_div; 
            element.style.display = 'none'; 
            const config = {
                params:{
                    "userNum": this.userNum,
                    "username":this.username
                }
            };
            axios.get(this.domain+"/api/seckill/gotResult",config)
            console.log()
            if(this.resultInfo.payurl!=null&&this.resultInfo.payurl!=""){
                window.open(this.resultInfo.payurl)
            }
        },
        displayAllUserInformDiv(){
            document.body.style.overflow = 'hidden';
            const element = this.$refs.allUserInform_div; 
            element.style.display = 'flex'; 
        },
        closeAllUserInformDiv(){
            document.body.style.overflow = '';
            const element = this.$refs.allUserInform_div; 
            element.style.display = 'none'; 
        },
        sponsor(){
            const element = this.$refs.sponsor_div; 
            element.style.display = 'flex'; 
        },
        closeSponsorDiv(){
            const element = this.$refs.sponsor_div; 
            element.style.display = 'none'; 
        },
        calcuteDelyToZeroTime(){
            //首先计算当前时间与00:00的时间延迟
            // 如果当前时间是00:00-12:00，那么返回-1，表示可以直接去发送请求了，否则正常计算
            // 获取当前时间的小时部分
            const currentTime = new Date();
            const currentHour = currentTime.getHours();
            // 判断当前时间是否在 00:00 到 12:00 之间
            const isBetweenMidnightAndNoon = currentHour >= 0 && currentHour < 12;
            if(isBetweenMidnightAndNoon){
                console.log("当前时间在 00:00 到 12:00 之间")
                return -1;
            }else{
                console.log("当前时间不在 00:00 到 12:00 之间")
                const midnight = new Date().setHours(23, 59, 59, 0);
                const timeDelay = midnight - currentTime+1000;
                // const timeDelay = 5000;//测试，延迟5s
                console.log('当前时间与 00:00 的时间延迟（毫秒）:', timeDelay);
                return timeDelay
            }
            
            
        },
        cancelTask(){
            this.displayLoaderDiv();
            console.log("取消任务")
            const config = {
                params:{
                    "userNum": this.userNum
                }
            };
            axios.get(this.domain+"/api/seckill/cancelTask",config)
            .then(({data})=>{
                console.log(data)
                if(data.success==true){
                    this.statusMsg=data.errorMsg;
                }else{
                    this.displayInformBtndiv(data.errorMsg)
                }
                this.taskStatusCode=0;
                //撤销任务后，应该同时取消主动拉取结果
                clearInterval(this.intervalId)
            }
            ).catch(function(){
                
            });
            this.closeLoaderDiv();
            
        },
        refreshReultTimely(){
            console.log("开始周期性获取结果")
            //刷新获取任务结果，为了减少服务器压力，每20s获取一次
            //获取到结果后关闭周期性请求，显示数据和弹窗
            const config = {
                params:{
                    "userNum": this.userNum
                }
            };
            const myvue=this
            this.intervalId = setInterval(function(){
                axios.get(myvue.domain+"/api/seckill/getResult",config)
                .then(data => {
                  // 处理响应数据
                  data=data.data
                  if(data.success==true){
                    //任务结束有了结果
                    console.log("任务结束，拿到结果，关闭周期性结果刷新请求")
                    clearInterval(myvue.intervalId);
                    data=data.data
                    //解析结果，显示弹窗
                    if(data.success==false){
                        myvue.statusMsg=data.errorMsg
                        myvue.displayInformBtndiv("您的预约任务失败，原因请查收");
                    }else if(data.success== true){
                        console.log(data)
                        if(data.data!=null){
                            data=data.data
                            myvue.issuccess=true;
                            myvue.isFinish=true;
                            myvue.resultInfo.name=data.name
                            myvue.resultInfo.time=data.time
                            myvue.resultInfo.price=data.price
                            myvue.resultInfo.requestNum=data.requestNum
                            myvue.resultInfo.payurl=data.payurl
                            myvue.resultInfo.seconds=data.seconds
                            myvue.statusMsg=data.body.msg
                            myvue.displayInformBtndiv("您的预约任务有了结果，请查收")
                        }else{
                            myvue.statusMsg=data.errorMsg
                            console.log("statusMsg:"+myvue.statusMsg)
                            myvue.displayInformBtndiv("您的预约任务失败，原因请查收");
                        }
                    }
                    myvue.taskStatusCode=0;
                    myvue.switchSpinMode();
                  }else if(data.success==false&&data.errorMsg=='该用户没有预约任务'){
                    //也要关闭周期性请求
                    clearInterval(intervalId);
                  }else if(data.success==false&&data.errorMsg=='任务还未完成'){
                    //继续周期性请求
                  }
                })
                .catch(error => {
                  // 处理请求错误
                  console.error('请求失败:', error);
                });
            }, 20000);
        },
        submitRequest(){
            
            this.displayLoaderDiv();
            console.log(this.spinMode)
            const config = {
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
                    "phoneNum":this.phoneNum,//手机号，用于短信下发
                    "spinMod":this.spinMode//，模式，true为自旋任务模式，false为直抢模式
                }
            };
            axios.get(this.domain+"/api/seckill/submitTask", config)
            .then(({data}) => {
                console.log("submitRequest方法响应数据")
                console.log(data)
                if(data.success==true){
                    if(data.data==null){
                        this.issuccess=false;
                        this.isFinish=true;
                        this.statusMsg=data.errorMsg;
                    }else{
                        data=data.data
                        console.log(data)
                        if(data.success==true){
                            console.log(data.success)
                            this.issuccess=true;
                            this.isFinish=true;
                            this.resultInfo.name=data.name;
                            this.resultInfo.time=data.time;
                            this.resultInfo.price=data.price;
                            this.resultInfo.requestNum=data.requestNum;
                            this.resultInfo.seconds=data.seconds;
                            this.statusMsg=data.body.msg;
                            this.resultInfo.payurl=data.payurl;
                            console.log(this.resultInfo.price)   
                            this.taskStatusCode=0;
                        }else{
                            this.issuccess=false;
                            this.isFinish=true;
                            this.statusMsg=data.msg;
                            console.log(this.statusMsg)
                            this.taskStatusCode=0;
                        }
                    }
                }else{
                    if(data.errorMsg.startsWith("出现异常")){
                        alert(data.errorMsg);
                    }
                    this.issuccess=false;
                    this.isFinish=true;
                    this.statusMsg=data.errorMsg;
                    console.log(this.statusMsg)
                    if(this.statusMsg.startsWith("成功：预约任务已提交")){
                        this.taskStatusCode=1;
                        //追加功能，定时发送请求获取结果
                        //00:00之后才请求刷新结果,且延迟10s
                        //首先计算当前时间与00:00的时间延迟
                        timeDelay=this.calcuteDelyToZeroTime();
                        if(timeDelay<=0){
                            console.log("已经过了00:00，10s后开始不断获取任务结果")
                            this.refreshReultTimely();
                        }else{
                            console.log("还没过00:00，"+(timeDelay/1000+10)+"s之后不断请求获取任务结果")
                            setTimeout(this.refreshReultTimely,timeDelay+10000);
                        }
                    }else{
                        this.taskStatusCode=0;
                    }
                    
                }
                this.closeLoaderDiv();
                }).catch(function (error) {
                console.log(error);
                this.closeLoaderDiv();
            });
        },
        handleSelection_expect_start(){//时间选择范围校验
            if(this.firstTwoHour){
                this.endTime=this.startTime+2
            }else{
                this.endTime=this.startTime+1
            }

            if(this.startTime<20){
                this.endHours=Array.from({ length: 2}, (_, i) => i+this.startTime+1)
            }else{
                this.endHours=Array.from({ length: 1}, (_, i) => i+this.startTime+1)
            }
            this.accept_endHours=Array.from({ length: 13-this.endTime+9 }, (_, i) => i + this.endTime)
            this.accept_startHours=Array.from({ length: this.startTime-9+1 }, (_, i) => i + 9)
            this.accept_startTime=this.startTime
            this.accept_endTime=this.endTime
        },
        handleSelection_expect_end(){//时间选择范围校验
            this.accept_endHours=Array.from({ length: 13-this.endTime+9 }, (_, i) => i + this.endTime)
            this.accept_endTime=this.endTime
        },
        switchFirstTwoHour(){
            console.log("this.firstTwoHour:"+this.firstTwoHour)
            if(this.firstTwoHour==true){
                this.notTwoHour=false
            }
        },
        switchNotTwoHour(){
            console.log("this.notTwoHour:"+this.notTwoHour)
            if(this.notTwoHour==true){
                this.firstTwoHour=false
            }
        },
        switchSpinMode(){
            this.spinMode=!this.spinMode;
        },
        data() {
            return {
                currentTime: ''
            };
        },


    },
    mounted() {
        var timestamp = Date.now();
        tokenStorageTime=localStorage.getItem("tokenStorageTime")
        var differenceInHours = Math.abs(Date.now() - tokenStorageTime) / (1000 * 60 * 60);
        if(differenceInHours>=24){
            this.displayAllUserInformDiv()//每24h显示1次
        }
        const myvue=this;
        myvue.displayLoaderDiv();
        this.accept_startHours=Array.from({ length: this.startTime-9+1 }, (_, i) => i + 9)
        this.accept_endHours=Array.from({ length: 13-this.endTime+9 }, (_, i) => i + this.endTime)
        // 获取token
        this.token = getQueryValueByKey("token");
        console.log("getQueryValueByKey:"+this.token)
        window.history.replaceState(null, null, this.domain);
        //根据token查用户信息
        var localToken =null;
        //新增需求，将token存在本地，过期时间为24小时
        if(this.token==null){//不是从统一认证系统来的，看下localstorage有没有存token
            localToken= localStorage.getItem("token")
            if(localToken!=null){
                tokenStorageTime=localStorage.getItem("tokenStorageTime")
                if(tokenStorageTime!==null){
                    //看下有没有超过24h
                    console.log("token已本地存储小时："+differenceInHours)
                    if(differenceInHours>=24){
                        console.log("token超过24小时,主动过期")
                        //就要清除本地token
                        localStorage.removeItem("token")
                        localStorage.removeItem("tokenStorageTime")
                    }else{
                        console.log("token未超过24小时,可以使用")
                        this.token=localToken
                    }
                }else{//客户端错误，只有token没有过期时间，可能是旧版本或者用户手动删除localstorage
                    console.log("客户端错误，只有token没有过期时间，可能是旧版本或者用户手动删除localstorage")
                }
            }else{
                //既不是从统一认证系统来的，还没有本地存储token，未登录状态
                console.log("既不是从统一认证系统来的，也没有本地存储token，未登录状态")
            }
        }else{//从统一认证系统来的，更新token
            console.log("更新token")
            var timestamp = Date.now();//存的是时间戳
            localStorage.setItem("token",this.token)
            localStorage.setItem("tokenStorageTime",timestamp)
        }
        

        if(this.token !== null){
            const config = {
                params:{
                    "token": this.token
                },
                headers: {
                    'Cache-Control': 'max-age=3600'
                }
            };
            //1,发送axios请求,获取登录用户 
            axios.get(this.domain+"/api/seckill/getUser", config)
            .then(({data}) => {
                console.log(data)
                if(data.errorMsg=="token过期，请退出重新登陆"){
                    this.displayInformBtndiv("token过期，请点击右上角退出重新登陆");
                }else{
                    var jsonObject = JSON.parse(data.data);
                    console.log(jsonObject.user)
                    this.username=jsonObject.user.username
                    this.userNum=jsonObject.user.userNum
                    this.phoneNum=jsonObject.user.mobile
                    this.isVip=jsonObject.user.isVip
                    this.taskStatusCode=jsonObject.user.taskStatusCode
                    console.log("taskStatusCode:"+this.taskStatusCode)
                    if(this.taskStatusCode==1){
                        this.statusMsg="您有一个计划抢购任务正在进行";
                        //追加功能，定时发送请求获取结果
                        //00:00之后才请求刷新结果,且延迟10s
                        //首先计算当前时间与00:00的时间延迟
                        timeDelay=this.calcuteDelyToZeroTime();
                        if(timeDelay<=0){
                            console.log("已经过了00:00，10s后开始不断获取任务结果")
                            this.refreshReultTimely();
                        }else{
                            console.log("还没过00:00，"+(timeDelay/1000+10)+"s之后不断请求获取任务结果")
                            setTimeout(this.refreshReultTimely,timeDelay+10000);
                        }
                    }else if(this.taskStatusCode==2){
                        this.statusMsg=jsonObject.user.taskResult.errorMsg
                        console.log("statusMsg:"+this.statusMsg)
                        this.displayInformBtndiv("您的预约任务失败，原因请查收");

                        this.taskStatusCode=0;
                        this.switchSpinMode();
                    }else if(this.taskStatusCode==3){
                        this.issuccess=true;
                        this.isFinish=true;
                        this.resultInfo.name=jsonObject.user.taskResult.data.name
                        this.resultInfo.time=jsonObject.user.taskResult.data.time
                        this.resultInfo.price=jsonObject.user.taskResult.data.price
                        this.resultInfo.requestNum=jsonObject.user.taskResult.data.requestNum
                        this.resultInfo.seconds=jsonObject.user.taskResult.data.seconds
                        this.resultInfo.payurl=jsonObject.user.taskResult.data.payurl
                        this.statusMsg=jsonObject.user.taskResult.data.body.msg
                        console.log(this.resultInfo.price)
                        this.displayInformBtndiv("您的预约任务有了结果，点击去支付");

                        this.taskStatusCode=0;
                        this.switchSpinMode();
                    }
                    //获取点赞信息
                    var lastLikeDay=localStorage.getItem(this.userNum)
                    var currentDate = new Date();
                    var today = currentDate.getDate();
                    console.log("当前点赞量:"+this.likesCount)
                    console.log("当前用户今天是否点赞过："+(today==lastLikeDay))
                    if(today==lastLikeDay){
                        //一天只允许点赞一次
                        this.isLikedToday=true;
                    }else{
                        this.isLikedToday=false;
                    }
                    console.log(this.username)
                    console.log(this.userNum)
                }
            }).catch(function (error) {
                console.log(error);
            }).finally(function(){
                //加载完成后取消加载条
                myvue.closeLoaderDiv();
            });
        }else{
            myvue.closeLoaderDiv();
        }
        //获取统计数
        axios.get(this.domain+"/api/seckill/getStatistic")
            .then(({data})=>{
                this.likesCount=data.data.likesCount
                this.taskCount=data.data.taskCount
                this.taskSuccessCount=data.data.taskSuccessCount
                //  百分比圆环
                var percent=Math.round(100*this.taskSuccessCount/this.taskCount)
                var cricleEl = document.querySelector('.circle-detail')
                var percentEl = document.querySelector('.percent')
                var circleLength = Math.floor(2 * Math.PI * 37);
                rotateCircle(cricleEl,percent)
                percentEl.innerHTML =percent + '%'
                function rotateCircle (el,percent) {
                    var val = parseFloat(percent).toFixed(0);
                    val = Math.max(0,val);
                    val = Math.min(100,val);
                    el.setAttribute("stroke-dasharray","" + circleLength * val / 100 + ",10000");
                }
            })
        //统计pv
        axios.get(this.domain+"/api/visit")




    }
});
function getQueryValueByKey(key) {
    var query = window.location.search.substring(1);
    var map = query.split("&");
    for (var element of map) {
        var keystr = element.split("=")[0];
            if (keystr === key) {
            return element.split("=")[1];
        }
    }
    return null;
}