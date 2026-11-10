package com.kiwicar.ui.charts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kiwicar.data.EntryWithStats
import com.kiwicar.ui.components.EntryDetailDialog
import com.kiwicar.ui.theme.KiwiCarSecondary
import com.kiwicar.ui.theme.KiwiCarTertiary

@Composable
fun ChartsScreen(
    onEditEntry: (Long) -> Unit,
    viewModel: ChartsViewModel = viewModel(factory = ChartsViewModel.Factory),
) {
    val window by viewModel.window.collectAsStateWithLifecycle()
    val data by viewModel.series.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<EntryWithStats?>(null) }
    val onTap: (ChartPoint) -> Unit = { selected = it.source }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("GRAFICI", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            ChartWindow.entries.forEachIndexed { i, w ->
                SegmentedButton(
                    selected = w == window,
                    onClick = { viewModel.setWindow(w) },
                    shape = SegmentedButtonDefaults.itemShape(i, ChartWindow.entries.size),
                ) { Text(w.label) }
            }
        }
        KiwiCarChart("RABBOCCHI OLIO", data.oilTopUps, ChartKind.BAR, KiwiCarTertiary, window, data.today, onTap)
        KiwiCarChart("CONSUMO BENZINA", data.fuelConsumption, ChartKind.LINE, KiwiCarSecondary, window, data.today, onTap)
        KiwiCarChart("CONSUMO OLIO", data.oilConsumption, ChartKind.LINE, KiwiCarTertiary, window, data.today, onTap)
        KiwiCarChart("COSTO MEDIO BENZINA (MENSILE)", data.monthlyFuelCost, ChartKind.BAR, KiwiCarSecondary, window, data.today, onTap)
    }

    selected?.let { e ->
        EntryDetailDialog(
            entryWithStats = e,
            onEdit = { selected = null; onEditEntry(e.entry.id) },
            onDelete = { viewModel.delete(e.entry); selected = null },
            onDismiss = { selected = null },
        )
    }
}
