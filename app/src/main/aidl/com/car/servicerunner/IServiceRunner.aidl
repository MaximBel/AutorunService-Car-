// IServiceRunner.aidl
package com.car.servicerunner;

interface IServiceRunner {
    oneway void setServiceList(in String[] packageArray, in boolean[] autorunArray);
    String[] getServicesPackages();
    boolean[] getServicesAutorunStates();
    void setAutorunDelay(in int delay);
}
