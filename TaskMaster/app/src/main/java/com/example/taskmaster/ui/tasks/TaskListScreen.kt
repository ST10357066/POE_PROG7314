package com.example.taskmaster.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.taskmaster.data.Task
import com.example.taskmaster.ui.theme.TaskMasterTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.ui.res.stringResource
import com.example.taskmaster.R
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import java.util.Calendar
/**
 * The "stateful" screen that connects to the ViewModel.
 * This is what our navigation graph calls.
 */
@Composable
fun TaskListScreen(taskListViewModel: TaskListViewModel,
                   onTaskClick: (Task) -> Unit) {
    val tasks by taskListViewModel.tasks.collectAsStateWithLifecycle()

    TaskListScreenContent(
        tasks = tasks,
        onAddTask = { title, desc, dateMillis -> taskListViewModel.addTask(title, desc, dateMillis) },
        onTaskCheckedChange = { task, isChecked ->
            // Connect the checkbox action
            taskListViewModel.updateTaskStatus(task, isChecked)
        },
        onDeleteTask = { task ->
            // Connect the delete action
            taskListViewModel.deleteTask(task)
        },
        onTaskClick = onTaskClick
    )
}

fun formatDueDate(isoDate: String?): String? {
    if (isoDate.isNullOrBlank()) return null
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        parser.timeZone = TimeZone.getTimeZone("UTC")
        val date = parser.parse(isoDate)
        val formatter = SimpleDateFormat("MMM d", Locale.getDefault())
        // We will build the final string in the composable itself
        formatter.format(date!!)
    } catch (e: Exception) {
        null
    }
}

/**
 * The "stateless" screen. It is dumb, only knows how to display state,
 * and is perfectly previewable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreenContent(
    tasks: List<Task>,
    onAddTask: (title: String, description: String, dueDateMillis: Long?) -> Unit,
    onTaskCheckedChange: (task: Task, isChecked: Boolean) -> Unit,
    onDeleteTask: (Task) -> Unit, // Add the new delete handler
    onTaskClick: (Task) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription =  stringResource(R.string.add_new_task))
            }
        }
    ) { paddingValues ->
        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.no_tasks_message))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(Modifier.height(8.dp)) } // Padding at the top
                items(tasks, key = { it.id }) { task -> // Use a key for better performance
                    SwipeToDeleteContainer(
                        item = task,
                        onDelete = { onDeleteTask(task) }
                    ) {
                        TaskItem(
                            task = task,
                            onCheckedChange = { isChecked -> onTaskCheckedChange(task, isChecked) },
                            onClick = { onTaskClick(task) }
                        )
                    }
                }
                item { Spacer(Modifier.height(8.dp)) } // Padding at the bottom
            }
        }

        if (showDialog) {
            AddTaskDialog(
                onDismiss = { showDialog = false },
                onConfirm = { title, desc, dateMillis ->
                    onAddTask(title, desc, dateMillis)
                    showDialog = false
                }
            )
        }
    }
}

/**
 * A single row item for a task.
 */
