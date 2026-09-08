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
import com.pedro.encoder.input.sources.audio.MicrophoneSource
import com.pedro.encoder.input.sources.audio.MixAudioSource
import com.pedro.encoder.input.sources.video.NoVideoSource
import com.pedro.encoder.input.sources.video.ScreenSource
import com.pedro.library.generic.GenericStream

class StreamService :
    Service(),
    ConnectChecker {

    companion object {

        const val ACTION_START =
            "nygus.START"

        const val ACTION_STOP =
            "nygus.STOP"

        const val EXTRA_RESULT_CODE =
            "resultCode"

        const val EXTRA_DATA =
            "data"

        const val EXTRA_ENDPOINT =
            "endpoint"

        const val EXTRA_AUDIO_MODE =
            "audioMode"
    }

    private lateinit var stream:
            GenericStream

    private var projection:
            MediaProjection? = null

    private val channelId =
        "nygus_live"

    override fun onCreate() {
        super.onCreate()

        createChannel()

        stream =
            GenericStream(
                this,
                this,
                NoVideoSource(),
                MicrophoneSource()
            )

        stream
            .getGlInterface()
            .setForceRender(
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
            128_000,
            echoCanceler = true,
            noiseSuppressor = true
        )
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        when (intent?.action) {

            ACTION_STOP -> {

                stopStreaming()
            }

            ACTION_START -> {

                val mode =
                    intent.getIntExtra(
                        EXTRA_AUDIO_MODE,
                        0
                    )

                startAsForeground(
                    mode
                )

                val resultCode =
                    intent.getIntExtra(
                        EXTRA_RESULT_CODE,
                        Activity.RESULT_CANCELED
                    )

                val data: Intent? =
                    if (
                        Build.VERSION.SDK_INT >= 33
                    ) {

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

                    startProjection(
                        resultCode,
                        data,
                        endpoint,
                        mode
                    )
                }
            }
        }

        return START_NOT_STICKY
    }

    private fun startProjection(
        resultCode: Int,
        data: Intent,
        endpoint: String,
        mode: Int
    ) {

        val manager =
            getSystemService(
                MEDIA_PROJECTION_SERVICE
            ) as MediaProjectionManager

        projection =
            manager.getMediaProjection(
                resultCode,
                data
            )

        val p =
            projection ?: return

        stream.changeVideoSource(
            ScreenSource(
                this,
                p
            )
        )

        when (mode) {

            1 -> {

                stream.changeAudioSource(
                    MicrophoneSource()
                )
            }

            2 -> {

                stream.changeAudioSource(
                    MixAudioSource(
                        p
                    )
                )
            }

            else -> {

                stream.changeAudioSource(
                    InternalAudioSource(
                        p
                    )
                )
            }
        }

        stream.startStream(
            endpoint
        )
    }

    private fun startAsForeground(
        mode: Int
    ) {

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

        if (
            Build.VERSION.SDK_INT >= 29
        ) {

            var type =
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION

            if (
                Build.VERSION.SDK_INT >= 30 &&
                mode != 0
            ) {

                type =
                    type or
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            }

            startForeground(
                7,
                notification,
                type
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

            projection?.stop()

        } catch (_: Exception) {
        }

        projection =
            null

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

            stream.release()

        } catch (_: Exception) {
        }

        projection?.stop()

        projection =
            null

        super.onDestroy()
    }

    private fun createChannel() {

        if (
            Build.VERSION.SDK_INT >= 26
        ) {

            val manager =
                getSystemService(
                    NOTIFICATION_SERVICE
                ) as NotificationManager

            manager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "NYGUS LIVE",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? = null

    override fun onConnectionStarted(
        url: String
    ) {
    }

    override fun onConnectionSuccess() {
    }

    override fun onConnectionFailed(
        reason: String
    ) {
        stopSelf()
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
