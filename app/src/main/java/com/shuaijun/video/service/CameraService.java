package com.shuaijun.video.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.shuaijun.avc.AvcEncoder;
import com.shuaijun.video.camera.UsbManager;

public class CameraService extends Service {

    public static final String TAG = "CameraService";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("TAG", "录像服务创建");
        UsbManager.getInstance().initUsbMonitor(getApplicationContext());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new CameraBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d("TAG", "录像服务解绑");
        UsbManager.getInstance().stopRecording();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        UsbManager.getInstance().releaseUsbMonitor();
        AvcEncoder.getInstance().release();
        Log.d("TAG", "录像服务销毁");
    }
}