package com.kiwicar.ui.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kiwicar.data.EntryType
import com.kiwicar.ui.components.KiwiCarIcons
import com.kiwicar.ui.formatDate
import com.kiwicar.ui.formatMoney
import com.kiwicar.ui.theme.KiwiCarSecondary
import com.kiwicar.ui.theme.KiwiCarTertiary
import java.time.Instant
import java.time.ZoneOffset

@Composable
fun AddEntryScreen(
    onSaved: () -> Unit,
    editingId: Long? = null,
    viewModel: AddEntryViewModel = viewModel(
        key = editingId?.let { "edit-$it" },
        factory = editingId?.let { AddEntryViewModel.factoryForEdit(it) } ?: AddEntryViewModel.Factory,
    ),
) {
    val type by viewModel.type.collectAsStateWithLifecycle()
    val date by viewModel.dateEpochDay.collectAsStateWithLifecycle()
    val km by viewModel.kmText.collectAsStateWithLifecycle()
    val liters by viewModel.litersText.collectAsStateWithLifecycle()
    val oilChoice by viewModel.oilChoice.collectAsStateWithLifecycle()
    val kmError by viewModel.kmError.collectAsStateWithLifecycle()
    val cost by viewModel.estimatedCost.collectAsStateWithLifecycle()
    val canSave by viewModel.canSave.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val notFound by viewModel.notFound.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(notFound) { if (notFound) onSaved() }
    LaunchedEffect(saved) { if (saved) onSaved() }
    LaunchedEffect(viewModel) { viewModel.messages.collect { snackbar.showSnackbar(it) } }
    var showPicker by remember { mutableStateOf(false) }

    val accent = if (type == EntryType.OLIO) KiwiCarTertiary else KiwiCarSecondary
    val decimalKeys = KeyboardOptions(keyboardType = KeyboardType.Decimal)

    Box(Modifier.fillMaxSize()) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(if (editingId != null) "MODIFICA INSERIMENTO" else "NUOVO INSERIMENTO", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TypeButton("Benzina", KiwiCarIcons.Pump, type == EntryType.BENZINA, KiwiCarSecondary, Modifier.weight(1f)) {
                viewModel.setType(EntryType.BENZINA)
            }
            TypeButton("Olio", KiwiCarIcons.Drop, type == EntryType.OLIO, KiwiCarTertiary, Modifier.weight(1f)) {
                viewModel.setType(EntryType.OLIO)
            }
        }

        OutlinedButton(onClick = { showPicker = true }, Modifier.fillMaxWidth().height(56.dp)) {
            Text("Data: ${formatDate(date)}")
        }

        OutlinedTextField(
            value = km,
            onValueChange = { viewModel.setKm(it.filter(Char::isDigit)) },
            label = { Text("Chilometraggio (km)") },
            isError = kmError != null,
            supportingText = kmError?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().onFocusChanged { if (!it.isFocused) viewModel.onKmFocusLost() },
        )

        if (type == EntryType.BENZINA) {
            OutlinedTextField(
                value = liters,
                onValueChange = viewModel::setLiters,
                label = { Text("Litri del pieno") },
                keyboardOptions = decimalKeys,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Text("QUANTITÀ", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(OilChoice.HALF to "0,5 L", OilChoice.ONE to "1 L", OilChoice.OTHER to "Altro").forEach { (c, label) ->
                    FilterChip(
                        selected = oilChoice == c,
                        onClick = { viewModel.setOilChoice(c) },
                        label = { Text(label, Modifier.padding(vertical = 10.dp)) },
                    )
                }
            }
            if (oilChoice == OilChoice.OTHER) {
                OutlinedTextField(
                    value = liters,
                    onValueChange = viewModel::setLiters,
                    label = { Text("Litri di olio") },
                    keyboardOptions = decimalKeys,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(KiwiCarIcons.Euro, contentDescription = null, tint = accent)
                Text("Costo stimato", style = MaterialTheme.typography.titleMedium)
            }
            Text(formatMoney(cost), style = MaterialTheme.typography.titleMedium, color = accent, fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = viewModel::submit,
            enabled = canSave && !isSaving,
            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) { Text("Salva") }
    }
    SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }

    if (showPicker) DatePickerDialogFor(date, { showPicker = false }) { viewModel.setDate(it); showPicker = false }
}

@Composable
private fun TypeButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    accent: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val colors = if (selected) {
        ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black)
    } else {
        ButtonDefaults.filledTonalButtonColors()
    }
    Button(onClick = onClick, colors = colors, modifier = modifier.height(56.dp)) {
        Icon(icon, contentDescription = null)
        Text(label, Modifier.padding(start = 8.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialogFor(epochDay: Long, onDismiss: () -> Unit, onPick: (Long) -> Unit) {
    // The picker works in UTC milliseconds; epochDay * 86_400_000 is exactly midnight UTC.
    val state = rememberDatePickerState(initialSelectedDateMillis = epochDay * 86_400_000L)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = state.selectedDateMillis
                if (millis == null) onDismiss()
                else onPick(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay())
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    ) { DatePicker(state) }
}
