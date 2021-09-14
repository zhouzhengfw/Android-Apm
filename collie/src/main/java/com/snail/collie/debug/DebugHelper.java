package com.snail.collie.debug;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Handler;

import androidx.annotation.NonNull;

import com.snail.collie.BuildConfig;
import com.snail.collie.Collie;
import com.snail.collie.core.ActivityStack;
import com.snail.collie.core.CollieHandlerThread;
import com.snail.collie.core.ITracker;
import com.snail.collie.core.SimpleActivityLifecycleCallbacks;

/**
 * 掉帧检测
 */
public class DebugHelper implements ITracker {

    private static volatile DebugHelper sInstance = null;

    private FloatingFpsView mDebugCollieView;
    private Handler mHandler;
    private FloatHelper mFloatHelper;
    private SimpleActivityLifecycleCallbacks mSimpleActivityLifecycleCallbacks = new SimpleActivityLifecycleCallbacks() {

        @Override
        public void onActivityStopped(@NonNull Activity activity) {
            super.onActivityStopped(activity);
            if (ActivityStack.getInstance().isInBackGround()) {
                hide();
            }
        }

        @Override
        public void onActivityResumed(@NonNull Activity activity) {
            super.onActivityResumed(activity);
            show(activity.getApplication());
        }
    };

    private DebugHelper() {
        mHandler = new Handler(CollieHandlerThread.getInstance().getHandlerThread().getLooper());
    }

    public static DebugHelper getInstance() {
        if (sInstance == null) {
            synchronized (DebugHelper.class) {
                if (sInstance == null) {
                    sInstance = new DebugHelper();
                }
            }
        }
        return sInstance;
    }

    public void show(final Application context) {

        if (mFloatHelper != null && mFloatHelper.isShowing()) {
            return;
        }
        if (mDebugCollieView == null) {
            mDebugCollieView = new FloatingFpsView(context);
            mFloatHelper = new FloatHelper(context);
            mFloatHelper.setAlignSide(false)
                    .setInitPosition(context.getResources().getDisplayMetrics().widthPixels - MeasureUtil.getMeasuredWidth(mDebugCollieView, 0), 200);

        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mFloatHelper.setView(mDebugCollieView)
                        .show(ActivityStack.getInstance().getTopActivity());
            }
        });
    }

    public void hide() {
        if (mFloatHelper != null) {
            mFloatHelper.destroy();
            mHandler.removeCallbacksAndMessages(null);
        }
    }

    public void update(final String content) {

        if (mFloatHelper != null && mFloatHelper.isShowing()) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mDebugCollieView.update(content);
                }
            });
        }
    }

    @Override
    public void destroy(Application application) {

    }

    @Override
    public void startTrack(Application application) {
        Collie.getInstance().addActivityLifecycleCallbacks(mSimpleActivityLifecycleCallbacks);
    }

    @Override
    public void pauseTrack(Application application) {

    }
}
