package me.shadovxw.glyph.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** DESIGN.md 2.5: line icons, 1.5 stroke, round caps and joins, 24 viewBox, no fill. Tint at the call site. */
object GlyphIcons {
    val Phone: ImageVector by lazy {
        line("Phone", "M5 4h4l2 5-2.5 1.5a11 11 0 0 0 5 5L15 13l5 2v4a2 2 0 0 1-2 2A16 16 0 0 1 3 6a2 2 0 0 1 2-2z")
    }
    val Messages: ImageVector by lazy {
        line("Messages", "M4 5h16v11H9l-5 4V5z")
    }
    val Camera: ImageVector by lazy {
        line("Camera", "M4 8h3l2-3h6l2 3h3v11H4z M12 9.5a3.5 3.5 0 1 0 0 7a3.5 3.5 0 1 0 0-7z")
    }
    val Apps: ImageVector by lazy {
        line("Apps", "M5 5h5v5H5z M14 5h5v5h-5z M5 14h5v5H5z M14 14h5v5h-5z")
    }

    val Moon: ImageVector by lazy {
        line("Moon", "M20 14.5A8 8 0 1 1 9.5 4a6.5 6.5 0 0 0 10.5 10.5z")
    }
    /** "Follow the system": a circle split down the middle (half light, half dark). */
    val AutoTheme: ImageVector by lazy {
        line("AutoTheme", "M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18z M12 3v18 M12 7.5a4.5 4.5 0 0 1 0 9")
    }
    val Sun: ImageVector by lazy {
        line(
            "Sun",
            "M12 8a4 4 0 1 0 0 8a4 4 0 1 0 0-8z M12 2.5v2 M12 19.5v2 M2.5 12h2 M19.5 12h2 " +
                "M5.3 5.3l1.4 1.4 M17.3 17.3l1.4 1.4 M5.3 18.7l1.4-1.4 M17.3 6.7l1.4-1.4",
        )
    }
    val Search: ImageVector by lazy {
        line("Search", "M11 4.5a6.5 6.5 0 1 0 0 13a6.5 6.5 0 1 0 0-13z M16 16l4.5 4.5")
    }
    val Mic: ImageVector by lazy {
        line("Mic", "M12 3.5a3 3 0 0 0-3 3v5a3 3 0 0 0 6 0v-5a3 3 0 0 0-3-3z M6 11.5a6 6 0 0 0 12 0 M12 17.5v3 M9 20.5h6")
    }
    val Lens: ImageVector by lazy {
        line("Lens", "M4 8V4h4 M16 4h4v4 M20 16v4h-4 M8 20H4v-4 M12 9a3 3 0 1 0 0 6a3 3 0 1 0 0-6z")
    }
    val Incognito: ImageVector by lazy {
        line(
            "Incognito",
            "M5 11l2-6h10l2 6 M3 11h18 M7.5 13.5a2.5 2.5 0 1 0 0 5a2.5 2.5 0 1 0 0-5z " +
                "M16.5 13.5a2.5 2.5 0 1 0 0 5a2.5 2.5 0 1 0 0-5z M10 16h4",
        )
    }

