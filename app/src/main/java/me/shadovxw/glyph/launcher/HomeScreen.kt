package me.shadovxw.glyph.launcher

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import me.shadovxw.glyph.data.AppEntry
import me.shadovxw.glyph.data.ThemeMode
import me.shadovxw.glyph.data.chat.ChatActions
import me.shadovxw.glyph.data.chat.ChatsState
import me.shadovxw.glyph.data.github.GitHubActions
import me.shadovxw.glyph.data.github.GitHubState
import me.shadovxw.glyph.data.groups.AppGroup
import me.shadovxw.glyph.data.groups.GroupActions
import me.shadovxw.glyph.data.music.DevicesState
import me.shadovxw.glyph.data.music.MusicActions
import me.shadovxw.glyph.data.music.MusicState
import me.shadovxw.glyph.data.today.TodayActions
import me.shadovxw.glyph.data.today.TodayData
import me.shadovxw.glyph.drawer.AppDrawer
import me.shadovxw.glyph.drawer.DrawerState
import me.shadovxw.glyph.drawer.rememberDrawerState
import me.shadovxw.glyph.shortcuts.ShortcutActions
import me.shadovxw.glyph.shortcuts.ShortcutsPopup
import me.shadovxw.glyph.shortcuts.rememberShortcutsPopupState
import me.shadovxw.glyph.screens.GitHubScreen
import me.shadovxw.glyph.screens.MusicScreen
import me.shadovxw.glyph.screens.PlaceholderScreen
import me.shadovxw.glyph.screens.TodayScreen
import me.shadovxw.glyph.screens.WhatsAppScreen
import me.shadovxw.glyph.screens.AppsScreen
import me.shadovxw.glyph.screens.apps.AddAppsSheet
import me.shadovxw.glyph.theme.Glyph
import me.shadovxw.glyph.theme.GlyphDimens
import me.shadovxw.glyph.widgets.WidgetHostController

/**
 * Lets a scrolling page open the drawer: once the page has scrolled as far as it can, the rest of an upward
 * drag pulls the drawer up. While the drawer is part-way open, a downward drag closes it before the page scrolls.
 */
private fun drawerPullConnection(drawer: DrawerState, shade: ShadePull, heightPx: () -> Float) = object : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (drawer.progress > 0f) shade.block()
        if (available.y > 0f && drawer.progress > 0f && drawer.progress < 1f) {
            drawer.dragBy(-available.y / heightPx())
            return Offset(0f, available.y)
        }
        return Offset.Zero
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.UserInput) return Offset.Zero
        if (available.y < 0f) {
            shade.block()
            drawer.dragBy(-available.y / heightPx())
            return Offset(0f, available.y)
        }
        // A page already at its top that is still being pulled down: that is the pull for the notification shade.
        if (available.y > 0f) shade.add(available.y)
        return Offset.Zero
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (drawer.progress > 0f && drawer.progress < 1f) {
            drawer.settle(-available.y / heightPx())
            return available
        }
        return Velocity.Zero
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        shade.release()
        return Velocity.Zero
    }
}

/**
 * Adds up a downward pull on home (from the page itself or from the root drag). [onMove] gets the running distance so
 * the shortcuts popup can follow the finger; [onRelease] gets the final distance. A gesture that touched the drawer
 * never counts.
 */
private class ShadePull(private val onMove: (Float) -> Unit, private val onRelease: (Float) -> Unit) {
    private var distance = 0f
    private var blocked = false

    fun add(dy: Float) {
        if (blocked) return
        distance = (distance + dy).coerceAtLeast(0f)
        onMove(distance)
    }

    fun block() {
        if (blocked) return
        blocked = true
        if (distance > 0f) onMove(0f)
        distance = 0f
    }

    fun release() {
        val final = if (blocked) 0f else distance
        distance = 0f
        blocked = false
        onRelease(final)
    }
}

/** Fixed page order (DESIGN.md 3 and 2.6). */
private val PageNames = listOf("Today", "Music", "WhatsApp", "GitHub", "Apps")

/**
 * The home pager with its Orbit background, page indicator and dock, plus the app drawer that slides
 * up over it. A vertical drag anywhere on home pulls the drawer up; [homePresses] fires when the Home
 * button is pressed while Glyph is already showing (closes the drawer, returns to Today).
 */
