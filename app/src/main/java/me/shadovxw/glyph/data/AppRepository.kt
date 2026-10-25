package me.shadovxw.glyph.data

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserHandle
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** Installed launcher apps, loaded off the main thread and refreshed when packages change. */
class AppRepository(private val context: Context) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps.asStateFlow()

    suspend fun refresh() {
        _apps.value = withContext(Dispatchers.Default) { load() }
    }

    private fun load(): List<AppEntry> {
        val iconPx = (ICON_DP * context.resources.displayMetrics.density).roundToInt()
        return launcherApps.getActivityList(null, Process.myUserHandle())
            .map { info ->
                AppEntry(
                    label = info.label.toString(),
                    packageName = info.componentName.packageName,
                    component = info.componentName,
                    androidCategory = info.applicationInfo.category,
                    icon = info.getBadgedIcon(0).toBitmap(iconPx, iconPx).asImageBitmap(),
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    /** Reloads whenever an app is installed, removed or updated. Call the result to stop. */
    fun observe(scope: CoroutineScope): () -> Unit {
        val callback = object : LauncherApps.Callback() {
            private fun changed() {
                scope.launch { refresh() }
            }

            override fun onPackageRemoved(packageName: String?, user: UserHandle?) = changed()
            override fun onPackageAdded(packageName: String?, user: UserHandle?) = changed()
            override fun onPackageChanged(packageName: String?, user: UserHandle?) = changed()
            override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = changed()
            override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = changed()
        }
        launcherApps.registerCallback(callback)
        return { launcherApps.unregisterCallback(callback) }
    }

    private companion object {
        const val ICON_DP = 56
    }
}
