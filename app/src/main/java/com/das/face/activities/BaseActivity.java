package com.das.face.activities;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Toast;

import com.das.face.Myapplication;

import java.lang.ref.WeakReference;

/**
 * created by jun on 2020/6/28
 * describe: 公共activity
 */
public abstract class BaseActivity extends Activity {
    private BaseHandler handler;
    public int screenWidth, screenHeight;
    public Myapplication application;
    public Activity activity;
    /**
     * 显示提示信息
     */
    private static final int SHOW_TOAST = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = this;
        application = (Myapplication) getApplication();
        handler = new BaseHandler(this);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenWidth = displayMetrics.widthPixels;
        screenHeight = displayMetrics.heightPixels;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    /**
     * 显示提示信息
     */
    public void showToast(String text) {
        Message message = new Message();
        message.what = SHOW_TOAST;
        message.obj = text;
        handler.sendMessage(message);
    }


    public void goBack(View view) {
        finish();
    }

    private static class BaseHandler extends Handler {

        private WeakReference<BaseActivity> weakActivity;
        private Toast toast;

        BaseHandler(BaseActivity activity) {
            weakActivity = new WeakReference<>(activity);
        }

        private void makeToast(Context context, String text) {
            if (toast != null)
                toast.cancel();
            toast = Toast.makeText(context, text, Toast.LENGTH_LONG);
            toast.show();
        }

        @Override
        public void handleMessage(Message msg) {
            BaseActivity activity = weakActivity.get();
            switch (msg.what) {
                case SHOW_TOAST:
                    String toast_msg = msg.obj.toString();
                    if (!TextUtils.isEmpty(toast_msg))
                        makeToast(activity, msg.obj.toString());
                    break;
            }
        }
    }

}
