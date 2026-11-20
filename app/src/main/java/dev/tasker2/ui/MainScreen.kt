package dev.tasker2.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.DropdownMenu
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tasker2.data.Category
import dev.tasker2.data.Task
import dev.tasker2.domain.TaskLogic
import dev.tasker2.ui.theme.BadgeBg
import dev.tasker2.ui.theme.CardColor
import dev.tasker2.ui.theme.Muted

fun priorityColor(priority: Int): Color = Color(TaskLogic.priorityColorArgb(priority))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    categories: List<Category>,
    tasks: List<Task>,
    selectedCategoryId: Long,
    onSelectCategory: (Long) -> Unit,
    onAddTask: () -> Unit,
    onEditTask: (Task) -> Unit,
    onToggleComplete: (Task) -> Unit,
    onMoveTask: (Task, Long) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onManageCategories: () -> Unit,
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
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            if (current != null) {
                FloatingActionButton(
                    onClick = onAddTask,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(18.dp),
                ) { Icon(Icons.Filled.Add, "Add task") }
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
            CategoryTabs(categories, counts, selectedIndex, onSelectCategory)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

            val open = TaskLogic.sortOpen(tasks.filter { it.categoryId == current.id })
            val done = TaskLogic.sortCompleted(tasks.filter { it.categoryId == current.id })
            var completedExpanded by rememberSaveable(current.id) { mutableStateOf(false) }

            LazyColumn(
                contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                item(key = "header") {
                    CardSection(top = true, bottom = open.isEmpty()) {
                        Text(
                            current.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = if (open.isEmpty()) 8.dp else 4.dp),
                        )
                        if (open.isEmpty()) {
                            Text("No open tasks", color = Muted, modifier = Modifier.padding(start = 20.dp, bottom = 20.dp))
                        }
                    }
                }
                items(open, key = { "open-${it.id}" }) { task ->
                    CardSection(top = false, bottom = task == open.last()) {
                        TaskRow(task, categories, onEditTask, onToggleComplete, onMoveTask, onDeleteTask)
                    }
                }
                if (done.isNotEmpty()) {
                    item(key = "completed-header") {
                        Spacer(Modifier.height(12.dp))
                        CardSection(top = true, bottom = !completedExpanded) {
                            Row(
                                Modifier.fillMaxWidth().clickable { completedExpanded = !completedExpanded }
                                    .padding(horizontal = 20.dp, vertical = 20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Completed (${done.size})", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                Icon(
                                    if (completedExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                    if (completedExpanded) "Collapse" else "Expand",
                                )
                            }
                        }
                    }
                    if (completedExpanded) {
                        items(done, key = { "done-${it.id}" }) { task ->
                            CardSection(top = false, bottom = task == done.last()) {
                                TaskRow(task, categories, onEditTask, onToggleComplete, onMoveTask, onDeleteTask)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryTabs(categories: List<Category>, counts: Map<Long, Int>, selectedIndex: Int, onSelect: (Long) -> Unit) {
    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        edgePadding = 8.dp,
        containerColor = MaterialTheme.colorScheme.background,
        divider = {},
        indicator = { positions ->
            if (selectedIndex < positions.size) {
                TabRowDefaults.PrimaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(positions[selectedIndex]),
                    width = 64.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
    ) {
        categories.forEachIndexed { i, cat ->
            val selected = i == selectedIndex
            Tab(
                selected = selected,
                onClick = { onSelect(cat.id) },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Row(Modifier.padding(horizontal = 6.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(cat.name, fontSize = 17.sp, maxLines = 1)
                    val n = counts[cat.id] ?: 0
                    if (n > 0) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            Modifier.clip(CircleShape).background(BadgeBg).padding(horizontal = 7.dp, vertical = 1.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text("$n", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface) }
                    }
                }
            }
        }
    }
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
    onEdit: (Task) -> Unit,
    onToggleComplete: (Task) -> Unit,
    onMove: (Task, Long) -> Unit,
    onDelete: (Task) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var moveMenuOpen by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.fillMaxWidth()
                .combinedClickable(onClick = { onEdit(task) }, onLongClick = { menuOpen = true })
                .padding(start = 8.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onToggleComplete(task) }) {
                if (task.completed) {
                    Icon(Icons.Filled.Check, "Mark not completed", tint = MaterialTheme.colorScheme.primary)
                } else {
                    Box(Modifier.size(22.dp).border(2.dp, Muted, CircleShape))
                }
            }
            Column(Modifier.weight(1f).padding(start = 4.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)) {
                Text(
                    task.title,
                    fontSize = 17.sp,
                    color = if (task.completed) Muted else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else null,
                )
                if (task.notes.isNotBlank()) {
                    Text(task.notes, fontSize = 13.sp, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            PriorityIndicator(task.priority, dimmed = task.completed)
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

/** Priority number badge plus a vertical urgency bar filled proportionally, colored cool → red. */
@Composable
fun PriorityIndicator(priority: Int, dimmed: Boolean = false, modifier: Modifier = Modifier) {
    val color = priorityColor(priority).let { if (dimmed) it.copy(alpha = 0.45f) else it }
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
