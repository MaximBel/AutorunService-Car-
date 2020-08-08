package com.car.servicerunner.strategies;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.car.servicerunner.ServicePackageData;
import com.car.servicerunner.utils.AutorunFileUtils;

import java.util.List;

public class AutorunStrategyBase implements AutorunStrategyInterface {
    protected static final int DEFAULT_AUTORUN_DELAY = 10000; // 10 sec
    private static final String TAG = AutorunStrategyBase.class.getSimpleName();
    private static final long SERVICE_MONITORING_OFFSET = 1000; // 1 sec

    protected String fileRootPath = "";
    protected Context context;

    public AutorunStrategyBase() {
        Log.d(TAG, "AutorunStartegyBase()");
    }

    protected static long getLastTimeUsedForPackage(final Context context, final String packageName) {
        //Log.d(TAG, "getLastTimeUsedForPackage()");
        long lastTimeUsed = Long.MIN_VALUE;


        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            UsageStatsManager usm = (UsageStatsManager) context.getSystemService(
                    Context.USAGE_STATS_SERVICE);
            long time = System.currentTimeMillis();

            List<UsageStats> appStatsList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,
                    time - SERVICE_MONITORING_OFFSET, time);
            if (appStatsList != null && !appStatsList.isEmpty()) {

                for (UsageStats stat : appStatsList) {
                    if (stat.getPackageName().equals(packageName)) {
                        if (System.currentTimeMillis() - stat.getLastTimeUsed() < SERVICE_MONITORING_OFFSET) {
                            lastTimeUsed = stat.getLastTimeUsed();
                            //Log.d(TAG, "getLastTimeUsedForPackage(): package has found in stats");
                        }
                        break;
                    }
                }
            }
        }

        return lastTimeUsed;
    }

    @Override
    public final void setContext(Context context) {
        Log.d(TAG, "setContext()");
        this.context = context;
        fileRootPath = AutorunFileUtils.getAppFileRootPath(context);
    }

    @Override
    public final void debugLastActivity() {
        Log.d(TAG, "getLastTimeUsedForPackage()");
        long lastTimeUsed = Long.MIN_VALUE;

        UsageStats currentApp;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            UsageStatsManager usm = (UsageStatsManager) context.getSystemService(
                    Context.USAGE_STATS_SERVICE);
            long time = System.currentTimeMillis();
            List<UsageStats> appStatsList = usm.queryUsageStats(UsageStatsManager.INTERVAL_YEARLY,
                    time - 1000 * 1000, time);
            if (appStatsList != null && !appStatsList.isEmpty()) {

                currentApp = appStatsList.get(0);
                for (UsageStats stat : appStatsList) {
                    if (currentApp.getLastTimeUsed() < stat.getLastTimeUsed()) {
                        currentApp = stat;
                    }
                }

                if (currentApp != null) {
                    Log.d(TAG, "Current app is " + currentApp.getPackageName());
                }
            }
        }
    }

    @Override
    public void run() {
        Log.d(TAG, "run()");
    }

    @Override
    public void setServiceList(String[] packageArray, boolean[] autorunArray) {
        Log.d(TAG, "setServiceList()");
    }

    @Override
    public String[] getServicesPackages() {
        Log.d(TAG, "getServicesPackages()");
        return new String[0];
    }

    @Override
    public boolean[] getServicesAutorunStates() {
        Log.d(TAG, "getServicesAutorunStates()");
        return new boolean[0];
    }

    @Override
    public void setAutorunDelay(int delay) {
        Log.d(TAG, "setAutorunDelay(): " + delay);
    }

    protected final void startApp(@NonNull String packageName) {
        Log.d(TAG, "startApp(): " + packageName);
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        context.startActivity(launchIntent);
    }

    protected static class ServiceDataSerializer {
        public static ServicePackageData deserializeServiceData(String dataString) {
            Log.d(TAG, "deserializeServiceData()");
            ServicePackageData returnData = null;
            String splitedString[] = dataString.split(":", 2);

            if (splitedString.length != 2) {
                return null;
            }
            returnData = new ServicePackageData(splitedString[0], Boolean.parseBoolean(splitedString[1]));
            return returnData;
        }

        public static String serializeServiceData(ServicePackageData serviceData) {
            Log.d(TAG, "serializeServiceData()");
            String returnString = serviceData.getPackageName() + ":" + serviceData.isAutorunRequired();
            return returnString;
        }

        public static int deserializeAutorunDelayData(String dataString) {
            Log.d(TAG, "deserializeAutorunDelayData()");
            int returnData = Integer.parseInt(dataString);

            if (returnData <= 0) {
                return DEFAULT_AUTORUN_DELAY;
            }

            return returnData;
        }

        public static String serializeAutorunDelayData(int autorunDelay) {
            Log.d(TAG, "serializeAutorunDelayData()");
            return String.valueOf(autorunDelay);
        }
    }
}
