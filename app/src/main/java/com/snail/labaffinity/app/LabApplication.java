package com.snail.labaffinity.app;

import android.app.Activity;
import android.app.Application;
import android.util.Log;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.netease.nis.bugrpt.CrashHandler;
import com.loopnow.apm.Apm;
import com.loopnow.apm.ApmListener;
import com.loopnow.apm.Config;
import com.loopnow.apm.battery.BatteryInfo;
import com.loopnow.apm.mem.TrackMemoryInfo;

import cn.campusapp.router.Router;

/**
 * Author: hzlishang
 * Data: 16/10/11 下午12:44
 * Des:
 * version:
 */
public class LabApplication extends Application {
    @Override
    public void onCreate() {

        super.onCreate();
        CrashHandler.init(getApplicationContext());
//        CrashReport.initCrashReport(getApplicationContext(), "e7f834a1e0", BuildConfig.DEBUG);
        sApplication = this;
//        SystemClock.sleep(3000);
        Router.initBrowserRouter(this);
        Router.initActivityRouter(getApplicationContext());
        FirebaseAnalytics.getInstance(this);
        Apm.getInstance().init(this, new Config(true, true, true, true, true, true,true), new ApmListener() {

            @Override
            public void onTrafficStats(Activity activity, long value) {
                Log.v("Collie", "" + activity.getClass().getSimpleName() + " 流量消耗 " + value * 1.0f / (1024 * 1024) + "M");

            }

            @Override
            public void onBatteryCost(BatteryInfo batteryInfo) {
                Log.v("Collie",  " 电量流量消耗 " +batteryInfo.cost);

            }

            @Override
            public void onAppColdLaunchCost(long duration ,String processName) {
                Log.v("Collie", "启动耗时 " + duration +" processName "+processName);
            }

            @Override
            public void onActivityLaunchCost(Activity activity, long duration,boolean finishNow) {
                Log.v("Collie", "activity启动耗时 " + activity + " " + duration + " finishNow "+finishNow);
//                if (duration > 800) {
//                toast 可能导致短时间内存泄露
//                    Toast.makeText(activity, "耗时 " + duration + "ms", Toast.LENGTH_SHORT).show();
//                }
            }

            @Override
            public void onLeakActivity(String activity, int count) {
                Log.v("Collie", "内存泄露 " + activity + " 数量 " + count);
            }

            @Override
            public void onCurrentMemoryCost(TrackMemoryInfo trackMemoryInfo) {
                Log.v("Collie", "内存  " + trackMemoryInfo.procName + " java内存  "
                        + trackMemoryInfo.appMemory.dalvikPss + " native内存  " +
                        trackMemoryInfo.appMemory.nativePss);
            }

            @Override
            public void onFpsTrack(Activity activity, long currentCostMils, long currentDropFrame, boolean isInFrameDraw, long averageFps) {
                if (currentDropFrame >= 2)
                    Log.v("Collie", "Activity " + activity + " 掉帧 " + currentDropFrame + " 是否因为Choro 绘制掉帧 " + isInFrameDraw + " 1s 平均帧率" + averageFps);
            }

            @Override
            public void onANRAppear(Activity activity) {
                Log.v("Collie", "Activity " + activity + " ANR  " );

            }
        });
    }

    private static Application sApplication;

    public static Application getContext() {
        return sApplication;
    }

}
