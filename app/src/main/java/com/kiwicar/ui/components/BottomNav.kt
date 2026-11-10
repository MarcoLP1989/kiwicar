package com.kiwicar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

data class BottomTab(val route: String, val label: String, val icon: ImageVector)

val bottomTabs = listOf(
    BottomTab("home", "Home", KiwiCarIcons.Home),
    BottomTab("charts", "Grafici", KiwiCarIcons.Chart),
    BottomTab("history", "Storico", KiwiCarIcons.History),
    BottomTab("settings", "Impostazioni", KiwiCarIcons.Settings),
)

/** Active tab = green pill with icon + label; the others show the icon only. */
@Composable
fun BottomNav(currentRoute: String?, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            bottomTabs.forEach { tab ->
                val selected = tab.route == currentRoute
                val pill = if (selected) {
                    Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
                } else Modifier
                Row(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .then(pill)
                        .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(tab.route) })
                        .padding(horizontal = if (selected) 16.dp else 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = if (selected) null else tab.label,
                        tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (selected) {
                        Spacer(Modifier.width(8.dp))
                        Text(tab.label, color = Color.White, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
