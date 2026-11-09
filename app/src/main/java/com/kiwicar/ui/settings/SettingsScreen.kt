package com.kiwicar.ui.settings

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kiwicar.ui.components.KiwiIcons
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exportError by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("IMPOSTAZIONI", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        SectionTitle("AUTO", KiwiIcons.Car)
        OutlinedTextField(
            value = viewModel.carName,
            onValueChange = { viewModel.carName = it },
            label = { Text("Marca e modello") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = viewModel.initialKm,
            onValueChange = { viewModel.initialKm = it.filter(Char::isDigit) },
            label = { Text("Chilometraggio iniziale (km)") },
            isError = viewModel.carError != null,
            supportingText = viewModel.carError?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = { scope.launch { viewModel.saveCar() } }, Modifier.fillMaxWidth()) { Text("Salva auto") }

        SectionTitle("PREZZI", KiwiIcons.Euro)
        val decimalKeys = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        OutlinedTextField(
            value = viewModel.fuelPrice,
            onValueChange = { viewModel.fuelPrice = it },
            label = { Text("Prezzo benzina (€/L)") },
            isError = viewModel.pricesError != null,
            keyboardOptions = decimalKeys,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = viewModel.oilPrice,
            onValueChange = { viewModel.oilPrice = it },
            label = { Text("Prezzo olio (€/L)") },
            isError = viewModel.pricesError != null,
            supportingText = viewModel.pricesError?.let { { Text(it) } },
            keyboardOptions = decimalKeys,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = { scope.launch { viewModel.savePrices() } }, Modifier.fillMaxWidth()) { Text("Salva prezzi") }

        SectionTitle("DATI", KiwiIcons.Download)
        OutlinedButton(
            onClick = {
                scope.launch {
                    exportError = false
                    try {
                        val csv = viewModel.buildCsv()
                        val file = withContext(Dispatchers.IO) { writeExportFile(context, csv) }
                        context.startActivity(shareIntent(context, file))
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        exportError = true
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(KiwiIcons.Download, contentDescription = null, modifier = Modifier.size(20.dp))
            Text("Esporta CSV", Modifier.padding(start = 8.dp))
        }
        if (exportError) {
            Text("Esportazione non riuscita", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun SectionTitle(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

private fun writeExportFile(context: Context, csv: String): File {
    val dir = File(context.cacheDir, "exports").apply { mkdirs() }
    val stamp = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
    return File(dir, "kiwicar-$stamp.csv").apply { writeText(csv, Charsets.UTF_8) }
}

private fun shareIntent(context: Context, file: File): Intent {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return Intent.createChooser(send, "Esporta CSV")
}