@Composable
fun TaskItem(task: Task, onCheckedChange: (Boolean) -> Unit,onClick: () -> Unit) {
    // By using the task.isDone value as a "key", we are explicitly telling
    // Compose that this Card and its children should be completely redrawn
    // if and only if the isDone value changes.
    key(task) {
        Card(modifier = Modifier.fillMaxWidth(),
            onClick = onClick
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = task.isDone,
                    onCheckedChange = onCheckedChange
                )
                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyLarge,
                        textDecoration = if (task.isDone) TextDecoration.LineThrough else null
                    )

                    formatDueDate(task.dueDate)?.let { formattedDate ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${stringResource(R.string.due_date_prefix)}: $formattedDate", 
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * The dialog for adding a new task.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String, dueDateMillis: Long?) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var dueDateMillis by rememberSaveable { mutableStateOf<Long?>(null) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState()
    // Initialize time picker to the current hour and minute
    val calendar = Calendar.getInstance()
    val timePickerState = rememberTimePickerState(
        initialHour = calendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = calendar.get(Calendar.MINUTE)
    )

    // --- Date Picker Dialog ---
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                        // IMPORTANT: After picking a date, immediately show the time picker
                        showTimePicker = true
                    }
                ) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) } }
        ) { DatePicker(state = datePickerState) }
    }

    // --- Time Picker Dialog ---
    if (showTimePicker) {
        // A simple dialog wrapper for the TimePicker
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Time") },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showTimePicker = false
                        // Combine the selected date and time into a final timestamp
                        val selectedDate = Calendar.getInstance().apply {
                            timeInMillis = datePickerState.selectedDateMillis!!
                        }
                        selectedDate.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        selectedDate.set(Calendar.MINUTE, timePickerState.minute)

                        dueDateMillis = selectedDate.timeInMillis
                    }
                ) { Text(stringResource(R.string.ok)) }
            }
        )
    }


    // --- Main Add Task Dialog ---
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_new_task)) },
        text = {
            Column {
                TextField(value = title, onValueChange = { title = it }, label = { Text(stringResource(R.string.title)) })
                Spacer(modifier = Modifier.height(8.dp))
                TextField(value = description, onValueChange = { description = it }, label = { Text(stringResource(R.string.description)) })
                Spacer(modifier = Modifier.height(16.dp))

                // The UI to show the selected Date and Time
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dueDateMillis?.let {
                            // --- UPDATE the date format to include time ---
                            "${stringResource(R.string.due_date_prefix)}: ${SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault()).format(Date(it))}"
                        } ?: stringResource(R.string.no_due_date),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = stringResource(R.string.select_due_date_desc))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, description, dueDateMillis) },
                enabled = title.isNotBlank()
            ) { Text(stringResource(R.string.add)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SwipeToDeleteContainer(
    item: T,
    onDelete: (T) -> Unit,
    content: @Composable (T) -> Unit
) {
    // This is the new, correct state for the M3 component
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            // Check if the user has swiped fully to the end (left or right)
            if (it == SwipeToDismissBoxValue.EndToStart || it == SwipeToDismissBoxValue.StartToEnd) {
                onDelete(item)
                return@rememberSwipeToDismissBoxState true // Confirm the deletion
            }
            false // Don't dismiss for other states (like settling)
        },
        // We can set a positional threshold, e.g., swipe 50% to dismiss
        positionalThreshold = { it * .5f }
    )

    // This is the new, correct Composable
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true, // Allow swiping right
        enableDismissFromEndToStart = true, // Allow swiping left
        backgroundContent = {
            val color = when (dismissState.targetValue) {
                SwipeToDismissBoxValue.EndToStart -> Color.Red.copy(alpha = 0.8f)
                SwipeToDismissBoxValue.StartToEnd -> Color.Red.copy(alpha = 0.8f)
                else -> Color.Transparent
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CardDefaults.shape)
                    .background(color)
                    .padding(12.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete_task_desc),
                    tint = Color.White,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete_task_desc),
                    tint = Color.White,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }
    ) {
        // This is where your actual TaskItem will go
        content(item)
    }
}



// --- PREVIEWS ---

@Preview(showBackground = true, name = "Task List States")
@Composable
fun TaskListScreenPreview(
    @PreviewParameter(TaskPreviewParameterProvider::class) tasks: List<Task>
) {
    TaskMasterTheme {
        TaskListScreenContent(
            tasks = tasks,
            onAddTask = { _, _, _ -> },
            onTaskCheckedChange = { _, _ -> },
            onDeleteTask = { _ -> },
            onTaskClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Add Task Dialog")
@Composable
fun AddTaskDialogPreview() {
    TaskMasterTheme {
        AddTaskDialog(
            onDismiss = {},
            onConfirm = { _, _, _ -> }
        )
    }
}


class TaskPreviewParameterProvider : PreviewParameterProvider<List<Task>> {
    override val values = sequenceOf(
        // Case 1: An empty list of tasks
        emptyList(),

        // Case 2: A list with a few sample tasks
        listOf(
            Task("1", "uid1", "Buy groceries", "Milk, bread, eggs", false, null, ""),
            Task("2", "uid1", "Finish the report", "Final draft due EOD", true, null, ""),
            Task("3", "uid1", "Call the dentist", "", false, null, ""),
            Task("4", "uid1", "Water the plants", null, false, null, "")
        )
    )
}