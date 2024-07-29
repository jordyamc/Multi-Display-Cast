package es.munix.multidisplaycast.services

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media.session.MediaButtonReceiver
import com.bumptech.glide.Glide
import es.munix.multidisplaycast.CastManager
import es.munix.multidisplaycast.R
import es.munix.multidisplaycast.helpers.NotificationsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class ControlsService : Service() {

    private val ID = 6158
    private var isPause = false
    private var title = ""
    private var subtitle = ""
    private var image: Bitmap? = null
    private var icon = ""
    private val whenLong = System.currentTimeMillis()
    private val mediaSession: MediaSessionCompat by lazy { MediaSessionCompat(this@ControlsService, "controls") }
    private val mediaMetadata get() = MediaMetadataCompat.Builder().apply {
        putText(MediaMetadataCompat.METADATA_KEY_TITLE, title)
        putText(MediaMetadataCompat.METADATA_KEY_ARTIST, subtitle)
        putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, image)
    }.build()
    private val playbackState get() = PlaybackStateCompat.Builder()
        .setState(if (isPause) PlaybackStateCompat.STATE_PAUSED else PlaybackStateCompat.STATE_PLAYING, 0L, 1F)
        .setActions(PlaybackStateCompat.ACTION_STOP or PlaybackStateCompat.ACTION_PLAY_PAUSE)
        .build()


    override fun onCreate() {
        startForeground(ID, notification)
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        /*if (intent?.hasExtra("image") == true) {
            val array = intent.getByteArrayExtra("image")
            image = BitmapFactory.decodeByteArray(array, 0, array?.size?:0)
        }*/
        intent?.getStringExtra("title")?.let { title = it }
        intent?.getStringExtra("subtitle")?.let { subtitle = it }
        intent?.getStringExtra("icon")?.let { icon = it }
        if (image == null && icon != "") {
            runBlocking(Dispatchers.IO) {
                image = Glide.with(this@ControlsService).asBitmap().load(icon).submit().get()
            }
        }
        when (intent?.action) {
            "cancel" -> {
                startForeground(ID, notification)
                stopSelf()
            }
            "togglePause" -> updateNotification(intent.getBooleanExtra("isPaused", false))
            else -> updateNotification()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun updateNotification(isPaused: Boolean = isPause) {
        isPause = isPaused
        (getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.notify(ID, notification)
    }

    private val notification: Notification
        get() {
            val notification = NotificationCompat.Builder(this, NotificationsHelper.CHANNEL).apply {
                setContentTitle(title)
                setContentText(subtitle)
                setOngoing(true)
                setAutoCancel(false)
                setColorized(true)
                setWhen(whenLong)
                setSmallIcon(R.drawable.cast_mc_on)
                if (image != null)
                    setLargeIcon(image)
                mediaSession.apply {
                    setMetadata(mediaMetadata)
                    setPlaybackState(playbackState)
                }
                setStyle(
                    MediaStyle()
                        .setShowActionsInCompactView(0,1)
                        .setMediaSession(mediaSession.sessionToken)
                )
                val castActivityIntent = Intent(this@ControlsService, CastManager.getInstance().controlsClass)
                val castActivityPendingIntent = PendingIntent.getActivity(this@ControlsService, ID + 1, castActivityIntent, PendingIntent.FLAG_IMMUTABLE)
                setContentIntent(castActivityPendingIntent)
                /*val disconnectIntent = Intent(this@ControlsService, CastReceiver::class.java)
                disconnectIntent.putExtra("action", "disconnect")
                addAction(R.drawable.ic_mc_stop, "Stop", PendingIntent.getBroadcast(this@ControlsService, ID + 2, disconnectIntent, PendingIntent.FLAG_IMMUTABLE))
                val pauseIntent = Intent(this@ControlsService, CastReceiver::class.java)
                pauseIntent.putExtra("action", "pause")
                val playIntent = Intent(this@ControlsService, CastReceiver::class.java)
                pauseIntent.putExtra("action", "play")
                val pausePendingIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(this@ControlsService, PlaybackStateCompat.ACTION_PLAY_PAUSE)
                val playPendingIntent = PendingIntent.getBroadcast(this@ControlsService, ID + 3, playIntent, PendingIntent.FLAG_IMMUTABLE)
                if (!isPause)
                    addAction(R.drawable.ic_mc_pause, "Pause", pausePendingIntent)
                else
                    addAction(R.drawable.ic_mc_play, "Play", playPendingIntent)*/
                Log.e("Cast service", "On update notification: paused $isPause")
                val stopPendingIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(this@ControlsService, PlaybackStateCompat.ACTION_STOP)
                val togglePendingIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(this@ControlsService, PlaybackStateCompat.ACTION_PLAY_PAUSE)
                addAction(if (isPause) R.drawable.ic_mc_play else R.drawable.ic_mc_pause, "Toggle", togglePendingIntent)
                addAction(R.drawable.ic_stop, "Stop", stopPendingIntent)
            }
            return notification.build()
        }
}