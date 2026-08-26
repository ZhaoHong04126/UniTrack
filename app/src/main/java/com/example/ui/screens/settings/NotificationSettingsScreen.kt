package com.example.ui.screens.settings

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudentViewModel
import com.example.util.NotificationHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    viewModel: StudentViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val preferences by viewModel.notificationPreferences.collectAsStateWithLifecycle()

    var isSystemPermissionGranted by remember {
        mutableStateOf(NotificationHelper.hasNotificationPermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        isSystemPermissionGranted = isGranted
        if (isGranted) {
            Toast.makeText(context, "已成功開啟系統推播通知權限", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "系統推播通知權限未開啟", Toast.LENGTH_SHORT).show()
        }
    }

    LifecycleResumeEffect(Unit) {
        isSystemPermissionGranted = NotificationHelper.hasNotificationPermission(context)
        onPauseOrDispose { }
    }

    val reminderOptions = listOf(
        10 to "10 分鐘前",
        15 to "15 分鐘前",
        30 to "30 分鐘前",
        60 to "1 小時前"
    )

    val expenseThresholdOptions = listOf(
        50 to "50%",
        60 to "60%",
        70 to "70%",
        75 to "75%",
        80 to "80%",
        90 to "90%",
        100 to "100%"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "系統推播通知",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    IconButton(
                        onClick = {
                            if (!isSystemPermissionGranted) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    NotificationHelper.openNotificationSettings(context)
                                }
                            } else {
                                viewModel.sendTestSystemNotification()
                                Toast.makeText(context, "已發送測試推播通知！請查看上方通知列", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddAlert,
                            contentDescription = "發送測試推播",
                            tint = SapphirePrimary
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. System OS Permission Status Banner
            if (isSystemPermissionGranted) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = EmeraldLight.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(EmeraldAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldAccent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "手機系統通知權限：已允許",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldAccent
                            )
                            Text(
                                text = "已允許 UniTrack+ 在手機通知列與鎖定畫面推播提醒",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = AmberLight.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(AmberAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsOff,
                                contentDescription = null,
                                tint = AmberAccent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "手機系統推播通知未開啟",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "通知無法在狀態列即時顯示，請點擊允許通知",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    NotificationHelper.openNotificationSettings(context)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberAccent),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "開啟通知",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Quick Test Notification Trigger Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = SapphirePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "即時測試推播通知",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "點擊下方按鈕測試不同情境的系統推播，確認手機狀態列是否能即時彈出橫幅：",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                if (!isSystemPermissionGranted) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        NotificationHelper.openNotificationSettings(context)
                                    }
                                } else {
                                    viewModel.sendNotification(
                                        title = "今日上課提醒",
                                        message = "下午 14:00 有「線性代數」課程，教室：理學院 302。",
                                        type = com.example.data.model.NotificationType.COURSE,
                                        actionRoute = "timetable"
                                    )
                                    Toast.makeText(context, "已發送「課表提醒」測試通知", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("📅 課表提醒", style = MaterialTheme.typography.labelMedium)
                        }

                        FilledTonalButton(
                            onClick = {
                                if (!isSystemPermissionGranted) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        NotificationHelper.openNotificationSettings(context)
                                    }
                                } else {
                                    viewModel.sendNotification(
                                        title = "⚠️ 記帳預算警示",
                                        message = "本月份生活預算已使用達 ${preferences.expenseAlertThresholdPercent}%，請留意近期支出。",
                                        type = com.example.data.model.NotificationType.EXPENSE,
                                        actionRoute = "expense"
                                    )
                                    Toast.makeText(context, "已發送「記帳預算」測試通知", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("💰 預算警示", style = MaterialTheme.typography.labelMedium)
                        }

                        FilledTonalButton(
                            onClick = {
                                if (!isSystemPermissionGranted) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        NotificationHelper.openNotificationSettings(context)
                                    }
                                } else {
                                    viewModel.sendNotification(
                                        title = "學業審查進度更新",
                                        message = "恭喜！您已滿足本系基礎模組必修 24 學分門檻。",
                                        type = com.example.data.model.NotificationType.GRADUATION,
                                        actionRoute = "graduation"
                                    )
                                    Toast.makeText(context, "已發送「學業審查」測試通知", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("🎓 畢業審查", style = MaterialTheme.typography.labelMedium)
                        }

                        FilledTonalButton(
                            onClick = {
                                if (!isSystemPermissionGranted) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        NotificationHelper.openNotificationSettings(context)
                                    }
                                } else {
                                    viewModel.sendNotification(
                                        title = "UniTrack+ 系統推播測試",
                                        message = "這是一則測試通知，代表手機系統推播功能運作正常！",
                                        type = com.example.data.model.NotificationType.SYSTEM,
                                        actionRoute = "notifications"
                                    )
                                    Toast.makeText(context, "已發送「系統推播」測試通知", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("📢 系統推播", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            // 2. Master App Toggle Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(SapphirePrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = SapphirePrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "接收所有應用程式通知",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "開啟以掌握課表、預算超標與學業進度",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = preferences.masterEnabled,
                        onCheckedChange = {
                            viewModel.updateNotificationPreferences(preferences.copy(masterEnabled = it))
                        }
                    )
                }
            }

            // 3. Sub-Category Settings Section
            Text(
                text = "各功能模組通知細項",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Item 1: Course
                    NotificationOptionRow(
                        icon = Icons.Default.CalendarMonth,
                        iconTint = TealSecondary,
                        title = "課表與上課提醒",
                        subtitle = "每日課表摘要與課前即時推播提醒",
                        checked = preferences.courseReminderEnabled && preferences.masterEnabled,
                        enabled = preferences.masterEnabled,
                        onCheckedChange = {
                            viewModel.updateNotificationPreferences(preferences.copy(courseReminderEnabled = it))
                        }
                    )

                    // Sub-timing Chips for course reminder
                    if (preferences.courseReminderEnabled && preferences.masterEnabled) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 52.dp, top = 2.dp, bottom = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "課前提醒時間：",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                reminderOptions.forEach { (mins, label) ->
                                    val isSelected = preferences.courseReminderMinutesBefore == mins
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            viewModel.updateNotificationPreferences(
                                                preferences.copy(courseReminderMinutesBefore = mins)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = TealSecondary.copy(alpha = 0.18f),
                                            selectedLabelColor = TealSecondary
                                        )
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                    // Item 2: Expense
                    NotificationOptionRow(
                        icon = Icons.Default.AccountBalanceWallet,
                        iconTint = AmberWarning,
                        title = "記帳與預算警示",
                        subtitle = "每月支出達 ${preferences.expenseAlertThresholdPercent}% 預算警示與月末結算",
                        checked = preferences.expenseAlertEnabled && preferences.masterEnabled,
                        enabled = preferences.masterEnabled,
                        onCheckedChange = {
                            viewModel.updateNotificationPreferences(preferences.copy(expenseAlertEnabled = it))
                        }
                    )

                    // Sub-threshold Chips for expense alert
                    if (preferences.expenseAlertEnabled && preferences.masterEnabled) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 52.dp, top = 2.dp, bottom = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "預算超支提醒門檻：",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                expenseThresholdOptions.forEach { (percent, label) ->
                                    val isSelected = preferences.expenseAlertThresholdPercent == percent
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            viewModel.updateNotificationPreferences(
                                                preferences.copy(expenseAlertThresholdPercent = percent)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = AmberWarning.copy(alpha = 0.18f),
                                            selectedLabelColor = AmberAccent
                                        )
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                    // Item 3: Graduation
                    NotificationOptionRow(
                        icon = Icons.Default.School,
                        iconTint = SapphirePrimary,
                        title = "學業與畢業審查",
                        subtitle = "學分門檻達成、GPA 計算與重要學業進度",
                        checked = preferences.graduationAlertEnabled && preferences.masterEnabled,
                        enabled = preferences.masterEnabled,
                        onCheckedChange = {
                            viewModel.updateNotificationPreferences(preferences.copy(graduationAlertEnabled = it))
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                    // Item 4: System
                    NotificationOptionRow(
                        icon = Icons.Default.Info,
                        iconTint = IndigoAccent,
                        title = "系統公告與備份提醒",
                        subtitle = "版本更新、雲端備份提醒與重要功能公告",
                        checked = preferences.systemNoticeEnabled && preferences.masterEnabled,
                        enabled = preferences.masterEnabled,
                        onCheckedChange = {
                            viewModel.updateNotificationPreferences(preferences.copy(systemNoticeEnabled = it))
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                    // Item 5: Vibration
                    NotificationOptionRow(
                        icon = Icons.Default.Vibration,
                        iconTint = SapphirePrimary,
                        title = "震動提醒回饋",
                        subtitle = "推播送達時啟用手機震動回饋",
                        checked = preferences.vibrationEnabled && preferences.masterEnabled,
                        enabled = preferences.masterEnabled,
                        onCheckedChange = {
                            viewModel.updateNotificationPreferences(preferences.copy(vibrationEnabled = it))
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun NotificationOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = if (enabled) 0.12f else 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) iconTint else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 0.8f else 0.4f)
                )
            }
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
    }
}
