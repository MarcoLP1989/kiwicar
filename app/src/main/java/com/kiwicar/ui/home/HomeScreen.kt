package com.kiwicar.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kiwicar.ui.components.KiwiIcons
import com.kiwicar.ui.components.StatCard
import com.kiwicar.ui.components.StatValue
import com.kiwicar.ui.formatConsumption
import com.kiwicar.ui.formatKm
import com.kiwicar.ui.formatMoney
import com.kiwicar.ui.theme.KiwiSecondary
import com.kiwicar.ui.theme.KiwiTertiary

@Composable
fun HomeScreen(onAddClick: () -> Unit, viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeContent(state, onAddClick)
}

@Composable
fun HomeContent(state: HomeUiState, onAddClick: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            state.carName.ifBlank { "KiwiCar" },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(formatKm(state.currentKm), style = MaterialTheme.typography.titleMedium, color = KiwiSecondary)

        StatCard("Consumi", KiwiIcons.Pump, KiwiSecondary) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatValue(formatConsumption(state.stats.fuelLper100), "L/100 km benzina")
                StatValue(formatConsumption(state.stats.oilLper1000), "L/1.000 km olio")
            }
        }
        StatCard("Costo medio benzina", KiwiIcons.Euro, KiwiSecondary) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatValue(formatMoney(state.stats.avgMonthlyCost), "al mese")
                StatValue(formatMoney(state.stats.avgYearlyCost), "all’anno")
            }
        }
        StatCard("Rabbocchi olio", KiwiIcons.Drop, KiwiTertiary) {
            OilBars(state.oilTopUps)
        }

        Button(onClick = onAddClick, modifier = Modifier.fillMaxWidth()) {
            Text("+ Nuovo rifornimento")
        }
    }
}

@Composable
private fun OilBars(liters: List<Double>) {
    if (liters.isEmpty()) {
        Text(
            "Nessun rabbocco",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val max = liters.max().coerceAtLeast(0.001)
    Canvas(Modifier.fillMaxWidth().height(80.dp)) {
        val gap = 8.dp.toPx()
        val barW = (size.width - gap * (liters.size - 1)) / liters.size
        liters.forEachIndexed { i, l ->
            val h = (size.height * (l / max)).toFloat().coerceAtLeast(2.dp.toPx())
            drawRoundRect(
                color = KiwiTertiary,
                topLeft = Offset(i * (barW + gap), size.height - h),
                size = Size(barW, h),
                cornerRadius = CornerRadius(3.dp.toPx()),
            )
        }
    }
}
