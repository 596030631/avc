package com.shuaijun.video.camera;

import android.os.Handler;
import android.os.HandlerThread;
import android.view.Surface;

import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;
import com.shuaijun.video.render.RenderHolderCallback;
import com.shuaijun.video.render.RendererHolder;

public class CameraHolder implements RenderHolderCallback {

    private final Handler handler;
    private final UVCCamera camera;
    private USBMonitor.UsbControlBlock usbControlBlock;
    private RendererHolder rendererHolder;

    public CameraHolder(String name, USBMonitor.UsbControlBlock usbControlBlock) {
        this.usbControlBlock = usbControlBlock;
        camera = new UVCCamera();
        HandlerThread handlerThread = new HandlerThread(name);
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        rendererHolder = new RendererHolder(640, 480, this);
    }

    public void openCamera() {
        handler.post(() -> {
            camera.open(usbControlBlock);
            camera.setPreviewSize(640, 480, UVCCamera.DEFAULT_PREVIEW_MODE);
            camera.setPreviewDisplay(rendererHolder.getSurface());
            camera.startPreview();
        });
    }

    public void closeCamera() {
        handler.post(() -> {
            camera.stopCapture();
            camera.stopPreview();
            camera.close();
            usbControlBlock.close();
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
