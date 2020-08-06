package com.nurflower.screenrecorder

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION
import android.hardware.display.VirtualDisplay
import android.media.*
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import java.io.IOException


class RecordService: Service() {

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var mediaProjection: MediaProjection? = null

    private var mediaRecorder: MediaRecorder? = null
    private var mVirtualDisplay: VirtualDisplay? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(SERVICE_ID, NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID).build())
        mediaProjectionManager = applicationContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Канал записи экрана",
            NotificationManager.IMPORTANCE_DEFAULT
        )

        val manager = getSystemService(NotificationManager::class.java) as NotificationManager
        manager.createNotificationChannel(serviceChannel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return if (intent != null) {
            when (intent.action) {
                ACTION_START -> {
                    mediaProjection = mediaProjectionManager.getMediaProjection(Activity.RESULT_OK, intent.getParcelableExtra(EXTRA_RESULT_DATA)!!) as MediaProjection
                    startVideoCapture()
                    START_STICKY
                }
                ACTION_STOP -> {
                    stopVideoCapture()
                    START_NOT_STICKY
                }
                else -> throw IllegalArgumentException("Unexpected action received: ${intent.action}")
            }
        } else {
            START_NOT_STICKY
        }
    }

    private fun startVideoCapture(){

        mediaRecorder = MediaRecorder()

        val metrics = DisplayMetrics()
        val wm = applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm.defaultDisplay.getRealMetrics(metrics)

        val mScreenDensity = metrics.densityDpi
        val displayWidth = metrics.widthPixels
        val displayHeight = metrics.heightPixels

        mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC) // audio source from microphone
        mediaRecorder?.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mediaRecorder?.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mediaRecorder?.setVideoEncodingBitRate(8 * 1000 * 1000)
        mediaRecorder?.setVideoFrameRate(15)

        mediaRecorder?.setVideoSize(displayWidth, displayHeight)

        val videoDir: String = getExternalFilesDir(Environment.DIRECTORY_MOVIES)?.absolutePath.toString()
        val timestamp = System.currentTimeMillis()

        var orientation = "portrait"
        if (displayWidth > displayHeight) {
            orientation = "landscape"
        }
        val filePathAndName = videoDir + "/time_" + timestamp.toString() + "_mode_" + orientation + ".mp4"

        mediaRecorder?.setOutputFile(filePathAndName)

        try {
            mediaRecorder?.prepare()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        val surface: Surface? = mediaRecorder?.surface
        mVirtualDisplay = mediaProjection?.createVirtualDisplay(
            "MainActivity",
            displayWidth,
            displayHeight,
            mScreenDensity,
            VIRTUAL_DISPLAY_FLAG_PRESENTATION,
            surface,
            null,
            null
        )
        mediaRecorder?.start()

        Log.d("RecordService", "Started recording")

    }
    private fun stopVideoCapture(){
        mediaRecorder?.stop()
        mediaProjection?.stop()
        mediaRecorder?.release()
        mVirtualDisplay?.release()
        Toast.makeText(this, "Stopped and saved", Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVideoCapture()
        stopSelf()
    }

    override fun onBind(p0: Intent?): IBinder? = null


    companion object {
        private const val SERVICE_ID = 123
        private const val NOTIFICATION_CHANNEL_ID = "Screen Capture channel"
        const val ACTION_START = "RecordService:Start"
        const val ACTION_STOP = "RecordService:Stop"
        const val EXTRA_RESULT_DATA = "RecordService:Extra:ResultData"
    }


}