    // Drawer custom-icon set (DESIGN.md 6): simple line marks, not the brands' own logos.
    val WhatsApp: ImageVector by lazy {
        line(
            "WhatsApp",
            "M4 20l1.3-4.2A8 8 0 1 1 8.2 18.8z M9.2 9.2c.2 2.6 2.4 4.8 5 5.2l1.2-1.2-1.8-1-.9.7c-.9-.4-1.7-1.2-2.1-2.1l.7-.9-1-1.8z",
        )
    }
    val Instagram: ImageVector by lazy {
        line(
            "Instagram",
            "M7 3.5h10a3.5 3.5 0 0 1 3.5 3.5v10a3.5 3.5 0 0 1-3.5 3.5H7A3.5 3.5 0 0 1 3.5 17V7A3.5 3.5 0 0 1 7 3.5z " +
                "M12 8.5a3.5 3.5 0 1 0 0 7a3.5 3.5 0 1 0 0-7z M17.2 6.8h.01",
        )
    }
    val Chess: ImageVector by lazy {
        line(
            "Chess",
            "M12 4a2.5 2.5 0 1 0 0 5a2.5 2.5 0 1 0 0-5z M10.5 9.5h3 M9.6 10.5c0 3-1.6 4-2.6 6h10c-1-2-2.6-3-2.6-6 " +
                "M6 19.5h12 M7 16.5h10",
        )
    }
    val Pinterest: ImageVector by lazy {
        line(
            "Pinterest",
            "M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18z M10.8 17.5l1.4-6.5 M9 11c0-2.2 1.6-3.4 3.2-3.4 1.8 0 3 1.2 3 2.9 " +
                "0 2-1.3 3.3-2.8 3.3-1 0-1.6-.5-1.9-1",
        )
    }
    // Today app shortcuts (DESIGN.md 3, screen 1): plain marks, not the brands' logos.
    val GPay: ImageVector by lazy { line("GPay", "M3 7h18v12H3z M3 11h18 M7 15h3") }
    val MapPin: ImageVector by lazy {
        line("Maps", "M12 21s-6.5-6.2-6.5-11a6.5 6.5 0 0 1 13 0c0 4.8-6.5 11-6.5 11z M12 7.5a2.5 2.5 0 1 0 0 5a2.5 2.5 0 1 0 0-5z")
    }
    val Teams: ImageVector by lazy {
        line("Teams", "M3.5 7h11v11h-11z M6.5 10h5 M9 10v5.5 M17.5 8.5a1.8 1.8 0 1 0 0-3.6a1.8 1.8 0 0 0 0 3.6z M15.5 11h5v5a2.5 2.5 0 0 1-2.5 2.5")
    }
    val Outlook: ImageVector by lazy {
        line("Outlook", "M9 6h11v12H9z M9 9l5.5 4L20 9 M6 9.5a2.8 2.8 0 1 0 0 5.6a2.8 2.8 0 0 0 0-5.6z")
    }
    val Gmail: ImageVector by lazy { line("Gmail", "M3 6.5h18v11H3z M3 6.5l9 7l9-7") }
    val Authenticator: ImageVector by lazy {
        line("Authenticator", "M12 3l7 3v5.5c0 4.5-3 8-7 9.5c-4-1.5-7-5-7-9.5V6z M9 12l2.2 2.2L15.5 10")
    }
    // Music player (DESIGN.md 3, screen 2)
    val Play: ImageVector by lazy { line("Play", "M7 4.5l12 7.5L7 19.5z") }
    val Pause: ImageVector by lazy { line("Pause", "M7 5h3.5v14H7z M13.5 5H17v14h-3.5z") }
    val Next: ImageVector by lazy { line("Next", "M6 5l9 7l-9 7z M18 5v14") }
    val Previous: ImageVector by lazy { line("Previous", "M18 5l-9 7l9 7z M6 5v14") }
    val Shuffle: ImageVector by lazy {
        line("Shuffle", "M3 7h4l10 10h4 M3 17h4l3-3 M14 10l3-3h4 M18.5 4.5L21 7l-2.5 2.5 M18.5 14.5L21 17l-2.5 2.5")
    }
    val Repeat: ImageVector by lazy {
        line("Repeat", "M4 11V9a3 3 0 0 1 3-3h13 M17 3l3 3-3 3 M20 13v2a3 3 0 0 1-3 3H4 M7 21l-3-3 3-3")
    }
    val ThumbUp: ImageVector by lazy {
        line("ThumbUp", "M7 11v9H4v-9z M7 11l4-7c1.5 0 2.5 1 2.5 2.5L13 10h6a2 2 0 0 1 2 2.3l-1.2 6A2 2 0 0 1 17.8 20H7")
    }
    val ThumbDown: ImageVector by lazy {
        line("ThumbDown", "M7 13V4H4v9z M7 13l4 7c1.5 0 2.5-1 2.5-2.5L13 14h6a2 2 0 0 0 2-2.3l-1.2-6A2 2 0 0 0 17.8 4H7")
    }
    val Volume: ImageVector by lazy {
        line("Volume", "M4 9.5h3.5L12 6v12l-4.5-3.5H4z M15.5 9a4 4 0 0 1 0 6 M18 6.5a7.5 7.5 0 0 1 0 11")
    }
    val Earbuds: ImageVector by lazy {
        line(
            "Earbuds",
            "M8 4.5A2.5 2.5 0 0 1 10.5 7v9a2 2 0 0 1-4 0V7A2.5 2.5 0 0 1 8 4.5z " +
                "M16 4.5A2.5 2.5 0 0 0 13.5 7v9a2 2 0 0 0 4 0V7A2.5 2.5 0 0 0 16 4.5z",
        )
    }
    val Headphones: ImageVector by lazy {
        line("Headphones", "M4 14v-2a8 8 0 0 1 16 0v2 M4 14h3v6H4z M17 14h3v6h-3z")
    }
    val PhoneSpeaker: ImageVector by lazy {
        line("PhoneSpeaker", "M8 3h8a1.5 1.5 0 0 1 1.5 1.5v15A1.5 1.5 0 0 1 16 21H8a1.5 1.5 0 0 1-1.5-1.5v-15A1.5 1.5 0 0 1 8 3z M12 12.5a2 2 0 1 0 0 4a2 2 0 0 0 0-4z M12 6.5h.01")
    }
    // GitHub page
    val Branch: ImageVector by lazy {
        line("Branch", "M6 3v12 M6 15a3 3 0 1 0 0 6a3 3 0 0 0 0-6z M18 3a3 3 0 1 0 0 6a3 3 0 0 0 0-6z M18 9c0 4-6 4-12 6")
    }
    val Check: ImageVector by lazy { line("Check", "M5 12.5l4.5 4.5L19 7") }
    val Cross: ImageVector by lazy { line("Cross", "M6 6l12 12 M18 6L6 18") }
    /** Checks still running: a circle that is not closed. */
    val Running: ImageVector by lazy { line("Running", "M12 4a8 8 0 1 0 8 8") }

