package me.shadovxw.glyph.data

import android.content.ComponentName
import android.content.Intent
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap

/** One launchable app in the drawer, with its real (Play Store) icon already rendered. */
@Immutable
class AppEntry(
    val label: String,
    val packageName: String,
    val component: ComponentName,
    val androidCategory: Int,
    val icon: ImageBitmap,
) {
    val key: String = component.flattenToString()

    /** A-Z for the letter index, '#' for everything else. */
    val letter: Char = label.firstOrNull { it.isLetterOrDigit() }
        ?.uppercaseChar()
        ?.takeIf { it in 'A'..'Z' }
        ?: '#'

    fun launchIntent(): Intent = Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_LAUNCHER)
        .setComponent(component)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
}
