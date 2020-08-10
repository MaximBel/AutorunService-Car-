package com.car.servicerunner.strategies;

import android.util.Log;

import com.car.servicerunner.ServicePackageData;
import com.car.servicerunner.utils.AutorunFileUtils;

import java.util.ArrayList;

public class AutorunStrategyRunIfIdle extends AutorunStrategyRunLastCaptured {
    private static final String TAG = AutorunStrategyRunIfIdle.class.getSimpleName();

    public AutorunStrategyRunIfIdle() {
        Log.d(TAG, "AutorunStrategyRunIfIdle()");
    }

    boolean isAutorunRequestedForPackage(String packageName) {
        for (ServicePackageData data : serviceDataList) {
            if (data.getPackageName().equals(packageName) && data.isAutorunRequired()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void run() {
        Log.d(TAG, "run()");

        readServiceList();
        readAutorunDelay();

        boolean disableAutorun = false;
        long runStartTime = System.currentTimeMillis();

        while (true) {
            String currentLastService = checkTheLastActivityByPackage(serviceDataList);

            if (!currentLastService.equals("") && !currentLastService.equals(lastService)) {
                lastService = currentLastService;

                Log.d(TAG, "Last service has updated locally: " + lastService);

                if (isAutorunRequestedForPackage(lastService)) {
                    ArrayList<String> outputString = new ArrayList<String>();
                    outputString.add(currentLastService);
                    AutorunFileUtils.WriteFile(fileRootPath, LAST_SERVICE_FILE, outputString);
                    Log.d(TAG, "Last service has updated on storage: " + lastService);
                }
            }

            if (((System.currentTimeMillis() - runStartTime) > autorunDelay) && !disableAutorun) {
                if (!lastService.equals("")) {
                    // if service from list has started, do nothing
                    disableAutorun = true;
                    continue;
                }
                String lastServiceFromStorage = tryGetLastService();

                if (!lastServiceFromStorage.equals("")) {
                    // run last suitable service
                    Log.d(TAG, "run(), send intent to start service: " + lastServiceFromStorage);
                    startApp(lastServiceFromStorage);
                } else {
                    // run the first autorunable service
                    for (ServicePackageData data : serviceDataList) {
                        if (data.isAutorunRequired()) {
                            startApp(data.getPackageName());
                            break;
                        }
                    }
                }
                disableAutorun = true;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Log.d(TAG, "Thread is interrupted");
            }
        }
    }

    String tryGetLastService() {
        Log.d(TAG, "tryGetLastService()");
        ArrayList<String> list = AutorunFileUtils.ReadFile(fileRootPath, LAST_SERVICE_FILE);
        if (!list.isEmpty()) {
            return list.get(0);
        } else {
            return "";
        }
    }
}
