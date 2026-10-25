package me.shadovxw.glyph.data.groups

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.shadovxw.glyph.data.AppEntry
import me.shadovxw.glyph.data.SettingsRepository

/** The Apps page's groups: loaded from DataStore, edited from the page, saved after every change. */
class GroupsRepository(private val settings: SettingsRepository, private val scope: CoroutineScope) {
    private val _groups = MutableStateFlow<List<AppGroup>>(emptyList())
    val groups: StateFlow<List<AppGroup>> = _groups.asStateFlow()

    private val loading = scope.launch {
        settings.appGroups.first()?.let { _groups.value = AppGroups.parse(it) }
    }

    /** First run only: make the design's starting groups from the apps this phone has. */
    fun ensureDefaults(apps: List<AppEntry>) {
        if (apps.isEmpty()) return
        scope.launch {
            loading.join()
            if (settings.appGroups.first() == null) save(AppGroups.defaults(apps))
        }
    }

    val actions = GroupActions(
        rename = { id, name -> change { g -> if (g.id == id) AppGroup(g.id, name, g.packages) else g } },
        removeApp = { id, pkg -> change { g -> if (g.id == id) AppGroup(g.id, g.name, g.packages - pkg) else g } },
        toggleApp = { id, pkg ->
            change { g ->
                when {
                    g.id != id -> g
                    pkg in g.packages -> AppGroup(g.id, g.name, g.packages - pkg)
                    else -> AppGroup(g.id, g.name, g.packages + pkg)
                }
            }
        },
        newGroup = {
            val id = "g${System.currentTimeMillis()}"
            save(_groups.value + AppGroup(id, "New group", emptyList()))
            id
        },
    )

    private fun change(transform: (AppGroup) -> AppGroup) = save(_groups.value.map(transform))

    private fun save(list: List<AppGroup>) {
        _groups.value = list
        scope.launch { settings.setAppGroups(AppGroups.toJson(list)) }
    }
}
