package com.snail.collie.mem.koom

import android.app.Application

interface InitTask {
  fun init(application: Application)
}