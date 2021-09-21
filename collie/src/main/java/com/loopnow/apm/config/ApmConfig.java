package com.loopnow.apm.config;

import static com.loopnow.apm.BuildConfig.DEBUG;

public class ApmConfig {

    public String appName = "";
    public String appVersion = "";
    public String apmId = "apm_unknown";
    public String userId = "";

    public boolean showDebugView = true;
    public boolean  fpsTrace = true;
    public boolean  trafficTrace = true;
    public boolean  memTrace = false ;
    public boolean  batteryTrace = true;
    public boolean  startUpTrace = true;
    public boolean  koom = true;

    public static class ConfigBuilder {
        private ApmConfig config = new ApmConfig();



        public ConfigBuilder setAppName(String appName) {
            this.config.appName = appName;
            return this;
        }

        public ConfigBuilder setAppVersion(String appVersion) {
            this.config.appVersion = appVersion;
            return this;
        }

        public ConfigBuilder setApmid(String apmId) {
            this.config.apmId = apmId;
            return this;
        }

        public ConfigBuilder setUserId(String userId) {
            this.config.userId = userId;
            return this;
        }


        public ConfigBuilder setFpsTraceEnabled(boolean flag) {
            this.config.fpsTrace = flag;
            return this;
        }

        public ConfigBuilder setDebugViewEnabled(boolean flag) {
            this.config.showDebugView = flag;
            return this;
        }

        public ConfigBuilder setTrafficEnabled(boolean flag) {
            this.config.trafficTrace = flag;
            return this;
        }

        public ConfigBuilder setMemTraceEnabled(boolean flag) {
            this.config.memTrace = flag;
            return this;
        }

        public ConfigBuilder setBatteryTraceEnabled(boolean flag) {
            this.config.batteryTrace = flag;
            return this;
        }

        public ConfigBuilder setStartUpTraceEnabled(boolean flag) {
            this.config.startUpTrace = flag;
            return this;
        }

        public ConfigBuilder setKoomEnabled(boolean flag) {
            this.config.koom = flag;
            return this;
        }

        public ApmConfig build() {
            if (DEBUG) {
            }

            return config;
        }
    }
}
