package me.shadovxw.glyph.launcher

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent

/** Starts [intent] in a new task; returns false when nothing can handle it. */
fun Context.startSafely(intent: Intent): Boolean = try {
    startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    true
} catch (_: ActivityNotFoundException) {
    false
}

private const val GoogleApp = "com.google.android.googlequicksearchbox"
private const val GoogleLens = "com.google.ar.lens"
private const val Chrome = "com.android.chrome"

/**
 * Search bar actions (DESIGN.md 3, screen 1). Voice, Lens and incognito were all checked on the 12R.
 * Chrome ignores the "open incognito" extra from other apps; its own launcher-shortcut action works.
 */
object SearchIntents {
    fun voice() = Intent(RecognizerIntent.ACTION_WEB_SEARCH)

    fun incognito() = Intent("org.chromium.chrome.browser.incognito.OPEN_PRIVATE_TAB").setPackage(Chrome)
}

/** Opens Google Lens (the standalone Lens app), falling back to the Google app's deep link. */
fun Context.openLens() {
    val lens = packageManager.getLaunchIntentForPackage(GoogleLens)
    if (lens != null && startSafely(lens)) return
    startSafely(Intent(Intent.ACTION_VIEW, Uri.parse("google://lens")).setPackage(GoogleApp))
}

/** Opens the Google app's search; falls back to the system web search. */
fun Context.openGoogleSearch() {
    val google = packageManager.getLaunchIntentForPackage(GoogleApp)
    if (google == null || !startSafely(google)) startSafely(Intent(Intent.ACTION_WEB_SEARCH))
}
