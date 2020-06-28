package com.das.face.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.baidu.aip.face.AipFace;
import com.das.face.R;
import com.das.face.util.ConstantUtil;
import com.das.face.util.FileUtils;
import com.das.face.util.SharedPrefsUtil;
import com.das.face.widgets.AutoFitTextureView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * created by jun on 2020/6/28
 * describe: 相机预览界面基于Camera2
 */
public class FaceCamera2Activity extends BaseActivity implements Handler.Callback {
    private static final String TAG = "FaceNewCamera2Activity";
    private AutoFitTextureView autoFitTextureView;
    private Animation animationDown, animationUp;
    private HandlerThread mCameraThread;
    private Handler mCameraHandler;
    //    private static String beforePath = ConstantUtil.FACE_PIC_PATH + "/image.jpg";
//    private static String afterPath = FileUtils.getItiDetailsPath() + "image.jpg";
    private int functionFlag;
    private String mCameraId;
    private static final int PREVIEW_WIDTH = 288;//预览的宽度
    private static final int PREVIEW_HEIGHT = 352;//预览的高度
    private static final int SAVE_WIDTH = 288;//保存图片的宽度
    private static final int SAVE_HEIGHT = 352;//保存图片的高度
    private CameraManager mCameraManager;//摄像头管理器，用于打开和关闭系统摄像头
    private ImageReader mImageReader;
    private CameraDevice mCameraDevice;//描述系统摄像头，类似于早期的Camera
    private CameraCaptureSession mCameraCaptureSession;//当需要拍照、预览等功能时，需要先创建该类的实例，然后通过该实例里的方法进行控制（例如：拍照 capture()）

    private CameraCharacteristics mCameraCharacteristics;//摄像头的各种特性，类似于Camera1中的CameraInfo

    private int mCameraSensorOrientation = 0;  //摄像头方向
    private int mDisplayRotation;//手机方向

    private boolean canTakePic = true; //是否可以拍照
    private boolean canExchangeCamera = false; //是否可以切换摄像头


