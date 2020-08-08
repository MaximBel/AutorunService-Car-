package com.car.servicerunner.strategies;

import android.content.Context;

public interface AutorunStrategyInterface {
    void setContext(Context context);

    void debugLastActivity();

    void setServiceList(String[] packageArray, boolean[] autorunArray);

    String[] getServicesPackages();

    boolean[] getServicesAutorunStates();

    void setAutorunDelay(int delay);

    void run();
}
