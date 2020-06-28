package com.das.face;

import android.app.Application;
import android.content.Context;

import java.lang.ref.WeakReference;

/**
 * created by jun on 2020/6/24
 * describe:
 */
public class Myapplication extends Application {
    private static WeakReference<Myapplication> weakApplication;
    @Override
    public void onCreate() {
        super.onCreate();
        weakApplication = new WeakReference<>(this);
    }
    public static Context getContext() {
        return weakApplication.get().getApplicationContext();
    }
}
