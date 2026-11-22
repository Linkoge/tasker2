package dev.tasker2.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tasker2.domain.AppSettings
import dev.tasker2.ui.theme.Muted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onChange: ((AppSettings) -> AppSettings) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SettingSlider(
                label = "Category tab font size", valueText = "${settings.tabFontSp} sp",
                value = settings.tabFontSp, range = AppSettings.TAB_FONT_RANGE,
                onChange = { v -> onChange { it.copy(tabFontSp = v) } },
            ) {
                // Live preview: two tabs separated by the chosen gap, in the chosen font.
                Row(horizontalArrangement = Arrangement.spacedBy(gapDp(settings))) {
                    Text("Daily 3", fontSize = settings.tabFontSp.sp, color = MaterialTheme.colorScheme.primary)
                    Text("Weekly", fontSize = settings.tabFontSp.sp)
                }
            }
            SettingSlider(
                label = "Task list font size", valueText = "${settings.listFontSp} sp",
                value = settings.listFontSp, range = AppSettings.LIST_FONT_RANGE,
                onChange = { v -> onChange { it.copy(listFontSp = v) } },
            ) {
                Column {
                    Text("Buy groceries", fontSize = settings.listFontSp.sp)
                    Text("Milk, eggs, bread", fontSize = settings.notesFontSp.sp, color = Muted)
                }
            }
            SettingSlider(
                label = "Gap between category tabs", valueText = "${settings.tabGapSpaces} spaces",
                value = settings.tabGapSpaces, range = AppSettings.TAB_GAP_RANGE,
                onChange = { v -> onChange { it.copy(tabGapSpaces = v) } },
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(gapDp(settings))) {
                    Text("Daily", fontSize = settings.tabFontSp.sp)
                    Text("Weekly", fontSize = settings.tabFontSp.sp)
                    Text("Monthly", fontSize = settings.tabFontSp.sp)
                }
            }
            SettingSlider(
                label = "Gap between tasks", valueText = "${settings.taskGapDp} dp",
                value = settings.taskGapDp, range = AppSettings.TASK_GAP_RANGE,
                onChange = { v -> onChange { it.copy(taskGapDp = v) } },
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(settings.taskGapDp.dp)) {
                    Text("First task", fontSize = settings.listFontSp.sp)
                    Text("Second task", fontSize = settings.listFontSp.sp)
                }
            }
            SettingSwitch("Show priority number", settings.showPriorityNumber) { v -> onChange { it.copy(showPriorityNumber = v) } }
            SettingSwitch("Show priority bar", settings.showPriorityBar) { v -> onChange { it.copy(showPriorityBar = v) } }
            OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) { Text("Reset to defaults") }
        }
    }
}

/** Width of N spaces at the tab font, same measurement as the real tabs. */
@Composable
private fun gapDp(settings: AppSettings): androidx.compose.ui.unit.Dp {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val style = androidx.compose.ui.text.TextStyle(fontSize = settings.tabFontSp.sp)
    return androidx.compose.runtime.remember(settings.tabFontSp, settings.tabGapSpaces) {
        with(density) { measurer.measure(TabLayout.gapText(settings.tabGapSpaces), style).size.width.toDp() }
    }
}

@Composable
private fun SettingSlider(
    label: String,
    valueText: String,
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit,
    preview: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(valueText, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(Math.round(it)) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = range.last - range.first - 1,
        )
        preview()
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}
