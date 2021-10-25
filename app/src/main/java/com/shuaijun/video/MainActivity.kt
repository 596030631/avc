package com.shuaijun.video

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File


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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1
                );
            }
        }

        bindService()


        findViewById<Button>(R.id.open_camera).setOnClickListener {
            cameraServiceAidl?.openCamera()
        }

        findViewById<Button>(R.id.close_camera).setOnClickListener {
            cameraServiceAidl?.closeCamera()
        }

        findViewById<Button>(R.id.start_recording).setOnClickListener {
            getExternalFilesDir(Environment.DIRECTORY_MOVIES).apply {
                File(this, "${SystemClock.elapsedRealtime()}.h264").let {
                    Log.d("et_log", "开始录制任务：${it.absolutePath}")
                    startRecording(it.absolutePath, 60)
                }
            }
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

    private fun startRecording(pathName: String, seconds: Int) {
        cameraServiceAidl?.startRecording(pathName, seconds)
    }

    private fun stopRecording() {
        cameraServiceAidl?.stopRecording()
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService()
    }
}