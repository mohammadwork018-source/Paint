package com.privtepaint.beta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class InkStroke(
    val path: Path,
    val color: Color,
    val width: Float,
    val reference: Boolean
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PrivtePaintApp() }
    }
}

@Composable
private fun PrivtePaintApp() {
    var dark by remember { mutableStateOf(true) }
    var arabic by remember { mutableStateOf(false) }
    var eraser by remember { mutableStateOf(false) }
    var referenceMode by remember { mutableStateOf(false) }
    var showLayers by remember { mutableStateOf(false) }
    var brushSize by remember { mutableFloatStateOf(7f) }
    var stabilization by remember { mutableFloatStateOf(0.55f) }
    var brushColor by remember { mutableStateOf(Color(0xFF292A38)) }
    val artwork = remember { mutableStateListOf<InkStroke>() }
    val reference = remember { mutableStateListOf<InkStroke>() }
    val undoStack = remember { mutableStateListOf<Pair<Boolean, InkStroke>>() }
    val ink = if (dark) Color(0xFFF4F2FF) else Color(0xFF222336)
    val panel = if (dark) Color(0xCC25283A) else Color(0xDDEEF0FA)
    val bg = if (dark) Color(0xFF111321) else Color(0xFFE9EAF3)
    val accent = Color(0xFF9588FF)
    fun label(en: String, ar: String) = if (arabic) ar else en

    MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
        Column(Modifier.fillMaxSize().background(bg).padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(panel)
                    .border(1.dp, Color.White.copy(.14f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(accent), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.AutoFixHigh, null, tint = Color.White)
                }
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text("privte paint", color = ink, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("BETA • STUDIO", color = ink.copy(.55f), fontSize = 9.sp, letterSpacing = 1.5.sp)
                }
                IconButton(onClick = {
                    val source = if (referenceMode) reference else artwork
                    if (source.isNotEmpty()) undoStack.add(referenceMode to source.removeAt(source.lastIndex))
                }) { Icon(Icons.Default.Undo, label("Undo", "تراجع"), tint = ink) }
                IconButton(onClick = {
                    if (undoStack.isNotEmpty()) {
                        val (isRef, stroke) = undoStack.removeAt(undoStack.lastIndex)
                        if (isRef) reference.add(stroke) else artwork.add(stroke)
                    }
                }) { Icon(Icons.Default.Redo, label("Redo", "إعادة"), tint = ink) }
                IconButton(onClick = { dark = !dark }) {
                    Icon(if (dark) Icons.Default.LightMode else Icons.Default.DarkMode, label("Theme", "المظهر"), tint = ink)
                }
                TextButton(onClick = { arabic = !arabic }) { Text(if (arabic) "EN" else "ع", color = accent) }
            }

            Spacer(Modifier.height(10.dp))
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Column(
                    Modifier.width(52.dp).clip(RoundedCornerShape(20.dp)).background(panel)
                        .border(1.dp, Color.White.copy(.12f), RoundedCornerShape(20.dp)).padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ToolButton(Icons.Default.Brush, !eraser, accent, ink) { eraser = false }
                    ToolButton(Icons.Default.AutoFixHigh, eraser, accent, ink) { eraser = true }
                    HorizontalDivider(Modifier.padding(horizontal = 8.dp), color = ink.copy(.15f))
                    ToolButton(Icons.Default.Layers, showLayers, accent, ink) { showLayers = !showLayers }
                    ToolButton(Icons.Default.Image, referenceMode, accent, ink) { referenceMode = !referenceMode }
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.size(27.dp).clip(CircleShape).background(brushColor).border(2.dp, Color.White, CircleShape))
                }

                Column(Modifier.weight(1f).fillMaxHeight()) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(label("Untitled artwork", "رسمة بدون عنوان"), color = ink, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        Surface(color = accent.copy(.17f), shape = CircleShape) {
                            Text(if (referenceMode) label("REFERENCE", "مرجع") else label("ARTWORK", "الرسم"),
                                Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)).background(Color.White)
                        .border(1.dp, Color.White.copy(.4f), RoundedCornerShape(18.dp))) {
                        Canvas(Modifier.fillMaxSize().pointerInput(eraser, referenceMode, brushSize, brushColor, stabilization) {
                            var current: Path? = null
                            var last = Offset.Zero
                            detectDragGestures(
                                onDragStart = { p ->
                                    current = Path().apply { moveTo(p.x, p.y) }
                                    last = p
                                    val stroke = InkStroke(current!!, if (eraser) Color.White else brushColor, brushSize, referenceMode)
                                    if (referenceMode) reference.add(stroke) else artwork.add(stroke)
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val p = change.position
                                    val factor = (1f - stabilization * .82f).coerceIn(.12f, 1f)
                                    val smooth = Offset(last.x + (p.x-last.x)*factor, last.y + (p.y-last.y)*factor)
                                    val mid = Offset((last.x+smooth.x)/2f, (last.y+smooth.y)/2f)
                                    current?.quadraticTo(last.x, last.y, mid.x, mid.y)
                                    current?.lineTo(smooth.x, smooth.y)
                                    last = smooth
                                    val target = if (referenceMode) reference else artwork
                                    if (target.isNotEmpty()) target[target.lastIndex] = target.last().copy(path = current!!)
                                },
                                onDragEnd = { current = null }
                            )
                        }) {
                            // Reference strokes are intentionally a separate collection.
                            // The time-lapse recorder must subscribe to artwork only.
                            reference.forEach { s ->
                                drawPath(s.path, s.color.copy(alpha = .55f),
                                    style = Stroke(s.width, cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                        join = androidx.compose.ui.graphics.StrokeJoin.Round))
                            }
                            artwork.forEach { s ->
                                drawPath(s.path, s.color,
                                    style = Stroke(s.width, cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                        join = androidx.compose.ui.graphics.StrokeJoin.Round))
                            }
                        }
                        if (artwork.isEmpty() && reference.isEmpty()) {
                            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Gesture, null, tint = Color(0xFF9997AD), modifier = Modifier.size(38.dp))
                                Spacer(Modifier.height(8.dp))
                                Text(label("Your canvas, your world.", "لوحتك، عالمك."), color = Color(0xFF77768B), fontSize = 13.sp)
                                Text(label("Start with a stroke", "ابدأ بأول لمسة"), color = Color(0xFFAAA8B8), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(23.dp)).background(panel)
                .border(1.dp, Color.White.copy(.13f), RoundedCornerShape(23.dp)).padding(13.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(label("BRUSH SIZE", "حجم الفرشاة"), color = ink.copy(.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text("${brushSize.toInt()} px", color = ink, fontSize = 11.sp)
                }
                Slider(brushSize, { brushSize = it }, valueRange = 1f..48f,
                    colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(label("LINE STABILIZER", "محسّن الخط"), color = ink.copy(.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text("${(stabilization*100).toInt()}%", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Slider(stabilization, { stabilization = it }, valueRange = 0f..1f,
                    colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    items(listOf(Color(0xFF292A38), Color(0xFFE85D75), Color(0xFFFFB86B), Color(0xFF66C7B5),
                        Color(0xFF6D9EFF), Color(0xFF9588FF), Color.White, Color.Black)) { swatch ->
                        Box(Modifier.size(27.dp).clip(CircleShape).background(swatch)
                            .border(if (brushColor == swatch) 3.dp else 1.dp,
                                if (brushColor == swatch) accent else ink.copy(.2f), CircleShape)
                            .clickable { brushColor = swatch })
                    }
                }
                Spacer(Modifier.height(7.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { showLayers = true }) {
                        Icon(Icons.Default.Layers, null, tint = accent)
                        Spacer(Modifier.width(4.dp))
                        Text(label("Layers", "الطبقات"), color = ink)
                    }
                    Spacer(Modifier.weight(1f))
                    Button(onClick = { /* Export implementation is a follow-up milestone. */ },
                        colors = ButtonDefaults.buttonColors(containerColor = accent), shape = RoundedCornerShape(13.dp)) {
                        Icon(Icons.Default.FileDownload, null)
                        Spacer(Modifier.width(5.dp))
                        Text(label("Export", "تصدير"))
                    }
                }
            }
        }

        if (showLayers) {
            AlertDialog(
                onDismissRequest = { showLayers = false },
                containerColor = panel,
                title = { Text(label("Layer studio", "استوديو الطبقات"), color = ink) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(label("Artwork layer • ${artwork.size} strokes", "طبقة الرسم • ${artwork.size} خطوط"), color = ink)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(label("Reference layer • ${reference.size} strokes", "طبقة المرجع • ${reference.size} خطوط"), color = ink, fontWeight = FontWeight.SemiBold)
                                Text(label("Excluded from time-lapse source", "مستبعدة من مصدر تسجيل التايم لابس"), color = ink.copy(.65f), fontSize = 11.sp)
                            }
                            Switch(referenceMode, { referenceMode = it })
                        }
                        Text(label("Reference and artwork strokes are stored separately.", "خطوط المرجع والرسم محفوظة بشكل منفصل."), color = ink.copy(.7f), fontSize = 12.sp)
                    }
                },
                confirmButton = { TextButton(onClick = { showLayers = false }) { Text(label("Done", "تم"), color = accent) } }
            )
        }
    }
}

@Composable
private fun ToolButton(icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean,
                       accent: Color, ink: Color, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(41.dp).clip(RoundedCornerShape(13.dp))
        .background(if (selected) accent.copy(.24f) else Color.Transparent)) {
        Icon(icon, null, tint = if (selected) accent else ink.copy(.8f))
    }
}
