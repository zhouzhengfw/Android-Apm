package com.snail.collie.mem.koom

import android.app.Application
import com.kwai.koom.base.CommonConfig
import com.kwai.koom.base.MonitorManager

object CommonInitTask : InitTask {
  override fun init(application: Application) {
    val config = CommonConfig.Builder()
        .setApplication(application)
        .setVersionNameInvoker { "1.0.0" }
        .build()

    MonitorManager.initCommonConfig(config)
      .apply { onApplicationCreate() }
  }
}