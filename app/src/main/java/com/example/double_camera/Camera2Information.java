package com.example.double_camera;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Range;
import android.util.Size;

public class Camera2Information {

    private String camera2Id;
    private CameraDevice cameraDevice;//相机设备
    private CaptureRequest.Builder previewBuilder;//捕获请求(捕获请求模式:预览,拍照等)
    private CameraCaptureSession cameraCaptureSession;//捕获会话的管理(开启或停止预览)
    private ImageReader imageReader;//预览,拍照数据回调

    private int previewFormat;//预览格式
    private Size previewSize;//预览尺寸
    private Size pictureSize;//图片尺寸
    private Range<Integer> previewFps;//FPS
    private String previewOrientation;//预览方向

    private HandlerThread handlerThread;
    private Handler handler;

    public String getCamera2Id() {
        return "1";
    }
}
