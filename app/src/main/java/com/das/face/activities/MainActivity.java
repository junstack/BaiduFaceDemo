package com.das.face.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

import com.baidu.aip.face.AipFace;
import com.das.face.R;
import com.das.face.util.ConstantUtil;
import com.das.face.util.SharedPrefsUtil;

import org.json.JSONException;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

/**
 * created by jun on 2020/6/28
 * describe: 主界面
 */
public class MainActivity extends BaseActivity implements View.OnClickListener {
    private static int BACK_CLICK_NUMBER = 0;
    private static AipFace client = new AipFace(ConstantUtil.BD_APP_ID, ConstantUtil.BD_API_KEY, ConstantUtil.BD_SECRET_KEY);
    /**
     * 是否注册人脸
     */
    private boolean isRegister;

    private Timer backTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn_rg).setOnClickListener(this);
        findViewById(R.id.btn_ex).setOnClickListener(this);
        findViewById(R.id.btn_up).setOnClickListener(this);
        findViewById(R.id.btn_logout).setOnClickListener(this);
        if (!SharedPrefsUtil.getLoginState()) {
            startActivity(new Intent(this, LoginActivity.class));
        }
        isFaceRegister();
    }

    /**
     * 查询是否已经注册人脸
     */
    public void isFaceRegister() {
        new Thread(() -> {
            HashMap<String, String> options = new HashMap<>();
            String idNumber = SharedPrefsUtil.getAccount("user");
            // 用户信息查询
            org.json.JSONObject res = client.getUser(idNumber, ConstantUtil.BD_GROUP_ID, options);
            try {
                int errorCode = res.getInt("error_code");
                isRegister = errorCode == 0;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_rg:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Intent intentFace = new Intent(this, FaceCamera2Activity.class);
                    intentFace.putExtra("functionFlag", 1);
                    startActivity(intentFace);
                } else {
                    Intent intentFace = new Intent(this, FaceCamera1Activity.class);
                    intentFace.putExtra("functionFlag", 1);
                    startActivity(intentFace);
                }
                break;
            case R.id.btn_ex:
                if (isRegister) {//已注册
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        Intent intentFace = new Intent(this, FaceCamera2Activity.class);
                        intentFace.putExtra("functionFlag", 2);
                        startActivity(intentFace);
                    } else {
                        Intent intentFace = new Intent(this, FaceCamera1Activity.class);
                        intentFace.putExtra("functionFlag", 2);
                        startActivity(intentFace);
                    }
                } else {//未注册（因为在MainActivity已经判断是否存在，存在就会注册，所以未注册说明不存在人像图片）
                    showToast("请先注册人脸");
                }
                break;
            case R.id.btn_up:
                if (isRegister) {//已注册
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        Intent intentFace = new Intent(this, FaceCamera2Activity.class);
                        intentFace.putExtra("functionFlag", 3);
                        startActivity(intentFace);
                    } else {
                        Intent intentFace = new Intent(this, FaceCamera1Activity.class);
                        intentFace.putExtra("functionFlag", 3);
                        startActivity(intentFace);
                    }
                } else {//未注册（因为在MainActivity已经判断是否存在，存在就会注册，所以未注册说明不存在人像图片）
                    Toast.makeText(this, "先注册人脸", Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.btn_logout:
                SharedPrefsUtil.clearAccount();
                SharedPrefsUtil.clearLoginState();
                finish();
                break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            BACK_CLICK_NUMBER++;
            switch (BACK_CLICK_NUMBER) {
                case 1:
                    showToast("再按一次返回桌面");
                    if (backTimer != null)
                        backTimer.cancel();
                    backTimer = new Timer();
                    backTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            BACK_CLICK_NUMBER = 0;
                        }
                    }, 2000);
                    break;
                case 2:
                    finish();
                    break;
            }
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }
}
