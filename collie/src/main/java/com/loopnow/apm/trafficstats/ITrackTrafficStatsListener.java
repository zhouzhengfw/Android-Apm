package com.loopnow.apm.trafficstats;

import android.app.Activity;

public interface ITrackTrafficStatsListener {

    void onTrafficStats(Activity activity, long value);

}
