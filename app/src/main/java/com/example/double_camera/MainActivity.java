package com.example.double_camera;

import androidx.annotation.NonNull;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.SessionConfiguration;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.graphics.SurfaceTexture;

import com.example.double_camera.databinding.ActivityMainBinding;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private ActivityMainBinding binding;
    private Button button1;
    private Button button2;
    private TextureView textureView1;
    private TextureView textureView2;
    private static final String TAG = MainActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Example of a call to a native method
        TextView tv = binding.sampleText;
        tv.setText(stringFromJNI());
        textureView1 = findViewById(R.id.textureView1);
        textureView1.setRotation(270);
        textureView2 = findViewById(R.id.textureView2);
        textureView2.setRotation(270);
        Camera2Information caminfo1 = new Camera2Information();
        caminfo1.camera2Id = "0";
        caminfo1.textureView = textureView1;
        Camera2Information caminfo2 = new Camera2Information();
        caminfo2.camera2Id = "1";
        caminfo2.textureView = textureView2;

        button1 = findViewById(R.id.button1);
        button2 = findViewById(R.id.button2);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tv.setText("fuck");
                openCamera(caminfo1);
                openCamera(caminfo2);
            }
        });
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture(caminfo1);
                takePicture(caminfo2);
            }
        });

        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        String[] cameraIdList = new String[0];
        try {
            cameraIdList = manager.getCameraIdList();
            for (String cameraId : cameraIdList) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                Size[] size1 = map.getOutputSizes(SurfaceTexture.class);
                Size[] size2 =map.getOutputSizes(ImageFormat.JPEG);
                Log.d(TAG, "预览尺寸：" + Arrays.toString(size1));
                Log.d(TAG, "拍照尺寸：" + Arrays.toString(size2));
                float[] ff = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
                Log.d(TAG, "焦距：" + Arrays.toString(ff));
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        tv.setText(Integer.toString(cameraIdList.length));
        Log.d(TAG, "逻辑ID：" + Arrays.toString(cameraIdList));

    }

    //开启摄像头
    public void openCamera(Camera2Information caminfo) {
        caminfo.handlerThread = new HandlerThread("DualCamera" + caminfo.getCamera2Id());
        caminfo.handlerThread.start();
        caminfo.handler = new Handler(caminfo.handlerThread.getLooper());
        caminfo.imageReader = ImageReader.newInstance(1280, 720, ImageFormat.JPEG, 2);
        CameraManager manager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
        try {
            //权限检查
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                //否则去请求相机权限
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
                return;
            }
            Log.d(TAG, "xxxx");
            CameraDeviceStateCallback cameraOpenCallBack = new CameraDeviceStateCallback(caminfo);
            manager.openCamera(caminfo.getCamera2Id(), cameraOpenCallBack,caminfo.handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //surfaceTexture的状态监听
    private TextureView.SurfaceTextureListener mSTListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //int position = (int) itemView.getTag();
            ////默认全部打开所有摄像头
            //mOpenBtn.setText(R.string.camera_case_btn_close);
            //mOpenBtn.setActivated(false);
            //openCamera();
            Log.d(TAG, Integer.toString(width)+"/"+Integer.toString(height));
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };

    public void takePicture(Camera2Information caminfo) {

        try {
            CaptureRequest.Builder captureRequestBuilder = caminfo.cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureRequestBuilder.addTarget(caminfo.imageReader.getSurface());
            // 自动对焦
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            // 自动曝光
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            // 获取手机方向
            //int rotation = getWindowManager().getDefaultDisplay().getRotation();
            // 根据设备方向计算设置照片的方向
            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, 90);
            //拍照
            CaptureRequest mCaptureRequest = captureRequestBuilder.build();
            caminfo.cameraCaptureSession.capture(mCaptureRequest, null, caminfo.handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //打开相机时候的监听器，通过他可以得到相机实例，这个实例可以创建请求建造者
    public class CameraDeviceStateCallback extends CameraDevice.StateCallback{
        private final Camera2Information mcaminfo;
        public CameraDeviceStateCallback(Camera2Information caminfo){
            mcaminfo = caminfo;
        }
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            Log.d(TAG, "相机已经打开");
            mcaminfo.cameraDevice = cameraDevice;
            //当逻辑摄像头开启后， 配置物理摄像头的参数
            try {
                //配置第一个物理摄像头
                mcaminfo.previewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                SurfaceTexture surfaceTexture = mcaminfo.textureView.getSurfaceTexture();
                surfaceTexture.setDefaultBufferSize(320, 240);
                Surface previewSurface = new Surface(surfaceTexture);
                mcaminfo.previewBuilder.addTarget(Objects.requireNonNull(previewSurface));

                //注册摄像头
                //cameraDevice.createCaptureSession(Arrays.asList(previewSurface, mcaminfo.imageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                cameraDevice.createCaptureSession(Arrays.asList(previewSurface), new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(CameraCaptureSession session) {
                        try {
                            mcaminfo.cameraCaptureSession = session;
                            CaptureRequest captureRequest = mcaminfo.previewBuilder.build();
                            session.setRepeatingRequest(captureRequest, null, mcaminfo.handler);
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onConfigureFailed(CameraCaptureSession session) {

                    }
                }, mcaminfo.handler);


            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {

        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {

        }
    }


    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

}