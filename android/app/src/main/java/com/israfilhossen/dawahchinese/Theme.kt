package com.israfilhossen.dawahchinese

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive

object C {
    val green = Color(0xFF0C3A2D)
    val green2 = Color(0xFF1F6B52)
    val mint = Color(0xFFE6F1EC)
    val gold = Color(0xFFC9A54A)
    val goldSoft = Color(0xFFF6EFD9)
    val bg = Color(0xFFFAF8F3)
    val card = Color.White
    val line = Color(0xFFEAE5DA)
    val ink = Color(0xFF1C2420)
    val muted = Color(0xFF6E7772)
    val ok = Color(0xFF2E9E5B)
    val bad = Color(0xFFD9534F)
    val tones = listOf(Color(0xFF8A8F8C), Color(0xFFD23F3F), Color(0xFF2E9E5B), Color(0xFF2F6FD6), Color(0xFF8E44C9))
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = C.green, onPrimary = Color.White, secondary = C.gold, background = C.bg, surface = C.card,
            onBackground = C.ink, onSurface = C.ink, surfaceVariant = C.mint, outline = C.line,
            primaryContainer = C.mint, onPrimaryContainer = C.green, secondaryContainer = C.mint,
        ),
        content = content,
    )
}

/** Interface text in the reader's chosen language. */
fun tr(en: String, bn: String) = if (Store.bengali) bn else en

@Composable
fun Panel(modifier: Modifier = Modifier, color: Color = C.card, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val m = modifier.fillMaxWidth()
    Surface(
        modifier = if (onClick != null) m.clip(RoundedCornerShape(22.dp)).clickable(onClick = onClick) else m,
        shape = RoundedCornerShape(22.dp), color = color,
        border = if (color == C.card) BorderStroke(1.dp, C.line) else null,
    ) { Column(Modifier.padding(18.dp), content = content) }
}

@Composable
fun Chip(text: String, bg: Color = C.mint, fg: Color = C.green) {
    Text(text, color = fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 10.dp, vertical = 4.dp))
}

@Composable
fun Title(text: String, sub: String? = null) {
    Column(Modifier.padding(top = 8.dp, bottom = 14.dp)) {
        Text(text, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = C.ink, lineHeight = 34.sp)
        if (sub != null) Text(sub, fontSize = 15.sp, color = C.muted, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(text.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = C.muted, letterSpacing = 1.2.sp,
        modifier = Modifier.padding(top = 22.dp, bottom = 8.dp, start = 4.dp))
}

/** Pinyin coloured by tone: red 1st, green 2nd, blue 3rd, purple 4th, grey neutral. */
@Composable
fun Pinyin(item: Item, size: TextUnit = 17.sp, align: TextAlign = TextAlign.Start, modifier: Modifier = Modifier) {
    val text = buildAnnotatedString {
        if (item.pc.isEmpty()) append(item.py)
        for (piece in item.pc) {
            val s = piece[0].jsonPrimitive.content; val t = piece[1].jsonPrimitive.int
            if (t < 0) append(s) else withStyle(SpanStyle(color = C.tones[t.coerceIn(0, 4)], fontWeight = FontWeight.Medium)) { append(s) }
        }
    }
    Text(text, fontSize = size, textAlign = align, color = C.muted, modifier = modifier)
}

@Composable
fun Ring(progress: Float, size: Dp, stroke: Dp = 10.dp, color: Color = C.green, content: @Composable () -> Unit = {}) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val w = stroke.toPx(); val d = this.size.minDimension - w
            drawArc(C.line, 0f, 360f, false, topLeft = Offset(w / 2, w / 2), size = androidx.compose.ui.geometry.Size(d, d), style = Stroke(w))
            drawArc(color, -90f, 360f * progress.coerceIn(0f, 1f), false, topLeft = Offset(w / 2, w / 2),
                size = androidx.compose.ui.geometry.Size(d, d), style = Stroke(w, cap = StrokeCap.Round))
        }
        content()
    }
}

@Composable
fun Bar(progress: Float, modifier: Modifier = Modifier, color: Color = C.green, height: Dp = 6.dp) {
    Box(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(50)).background(C.line)) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(height).clip(RoundedCornerShape(50)).background(color))
    }
}

@Composable
fun IconBubble(emoji: String, bg: Color = C.mint, size: Dp = 46.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
        Text(emoji, fontSize = (size.value * 0.45f).sp)
    }
}

@Composable
fun RowSpacer(w: Dp = 12.dp) = Spacer(Modifier.width(w))

@Composable
fun Gap(h: Dp = 12.dp) = Spacer(Modifier.height(h))

val chineseStyle = TextStyle(fontWeight = FontWeight.Medium, color = C.ink)

@Composable
fun KeyValue(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = C.muted, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(value, color = C.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
