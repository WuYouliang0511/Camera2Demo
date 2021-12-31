package com.lotogram.conference;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.Log;
import android.util.Range;
import android.util.Size;

import androidx.annotation.NonNull;

import java.util.List;

public class Camera2Helper {

    private static final String TAG = "Camera2Helper";

    public static void showCameraInfo(@NonNull Context context, String cameraId) throws CameraAccessException {
        CameraManager mManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        Log.d(TAG, "----------------------------------------");
        Log.d(TAG, "Camera名称: " + cameraId);
        CameraCharacteristics characteristics = mManager.getCameraCharacteristics(cameraId);

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

        int orientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        Log.d(TAG, "orientation: " + orientation);

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

        StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        Size[] saveSize = map.getOutputSizes(ImageFormat.JPEG);
        for (Size size : saveSize) {
            Log.d(TAG, "保存尺寸 宽: " + size.getWidth() + "  高: " + size.getHeight());
        }

        Size[] previewSize = map.getOutputSizes(SurfaceTexture.class);
        for (Size size : previewSize) {
            Log.d(TAG, "预览尺寸 宽: " + size.getWidth() + "  高: " + size.getHeight());
        }

        // 该相机的FPS范围
        Range<Integer>[] fpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
        for (Range<Integer> fps : fpsRanges) {
            Log.d(TAG, "Down: " + fps.getLower() + "   UP: " + fps.getUpper());
        }


        List<CaptureRequest.Key<?>> list = characteristics.getAvailableCaptureRequestKeys();
        for (CaptureRequest.Key<?> key : list) {


            Log.d(TAG, "key: "+key.getName());
        }


    }
}
