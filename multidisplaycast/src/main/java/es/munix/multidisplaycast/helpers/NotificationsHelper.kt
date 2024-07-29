package es.munix.multidisplaycast.helpers

import androidx.core.content.ContextCompat
import android.content.Intent
import es.munix.multidisplaycast.services.ControlsService
import android.annotation.TargetApi
import android.app.Notification
import android.os.Build
import android.app.NotificationManager
import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media.session.MediaButtonReceiver
import com.bumptech.glide.Glide
import es.munix.multidisplaycast.CastManager
import es.munix.multidisplaycast.R
import es.munix.multidisplaycast.helpers.NotificationsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * Created by munix on 3/11/16.
 */
open class NotificationsHelper {

    private var mediaSession: MediaSessionCompat? = null
    private var title = ""
    private var subtitle = ""
    private var image: Bitmap? = null
    private var isPause = false
    private var whenLong: Long = 0L
    private val mediaMetadata get() = MediaMetadataCompat.Builder().apply {
        putText(MediaMetadataCompat.METADATA_KEY_TITLE, title)
        putText(MediaMetadataCompat.METADATA_KEY_ARTIST, subtitle)
        putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, image)
    }.build()
    private val playbackState get() = PlaybackStateCompat.Builder()
        .setState(if (isPause) PlaybackStateCompat.STATE_PAUSED else PlaybackStateCompat.STATE_PLAYING, 0L, 1F)
        .setActions(PlaybackStateCompat.ACTION_STOP or PlaybackStateCompat.ACTION_PLAY_PAUSE)
        .build()

    fun cancelNotification(context: Context?) {
        if (context != null) {
            stop(context)
            /*ContextCompat.startForegroundService(
                context, Intent(context, ControlsService::class.java)
                    .setAction("cancel")
            )*/
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL,
                "Cast controls",
                NotificationManager.IMPORTANCE_MIN
            )
        )
    }

    fun updateButton(context: Context, isPaused: Boolean) {
        isPause = isPaused
        updateNotification(context)
        /*ContextCompat.startForegroundService(
            context, Intent(context, ControlsService::class.java)
                .setAction("togglePause")
                .putExtra("isPaused", isPaused)
        )*/
    }

    fun stop(context: Context) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.cancel(ID)
        reset()
        /*ContextCompat.startForegroundService(
            context, Intent(context, ControlsService::class.java)
                .setAction("cancel")
        )*/
    }

    fun reset() {
        title = ""
        subtitle = ""
        image = null
        whenLong = 0L
        mediaSession?.release()
        mediaSession = null
    }

    fun showNotification(context: Context, title: String, subtitle: String, icon: String?) {
        mediaSession = MediaSessionCompat(context, "controls")
        this.title = title
        this.subtitle = subtitle
        if (image == null && icon != "") {
            runBlocking(Dispatchers.IO) {
                image = Glide.with(context).asBitmap().load(icon).submit().get()
            }
        }
        updateNotification(context)
        /*val intent = Intent(context, ControlsService::class.java)
            .putExtra("title", title)
            .putExtra("subtitle", subtitle)
            .putExtra("icon", icon)
        ContextCompat.startForegroundService(context, intent)*/
    }

    private fun updateNotification(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) createChannel(context)
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.notify(ID, createNotification(context))
    }

    private fun createNotification(context: Context): Notification {
            val notification = NotificationCompat.Builder(context, CHANNEL).apply {
                setContentTitle(title)
                setContentText(subtitle)
                setOngoing(true)
                setAutoCancel(false)
                setColorized(true)
                if (whenLong == 0L) whenLong = System.currentTimeMillis()
                setWhen(whenLong)
                setSmallIcon(R.drawable.cast_mc_on)
                if (image != null)
                    setLargeIcon(image)
                mediaSession?.apply {
                    setMetadata(mediaMetadata)
                    setPlaybackState(playbackState)
                }
                setStyle(
                    androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0,1)
                        .setMediaSession(mediaSession?.sessionToken)
                )
                val castActivityIntent = Intent(context, CastManager.getInstance().controlsClass)
                val castActivityPendingIntent = PendingIntent.getActivity(context, ID + 1, castActivityIntent, PendingIntent.FLAG_IMMUTABLE)
                setContentIntent(castActivityPendingIntent)
                val stopPendingIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP)
                val togglePendingIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_PLAY_PAUSE)
                addAction(if (isPause) R.drawable.ic_mc_play else R.drawable.ic_mc_pause, "Toggle", togglePendingIntent)
                addAction(R.drawable.ic_stop, "Stop", stopPendingIntent)
            }
            return notification.build()
        }

    companion object {
        const val ID = 6158
        const val CHANNEL = "cast_notification"
    }
}