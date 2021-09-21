package com.snail.labaffinity.app;

import android.app.Activity;
import android.app.Application;
import android.util.Log;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.loopnow.apm.config.ApmConfig;
import com.loopnow.apm.config.Issue;
import com.loopnow.apm.config.PluginListener;
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

        ApmConfig apmConfig = new ApmConfig.ConfigBuilder().setKoomEnabled(true).build();

        Apm.getInstance().init(this, apmConfig, issue -> {

        });
    }

    private static Application sApplication;

    public static Application getContext() {
        return sApplication;
    }

}
