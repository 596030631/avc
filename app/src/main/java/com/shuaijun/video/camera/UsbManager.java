package com.shuaijun.video.camera;

import android.content.Context;
import android.hardware.camera2.CameraDevice;
import android.hardware.usb.UsbDevice;
import android.util.Log;

import com.serenegiant.usb.DeviceFilter;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;
import com.shuaijun.video.R;
import com.shuaijun.video.service.CameraBinder;

import java.util.List;
import java.util.Locale;

public final class UsbManager implements USBMonitor.OnDeviceConnectListener {
    private static final String TAG = "CameraManager";

    private USBMonitor usbMonitor;
    private CameraHolder cameraHolder;

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
    }

    @Override
    public void onConnect(UsbDevice usbDevice, USBMonitor.UsbControlBlock usbControlBlock, boolean b) {
        Log.d(TAG, "onConnect:" + usbDevice.getDeviceName());
        cameraHolder = new CameraHolder("camera-"+usbDevice.getDeviceId(), usbControlBlock);
        cameraHolder.openCamera();
    }

    @Override
    public void onDisconnect(UsbDevice usbDevice, USBMonitor.UsbControlBlock usbControlBlock) {
        Log.d(TAG, "onDisconnect:" + usbDevice.getDeviceName());
    }

    @Override
    public void onCancel(UsbDevice usbDevice) {
        Log.d(TAG, "onCancel:" + usbDevice.getDeviceName());
    }

    public void initUsbMonitor(Context context) {
        if (context == null) return;
        usbMonitor = new USBMonitor(context, this);
        List<DeviceFilter> filter = DeviceFilter.getDeviceFilters(context, R.xml.device_filter);
        usbMonitor.addDeviceFilter(filter);
        usbMonitor.register();
    }

    public void releaseUsbMonitor() {
        if (usbMonitor != null) {
            usbMonitor.unregister();
            usbMonitor = null;
        }
    }

    private static class Holder {
        private static final UsbManager ins = new UsbManager();
    }

}
