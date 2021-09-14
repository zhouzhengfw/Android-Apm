package com.snail.collie;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kwai.koom.javaoom.monitor.OOMHprofUploader;
import com.kwai.koom.javaoom.monitor.OOMMonitor;
import com.snail.collie.battery.BatteryInfo;
import com.snail.collie.battery.BatteryStatsTracker;
import com.snail.collie.core.ActivityStack;
import com.snail.collie.core.CollieHandlerThread;
import com.snail.collie.debug.DebugHelper;
import com.snail.collie.fps.FpsTracker;
import com.snail.collie.fps.ITrackFpsListener;
import com.snail.collie.mem.KoomTrack;
import com.snail.collie.mem.MemoryLeakTrack;
import com.snail.collie.mem.TrackMemoryInfo;
import com.snail.collie.mem.koom.CommonInitTask;
import com.snail.collie.startup.LauncherTracker;
import com.snail.collie.trafficstats.ITrackTrafficStatsListener;
import com.snail.collie.trafficstats.TrafficStatsTracker;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Collie {

    private static volatile Collie sInstance = null;
    private Handler mHandler;
    private ITrackFpsListener mITrackListener;
    private ITrackTrafficStatsListener mTrackTrafficStatsListener;
    private MemoryLeakTrack.ITrackMemoryListener mITrackMemoryLeakListener;
    private LauncherTracker.ILaunchTrackListener mILaunchTrackListener;
    private BatteryStatsTracker.IBatteryListener mIBatteryListener;
    private KoomTrack.IKoomTrackListener mIKoomTrackListener;

    private List<CollieListener> mCollieListeners = new ArrayList<>();
    private HashSet<Application.ActivityLifecycleCallbacks> mActivityLifecycleCallbacks = new HashSet<>();

    private Collie() {
        mHandler = new Handler(CollieHandlerThread.getInstance().getHandlerThread().getLooper());
        mITrackListener = new ITrackFpsListener() {
            @Override
            public void onFpsTrack(final Activity activity, final long currentCostMils, final long currentDropFrame, final boolean isInFrameDraw, final long averageFps) {
                final long currentFps = currentCostMils == 0 ? 60 : Math.min(60, 1000 / currentCostMils);
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {

                        if (currentDropFrame > 1)
                            DebugHelper.getInstance().update("实时fps " + currentFps +
                                    "\n 丢帧 " + currentDropFrame + " \n1s平均fps " + averageFps
                                    + " \n本次耗时 " + currentCostMils);

                        for (CollieListener collieListener : mCollieListeners) {
                            collieListener.onFpsTrack(activity, currentCostMils, currentDropFrame, isInFrameDraw, averageFps);
                        }
                    }

                });
            }

            @Override
            public void onANRAppear(Activity activity) {
                for (CollieListener collieListener : mCollieListeners) {
                    collieListener.onANRAppear(activity);
                }
            }
        };

        mTrackTrafficStatsListener = new ITrackTrafficStatsListener() {
            @Override
            public void onTrafficStats(Activity activity, long value) {
                for (CollieListener collieListener : mCollieListeners) {
                    collieListener.onTrafficStats(activity, value);
                }
//                Log.v("Collie", "" + activityName + " 流量消耗 " + value * 1.0f / (1024 * 1024) + "M");
            }
        };

        mITrackMemoryLeakListener = new MemoryLeakTrack.ITrackMemoryListener() {
            @Override
            public void onLeakActivity(String activity, int count) {
                Log.v("Collie", "内存泄露 " + activity + " 数量 " + count);
                for (CollieListener collieListener : mCollieListeners) {
                    collieListener.onLeakActivity(activity, count);
                }
            }

            @Override
            public void onCurrentMemoryCost(TrackMemoryInfo trackMemoryInfo) {
//                Log.v("Collie", "内存  " + trackMemoryInfo.procName + " java内存  "
//                        + trackMemoryInfo.appMemory.dalvikPss + " native内存  " +
//                        trackMemoryInfo.appMemory.nativePss);
                for (CollieListener collieListener : mCollieListeners) {
                    collieListener.onCurrentMemoryCost(trackMemoryInfo);
                }
            }
        };
        mILaunchTrackListener = new LauncherTracker.ILaunchTrackListener() {
            @Override
            public void onAppColdLaunchCost(long duration ,String processName) {
//                Log.v("Collie", "cold " + duration);
                for (CollieListener collieListener : mCollieListeners) {
                    collieListener.onAppColdLaunchCost(duration,processName);
                }
            }

            @Override
            public void onActivityLaunchCost(Activity activity, long duration,boolean finishNow) {
////                Log.v("Collie", "activity启动耗时 " + activity + " " + duration);
//                if(duration>800){
//                    Toast.makeText(activity,"耗时 "+duration+"ms",Toast.LENGTH_SHORT).show();
//                }
                for (CollieListener collieListener : mCollieListeners) {
                    collieListener.onActivityLaunchCost(activity, duration,finishNow);
                }
            }
        };

        mIBatteryListener = new BatteryStatsTracker.IBatteryListener() {
            @Override
            public void onBatteryCost(BatteryInfo batteryInfo) {
                for (CollieListener collieListener : mCollieListeners) {
                    collieListener.onBatteryCost(batteryInfo);
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

    public static Collie getInstance() {
        if (sInstance == null) {
            synchronized (Collie.class) {
                if (sInstance == null) {
                    sInstance = new Collie();
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
                     final CollieListener listener) {
        application.registerActivityLifecycleCallbacks(mActivityLifecycleCallback);
        mCollieListeners.add(listener);

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

    public void registerCollieListener(CollieListener listener) {
        mCollieListeners.add(listener);
    }

    public void unRegisterCollieListener(CollieListener listener) {
        mCollieListeners.remove(listener);
    }

    public void stop(@NonNull Application application) {
        Log.e("OOMMonitor","stop");
        application.unregisterActivityLifecycleCallbacks(mActivityLifecycleCallback);
        CollieHandlerThread.getInstance().getHandlerThread().quitSafely();
        OOMMonitor.INSTANCE.stopLoop();
    }
}
