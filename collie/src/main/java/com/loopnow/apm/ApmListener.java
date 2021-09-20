package com.loopnow.apm;

import com.loopnow.apm.battery.BatteryStatsTracker;
import com.loopnow.apm.fps.ITrackFpsListener;
import com.loopnow.apm.mem.MemoryLeakTrack;
import com.loopnow.apm.startup.LauncherTracker;
import com.loopnow.apm.trafficstats.ITrackTrafficStatsListener;

public interface ApmListener extends LauncherTracker.ILaunchTrackListener, ITrackFpsListener, MemoryLeakTrack.ITrackMemoryListener, ITrackTrafficStatsListener, BatteryStatsTracker.IBatteryListener {

}
