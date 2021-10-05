package com.snail.test.app;

import android.app.Application;
import android.util.Log;

import com.loopnow.apm.config.ApmConfig;
import com.netease.nis.bugrpt.CrashHandler;
import com.loopnow.apm.Apm;

import cn.campusapp.router.Router;


public class LabApplication extends Application {
    @Override
    public void onCreate() {

        super.onCreate();
        CrashHandler.init(getApplicationContext());
//        CrashReport.initCrashReport(getApplicationContext(), "e7f834a1e0", BuildConfig.DEBUG);
        sApplication = this;
        Router.initBrowserRouter(this);
        Router.initActivityRouter(getApplicationContext());

        ApmConfig apmConfig = new ApmConfig.ConfigBuilder().setKoomEnabled(true).build();

        Apm.getInstance().init(this, apmConfig, issue -> {
//            ToastUtil.show("type"+ issue.getType()+issue.getContent());
            Log.e("issue",issue.getContent().toString());
        });
    }

    private static Application sApplication;

    public static Application getContext() {
        return sApplication;
    }

}
