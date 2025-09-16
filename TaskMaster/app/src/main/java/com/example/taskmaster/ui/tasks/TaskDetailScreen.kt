package com.example.taskmaster.ui.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.taskmaster.data.Task
import com.example.taskmaster.ui.theme.TaskMasterTheme
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.tooling.preview.Preview
import com.example.taskmaster.R
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    onNavigateUp: () -> Unit,
    taskDetailViewModel: TaskDetailViewModel = viewModel()
) {
    val task by taskDetailViewModel.task.collectAsStateWithLifecycle()
    var showEditDialog by remember { mutableStateOf(false) }
    //val errorMessage by taskDetailViewModel.errorMessage.collectAsStateWithLifecycle()

    // Show error dialog if an error occurs
//    errorMessage?.let {
//        AlertDialog(
//            onDismissRequest = { taskDetailViewModel.dismissError() },
//            title =  { Text(stringResource(R.string.error_dialog_title))},
//            text = { Text(it) },
//            confirmButton = { Button(onClick = { taskDetailViewModel.dismissError() }) { Text( stringResource(R.string.ok)) } }
//        )
//    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.task_details_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, contentDescription =  stringResource(R.string.back_button_desc))
                    }
                },
                actions = {
                    // Only show the edit button if the task has loaded
                    if (task != null) {
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription =  stringResource(R.string.edit_task_desc))
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (task == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            // We pass the non-nullable task to the content
            TaskDetailContent(
                modifier = Modifier.padding(paddingValues),
                task = task!!
            )
        }
    }

    // Show the Edit dialog when the state is true
    if (showEditDialog && task != null) {
        EditTaskDialog(
            task = task!!,
            onDismiss = { showEditDialog = false },
            onConfirm = { title, desc, dateMillis ->
                taskDetailViewModel.updateTask(title, desc, dateMillis)
                showEditDialog = false
            }
        )
    }
}

@Composable
fun TaskDetailContent(modifier: Modifier = Modifier, task: Task) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = task.title,
            style = MaterialTheme.typography.headlineMedium,
            textDecoration = if (task.isDone) TextDecoration.LineThrough else null
        )
        Spacer(modifier = Modifier.height(8.dp))
        formatDueDate(task.dueDate)?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (!task.description.isNullOrBlank()) {
            Text(
                text = task.description,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskDialog(
    task: Task,
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String, dueDateMillis: Long?) -> Unit
) {
    var title by rememberSaveable { mutableStateOf(task.title) }
    var description by rememberSaveable { mutableStateOf(task.description ?: "") }

    // Convert ISO string back to millis for the date picker state
    val initialDateMillis: Long? = remember(task.dueDate) {
        task.dueDate?.let {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                sdf.parse(it)?.time
            } catch (e: Exception) { null }
        }
    }
    var dueDateMillis by rememberSaveable { mutableStateOf(initialDateMillis) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dueDateMillis)

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        dueDateMillis = datePickerState.selectedDateMillis
                        showDatePicker = false
                    }
                ) { Text( stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text( stringResource(R.string.cancel)) } }
        ) { DatePicker(state = datePickerState) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Task") },
        text = {
            Column {
                TextField(value = title, onValueChange = { title = it }, label = { Text( stringResource(R.string.title)) })
                Spacer(modifier = Modifier.height(8.dp))
                TextField(value = description, onValueChange = { description = it }, label = { Text( stringResource(R.string.description)) })
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dueDateMillis?.let {
                            "Due: ${SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(it))}"
                        } ?:  stringResource(R.string.no_due_date),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription =  stringResource(R.string.select_due_date_desc))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, description, dueDateMillis) },
                enabled = title.isNotBlank()
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text( stringResource(R.string.cancel)) } }
    )
}
// --- PREVIEW ---

@Preview(showBackground = true)
@Composable
fun TaskDetailScreenPreview() {
    TaskMasterTheme {
        // We can't easily preview this screen because it depends on a ViewModel
        // that requires a taskId. A stateless version would be needed for a good preview.
        // For now, we know the components (Text, Spacer) work.
    }
}