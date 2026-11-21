package com.yogeshpaliyal.gapwise.ui.detail

import com.yogeshpaliyal.gapwise.L10n
import com.yogeshpaliyal.gapwise.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yogeshpaliyal.gapwise.domain.MilestoneInfo
import com.yogeshpaliyal.gapwise.domain.TimeText
import com.yogeshpaliyal.gapwise.ui.common.GapStatus
import com.yogeshpaliyal.gapwise.ui.common.StatusBadge
import com.yogeshpaliyal.gapwise.ui.home.EventUi
import com.yogeshpaliyal.gapwise.ui.home.tint
import com.yogeshpaliyal.gapwise.ui.theme.CountStyle

/** A milestone has one date and nothing to log: how long it's been, and what's coming up. */
@Composable
fun MilestoneDetail(ui: EventUi, m: MilestoneInfo, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            StatusBadge(ui.event.emoji, GapStatus.MILESTONE, container = tint(color, 0.38f), size = 132.dp)
            Spacer(Modifier.height(10.dp))
            Text(GapStatus.MILESTONE.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            val (n, u) = TimeText.milestoneElapsed(m)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(n, style = CountStyle.copy(fontSize = 80.sp, lineHeight = 80.sp))
                Spacer(Modifier.width(10.dp))
                Text(u, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))
            }
            Text(TimeText.milestoneBreakdown(m), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text(
                L10n.s(R.string.ms_since, TimeText.date(m.since)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (m.anniversaryToday || m.roundToday) {
                Spacer(Modifier.height(12.dp))
                Text("🎉 " + TimeText.milestoneLine(m), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            }
        }

        MilestoneCard(L10n.s(R.string.ms_card_total)) {
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                Total(L10n.s(R.string.ms_stat_days), TimeText.count(m.totalDays))
                Total(L10n.s(R.string.ms_stat_weeks), TimeText.count(m.totalDays / 7))
                Total(L10n.s(R.string.ms_stat_months), TimeText.count((m.years * 12 + m.months).toLong()))
            }
        }

        MilestoneCard(L10n.s(R.string.ms_card_next)) {
            ComingUp(
                L10n.p(R.plurals.ms_years, m.nextAnniversaryYears),
                TimeText.date(m.nextAnniversary),
                TimeText.inDays(m.daysToAnniversary),
            )
            Spacer(Modifier.height(12.dp))
            ComingUp(
                L10n.s(R.string.ms_n_days, TimeText.count(m.nextRoundDays)),
                TimeText.date(m.nextRoundDate),
                TimeText.inDays(m.daysToRound),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (ui.event.nudgesEnabled) L10n.s(R.string.ms_reminders_on) else L10n.s(R.string.ms_reminders_off),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ComingUp(what: String, date: String, whenText: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(what, style = MaterialTheme.typography.titleMedium)
            Text(date, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(whenText, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun Total(label: String, value: String) {
    Column {
        Text(value, style = CountStyle.copy(fontSize = 26.sp, lineHeight = 28.sp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MilestoneCard(title: String, content: @Composable () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(20.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            content()
        }
    }
}
