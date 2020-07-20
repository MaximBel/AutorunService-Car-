package com.car.servicerunner.strategies;

import android.util.Log;

import com.car.servicerunner.ServicePackageData;
import com.car.servicerunner.utils.AutorunFileUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class AutorunStrategyRunLastCaptured extends AutorunStartegyBase {
    private static final String TAG = AutorunStrategyRunLastCaptured.class.getSimpleName();
    private static final int AUTORUN_DELAY_MSEC = 30000;
    private final static String SERVICE_LIST_FILE = "service_list.txt";
    private final static String LAST_SERVICE_FILE = "last_service.txt";

    private String lastService = "";
    private ArrayList<ServicePackageData> serviceDataList = null;

    public AutorunStrategyRunLastCaptured() {
        serviceDataList = new ArrayList<ServicePackageData>();
    }

    @Override
    public void run() {
        Log.d(TAG, "run()");

        readServiceList();
        tryStartLastService();

        while (true) {
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
                            startApp(data.getPackageName());
                        }
                    }
                }
            }
        };

        thread.start();
    }

    private String checkTheLastActivityByPackage(ArrayList<ServicePackageData> packages) {
        HashMap<String, Long> packageMap = new HashMap<String, Long>();

        for (ServicePackageData key : packages) {
            packageMap.put(key.getPackageName(), getLastTimeUsedForPackage(context, key.getPackageName()));
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

    private void readServiceList() {
        ArrayList<String> fileStringList = AutorunFileUtils.ReadFile(fileRootPath, SERVICE_LIST_FILE);

        serviceDataList.clear();
        for (String line : fileStringList) {
            ServicePackageData data = AutorunStartegyBase.ServiceDataSerializer.deserializeServiceData(line);
            if (data != null) {
                serviceDataList.add(data);
            }
        }
    }

    private void writeServiceList(ArrayList<ServicePackageData> serviceData) {
        ArrayList<String> fileStringList = new ArrayList<String>();
        for (ServicePackageData data : serviceData) {
            fileStringList.add(AutorunStartegyBase.ServiceDataSerializer.serializeServiceData(data));
        }
        AutorunFileUtils.WriteFile(fileRootPath, SERVICE_LIST_FILE, fileStringList);
    }
}
