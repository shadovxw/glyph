package me.shadovxw.glyph.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import me.shadovxw.glyph.R

private fun geist(weight: Int) = Font(
    resId = R.font.geist,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

private fun geistMono(weight: Int) = Font(
    resId = R.font.geist_mono,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Geist = FontFamily(geist(200), geist(300), geist(400), geist(500))
val GeistMono = FontFamily(geistMono(300), geistMono(400))

/**
 * DESIGN.md 2.2: very small type throughout. Ranges in the spec use their midpoint here;
 * pass `.copy(fontSize = ...)` for a specific widget that needs the other end.
 * Colour is not set here; callers apply `Glyph.colors.ink` or `inkDim`.
 */
object GlyphType {
    val homeClock = TextStyle(fontFamily = Geist, fontWeight = FontWeight(200), fontSize = 56.sp, lineHeight = 56.sp, letterSpacing = (-0.045).em)
    val worldClock = TextStyle(fontFamily = Geist, fontWeight = FontWeight(200), fontSize = 48.sp, lineHeight = 48.sp, letterSpacing = (-0.045).em)
    val bigStat = TextStyle(fontFamily = Geist, fontWeight = FontWeight(200), fontSize = 28.sp)
    val mediumStat = TextStyle(fontFamily = Geist, fontWeight = FontWeight(200), fontSize = 23.sp)
    val monoStat = TextStyle(fontFamily = GeistMono, fontWeight = FontWeight(300), fontSize = 18.sp)
    val screenTitle = TextStyle(fontFamily = Geist, fontWeight = FontWeight(400), fontSize = 13.5.sp)
    val rowPrimary = TextStyle(fontFamily = Geist, fontWeight = FontWeight(400), fontSize = 11.sp)
    val rowSecondary = TextStyle(fontFamily = Geist, fontWeight = FontWeight(400), fontSize = 9.5.sp)
    val label = TextStyle(fontFamily = Geist, fontWeight = FontWeight(400), fontSize = 9.sp)
    val meta = TextStyle(fontFamily = GeistMono, fontWeight = FontWeight(400), fontSize = 8.5.sp)
    val smallest = TextStyle(fontFamily = Geist, fontWeight = FontWeight(400), fontSize = 7.5.sp)
}
