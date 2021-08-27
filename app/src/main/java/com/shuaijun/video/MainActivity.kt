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

        findViewById<Button>(R.id.bind_service).setOnClickListener {
            bindService()
        }

        findViewById<Button>(R.id.unbind_service).setOnClickListener {
            unbindService()
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
}