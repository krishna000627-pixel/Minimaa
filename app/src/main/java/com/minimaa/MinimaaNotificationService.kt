package com.minimaa

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.view.KeyEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MediaState(
    val title: String = "NO MEDIA",
    val artist: String = "TAP TO PLAY",
    val isPlaying: Boolean = false,
    val pkg: String? = null
)

data class NotifItem(
    val key: String,
    val pkg: String,
    val title: String,
    val text: String,
    val time: Long
)

object SystemBridge {
    private val _media = MutableStateFlow(MediaState())
    val media = _media.asStateFlow()

    private val _notifs = MutableStateFlow<List<NotifItem>>(emptyList())
    val notifs = _notifs.asStateFlow()

    private var activeController: MediaController? = null

    fun isNotificationAccessEnabled(ctx: Context): Boolean {
        val flat = Settings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners") ?: return false
        val cn = ComponentName(ctx, MinimaaNotificationService::class.java).flattenToString()
        return flat.split(":").any { it.equals(cn, ignoreCase = true) || it.contains(ctx.packageName) }
    }

    fun updateMedia(controller: MediaController?) {
        activeController = controller
        if (controller == null) {
            _media.value = MediaState()
            return
        }
        val meta = controller.metadata
        val title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: "UNKNOWN TRACK"
        val artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: "UNKNOWN ARTIST"
        val playing = controller.playbackState?.state == PlaybackState.STATE_PLAYING
        _media.value = MediaState(
            title = title.uppercase(),
            artist = artist.uppercase(),
            isPlaying = playing,
            pkg = controller.packageName
        )
    }

    fun updateNotifications(list: List<NotifItem>) {
        _notifs.value = list
    }

    fun togglePlayPause(ctx: Context) {
        val c = activeController
        if (c != null) {
            val state = c.playbackState?.state
            if (state == PlaybackState.STATE_PLAYING) c.transportControls.pause()
            else c.transportControls.play()
        } else {
            val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
        }
    }
}

class MinimaaNotificationService : NotificationListenerService() {
    private var sessionManager: MediaSessionManager? = null
    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        refreshMedia(controllers)
    }
    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = refreshFromManager()
        override fun onMetadataChanged(metadata: MediaMetadata?) = refreshFromManager()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        sessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
        val cn = ComponentName(this, MinimaaNotificationService::class.java)
        try {
            sessionManager?.addOnActiveSessionsChangedListener(sessionsListener, cn)
            refreshFromManager()
        } catch (_: SecurityException) { }
        syncNotifications()
    }

    override fun onListenerDisconnected() {
        try {
            sessionManager?.removeOnActiveSessionsChangedListener(sessionsListener)
        } catch (_: Exception) { }
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) = syncNotifications()
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = syncNotifications()

    private fun refreshFromManager() {
        val cn = ComponentName(this, MinimaaNotificationService::class.java)
        try {
            refreshMedia(sessionManager?.getActiveSessions(cn))
        } catch (_: SecurityException) { }
    }

    private fun refreshMedia(controllers: List<MediaController>?) {
        val primary = controllers?.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        } ?: controllers?.firstOrNull()
        primary?.let {
            try {
                it.unregisterCallback(controllerCallback)
                it.registerCallback(controllerCallback)
            } catch (_: Exception) { }
        }
        SystemBridge.updateMedia(primary)
    }

    private fun syncNotifications() {
        val items = try {
            activeNotifications.orEmpty()
                .filter { !it.isOngoing && it.packageName != packageName }
                .mapNotNull { sbn ->
                    val extras = sbn.notification.extras
                    val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
                    val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
                    if (title.isEmpty() && text.isEmpty()) null
                    else NotifItem(sbn.key, sbn.packageName, title, text, sbn.postTime)
                }
                .sortedByDescending { it.time }
        } catch (_: Exception) {
            emptyList()
        }
        SystemBridge.updateNotifications(items)
    }
}
