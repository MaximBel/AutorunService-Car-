package com.car.servicerunner;

import androidx.annotation.NonNull;

public final class ServicePackageData {
    private final String packageName;
    private final boolean autorunRequired;

    ServicePackageData(@NonNull String servicePackage, boolean autorun) {
        packageName = servicePackage;
        autorunRequired = autorun;
    }
    public String getPackageName() {
        return packageName;
    }
    public boolean isAutorunRequired() {
        return autorunRequired;
    }
}
