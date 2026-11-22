package dev.tasker2.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.tasker2.data.Category
import dev.tasker2.domain.BulkEntry
import dev.tasker2.domain.BulkState
import dev.tasker2.domain.TaskLogic

private val BulkStateSaver = listSaver<BulkState, Any>(
    save = { s -> listOf<Any>(s.selected) + s.entries.flatMap { listOf(it.title, it.notes, it.priority) } },
    restore = { l ->
        val entries = l.drop(1).chunked(3).map { BulkEntry(it[0] as String, it[1] as String, it[2] as Int) }
        BulkState(entries, l[0] as Int).normalized()
    },
)

/**
 * The whole form is ONE scroll container (titles, then details, then buttons) with `imePadding`.
 * Previously a weighted LazyColumn sat above a fixed bottom section; when the keyboard opened the
 * bottom section took all the height and squeezed the title list to nothing. Now nothing can be
 * squeezed: the form scrolls, and the focused title is brought into view with its neighbours.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BulkAddScreen(
    initialCategoryId: Long,
    categories: List<Category>,
    onBack: () -> Unit,
    onSave: (categoryId: Long, entries: List<BulkEntry>) -> Unit,
) {
    var state by rememberSaveable(stateSaver = BulkStateSaver) { mutableStateOf(BulkState()) }
    var categoryId by rememberSaveable { mutableLongStateOf(initialCategoryId) }
    val toCreate = TaskLogic.bulkTasksToCreate(state.entries)
    val requesters = remember { mutableMapOf<Int, FocusRequester>() }
    var pendingFocus by remember { mutableIntStateOf(-1) }
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)

    // Enter/Next: focus the next title once its field is composed and enabled.
    LaunchedEffect(pendingFocus, state.entries.size) {
        if (pendingFocus >= 0) {
            requesters[pendingFocus]?.let { runCatching { it.requestFocus() }; pendingFocus = -1 }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("New tasks") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.entries.forEachIndexed { i, entry ->
                val focusRequester = remember { FocusRequester() }
                val bringIntoView = remember { BringIntoViewRequester() }
                var focused by remember { mutableStateOf(false) }
                var rowSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
                requesters[i] = focusRequester
                val marginPx = with(LocalDensity.current) { BulkState.NEIGHBOUR_MARGIN_DP.dp.toPx() }
                // Keep the focused row (plus neighbours) visible, also while the keyboard animates open.
                LaunchedEffect(focused, imeBottom, rowSize) {
                    if (focused && rowSize.height > 0) {
                        val (top, bottom) = BulkState.focusRegion(rowSize.height.toFloat(), marginPx)
                        bringIntoView.bringIntoView(Rect(0f, top, rowSize.width.toFloat(), bottom))
                    }
                }
                Row(
                    Modifier.onSizeChanged { rowSize = it }.bringIntoViewRequester(bringIntoView),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("${i + 1}.", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(end = 4.dp))
                    OutlinedTextField(
                        value = entry.title,
                        onValueChange = { state = state.setTitle(i, it) },
                        enabled = state.isEnabled(i),
                        singleLine = true,
                        label = { Text("Title") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { state.nextFocusIndex(i)?.let { pendingFocus = it } }),
                        modifier = Modifier.weight(1f).testTag("bulk-title-$i").focusRequester(focusRequester)
                            .onFocusChanged {
                                focused = it.isFocused
                                if (it.isFocused) state = state.select(i)
                            },
                    )
                }
            }
            Column(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Details for ${state.selected + 1}.", style = MaterialTheme.typography.labelMedium)
                OutlinedTextField(
                    value = state.current.notes, onValueChange = { state = state.setNotes(it) },
                    label = { Text("Notes") }, minLines = 2, maxLines = 3, modifier = Modifier.fillMaxWidth(),
                )
                PriorityEditor(state.current.priority.toFloat()) { state = state.setPriority(Math.round(it)) }
                CategoryDropdown(categories, categoryId) { categoryId = it }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(
                        onClick = { onSave(categoryId, toCreate) },
                        enabled = toCreate.isNotEmpty() && categories.any { it.id == categoryId },
                        modifier = Modifier.weight(1f),
                    ) { Text("Save") }
                }
            }
        }
    }
}
