package com.car.servicerunner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AutorunReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent myIntent = new Intent(context, AutorunService.class);
        context.startForegroundService(myIntent);
    }
}
