package com.nowwhat.app.screens

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.nowwhat.app.R
import com.nowwhat.app.model.Task
import com.nowwhat.app.model.Urgency
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
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var isPaused by remember { mutableStateOf(true) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var showFinishDialog by remember { mutableStateOf(false) }
    var showDndPermissionDialog by remember { mutableStateOf(false) }
    var isDndEnabled by remember { mutableStateOf(false) }
    var hasCheckedPermission by remember { mutableStateOf(false) }

    val targetSeconds = focusDuration * 60
    val remainingSeconds = (targetSeconds - elapsedSeconds).coerceAtLeast(0)
    val progressPercent = if (targetSeconds > 0) {
        (elapsedSeconds.toFloat() / targetSeconds).coerceIn(0f, 1f)
    } else 0f

    // פונקציה לבדיקה והפעלה של DND
    fun checkAndEnableDnd() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (!notificationManager.isNotificationPolicyAccessGranted) {
                if (!hasCheckedPermission) {
                    showDndPermissionDialog = true
                    hasCheckedPermission = true
                }
            } else {
                isDndEnabled = enableDND(context)
            }
        }
    }

    // ניהול מחזור חיים עבור DND
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkAndEnableDnd()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            disableDND(context)
        }
    }

    // טיימר
    LaunchedEffect(isPaused) {
        while (!isPaused && elapsedSeconds < targetSeconds) {
            delay(1000L)
            elapsedSeconds++
        }
    }

    // דיאלוג הרשאות DND
    if (showDndPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showDndPermissionDialog = false },
            title = { Text("נדרשת הרשאת 'נא לא להפריע'") },
            text = { Text("כדי שמצב פוקוס יוכל להשתיק התראות, האפליקציה זקוקה לאישור גישה להגדרות 'נא לא להפריע'.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDndPermissionDialog = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                        }
                    }
                ) {
                    Text("פתח הגדרות")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDndPermissionDialog = false }) {
                    Text("דלג")
                }
            }
        )
    }

    // דיאלוג ביטול
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

    // דיאלוג סיום
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
            // חלק עליון: פרטי משימה ו-DND
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    task.title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(Modifier.height(8.dp))

                // הצגת הציון ב"מטבע"
                UrgencyChipSmall(task.urgency, task.urgencyScore)

                Spacer(Modifier.height(24.dp))

                // סטטוס DND
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDndEnabled) Color(0xFFFF9800).copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.DoNotDisturb,
                            contentDescription = null,
                            tint = if (isDndEnabled) Color(0xFFFF9800) else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            if (isDndEnabled) stringResource(R.string.focus_dnd_active) else "DND inactive (Tap to Fix)",
                            fontSize = 14.sp,
                            color = if (isDndEnabled) Color(0xFFFF9800) else Color.Gray,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable {
                                if (!isDndEnabled) checkAndEnableDnd()
                            }
                        )
                    }
                }
            }

            // חלק מרכזי: טיימר
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { progressPercent },
                        modifier = Modifier.size(280.dp),
                        strokeWidth = 16.dp,
                        color = Color(0xFF6200EE),
                        trackColor = Color(0xFFE0E0E0)
                    )

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
                            if(isPaused) "Ready to Focus?" else stringResource(R.string.focus_stay_focused),
                            fontSize = 16.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

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

            // חלק תחתון: כפתורים
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { isPaused = !isPaused },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isPaused) Color(0xFFFF9800) else Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        if (!isPaused) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (!isPaused) stringResource(R.string.focus_pause) else "Start Focus",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

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
fun UrgencyChipSmall(urgency: Urgency, score: Int) {
    val color = when (urgency) {
        Urgency.Critical -> Color(0xFFD32F2F)
        Urgency.VeryHigh -> Color(0xFFFF5722)
        Urgency.High -> Color(0xFFFF9800)
        Urgency.Medium -> Color(0xFF4CAF50)
        Urgency.Low -> Color(0xFF2196F3)
    }

    // עיצוב "מטבע" עם ציון בלבד
    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(2.dp, color),
        modifier = Modifier.size(50.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = score.toString(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

fun formatTime(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, secs)
    else String.format("%02d:%02d", minutes, secs)
}

private fun enableDND(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.isNotificationPolicyAccessGranted) {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            return true
        }
    }
    return false
}

private fun disableDND(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.isNotificationPolicyAccessGranted) {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        }
    }
}