package com.loopnow.apm;

import static com.loopnow.apm.config.IssueType.ACTIVITYLAUNCH;
import static com.loopnow.apm.config.IssueType.ANR;
import static com.loopnow.apm.config.IssueType.APPLAUNCH;
import static com.loopnow.apm.config.IssueType.BATTERYCOST;
import static com.loopnow.apm.config.IssueType.FPS;
import static com.loopnow.apm.config.IssueType.KOOM;
import static com.loopnow.apm.config.IssueType.LEAK;
import static com.loopnow.apm.config.IssueType.MEMORYCOST;
import static com.loopnow.apm.config.IssueType.TRAFFIC;

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
import com.loopnow.apm.config.ApmConfig;
import com.loopnow.apm.config.Issue;
import com.loopnow.apm.config.IssueType;
import com.loopnow.apm.config.PluginListener;
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

import org.json.JSONException;
import org.json.JSONObject;

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

    private List<PluginListener> mApmListeners = new ArrayList<>();
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

                        Issue issue = new Issue();
                        issue.setType(FPS.getType());
                        Log.e("FPS",""+FPS.getType());
                        JSONObject info = new JSONObject();
                        try {
                            info.put("activity",activity.getClass().getName());
                            info.put("currentCostMils",currentCostMils);
                            info.put("currentDropFrame",currentDropFrame);
                            info.put("isInFrameDraw",isInFrameDraw);
                            info.put("averageFps",averageFps);
                            issue.setContent(info);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        for (PluginListener apmListener : mApmListeners) {
                            apmListener.onReportIssue(issue);
                        }
                    }

                });
            }

            @Override
            public void onANRAppear(Activity activity) {
                Issue issue = new Issue();
                issue.setType(ANR.getType());

                JSONObject info = new JSONObject();
                try {
                    info.put("activity",activity.getClass().getName());

                    issue.setContent(info);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                for (PluginListener apmListener : mApmListeners) {
                    apmListener.onReportIssue(issue);
                }
            }
        };

        mTrackTrafficStatsListener = new ITrackTrafficStatsListener() {
            @Override
            public void onTrafficStats(Activity activity, long value) {
                Log.e("onTrafficStats",""+value);
                Issue issue = new Issue();
                issue.setType(TRAFFIC.getType());

                JSONObject info = new JSONObject();
                try {
                    info.put("activity",activity.getClass().getName());
                    info.put("value",value);

                    issue.setContent(info);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                for (PluginListener apmListener : mApmListeners) {
                    apmListener.onReportIssue(issue);
                }
            }
        };

        mITrackMemoryLeakListener = new MemoryLeakTrack.ITrackMemoryListener() {
            @Override
            public void onLeakActivity(String activity, int count) {
                Log.v("Collie", "memoryLeak " + activity + " count " + count);
                Issue issue = new Issue();
                issue.setType(LEAK.getType());

                JSONObject info = new JSONObject();
                try {
                    info.put("activity",activity.getClass().getName());
                    info.put("count",count);
                    issue.setContent(info);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                for (PluginListener apmListener : mApmListeners) {
                    apmListener.onReportIssue(issue);
                }
            }

            @Override
            public void onCurrentMemoryCost(TrackMemoryInfo trackMemoryInfo) {

                Issue issue = new Issue();
                issue.setType(MEMORYCOST.getType());

                JSONObject info = new JSONObject();
                try {
                    info.put("procName",trackMemoryInfo.procName);
                    info.put("javaHeap",trackMemoryInfo.appMemory.dalvikPss);
                    info.put("nativeHeap",trackMemoryInfo.appMemory.nativePss);
                    issue.setContent(info);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                for (PluginListener apmListener : mApmListeners) {
                    apmListener.onReportIssue(issue);
                }
            }
        };
        mILaunchTrackListener = new LauncherTracker.ILaunchTrackListener() {
            @Override
            public void onAppColdLaunchCost(long duration ,String processName) {
                Issue issue = new Issue();
                issue.setType(APPLAUNCH.getType());
                Log.e("onAppColdLaunchCost",duration+"");
                JSONObject info = new JSONObject();
                try {
                    info.put("procName",processName);
                    info.put("duration",duration);
                    issue.setContent(info);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                for (PluginListener apmListener : mApmListeners) {
                    apmListener.onReportIssue(issue);
                }
            }

            @Override
            public void onActivityLaunchCost(Activity activity, long duration,boolean finishNow) {
                Issue issue = new Issue();
                issue.setType(ACTIVITYLAUNCH.getType());
                Log.e("onActivityLaunchCost",duration+"");

                JSONObject info = new JSONObject();
                try {
                    info.put("activity",activity.getClass().getName());
                    info.put("duration",duration);
                    issue.setContent(info);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                for (PluginListener apmListener : mApmListeners) {
                    apmListener.onReportIssue(issue);
                }

            }
        };

        mIBatteryListener = new BatteryStatsTracker.IBatteryListener() {
            @Override
            public void onBatteryCost(BatteryInfo batteryInfo) {
                Issue issue = new Issue();
                issue.setType(BATTERYCOST.getType());

                JSONObject info = new JSONObject();
                try {
                    info.put("charging",batteryInfo.charging);
                    info.put("cost",batteryInfo.cost);
                    issue.setContent(info);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };

        mIKoomTrackListener = new KoomTrack.IKoomTrackListener() {
            @Override
            public void onHprofUploader(File file, OOMHprofUploader.HprofType type) {

            }

            @Override
            public void onReportUploader(File file, String content) {
                Issue issue = new Issue();
                issue.setType(KOOM.getType());

                JSONObject info = new JSONObject();
                try {
                    info.put("content",content);
                    issue.setContent(info);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                for (PluginListener apmListener : mApmListeners) {
                    apmListener.onReportIssue(issue);
                }
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
                     final ApmConfig config,
                     final PluginListener listener) {
        application.registerActivityLifecycleCallbacks(mActivityLifecycleCallback);
        mApmListeners.add(listener);

        if (config.trafficTrace) {
            TrafficStatsTracker.getInstance().addTackTrafficStatsListener(mTrackTrafficStatsListener);
            TrafficStatsTracker.getInstance().startTrack(application);
        }
        if (config.memTrace) {
            MemoryLeakTrack.getInstance().startTrack(application);
            MemoryLeakTrack.getInstance().addOnMemoryLeakListener(mITrackMemoryLeakListener);
        }
        if (config.fpsTrace) {
            FpsTracker.getInstance().setTrackerListener(mITrackListener);
            FpsTracker.getInstance().startTrack(application);
        }
        if (config.showDebugView) {
            DebugHelper.getInstance().startTrack(application);
        }

        if (config.batteryTrace) {
            BatteryStatsTracker.getInstance().addBatteryListener(mIBatteryListener);
            BatteryStatsTracker.getInstance().startTrack(application);
        }

        if (config.startUpTrace) {
            LauncherTracker.getInstance().addLaunchTrackListener(mILaunchTrackListener);
            LauncherTracker.getInstance().startTrack(application);
        }


        if (config.koom) {
            CommonInitTask.INSTANCE.init(application);
            KoomTrack.getInstance().addOOMHprofUploader(mIKoomTrackListener);
            KoomTrack.getInstance().startTrack(application);
        }
    }

    public void registerApmListener(PluginListener listener) {
        mApmListeners.add(listener);
    }

    public void unRegisterApmListener(PluginListener listener) {
        mApmListeners.remove(listener);
    }

    public void stop(@NonNull Application application) {
        Log.e("OOMMonitor","stop");
        application.unregisterActivityLifecycleCallbacks(mActivityLifecycleCallback);
        CollieHandlerThread.getInstance().getHandlerThread().quitSafely();
        OOMMonitor.INSTANCE.stopLoop();
    }
}
