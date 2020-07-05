package com.car.servicerunner;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;

import java.util.ArrayList;

public class CustomListAdapter extends BaseAdapter {
    private static final String DEFAULT_STRING = "Enter package name";
    Context context;
    ArrayList<View> viewData;
    ArrayList<ServicePackageData> serviceData;
    LayoutInflater inflter;

    public CustomListAdapter(Context applicationContext) {
        viewData = new ArrayList<>();
        serviceData = new ArrayList<>();
        this.context = applicationContext;
        inflter = (LayoutInflater.from(applicationContext));
    }

    @Override
    public int getCount() {
        return serviceData.size();
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        // handle new line of the list
        if (viewData.get(i) == null) {
            view = inflter.inflate(R.layout.activity_listview, null);
            // save view to use it further
            viewData.set(i, view);
            EditText packageName = (EditText) viewData.get(i).findViewById(R.id.servicePackageName);
            CheckBox autorunState = (CheckBox) viewData.get(i).findViewById(R.id.autorunCheckBox);
            packageName.setText(serviceData.get(i).getPackageName());
            autorunState.setChecked(serviceData.get(i).isAutorunRequired());
        }
        return viewData.get(i);
    }

    public void clearData() {
        serviceData.clear();
        viewData.clear();
    }

    public void addElement(String elementString, boolean elementCB) {
        serviceData.add(new ServicePackageData(elementString, elementCB));
        viewData.add(null);
    }

    public void addElement() {
        addElement(DEFAULT_STRING, false);
        viewData.add(null);
    }

    public void removeElement() {
        if (serviceData.size() == 0) {
            return;
        }
        int index = serviceData.size() - 1;
        serviceData.remove(index);
        viewData.remove(index);
    }

    public String[] getElementsStrings() {
        String[] outStr = new String[serviceData.size()];

        for (int i = 0; i < outStr.length; i++) {
            outStr[i] = ((EditText) viewData.get(i).findViewById(R.id.servicePackageName)).getText().toString();
            ;
        }

        return outStr;
    }

    public boolean[] getElementsCheckBoxes() {
        boolean[] outBool = new boolean[serviceData.size()];

        for (int i = 0; i < outBool.length; i++) {
            outBool[i] = ((CheckBox) viewData.get(i).findViewById(R.id.autorunCheckBox)).isChecked();
        }

        return outBool;
    }
}