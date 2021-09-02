package com.shuaijun.video

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {

    private var cameraServiceAidl: ICameraServiceAidl? = null

    private val serviceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            cameraServiceAidl = ICameraServiceAidl.Stub.asInterface(service)
        }

        override fun onServiceDisconnected(name: ComponentName?) {

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindService()


        findViewById<Button>(R.id.open_camera).setOnClickListener {
            cameraServiceAidl?.openCamera()
        }

        findViewById<Button>(R.id.close_camera).setOnClickListener {
            cameraServiceAidl?.closeCamera()
        }

        findViewById<Button>(R.id.start_recording).setOnClickListener {
            startRecording(5)
        }

        findViewById<Button>(R.id.stop_recording).setOnClickListener {
            stopRecording()
        }
    }

    private fun bindService() {
        val intent = Intent()
        intent.type = SystemClock.elapsedRealtime().toString()
        intent.component =
            ComponentName("com.shuaijun.video", "com.shuaijun.video.service.CameraService")
        intent.setPackage(BuildConfig.APPLICATION_ID)
        applicationContext.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun unbindService() {
        applicationContext.unbindService(serviceConnection)
    }

    private fun startRecording(seconds: Int) {
        cameraServiceAidl?.startRecording(seconds)
    }

    private fun stopRecording() {
        cameraServiceAidl?.stopRecording()
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService()
    }
}