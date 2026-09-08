package pl.nygus.live

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder

import com.pedro.common.ConnectChecker
import com.pedro.encoder.input.sources.audio.InternalAudioSource
import com.pedro.encoder.input.sources.audio.NoAudioSource
import com.pedro.encoder.input.sources.video.NoVideoSource
import com.pedro.encoder.input.sources.video.ScreenSource
import com.pedro.library.generic.GenericStream

class StreamService : Service(), ConnectChecker {

    companion object {
        const val ACTION_START = "nygus.START"
        const val ACTION_STOP = "nygus.STOP"

        const val EXTRA_RESULT_CODE = "resultCode"
        const val EXTRA_DATA = "data"
        const val EXTRA_ENDPOINT = "endpoint"
    }

    private lateinit var stream: GenericStream

    private var mediaProjection: MediaProjection? = null

    private val channelId = "nygus_live"

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        /*
         * WAŻNE:
         * Tu NIE MA MicrophoneSource.
         * Aplikacja nie pobiera dźwięku z mikrofonu.
         */
        stream = GenericStream(
            this,
            this,
            NoVideoSource(),
            NoAudioSource()
        )

        /*
         * Stałe FPS przy przechwytywaniu ekranu.
         */
        stream.getGlInterface().setForceRender(
            true,
            30
        )

        stream.prepareVideo(
            720,
            1280,
            2_500_000,
            rotation = 90
        )

        stream.prepareAudio(
            44100,
            true,
            128_000
        )
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        when (intent?.action) {

            ACTION_START -> {

                /*
                 * Najpierw uruchamiamy foreground service.
                 */
                startAsForeground()

                val resultCode =
                    intent.getIntExtra(
                        EXTRA_RESULT_CODE,
                        Activity.RESULT_CANCELED
                    )

                val data: Intent? =
                    if (Build.VERSION.SDK_INT >= 33) {

                        intent.getParcelableExtra(
                            EXTRA_DATA,
                            Intent::class.java
                        )

                    } else {

                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(
                            EXTRA_DATA
                        )
                    }

                val endpoint =
                    intent.getStringExtra(
                        EXTRA_ENDPOINT
                    ).orEmpty()

                if (
                    data != null &&
                    endpoint.isNotBlank()
                ) {

                    startStreaming(
                        resultCode,
                        data,
                        endpoint
                    )
                }
            }

            ACTION_STOP -> {
                stopStreaming()
            }
        }

        return START_NOT_STICKY
    }

    private fun startStreaming(
        resultCode: Int,
        data: Intent,
        endpoint: String
    ) {

        try {

            val projectionManager =
                getSystemService(
                    MEDIA_PROJECTION_SERVICE
                ) as MediaProjectionManager

            mediaProjection?.stop()

            mediaProjection =
                projectionManager.getMediaProjection(
                    resultCode,
                    data
                )

            val projection =
                mediaProjection ?: return

            /*
             * OBRAZ:
             * ekran telefonu / nasz HTML WebView.
             */
            stream.changeVideoSource(
                ScreenSource(
                    applicationContext,
                    projection
                )
            )

            /*
             * AUDIO:
             * TYLKO dźwięk odtwarzany przez telefon.
             * Brak MicrophoneSource.
             */
            stream.changeAudioSource(
                InternalAudioSource(
                    projection
                )
            )

            stream.startStream(
                endpoint
            )

        } catch (e: Exception) {

            e.printStackTrace()

            stopStreaming()
        }
    }

    private fun startAsForeground() {

        val notification =
            Notification.Builder(
                this,
                channelId
            )
                .setContentTitle(
                    "NYGUS LIVE"
                )
                .setContentText(
                    "Transmisja RTMP działa"
                )
                .setSmallIcon(
                    android.R.drawable.presence_video_online
                )
                .setOngoing(true)
                .build()

        if (Build.VERSION.SDK_INT >= 29) {

            startForeground(
                7,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )

        } else {

            startForeground(
                7,
                notification
            )
        }
    }

    private fun stopStreaming() {

        try {

            if (
                ::stream.isInitialized &&
                stream.isStreaming
            ) {

                stream.stopStream()
            }

        } catch (_: Exception) {
        }

        try {

            mediaProjection?.stop()

        } catch (_: Exception) {
        }

        mediaProjection = null

        stopForeground(
            STOP_FOREGROUND_REMOVE
        )

        stopSelf()
    }

    override fun onDestroy() {

        try {

            if (
                ::stream.isInitialized &&
                stream.isStreaming
            ) {

                stream.stopStream()
            }

        } catch (_: Exception) {
        }

        try {

            if (::stream.isInitialized) {
                stream.release()
            }

        } catch (_: Exception) {
        }

        try {

            mediaProjection?.stop()

        } catch (_: Exception) {
        }

        mediaProjection = null

        super.onDestroy()
    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= 26) {

            val manager =
                getSystemService(
                    NOTIFICATION_SERVICE
                ) as NotificationManager

            val channel =
                NotificationChannel(
                    channelId,
                    "NYGUS LIVE",
                    NotificationManager.IMPORTANCE_LOW
                )

            manager.createNotificationChannel(
                channel
            )
        }
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? {
        return null
    }

    override fun onConnectionStarted(
        url: String
    ) {
    }

    override fun onConnectionSuccess() {
    }

    override fun onConnectionFailed(
        reason: String
    ) {

        stopStreaming()
    }

    override fun onNewBitrate(
        bitrate: Long
    ) {
    }

    override fun onDisconnect() {
    }

    override fun onAuthError() {
    }

    override fun onAuthSuccess() {
    }
}
