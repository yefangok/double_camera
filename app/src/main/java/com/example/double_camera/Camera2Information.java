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

    String camera2Id;
    CameraDevice cameraDevice;//相机设备
    CaptureRequest.Builder previewBuilder;//捕获请求(捕获请求模式:预览,拍照等)
    CameraCaptureSession cameraCaptureSession;//捕获会话的管理(开启或停止预览)
    ImageReader imageReader;//预览,拍照数据回调

    int previewFormat;//预览格式
    Size previewSize;//预览尺寸
    Size pictureSize;//图片尺寸
    Range<Integer> previewFps;//FPS
    String previewOrientation;//预览方向

    HandlerThread handlerThread;
    Handler handler;

    public String getCamera2Id() {
        return "1";
    }
}
