// קובץ: app/src/main/java/com/nowwhat/app/screens/AddSubTaskDialog.kt
package com.nowwhat.app.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.nowwhat.app.model.SubTask

@Composable
fun AddSubTaskDialog(
    taskId: Int,
    onDismiss: () -> Unit,
    onConfirm: (SubTask) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var titleError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Item") }, // שיניתי מ-SubTask ל-Item לתחושה קלילה יותר
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        titleError = false
                    },
                    label = { Text("What needs to be done?") },
                    isError = titleError,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = true
                    } else {
                        onConfirm(
                            SubTask(
                                taskId = taskId,
                                title = title.trim(),
                                estimatedMinutes = 0, // אוטומטית 0
                                deadline = null       // אוטומטית ללא דד-ליין
                            )
                        )
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}