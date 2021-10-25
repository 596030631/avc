package com.shuaijun.video.camera;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Log;
import android.view.Surface;

import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;
import com.shuaijun.avc.AvcEncoder;
import com.shuaijun.video.render.RenderHolderCallback;
import com.shuaijun.video.render.RendererHolder;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class CameraHolder implements RenderHolderCallback {

    public static final int WIDTH = 640;
    public static final int HEIGHT = 480;

    private final Handler handler;
    private final UVCCamera camera;
    private final USBMonitor.UsbControlBlock usbControlBlock;
    private final RendererHolder rendererHolder;
    private Timer timer;

    private static final int CODE_IDLE = 0;
    private static final int CODE_OPENED = 1;
    private static final int CODE_RECORDING = 2;
    private int flag = CODE_IDLE;


    public CameraHolder(String name, USBMonitor.UsbControlBlock usbControlBlock) throws IOException {
        this.usbControlBlock = usbControlBlock;
        camera = new UVCCamera();
        HandlerThread handlerThread = new HandlerThread(name);
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        rendererHolder = new RendererHolder(WIDTH, HEIGHT, this);
    }

    public void openCamera() {
        handler.post(() -> {
            if (flag == CODE_IDLE) {
                flag = CODE_OPENED;
                camera.open(usbControlBlock);
            }
        });
    }

    public void startRecording(String pathName, final int seconds) {
        handler.post(() -> {
            if (flag == CODE_OPENED) {
                flag = CODE_RECORDING;
                File outFile = new File(pathName);
                Log.d("et_log", "开始录像中，录像输出文件：" + outFile.getAbsolutePath());
                AvcEncoder.getInstance().startEncoder(outFile);
                camera.setPreviewSize(WIDTH, HEIGHT, UVCCamera.DEFAULT_PREVIEW_MODE);
                camera.setPreviewDisplay(rendererHolder.getSurface());
                camera.setFrameCallback(AvcEncoder.getInstance()::offerByteBuffer, UVCCamera.PIXEL_FORMAT_NV21);
                camera.startPreview();
            }

            if (flag == CODE_RECORDING) {
                if (timer != null) timer.cancel();
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        stopRecording();
                        timer = null;
                    }
                }, Math.max(seconds * 1000, 5_000));
            }
        });
    }

    public void stopRecording() {
        handler.post(() -> {
            if (flag == CODE_RECORDING) {
                flag = CODE_OPENED;
                AvcEncoder.getInstance().stopEncoder();
                camera.stopPreview();
            }
        });
    }

    public void closeCamera() {
        handler.post(() -> {
            switch (flag) {
                case CODE_IDLE:
                    rendererHolder.release();
                    camera.destroy();
                    break;
                case CODE_OPENED:
                    AvcEncoder.getInstance().stopEncoder();
                    camera.stopPreview();
                    rendererHolder.release();
                    camera.close();
                    usbControlBlock.close();
                    camera.destroy();
                    flag = CODE_IDLE;
                    break;
                case CODE_RECORDING:
                    stopRecording();
                    closeCamera();
                    break;
                default:
                    break;
            }
        });
    }

    @Override
    public void onCreate(Surface surface) {

    }

    @Override
    public void onFrameAvailable() {

    }

    @Override
    public void onRenderDestroy() {

    }
}
