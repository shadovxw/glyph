package me.shadovxw.glyph.launcher

import android.Manifest
import android.app.ActivityOptions
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import androidx.compose.ui.geometry.Rect
import me.shadovxw.glyph.data.AppEntry
import me.shadovxw.glyph.data.AppRepository
import me.shadovxw.glyph.data.groups.GroupsRepository
import me.shadovxw.glyph.data.SettingsRepository
import me.shadovxw.glyph.data.chat.ChatActions
import me.shadovxw.glyph.data.chat.Chats
import me.shadovxw.glyph.BuildConfig
import me.shadovxw.glyph.data.github.GitHubActions
import me.shadovxw.glyph.data.github.GitHubRepository
import me.shadovxw.glyph.data.orders.Orders
import me.shadovxw.glyph.data.music.DevicesRepository
import me.shadovxw.glyph.data.music.MusicActions
import me.shadovxw.glyph.data.music.MusicRepository
import me.shadovxw.glyph.data.suggestedApps
import me.shadovxw.glyph.data.today.SourceState
import me.shadovxw.glyph.data.today.TodayActions
import me.shadovxw.glyph.data.today.TodayData
import me.shadovxw.glyph.data.today.TodayRepository
import me.shadovxw.glyph.services.GlyphNotificationListener
import me.shadovxw.glyph.shortcuts.Flashlight
import me.shadovxw.glyph.shortcuts.ShortcutActions
import me.shadovxw.glyph.tiles.GpayScanActivity
import me.shadovxw.glyph.tiles.HomelabCheck
import me.shadovxw.glyph.tiles.HotspotQrActivity
import me.shadovxw.glyph.tiles.TileOffers
import me.shadovxw.glyph.theme.GlyphTheme
import me.shadovxw.glyph.widgets.WidgetHostController

private const val YouTubeMusic = "com.google.android.apps.youtube.music"
private const val WhatsApp = "com.whatsapp"
private const val RequestBindWidget = 7101
private const val RequestConfigureWidget = 7102

class HomeActivity : ComponentActivity() {
    /** Fires when the Home button is pressed while Glyph is already on screen (singleTask delivers a new intent). */
    private val homePresses = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private var stopObservingApps: (() -> Unit)? = null
    private lateinit var today: TodayRepository
    private lateinit var music: MusicRepository
    private lateinit var devices: DevicesRepository
    private lateinit var whatsAppWidget: WidgetHostController
    private lateinit var github: GitHubRepository
    private lateinit var flashlight: Flashlight
    private lateinit var groups: GroupsRepository

    /** True only during the one-time sequence of first-run prompts (location, then calendar, then steps). */
    private var firstRunChain = false

    private val locationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        today.refresh(force = true)
        if (firstRunChain) calendarPermission.launch(Manifest.permission.READ_CALENDAR)
        else if (!granted) openAppSettingsIfBlocked(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    private val calendarPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        today.refresh(force = true)
        if (firstRunChain) requestSteps()
        else if (!granted) openAppSettingsIfBlocked(Manifest.permission.READ_CALENDAR)
    }

    private val stepsPermission = registerForActivityResult(PermissionController.createRequestPermissionResultContract()) {
        firstRunChain = false
        today.refresh(force = true)
    }

    /** For the Focus tile's "done" notification. Asked once, after the tiles were offered. */
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    private val bluetoothPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        devices.refresh()
        if (!granted) openAppSettingsIfBlocked(Manifest.permission.BLUETOOTH_CONNECT)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = SettingsRepository(applicationContext)
        val appRepository = AppRepository(applicationContext)
        today = TodayRepository(applicationContext, settings, lifecycleScope)
        Chats.init(applicationContext)
        music = MusicRepository(applicationContext, lifecycleScope)
        devices = DevicesRepository(applicationContext)
        whatsAppWidget = WidgetHostController(
            context = applicationContext,
            provider = ComponentName(WhatsApp, "com.whatsapp.appwidget.WidgetProvider"),
            prefsKey = "whatsapp_widget_id",
        )
        github = GitHubRepository(BuildConfig.GITHUB_ACCOUNTS, lifecycleScope)
        flashlight = Flashlight(applicationContext)
        groups = GroupsRepository(settings, lifecycleScope)
        lifecycleScope.launch { appRepository.refresh() }
        // First run: the Apps page's starting groups need the installed apps, so they are made once the list is there.
        lifecycleScope.launch { groups.ensureDefaults(appRepository.apps.first { it.isNotEmpty() }) }
        stopObservingApps = appRepository.observe(lifecycleScope)

