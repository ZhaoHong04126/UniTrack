package com.example.ui.screens.settings

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AuthProvider
import com.example.data.model.GraduationPlan
import com.example.data.model.UserProfile
import com.example.ui.components.SectionHeader
import com.example.ui.screens.graduation.GraduationPlanDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudentViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: StudentViewModel,
    modifier: Modifier = Modifier,
    onNavigateToAuth: () -> Unit = {}
) {
    val context = LocalContext.current

    val plan by viewModel.graduationPlan.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val showWeekend by viewModel.showWeekend.collectAsStateWithLifecycle()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showDaysOptionDialog by remember { mutableStateOf(false) }
    var showPlanDialog by remember { mutableStateOf(false) }
    var showSignOutConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteAccountConfirmDialog by remember { mutableStateOf(false) }
    var showFinalExecutionDialog by remember { mutableStateOf(false) }

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
            onEditClick = { showEditProfileDialog = true }
        )

        // Section: Account (帳號設定)
        SectionHeader(title = "帳號")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            SettingTileRow(
                icon = Icons.Default.Person,
                title = "編輯個人資料",
                subtitle = "管理暱稱與個人資訊",
                iconTint = SapphirePrimary,
                onClick = { showEditProfileDialog = true }
            )
        }

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

    // Edit Profile Dialog (帳號個人資料編輯頁面)
    if (showEditProfileDialog) {
        EditProfileDialog(
            currentName = plan.studentName,
            currentDepartment = plan.department,
            currentAdmissionSemester = plan.admissionSemester,
            currentSemester = plan.currentSemester,
            currentUser = currentUser,
            onDismiss = { showEditProfileDialog = false },
            onSave = { newName, newDept, newAdmissionSem, newSemester ->
                viewModel.updateGraduationPlan(
                    plan.copy(
                        studentName = newName.trim(),
                        department = newDept.trim(),
                        admissionSemester = newAdmissionSem.trim(),
                        currentSemester = newSemester.trim()
                    )
                )
                Toast.makeText(context, "個人資料已儲存更新", Toast.LENGTH_SHORT).show()
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

    // Delete Account Confirm Dialog (嚴格雙重確認安全機制)
    if (showDeleteAccountConfirmDialog) {
        var isDeleting by remember { mutableStateOf(false) }
        var confirmText by remember { mutableStateOf("") }
        var agreeDataLoss by remember { mutableStateOf(false) }
        var agreeIrreversible by remember { mutableStateOf(false) }

        val requiredText = "刪除帳號"
        val isConfirmed = agreeDataLoss && agreeIrreversible && confirmText.trim() == requiredText

        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteAccountConfirmDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(RoseLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = RoseAccent,
                        modifier = Modifier.size(30.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "危險操作：永久刪除帳號",
                    color = RoseAccent,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = RoseLight.copy(alpha = 0.55f),
                        border = BorderStroke(1.5.dp, RoseAccent.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "⚠️ 執行後將立即永久刪除：",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = RoseAccent
                            )
                            Text(
                                text = "• 所有雲端與本機課表、成績學分紀錄\n• 所有收支記帳與預算明細\n• 帳號認證身分與學生檔案（無法復原）",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 22.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Checkbox 1
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(enabled = !isDeleting) { agreeDataLoss = !agreeDataLoss }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = agreeDataLoss,
                            onCheckedChange = { agreeDataLoss = it },
                            enabled = !isDeleting,
                            colors = CheckboxDefaults.colors(checkedColor = RoseAccent)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "我了解所有雲端與本機資料將被立即抹除",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Checkbox 2
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(enabled = !isDeleting) { agreeIrreversible = !agreeIrreversible }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = agreeIrreversible,
                            onCheckedChange = { agreeIrreversible = it },
                            enabled = !isDeleting,
                            colors = CheckboxDefaults.colors(checkedColor = RoseAccent)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "我確認放棄此帳號，且此動作無法復原",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Type Confirmation Text Field
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "請在下方輸入「$requiredText」以解鎖按鈕：",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        OutlinedTextField(
                            value = confirmText,
                            onValueChange = { confirmText = it },
                            placeholder = { Text("輸入「$requiredText」") },
                            singleLine = true,
                            enabled = !isDeleting,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (confirmText.trim() == requiredText) RoseAccent else MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = if (confirmText.trim() == requiredText) RoseAccent else MaterialTheme.colorScheme.outline
                            )
                        )
                    }

                    if (isDeleting) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = RoseAccent,
                                strokeWidth = 2.5.dp
                            )
                            Text(
                                text = "正在註銷帳號並清除所有雲端與本機資料...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isConfirmed) {
                            showFinalExecutionDialog = true
                        }
                    },
                    enabled = isConfirmed && !isDeleting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RoseAccent,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "確認永久刪除帳號",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isConfirmed && !isDeleting) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteAccountConfirmDialog = false },
                    enabled = !isDeleting
                ) {
                    Text("取消", style = MaterialTheme.typography.bodyMedium)
                }
            }
        )
    }

    // Second-Stage Final Execution Dialog (最終二次執行確認)
    if (showFinalExecutionDialog) {
        var isExecutingDelete by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isExecutingDelete) showFinalExecutionDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(RoseLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = RoseAccent,
                        modifier = Modifier.size(30.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "是否確認執行永久刪除？",
                    color = RoseAccent,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "這是最後一次確認！一旦點擊「確定執行刪除」，您的帳號及所有雲端、本機課表與學業記錄將被永久銷毀，無法進行任何還原。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp
                    )
                    if (isExecutingDelete) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = RoseAccent,
                                strokeWidth = 2.5.dp
                            )
                            Text(
                                text = "正在執行註銷並抹除所有資料...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isExecutingDelete = true
                        viewModel.deleteAccount { success, errorMsg ->
                            isExecutingDelete = false
                            if (success) {
                                showFinalExecutionDialog = false
                                showDeleteAccountConfirmDialog = false
                                onNavigateToAuth()
                            } else {
                                Toast.makeText(context, errorMsg ?: "刪除帳號失敗", Toast.LENGTH_LONG).show()
                                if (errorMsg?.contains("重新登入") == true) {
                                    showFinalExecutionDialog = false
                                    showDeleteAccountConfirmDialog = false
                                    onNavigateToAuth()
                                }
                            }
                        }
                    },
                    enabled = !isExecutingDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = RoseAccent),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "確定執行刪除",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showFinalExecutionDialog = false },
                    enabled = !isExecutingDelete
                ) {
                    Text("再想想（取消）", style = MaterialTheme.typography.bodyMedium)
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
    onEditClick: () -> Unit = {}
) {
    val isLoggedIn = currentUser != null

    Card(
        onClick = onEditClick,
        modifier = Modifier.fillMaxWidth(),
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
                                else -> "離線模式"
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
                    // Avatar Container (純展示頭像)
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
 * 編輯個人資料頁面（帳號個人資訊）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileDialog(
    currentName: String,
    currentDepartment: String,
    currentAdmissionSemester: String,
    currentSemester: String,
    currentUser: UserProfile?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(currentName) }
    var department by remember { mutableStateOf(currentDepartment) }
    var admissionSem by remember { mutableStateOf(currentAdmissionSemester) }
    var semester by remember { mutableStateOf(currentSemester) }

    var showDeptDialog by remember { mutableStateOf(false) }

    if (showDeptDialog) {
        DepartmentSelectDialog(
            selectedDepartment = department,
            onSelect = { department = it },
            onDismiss = { showDeptDialog = false }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "編輯個人資料",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.background,
                    shadowElevation = 8.dp
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        Button(
                            onClick = {
                                if (name.isNotBlank()) {
                                    onSave(name, department, admissionSem, semester)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SapphirePrimary
                            ),
                            enabled = name.isNotBlank()
                        ) {
                            Text(
                                text = "儲存變更",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Avatar with Camera Badge
                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(104.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (name.isNotBlank()) {
                                Text(
                                    text = name.trim().take(1),
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(56.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    Surface(
                        onClick = {
                            Toast.makeText(context, "已使用個人預設頭像", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(34.dp),
                        shape = CircleShape,
                        color = SapphirePrimary,
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.background)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "更換頭像",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Nickname / Name Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "暱稱",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        placeholder = { Text("請輸入您的姓名或暱稱") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }

                // Affiliation Info Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "所屬資訊",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            ProfileInfoRow(
                                label = "電子郵件",
                                value = currentUser?.email ?: "離線本機帳號"
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            ProfileInfoRow(
                                label = "學校",
                                value = "國立臺東大學"
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            ProfileInteractiveRow(
                                label = "主修科系",
                                value = if (department.isBlank() || department == "尚未設定系所") "點擊選擇科系" else department,
                                isPlaceholder = department.isBlank() || department == "尚未設定系所",
                                onClick = { showDeptDialog = true }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            ProfileInfoRow(
                                label = "入學年度",
                                value = if (admissionSem.isNotBlank()) "$admissionSem 學期" else "114 學年度"
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            ProfileInfoRow(
                                label = "當前學期",
                                value = if (semester.isNotBlank()) "$semester 學期" else "114-1"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Suppress("SameParameterValue")
@Composable
private fun ProfileInteractiveRow(
    label: String,
    value: String,
    isPlaceholder: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isPlaceholder) SapphirePrimary else MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun DepartmentSelectDialog(
    selectedDepartment: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp, vertical = 24.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "選擇主修系所",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "國立臺東大學",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "關閉")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    com.example.data.local.DefaultData.NTTU_COLLEGES.forEach { college ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(college.icon, fontSize = 20.sp)
                                    Text(
                                        text = college.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = SapphirePrimary
                                    )
                                }

                                college.departments.forEach { dept ->
                                    val isSelected = selectedDepartment == dept
                                    Surface(
                                        onClick = {
                                            onSelect(dept)
                                            onDismiss()
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) SapphirePrimary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) SapphirePrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = dept,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) SapphirePrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = SapphirePrimary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
