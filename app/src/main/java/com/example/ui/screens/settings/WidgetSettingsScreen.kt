package com.example.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ExpenseType
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudentViewModel
import com.example.widget.ExpenseWidget
import com.example.widget.TodayScheduleWidget
import com.example.widget.WidgetUpdateHelper
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetSettingsScreen(
    viewModel: StudentViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToTimetable: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToExpense: () -> Unit = {}
) {
    val context = LocalContext.current
    val todayClasses by viewModel.todayClasses.collectAsStateWithLifecycle()
    val plan by viewModel.graduationPlan.collectAsStateWithLifecycle()
    val semesterTimeConfigVersion by viewModel.semesterTimeConfigVersion.collectAsStateWithLifecycle()
    val summary by viewModel.monthlyExpenseSummary.collectAsStateWithLifecycle()
    val allExpenses by viewModel.allExpenses.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedExpenseMonth.collectAsStateWithLifecycle()

    val currentSemester = plan.currentSemester.ifBlank { com.example.data.local.DefaultData.getCurrentAcademicSemester() }
    val currentStartDateStr = remember(currentSemester, semesterTimeConfigVersion) {
        viewModel.getSemesterStartDate(currentSemester)
    }
    val currentTotalWeeks = remember(currentSemester, semesterTimeConfigVersion) {
        viewModel.getSemesterTotalWeeks(currentSemester)
    }

    val calendar = Calendar.getInstance()
    val dayOfWeekIndex = TodayScheduleWidget.getDayOfWeekIndex(calendar)
    val dayOfWeekName = TodayScheduleWidget.getDayOfWeekName(dayOfWeekIndex)
    val currentWeek = TodayScheduleWidget.calculateCurrentWeek(currentStartDateStr, currentTotalWeeks)

    val month = calendar.get(Calendar.MONTH) + 1
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val dateText = "${month}月${day}日 $dayOfWeekName"

    val nowMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    val nextClassStatus = remember(todayClasses, nowMinutes) {
        TodayScheduleWidget.findNextOrOngoingClass(todayClasses, nowMinutes)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "桌面小工具管理",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回設定"
                        )
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
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Widget Live Preview
            SectionHeader(title = "小工具樣式預覽")

            // Today Schedule Widget Preview
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                            // Header Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = SapphirePrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = dateText,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = SapphirePrimary
                                ) {
                                    Text(
                                        text = "第 $currentWeek 週",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Next / Ongoing Class Banner
                            if (nextClassStatus != null) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF0F7FF),
                                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = SapphirePrimary
                                            ) {
                                                Text(
                                                    text = if (nextClassStatus.isOngoing) "⚡ 進行中" else "⏱️ 下一堂課",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Text(
                                                text = nextClassStatus.timeDisplay,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = SapphireDark
                                            )
                                        }
                                        Text(
                                            text = nextClassStatus.course.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        val location = nextClassStatus.course.location.ifBlank { "教室未定" }
                                        val teacher = if (nextClassStatus.course.teacher.isNotBlank()) " • ${nextClassStatus.course.teacher}" else ""
                                        Text(
                                            text = "📍 $location$teacher",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else if (todayClasses.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "🎉 今日無課程",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = SapphirePrimary
                                        )
                                        Text(
                                            text = "好好休息或點擊查看完整週課表",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Today's classes list preview
                            if (todayClasses.isNotEmpty()) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    todayClasses.take(3).forEach { c ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = SapphirePrimary.copy(alpha = 0.9f),
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Box(
                                                        contentAlignment = Alignment.Center,
                                                        modifier = Modifier.fillMaxSize()
                                                    ) {
                                                        Text(
                                                            text = c.startPeriod.toString(),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White
                                                        )
                                                    }
                                                }

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = c.name,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    val timeStr = if (c.startTime.isNotBlank() && c.endTime.isNotBlank()) {
                                                        "${c.startTime}-${c.endTime}"
                                                    } else {
                                                        "第${c.startPeriod}-${c.endPeriod}節"
                                                    }
                                                    val locStr = if (c.location.isNotBlank()) " • ${c.location}" else ""
                                                    Text(
                                                        text = "$timeStr$locStr",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (todayClasses.size > 3) {
                                        Text(
                                            text = "+ 還有 ${todayClasses.size - 3} 堂課",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SapphirePrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(start = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

            // Expense Widget Preview
            val todayDateStr = remember {
                java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            }
            val todayExpense = remember(allExpenses, todayDateStr) {
                allExpenses.filter { it.dateString == todayDateStr && (it.type == ExpenseType.EXPENSE || it.type == ExpenseType.TRANSFER_OUT) }
                    .sumOf { it.amount }
            }
            val currentMonthExpenses = remember(allExpenses, selectedMonth) {
                allExpenses.filter { it.dateString.startsWith(selectedMonth) }
            }

            SectionHeader(title = "記帳本小工具樣式")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = SapphirePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "UniTrack+ 記帳本",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SapphirePrimary
                            ) {
                                Text(
                                    text = "${selectedMonth.takeLast(2).toIntOrNull() ?: (calendar.get(Calendar.MONTH) + 1)}月",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Action Buttons
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SapphirePrimary,
                                modifier = Modifier.height(26.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "記一筆",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // Subtitle: Today's Expense
                    Text(
                        text = "今日已支出 $${todayExpense.toInt()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Budget Overview Box
                    val isOverBudget = summary.remainingBudget < 0
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isOverBudget) Color(0xFFFEF2F2) else Color(0xFFF0F7FF),
                        border = BorderStroke(1.dp, if (isOverBudget) Color(0xFFFECACA) else Color(0xFFBFDBFE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "本月支出總計",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "$ ${summary.totalExpense.toInt()}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isOverBudget) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (isOverBudget) "已超支" else "剩餘預算",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (isOverBudget) "-$ ${(-summary.remainingBudget).toInt()} ⚠️" else "$ ${summary.remainingBudget.toInt()}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isOverBudget) Color(0xFFDC2626) else SapphirePrimary
                                    )
                                }
                            }

                            LinearProgressIndicator(
                                progress = {
                                    if (summary.budgetAmount > 0) {
                                        (summary.totalExpense / summary.budgetAmount).coerceIn(0.0, 1.0).toFloat()
                                    } else 0f
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = if (isOverBudget) Color(0xFFDC2626) else SapphirePrimary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isOverBudget) "已使用 ${(summary.budgetUsagePercentage * 100).toInt()}% (超支)" else "已使用 ${(summary.budgetUsagePercentage * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isOverBudget) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "預算上限 $${summary.budgetAmount.toInt()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Recent Transactions List (up to 3)
                    if (currentMonthExpenses.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "📝 本月尚無記帳記錄",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SapphirePrimary
                                )
                                Text(
                                    text = "點擊右上角「記一筆」快速記錄收支",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            currentMonthExpenses.take(3).forEach { exp ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = ExpenseWidget.getCategoryEmoji(exp.category, exp.type),
                                            style = MaterialTheme.typography.bodyLarge
                                        )

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = exp.title.ifBlank { exp.category.label },
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            val relativeDate = ExpenseWidget.formatRelativeDate(exp.dateString, todayDateStr)
                                            Text(
                                                text = "${exp.category.label} • ${exp.paymentMethod.label} • $relativeDate",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        val isExpense = exp.type == ExpenseType.EXPENSE || exp.type == ExpenseType.TRANSFER_OUT
                                        Text(
                                            text = "${if (isExpense) "-$" else "+$"}${exp.amount.toInt()}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isExpense) Color(0xFFDC2626) else Color(0xFF059669)
                                        )
                                    }
                                }
                            }

                            if (currentMonthExpenses.size > 3) {
                                Text(
                                    text = "+ 還有 ${currentMonthExpenses.size - 3} 筆收支記錄",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SapphirePrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }
                }
            }


            // Quick Actions Card
            SectionHeader(title = "快速操作與同步")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column {
                    WidgetActionRow(
                        icon = Icons.Default.CalendarMonth,
                        title = "前往編輯課表與上課時間",
                        subtitle = "新增課程、設定節次時間與開學週次",
                        iconTint = TealSecondary,
                        onClick = onNavigateToTimetable
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    WidgetActionRow(
                        icon = Icons.Default.AccountBalanceWallet,
                        title = "前往記帳本與設定預算",
                        subtitle = "記帳收支明細、每月預算與類別上限設定",
                        iconTint = SapphirePrimary,
                        onClick = onNavigateToExpense
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    WidgetActionRow(
                        icon = Icons.Default.Refresh,
                        title = "立即強制同步所有桌面小工具",
                        subtitle = "向系統廣播重新整理今日課表與記帳小工具",
                        iconTint = AmberWarning,
                        onClick = {
                            WidgetUpdateHelper.updateAllWidgets(context)
                            Toast.makeText(context, "已通知桌面所有小工具更新！", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = iconTint.copy(alpha = 0.12f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}
