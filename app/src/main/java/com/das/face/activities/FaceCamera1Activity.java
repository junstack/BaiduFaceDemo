package com.das.face.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.core.content.ContextCompat;

import com.baidu.aip.face.AipFace;
import com.das.face.R;
import com.das.face.util.ConstantUtil;
import com.das.face.util.FileUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * created by jun on 2020/6/28
 * describe: 相机预览界面基于Camera1
 */
public class FaceCamera1Activity extends BaseActivity implements Handler.Callback {

    private static final int MSG_OPEN_CAMERA = 1;
    private static final int MSG_CLOSE_CAMERA = 2;
    private static final int MSG_SET_PREVIEW_SIZE = 3;
    private static final int MSG_SET_PREVIEW_SURFACE = 4;
    private static final int MSG_START_PREVIEW = 5;
    private static final int MSG_STOP_PREVIEW = 6;
    private static final int MSG_SET_PICTURE_SIZE = 7;
    private static final int MSG_TAKE_PICTURE = 8;
    public static final int MSG_TO_LOGIN = 9;
    private static final int REQUEST_PERMISSIONS_CODE = 1;
    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static final int PREVIEW_FORMAT = ImageFormat.JPEG;
    private static final String TAG = "FaceCameraTest";
    private HandlerThread mCameraThread;
    private Handler mCameraHandler;
    @Nullable
    private Camera.CameraInfo mFrontCameraInfo = null;//前置摄像头信息
    private int mFrontCameraId = -1;//前置摄像头id

    @Nullable
    private Camera mCamera;
    private Camera.CameraInfo mCameraInfo;

    @Nullable
    private SurfaceHolder mPreviewSurface;
    private int mPreviewSurfaceWidth;
    private int mPreviewSurfaceHeight;

    @Nullable
    private DeviceOrientationListener mDeviceOrientationListener;

//    private static String beforePath = ConstantUtil.FACE_PIC_PATH + "/image.jpg";
//    private static String afterPath = FileUtils.getItiDetailsPath() + "image.jpg";

    private Animation animationDown, animationUp;

