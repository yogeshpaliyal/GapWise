package com.yogeshpaliyal.gapwise.ui.common

import com.yogeshpaliyal.gapwise.L10n
import com.yogeshpaliyal.gapwise.R

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import com.yogeshpaliyal.gapwise.ui.home.EventUi

/**
 * Where an event is in its cycle. The badge shape encodes this, so you can
 * read the whole home screen at a glance:
 *   circle → fresh, scallop → building, sun → around now, burst → past usual.
 */
enum class GapStatus(@androidx.annotation.StringRes private val labelRes: Int) {
    NEW(R.string.status_new),
    LEARNING(R.string.status_learning),
    FRESH(R.string.status_fresh),
    BUILDING(R.string.status_building),
    DUE(R.string.status_due),
    PAST(R.string.status_past),
    ONGOING(R.string.status_ongoing),
    PAUSED(R.string.status_paused),
    MILESTONE(R.string.status_milestone);

    val label: String get() = L10n.s(labelRes)
}

fun EventUi.status(now: Long): GapStatus = when {
    event.isMilestone -> GapStatus.MILESTONE
    event.paused -> GapStatus.PAUSED
    ongoing -> GapStatus.ONGOING
    rhythm.logCount == 0 -> GapStatus.NEW
    !rhythm.hasRhythm -> GapStatus.LEARNING
    else -> {
        val p = rhythm.progress(now)
        when {
            p < 0.5f -> GapStatus.FRESH
            p < 0.9f -> GapStatus.BUILDING
            p < 1.15f -> GapStatus.DUE
            else -> GapStatus.PAST
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun GapStatus.polygon(): RoundedPolygon = when (this) {
    GapStatus.NEW -> MaterialShapes.Clover4Leaf
    GapStatus.LEARNING -> MaterialShapes.Cookie4Sided
    GapStatus.FRESH -> MaterialShapes.Circle
    GapStatus.BUILDING -> MaterialShapes.Cookie9Sided
    GapStatus.DUE -> MaterialShapes.Sunny
    GapStatus.PAST -> MaterialShapes.SoftBurst
    GapStatus.ONGOING -> MaterialShapes.Pill
    GapStatus.PAUSED -> MaterialShapes.Square
    GapStatus.MILESTONE -> MaterialShapes.Flower
}

/** A [Shape] drawn from a [Morph] between two normalised (0..1) polygons. */
class MorphShape(private val morph: Morph, private val progress: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        val cubics = morph.asCubics(progress)
        if (cubics.isEmpty()) return Outline.Rectangle(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
        val w = size.width
        val h = size.height
        path.moveTo(cubics.first().anchor0X * w, cubics.first().anchor0Y * h)
        cubics.forEach { c ->
            path.cubicTo(c.control0X * w, c.control0Y * h, c.control1X * w, c.control1Y * h, c.anchor1X * w, c.anchor1Y * h)
        }
        path.close()
        return Outline.Generic(path)
    }
}

/**
 * The emoji inside a status shape. When the status changes (for example after
 * you log something), the old shape morphs into the new one with the
 * expressive spatial spring.
 */
@Composable
fun StatusBadge(
    emoji: String,
    status: GapStatus,
    container: Color,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier,
) {
    var from by remember { mutableStateOf(status.polygon()) }
    var to by remember { mutableStateOf(status.polygon()) }
    val progress = remember { Animatable(1f) }
    val spec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()

    LaunchedEffect(status) {
        val target = status.polygon()
        if (target !== to) {
            from = to
            to = target
            progress.snapTo(0f)
            progress.animateTo(1f, spec)
        }
    }

    val morph = remember(from, to) { Morph(from, to) }
    Box(
        modifier
            .size(size)
            .clip(MorphShape(morph, progress.value))
            .background(container)
            .semantics { contentDescription = status.label },
        contentAlignment = Alignment.Center,
    ) {
        if (emoji.isNotBlank()) Text(emoji, fontSize = (size.value * 0.46f).sp)
    }
}