    private Size mPreviewSize = new Size(PREVIEW_WIDTH, PREVIEW_HEIGHT); //预览大小
    private Size mSavePicSize = new Size(SAVE_WIDTH, SAVE_HEIGHT);  //保存图片大小
    private CaptureRequest.Builder captureRequestBuilder;
    private static AipFace client;
    private static final int MSG_OPEN_CAMERA = 5;
    public static final int MSG_TO_LOGIN = 6;
    private String fileImagePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera2);
        fileImagePath = FileUtils.getCacheDir(FaceCamera2Activity.this, "test").getAbsolutePath() + "/image.png";
        functionFlag = getIntent().getIntExtra("functionFlag", 0);
        client = new AipFace(ConstantUtil.BD_APP_ID, ConstantUtil.BD_API_KEY, ConstantUtil.BD_SECRET_KEY);
        autoFitTextureView = findViewById(R.id.TextureView);
        mDisplayRotation = getResources().getConfiguration().orientation;
        startCameraThread();
        autoFitTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
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

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            initCamera2Info();
            configureTransform(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            releaseCamera();
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }

    };

    @Override
    protected void onResume() {
        super.onResume();
        releaseCamera();
        if (autoFitTextureView.isAvailable()) {
            openCamera();
        } else {
            autoFitTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    public void onPause() {
        stopCameraThread();
        super.onPause();
    }

    /**
     * 初始化相机
     */
    private void initCamera2Info() {
        mCameraManager = (CameraManager) getApplicationContext().getSystemService(CAMERA_SERVICE);
        try {
            String[] cameraIdList = mCameraManager.getCameraIdList();
            if (cameraIdList.length == 0) {
                showToast("没有相机可用");
                return;
            }
            for (String id : cameraIdList) {
                CameraCharacteristics cameraCharacteristics = mCameraManager.getCameraCharacteristics(id);
                Integer facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                //默认使用后置摄像头
                int mCameraFacing = CameraCharacteristics.LENS_FACING_FRONT;
                if (facing != null && facing == mCameraFacing) {
                    mCameraId = id;
                    mCameraCharacteristics = cameraCharacteristics;
                }
            }
//            Integer supportLevel = mCameraCharacteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
//            if (supportLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
//                showToast("相机硬件不支持新特性");
//            }
            //获取相机的方向
            mCameraSensorOrientation = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            //获取StreamConfigurationMap，它是管理摄像头支持的所有输出格式和尺寸
            StreamConfigurationMap configurationMap = mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size[] savePicSize = configurationMap.getOutputSizes(ImageFormat.JPEG);          //保存照片尺寸
            Size[] previewSize = configurationMap.getOutputSizes(SurfaceTexture.class); //预览尺寸

            boolean exchange = exchangeWidthAndHeight(mDisplayRotation, mCameraSensorOrientation);
            mSavePicSize = getBestSize(exchange ? mSavePicSize.getHeight() : mSavePicSize.getWidth(),
                    exchange ? mSavePicSize.getWidth() : mSavePicSize.getHeight(), exchange ? mSavePicSize.getHeight() : mSavePicSize.getWidth(),
                    exchange ? mSavePicSize.getWidth() : mSavePicSize.getHeight(), Arrays.asList(savePicSize));

            mPreviewSize = getBestSize(exchange ? mPreviewSize.getHeight() : mPreviewSize.getWidth(),
                    exchange ? mPreviewSize.getWidth() : mPreviewSize.getHeight(), exchange ? autoFitTextureView.getHeight() : autoFitTextureView.getWidth(),
                    exchange ? autoFitTextureView.getWidth() : autoFitTextureView.getHeight(), Arrays.asList(previewSize));

            autoFitTextureView.getSurfaceTexture().setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            //根据预览的尺寸大小调整TextureView的大小，保证画面不被拉伸
            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                autoFitTextureView.setAspectRatio(
                        mPreviewSize.getWidth(), mPreviewSize.getHeight());
            } else {
                autoFitTextureView.setAspectRatio(
                        mPreviewSize.getHeight(), mPreviewSize.getWidth());
            }
            float s1 = mPreviewSize.getWidth();
            float s2 = mSavePicSize.getWidth();
            Log.e(TAG, "预览最优尺寸 ：" + mPreviewSize.getWidth() + "*" + mPreviewSize.getHeight() + "比例:" + s1 / mPreviewSize.getHeight());
            Log.e(TAG, "保存图片最优尺寸 ：" + mSavePicSize.getWidth() + "*" + mSavePicSize.getHeight() + "比例:" + s2 / mSavePicSize.getHeight());

            mImageReader = ImageReader.newInstance(mSavePicSize.getWidth(), mSavePicSize.getHeight(), ImageFormat.JPEG, 1);
            mImageReader.setOnImageAvailableListener(onImageAvailableListener, mCameraHandler);

            openCamera();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 打开相机
     */
    private void openCamera() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            showToast("没有相机权限！");
            return;
        }
        try {
            mCameraManager.openCamera(mCameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    mCameraDevice = camera;
                    createCaptureSession(camera);
                    mCameraHandler.sendEmptyMessage(MSG_OPEN_CAMERA);
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {

                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    showToast("打开相机失败" + error);
                }
            }, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 创建预览会话
     */
    private void createCaptureSession(CameraDevice cameraDevice) {

        try {
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);// 创建一个用于预览的Builder对象
            Surface surface = new Surface(autoFitTextureView.getSurfaceTexture());
            captureRequestBuilder.addTarget(surface);  // 将CaptureRequest的构建器与Surface对象绑定在一起
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);      // 闪光灯
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE); // 自动对焦
            // 为相机预览，创建一个CameraCaptureSession对象
            cameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    mCameraCaptureSession = session;
                    try {
                        //发起预览请求
                        mCameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), captureCallback, mCameraHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    showToast("开启预览会话失败");

                }
            }, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    private CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            canExchangeCamera = true;
            canTakePic = true;
        }

