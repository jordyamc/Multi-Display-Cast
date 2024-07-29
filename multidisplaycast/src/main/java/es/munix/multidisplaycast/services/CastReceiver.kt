package es.munix.multidisplaycast.services

import android.content.Context
import androidx.media.session.MediaButtonReceiver
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import es.munix.multidisplaycast.CastManager
import kotlin.math.log

/**
 * Created by munix on 2/11/16.
 */
class CastReceiver : MediaButtonReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val extras = intent.extras ?: return
        if (extras.containsKey("action")) {
            val action = extras.getString("action")
            if (action == "disconnect") {
                CastManager.getInstance().stop()
            } else if (action == "pause") {
                CastManager.getInstance().pause()
            } else if (action == "play") {
                CastManager.getInstance().play()
            }
        } else if (extras.containsKey(Intent.EXTRA_KEY_EVENT)){
            val event = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT) ?: return
            if (event.action == KeyEvent.ACTION_UP || event.action == KeyEvent.ACTION_DOWN) {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_MEDIA_STOP -> CastManager.getInstance().stop()
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> CastManager.getInstance().togglePause()
                }
            }
        }
    }
}