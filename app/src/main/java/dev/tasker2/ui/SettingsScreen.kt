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
import dev.tasker2.domain.TaskRowMetrics
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.ui.graphics.Color
import dev.tasker2.domain.CrossedOutStyle
import dev.tasker2.domain.TaskLogic
import androidx.compose.foundation.layout.heightIn
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
                label = "Gap between tasks", valueText = TaskRowMetrics.label(settings.taskGapDp),
                value = settings.taskGapDp, range = AppSettings.TASK_GAP_RANGE,
                onChange = { v -> onChange { it.copy(taskGapDp = v) } },
            ) {
                val m = TaskRowMetrics.metrics(settings.taskGapDp)
                Column(verticalArrangement = Arrangement.spacedBy(m.listSpacingDp.dp)) {
                    for (name in listOf("First task", "Second task")) {
                        Box(Modifier.fillMaxWidth().heightIn(min = m.minRowHeightDp.dp), contentAlignment = Alignment.CenterStart) {
                            Text(name, fontSize = settings.listFontSp.sp)
                        }
                    }
                }
            }
            SettingSwitch("Show priority number", settings.showPriorityNumber) { v -> onChange { it.copy(showPriorityNumber = v) } }
            SettingSwitch("Show priority bar", settings.showPriorityBar) { v -> onChange { it.copy(showPriorityBar = v) } }
            StrikeThicknessSetting(settings, onChange)
            CrossedStyleSetting(settings, onChange)
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

@Composable
private fun StrikeThicknessSetting(settings: AppSettings, onChange: ((AppSettings) -> AppSettings) -> Unit) {
    val range = AppSettings.STRIKE_HALF_RANGE
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Cross-out line thickness", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("${settings.strikeThicknessDp} dp", color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = settings.strikeHalfDp.toFloat(),
            onValueChange = { v -> onChange { it.copy(strikeHalfDp = Math.round(v)) } },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = range.last - range.first - 1,
        )
        Box(Modifier.fillMaxWidth().height(24.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxWidth().height(settings.strikeThicknessDp.dp).background(Color(TaskLogic.Gray.TEXT)))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CrossedStyleSetting(settings: AppSettings, onChange: ((AppSettings) -> AppSettings) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Crossed-out tasks", style = MaterialTheme.typography.titleMedium)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            CrossedOutStyle.entries.forEachIndexed { i, style ->
                SegmentedButton(
                    selected = settings.crossedOutStyle == style,
                    onClick = { onChange { it.copy(crossedOutStyle = style) } },
                    shape = SegmentedButtonDefaults.itemShape(i, CrossedOutStyle.entries.size),
                ) { Text(style.label) }
            }
        }
        // Preview of a crossed-out priority 10 task in the chosen style.
        val p = TaskLogic.crossedPalette(10, settings.crossedOutStyle)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(28.dp).border(1.dp, Color(p.accent), RoundedCornerShape(8.dp)).background(Color(p.badgeFill), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Text("10", color = Color(p.accent), fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
            Text("Crossed-out task", color = Color(p.text), fontSize = settings.listFontSp.sp)
        }
    }
}
