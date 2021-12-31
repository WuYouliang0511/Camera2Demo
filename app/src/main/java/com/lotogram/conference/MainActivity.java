package com.lotogram.conference;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.lotogram.conference.databinding.ActivityMainBinding;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener {

    private static final String TAG = "MainActivity";

    private ActivityMainBinding mBinding;
    private CameraManager mManager;
    private CameraDevice mCameraDevice;
    private SurfaceTexture mSurfaceTexture;
    private ImageReader mImageReader;
    private CameraCaptureSession mCameraCaptureSession;

    int cout;

    HandlerThread thread;

    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        mBinding.textureView.setSurfaceTextureListener(this);
        mManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

//        Log.d(TAG, "SDK_INT: " + Build.VERSION.SDK_INT);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
//            intent.setData(Uri.parse("package:" + getPackageName()));
//            startActivityForResult(intent, 9000);
//            Log.d(TAG, "管理文件权限");
//        }

        thread = new HandlerThread("Camera2-Thread");
        thread.start();
        handler = new Handler(thread.getLooper());

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
        Log.d(TAG, "屏幕  宽: " + metrics.widthPixels + "  高: " + metrics.heightPixels);

        int displayRotation = getWindowManager().getDefaultDisplay().getOrientation();
        Log.d(TAG, "当前手机方向: " + displayRotation);

        try {
            String[] ids = mManager.getCameraIdList();
            Log.d(TAG, "可用Camera数量: " + ids.length);
//            for (String id : ids) {
//
//            }
            Camera2Helper.showCameraInfo(this, "0");
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }

    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "width: " + width);
        Log.d(TAG, "height: " + height);
        mSurfaceTexture = surface;
        mSurfaceTexture.setDefaultBufferSize(height, width);
        openCamera(width, height);
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
        openCamera(width, height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

    }

    private void openCamera(int width, int height) {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        try {

            setUpCameraOutputs(width, height);


            setupImageReader(height, width);

//            CameraCharacteristics characteristics
//                    = mManager.getCameraCharacteristics("0");
//            StreamConfigurationMap map = characteristics.get(
//                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
//
//            Size largest = Collections.max(
//                    Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
//                    new CompareSizesByArea());
//            setupImageReader(largest.getWidth(), largest.getWidth());
//
//            // Find out if we need to swap dimension to get the preview size relative to sensor
//            // coordinate.
//            int displayRotation = getWindowManager().getDefaultDisplay().getRotation();
//            mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
//
//            boolean swappedDimensions = false;
//
//            switch (displayRotation) {
//                case Surface.ROTATION_0:
//                case Surface.ROTATION_180:
//                    if (mSensorOrientation == 90 || mSensorOrientation == 270) {
//                        swappedDimensions = true;
//                    }
//                    break;
//                case Surface.ROTATION_90:
//                case Surface.ROTATION_270:
//                    if (mSensorOrientation == 0 || mSensorOrientation == 180) {
//                        swappedDimensions = true;
//                    }
//                    break;
//                default:
//                    Log.e(TAG, "Display rotation is invalid: " + displayRotation);
//            }
//
//            Point displaySize = new Point();
//            getWindowManager().getDefaultDisplay().getSize(displaySize);
//            int maxPreviewWidth = displaySize.x;
//            int maxPreviewHeight = displaySize.y;
//            Log.d(TAG, "rotatedPreviewWidth: " + width);
//            Log.d(TAG, "rotatedPreviewHeight: " + height);
//            Log.d(TAG, "maxPreviewWidth: " + maxPreviewWidth);
//            Log.d(TAG, "maxPreviewHeight: " + maxPreviewHeight);
//
//            // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
//            // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
//            // garbage capture data.
//            mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
//                    width, height, maxPreviewWidth,
//                    maxPreviewHeight, largest);
//
//            int orientation = getResources().getConfiguration().orientation;
//            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
//                mBinding.textureView.setAspectRatio(
//                        mPreviewSize.getWidth(), mPreviewSize.getHeight());
//            } else {
//                mBinding.textureView.setAspectRatio(
//                        mPreviewSize.getHeight(), mPreviewSize.getWidth());
//            }
//            configureTransform(width, height);
            mManager.openCamera("0", cameraDeviceStateCallback, handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private final CameraDevice.StateCallback cameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.d(TAG, "打开相机成功");
            mCameraDevice = camera;
            List<Surface> outputs = new ArrayList<>();
            outputs.add(mImageReader.getSurface());
            outputs.add(new Surface(mSurfaceTexture));

            try {
                mCameraDevice.createCaptureSession(outputs, cameraCaptureSessionStateCallback, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.d(TAG, "相机失去连接");
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.d(TAG, "打开相机错误");
            camera.close();
            mCameraDevice = null;
        }
    };

    private final CameraCaptureSession.StateCallback cameraCaptureSessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            Log.d(TAG, "相机会话配置成功");
            mCameraCaptureSession = session;
            try {
                CaptureRequest.Builder builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                // 自动对焦
                builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                // 自动曝光
                builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                // fps
                builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range.create(60, 60));

                builder.set(CaptureRequest.SCALER_CROP_REGION, new Rect(0, 0, 2000, 3000));
                builder.addTarget(new Surface(mSurfaceTexture));
//                builder.addTarget(mImageReader.getSurface());
                mCameraCaptureSession.setRepeatingRequest(builder.build(), new CameraCaptureSession.CaptureCallback() {
                    @Override
                    public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                        super.onCaptureStarted(session, request, timestamp, frameNumber);
                        Log.d(TAG, "onCaptureStarted: "+frameNumber);
                    }

                    @Override
                    public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
                        super.onCaptureProgressed(session, request, partialResult);
                    }

                    @Override
                    public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                        super.onCaptureCompleted(session, request, result);
                    }

                    @Override
                    public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
                        super.onCaptureFailed(session, request, failure);
                    }

                    @Override
                    public void onCaptureSequenceCompleted(@NonNull CameraCaptureSession session, int sequenceId, long frameNumber) {
                        super.onCaptureSequenceCompleted(session, sequenceId, frameNumber);
                    }

                    @Override
                    public void onCaptureSequenceAborted(@NonNull CameraCaptureSession session, int sequenceId) {
                        super.onCaptureSequenceAborted(session, sequenceId);
                    }

                    @Override
                    public void onCaptureBufferLost(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull Surface target, long frameNumber) {
                        super.onCaptureBufferLost(session, request, target, frameNumber);
                    }
                }, handler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            Log.d(TAG, "相机会话配置失败");

        }
    };

    public void takePicture(View view) {
        try {
            CaptureRequest.Builder builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            builder.addTarget(mImageReader.getSurface());
            // 自动对焦
            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            // 自动曝光
            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
            builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
            builder.set(CaptureRequest.SCALER_CROP_REGION, new Rect(0, 0, 2000, 3000));
            // 获取手机方向
            //int rotation = getWindowManager().getDefaultDisplay().getRotation();
            // 根据设备方向计算设置照片的方向
//            builder.set(CaptureRequest.JPEG_ORIENTATION, 180);

            mCameraCaptureSession.capture(builder.build(), null, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

//    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

//    ///为了使照片竖直显示
//    static {
//        ORIENTATIONS.append(Surface.ROTATION_0, 90);
//        ORIENTATIONS.append(Surface.ROTATION_90, 0);
//        ORIENTATIONS.append(Surface.ROTATION_180, 270);
//        ORIENTATIONS.append(Surface.ROTATION_270, 180);
//    }

    private void setupImageReader(int width, int height) {
        mImageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 2);
        mImageReader.setOnImageAvailableListener(reader -> {
            //
            Image image = reader.acquireNextImage();
            Image.Plane[] planes = image.getPlanes();
            Log.d(TAG, "count: " + cout++);
            ByteBuffer buffer = planes[0].getBuffer();
            int a = buffer.remaining();
            byte[] b = new byte[a];
            buffer.get(b);
            image.close();
//
            Bitmap bitmap = BitmapFactory.decodeByteArray(b, 0, a);

            Toast.makeText(this, "获得照片 宽" + bitmap.getWidth() + "  高" + bitmap.getHeight(), Toast.LENGTH_SHORT).show();


            File file = new File(Environment.getExternalStoragePublicDirectory(""), "aaa.jpg");

            try {

                Matrix matrix = new Matrix();
                matrix.postRotate(90);

                FileOutputStream stream = new FileOutputStream(file);
                Bitmap bitmap1 = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                bitmap1.compress(Bitmap.CompressFormat.JPEG, 100, stream);


            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            mBinding.getRoot().postDelayed(() -> mBinding.aaa.setImageBitmap(BitmapFactory.decodeFile(Environment.getExternalStoragePublicDirectory("") + "/aaa.jpg")), 1000);
        }, null);
    }

//    private void configureTransform(int viewWidth, int viewHeight) {
//        if (null == mPreviewSize) {
//            return;
//        }
//        int rotation = getWindowManager().getDefaultDisplay().getRotation();
//        Matrix matrix = new Matrix();
//        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
//        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
//        float centerX = viewRect.centerX();
//        float centerY = viewRect.centerY();
//        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
//            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
//            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
//            float scale = Math.max(
//                    (float) viewHeight / mPreviewSize.getHeight(),
//                    (float) viewWidth / mPreviewSize.getWidth());
//            matrix.postScale(scale, scale, centerX, centerY);
//            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
//        } else if (Surface.ROTATION_180 == rotation) {
//            matrix.postRotate(180, centerX, centerY);
//        }
//        mBinding.textureView.setTransform(matrix);
//    }

    //    /**
//     * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that
//     * is at least as large as the respective texture view size, and that is at most as large as the
//     * respective max size, and whose aspect ratio matches with the specified value. If such size
//     * doesn't exist, choose the largest one that is at most as large as the respective max size,
//     * and whose aspect ratio matches with the specified value.
//     *
//     * @param choices           The list of sizes that the camera supports for the intended output
//     *                          class
//     * @param textureViewWidth  The width of the texture view relative to sensor coordinate
//     * @param textureViewHeight The height of the texture view relative to sensor coordinate
//     * @param maxWidth          The maximum width that can be chosen
//     * @param maxHeight         The maximum height that can be chosen
//     * @param aspectRatio       The aspect ratio
//     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
//     */
//    private static Size chooseOptimalSize(@NonNull Size[] choices, int textureViewWidth,
//                                          int textureViewHeight, int maxWidth, int maxHeight, @NonNull Size aspectRatio) {
//
//        // Collect the supported resolutions that are at least as big as the preview Surface
//        List<Size> bigEnough = new ArrayList<>();
//        // Collect the supported resolutions that are smaller than the preview Surface
//        List<Size> notBigEnough = new ArrayList<>();
//        int w = aspectRatio.getWidth();
//        int h = aspectRatio.getHeight();
//        for (Size option : choices) {
//            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
//                    option.getHeight() == option.getWidth() * h / w) {
//                if (option.getWidth() >= textureViewWidth &&
//                        option.getHeight() >= textureViewHeight) {
//                    bigEnough.add(option);
//                } else {
//                    notBigEnough.add(option);
//                }
//            }
//        }
//
//        // Pick the smallest of those big enough. If there is no one big enough, pick the
//        // largest of those not big enough.
//        if (bigEnough.size() > 0) {
//            return Collections.min(bigEnough, new CompareSizesByArea());
//        } else if (notBigEnough.size() > 0) {
//            return Collections.max(notBigEnough, new CompareSizesByArea());
//        } else {
//            Log.e(TAG, "Couldn't find any suitable preview size");
//            return choices[0];
//        }
//    }
    private void setUpCameraOutputs(int width, int height) throws CameraAccessException {

        CameraCharacteristics characteristics
                = mManager.getCameraCharacteristics("0");

        StreamConfigurationMap map = characteristics.get(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        // For still image captures, we use the largest available size.
        Size largest = Collections.max(
                Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                new CompareSizesByArea());

        Log.d(TAG, "largest: " + largest);
        //setupImageReader(largest.getWidth(), largest.getHeight());

    }
}