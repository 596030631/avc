package com.shuaijun.video.service;

import android.os.RemoteException;

import com.shuaijun.video.ICameraServiceAidl;
import com.shuaijun.video.camera.UsbManager;

public class CameraBinder extends ICameraServiceAidl.Stub {


    @Override
    public void startRecording(String pathName, int seconds) throws RemoteException {
        UsbManager.getInstance().startRecording(pathName, seconds);
    }

    @Override
    public void stopRecording() throws RemoteException {
        UsbManager.getInstance().stopRecording();
    }

    @Override
    public void openCamera() throws RemoteException {
        UsbManager.getInstance().openCamera();
    }

    @Override
    public void closeCamera() throws RemoteException {
        UsbManager.getInstance().closeCamera();
    }
}
