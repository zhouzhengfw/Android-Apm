package com.snail.collie.trafficstats;

import android.app.Activity;
import android.app.Application;
import android.net.TrafficStats;
import android.os.Process;

import androidx.annotation.NonNull;

import com.snail.collie.Collie;
import com.snail.collie.core.ITracker;
import com.snail.collie.core.SimpleActivityLifecycleCallbacks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TrafficStatsTracker implements ITracker {

    private static volatile TrafficStatsTracker sInstance = null;
    private HashMap<Activity, TrafficStatsItem> mHashMap = new HashMap<>();
    private long mCurrentStats;
    private static int sSequence;
    private List<ITrackTrafficStatsListener> mStatsListeners = new ArrayList<>();
    private Application.ActivityLifecycleCallbacks mActivityLifecycleCallbacks = new SimpleActivityLifecycleCallbacks() {
        @Override
        public void onActivityStarted(@NonNull Activity activity) {
            super.onActivityStarted(activity);
            markActivityStart(activity);
        }

        @Override
        public void onActivityPaused(@NonNull Activity activity) {
            super.onActivityPaused(activity);
            markActivityPause(activity);
        }

        @Override
        public void onActivityDestroyed(@NonNull Activity activity) {
            super.onActivityDestroyed(activity);
            markActivityDestroy(activity);
        }
    };

    public void addTackTrafficStatsListener(ITrackTrafficStatsListener listener) {
        mStatsListeners.add(listener);
    }

    public void removeTrackTrafficStatsListener(ITrackTrafficStatsListener listener) {

        mStatsListeners.remove(listener);
    }

    private TrafficStatsTracker() {
    }

    public static TrafficStatsTracker getInstance() {
        if (sInstance == null) {
            synchronized (TrafficStatsTracker.class) {
                if (sInstance == null) {
                    sInstance = new TrafficStatsTracker();
                }
            }
        }
        return sInstance;
    }

    public void markActivityStart(Activity activity) {
        if (mHashMap.get(activity) == null) {
            TrafficStatsItem item = new TrafficStatsItem();
            item.activity = activity;
            item.sequence = sSequence++;
            item.trafficCost = 0;
            item.activityName = activity.getClass().getSimpleName();
            mHashMap.put(activity, item);
        }
        mCurrentStats = TrafficStats.getUidRxBytes(Process.myUid());
    }

    //  以pause为中断点
    public void markActivityPause(Activity activity) {
        TrafficStatsItem item = mHashMap.get(activity);
        if (item != null) {
            item.trafficCost += TrafficStats.getUidRxBytes(Process.myUid()) - mCurrentStats;
        }
    }

    //   防止泄露
    public void markActivityDestroy(Activity activity) {
        TrafficStatsItem item = mHashMap.get(activity);
        if (item != null) {
            for (ITrackTrafficStatsListener trafficStatsListener : mStatsListeners) {
                trafficStatsListener.onTrafficStats(item.activity, item.trafficCost);
                mHashMap.remove(activity);
            }
            item.activity = null;
        }
    }

    @Override
    public void destroy(Application application) {
        Collie.getInstance().removeActivityLifecycleCallbacks(mActivityLifecycleCallbacks);
        sInstance = null;
    }

    @Override
    public void startTrack(Application application) {
        Collie.getInstance().addActivityLifecycleCallbacks(mActivityLifecycleCallbacks);
    }

    @Override
    public void pauseTrack(Application application) {

    }

}
