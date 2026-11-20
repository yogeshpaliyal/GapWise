package com.yogeshpaliyal.gapwise.ui.welcome

import com.yogeshpaliyal.gapwise.L10n
import com.yogeshpaliyal.gapwise.R
import com.yogeshpaliyal.gapwise.BuildConfig

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.NoAccounts
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.Morph
import com.yogeshpaliyal.gapwise.ui.common.GapStatus
import com.yogeshpaliyal.gapwise.ui.common.MorphShape
import com.yogeshpaliyal.gapwise.ui.common.StatusBadge
import com.yogeshpaliyal.gapwise.ui.common.polygon
import com.yogeshpaliyal.gapwise.ui.home.eventColor
import com.yogeshpaliyal.gapwise.ui.home.tint
import com.yogeshpaliyal.gapwise.ui.theme.CountStyle
import com.yogeshpaliyal.gapwise.update.isRobolectric
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private class IntroPage(val title: String, val body: String, val art: @Composable () -> Unit)

private val pages: List<IntroPage> get() = listOf(
    IntroPage(
        L10n.s(R.string.intro1_title),
        L10n.s(R.string.intro1_body),
    ) { ArtCards() },
    IntroPage(
        L10n.s(R.string.intro2_title),
        L10n.s(R.string.intro2_body),
    ) { ArtTap() },
    IntroPage(
        L10n.s(R.string.intro3_title),
        L10n.s(R.string.intro3_body),
    ) { ArtShapes() },
    IntroPage(
        L10n.s(R.string.intro4_title),
        L10n.s(if (BuildConfig.HAS_DRIVE) R.string.intro4_body else R.string.intro4_body_no_drive),
    ) { ArtPrivate() },
)

/** First-run intro: four short pages before picking starter events. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IntroScreen(onDone: () -> Unit) {
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == pages.lastIndex

    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.End) {
                if (!last) TextButton(onClick = onDone) { Text(L10n.s(R.string.skip)) } else Spacer(Modifier.height(48.dp))
            }
            HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { i ->
                val page = pages[i]
                Column(Modifier.fillMaxSize().padding(horizontal = 28.dp)) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { page.art() }
                    Text(page.title, style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        page.body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Dots(pages.size, pager.currentPage, Modifier.weight(1f))
                Button(
                    onClick = { if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.heightIn(min = 56.dp),
                ) { Text(if (last) L10n.s(R.string.get_started) else L10n.s(R.string.next), style = MaterialTheme.typography.titleMedium) }
            }
        }
    }
}

@Composable
private fun Dots(count: Int, current: Int, modifier: Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { i ->
            val w by animateDpAsState(if (i == current) 24.dp else 8.dp, label = "dot")
            Box(
                Modifier.size(width = w, height = 8.dp).clip(CircleShape).background(
                    if (i == current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                ),
            )
        }
    }
}

/** A soft expressive shape behind each page's illustration. */
@Composable
private fun Backdrop(status: GapStatus, content: @Composable () -> Unit) {
    val shape = remember(status) { status.polygon().let { MorphShape(Morph(it, it), 0f) } }
    Box(contentAlignment = Alignment.Center) {
        Box(Modifier.size(260.dp).clip(shape).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)))
        content()
    }
}

@Composable
private fun MiniCard(emoji: String, name: String, count: String, unit: String, color: Long, status: GapStatus, modifier: Modifier = Modifier) {
    val c = eventColor(color)
    Surface(shape = RoundedCornerShape(28.dp), color = tint(c, 0.22f), shadowElevation = 6.dp, modifier = modifier.width(176.dp)) {
        Column(Modifier.padding(16.dp)) {
            StatusBadge(emoji, status, container = tint(c, 0.35f), size = 40.dp)
            Spacer(Modifier.height(10.dp))
            Text(name, style = MaterialTheme.typography.labelLarge)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(count, style = CountStyle.copy(fontSize = 34.sp, lineHeight = 34.sp))
                Spacer(Modifier.width(6.dp))
                Text(unit, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 4.dp))
            }
        }
    }
}

@Composable
private fun ArtCards() = Backdrop(GapStatus.BUILDING) {
    Box(Modifier.size(300.dp, 280.dp)) {
        MiniCard("💇", L10n.s(R.string.sug_haircut), "36", L10n.p(R.plurals.time_days_ago_unit, 36), 0xFFE0569A, GapStatus.DUE, Modifier.align(Alignment.TopStart).rotate(-6f))
        MiniCard("🛢️", L10n.s(R.string.sug_oil_change), "4", L10n.p(R.plurals.time_months_ago_unit, 4), 0xFF3E7CB1, GapStatus.BUILDING, Modifier.align(Alignment.BottomEnd).rotate(5f))
    }
}

@Composable
private fun ArtTap() = Backdrop(GapStatus.FRESH) {
    var done by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (!isRobolectric) {
            delay(1600); done = true
            delay(2200); done = false
        }
    }
    val press by animateFloatAsState(if (done) 0.94f else 1f, MaterialTheme.motionScheme.fastSpatialSpec(), label = "press")
    Box(contentAlignment = Alignment.Center) {
        AnimatedContent(done, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "tap") { d ->
            MiniCard(
                "🪴", L10n.s(R.string.sug_water_plants),
                if (d) L10n.s(R.string.time_now) else "12", if (d) L10n.s(R.string.time_just_logged) else L10n.p(R.plurals.time_days_ago_unit, 12),
                0xFF4E7D57, if (d) GapStatus.FRESH else GapStatus.PAST,
                Modifier.scale(1.25f),
            )
        }
        // A fingertip that presses when the card logs.
        Box(
            Modifier.offset(x = 70.dp, y = 70.dp).scale(press).size(44.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.inverseSurface.copy(alpha = if (done) 0.35f else 0.2f)),
        )
    }
}

@Composable
private fun ArtShapes() = Backdrop(GapStatus.DUE) {
    val steps = listOf(GapStatus.FRESH, GapStatus.BUILDING, GapStatus.DUE, GapStatus.PAST)
    var i by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (!isRobolectric) { delay(1400); i = (i + 1) % steps.size } }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        StatusBadge("💇", steps[i], container = MaterialTheme.colorScheme.surfaceContainerLowest, size = 120.dp)
        Spacer(Modifier.height(16.dp))
        AnimatedContent(steps[i], transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "label") {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerLowest) {
                Text(it.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
        }
    }
}

@Composable
private fun ArtPrivate() = Backdrop(GapStatus.LEARNING) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Promise(Icons.Rounded.NoAccounts, L10n.s(R.string.promise_no_account), -3f)
        if (BuildConfig.HAS_DRIVE) Promise(Icons.Rounded.CloudDone, L10n.s(R.string.promise_drive), 2f)
        else Promise(Icons.Rounded.SaveAlt, L10n.s(R.string.promise_export), 2f)
        Promise(Icons.Rounded.Code, L10n.s(R.string.promise_open_source), -2f)
    }
}

@Composable
private fun Promise(icon: ImageVector, text: String, tilt: Float) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerLowest, shadowElevation = 4.dp, modifier = Modifier.rotate(tilt)) {
        Row(Modifier.padding(start = 8.dp, end = 20.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimary)
            }
            Spacer(Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.titleMedium, color = Color.Unspecified)
        }
    }
}
