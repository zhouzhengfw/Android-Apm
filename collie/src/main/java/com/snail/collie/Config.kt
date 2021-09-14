package com.snail.collie

data class Config(
    var showDebugView: Boolean = true,
    var userFpsTrack: Boolean = true,
    var userTrafficTrack: Boolean = true,
    var userMemTrack: Boolean = false ,
    var userBatteryTrack: Boolean = true,
    var userStartUpTrack: Boolean = true,
    var userKoom: Boolean = true
)