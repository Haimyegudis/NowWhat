package com.nowwhat.app.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowwhat.app.R
import com.nowwhat.app.model.Priority
import com.nowwhat.app.model.Severity
import com.nowwhat.app.model.Task
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusModeScreen(
    task: Task,
    focusDuration: Int,
    onBackClick: () -> Unit,
    onFinishTask: (actualMinutes: Int) -> Unit
) {
    var elapsedSeconds by remember { mutableStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var showFinishDialog by remember { mutableStateOf(false) }

    val targetSeconds = focusDuration * 60
    val remainingSeconds = (targetSeconds - elapsedSeconds).coerceAtLeast(0)
    val progressPercent = if (targetSeconds > 0) {
        (elapsedSeconds.toFloat() / targetSeconds).coerceIn(0f, 1f)
    } else 0f

    // Timer effect
    LaunchedEffect(isPaused) {
        while (!isPaused && elapsedSeconds < targetSeconds) {
            delay(1000L)
            elapsedSeconds++
        }
    }

    // Cancel Dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text(stringResource(R.string.focus_cancel)) },
            text = { Text(stringResource(R.string.focus_confirm_cancel)) },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelDialog = false
                        onBackClick()
                    }
                ) {
                    Text(stringResource(R.string.project_detail_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text(stringResource(R.string.create_project_cancel))
                }
            }
        )
    }

    // Finish Dialog
    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text(stringResource(R.string.focus_finish)) },
            text = { Text(stringResource(R.string.focus_confirm_finish)) },
            confirmButton = {
                Button(
                    onClick = {
                        val actualMinutes = (elapsedSeconds / 60.0).roundToInt()
                        onFinishTask(actualMinutes)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Text(stringResource(R.string.project_detail_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) {
                    Text(stringResource(R.string.create_project_cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.focus_title)) },
                navigationIcon = {
                    IconButton(onClick = { showCancelDialog = true }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6200EE),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Task Info
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    task.title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PriorityChipSmall(task.priority)
                    SeverityChipSmall(task.severity)
                }

                Spacer(Modifier.height(24.dp))

                // DND Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFF9800).copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.DoNotDisturb,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            stringResource(R.string.focus_dnd_active),
                            fontSize = 14.sp,
                            color = Color(0xFFFF9800),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Timer Circle
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    // Progress Circle
                    CircularProgressIndicator(
                        progress = { progressPercent },
                        modifier = Modifier.size(280.dp),
                        strokeWidth = 16.dp,
                        color = Color(0xFF6200EE),
                        trackColor = Color(0xFFE0E0E0)
                    )

                    // Time Display
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            formatTime(remainingSeconds),
                            fontSize = 56.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6200EE)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.focus_stay_focused),
                            fontSize = 16.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                // Stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        label = stringResource(R.string.focus_time_spent),
                        value = formatTime(elapsedSeconds),
                        color = Color(0xFF2196F3)
                    )
                    StatItem(
                        label = stringResource(R.string.focus_estimated_time),
                        value = "${task.estimatedMinutes}m",
                        color = Color(0xFFFF9800)
                    )
                }
            }

            // Control Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Pause/Resume Button
                Button(
                    onClick = { isPaused = !isPaused },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPaused) Color(0xFF4CAF50) else Color(0xFFFF9800)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isPaused)
                            stringResource(R.string.focus_resume)
                        else
                            stringResource(R.string.focus_pause),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Finish Button
                OutlinedButton(
                    onClick = { showFinishDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.focus_finish),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun PriorityChipSmall(priority: Priority) {
    val (emoji, color) = when (priority) {
        Priority.Immediate -> "⚡" to Color(0xFFFF5722)
        Priority.High -> "🔴" to Color(0xFFFF9800)
        Priority.Medium -> "🟡" to Color(0xFFFFC107)
        Priority.Low -> "🟢" to Color(0xFF4CAF50)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 14.sp)
            Text(
                priority.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun SeverityChipSmall(severity: Severity) {
    val (emoji, color) = when (severity) {
        Severity.Critical -> "💀" to Color(0xFFD32F2F)
        Severity.High -> "⚠️" to Color(0xFFFF5722)
        Severity.Medium -> "📊" to Color(0xFF2196F3)
        Severity.Low -> "📋" to Color(0xFF9E9E9E)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 14.sp)
            Text(
                severity.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            fontSize = 12.sp,
            color = Color.Gray
        )
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

fun formatTime(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60

    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%02d:%02d", minutes, secs)
    }
}