package me.shadovxw.glyph.data.groups

import androidx.compose.runtime.Immutable
import me.shadovxw.glyph.data.AppEntry
import org.json.JSONArray
import org.json.JSONObject

/** One group on the Apps page: a name and the packages in it, in the order the user put them. */
@Immutable
class AppGroup(val id: String, val name: String, val packages: List<String>)

/** What the Apps page does to the groups. */
class GroupActions(
    val rename: (id: String, name: String) -> Unit,
    val removeApp: (id: String, packageName: String) -> Unit,
    val toggleApp: (id: String, packageName: String) -> Unit,
    /** Makes an empty group and returns its id. */
    val newGroup: () -> String,
) {
    companion object {
        val None = GroupActions({ _, _ -> }, { _, _ -> }, { _, _ -> }, { "" })
    }
}

object AppGroups {
    fun toJson(groups: List<AppGroup>): String = JSONArray().also { array ->
        groups.forEach { g ->
            array.put(
                JSONObject()
                    .put("id", g.id)
                    .put("name", g.name)
                    .put("apps", JSONArray(g.packages)),
            )
        }
    }.toString()

    fun parse(json: String): List<AppGroup> = try {
        val array = JSONArray(json)
        (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            val apps = o.getJSONArray("apps")
            AppGroup(o.getString("id"), o.getString("name"), (0 until apps.length()).map { apps.getString(it) })
        }
    } catch (_: Exception) {
        emptyList()
    }

    /** The starting groups from the design (DESIGN.md 3, screen 5), as app names. Each name is matched to an installed app. */
    private val starting = listOf(
        "Food" to listOf("Zomato", "Blinkit", "Amazon", "GPay"),
        "Work" to listOf("Gmail", "Outlook", "Teams", "Calendar", "Classroom", "Authenticator"),
        "Dev & AI" to listOf("GitHub", "Claude", "ChatGPT", "Assistant", "Chrome", "Browser"),
        "Games" to listOf("Chess", "Sudoku", "Bubble Pop!", "Clash of Clans"),
        "Social" to listOf("WhatsApp", "Instagram", "Pinterest", "YouTube", "YT Music"),
        "System" to listOf("Settings", "Files", "Calculator", "Clock", "Google", "Authenticator"),
        "Photos" to listOf("Photos", "Camera", "Pinterest"),
        "Travel" to listOf("Chalo", "Maps", "Adani OneApp", "BookMyShow", "NoBrokerHood"),
    )

    /** Names the phone may use for the design's names (the Play Store calls GPay "Google Pay"). */
    private val aliases = mapOf("GPay" to "Google Pay", "Browser" to "Internet")

    private fun norm(text: String) = text.lowercase().filter { it.isLetterOrDigit() }

    /** The design's groups with only the apps this phone has; a group with none of them is left out. */
    fun defaults(apps: List<AppEntry>): List<AppGroup> {
        fun find(name: String): AppEntry? {
            for (candidate in listOfNotNull(name, aliases[name])) {
                val n = norm(candidate)
                apps.firstOrNull { norm(it.label) == n }?.let { return it }
                apps.firstOrNull { norm(it.label).startsWith(n) }?.let { return it }
                if (n.length >= 5) apps.firstOrNull { n in norm(it.label) }?.let { return it }
            }
            return null
        }
        return starting.mapIndexedNotNull { index, (name, wanted) ->
            val packages = wanted.mapNotNull { find(it)?.packageName }.distinct()
            if (packages.isEmpty()) null else AppGroup("g$index", name, packages)
        }
    }
}