        val actions = TodayActions(
            grantLocation = { locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
            grantCalendar = { calendarPermission.launch(Manifest.permission.READ_CALENDAR) },
            grantUsageAccess = {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:$packageName")))
            },
            grantSteps = { requestSteps() },
        )

        val musicActions = MusicActions(
            allowNotificationAccess = {
                val component = ComponentName(this, GlyphNotificationListener::class.java)
                startActivity(
                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                        .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, component.flattenToString()),
                )
            },
            allowBluetooth = { bluetoothPermission.launch(Manifest.permission.BLUETOOTH_CONNECT) },
            // The song's own screen when the app offers one, otherwise just the app.
            openMusicApp = {
                if (!music.openPlayer()) {
                    packageManager.getLaunchIntentForPackage(YouTubeMusic)?.let { startSafely(it) }
                }
            },
            openBluetoothSettings = { startSafely(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) },
            playPause = music::playPause,
            next = music::next,
            previous = music::previous,
            like = music::like,
            dislike = music::dislike,
            shuffle = music::shuffle,
            repeat = music::repeat,
        )

        val chatActions = ChatActions(
            allowAccess = musicActions.allowNotificationAccess,
            openWhatsApp = {
                packageManager.getLaunchIntentForPackage(WhatsApp)?.let { startSafely(it) }
            },
            // WhatsApp's own "send to" picker, with nothing to send: pick a person and you land in a new chat.
            newChat = {
                startSafely(
                    Intent(Intent.ACTION_SEND).setPackage(WhatsApp).setType("text/plain").putExtra(Intent.EXTRA_TEXT, " "),
                )
            },
            addWidget = { addWhatsAppWidget() },
            removeWidget = { whatsAppWidget.remove() },
        )

        val shortcutActions = ShortcutActions(
            showHotspotQr = { startSafely(Intent(this, HotspotQrActivity::class.java)) },
            openHotspotSettings = {
                // The hotspot page itself where the phone has one; otherwise the wireless settings list.
                val opened = try {
                    startSafely(Intent().setClassName("com.android.settings", "com.android.settings.TetherSettings"))
                } catch (_: SecurityException) {
                    false
                }
                if (!opened) startSafely(Intent(Settings.ACTION_WIRELESS_SETTINGS))
            },
            toggleTorch = flashlight::toggle,
            scanGpay = { startSafely(Intent(this, GpayScanActivity::class.java)) },
            openHomelab = { HomelabCheck.address?.let { startSafely(Intent(Intent.ACTION_VIEW, it)) } },
        )

