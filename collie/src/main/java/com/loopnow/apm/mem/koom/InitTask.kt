package com.loopnow.apm.mem.koom

import android.app.Application

interface InitTask {
  fun init(application: Application)
}