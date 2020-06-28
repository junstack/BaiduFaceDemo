package com.das.face.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.das.face.Myapplication;

/**
 * created by jun on 2020/6/24
 * describe: sp操作类
 */
public class SharedPrefsUtil {
    private static final String IS_LOGIN = "IS_LOGIN";
    private static final String RECORD_ACCOUNT = "RECORD_ACCOUNT";

    /**
     * 记录账户信息
     */
    public static void recordAccount(String user) {
        SharedPreferences preferences = Myapplication.getContext().getSharedPreferences(RECORD_ACCOUNT, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("user", user);
        editor.apply();
    }

    /**
     * 获取账户信息
     */
    public static String getAccount(String key) {
        SharedPreferences sp = Myapplication.getContext().getSharedPreferences(RECORD_ACCOUNT, Context.MODE_PRIVATE);
        return sp.getString(key, "");
    }

    /**
     * 清除账户信息
     */
    public static void clearAccount() {
        SharedPreferences sp = Myapplication.getContext().getSharedPreferences(RECORD_ACCOUNT, Context.MODE_PRIVATE);
        sp.edit().clear().apply();
    }

    /**
     * 记录登录状态
     */
    public static void recordLoginState() {
        SharedPreferences sharedPreferences = Myapplication.getContext().getSharedPreferences(IS_LOGIN,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("is_login", true);
        editor.apply();
    }

    /**
     * 获取登录状态
     */
    public static boolean getLoginState() {
        SharedPreferences sharedPreferences = Myapplication.getContext().getSharedPreferences(IS_LOGIN,
                Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean("is_login", false);
    }

    /**
     * 清除登录状态
     */
    public static void clearLoginState() {
        SharedPreferences sp = Myapplication.getContext().getSharedPreferences(IS_LOGIN, Context.MODE_PRIVATE);
        sp.edit().clear().apply();
    }

}
