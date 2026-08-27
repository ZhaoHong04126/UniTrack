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

    val dailySummaryTimeOptions = listOf(
        "07:00" to "07:00",
        "07:30" to "07:30",
        "08:00" to "08:00",
        "08:30" to "08:30",
        "09:00" to "09:00"
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

    val dailyExpenseTimeOptions = listOf(
        "20:00" to "20:00",
        "20:30" to "20:30",
        "21:00" to "21:00",
        "21:30" to "21:30",
        "22:00" to "22:00",
        "22:30" to "22:30",
        "23:00" to "23:00"
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
                            NotificationHelper.openNotificationSettings(context)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "手機系統通知權限設定",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
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

            // 3. Sub-Category Settings Section - 分區塊管理
            Text(
                text = "各功能模組通知細項管理",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )

            // Block 1: 課表與課程提醒
            SectionCard(title = "課表與課程提醒", icon = Icons.Default.CalendarMonth, iconTint = TealSecondary) {
                NotificationOptionRow(
                    icon = Icons.Default.Schedule,
                    iconTint = TealSecondary,
                    title = "課表與上課提醒",
                    subtitle = "啟用每日課表摘要與課前即時推播提醒",
                    checked = preferences.courseReminderEnabled && preferences.masterEnabled,
                    enabled = preferences.masterEnabled,
                    onCheckedChange = {
                        viewModel.updateNotificationPreferences(preferences.copy(courseReminderEnabled = it))
                    }
                )

                if (preferences.courseReminderEnabled && preferences.masterEnabled) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 48.dp, top = 2.dp, bottom = 4.dp),
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

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                    SubOptionRow(
                        title = "每日晨間課表摘要",
                        subtitle = "每日早上 ${preferences.courseDailySummaryTime} 推播當日排課與第一堂課教室",
                        checked = preferences.courseDailySummaryEnabled,
                        onCheckedChange = {
                            viewModel.updateNotificationPreferences(preferences.copy(courseDailySummaryEnabled = it))
                        }
                    )

                    if (preferences.courseDailySummaryEnabled) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 48.dp, top = 2.dp, bottom = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "晨間摘要推播時間：",
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
                                dailySummaryTimeOptions.forEach { (timeStr, label) ->
                                    val isSelected = preferences.courseDailySummaryTime == timeStr
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            viewModel.updateNotificationPreferences(
                                                preferences.copy(courseDailySummaryTime = timeStr)
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

                    SubOptionRow(
                        title = "課程異動即時通知",
                        subtitle = "新增、編輯或調課時立即發送推播通知",
                        checked = preferences.courseChangeNoticeEnabled,
                        onCheckedChange = {
                            viewModel.updateNotificationPreferences(preferences.copy(courseChangeNoticeEnabled = it))
                        }
                    )

                    SubOptionRow(
                        title = "僅在開學期間提醒",
                        subtitle = "寒暑假或非開學週自動靜音，開學後自動恢復",
                        checked = preferences.courseOnlyInSession,
                        onCheckedChange = {
                            viewModel.updateNotificationPreferences(preferences.copy(courseOnlyInSession = it))
                        }
                    )
                }
            }

            // Block 2: 記帳與預算警示
            SectionCard(title = "記帳與預算警示", icon = Icons.Default.AccountBalanceWallet, iconTint = AmberWarning) {
                NotificationOptionRow(
                    icon = Icons.Default.MonetizationOn,
                    iconTint = AmberWarning,
                    title = "記帳與預算警示",
                    subtitle = "每月支出達 ${preferences.expenseAlertThresholdPercent}% 預算警示與月末結算",
                    checked = preferences.expenseAlertEnabled && preferences.masterEnabled,
                    enabled = preferences.masterEnabled,
                    onCheckedChange = {
                        viewModel.updateNotificationPreferences(preferences.copy(expenseAlertEnabled = it))
                    }
                )

                if (preferences.expenseAlertEnabled && preferences.masterEnabled) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 48.dp, top = 2.dp, bottom = 4.dp),
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

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                    SubOptionRow(
                        title = "每日夜間記帳提醒",
                        subtitle = "每晚 ${preferences.expenseDailyReminderTime} 提醒記錄今日消費與日常開支",
                        checked = preferences.expenseDailyReminderEnabled,
                        onCheckedChange = {
                            viewModel.updateNotificationPreferences(preferences.copy(expenseDailyReminderEnabled = it))
                        }
                    )

                    if (preferences.expenseDailyReminderEnabled) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 48.dp, top = 2.dp, bottom = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "夜間記帳提醒時間：",
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
                                dailyExpenseTimeOptions.forEach { (timeStr, label) ->
                                    val isSelected = preferences.expenseDailyReminderTime == timeStr
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            viewModel.updateNotificationPreferences(
                                                preferences.copy(expenseDailyReminderTime = timeStr)
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

                    SubOptionRow(
                        title = "月末支出結算報告",
                        subtitle = "每月最後一日彙整本月總花費與主要支出類別",
                        checked = preferences.expenseMonthlyReportEnabled,
                        onCheckedChange = {
                            viewModel.updateNotificationPreferences(preferences.copy(expenseMonthlyReportEnabled = it))
                        }
                    )

                    SubOptionRow(
                        title = "記帳收支異動即時通知",
                        subtitle = "新增、編輯或刪除記帳明細時立即發送推播通知",
                        checked = preferences.expenseTransactionNoticeEnabled,
                        onCheckedChange = {
                            viewModel.updateNotificationPreferences(preferences.copy(expenseTransactionNoticeEnabled = it))
                        }
                    )
                }
            }

            // Block 3: 學業與畢業審查
            SectionCard(title = "學業與畢業審查", icon = Icons.Default.School, iconTint = SapphirePrimary) {
                NotificationOptionRow(
                    icon = Icons.Default.WorkspacePremium,
                    iconTint = SapphirePrimary,
                    title = "學業與畢業審查",
                    subtitle = "學分門檻達成、GPA 計算與重要學業進度",
                    checked = preferences.graduationAlertEnabled && preferences.masterEnabled,
                    enabled = preferences.masterEnabled,
                    onCheckedChange = {
                        viewModel.updateNotificationPreferences(preferences.copy(graduationAlertEnabled = it))
                    }
                )

                if (preferences.graduationAlertEnabled && preferences.masterEnabled) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                    SubOptionRow(
                        title = "學分門檻達成通知",
                        subtitle = "必修、通識或領域模組滿足畢業門檻時主動推播",
                        checked = preferences.graduationCreditThresholdNotice,
                        onCheckedChange = {
                            viewModel.updateNotificationPreferences(preferences.copy(graduationCreditThresholdNotice = it))
                        }
                    )

                    SubOptionRow(
                        title = "學期 GPA 結算提醒",
                        subtitle = "期末提醒登錄學期成績並自動更新歷年 GPA 趨勢",
                        checked = preferences.graduationGpaSettlementNotice,
                        onCheckedChange = {
                            viewModel.updateNotificationPreferences(preferences.copy(graduationGpaSettlementNotice = it))
                        }
                    )

                    SubOptionRow(
                        title = "畢業缺修學分預警",
                        subtitle = "高年級學期自動檢視尚缺修必修學分與門檻進度",
                        checked = preferences.graduationAuditAlertNotice,
                        onCheckedChange = {
                            viewModel.updateNotificationPreferences(preferences.copy(graduationAuditAlertNotice = it))
                        }
                    )
                }
            }

            // Block 4: 系統與雲端備份
            SectionCard(title = "系統與雲端備份", icon = Icons.Default.CloudSync, iconTint = IndigoAccent) {
                NotificationOptionRow(
                    icon = Icons.Default.Info,
                    iconTint = IndigoAccent,
                    title = "系統公告與備份提醒",
                    subtitle = "雲端備份提醒、版本更新與重要功能公告",
                    checked = preferences.systemNoticeEnabled && preferences.masterEnabled,
                    enabled = preferences.masterEnabled,
                    onCheckedChange = {
                        viewModel.updateNotificationPreferences(preferences.copy(systemNoticeEnabled = it))
                    }
                )

                if (preferences.systemNoticeEnabled && preferences.masterEnabled) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                    SubOptionRow(
                        title = "雲端同步與備份提醒",
                        subtitle = "定期提醒將課表與記帳資料備份至 Firebase 雲端",
                        checked = preferences.systemCloudBackupNotice,
                        onCheckedChange = {
                            viewModel.updateNotificationPreferences(preferences.copy(systemCloudBackupNotice = it))
                        }
                    )

                    SubOptionRow(
                        title = "功能更新與公告通知",
                        subtitle = "UniTrack+ 重大功能更新與學業小幫手公告",
                        checked = preferences.systemUpdateNotice,
                        onCheckedChange = {
                            viewModel.updateNotificationPreferences(preferences.copy(systemUpdateNotice = it))
                        }
                    )
                }
            }

            // Block 5: 提醒方式與回饋
            SectionCard(title = "提醒方式與顯示", icon = Icons.Default.Tune, iconTint = MaterialTheme.colorScheme.primary) {
                NotificationOptionRow(
                    icon = Icons.Default.Vibration,
                    iconTint = SapphirePrimary,
                    title = "震動提醒回饋",
                    subtitle = "推播送達時啟用手機震動提醒",
                    checked = preferences.vibrationEnabled && preferences.masterEnabled,
                    enabled = preferences.masterEnabled,
                    onCheckedChange = {
                        viewModel.updateNotificationPreferences(preferences.copy(vibrationEnabled = it))
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                NotificationOptionRow(
                    icon = Icons.Default.MarkChatUnread,
                    iconTint = EmeraldAccent,
                    title = "桌面圖示角標 (Badge)",
                    subtitle = "在手機主畫面圖示上顯示未讀通知數量",
                    checked = preferences.badgeEnabled && preferences.masterEnabled,
                    enabled = preferences.masterEnabled,
                    onCheckedChange = {
                        viewModel.updateNotificationPreferences(preferences.copy(badgeEnabled = it))
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 2.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            content()
        }
    }
}

@Composable
private fun SubOptionRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.padding(start = 8.dp)
        )
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
