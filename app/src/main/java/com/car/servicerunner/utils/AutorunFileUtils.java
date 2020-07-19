package com.car.servicerunner.utils;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class AutorunFileUtils {
    private static final String TAG = AutorunFileUtils.class.getSimpleName();

    public static String getAppFileRootPath(@NonNull Context context) {
        return context.getApplicationContext().getFilesDir().getPath();
    }

    public static ArrayList<String> ReadFile(String rootPath, String fileName) {
        Log.d(TAG, "ReadFile()");
        ArrayList<String> returnList = new ArrayList<>();
        String tempString = null;

        try {
            FileInputStream fileInputStream = new FileInputStream(new File(rootPath + "/" + fileName));
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            while ((tempString = bufferedReader.readLine()) != null) {
                returnList.add(tempString);
            }
            fileInputStream.close();
            bufferedReader.close();
        } catch (IOException ex) {
            Log.d(TAG, ex.getMessage());
        }
        return returnList;
    }

    public static boolean WriteFile(String rootPath, String fileName, ArrayList<String> stringList) {
        Log.d(TAG, "WriteFile()");

        try {
            new File(rootPath).mkdir();
            File file = new File(rootPath + "/" + fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream fileOutputStream = new FileOutputStream(file, false);
            fileOutputStream.flush();
            for (String str : stringList) {
                fileOutputStream.write((str + System.getProperty("line.separator")).getBytes());
            }
            fileOutputStream.close();
            return true;
        } catch (IOException ex) {
            Log.d(TAG, ex.getMessage());
        }
        return false;
    }
}
