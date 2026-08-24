package com.example.ui.screens.settings

import android.content.ClipData
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AuthProvider
import com.example.data.model.GraduationPlan
import com.example.data.model.UserProfile
import com.example.ui.components.SectionHeader
import com.example.ui.screens.graduation.GraduationPlanDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudentViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: StudentViewModel,
    modifier: Modifier = Modifier,
    onNavigateToAuth: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()

    val plan by viewModel.graduationPlan.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val showWeekend by viewModel.showWeekend.collectAsStateWithLifecycle()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showDaysOptionDialog by remember { mutableStateOf(false) }
    var showPlanDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportedJsonText by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonInput by remember { mutableStateOf("") }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showSignOutConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteAccountConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "個人設定與資訊",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "UniTrack+ 離線智慧學業助理",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Student ID Card Widget (學生證風格卡片)
        StudentIdCard(
            plan = plan,
            currentUser = currentUser,
            onEditClick = { showEditProfileDialog = true },
            onAuthClick = onNavigateToAuth
        )

        // Section 1: Academic & Profile Settings
        SectionHeader(title = "學業與審查設定")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            SettingTileRow(
                icon = Icons.Default.Tune,
                title = "畢業審查標準設定",
                subtitle = "總目標 ${plan.targetTotalCredits.toInt()} 學分・各模組門檻",
                iconTint = IndigoAccent,
                onClick = { showPlanDialog = true }
            )
        }

        // Section 2: Timetable Display Settings
        SectionHeader(title = "課表與顯示偏好")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            SettingTileRow(
                icon = Icons.Default.CalendarViewWeek,
                title = "每週顯示天數",
                subtitle = if (showWeekend) "一週 (七天・含週末)" else "平日 (五天・週一至週五)",
                iconTint = TealSecondary,
                onClick = { showDaysOptionDialog = true }
            )
        }

        // Section 3: Data Backup & Security
        SectionHeader(title = "資料備份與安全")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column {
                SettingTileRow(
                    icon = Icons.Default.CloudDownload,
                    title = "匯出本機備份 JSON",
                    subtitle = "複製完整資料至剪貼簿以供備份",
                    iconTint = SapphirePrimary,
                    onClick = {
                        coroutineScope.launch {
                            exportedJsonText = viewModel.exportJson()
                            showExportDialog = true
                        }
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )

                SettingTileRow(
                    icon = Icons.Default.CloudUpload,
                    title = "匯入還原 JSON",
                    subtitle = "從先前備份的 JSON 內容還原資料",
                    iconTint = TealSecondary,
                    onClick = {
                        importJsonInput = ""
                        showImportDialog = true
                    }
                )
            }
        }

        // Section 3: Data Management
        SectionHeader(title = "資料管理")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column {
                SettingTileRow(
                    icon = Icons.Default.RestartAlt,
                    title = "重置為大學生範例資料",
                    subtitle = "重新載入預設課程、記帳與審查範例",
                    iconTint = SapphirePrimary,
                    titleColor = SapphirePrimary,
                    onClick = { showResetConfirmDialog = true }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )

                SettingTileRow(
                    icon = Icons.Default.DeleteOutline,
                    title = "清空所有本機資料",
                    subtitle = "清除所有課表、記帳明細與學分記錄",
                    iconTint = RoseAccent,
                    titleColor = RoseAccent,
                    onClick = { showClearConfirmDialog = true }
                )
            }
        }

        // Footer: Sign Out / Delete Account & Copyright
        val user = currentUser
        if (user != null && !user.isAnonymous) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = { showSignOutConfirmDialog = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "登出",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "|",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    TextButton(
                        onClick = { showDeleteAccountConfirmDialog = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "刪除帳號",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "© 2026 UniTrack+. All rights reserved.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "© 2026 UniTrack+. All rights reserved.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(72.dp))
    }

    // Edit Profile & Semester Dialog
    if (showEditProfileDialog) {
        EditProfileDialog(
            currentName = plan.studentName,
            currentAdmissionSemester = plan.admissionSemester,
            currentSemester = plan.currentSemester,
            onDismiss = { showEditProfileDialog = false },
            onSave = { newName, newAdmissionSem, newSemester ->
                viewModel.updateGraduationPlan(
                    plan.copy(
                        studentName = newName.trim(),
                        admissionSemester = newAdmissionSem.trim(),
                        currentSemester = newSemester.trim()
                    )
                )
                Toast.makeText(context, "已更新個人與學期資料", Toast.LENGTH_SHORT).show()
                showEditProfileDialog = false
            }
        )
    }

    // Timetable Days Option Dialog (5 days vs 7 days)
    if (showDaysOptionDialog) {
        AlertDialog(
            onDismissRequest = { showDaysOptionDialog = false },
            title = {
                Text(
                    text = "設定課表每週顯示天數",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Option 1: 平日 (五天)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (!showWeekend) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            )
                            .clickable {
                                viewModel.setShowWeekend(false)
                                showDaysOptionDialog = false
                                Toast.makeText(context, "已切換為：平日 (週一至週五)", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "平日 (五天)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "顯示週一至週五（版面更寬敞）",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!showWeekend) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Option 2: 一週 (七天)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (showWeekend) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            )
                            .clickable {
                                viewModel.setShowWeekend(true)
                                showDaysOptionDialog = false
                                Toast.makeText(context, "已切換為：一週 (週一至週日)", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "一週 (七天)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "顯示週一至週日（含週末排課）",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (showWeekend) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDaysOptionDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // Graduation Plan Thresholds Dialog
    if (showPlanDialog) {
        GraduationPlanDialog(
            currentPlan = plan,
            onDismiss = { showPlanDialog = false },
            onSave = { updated ->
                viewModel.updateGraduationPlan(updated)
                showPlanDialog = false
            }
        )
    }

    // JSON Export Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("本機備份 JSON", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "以下為您的完整離線資料備份，可複製儲存於本機備忘錄或傳至電腦：",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = exportedJsonText,
                        onValueChange = {},
                        readOnly = true,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            clipboardManager.setClipEntry(ClipEntry(ClipData.newPlainText("UniTrack+ Backup", exportedJsonText)))
                            Toast.makeText(context, "已複製 JSON 備份內容到剪貼簿", Toast.LENGTH_SHORT).show()
                            showExportDialog = false
                        }
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("複製到剪貼簿")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("關閉")
                }
            }
        )
    }

    // JSON Import Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("匯入 JSON 備份", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "請貼上先前匯出的 JSON 備份內容：",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = importJsonInput,
                        onValueChange = { importJsonInput = it },
                        placeholder = { Text("{\"version\": 1, ...}") },
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val ok = viewModel.importJson(importJsonInput)
                            if (ok) showImportDialog = false
                        }
                    },
                    enabled = importJsonInput.isNotBlank()
                ) {
                    Text("確認匯入")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // Reset Confirm Dialog
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("重置為範例資料？") },
            text = { Text("這將會把課程表、畢業審查及記帳資料重置為大學生預設範例資料。") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetToSampleData()
                        showResetConfirmDialog = false
                    }
                ) {
                    Text("確認重置")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // Clear Confirm Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("清空所有資料？", color = RoseAccent) },
            text = { Text("這將會清空所有課程表、記帳明細與自訂門檻，此操作無法復原。") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseAccent)
                ) {
                    Text("確定清空")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
    // Sign Out Confirm Dialog
    if (showSignOutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirmDialog = false },
            title = { Text("確認登出帳號？") },
            text = { Text("登出後將返回登入畫面，您可以隨時重新登入同步資料。") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.signOut()
                        showSignOutConfirmDialog = false
                        onNavigateToAuth()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseAccent)
                ) {
                    Text("確認登出")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // Delete Account Confirm Dialog
    if (showDeleteAccountConfirmDialog) {
        var isDeleting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteAccountConfirmDialog = false },
            title = { Text("確認刪除帳號？", color = RoseAccent, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("這將會永久刪除您的帳號，並同步清空雲端與本機的所有課表、記帳與學業檔案。此操作無法復原。")
                    if (isDeleting) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = RoseAccent,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "正在清除雲端與本機資料並刪除帳號...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isDeleting = true
                        viewModel.deleteAccount { success, errorMsg ->
                            isDeleting = false
                            if (success) {
                                showDeleteAccountConfirmDialog = false
                                onNavigateToAuth()
                            } else {
                                Toast.makeText(context, errorMsg ?: "刪除帳號失敗", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = !isDeleting,
                    colors = ButtonDefaults.buttonColors(containerColor = RoseAccent)
                ) {
                    Text("確定刪除")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteAccountConfirmDialog = false },
                    enabled = !isDeleting
                ) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 數位學生證風格卡片 (Digital Student ID Card)
 */
@Composable
private fun StudentIdCard(
    plan: GraduationPlan,
    currentUser: UserProfile?,
    onEditClick: () -> Unit,
    onAuthClick: () -> Unit
) {
    val isLoggedIn = currentUser != null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = if (isLoggedIn) onEditClick else onAuthClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF1E3A8A), // Royal Navy Blue
                            Color(0xFF2563EB), // Sapphire Primary
                            Color(0xFF0284C7)  // Ocean Blue
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Top Badge Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "在校生",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            text = "UniTrack+ 數位學生證",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isLoggedIn) EmeraldAccent.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, if (isLoggedIn) EmeraldAccent.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = when {
                                isLoggedIn && currentUser.provider == AuthProvider.GOOGLE -> "Google 認證"
                                isLoggedIn -> "Email 帳號"
                                else -> "未登入"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isLoggedIn) Color(0xFF6EE7B7) else Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // Middle Profile Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Avatar Container with Edit / Login Badge
                    Box(
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isLoggedIn) Icons.Default.AccountCircle else Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isLoggedIn) Icons.Default.Edit else Icons.AutoMirrored.Filled.Login,
                                contentDescription = "操作",
                                tint = SapphirePrimary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    // Student Information
                    val admissionYearDisplay = remember(plan.admissionSemester) {
                        val code = plan.admissionSemester.substringBefore("-").trim()
                        if (plan.admissionSemester.contains("學年度")) plan.admissionSemester else "$code 學年度 (${plan.admissionSemester})"
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isLoggedIn && !currentUser.email.isNullOrBlank()) currentUser.email else "學生姓名",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 1
                        )
                        Text(
                            text = plan.studentName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "入學年度：$admissionYearDisplay",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                // Bottom Info Pills inside Card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .padding(vertical = 8.dp, horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = plan.department.ifBlank { "尚未設定系所" },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1
                            )
                            Text(
                                text = "主修系所",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .padding(vertical = 8.dp, horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = plan.currentSemester,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1
                            )
                            Text(
                                text = "當前學期",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 分組式設定項目列 (Grouped Setting Tile)
 */
@Composable
private fun SettingTileRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color = SapphirePrimary,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    badgeText: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
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
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (badgeText != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EmeraldLight
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldAccent,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * 編輯姓名與學期對話框
 */
@Composable
private fun EditProfileDialog(
    currentName: String,
    currentAdmissionSemester: String,
    currentSemester: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var admissionSem by remember { mutableStateOf(currentAdmissionSemester) }
    var semester by remember { mutableStateOf(currentSemester) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "編輯個人與學期資料",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("學生姓名/稱呼") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = admissionSem,
                    onValueChange = { admissionSem = it },
                    label = { Text("入學學期（大一基準）") },
                    placeholder = { Text("例如：114-1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = semester,
                    onValueChange = { semester = it },
                    label = { Text("主要學期（儀表板當前顯示）") },
                    placeholder = { Text("例如：114-1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && admissionSem.isNotBlank() && semester.isNotBlank()) {
                        onSave(name, admissionSem, semester)
                    }
                },
                enabled = name.isNotBlank() && admissionSem.isNotBlank() && semester.isNotBlank()
            ) {
                Text("儲存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
