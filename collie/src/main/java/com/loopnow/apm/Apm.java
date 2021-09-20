package com.loopnow.apm;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kwai.koom.javaoom.monitor.OOMHprofUploader;
import com.kwai.koom.javaoom.monitor.OOMMonitor;
import com.loopnow.apm.battery.BatteryInfo;
import com.loopnow.apm.battery.BatteryStatsTracker;
import com.loopnow.apm.core.ActivityStack;
import com.loopnow.apm.core.CollieHandlerThread;
import com.loopnow.apm.debug.DebugHelper;
import com.loopnow.apm.fps.FpsTracker;
import com.loopnow.apm.fps.ITrackFpsListener;
import com.loopnow.apm.mem.KoomTrack;
import com.loopnow.apm.mem.MemoryLeakTrack;
import com.loopnow.apm.mem.TrackMemoryInfo;
import com.loopnow.apm.mem.koom.CommonInitTask;
import com.loopnow.apm.startup.LauncherTracker;
import com.loopnow.apm.trafficstats.ITrackTrafficStatsListener;
import com.loopnow.apm.trafficstats.TrafficStatsTracker;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Apm {

    private static volatile Apm sInstance = null;
    private Handler mHandler;
    private ITrackFpsListener mITrackListener;
    private ITrackTrafficStatsListener mTrackTrafficStatsListener;
    private MemoryLeakTrack.ITrackMemoryListener mITrackMemoryLeakListener;
    private LauncherTracker.ILaunchTrackListener mILaunchTrackListener;
    private BatteryStatsTracker.IBatteryListener mIBatteryListener;
    private KoomTrack.IKoomTrackListener mIKoomTrackListener;

    private List<ApmListener> mApmListeners = new ArrayList<>();
    private HashSet<Application.ActivityLifecycleCallbacks> mActivityLifecycleCallbacks = new HashSet<>();

    private Apm() {
        mHandler = new Handler(CollieHandlerThread.getInstance().getHandlerThread().getLooper());
        mITrackListener = new ITrackFpsListener() {
            @Override
            public void onFpsTrack(final Activity activity, final long currentCostMils, final long currentDropFrame, final boolean isInFrameDraw, final long averageFps) {
                final long currentFps = currentCostMils == 0 ? 60 : Math.min(60, 1000 / currentCostMils);
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {

                        if (currentDropFrame > 1)
                            DebugHelper.getInstance().update("Current fps " + currentFps +
                                    "\n lostFrame " + currentDropFrame + " \n1s average fps " + averageFps
                                    + " \ncostTime " + currentCostMils);

                        for (ApmListener apmListener : mApmListeners) {
                            apmListener.onFpsTrack(activity, currentCostMils, currentDropFrame, isInFrameDraw, averageFps);
                        }
                    }

                });
            }

            @Override
            public void onANRAppear(Activity activity) {
                for (ApmListener apmListener : mApmListeners) {
                    apmListener.onANRAppear(activity);
                }
            }
        };

        mTrackTrafficStatsListener = new ITrackTrafficStatsListener() {
            @Override
            public void onTrafficStats(Activity activity, long value) {
                for (ApmListener apmListener : mApmListeners) {
                    apmListener.onTrafficStats(activity, value);
                }
            }
        };

        mITrackMemoryLeakListener = new MemoryLeakTrack.ITrackMemoryListener() {
            @Override
            public void onLeakActivity(String activity, int count) {
                Log.v("Collie", "memoryLeak " + activity + " count " + count);
                for (ApmListener apmListener : mApmListeners) {
                    apmListener.onLeakActivity(activity, count);
                }
            }

            @Override
            public void onCurrentMemoryCost(TrackMemoryInfo trackMemoryInfo) {
//                Log.v("Collie", "内存  " + trackMemoryInfo.procName + " java内存  "
//                        + trackMemoryInfo.appMemory.dalvikPss + " native内存  " +
//                        trackMemoryInfo.appMemory.nativePss);
                for (ApmListener apmListener : mApmListeners) {
                    apmListener.onCurrentMemoryCost(trackMemoryInfo);
                }
            }
        };
        mILaunchTrackListener = new LauncherTracker.ILaunchTrackListener() {
            @Override
            public void onAppColdLaunchCost(long duration ,String processName) {
//                Log.v("Collie", "cold " + duration);
                for (ApmListener apmListener : mApmListeners) {
                    apmListener.onAppColdLaunchCost(duration,processName);
                }
            }

            @Override
            public void onActivityLaunchCost(Activity activity, long duration,boolean finishNow) {
////                Log.v("Collie", "activity启动耗时 " + activity + " " + duration);
//                if(duration>800){
//                    Toast.makeText(activity,"耗时 "+duration+"ms",Toast.LENGTH_SHORT).show();
//                }
                for (ApmListener apmListener : mApmListeners) {
                    apmListener.onActivityLaunchCost(activity, duration,finishNow);
                }
            }
        };

        mIBatteryListener = new BatteryStatsTracker.IBatteryListener() {
            @Override
            public void onBatteryCost(BatteryInfo batteryInfo) {
                for (ApmListener apmListener : mApmListeners) {
                    apmListener.onBatteryCost(batteryInfo);
                }
            }
        };

        mIKoomTrackListener = new KoomTrack.IKoomTrackListener() {
            @Override
            public void onHprofUploader(File file, OOMHprofUploader.HprofType type) {

            }

            @Override
            public void onReportUploader(File file, String content) {

            }
        };
    }

    public static Apm getInstance() {
        if (sInstance == null) {
            synchronized (Apm.class) {
                if (sInstance == null) {
                    sInstance = new Apm();
                }
            }
        }
        return sInstance;
    }

    public void addActivityLifecycleCallbacks(Application.ActivityLifecycleCallbacks callbacks) {
        mActivityLifecycleCallbacks.add(callbacks);
    }

    public void removeActivityLifecycleCallbacks(Application.ActivityLifecycleCallbacks callbacks) {
        mActivityLifecycleCallbacks.remove(callbacks);
    }

    private Application.ActivityLifecycleCallbacks mActivityLifecycleCallback = new Application.ActivityLifecycleCallbacks() {
        @Override
        public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {
            ActivityStack.getInstance().push(activity);
            for (Application.ActivityLifecycleCallbacks item : mActivityLifecycleCallbacks) {
                item.onActivityCreated(activity, bundle);
            }
        }

        @Override
        public void onActivityStarted(@NonNull final Activity activity) {
            ActivityStack.getInstance().markStart();
            for (Application.ActivityLifecycleCallbacks item : mActivityLifecycleCallbacks) {
                item.onActivityStarted(activity);
            }
        }

        @Override
        public void onActivityResumed(@NonNull Activity activity) {
            for (Application.ActivityLifecycleCallbacks item : mActivityLifecycleCallbacks) {
                item.onActivityResumed(activity);
            }
        }

        @Override
        public void onActivityPaused(@NonNull Activity activity) {
            for (Application.ActivityLifecycleCallbacks item : mActivityLifecycleCallbacks) {
                item.onActivityPaused(activity);
            }
        }

        @Override
        public void onActivityStopped(@NonNull Activity activity) {
            ActivityStack.getInstance().markStop();
            for (Application.ActivityLifecycleCallbacks item : mActivityLifecycleCallbacks) {
                item.onActivityStopped(activity);
            }
        }

        @Override
        public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {
            for (Application.ActivityLifecycleCallbacks item : mActivityLifecycleCallbacks) {
                item.onActivitySaveInstanceState(activity, bundle);
            }
        }

        @Override
        public void onActivityDestroyed(@NonNull Activity activity) {
            for (Application.ActivityLifecycleCallbacks item : mActivityLifecycleCallbacks) {
                item.onActivityDestroyed(activity);
            }
            ActivityStack.getInstance().pop(activity);
        }
    };

    public void init(@NonNull Application application,
                     final Config config,
                     final ApmListener listener) {
        application.registerActivityLifecycleCallbacks(mActivityLifecycleCallback);
        mApmListeners.add(listener);

        if (config.getUserTrafficTrack()) {
            TrafficStatsTracker.getInstance().addTackTrafficStatsListener(mTrackTrafficStatsListener);
            TrafficStatsTracker.getInstance().startTrack(application);
        }
        if (config.getUserMemTrack()) {
            MemoryLeakTrack.getInstance().startTrack(application);
            MemoryLeakTrack.getInstance().addOnMemoryLeakListener(mITrackMemoryLeakListener);
        }
        if (config.getUserFpsTrack()) {
            FpsTracker.getInstance().setTrackerListener(mITrackListener);
            FpsTracker.getInstance().startTrack(application);
        }
        if (config.getShowDebugView()) {
            DebugHelper.getInstance().startTrack(application);
        }

        if (config.getUserBatteryTrack()) {
            BatteryStatsTracker.getInstance().addBatteryListener(mIBatteryListener);
            BatteryStatsTracker.getInstance().startTrack(application);
        }

        if (config.getUserStartUpTrack()) {
            LauncherTracker.getInstance().addLaunchTrackListener(mILaunchTrackListener);
            LauncherTracker.getInstance().startTrack(application);
        }


        if (config.getUserKoom()) {
            CommonInitTask.INSTANCE.init(application);
            KoomTrack.getInstance().addOOMHprofUploader(mIKoomTrackListener);
            KoomTrack.getInstance().startTrack(application);
        }
    }

    public void registerCollieListener(ApmListener listener) {
        mApmListeners.add(listener);
    }

    public void unRegisterCollieListener(ApmListener listener) {
        mApmListeners.remove(listener);
    }

    public void stop(@NonNull Application application) {
        Log.e("OOMMonitor","stop");
        application.unregisterActivityLifecycleCallbacks(mActivityLifecycleCallback);
        CollieHandlerThread.getInstance().getHandlerThread().quitSafely();
        OOMMonitor.INSTANCE.stopLoop();
    }
}
