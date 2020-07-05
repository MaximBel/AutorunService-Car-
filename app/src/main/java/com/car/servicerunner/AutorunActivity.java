package com.car.servicerunner;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

public class AutorunActivity extends AppCompatActivity implements View.OnClickListener {
    static private final String TAG = AutorunActivity.class.getSimpleName();

    ListView simpleList;
    CustomListAdapter customAdapter;

    private IServiceRunner iServiceRunner = null;

    ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected");
            iServiceRunner = IServiceRunner.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected");
            iServiceRunner = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindToService();

        simpleList = (ListView) findViewById(R.id.simpleListView);
        customAdapter = new CustomListAdapter(getApplicationContext());
        simpleList.setAdapter(customAdapter);
    }

    private void bindToService() {
        Intent intent = new Intent(this, AutorunService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void startService() {
        Intent myIntent = new Intent(this.getApplicationContext(), AutorunService.class);
        this.getApplicationContext().startForegroundService(myIntent);

        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        startActivity(intent);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.buttonStartService:
                startService();
                break;
            case R.id.buttonAddRow:
                customAdapter.addElement();
                updateCustomList();
                break;
            case R.id.buttonRemRow:
                customAdapter.removeElement();
                updateCustomList();
                break;
            case R.id.buttonReadPack:
                handleReadPackage();
                break;
            case R.id.buttonWritePack:
                handleWritePackage();
                break;
            default:
                Log.d(TAG, "Wrong button id");
        }
    }

    private void handleReadPackage() {
        if (iServiceRunner == null) {
            return;
        }

        String[] packageData = null;
        boolean[] autorunState = null;

        try {
            packageData = iServiceRunner.getServicesPackages();
            autorunState = iServiceRunner.getServicesAutorunStates();
        } catch (RemoteException e) {
            Log.d(TAG, "handleReadPackage()", e);
        }

        if (packageData.length != autorunState.length) {
            return;
        }

        customAdapter.clearData();
        for (int i = 0; i < packageData.length; i++) {
            customAdapter.addElement(packageData[i], autorunState[i]);
        }
        updateCustomList();
    }

    private void handleWritePackage() {
        if (iServiceRunner == null) {
            return;
        }

        try {
            iServiceRunner.setServiceList(customAdapter.getElementsStrings(), customAdapter.getElementsCheckBoxes());
        } catch (RemoteException e) {
            Log.d(TAG, "handleWritePackage()", e);
        }
    }

    void updateCustomList() {
        simpleList.invalidateViews();
        simpleList.refreshDrawableState();
    }
}