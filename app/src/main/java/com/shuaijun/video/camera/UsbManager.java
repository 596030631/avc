package com.shuaijun.video.camera;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.util.Log;

import com.serenegiant.usb.DeviceFilter;
import com.serenegiant.usb.USBMonitor;
import com.shuaijun.video.R;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public final class UsbManager implements USBMonitor.OnDeviceConnectListener {
    private static final String TAG = "CameraManager";

    private USBMonitor usbMonitor;
    private CameraHolder cameraHolder;
    private List<DeviceFilter> filter;

    private UsbManager() {
        // req
    }

    public static UsbManager getInstance() {
        return Holder.ins;
    }

    @Override
    public void onAttach(UsbDevice usbDevice) {
        Log.d(TAG, String.format(Locale.CHINA, "onAttach:\tproduct[%s]\tvendor[%s]\tname[%s]", usbDevice.getProductId(), usbDevice.getVendorId(), usbDevice.getDeviceName()));
        usbMonitor.requestPermission(usbDevice);
    }

    @Override
    public void onDettach(UsbDevice usbDevice) {
        Log.d(TAG, "onDetach:" + usbDevice.getDeviceName());
        closeCamera();
    }

    @Override
    public void onConnect(UsbDevice usbDevice, USBMonitor.UsbControlBlock usbControlBlock, boolean b) {
        Log.d(TAG, "onConnect:" + usbDevice.getDeviceName());
        try {
            closeCamera();
            cameraHolder = new CameraHolder("camera-" + usbDevice.getDeviceId(), usbControlBlock);
            cameraHolder.openCamera();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDisconnect(UsbDevice usbDevice, USBMonitor.UsbControlBlock usbControlBlock) {
        Log.d(TAG, "onDisconnect:" + usbDevice.getDeviceName());
        closeCamera();
    }

    @Override
    public void onCancel(UsbDevice usbDevice) {
        Log.d(TAG, "onCancel:" + usbDevice.getDeviceName());
    }

    public void initUsbMonitor(Context context) {
        if (context == null) return;
        usbMonitor = new USBMonitor(context, this);
        filter = DeviceFilter.getDeviceFilters(context, R.xml.device_filter);
        usbMonitor.addDeviceFilter(filter);
        usbMonitor.register();
    }

    public void releaseUsbMonitor() {
        if (usbMonitor != null) {
            usbMonitor.unregister();
            usbMonitor = null;
        }
    }

    public void startRecording() {
        if (cameraHolder != null) cameraHolder.startRecording();
    }

    public void stopRecording() {
        if (cameraHolder != null) cameraHolder.stopRecording();
    }

    public void openCamera() {
        List<UsbDevice> list = usbMonitor.getDeviceList(filter);
        if (list.isEmpty()) return;
        onAttach(list.get(0)); // 只取一个能用的
    }

    public void closeCamera() {
        if (cameraHolder != null) {
            cameraHolder.closeCamera();
            cameraHolder = null;
        }
    }

    private static class Holder {
        private static final UsbManager ins = new UsbManager();
    }

}
