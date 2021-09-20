package com.loopnow.apm.mem.koom

import android.app.Application
import com.kwai.koom.base.MonitorLog

import com.kwai.koom.base.MonitorManager
import com.kwai.koom.javaoom.monitor.OOMHprofUploader
import com.kwai.koom.javaoom.monitor.OOMMonitorConfig
import com.kwai.koom.javaoom.monitor.OOMReportUploader
import java.io.File

object OOMMonitorInitTask : InitTask {

  override fun init(application: Application) {
    val config = OOMMonitorConfig.Builder()

        .setLoopInterval(5_000) // 5_000 for test! Please use default value!
        .setEnableHprofDumpAnalysis(true)
        .setHprofUploader(object: OOMHprofUploader {
          override fun upload(file: File, type: OOMHprofUploader.HprofType) {
            MonitorLog.e("OOMMonitor", "todo, upload hprof ${file.name} if necessary")
          }
        })
        .setReportUploader(object: OOMReportUploader {
          override fun upload(file: File, content: String) {
            MonitorLog.i("OOMMonitor", content)
            MonitorLog.e("OOMMonitor", "todo, upload report ${file.name} if necessary")
          }
        })
        .build()

    MonitorManager.addMonitorConfig(config)
  }
}