package com.Revsoft.Wabbitemu.utils;

import android.os.Environment;

public class StorageUtils {
    public static boolean hasExternalStorage() {
        String state = Environment.getExternalStorageState();
        return state.contentEquals("mounted") || state.contentEquals("mounted_ro");
    }

    public static String getPrimaryStoragePath() {
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }
}
