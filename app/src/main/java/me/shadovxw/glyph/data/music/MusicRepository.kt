package me.shadovxw.glyph.data.music

import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.shadovxw.glyph.services.GlyphNotificationListener
import java.net.URL

/**
 * What is playing, from the music app's media session (DESIGN.md 8). Android only shows other apps' sessions to an
 * app with an enabled notification listener, so this needs "Notification access". It prefers YouTube Music and
 * falls back to any other session that has a title.
 */
class MusicRepository(private val context: Context, private val scope: CoroutineScope) {
    private val manager = context.getSystemService(MediaSessionManager::class.java)
    private val listener = ComponentName(context, GlyphNotificationListener::class.java)

    private val _state = MutableStateFlow(MusicState())
    val state: StateFlow<MusicState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private var listening = false
    private var art: Pair<String, androidx.compose.ui.graphics.ImageBitmap>? = null
    private var artJob: Job? = null

    private val sessionsChanged = MediaSessionManager.OnActiveSessionsChangedListener { select(it.orEmpty()) }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
        override fun onSessionDestroyed() = refresh()
    }

    fun hasAccess(): Boolean = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    /** Call when Glyph comes forward; it picks up access that was just granted and sessions that came and went. */
    fun refresh() {
        if (!hasAccess()) {
            detach()
            _state.value = MusicState(MusicAccess.NeedsNotificationAccess, null)
            return
        }
        try {
            if (!listening) {
                manager.addOnActiveSessionsChangedListener(sessionsChanged, listener)
                listening = true
            }
            select(manager.getActiveSessions(listener))
        } catch (_: SecurityException) {
            detach()
            _state.value = MusicState(MusicAccess.NeedsNotificationAccess, null)
        }
    }

    private fun detach() {
        controller?.unregisterCallback(controllerCallback)
        controller = null
        if (listening) {
            runCatching { manager.removeOnActiveSessionsChangedListener(sessionsChanged) }
            listening = false
        }
    }

    private fun select(controllers: List<MediaController>) {
        fun hasTitle(c: MediaController) = !c.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE).isNullOrBlank()
        val chosen = controllers.firstOrNull { it.packageName == YouTubeMusic && hasTitle(it) }
            ?: controllers.firstOrNull { hasTitle(it) }
        if (chosen?.sessionToken != controller?.sessionToken) {
            controller?.unregisterCallback(controllerCallback)
            controller = chosen
            chosen?.registerCallback(controllerCallback)
        }
        publish()
    }

    private fun publish() {
        val c = controller
        val metadata = c?.metadata
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
        if (c == null || title.isNullOrBlank()) {
            _state.value = MusicState(MusicAccess.Ready, null)
            return
        }
        val playback = c.playbackState
        val like = playback?.find("like", "thumb_up", "thumbs_up", "heart", exclude = arrayOf("dislike", "thumb_down"))
        val dislike = playback?.find("dislike", "thumb_down", "thumbs_down")
        val shuffle = playback?.find("shuffle")
        val repeat = playback?.find("repeat")
        _state.value = MusicState(
            access = MusicAccess.Ready,
            session = MusicSession(
                appName = appLabel(c.packageName),
                packageName = c.packageName,
                title = title,
                artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
                    ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST).orEmpty(),
                album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM).orEmpty(),
                art = artFor(metadata),
                isPlaying = playback?.state == PlaybackState.STATE_PLAYING || playback?.state == PlaybackState.STATE_BUFFERING,
                positionMs = playback?.position?.coerceAtLeast(0) ?: 0,
                durationMs = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION).coerceAtLeast(0),
                reportedAt = playback?.lastPositionUpdateTime ?: SystemClock.elapsedRealtime(),
                speed = playback?.playbackSpeed ?: 1f,
                canLike = like != null,
                canDislike = dislike != null,
                liked = like.isActive(),
                disliked = dislike.isActive(),
                canShuffle = shuffle != null,
                shuffleOn = shuffle.isActive(),
                canRepeat = repeat != null,
                repeatOn = repeat.isActive(),
            ),
        )
    }

    // ---- controls ----

    fun playPause() {
        val c = controller ?: return
        val playing = c.playbackState?.state == PlaybackState.STATE_PLAYING
        if (playing) c.transportControls.pause() else c.transportControls.play()
    }

    /**
     * Opens the music app on the song that is playing, using the screen the app registered for its session.
     * Returns false when there is no session or the app did not register one.
     */
    fun openPlayer(): Boolean {
        val intent = controller?.sessionActivity ?: return false
        // Since Android 14 the sender must say it allows the start; without this the system drops it without an error.
        val options = ActivityOptions.makeBasic()
        if (Build.VERSION.SDK_INT >= 34) {
            options.setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
        }
        return runCatching { intent.send(context, 0, null, null, null, null, options.toBundle()) }.isSuccess
    }

    fun next() { controller?.transportControls?.skipToNext() }

    fun previous() { controller?.transportControls?.skipToPrevious() }

    fun like() = custom("like", "thumb_up", "thumbs_up", "heart", exclude = arrayOf("dislike", "thumb_down"))

    fun dislike() = custom("dislike", "thumb_down", "thumbs_down")

    fun shuffle() = custom("shuffle")

    fun repeat() = custom("repeat")

    private fun custom(vararg include: String, exclude: Array<String> = emptyArray()) {
        val c = controller ?: return
        val action = c.playbackState?.find(*include, exclude = exclude) ?: return
        c.transportControls.sendCustomAction(action, null)
    }

    /**
     * Music apps expose like, dislike, shuffle and repeat as custom actions with their own ids, so they are found by
     * name. [exclude] stops "like" matching "dislike".
     */
    private fun PlaybackState.find(vararg include: String, exclude: Array<String> = emptyArray()): PlaybackState.CustomAction? =
        customActions.firstOrNull { action ->
            val key = (action.action + " " + action.name).lowercase()
            include.any { it in key } && exclude.none { it in key }
        }

    /**
     * An action that offers to undo something ("Unlike", "Shuffle off") means that thing is currently on. This is a
     * best guess from the action's name; the exact names depend on the music app and are checked on the phone.
     */
    private fun PlaybackState.CustomAction?.isActive(): Boolean {
        val key = this?.let { (it.action + " " + it.name).lowercase() } ?: return false
        return listOf("unlike", "remove", " off", "_off", "disable", "selected", "filled").any { it in key }
    }

    // ---- art ----

    private fun artFor(metadata: MediaMetadata): androidx.compose.ui.graphics.ImageBitmap? {
        val bitmap = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        if (bitmap != null) return bitmap.asImageBitmap()

        val uri = listOf(
            MediaMetadata.METADATA_KEY_ALBUM_ART_URI,
            MediaMetadata.METADATA_KEY_ART_URI,
            MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI,
        ).firstNotNullOfOrNull { metadata.getString(it)?.takeIf { value -> value.isNotBlank() } } ?: return null

        art?.takeIf { it.first == uri }?.let { return it.second }
        artJob?.cancel()
        artJob = scope.launch {
            val loaded = withContext(Dispatchers.IO) { runCatching { load(uri) }.getOrNull() }
            if (loaded != null) {
                art = uri to loaded.asImageBitmap()
                publish()
            }
        }
        return null
    }

    private fun load(uri: String): Bitmap? {
        val parsed = Uri.parse(uri)
        val stream = when (parsed.scheme) {
            "http", "https" -> URL(uri).openStream()
            else -> context.contentResolver.openInputStream(parsed)
        } ?: return null
        val bitmap = stream.use { BitmapFactory.decodeStream(it) } ?: return null
        return if (bitmap.width > MaxArtWidth) {
            Bitmap.createScaledBitmap(bitmap, MaxArtWidth, bitmap.height * MaxArtWidth / bitmap.width, true)
        } else {
            bitmap
        }
    }

    private fun appLabel(packageName: String): String = runCatching {
        val info = context.packageManager.getApplicationInfo(packageName, 0)
        context.packageManager.getApplicationLabel(info).toString()
    }.getOrDefault(packageName)

    private companion object {
        const val YouTubeMusic = "com.google.android.apps.youtube.music"
        const val MaxArtWidth = 1080
    }
}