@Composable
fun HomeScreen(
    themeMode: ThemeMode,
    onSetThemeMode: (ThemeMode) -> Unit,
    motion: Boolean,
    onSetMotion: (Boolean) -> Unit,
    apps: List<AppEntry>,
    suggested: List<AppEntry>,
    /** Starts an app. The rectangle is its icon's place on screen when there is one, so the app can grow out of it. */
    onLaunchApp: (AppEntry, Rect?) -> Unit,
    homePresses: Flow<Unit>,
    today: TodayData,
    todayActions: TodayActions,
    music: MusicState,
    devices: DevicesState,
    musicActions: MusicActions,
    chats: ChatsState,
    chatActions: ChatActions,
    whatsAppWidgets: WidgetHostController,
    whatsAppWidgetId: Int?,
    groups: List<AppGroup>,
    groupActions: GroupActions,
    github: GitHubState,
    githubActions: GitHubActions,
    onOpenShade: () -> Unit,
    torchOn: Boolean,
    shortcutActions: ShortcutActions,
) {
    val c = Glyph.colors
    val pager = rememberPagerState { PageNames.size }
    val drawer = rememberDrawerState()
    val popup = rememberShortcutsPopupState()
    val scope = rememberCoroutineScope()
    var heightPx by remember { mutableFloatStateOf(1f) }
    val openShade by rememberUpdatedState(onOpenShade)
    val systemDark = isSystemInDarkTheme()
    var switcherOpen by remember { mutableStateOf(false) }
    var pickingGroup by remember { mutableStateOf<String?>(null) }
    var indicatorBounds by remember { mutableStateOf(Rect.Zero) }

    // A pull down on home, measured in dp: past SHORTCUTS_DP the shortcuts popup stays; past SHADE_DP it hands over to
    // the system notification shade; the popup is drawn along the way so the user sees which one they are about to get.
    val density = LocalDensity.current
    val shade = remember(density) {
        val revealPx = with(density) { 150.dp.toPx() }
        val shortcutsPx = with(density) { ShortcutsPullDp.dp.toPx() }
        val shadePx = with(density) { ShadePullDp.dp.toPx() }
        ShadePull(
            onMove = { d ->
                if (!popup.isOpen) {
                    val fade = 1f - ((d - 0.7f * shadePx) / (0.3f * shadePx)).coerceIn(0f, 1f) * 0.8f
                    popup.follow((d / revealPx).coerceIn(0f, 1f), fade)
                }
            },
            onRelease = { d ->
                when {
                    d >= shadePx -> {
                        popup.close()
                        openShade()
                    }
                    d >= shortcutsPx -> popup.open()
                    !popup.isOpen -> popup.close()
                }
            },
        )
    }

    // Pages that scroll (Today) hand drags they cannot use to the drawer (up) or the shortcuts/shade (down);
    // pages that do not scroll are covered by the draggable on the root box below.
    val pullConnection = remember(drawer, shade) { drawerPullConnection(drawer, shade) { heightPx } }

    // A launcher never exits on Back: close the popup, else the drawer, else return to Today, else do nothing.
    BackHandler {
        when {
            switcherOpen -> switcherOpen = false
            pickingGroup != null -> pickingGroup = null
            popup.isOpen -> popup.close()
            drawer.isOpen -> drawer.close()
            pager.currentPage != 0 -> scope.launch { pager.animateScrollToPage(0) }
        }
    }
    LaunchedEffect(homePresses) {
        homePresses.collect {
            switcherOpen = false
            pickingGroup = null
            popup.close()
            drawer.close()
            pager.animateScrollToPage(0)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(c.screenBg)
            .onSizeChanged { heightPx = it.height.toFloat().coerceAtLeast(1f) }
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState {
                    if (drawer.progress > 0f) shade.block() else shade.add(it)
                    drawer.dragBy(-it / heightPx)
                },
                onDragStopped = { velocity ->
                    drawer.settle(-velocity / heightPx)
                    shade.release()
                },
            ),
    ) {
        OrbitBackground(
            pagePosition = { pager.currentPage + pager.currentPageOffsetFraction },
            animate = motion,
        )
        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxSize().nestedScroll(pullConnection),
            flingBehavior = PagerDefaults.flingBehavior(
                state = pager,
                snapAnimationSpec = tween(GlyphSettleMillis, easing = GlyphEasing),
            ),
        ) { page ->
            CompositionLocalProvider(LocalGlyphPager provides pager, LocalGlyphPage provides page) {
                Box(Modifier.fillMaxSize().glyphPage(pager, page)) {
                    when (page) {
                        0 -> TodayScreen(themeMode, onSetThemeMode, motion, onSetMotion, today, todayActions)
                        1 -> MusicScreen(music, devices, musicActions)
                        2 -> WhatsAppScreen(chats, chatActions, whatsAppWidgets, whatsAppWidgetId)
                        3 -> GitHubScreen(github, githubActions)
                        4 -> AppsScreen(groups, apps, groupActions, onLaunch = { app, rect -> onLaunchApp(app, rect) }, onPickApps = { pickingGroup = it })
                        else -> PlaceholderScreen(PageNames[page])
                    }
                }
            }
        }
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PageIndicator(
                state = pager,
                names = PageNames,
                onTap = { switcherOpen = true },
                modifier = Modifier.onGloballyPositioned { indicatorBounds = it.boundsInRoot() },
            )
            // The track sits 22 dp above the dock (DESIGN.md 2.6): 10 dp of that is the indicator's own touch padding.
            Spacer(Modifier.height(12.dp))
            Dock()
            Spacer(Modifier.height(GlyphDimens.screenBottom))
        }

        if (drawer.progress > 0f) {
            AppDrawer(
                state = drawer,
                heightPx = heightPx,
                apps = apps,
                suggested = suggested,
                onLaunch = { app ->
                    drawer.snapClosed()
                    onLaunchApp(app, null)
                },
                modifier = Modifier.graphicsLayer { translationY = (1f - drawer.progress) * heightPx },
            )
        }

        // The add-apps sheet slides up over the dock (kept in place while it slides back down).
        val sheetGroup = groups.firstOrNull { it.id == pickingGroup }
        val sheetOpen = sheetGroup != null
        val sheetT by animateFloatAsState(if (sheetOpen) 0f else 1f, tween(420, easing = GlyphEasing), label = "addAppsSheet")
        var lastSheetGroup by remember { mutableStateOf<AppGroup?>(null) }
        if (sheetGroup != null) lastSheetGroup = sheetGroup
        val shownSheetGroup = sheetGroup ?: lastSheetGroup
        if (shownSheetGroup != null && sheetT < 1f) {
            AddAppsSheet(
                group = shownSheetGroup,
                apps = apps,
                onToggle = { groupActions.toggleApp(shownSheetGroup.id, it) },
                onDone = { pickingGroup = null },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxHeight(0.7f)
                    .graphicsLayer { translationY = sheetT * size.height },
            )
        }

        if (switcherOpen) {
            // The half circle opens just above the track: 10 dp of touch padding plus 2 dp of track below its flat edge.
            val trackTop = indicatorBounds.bottom - with(density) { 14.dp.toPx() }
            PageSwitcher(
                names = PageNames,
                current = pager.currentPage,
                anchor = Offset(indicatorBounds.center.x, trackTop),
                onPick = { page ->
                    switcherOpen = false
                    scope.launch { pager.animateScrollToPage(page, animationSpec = tween(GlyphSettleMillis, easing = GlyphEasing)) }
                },
                onClose = { switcherOpen = false },
            )
        }

        if (popup.isVisible) {
            ShortcutsPopup(
                state = popup,
                dark = themeMode.resolve(systemDark),
                onToggleMode = { onSetThemeMode(if (themeMode.resolve(systemDark)) ThemeMode.Light else ThemeMode.Dark) },
                torchOn = torchOn,
                actions = shortcutActions,
                onOpenShade = onOpenShade,
            )
        }
    }
}

/** How far (dp) a pull down must go to keep the shortcuts popup, and to go on to the notification shade instead. */
private const val ShortcutsPullDp = 56
private const val ShadePullDp = 300
