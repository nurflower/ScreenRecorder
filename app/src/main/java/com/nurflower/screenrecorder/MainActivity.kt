package com.nurflower.screenrecorder

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.nurflower.screenrecorder.databinding.ActivityMainBinding


private const val RECORD_REQUEST_CODE = 101
private const val PERMISSION_REQUEST_CODE = 102


class MainActivity : AppCompatActivity() {

    private lateinit var activityViewModel: MainActivityViewModel
    private lateinit var binding: ActivityMainBinding
    private var projectionManager: MediaProjectionManager? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        activityViewModel = ViewModelProvider(this)[MainActivityViewModel::class.java]

        binding.apply {
            viewModel = activityViewModel
            lifecycleOwner = this@MainActivity
        }

        if(!checkPermissions()){
            requestPermissions()
        }

        activityViewModel.startRecording.observe(this, Observer {
            if (it){
                activityViewModel.changeStartButtonStatus()
                startCapturing()
            }
        })

        activityViewModel.stopRecording.observe(this, Observer {
            if (it){
                activityViewModel.changeStopButtonStatus()
                stopCapturing()
            }
        })

    }

    private fun requestPermissions(){
        ActivityCompat.requestPermissions(this, listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO).toTypedArray(), PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE){
            if (grantResults.size == 2) {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED || grantResults[1] != PackageManager.PERMISSION_GRANTED) {
                    finish()
                }
            }
        }
    }

    private fun checkPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RECORD_REQUEST_CODE && resultCode == RESULT_OK) {
            val intent = Intent(this, RecordService::class.java).apply {
                action = RecordService.ACTION_START
                putExtra(RecordService.EXTRA_RESULT_DATA, data!!)
            }
            startForegroundService(intent)

            setButtonsEnabled(isCapturingAudio = true)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun setButtonsEnabled(isCapturingAudio: Boolean) {
        binding.startBtn.isEnabled = !isCapturingAudio
        binding.stopBtn.isEnabled = isCapturingAudio
    }

    private fun stopCapturing() {
        setButtonsEnabled(isCapturingAudio = false)
        startService(Intent(this, RecordService::class.java).apply {
            action = RecordService.ACTION_STOP
        })
    }


    private fun startMediaProjectionRequest() {
        projectionManager = applicationContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(projectionManager?.createScreenCaptureIntent(), RECORD_REQUEST_CODE)
    }

    private fun startCapturing() {
        if (!checkPermissions()) {
            requestPermissions()
        } else {
            startMediaProjectionRequest()
        }
    }

}