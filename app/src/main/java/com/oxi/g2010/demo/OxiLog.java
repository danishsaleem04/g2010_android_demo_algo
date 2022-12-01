package com.oxi.g2010.demo;

import android.util.Log;

/**
 * Created by wei.cui on 2017/1/24.
 */

public class OxiLog {

    private static String TAG = "g2010";
    private static boolean isTag = true;

    public static void Log(String content) {
        if (isTag)
            Log.d(TAG, content);
    }

    public static void Log(String tag, String content) {
        if (isTag)
            Log.d(tag, content);
    }

}
