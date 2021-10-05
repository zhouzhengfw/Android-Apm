
轻量级Android性能监测工具

* FPSMonitor:  利用Looper的printLoop来实现
* 流量监测： Trafficstats
* 耗电 ：Battery BroadCast 
* 内存占用：Debug
* 内存泄漏：weakHashMap
* 启动耗时：ContentProvier+onwindforcus



### mavenCenter  maven {url "https://raw.githubusercontent.com/zhouzhengfw/Android-Apm/main"}

app的build.gradle添加

implementation 'com.loopnow:apm:1.0.2'
 

 add code in Application


        ApmConfig apmConfig = new ApmConfig.ConfigBuilder().setKoomEnabled(true).build();

        Apm.getInstance().init(this, apmConfig, issue -> {
            ToastUtil.show("type"+ issue.getType()+issue.getContent());
        });
	    
	
