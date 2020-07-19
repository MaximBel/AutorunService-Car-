package com.car.servicerunner;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.car.servicerunner.utils.AutorunFileUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class AutorunService extends Service implements Runnable {
    private static final boolean DEBUG_PROCESSES = false;
    private final static String SERVICE_LIST_FILE = "service_list.txt";
    private final static String LAST_SERVICE_FILE = "last_service.txt";
    private static final String TAG = AutorunService.class.getSimpleName();
    private static final String NOTIFICATION_CHANNEL = "ServiceRunner notification channel";
    private static final int AUTORUN_DELAY_MSEC = 30000;

    private String fileRootPath = "";
    private String lastService = "";
    private ArrayList<ServicePackageData> serviceDataList = null;

    IServiceRunner iServiceRunner = new IServiceRunner.Stub() {
        @Override
        public void setServiceList(String[] packageArray, boolean[] autorunArray) {
            Log.d(TAG, "setServiceList()");

            serviceDataList.clear();

            for (int i = 0; i < packageArray.length; i++) {
                serviceDataList.add(new ServicePackageData(packageArray[i], autorunArray[i]));
            }

            writeServiceList(serviceDataList);
            // check that data has successfully loaded to file
            readServiceList();
        }

        @Override
        public String[] getServicesPackages() {
            Log.d(TAG, "getServicesPackages()");

            String[] packageData = new String[serviceDataList.size()];

            for (ServicePackageData data : serviceDataList) {
                packageData[serviceDataList.indexOf(data)] = data.getPackageName();
            }

            return packageData;
        }

        @Override
        public boolean[] getServicesAutorunStates() {
            Log.d(TAG, "getServicesAutorunStates()");

            boolean[] autorunState = new boolean[serviceDataList.size()];

            for (ServicePackageData data : serviceDataList) {
                autorunState[serviceDataList.indexOf(data)] = data.isAutorunRequired();
            }

            return autorunState;
        }
    };

    public AutorunService() {
        Log.d(TAG, "AutorunService()");

        serviceDataList = new ArrayList<ServicePackageData>();

        new Thread(this).start();
    }

    public static void buildNotificationChannel(Context appContext) {
        NotificationChannel chan1 = new NotificationChannel(NOTIFICATION_CHANNEL,
                appContext.getString(R.string.default_notification_channel_id),
                NotificationManager.IMPORTANCE_NONE);
        chan1.setSound(null, null);

        NotificationManager nm = (NotificationManager) appContext.getSystemService(
                Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.createNotificationChannel(chan1);
        }
    }

    public static long getLastTimeUsedForPackage(final Context context, final String packageName) {
        Log.d(TAG, "getLastTimeUsedForPackage()");
        long lastTimeUsed = Long.MIN_VALUE;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            UsageStatsManager usm = (UsageStatsManager) context.getSystemService(
                    Context.USAGE_STATS_SERVICE);
            long time = System.currentTimeMillis();
            List<UsageStats> appStatsList = usm.queryUsageStats(UsageStatsManager.INTERVAL_YEARLY,
                    time - 1000 * 1000, time);
            if (appStatsList != null && !appStatsList.isEmpty()) {

                for (UsageStats stat : appStatsList) {
                    if (stat.getPackageName().equals(packageName)) {
                        lastTimeUsed = stat.getLastTimeUsed();
                        Log.d(TAG, "getLastTimeUsedForPackage(): package has found in stats");
                        break;
                    }
                }
            }
        }

        return lastTimeUsed;
    }

    public static void checkCurrentApp(final Context context) {
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
                    if( currentApp.getLastTimeUsed() < stat.getLastTimeUsed()) {
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
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand()");

        buildNotificationChannel(this.getApplicationContext());

        Notification.Builder builder = new Notification.Builder(this, NOTIFICATION_CHANNEL)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Monitoring")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setAutoCancel(true);

        Notification notification = builder.build();
        startForeground(1, notification);

        fileRootPath = AutorunFileUtils.getAppFileRootPath(this.getApplicationContext());

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind()");
        return (IBinder) iServiceRunner;
    }

    @Override
    public void run() {
        Log.d(TAG, "run()");

        readServiceList();
        tryStartLastService();

        while (true) {

            if(DEBUG_PROCESSES) {

                checkCurrentApp(this.getApplicationContext());
            }

            String currentLastService = checkTheLastActivityByPackage(serviceDataList);

            if (!currentLastService.equals("") && !currentLastService.equals(lastService)) {
                lastService = currentLastService;

                ArrayList<String> outputString = new ArrayList<String>();
                outputString.add(currentLastService);
                AutorunFileUtils.WriteFile(fileRootPath, LAST_SERVICE_FILE, outputString);
                Log.d(TAG, "Last service has updated: " + lastService);
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Log.d(TAG, "Thread is interrupted");
            }
        }
    }



    private String checkTheLastActivityByPackage(ArrayList<ServicePackageData> packages) {
        HashMap<String, Long> packageMap = new HashMap<String, Long>();

        for (ServicePackageData key : packages) {
            packageMap.put(key.getPackageName(), getLastTimeUsedForPackage(this.getApplicationContext(), key.getPackageName()));
        }

        Map.Entry<String, Long> theLastActivity = null;

        Iterator it = packageMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> pair = (Map.Entry) it.next();

            if (theLastActivity == null && pair.getValue().longValue() != Long.MIN_VALUE) {
                theLastActivity = pair;
                continue;
            }
            if (theLastActivity != null && theLastActivity.getValue() <= pair.getValue()) {
                theLastActivity = pair;
            }
        }

        return (theLastActivity != null) ? theLastActivity.getKey() : "";
    }

    @Override
    public ComponentName startForegroundService(Intent service) {
        Log.d(TAG, "startForegroundService()");

        new Thread(this).start();
        return super.startForegroundService(service);
    }

    private void tryStartLastService() {
        Log.d(TAG, "tryStartLastService()");

        Thread thread = new Thread() {
            public void run() {
                Log.d(TAG, "tryStartLastService().run()");

                try {
                    Thread.sleep(AUTORUN_DELAY_MSEC);
                } catch (InterruptedException e) {
                    Log.d(TAG, "run() thread is interrupted");
                }

                ArrayList<String> list = AutorunFileUtils.ReadFile(fileRootPath, LAST_SERVICE_FILE);
                if (!list.isEmpty()) {
                    for (ServicePackageData data : serviceDataList) {
                        if (data.getPackageName().equals(list.get(0)) && data.isAutorunRequired()) {
                            Log.d(TAG, "run(), send intent to start service");
                            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(data.getPackageName());
                            startActivity(launchIntent);
                        }
                    }
                }
            }
        };

        thread.start();
    }

    private void readServiceList() {
        ArrayList<String> fileStringList = AutorunFileUtils.ReadFile(fileRootPath, SERVICE_LIST_FILE);

        serviceDataList.clear();
        for (String line : fileStringList) {
            ServicePackageData data = ServiceDataSerializer.deserializeServiceData(line);
            if (data != null) {
                serviceDataList.add(data);
            }
        }
    }

    private void writeServiceList(ArrayList<ServicePackageData> serviceData) {
        ArrayList<String> fileStringList = new ArrayList<String>();
        for (ServicePackageData data : serviceData) {
            fileStringList.add(ServiceDataSerializer.serializeServiceData(data));
        }
        AutorunFileUtils.WriteFile(fileRootPath, SERVICE_LIST_FILE, fileStringList);
    }

    private static class ServiceDataSerializer {
        public static ServicePackageData deserializeServiceData(String dataString) {
            ServicePackageData returnData = null;
            String splitedString[] = dataString.split(":", 2);

            if (splitedString.length != 2) {
                return null;
            }
            returnData = new ServicePackageData(splitedString[0], Boolean.parseBoolean(splitedString[1]));
            return returnData;
        }

        public static String serializeServiceData(ServicePackageData serviceData) {
            String returnString = serviceData.getPackageName() + ":" + serviceData.isAutorunRequired();
            return returnString;
        }
    }
}
