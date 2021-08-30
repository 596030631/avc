// ICameraServiceAidl.aidl
package com.shuaijun.video;

// Declare any non-default types here with import statements

interface ICameraServiceAidl {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void openCamera();

    void closeCamera();

    void startRecording();

    void stopRecording();

}