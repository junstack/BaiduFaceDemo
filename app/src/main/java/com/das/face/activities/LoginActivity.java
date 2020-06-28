package com.das.face.activities;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;

import com.das.face.R;
import com.das.face.util.ConstantUtil;
import com.das.face.util.PublicMethods;
import com.das.face.util.SharedPrefsUtil;

/**
 * created by jun on 2020/6/28
 * describe: 登录界面
 */
public class LoginActivity extends BaseActivity implements View.OnClickListener {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        findViewById(R.id.btn_delete).setOnClickListener(this);
        findViewById(R.id.btn_login).setOnClickListener(this);
        findViewById(R.id.btn_change_type).setOnClickListener(this);
        //设置图标大小
        Drawable drawable1 = getResources().getDrawable(R.mipmap.ic_login_user);
        int d_size = PublicMethods.dip2px(this, 24);
        drawable1.setBounds(0, 0, d_size, d_size);
        getEtAccount().setCompoundDrawables(drawable1, null, null, null);
        Drawable drawable2 = getResources().getDrawable(R.mipmap.ic_login_password);
        drawable2.setBounds(0, 0, d_size, d_size);
        getEtPassword().setCompoundDrawables(drawable2, null, null, null);
        getCbPswVisibility().setOnCheckedChangeListener((buttonView, isChecked) ->
                getEtPassword().setTransformationMethod(isChecked ? HideReturnsTransformationMethod
                        .getInstance() : PasswordTransformationMethod.getInstance()));
    }

    private EditText getEtAccount() {
        return findViewById(R.id.et_account);
    }

    private EditText getEtPassword() {
        return findViewById(R.id.et_password);
    }

    private CheckBox getCbPswVisibility() {
        return findViewById(R.id.cb_psw_visibility);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == ConstantUtil.REQUEST_CODE_FACE_RECOGNITION_LOGIN) {//人脸识别成功，返回登陆
                String idNumber = data.getStringExtra("idNumber");
                String name = data.getStringExtra("name");
                getEtAccount().setText(idNumber);
                showToast("欢迎您，" + name + "，登陆中");
                userLogin(1);
            }
        }
    }

    /**
     * 模拟登录
     */
    private void userLogin(int flag) {
        switch (flag) {
            case 0:
                if (getEtAccount().getText().toString().equals("510902199308287831") && getEtPassword().getText().toString().equals("123")) {
                    startActivity(new Intent(this, MainActivity.class));
                    SharedPrefsUtil.recordLoginState();
                    SharedPrefsUtil.recordAccount(getEtAccount().getText().toString());
                } else {
                    showToast("帐号密码错误");
                }
                break;
            case 1:
                startActivity(new Intent(this, MainActivity.class));
                SharedPrefsUtil.recordLoginState();
                break;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_delete:
                //TODO implement
                getEtAccount().setText("");
                break;
            case R.id.btn_login:
                //TODO implement
                userLogin(0);
                break;
            case R.id.btn_change_type:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Intent intent = new Intent(this, FaceCamera2Activity.class);
                    intent.putExtra("functionFlag", 0);
                    startActivityForResult(intent, ConstantUtil.REQUEST_CODE_FACE_RECOGNITION_LOGIN);
                } else {
                    Intent intent = new Intent(this, FaceCamera1Activity.class);
                    intent.putExtra("functionFlag", 0);
                    startActivityForResult(intent, ConstantUtil.REQUEST_CODE_FACE_RECOGNITION_LOGIN);
                }
                break;
        }
    }
}