    // Apps page
    val Plus: ImageVector by lazy { line("Plus", "M12 5v14 M5 12h14") }
    val Minus: ImageVector by lazy { line("Minus", "M6 12h12", width = 2f) }

    // Shortcuts popup (swipe down on home)
    val Qr: ImageVector by lazy {
        line("Qr", "M4 4h6v6H4z M14 4h6v6h-6z M4 14h6v6H4z M14 14h2v2h-2z M18 14h2 M14 18h2 M18 18h2v2h-2z")
    }
    val Timer: ImageVector by lazy { line("Timer", "M12 5a8 8 0 1 0 0 16a8 8 0 1 0 0-16z M12 9v4l2.5 1.5 M9.5 2.5h5") }
    val Server: ImageVector by lazy { line("Server", "M4 5h16v5H4z M4 14h16v5H4z M7.5 7.5h.01 M7.5 16.5h.01", width = 2f) }
    val Scan: ImageVector by lazy { line("Scan", "M4 8V4h4 M16 4h4v4 M20 16v4h-4 M8 20H4v-4 M7 12h10") }
    val Flashlight: ImageVector by lazy { line("Flashlight", "M9 3h6v5l-1.5 2v11h-3V10L9 8z M12 13v2") }
    val Hotspot: ImageVector by lazy {
        line("Hotspot", "M12 13a1.5 1.5 0 1 0 0 3a1.5 1.5 0 0 0 0-3z M8.7 12a4.7 4.7 0 0 1 6.6 0 M6 9.5a8.5 8.5 0 0 1 12 0 M12 16v5")
    }

    // WhatsApp page: a new chat is a speech bubble with a plus.
    val NewChat: ImageVector by lazy { line("NewChat", "M4 5h16v11H9l-5 4V5z M12 8v5 M9.5 10.5h5") }
    val MoreVert: ImageVector by lazy {
        line("MoreVert", "M12 5.5h.01 M12 12h.01 M12 18.5h.01", width = 3f)
    }

    private fun line(name: String, path: String, width: Float = 1.5f): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).addPath(
            pathData = PathParser().parsePathString(path).toNodes(),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = width,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ).build()
}
