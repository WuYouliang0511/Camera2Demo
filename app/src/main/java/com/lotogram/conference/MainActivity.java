package com.lotogram.conference;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.lotogram.conference.databinding.ActivityMainBinding;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener {

    private static final String TAG = "Conference";

    private ActivityMainBinding mBinding;
    private CameraManager mManager;

    private CameraDevice mCameraDevice;

    private SurfaceTexture mSurfaceTexture;
    private ImageReader mImageReader;
    private CameraCaptureSession mCameraCaptureSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        mBinding.textureView.setSurfaceTextureListener(this);
        mManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
        Log.d(TAG, "屏幕  宽: " + metrics.widthPixels + "  高: " + metrics.heightPixels);

        int displayRotation = getWindowManager().getDefaultDisplay().getOrientation();
        Log.d(TAG, "当前手机方向: " + displayRotation);

        try {
            String[] ids = mManager.getCameraIdList();
            Log.d(TAG, "可用Camera数量: " + ids.length);

            for (String id : ids) {
                Log.d(TAG, "----------------------------------------");
                Log.d(TAG, "Camera名称: " + id);
                CameraCharacteristics characteristics = mManager.getCameraCharacteristics(id);
                Integer deviceLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                if (deviceLevel == null) return;
                //各个等级从支持的功能多少排序为: LEGACY < LIMITED < FULL < LEVEL_3
                switch (deviceLevel) {
                    case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY:
                        Log.d(TAG, "hardware supported level:LEVEL_LEGACY");
                        Log.d(TAG, "向后兼容模式, 如果是此等级, 基本没有额外功能, HAL层大概率就是HAL1");
                        break;
                    case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED:
                        Log.d(TAG, "hardware supported level:LEVEL_LIMITED");
                        Log.d(TAG, "有最基本的功能, 还支持一些额外的高级功能, 这些高级功能是LEVEL_FULL的子集");
                        break;
                    case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL:
                        Log.d(TAG, "hardware supported level:LEVEL_FULL");
                        Log.d(TAG, "支持对每一帧数据进行控制,还支持高速率的图片拍摄");
                        break;
                    case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3:
                        Log.d(TAG, "hardware supported level:LEVEL_3");
                        Log.d(TAG, "支持YUV后处理和Raw格式图片拍摄, 还支持额外的输出流配置");
                        break;
                }

                Integer lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
                switch (lensFacing) {
                    case CameraCharacteristics.LENS_FACING_BACK:
                        Log.d(TAG, "后置摄像头");
                        break;
                    case CameraCharacteristics.LENS_FACING_FRONT:
                        Log.d(TAG, "前置摄像头");
                        break;
                    case CameraCharacteristics.LENS_FACING_EXTERNAL:
                        Log.d(TAG, "外接摄像头");
                        break;
                }

                Boolean flashInfo = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                Log.d(TAG, flashInfo ? "支持闪光灯" : "不支持闪光灯");

                Integer sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                Log.d(TAG, "摄像头方向: " + sensorOrientation);

                Integer maxFaceCount = characteristics.get(CameraCharacteristics.STATISTICS_INFO_MAX_FACE_COUNT);
                Log.d(TAG, "同时检测到人脸的数量: " + maxFaceCount);

                float maxZoom = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
                Log.d(TAG, "最大放大倍数: " + maxZoom);

                Rect rect = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
                Log.d(TAG, "未缩放的正常预览画面大小: " + rect);

//                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
//                Size[] saveSize = map.getOutputSizes(ImageFormat.JPEG);
//                for (Size size : saveSize) {
//                    Log.d(TAG, "保存尺寸 宽: " + size.getWidth() + "  高: " + size.getHeight());
//                }
//
//                Size[] previewSize = map.getOutputSizes(SurfaceTexture.class);
//                for (Size size : previewSize) {
//                    Log.d(TAG, "预览尺寸 宽: " + size.getWidth() + "  高: " + size.getHeight());
//                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "width: " + width);
        Log.d(TAG, "height: " + height);
        mSurfaceTexture = surface;
        setupImageReader(width, height);
        openCamera();
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

    }

    private void openCamera() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        try {

            mManager.openCamera("0", new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    Log.d(TAG, "打开相机成功");
                    mCameraDevice = camera;

                    try {
                        mCameraDevice.createCaptureSession(Arrays.asList(mImageReader.getSurface(), new Surface(mSurfaceTexture)), new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(@NonNull CameraCaptureSession session) {
                                Log.d(TAG, "相机会话配置成功");
                                mCameraCaptureSession = session;
                                try {
                                    CaptureRequest.Builder builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

//                                    builder.addTarget(mImageReader.getSurface());
                                    builder.addTarget(new Surface(mSurfaceTexture));

                                    mCameraCaptureSession.setRepeatingRequest(builder.build(), null, null);
                                } catch (CameraAccessException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                                Log.d(TAG, "相机会话配置失败");

                            }
                        }, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    Log.d(TAG, "相机失去连接");
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    Log.d(TAG, "打开相机错误");
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void takePicture(View view) {

        try {
            CaptureRequest.Builder builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);

            builder.addTarget(mImageReader.getSurface());
            // 自动对焦
            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            // 自动曝光
            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            // 获取手机方向
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            // 根据设备方向计算设置照片的方向
            builder.set(CaptureRequest.JPEG_ORIENTATION, 270);

            mCameraCaptureSession.capture(builder.build(), null, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    ///为了使照片竖直显示
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private void setupImageReader(int width, int height) {
        mImageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 2);
        mImageReader.setOnImageAvailableListener(reader -> {
            Log.d(TAG, "获得照片");
            Image image = reader.acquireNextImage();
            Image.Plane[] planes = image.getPlanes();
            Log.d(TAG, "size: " + planes.length);
            ByteBuffer buffer = planes[0].getBuffer();
            int a = buffer.remaining();
            byte[] b = new byte[a];
            buffer.get(b);
            image.close();

            Bitmap bitmap = BitmapFactory.decodeByteArray(b, 0, a);
            mBinding.imageView.setImageBitmap(bitmap);
        }, null);
    }
}