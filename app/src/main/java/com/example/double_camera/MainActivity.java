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
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.SessionConfiguration;
import android.media.Image;
import android.media.ImageReader;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
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
    private TextureView textureView1;
    private TextureView textureView2;
    private static final String TAG = MainActivity.class.getName();
    private Handler mBackgroundHandler1;
    private Handler mBackgroundHandler2;
    private ImageReader mImageReader1;
    private ImageReader mImageReader2;
    private CameraCaptureSession mCameraCaptureSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Example of a call to a native method
        TextView tv = binding.sampleText;
        tv.setText(stringFromJNI());

        textureView1 = findViewById(R.id.textureView1);
        textureView2 = findViewById(R.id.textureView2);
        button1 = findViewById(R.id.button1);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tv.setText("fuck");
                openCamera();
            }
        });

        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        String[] cameraIdList = new String[0];
        try {
            cameraIdList = manager.getCameraIdList();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        tv.setText(Integer.toString(cameraIdList.length));
        Log.d(TAG, "逻辑ID：" + Arrays.toString(cameraIdList));


    }


    //打开相机时候的监听器，通过他可以得到相机实例，这个实例可以创建请求建造者
    private CameraDevice.StateCallback cameraOpenCallBack = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice cameraDevice) {
            Log.d(TAG, "相机已经打开");
            //当逻辑摄像头开启后， 配置物理摄像头的参数
            config(cameraDevice);
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            Log.d(TAG, "相机连接断开");
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            Log.d(TAG, "相机打开失败");
        }
    };

    //开启摄像头
    public void openCamera() {
        HandlerThread thread1 = new HandlerThread("DualCamera1");
        HandlerThread thread2 = new HandlerThread("DualCamera2");
        thread1.start();
        thread2.start();
        mBackgroundHandler1 = new Handler(thread1.getLooper());
        mBackgroundHandler2 = new Handler(thread2.getLooper());
        mImageReader1 = ImageReader.newInstance(640, 480, ImageFormat.JPEG, 2);
        mImageReader2 = ImageReader.newInstance(640, 480, ImageFormat.JPEG, 2);
        CameraManager manager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
        try {
            //权限检查
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                //否则去请求相机权限
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
                return;
            }
            Log.d(TAG, "xxxx");
            manager.openCamera("0", cameraOpenCallBack,mBackgroundHandler1);
            manager.openCamera("1", cameraOpenCallBack,mBackgroundHandler2);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 配置摄像头参数
     * @param cameraDevice
     */
    public void config(CameraDevice cameraDevice){
        try {
            //构建输出参数  在参数中设置物理摄像头
            CaptureRequest.Builder previewBuidler = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            String devid = cameraDevice.getId();
            Surface previewSurface;
            ImageReader mImageReader;
            Handler backgroundHandler;
            if(devid=="0") {
                //配置第一个物理摄像头
                previewSurface = new Surface(textureView1.getSurfaceTexture());
                mImageReader = mImageReader1;
                backgroundHandler = mBackgroundHandler1;
            }
            else{
                previewSurface = new Surface(textureView2.getSurfaceTexture());
                mImageReader = mImageReader2;
                backgroundHandler = mBackgroundHandler2;
            }
            previewBuidler.addTarget(Objects.requireNonNull(previewSurface));

            //注册摄像头
            cameraDevice.createCaptureSession(Arrays.asList(previewSurface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        CaptureRequest captureRequest = previewBuidler.build();
                        session.setRepeatingRequest(captureRequest, null, backgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {

                }
            }, backgroundHandler);


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
            openCamera();
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



    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}