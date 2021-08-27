package com.shuaijun.video.service;

import android.os.RemoteException;

import com.shuaijun.video.ICameraServiceAidl;

public class CameraBinder extends ICameraServiceAidl.Stub {

    @Override
    public void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat, double aDouble, String aString) throws RemoteException {

    }

}
