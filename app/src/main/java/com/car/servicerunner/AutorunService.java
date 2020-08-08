package com.car.servicerunner;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.car.servicerunner.strategies.AutorunStrategyInterface;
import com.car.servicerunner.strategies.AutorunStrategyRunIfIdle;

public class AutorunService extends Service implements Runnable {
    private static final boolean DEBUG_PROCESSES = false;
    private static final String TAG = AutorunService.class.getSimpleName();
    private static final String NOTIFICATION_CHANNEL = "ServiceRunner notification channel";

    private AutorunStrategyInterface autorunStrategyInterface;

    IServiceRunner iServiceRunner = new IServiceRunner.Stub() {
        @Override
        public void setServiceList(String[] packageArray, boolean[] autorunArray) {
            Log.d(TAG, "setServiceList()");
            autorunStrategyInterface.setServiceList(packageArray, autorunArray);
        }

        @Override
        public String[] getServicesPackages() {
            Log.d(TAG, "getServicesPackages()");
            return autorunStrategyInterface.getServicesPackages();
        }

        @Override
        public boolean[] getServicesAutorunStates() {
            Log.d(TAG, "getServicesAutorunStates()");
            return autorunStrategyInterface.getServicesAutorunStates();
        }

        @Override
        public void setAutorunDelay(int delay) {
            Log.d(TAG, "getServicesAutorunStates()");
            autorunStrategyInterface.setAutorunDelay(delay);
        }

    };

    public AutorunService() {
        Log.d(TAG, "AutorunService()");
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

        //autorunStrategyInterface = new AutorunStrategyRunLastCaptured();
        autorunStrategyInterface = new AutorunStrategyRunIfIdle();
        autorunStrategyInterface.setContext(this.getApplicationContext());

        new Thread(this).start();

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
        while (true) {
            if (DEBUG_PROCESSES) {
                autorunStrategyInterface.debugLastActivity();
            }
            autorunStrategyInterface.run();
        }
    }

    @Override
    public ComponentName startForegroundService(Intent service) {
        Log.d(TAG, "startForegroundService()");
        new Thread(this).start();
        return super.startForegroundService(service);
    }
}
