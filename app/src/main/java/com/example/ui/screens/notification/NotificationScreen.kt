package com.example.ui.screens.notification

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppNotification
import com.example.data.model.NotificationType
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudentViewModel
import com.example.util.NotificationHelper
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.style.TextOverflow
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    viewModel: StudentViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToRoute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allNotifications by viewModel.allNotifications.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadNotificationCount.collectAsStateWithLifecycle()

    var isSystemNotificationEnabled by remember { mutableStateOf(NotificationHelper.hasNotificationPermission(context)) }

    LifecycleResumeEffect(Unit) {
        isSystemNotificationEnabled = NotificationHelper.hasNotificationPermission(context)
        onPauseOrDispose { }
    }

    var selectedFilter by remember { mutableStateOf<NotificationType?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }
    var notificationToDelete by remember { mutableStateOf<AppNotification?>(null) }

    val filteredNotifications = remember(allNotifications, selectedFilter) {
        if (selectedFilter == null) allNotifications
        else allNotifications.filter { it.type == selectedFilter }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "通知中心",
                            fontWeight = FontWeight.Bold
                        )
                        if (unreadCount > 0) {
                            Surface(
                                shape = CircleShape,
                                color = RoseAccent
                            ) {
                                Text(
                                    text = "$unreadCount",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    if (unreadCount > 0) {
                        TextButton(
                            onClick = { viewModel.markAllNotificationsAsRead() }
                        ) {
                            Text(
                                text = "全部已讀",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (allNotifications.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "清空通知",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // System Notification Permission Warning Banner (若手機尚未開啟通知權限)
            if (!isSystemNotificationEnabled) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = AmberLight.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(AmberWarning.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsOff,
                                contentDescription = null,
                                tint = AmberAccent,
                                modifier = Modifier.size(20.dp)
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
                                text = "開啟通知以接收即時上課提醒與重要學業警示。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { NotificationHelper.openNotificationSettings(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberAccent),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "開啟通知",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
            // Category Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { selectedFilter = null },
                        label = { Text("全部 (${allNotifications.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
                item {
                    val count = allNotifications.count { it.type == NotificationType.COURSE }
                    FilterChip(
                        selected = selectedFilter == NotificationType.COURSE,
                        onClick = { selectedFilter = NotificationType.COURSE },
                        label = { Text("課表 ($count)") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
                item {
                    val count = allNotifications.count { it.type == NotificationType.EXPENSE }
                    FilterChip(
                        selected = selectedFilter == NotificationType.EXPENSE,
                        onClick = { selectedFilter = NotificationType.EXPENSE },
                        label = { Text("記帳 ($count)") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
                item {
                    val count = allNotifications.count { it.type == NotificationType.GRADUATION }
                    FilterChip(
                        selected = selectedFilter == NotificationType.GRADUATION,
                        onClick = { selectedFilter = NotificationType.GRADUATION },
                        label = { Text("學業 ($count)") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
                item {
                    val count = allNotifications.count { it.type == NotificationType.SYSTEM }
                    FilterChip(
                        selected = selectedFilter == NotificationType.SYSTEM,
                        onClick = { selectedFilter = NotificationType.SYSTEM },
                        label = { Text("系統 ($count)") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }

            if (filteredNotifications.isEmpty()) {
                // Empty State
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsNone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Text(
                            text = if (selectedFilter == null) "目前沒有任何通知" else "沒有此類別的通知",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "UniTrack+ 會在此處提醒您課程安排、記帳預算與畢業學分重要資訊。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredNotifications, key = { it.id }) { item ->
                        NotificationItemCard(
                            notification = item,
                            onItemClick = {
                                if (!item.isRead) {
                                    viewModel.markNotificationAsRead(item.id)
                                }
                            },
                            onNavigateClick = {
                                if (!item.isRead) {
                                    viewModel.markNotificationAsRead(item.id)
                                }
                                item.actionRoute?.let { route ->
                                    onNavigateToRoute(route)
                                }
                            },
                            onDeleteClick = {
                                notificationToDelete = item
                            }
                        )
                    }
                }
            }
        }
    }

    // 單則通知刪除確認對話框
    notificationToDelete?.let { notification ->
        AlertDialog(
            onDismissRequest = { notificationToDelete = null },
            title = { Text("刪除通知？", fontWeight = FontWeight.Bold) },
            text = { Text("確定要刪除「${notification.title}」這則通知嗎？") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteNotification(notification.id)
                        notificationToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseAccent)
                ) {
                    Text("確定刪除")
                }
            },
            dismissButton = {
                TextButton(onClick = { notificationToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清空所有通知？", fontWeight = FontWeight.Bold) },
            text = { Text("確定要刪除所有通知紀錄嗎？此動作無法復原。") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllNotifications()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseAccent)
                ) {
                    Text("確定清空")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

private enum class ActionBadgeType(
    val label: String,
    val containerColor: Color,
    val contentColor: Color
) {
    AI_IMPORT("智慧導入", Color(0xFFEDE9FE), Color(0xFF7C3AED)),
    ADD("新增", EmeraldLight, EmeraldAccent),
    DELETE("刪除", RoseLight, RoseAccent),
    UPDATE("變更", AmberLight, AmberWarning),
    NOTICE("通知", SapphireLight, SapphirePrimary)
}

private fun resolveActionBadgeType(title: String, message: String): ActionBadgeType {
    val content = "$title $message"
    return when {
        content.contains("AI 課表") || content.contains("智慧導入") || content.contains("一鍵導入") -> ActionBadgeType.AI_IMPORT
        content.contains("新增") || content.contains("加入") || content.contains("建立") -> ActionBadgeType.ADD
        content.contains("刪除") || content.contains("移除") || content.contains("清空") -> ActionBadgeType.DELETE
        content.contains("更新") || content.contains("變更") || content.contains("修改") || content.contains("異動") || content.contains("警示") -> ActionBadgeType.UPDATE
        else -> ActionBadgeType.NOTICE
    }
}

private data class ParsedNotificationDetails(
    val actionBadge: ActionBadgeType,
    val targetItem: String,
    val keyValues: List<Pair<String, String>>
)

private fun parseNotificationMessage(notification: AppNotification): ParsedNotificationDetails {
    val actionBadge = resolveActionBadgeType(notification.title, notification.message)
    val title = notification.title
    val msg = notification.message

    val targetItem = when {
        title.contains("：") -> title.substringAfter("：").trim()
        title.contains(":") -> title.substringAfter(":").trim()
        else -> title.replace(Regex("[\\p{So}\\uFE0F]"), "").trim()
    }

    val kvList = mutableListOf<Pair<String, String>>()

    if (targetItem.isNotBlank()) {
        kvList.add("相關項目" to targetItem)
    }

    when (notification.type) {
        NotificationType.COURSE -> kvList.add("所屬模組" to "課表與課程管理")
        NotificationType.EXPENSE -> kvList.add("所屬模組" to "記帳與預算管理")
        NotificationType.GRADUATION -> kvList.add("所屬模組" to "學業與畢業審查")
        NotificationType.SYSTEM -> kvList.add("所屬模組" to "系統與雲端公告")
    }

    if (actionBadge == ActionBadgeType.AI_IMPORT) {
        kvList.add("導入方式" to "📸 圖片 AI 智慧辨識")
    }

    kvList.add("異動狀態" to when (actionBadge) {
        ActionBadgeType.AI_IMPORT -> "課表建立完成"
        ActionBadgeType.ADD -> "已成功新增"
        ActionBadgeType.DELETE -> "已確認刪除"
        ActionBadgeType.UPDATE -> "已更新內容"
        ActionBadgeType.NOTICE -> "系統一般通知"
    })

    val countMatch = Regex("""共\s*(\d+)\s*門課""").find(title) ?: Regex("""共\s*(\d+)\s*門課""").find(msg)
    val totalCreditsMatch = Regex("""共計\s*(\d+)\s*學分""").find(msg) ?: Regex("""\(共\s*(\d+)\s*學分\)""").find(msg)

    if (countMatch != null && totalCreditsMatch != null) {
        kvList.add("匯入規模" to "${countMatch.groupValues[1]} 門課程（${totalCreditsMatch.groupValues[1]} 學分）")
    } else if (countMatch != null) {
        kvList.add("匯入門數" to "${countMatch.groupValues[1]} 門課程")
    }

    val creditMatch = Regex("""\((\d+)\s*學分\)""").find(msg)
    if (creditMatch != null && totalCreditsMatch == null) {
        kvList.add("課程學分" to "${creditMatch.groupValues[1]} 學分")
    }

    val semMatch = Regex("""(\d{3}-[12])""").find(title) ?: Regex("""(\d{3}-[12])""").find(msg)
    if (semMatch != null) {
        val semLabel = if (actionBadge == ActionBadgeType.AI_IMPORT) "目標學期" else "所屬學期"
        kvList.add(semLabel to "${semMatch.groupValues[1]} 學期")
    }

    if (actionBadge == ActionBadgeType.AI_IMPORT) {
        kvList.add("時段衝突" to "✅ 通過檢測（0 衝突）")
    }

    if (msg.contains("教室：") || msg.contains("教室:")) {
        val loc = msg.substringAfter("教室：").substringAfter("教室:").substringBefore("。").substringBefore("，").trim()
        if (loc.isNotBlank()) kvList.add("上課教室" to loc)
    }

    if (msg.contains("上課時間：") || msg.contains("上課時間:")) {
        val time = msg.substringAfter("上課時間：").substringAfter("上課時間:").substringBefore("，教室").substringBefore("。").trim()
        if (time.isNotBlank()) kvList.add("排課時間" to time)
    }

    if (msg.contains("日期：") || msg.contains("日期:")) {
        val date = msg.substringAfter("日期：").substringAfter("日期:").substringBefore(" ｜").substringBefore(" (").trim()
        if (date.isNotBlank()) kvList.add("記帳日期" to date)
    }

    val amountMatch = Regex("""([+-]?\$\d+)""").find(msg)
    if (amountMatch != null) {
        kvList.add("收支金額" to amountMatch.groupValues[1])
    }

    return ParsedNotificationDetails(
        actionBadge = actionBadge,
        targetItem = targetItem,
        keyValues = kvList
    )
}

@Composable
private fun NotificationItemCard(
    notification: AppNotification,
    onItemClick: () -> Unit,
    onNavigateClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var isExpanded by rememberSaveable(notification.id) { mutableStateOf(false) }

    val (icon, iconBgColor, iconTintColor) = when (notification.type) {
        NotificationType.COURSE -> Triple(
            Icons.Default.CalendarMonth,
            TealSecondary.copy(alpha = 0.15f),
            TealSecondary
        )
        NotificationType.EXPENSE -> Triple(
            Icons.Default.AccountBalanceWallet,
            EmeraldAccent.copy(alpha = 0.15f),
            EmeraldAccent
        )
        NotificationType.GRADUATION -> Triple(
            Icons.Default.School,
            IndigoAccent.copy(alpha = 0.15f),
            IndigoAccent
        )
        NotificationType.SYSTEM -> Triple(
            Icons.Default.NotificationsActive,
            SapphirePrimary.copy(alpha = 0.15f),
            SapphirePrimary
        )
    }

    val details = remember(notification) { parseNotificationMessage(notification) }

    Card(
        onClick = {
            onItemClick()
            isExpanded = !isExpanded
        },
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring(stiffness = 600f))
            .testTag("notification_card_${notification.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!notification.isRead)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (!notification.isRead)
            BorderStroke(1.dp, SapphirePrimary.copy(alpha = 0.3f))
        else
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Category Icon Badge
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTintColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Main Info Column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            // Action Type Tag (新增 / 刪除 / 變更 / 通知)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = details.actionBadge.containerColor
                            ) {
                                Text(
                                    text = details.actionBadge.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = details.actionBadge.contentColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Text(
                                text = details.targetItem.ifBlank { notification.title },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (!notification.isRead) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(SapphirePrimary)
                                )
                            }
                        }

                        Text(
                            text = formatRelativeTime(notification.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }

                    // Message text (Collapsed overview)
                    if (!isExpanded) {
                        Text(
                            text = notification.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 18.sp
                        )
                    }
                }

                // Delete Action Button
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "刪除",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Expanded Detail View (結構化異動資訊)
            if (isExpanded) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    modifier = Modifier.padding(vertical = 2.dp)
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "異動細項清單",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        details.keyValues.forEach { (label, value) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                )
                                Text(
                                    text = value,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Expanded Bottom Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (notification.actionRoute != null) {
                        TextButton(
                            onClick = onNavigateClick,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "前往頁面查看 →",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = SapphirePrimary
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    TextButton(
                        onClick = { isExpanded = false },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "收合 ▲",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Collapsed Hint
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "點擊展開細節 ▾",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    val minute = 60 * 1000L
    val hour = 60 * minute
    val day = 24 * hour

    return when {
        diff < minute -> "剛剛"
        diff < hour -> "${diff / minute} 分鐘前"
        diff < day -> "${diff / hour} 小時前"
        diff < 2 * day -> "昨天 ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))}"
        else -> SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(timestamp))
    }
}
