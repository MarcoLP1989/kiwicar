package com.kiwicar.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kiwicar.data.EntryType
import com.kiwicar.data.EntryWithStats
import com.kiwicar.ui.EMPTY_VALUE
import com.kiwicar.ui.formatConsumption
import com.kiwicar.ui.formatDate
import com.kiwicar.ui.formatKm
import com.kiwicar.ui.formatMoney
import java.text.NumberFormat
import java.util.Locale

/** Detail popup for one entry; shared by Charts and History. Delete asks for confirmation first. */
@Composable
fun EntryDetailDialog(
    entryWithStats: EntryWithStats,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val e = entryWithStats.entry
    val isFuel = e.type == EntryType.BENZINA
    val unit = if (isFuel) "L/100 km" else "L/1.000 km"
    val liters = NumberFormat.getNumberInstance(Locale.ITALY).apply {
        minimumFractionDigits = 1
        maximumFractionDigits = 2
    }.format(e.liters) + " L"
    val consumption = entryWithStats.consumption?.let { "${formatConsumption(it)} $unit" } ?: EMPTY_VALUE

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Eliminare questo inserimento?") },
            confirmButton = { TextButton(onClick = onDelete) { Text("Elimina") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Annulla") } },
        )
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isFuel) "Benzina" else "Olio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DetailRow("Data", formatDate(e.dateEpochDay))
                DetailRow("Km", formatKm(e.odometerKm))
                DetailRow("Litri", liters)
                DetailRow("Costo stimato", formatMoney(entryWithStats.estimatedCost))
                DetailRow("Dal precedente", entryWithStats.distanceFromPreviousKm?.let { formatKm(it) } ?: EMPTY_VALUE)
                DetailRow("Consumo", consumption)
            }
        },
        confirmButton = { TextButton(onClick = onEdit) { Text("Modifica") } },
        dismissButton = {
            Row {
                TextButton(onClick = { confirmDelete = true }) { Text("Elimina") }
                TextButton(onClick = onDismiss) { Text("Chiudi") }
            }
        },
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Bold)
    }
}