//        @Override
//        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
//            super.onCaptureFailed(session, request, failure);
//            showToast("开启预览失败");
//        }
    };
    private ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            mCameraHandler.post(new ImageSaver(reader.acquireNextImage()));
        }
    };

    /**
     * Saves a JPEG {@link Image} into the specified {@link File}.
     */
    private class ImageSaver implements Runnable {

        /**
         * The JPEG image
         */
        private final Image mImage;

        ImageSaver(Image image) {
            mImage = image;
        }

        @Override
        public void run() {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if (FileUtils.writeBitmap(new File(fileImagePath), bitmap)) {
                showToast("写入图片成功");
                mCameraHandler.sendEmptyMessage(MSG_TO_LOGIN);
            } else {
                showToast("写入图片失败");
            }
            mImage.close();
        }

    }

    /**
     * 根据提供的参数值返回与指定宽高相等或最接近的尺寸
     *
     * @param targetWidth  目标宽度
     * @param targetHeight 目标高度
     * @param maxWidth     最大宽度(即TextureView的宽度)
     * @param maxHeight    最大高度(即TextureView的高度)
     * @param sizeList     支持的Size列表 1280x720 640x480 352x288 320x240 176x144
     * @return 返回与指定宽高相等或最接近的尺寸
     */
    private Size getBestSize(int targetWidth, int targetHeight, int maxWidth, int maxHeight, List<Size> sizeList) {
        List<Size> bigEnough = new ArrayList<>();//比指定宽高大的Size列表
        List<Size> notBigEnough = new ArrayList<>();//比指定宽高小的Size列表
        for (Size size : sizeList) {
            //宽<=最大宽度  &&  高<=最大高度  &&  宽高比 == 目标值宽高比
            if (size.getWidth() <= maxWidth && size.getHeight() <= maxHeight
                    && size.getWidth() == size.getHeight() * targetWidth / targetHeight) {
                if (size.getWidth() >= targetWidth && size.getHeight() >= targetHeight) {
                    bigEnough.add(size);
                } else {
                    notBigEnough.add(size);
                }
                float s1 = size.getWidth();
                Log.e(TAG, "系统支持的尺寸: " + size.getWidth() + "*" + size.getHeight() + "比例:" + s1 / size.getHeight());
            }
        }
        float s2 = targetWidth;
        Log.e(TAG, "最大尺寸: " + maxWidth + "*" + maxHeight + "比例:" + s2 / targetHeight);
        Log.e(TAG, "目标尺寸: " + targetWidth + "*" + targetHeight + "比例:" + s2 / targetHeight);
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        }
        if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        }
        return sizeList.get(0);
    }


    /**
     * 根据提供的屏幕方向 [displayRotation] 和相机方向 [sensorOrientation] 返回是否需要交换宽高
     */
    private Boolean exchangeWidthAndHeight(int displayRotation, int sensorOrientation) {
        boolean exchange = false;
        switch (displayRotation) {
            case Surface.ROTATION_0:
            case Surface.ROTATION_180:
                if (sensorOrientation == 90 || sensorOrientation == 270) {
                    exchange = true;
                }
                break;
            case Surface.ROTATION_90:
            case Surface.ROTATION_270:
                if (sensorOrientation == 0 || sensorOrientation == 180) {
                    exchange = true;
                }
                break;
            default:
                Log.e(TAG, "Display rotation is invalid: " + displayRotation);
        }
//        if (displayRotation == Surface.ROTATION_0 || displayRotation == Surface.ROTATION_180) {
//            if (sensorOrientation == 90 || sensorOrientation == 270) {
//                exchange = true;
//            }
//        } else if (displayRotation == Surface.ROTATION_90 || displayRotation == Surface.ROTATION_270) {
//            if (sensorOrientation == 0 || sensorOrientation == 180) {
//                exchange = true;
//            }
//        } else {
//            Log.e(TAG, "Display rotation is invalid:" + displayRotation);
//        }
        Log.e(TAG, "屏幕方向:" + displayRotation);
        Log.e(TAG, "相机方向:" + sensorOrientation);
        return exchange;
    }

    /**
     * Configures the necessary {@link Matrix} transformation to `mTextureView`.
     * This method should be called after the camera preview size is determined in
     * setUpCameraOutputs and also the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = this;
        if (null == autoFitTextureView || null == mPreviewSize || null == activity) {
            return;
        }
        int rotation = this.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        autoFitTextureView.setTransform(matrix);
    }

    private static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size size1, Size size2) {
            long s1 = size1.getWidth();
            long s2 = size2.getWidth();
            return Long.signum(s1 * size1.getHeight() - s2 * size2.getHeight());
        }

        @Override
        public boolean equals(Object obj) {
            return false;
        }
    }

    /**
     * 拍照
     */
    private void takePic() {
        if (mCameraDevice == null || !autoFitTextureView.isAvailable() || !canTakePic) {
            return;
        }
        try {
            // 创建一个拍照请求的Builder对象
            CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());
            // Use the same AE and AF modes as the preview.
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);// 自动对焦
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);// 闪光灯
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, mCameraSensorOrientation);//根据摄像头方向对保存的照片进行旋转，使其为"自然方向"
            CameraCaptureSession.CaptureCallback CaptureCallback
                    = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
//                    showToast("Saved: " + beforePath);
                    Log.e(TAG, fileImagePath);
                }
            };
            mCameraCaptureSession.stopRepeating();
            mCameraCaptureSession.abortCaptures();
            mCameraCaptureSession.capture(captureBuilder.build(), CaptureCallback, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
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
     * Closes the current {@link CameraDevice}.
     */
    private void releaseCamera() {
        if (null != mCameraCaptureSession) {
            mCameraCaptureSession.close();
            mCameraCaptureSession = null;
        }
        if (null != mCameraDevice) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if (null != mImageReader) {
            mImageReader.close();
            mImageReader = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopCameraThread();
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_OPEN_CAMERA://打开相机采集图片
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        takePic();
                    }
                }, 2000);
                break;
            case MSG_TO_LOGIN:
                String image = bitmap2String();
                if (image.startsWith("data:"))
                    image = image.split(",")[1];
                final String idNumber = SharedPrefsUtil.getAccount("user");
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
//                            // 传入可选参数调用接口
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
                                    //播放欢迎语
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
                                    } else {
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
                    case 4://删除
                        new Thread(() -> {
                            HashMap<String, String> map = new HashMap<>();
                            //人脸搜索 M:N 识别
                            JSONObject res = client.faceDelete(idNumber, ConstantUtil.BD_GROUP_ID, finalImage, null);
                            try {
                                int errorCode = res.getInt("error_code");
                                if (errorCode == 0) {
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


        }
        return false;
    }

}