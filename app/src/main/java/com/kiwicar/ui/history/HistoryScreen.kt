package com.kiwicar.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kiwicar.data.EntryType
import com.kiwicar.data.EntryWithStats
import com.kiwicar.ui.EMPTY_VALUE
import com.kiwicar.ui.components.EntryDetailDialog
import com.kiwicar.ui.components.KiwiIcons
import com.kiwicar.ui.formatConsumption
import com.kiwicar.ui.formatDate
import com.kiwicar.ui.formatKm
import com.kiwicar.ui.formatMoney
import com.kiwicar.ui.theme.KiwiSecondary
import com.kiwicar.ui.theme.KiwiTertiary
import java.text.NumberFormat
import java.util.Locale

@Composable
fun HistoryScreen(
    onEditEntry: (Long) -> Unit,
    viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<EntryWithStats?>(null) }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(
            "STORICO",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp),
        )
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Nessun inserimento") }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(items, key = { it.entry.id }) { item ->
                    HistoryRow(item, Modifier.clickable { selected = item })
                    HorizontalDivider()
                }
            }
        }
    }

    selected?.let { e ->
        EntryDetailDialog(
            entryWithStats = e,
            onEdit = { selected = null; onEditEntry(e.entry.id) },
            onDelete = { viewModel.deleteAsync(e.entry); selected = null },
            onDismiss = { selected = null },
        )
    }
}

@Composable
private fun HistoryRow(item: EntryWithStats, modifier: Modifier) {
    val e = item.entry
    val isFuel = e.type == EntryType.BENZINA
    val accent = if (isFuel) KiwiSecondary else KiwiTertiary
    val unit = if (isFuel) "L/100 km" else "L/1.000 km"
    val liters = NumberFormat.getNumberInstance(Locale.ITALY).apply {
        minimumFractionDigits = 1
        maximumFractionDigits = 2
    }.format(e.liters) + " L"
    val consumption = item.consumption?.let { "${formatConsumption(it)} $unit" } ?: EMPTY_VALUE

    Row(
        modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (isFuel) KiwiIcons.Pump else KiwiIcons.Drop,
            contentDescription = if (isFuel) "Benzina" else "Olio",
            tint = accent,
        )
        Column(Modifier.weight(1f)) {
            Text(formatDate(e.dateEpochDay), fontWeight = FontWeight.Bold)
            Text("${formatKm(e.odometerKm)} · $liters", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(consumption, color = accent, fontWeight = FontWeight.Bold)
            Text(formatMoney(item.estimatedCost), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