    private static AipFace client = new AipFace(ConstantUtil.BD_APP_ID, ConstantUtil.BD_API_KEY, ConstantUtil.BD_SECRET_KEY);
    /**
     * 百度接口类型标志
     */
    private int functionFlag;
    private String fileImagePath;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera1);
        fileImagePath = FileUtils.getCacheDir(FaceCamera1Activity.this, "test").getAbsolutePath() + "/image.png";
        functionFlag = getIntent().getIntExtra("functionFlag", 0);
        mDeviceOrientationListener = new DeviceOrientationListener(this);
        startCameraThread();
        initCameraInfo();
        SurfaceView cameraPreview = findViewById(R.id.surfaceView);
        cameraPreview.getHolder().addCallback(new PreviewSurfaceCallback());
        ImageView iv_anim = findViewById(R.id.iv_anim);
        animationDown = AnimationUtils.loadAnimation(this, R.anim.anim_translate_down);
        animationUp = AnimationUtils.loadAnimation(this, R.anim.anim_translate_up);
        iv_anim.startAnimation(animationDown);
        animationDown.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                iv_anim.startAnimation(animationUp);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        animationUp.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                iv_anim.startAnimation(animationDown);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        DeviceOrientationListener deviceOrientationListener = mDeviceOrientationListener;
        if (deviceOrientationListener != null) {
            deviceOrientationListener.enable();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 动态权限检查
        if (!isRequiredPermissionsGranted() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_PERMISSIONS_CODE);
        } else if (mCameraHandler != null) {
            mCameraHandler.obtainMessage(MSG_OPEN_CAMERA, getCameraId(), 0).sendToTarget();
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_OPEN_CAMERA: {//打开相机采集图片
                openCamera();
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        mCamera.takePicture(null, null, (data, camera1) -> {
                            File tempFile = new File(fileImagePath);
                            FileOutputStream fos = null;
                            try {
                                fos = new FileOutputStream(tempFile);
                                fos.write(data);
                                Bitmap bitmap = BitmapFactory.decodeFile(fileImagePath);
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos); // Bitmap.CompressFormat format 图像的压缩格式；int quality 图像压缩率，0-100。 0 压缩100%，100意味着不压缩；OutputStream stream 写入压缩数据的输出流；
                                fos.flush();//仅仅是刷新缓冲区(一般写字符时要用,因为字符是先进入的缓冲区)，流对象还可以继续使用
                                mCameraHandler.sendEmptyMessage(MSG_TO_LOGIN);
                            } catch (IOException e) {
                                e.printStackTrace();
                            } finally {
                                if (fos != null) {
                                    try {
                                        fos.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                                //重新开始预览
                                mCameraHandler.sendEmptyMessage(MSG_STOP_PREVIEW);
                            }
                        });
                    }
                }, 2000);
                break;
            }
            case MSG_CLOSE_CAMERA: //关闭相机
                closeCamera();
                break;

            case MSG_SET_PREVIEW_SIZE: //设置预览的大小
                int shortSide = msg.arg1;
                int longSide = msg.arg2;
                setPreviewSize(shortSide, longSide);
                break;

            case MSG_SET_PREVIEW_SURFACE:
                SurfaceHolder previewSurface = (SurfaceHolder) msg.obj;
                setPreviewSurface(previewSurface);
                break;

            case MSG_START_PREVIEW:
                startPreview();
                break;

            case MSG_STOP_PREVIEW:
                stopPreview();
                break;

            case MSG_SET_PICTURE_SIZE:
                int shortSidePicture = msg.arg1;
                int longSidePicture = msg.arg2;
                setPictureSize(shortSidePicture, longSidePicture);
                break;

            case MSG_TAKE_PICTURE:
                takePicture();
            case MSG_TO_LOGIN:
                String image = bitmap2String();
                if (image.startsWith("data:"))
                    image = image.split(",")[1];
                final String idNumber = "510902199308287831";
                final String name = "测试";
                String finalImage = image;
                switch (functionFlag) {
                    case 0://登陆
                        new Thread(() -> {
                            HashMap<String, String> map = new HashMap<>();
                            map.put("max_face_num", "1");
                            map.put("match_threshold", "70");
                            map.put("quality_control", "NORMAL");
                            map.put("liveness_control", "LOW");
                            map.put("max_user_num", "1");
                            //人脸搜索 M:N 识别
                            JSONObject res = client.search(finalImage, "BASE64", ConstantUtil.BD_GROUP_ID, map);
                            try {
                                Log.e("搜索结果", res.toString());
                                int errorCode = res.getInt("error_code");
                                if (errorCode == 0) {
                                    String result = res.getString("result");
                                    res = new JSONObject(result);
                                    String user_list = res.getString("user_list");
                                    JSONArray jsonArray = new JSONArray(user_list);
                                    if (jsonArray.length() == 1) {
                                        res = jsonArray.getJSONObject(0);
                                        String user = res.getString("user_id");
                                        String username = res.getString("user_info");
                                        Intent intent = new Intent();
                                        intent.putExtra("idNumber", user);
                                        intent.putExtra("name", username);
                                        setResult(RESULT_OK, intent);
                                    }

                                } else {
                                    showToast("未注册人脸");
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                                showToast("识别失败");
                            } finally {
                                finish();
                            }
                        }).start();
                        break;
                    case 1://注册
                        if (TextUtils.isEmpty(idNumber) || TextUtils.isEmpty(name)) {
                            showToast("您还未登陆");
                            finish();
                            break;
                        }
                        new Thread(() -> {
                            // 传入可选参数调用接口
                            HashMap<String, String> options = new HashMap<>();
                            options.put("user_info", name);
                            options.put("user_id", idNumber);
                            options.put("quality_control", "NORMAL");
                            options.put("liveness_control", "LOW");
                            options.put("action_type", "REPLACE");

                            // 人脸注册
                            JSONObject res = client.addUser(finalImage, "BASE64", ConstantUtil.BD_GROUP_ID, idNumber, options);
                            try {
                                int errorCode = res.getInt("error_code");
                                if (errorCode == 0) {
                                    showToast("注册成功");
                                } else {
                                    showToast("注册失败，请重试");
                                }
                                finish();
                            } catch (JSONException e) {
                                e.printStackTrace();
                                showToast("识别失败");
                            } finally {
                                finish();
                            }
                        }).start();
                        break;
                    case 2://体验刷脸
                        new Thread(() -> {
                            HashMap<String, String> map = new HashMap<>();
                            map.put("max_face_num", "1");
                            map.put("match_threshold", "70");
                            map.put("quality_control", "NORMAL");
                            map.put("liveness_control", "LOW");
                            map.put("max_user_num", "1");
                            map.put("user_id", idNumber);
                            //人脸搜索 M:N 识别
                            JSONObject res = client.search(finalImage, "BASE64", ConstantUtil.BD_GROUP_ID, map);
                            try {
                                int errorCode = res.getInt("error_code");
                                if (errorCode == 0) {
                                    showToast("确认过眼神，你是对的人");
                                } else {
                                    showToast("识别失败");
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                                showToast("识别失败");
                            } finally {
                                finish();
                            }
                        }).start();
                        break;
                    case 3://重新上传
                        new Thread(() -> {
                            HashMap<String, String> map = new HashMap<>();
                            map.put("max_face_num", "1");
                            map.put("match_threshold", "70");
                            map.put("quality_control", "NORMAL");
                            map.put("liveness_control", "LOW");
                            map.put("max_user_num", "1");
                            map.put("user_id", idNumber);
                            //人脸搜索 M:N 识别
                            JSONObject res = client.search(finalImage, "BASE64", ConstantUtil.BD_GROUP_ID, map);
                            try {
                                int errorCode = res.getInt("error_code");
                                if (errorCode == 0) {//识别成功，再次进入，重新注册
                                    showToast("验证成功，请重新注册");
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                        Intent intentFace = new Intent(this, FaceCamera2Activity.class);
                                        intentFace.putExtra("functionFlag", 1);
                                        startActivity(intentFace);
                                    }else {
                                        Intent intentFace = new Intent(this, FaceCamera1Activity.class);
                                        intentFace.putExtra("functionFlag", 1);
                                        startActivity(intentFace);
                                    }
                                } else {
                                    showToast("识别失败");
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                                showToast("识别失败");
                            } finally {
                                finish();
                            }
                        }).start();
                        break;
                }
                break;
            default:
                throw new IllegalArgumentException("Illegal message: " + msg.what);
        }
        return false;
    }

    /**
     * bitmap转base64
     *
     * @return base64编码图片
     */
    public String bitmap2String() {
        File file = new File(fileImagePath);
        long size = file.length();
        size /= 1024;
        Bitmap bitmap = BitmapFactory.decodeFile(fileImagePath);
        int quality = size < 100 ? 100 : 65;
        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, bStream);
        byte[] bytes = bStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }


    /**
     * 开起线程
     */
    private void startCameraThread() {
        mCameraThread = new HandlerThread("CameraThread");
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper(), this);
    }

    private void stopCameraThread() {
        if (mCameraThread != null) {
            mCameraThread.quitSafely();
        }
        mCameraThread = null;
        mCameraHandler = null;
        animationDown.cancel();
        animationUp.cancel();
        functionFlag = -1;
    }

    /**
     * 判断指定的预览格式是否支持。
     */
    private boolean isPreviewFormatSupported(Camera.Parameters parameters, int format) {
        List<Integer> supportedPreviewFormats = parameters.getSupportedPreviewFormats();
        return supportedPreviewFormats != null && supportedPreviewFormats.contains(format);
    }

    /**
     * 根据指定的尺寸要求设置预览尺寸，我们会同时考虑指定尺寸的比例和大小。
     *
     * @param shortSide 短边长度
     * @param longSide  长边长度
     */
    @WorkerThread
    private void setPreviewSize(int shortSide, int longSide) {
        Camera camera = mCamera;
        if (camera != null && shortSide != 0 && longSide != 0) {
            float aspectRatio = (float) longSide / shortSide;
            Camera.Parameters parameters = camera.getParameters();
            List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
            for (Camera.Size previewSize : supportedPreviewSizes) {
                if ((float) previewSize.width / previewSize.height == aspectRatio && previewSize.height <= shortSide && previewSize.width <= longSide) {
                    parameters.setPreviewSize(previewSize.width, previewSize.height);
                    Log.d(TAG, "setPreviewSize() called with: width = " + previewSize.width + "; height = " + previewSize.height);

                    if (isPreviewFormatSupported(parameters, PREVIEW_FORMAT)) {
                        parameters.setPreviewFormat(PREVIEW_FORMAT);
                        int frameWidth = previewSize.width;
                        int frameHeight = previewSize.height;
                        int previewFormat = parameters.getPreviewFormat();
                        PixelFormat pixelFormat = new PixelFormat();
                        PixelFormat.getPixelFormatInfo(previewFormat, pixelFormat);
                        int bufferSize = (frameWidth * frameHeight * pixelFormat.bitsPerPixel) / 8;
                        camera.addCallbackBuffer(new byte[bufferSize]);
                        camera.addCallbackBuffer(new byte[bufferSize]);
                        camera.addCallbackBuffer(new byte[bufferSize]);
                        Log.d(TAG, "Add three callback buffers with size: " + bufferSize);
                    }

                    camera.setParameters(parameters);
                    break;
                }
            }
        }
    }

    /**
     * 根据指定的尺寸要求设置照片尺寸，我们会考虑指定尺寸的比例，并且去符合比例的最大尺寸作为照片尺寸。
     *
     * @param shortSide 短边长度
     * @param longSide  长边长度
     */
    @WorkerThread
    private void setPictureSize(int shortSide, int longSide) {
        Camera camera = mCamera;
        if (camera != null && shortSide != 0 && longSide != 0) {
            float aspectRatio = (float) longSide / shortSide;
            Camera.Parameters parameters = camera.getParameters();
            List<Camera.Size> supportedPictureSizes = parameters.getSupportedPictureSizes();
            for (Camera.Size pictureSize : supportedPictureSizes) {
                if ((float) pictureSize.width / pictureSize.height == aspectRatio) {
                    parameters.setPictureSize(pictureSize.width, pictureSize.height);
                    camera.setParameters(parameters);
                    Log.d(TAG, "setPictureSize() called with: width = " + pictureSize.width + "; height = " + pictureSize.height);
                    break;
                }
            }
        }
    }

    /**
     * 初始化摄像头信息。
     */
    private void initCameraInfo() {
        int numberOfCameras = Camera.getNumberOfCameras();// 获取摄像头个数
        for (int cameraId = 0; cameraId < numberOfCameras; cameraId++) {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(cameraId, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                // 前置摄像头信息
                mFrontCameraId = cameraId;
                mFrontCameraInfo = cameraInfo;
            }
        }
    }

    /**
     * 开启指定摄像头
     */
    @WorkerThread
    private void openCamera() {
        Camera camera = mCamera;
        if (camera != null) {
            throw new RuntimeException("相机已经被开启，无法同时开启多个相机实例！");
        }
        //检查是否给相机权限，其中PackageManager.PERMISSION_GRANTED=0表示授权
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
            mCameraInfo = mFrontCameraInfo;
            assert mCamera != null;
            mCamera.setDisplayOrientation(getCameraDisplayOrientation(mCameraInfo));
        }
    }

    /**
     * 获取预览画面要校正的角度。
     */
    private int getCameraDisplayOrientation(Camera.CameraInfo cameraInfo) {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (cameraInfo.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (cameraInfo.orientation - degrees + 360) % 360;
        }
        return result;
    }

    /**
     * 获取要开启的相机 ID，优先开启前置。
     */
    private int getCameraId() {
        if (hasFrontCamera()) {
            return mFrontCameraId;
        } else {
            throw new RuntimeException("No available camera id found.");
        }
    }


    /**
     * 判断我们需要的权限是否被授予，只要有一个没有授权，我们都会返回 false。
     *
     * @return true 权限都被授权
     */
    private boolean isRequiredPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }

    /**
     * 设置预览 Surface。
     */
    @WorkerThread
    private void setPreviewSurface(@Nullable SurfaceHolder previewSurface) {
        Camera camera = mCamera;
        if (camera != null && previewSurface != null) {
            try {
                camera.setPreviewDisplay(previewSurface);
                Log.d(TAG, "setPreviewSurface() called");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 开始预览。
     */
    @WorkerThread
    private void startPreview() {
        Camera camera = mCamera;
        SurfaceHolder previewSurface = mPreviewSurface;
        if (camera != null && previewSurface != null) {
            camera.setPreviewCallbackWithBuffer(new PreviewCallback());
            camera.startPreview();
            Log.d(TAG, "startPreview() called");
        }
    }

    /**
     * 拍照。
     */
    @WorkerThread
    private void takePicture() {
        Camera camera = mCamera;
        if (camera != null) {
            Camera.Parameters parameters = camera.getParameters();
            camera.setParameters(parameters);
            camera.takePicture(new ShutterCallback(), new RawCallback(), new PostviewCallback(), new JpegCallback());
        }
    }

    /**
     * 停止预览。
     */
    @WorkerThread
    private void stopPreview() {
        Camera camera = mCamera;
        if (camera != null) {
            camera.stopPreview();
            Log.d(TAG, "stopPreview() called");
        }
    }

    /**
     * 判断是否有前置摄像头。
     *
     * @return true 代表有前置摄像头
     */
    private boolean hasFrontCamera() {
        return mFrontCameraId != -1;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            Window window = getWindow();
            View decorView = window.getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCameraHandler != null) {
            mCameraHandler.removeMessages(MSG_OPEN_CAMERA);
            mCameraHandler.sendEmptyMessage(MSG_CLOSE_CAMERA);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        DeviceOrientationListener deviceOrientationListener = mDeviceOrientationListener;
        if (deviceOrientationListener != null) {
            deviceOrientationListener.disable();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopCameraThread();
    }

    /**
     * 关闭相机。
     */
    @WorkerThread
    private void closeCamera() {
        Camera camera = mCamera;
        mCamera = null;
        if (camera != null) {
            camera.release();
            mCameraInfo = null;
        }
    }

    private class PreviewCallback implements Camera.PreviewCallback {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            // 在使用完 Buffer 之后记得回收复用。
            camera.addCallbackBuffer(data);
        }
    }

    private class DeviceOrientationListener extends OrientationEventListener {

        private DeviceOrientationListener(Context context) {
            super(context);
        }

        @Override
        public void onOrientationChanged(int orientation) {

        }
    }

    private class ShutterCallback implements Camera.ShutterCallback {
        @Override
        public void onShutter() {
            Log.d(TAG, "onShutter() called");
        }
    }

    private class RawCallback implements Camera.PictureCallback {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Log.d(TAG, "On raw taken.");
        }
    }

    private class PostviewCallback implements Camera.PictureCallback {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Log.d(TAG, "On postview taken.");
        }
    }

    private class JpegCallback implements Camera.PictureCallback {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Log.d(TAG, "On jpeg taken.");
        }
    }

    private class PreviewSurfaceCallback implements SurfaceHolder.Callback {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {

        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            mPreviewSurface = holder;
            mPreviewSurfaceWidth = width;
            mPreviewSurfaceHeight = height;
            Handler cameraHandler = mCameraHandler;
            if (cameraHandler != null) {
                cameraHandler.obtainMessage(MSG_SET_PREVIEW_SIZE, width, height).sendToTarget();
                cameraHandler.obtainMessage(MSG_SET_PICTURE_SIZE, width, height).sendToTarget();
                cameraHandler.obtainMessage(MSG_SET_PREVIEW_SURFACE, holder).sendToTarget();
                cameraHandler.sendEmptyMessage(MSG_START_PREVIEW);
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            mPreviewSurface = null;
            mPreviewSurfaceWidth = 0;
            mPreviewSurfaceHeight = 0;
        }
    }

}
