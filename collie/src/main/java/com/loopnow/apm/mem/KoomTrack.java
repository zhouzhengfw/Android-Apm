package com.loopnow.apm.mem;

import android.app.Application;

import com.kwai.koom.base.MonitorLog;
import com.kwai.koom.base.MonitorManager;
import com.kwai.koom.javaoom.monitor.OOMHprofUploader;
import com.kwai.koom.javaoom.monitor.OOMMonitor;
import com.kwai.koom.javaoom.monitor.OOMMonitorConfig;
import com.kwai.koom.javaoom.monitor.OOMReportUploader;
import com.loopnow.apm.core.ITracker;
import com.loopnow.apm.mem.koom.OOMMonitorInitTask;

import java.io.File;

public class KoomTrack implements ITracker {

    private static volatile KoomTrack sInstance = null;
    OOMMonitorConfig.Builder builder = new OOMMonitorConfig.Builder();

    private KoomTrack() {
    }

    public static KoomTrack getInstance() {
        if (sInstance == null) {
            synchronized (KoomTrack.class) {
                if (sInstance == null) {
                    sInstance = new KoomTrack();
                }
            }
        }
        return sInstance;
    }

    @Override
    public void destroy(Application application) {
    }

    public void addOOMHprofUploader(final IKoomTrackListener listener){
        builder.setLoopInterval(5_000) // 5_000 for test! Please use default value!
                .setEnableHprofDumpAnalysis(true)
                .setHprofUploader(new OOMHprofUploader() {
                    public void upload(File file, OOMHprofUploader.HprofType type) {
                        MonitorLog.e("OOMMonitor", "todo, upload hprof ${file.name} if necessary");
                        listener.onHprofUploader(file,type);
                    }
                })
                .setReportUploader(new OOMReportUploader() {
                    public void upload(File file, String content) {
                        MonitorLog.i("OOMMonitor", content);
                        MonitorLog.e("OOMMonitor", "todo, upload report ${" +
                                file.getAbsolutePath() +
                                " if necessary");
                        listener.onReportUploader(file,content);
                    }
                });
    }

    public interface IKoomTrackListener {

        void onHprofUploader(File file, OOMHprofUploader.HprofType type);

        void onReportUploader(File file, String content);
    }


    @Override
    public void startTrack(Application application) {
        OOMMonitorInitTask.INSTANCE.init(application);
        MonitorManager.addMonitorConfig(builder.build());
        OOMMonitor.INSTANCE.startLoop(true, false, 5_000L);
    }

    @Override
    public void pauseTrack(Application application) {
    }
}