        val githubActions = GitHubActions(
            openUrl = { url -> startSafely(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
            refresh = { github.refresh(force = true) },
        )

        // One quick read up front so the first frame already has the right theme.
        val initialMode = runBlocking { settings.themeMode.first() }
        setContent {
            val themeMode by settings.themeMode.collectAsStateWithLifecycle(initialValue = initialMode)
            // "Auto" follows the phone: the activity is recreated when the phone switches, so this is always current.
            val dark = themeMode.resolve(isSystemInDarkTheme())
            val apps by appRepository.apps.collectAsStateWithLifecycle()
            val counts by settings.launchCounts.collectAsStateWithLifecycle(initialValue = emptyMap())
            val todayData by today.state.collectAsStateWithLifecycle()
            val motion by settings.motion.collectAsStateWithLifecycle(initialValue = true)
            val chatsState by Chats.state.collectAsStateWithLifecycle()
            val whatsAppWidgetId by whatsAppWidget.boundId.collectAsStateWithLifecycle()
            val groupList by groups.groups.collectAsStateWithLifecycle()
            val githubState by github.state.collectAsStateWithLifecycle()
            val torchOn by flashlight.on.collectAsStateWithLifecycle()
            val musicState by music.state.collectAsStateWithLifecycle()
            val devicesState by devices.state.collectAsStateWithLifecycle()
            val suggested = remember(apps, counts) { suggestedApps(apps, counts) }
            val scope = rememberCoroutineScope()
            // System bar icons follow Glyph's mode, not the phone's theme.
            DisposableEffect(dark) {
                val bars = if (dark) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
                onDispose {}
            }
            GlyphTheme(dark = dark) {
                HomeScreen(
                    themeMode = themeMode,
                    onSetThemeMode = { scope.launch { settings.setThemeMode(it) } },
                    motion = motion,
                    onSetMotion = { scope.launch { settings.setMotion(it) } },
                    apps = apps,
                    suggested = suggested,
                    onLaunchApp = { app, iconBounds ->
                        launchApp(app, iconBounds)
                        scope.launch { settings.recordLaunch(app.packageName) }
                    },
                    homePresses = homePresses,
                    today = todayData,
                    todayActions = actions,
                    music = musicState,
                    devices = devicesState,
                    musicActions = musicActions,
                    chats = chatsState,
                    chatActions = chatActions,
                    whatsAppWidgets = whatsAppWidget,
                    whatsAppWidgetId = whatsAppWidgetId,
                    groups = groupList,
                    groupActions = groups.actions,
                    github = githubState,
                    githubActions = githubActions,
                    onOpenShade = ::openNotificationShade,
                    torchOn = torchOn,
                    shortcutActions = shortcutActions,
                )
            }
        }

        askForPermissionsOnce()
    }

    override fun onStart() {
        super.onStart()
        // Coming back from a Settings screen (usage access, app permissions) should pick up the change straight away.
        today.refresh(force = today.state.value.needsAttention())
        // Notification access or Bluetooth may have just been switched on in Settings.
        music.refresh()
        devices.start()
        whatsAppWidget.startListening()
        Chats.setStatus(
            installed = packageManager.getLaunchIntentForPackage(WhatsApp) != null,
            hasAccess = music.hasAccess(),
        )
        Orders.setAccess(music.hasAccess())
        github.refresh()
        flashlight.start()
        if (!firstRunChain) offerTiles()
    }

    /** Starts an app. With its icon's place on screen, the app grows out of that icon (Android's scale-up animation). */
    private fun launchApp(app: AppEntry, iconBounds: Rect?) {
        val options = iconBounds?.takeIf { !it.isEmpty }?.let {
            ActivityOptions.makeScaleUpAnimation(
                window.decorView, it.left.toInt(), it.top.toInt(), it.width.toInt(), it.height.toInt(),
            ).toBundle()
        }
        try {
            startActivity(app.launchIntent(), options)
        } catch (_: ActivityNotFoundException) {
            // The app was removed since the list was made.
        }
    }

    /** Once per install: "Add tile?" for each Quick Settings tile, then the notification permission for Focus. */
    private fun offerTiles() {
        TileOffers.offerOnce(this) {
            val prefs = getSharedPreferences("glyph", MODE_PRIVATE)
            if (!prefs.getBoolean("asked_notifications", false) &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                prefs.edit().putBoolean("asked_notifications", true).apply()
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    /**
     * Swipe down on home: the phone's own notification shade, with its icons and quick settings. Android has no public
     * call for it, so this uses the status bar service (allowed by the EXPAND_STATUS_BAR permission).
     */
    private fun openNotificationShade() {
        try {
            val bar = getSystemService("statusbar")
            Class.forName("android.app.StatusBarManager").getMethod("expandNotificationsPanel").invoke(bar)
        } catch (e: Exception) {
            Log.d("Glyph", "Could not open the notification shade", e)
        }
    }

    override fun onStop() {
        devices.stop()
        whatsAppWidget.stopListening()
        flashlight.stop()
        super.onStop()
    }

    /** Adding WhatsApp's widget: the system asks the user to allow it, then the widget may have its own set-up screen. */
    private fun addWhatsAppWidget() {
        when (val step = whatsAppWidget.begin()) {
            is WidgetHostController.Step.Bound -> whatsAppWidget.configureOrFinish(this, step.id, RequestConfigureWidget)
            is WidgetHostController.Step.NeedsConsent -> startActivityForResult(step.intent, RequestBindWidget)
            WidgetHostController.Step.Unavailable -> Unit
        }
    }

    @Deprecated("The widget host API reports through onActivityResult")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            RequestBindWidget -> whatsAppWidget.onBindResult(this, resultCode == RESULT_OK, RequestConfigureWidget)
            RequestConfigureWidget -> whatsAppWidget.onConfigureResult(resultCode == RESULT_OK)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && Intent.CATEGORY_HOME in (intent.categories ?: emptySet())) {
            homePresses.tryEmit(Unit)
        }
    }

    override fun onDestroy() {
        stopObservingApps?.invoke()
        super.onDestroy()
    }

    /** The first time Glyph runs, ask for location, calendar and steps one after another. Later, the cards ask. */
    private fun askForPermissionsOnce() {
        val prefs = getSharedPreferences("glyph", MODE_PRIVATE)
        if (prefs.getBoolean("asked_permissions", false)) return
        prefs.edit().putBoolean("asked_permissions", true).apply()
        firstRunChain = true
        locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    private fun requestSteps() {
        if (today.steps.isAvailable()) {
            stepsPermission.launch(today.steps.permissions)
        } else {
            firstRunChain = false
        }
    }

    /** After a "don't ask again" denial the system shows no dialog, so send the user to Glyph's settings page instead. */
    private fun openAppSettingsIfBlocked(permission: String) {
        if (!shouldShowRequestPermissionRationale(permission)) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }
    }

    private fun TodayData.needsAttention(): Boolean = listOf(weatherState, screenTimeState, calendarState, stepsState)
        .any { it == SourceState.NeedsPermission }
}
