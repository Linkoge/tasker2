package dev.tasker2.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.material3.DropdownMenu
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tasker2.data.Category
import dev.tasker2.data.Task
import dev.tasker2.domain.AppSettings
import dev.tasker2.domain.TaskLogic
import dev.tasker2.ui.theme.BadgeBg
import dev.tasker2.ui.theme.CardColor
import dev.tasker2.ui.theme.Muted

fun priorityColor(priority: Int): Color = Color(TaskLogic.priorityColorArgb(priority))

/** Text with a custom ~2dp strikethrough line (the default TextDecoration is too thin). */
@Composable
private fun StrikeText(text: String, fontSize: androidx.compose.ui.unit.TextUnit, color: Color, struck: Boolean) {
    var layout by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }
    val strokePx = with(LocalDensity.current) { 2.dp.toPx() }
    Text(
        text, fontSize = fontSize, color = color,
        onTextLayout = { layout = it },
        modifier = if (!struck) Modifier else Modifier.drawBehind {
            val l = layout ?: return@drawBehind
            for (i in 0 until l.lineCount) {
                val y = (l.getLineTop(i) + l.getLineBottom(i)) / 2f
                drawLine(color, Offset(l.getLineLeft(i), y), Offset(l.getLineRight(i), y), strokeWidth = strokePx)
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    categories: List<Category>,
    tasks: List<Task>,
    selectedCategoryId: Long,
    onSelectCategory: (Long) -> Unit,
    onAddTask: () -> Unit,
    onAddMany: () -> Unit,
    onEditTask: (Task) -> Unit,
    onToggleComplete: (Task) -> Unit,
    onToggleCrossedOut: (Task) -> Unit,
    onMoveTask: (Task, Long) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onManageCategories: () -> Unit,
    onOpenSettings: () -> Unit = {},
    settings: AppSettings = AppSettings(),
) {
    var menuOpen by remember { mutableStateOf(false) }
    val counts = remember(tasks) { TaskLogic.openCounts(tasks) }
    val selectedIndex = categories.indexOfFirst { it.id == selectedCategoryId }.coerceAtLeast(0)
    val current = categories.getOrNull(selectedIndex)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Tasks", fontSize = 26.sp) },
                actions = {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, "Menu") }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Manage categories") },
                            onClick = { menuOpen = false; onManageCategories() },
                        )
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            onClick = { menuOpen = false; onOpenSettings() },
                        )
                        val context = LocalContext.current
                        val versionName = remember {
                            runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "?"
                        }
                        DropdownMenuItem(text = { Text("Version $versionName") }, onClick = {}, enabled = false)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            if (current != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    FloatingActionButton(
                        onClick = onAddMany,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = RoundedCornerShape(18.dp),
                    ) { Text("+++", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                    FloatingActionButton(
                        onClick = onAddTask,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = RoundedCornerShape(18.dp),
                    ) { Icon(Icons.Filled.Add, "Add task") }
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (current == null) {
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("No categories yet", color = Muted)
                    TextButton(onClick = onManageCategories) { Text("Manage categories") }
                }
                return@Column
            }
            CategoryTabs(categories, counts, selectedIndex, settings, onSelectCategory)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

            val open = TaskLogic.sortOpen(tasks.filter { it.categoryId == current.id })
            val done = TaskLogic.sortCompleted(tasks.filter { it.categoryId == current.id })
            var completedExpanded by rememberSaveable(current.id) { mutableStateOf(false) }

            LazyColumn(
                contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                if (open.isNotEmpty()) item(key = "header") {
                    CardSection(top = true, bottom = false) {
                        Text(
                            current.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp),
                        )
                    }
                }
                items(open, key = { "open-${it.id}" }) { task ->
                    CardSection(top = false, bottom = task == open.last()) {
                        TaskRow(task, categories, settings, onEditTask, onToggleComplete, onToggleCrossedOut, onMoveTask, onDeleteTask)
                    }
                }
                if (done.isNotEmpty()) {
                    item(key = "completed-header") {
                        Spacer(Modifier.height(12.dp))
                        // No background of any kind: only the arrow on the plain screen background.
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    completedExpanded = !completedExpanded
                                }
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Spacer(Modifier.weight(1f))
                            Icon(
                                if (completedExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                if (completedExpanded) "Collapse" else "Expand",
                            )
                        }
                    }
                    if (completedExpanded) {
                        items(done, key = { "done-${it.id}" }) { task ->
                            CardSection(top = task == done.first(), bottom = task == done.last()) {
                                TaskRow(task, categories, settings, onEditTask, onToggleComplete, onToggleCrossedOut, onMoveTask, onDeleteTask)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryTabs(
    categories: List<Category>,
    counts: Map<Long, Int>,
    selectedIndex: Int,
    settings: AppSettings,
    onSelect: (Long) -> Unit,
) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val tabStyle = TextStyle(fontSize = settings.tabFontSp.sp)
    // Gap between adjacent tabs = width of N spaces (settings, default 6) in the tab font.
    val gap = remember(measurer, settings.tabFontSp, settings.tabGapSpaces) {
        with(density) { measurer.measure(TabLayout.gapText(settings.tabGapSpaces), tabStyle).size.width.toDp() }
    }
    val nameWidths = remember { mutableStateMapOf<Int, Dp>() }
    // Plain scrolling row (no material Tab / TabRow: those add ripple, padding and a 90dp min width).
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(gap),
    ) {
        categories.forEachIndexed { i, cat ->
            val selected = i == selectedIndex
            val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            Box(
                Modifier.selectable(
                    selected = selected, role = Role.Tab,
                    interactionSource = remember { MutableInteractionSource() }, indication = null,
                    onClick = { onSelect(cat.id) },
                ),
            ) {
                Row(Modifier.padding(top = 12.dp, bottom = 15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        cat.name, style = tabStyle, color = color, maxLines = 1,
                        onTextLayout = { nameWidths[i] = with(density) { it.size.width.toDp() } },
                    )
                    val n = counts[cat.id] ?: 0
                    if (n > 0) Text(" $n", style = tabStyle, color = color, maxLines = 1)
                }
                if (selected) {
                    Box(
                        Modifier.align(Alignment.BottomStart).width(nameWidths[i] ?: 0.dp).height(3.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)),
                    )
                }
            }
        }
    }
}

/** Tab layout constants shared by the UI and tests. */
object TabLayout {
    const val FONT_SP = AppSettings.DEFAULT_TAB_FONT_SP
    const val GAP_SPACES = AppSettings.DEFAULT_TAB_GAP_SPACES
    fun gapText(spaces: Int = GAP_SPACES): String = " ".repeat(spaces)
}

/** Draws a slice of a rounded card so several LazyColumn items look like one card. */
@Composable
private fun CardSection(top: Boolean, bottom: Boolean, content: @Composable () -> Unit) {
    val r = 22.dp
    val shape = RoundedCornerShape(
        topStart = if (top) r else 0.dp, topEnd = if (top) r else 0.dp,
        bottomStart = if (bottom) r else 0.dp, bottomEnd = if (bottom) r else 0.dp,
    )
    Column(
        Modifier.fillMaxWidth().clip(shape).background(CardColor)
            .padding(bottom = if (bottom) 8.dp else 0.dp),
    ) { content() }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskRow(
    task: Task,
    categories: List<Category>,
    settings: AppSettings,
    onEdit: (Task) -> Unit,
    onToggleComplete: (Task) -> Unit,
    onToggleCrossedOut: (Task) -> Unit,
    onMove: (Task, Long) -> Unit,
    onDelete: (Task) -> Unit,
) {
    val grayed = TaskLogic.isDimmed(task)
    var menuOpen by remember { mutableStateOf(false) }
    var moveMenuOpen by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.fillMaxWidth()
                .combinedClickable(onClick = { onEdit(task) }, onLongClick = { menuOpen = true })
                .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Round circle: one tap completes (or reopens) the task, crossed out or not.
            Box(
                Modifier.size(48.dp).clip(CircleShape).clickable { onToggleComplete(task) },
                contentAlignment = Alignment.Center,
            ) {
                if (task.completed) {
                    Icon(Icons.Filled.Check, "Mark not completed", tint = MaterialTheme.colorScheme.primary)
                } else {
                    Box(Modifier.size(22.dp).border(2.dp, Muted, CircleShape))
                }
            }
            Column(Modifier.weight(1f).padding(start = 4.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)) {
                StrikeText(
                    task.title,
                    fontSize = settings.listFontSp.sp,
                    color = if (grayed) Muted else MaterialTheme.colorScheme.onSurface,
                    struck = grayed,
                )
                if (task.notes.isNotBlank()) {
                    Text(task.notes, fontSize = settings.notesFontSp.sp, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (TaskLogic.hasPriority(task.priority)) {
                PriorityIndicator(task.priority, alpha = TaskLogic.priorityAlpha(task))
            }
            // Square checkbox at the far right: toggles crossed-out (task stays in the open list).
            Checkbox(
                checked = task.crossedOut,
                onCheckedChange = { onToggleCrossedOut(task) },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = Muted,
                    checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(text = { Text("Edit") }, onClick = { menuOpen = false; onEdit(task) })
            DropdownMenuItem(
                text = { Text(if (task.completed) "Mark not completed" else "Mark completed") },
                onClick = { menuOpen = false; onToggleComplete(task) },
            )
            if (categories.size > 1) {
                DropdownMenuItem(text = { Text("Move to…") }, onClick = { menuOpen = false; moveMenuOpen = true })
            }
            DropdownMenuItem(text = { Text("Delete") }, onClick = { menuOpen = false; onDelete(task) })
        }
        DropdownMenu(expanded = moveMenuOpen, onDismissRequest = { moveMenuOpen = false }) {
            categories.filter { it.id != task.categoryId }.forEach { cat ->
                DropdownMenuItem(text = { Text(cat.name) }, onClick = { moveMenuOpen = false; onMove(task, cat.id) })
            }
        }
    }
}

/** Priority number badge plus (colored, only dimmed to [alpha], never gray)  a vertical urgency bar filled proportionally, colored gray-yellow → red. */
@Composable
fun PriorityIndicator(priority: Int, alpha: Float = 1f, modifier: Modifier = Modifier) {
    val color = priorityColor(priority).let { if (alpha < 1f) it.copy(alpha = alpha) else it }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.18f))
                .border(1.dp, color, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) { Text("$priority", color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier.width(6.dp).height(32.dp).clip(RoundedCornerShape(3.dp)).background(BadgeBg),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(Modifier.fillMaxWidth().height((32f * priority / Task.MAX_PRIORITY).dp).background(color))
        }
    }
}
