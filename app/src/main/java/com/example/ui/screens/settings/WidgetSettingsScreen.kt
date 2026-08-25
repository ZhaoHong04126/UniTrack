package com.example.ui.screens.settings

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudentViewModel
import com.example.widget.TodayScheduleWidget
import com.example.widget.WeeklyGridBitmapRenderer
import com.example.widget.WeeklyOverviewBitmapRenderer
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetSettingsScreen(
    viewModel: StudentViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToTimetable: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val todayClasses by viewModel.todayClasses.collectAsStateWithLifecycle()
    val currentSemesterCourses by viewModel.currentSemesterCourses.collectAsStateWithLifecycle()
    val plan by viewModel.graduationPlan.collectAsStateWithLifecycle()
    val showWeekend by viewModel.showWeekend.collectAsStateWithLifecycle()
    val semesterTimeConfigVersion by viewModel.semesterTimeConfigVersion.collectAsStateWithLifecycle()

    var selectedPreviewTab by remember { mutableIntStateOf(0) }

    val currentSemester = plan.currentSemester.ifBlank { "114-1" }
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

    // Weekly Grid Bitmap (Always rendered)
    val weeklyGridBitmap: Bitmap = remember(currentSemesterCourses, dayOfWeekIndex, currentWeek, showWeekend) {
        WeeklyGridBitmapRenderer.renderWeeklyGrid(
            courses = currentSemesterCourses,
            currentDayOfWeek = dayOfWeekIndex,
            currentWeek = currentWeek,
            showWeekend = showWeekend,
            width = 1000,
            height = 1400
        )
    }

    // Weekly Overview Mini Cards Bitmap (Always rendered)
    val weeklyOverviewBitmap: Bitmap = remember(currentSemesterCourses, dayOfWeekIndex, currentWeek, showWeekend) {
        WeeklyOverviewBitmapRenderer.renderOverviewCards(
            courses = currentSemesterCourses,
            currentDayOfWeek = dayOfWeekIndex,
            currentWeek = currentWeek,
            showWeekend = showWeekend,
            width = 1000,
            height = 600
        )
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
            // Widget Type Switcher Tabs
            SectionHeader(title = "小工具樣式預覽")

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                val tabTitles = listOf("今日課表", "全週網格", "今日+週概覽")
                tabTitles.forEachIndexed { index, title ->
                    SegmentedButton(
                        selected = selectedPreviewTab == index,
                        onClick = { selectedPreviewTab = index },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = tabTitles.size)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selectedPreviewTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // Live Preview Container
            when (selectedPreviewTab) {
                0 -> {
                    // Preview 1: Today Schedule Widget
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
                }
                1 -> {
                    // Preview 2: Full Week Grid Timetable Widget (TimeSpread Style)
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
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "📅 $currentSemester 週課表全景",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
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

                            Image(
                                bitmap = weeklyGridBitmap.asImageBitmap(),
                                contentDescription = "一週網格課表預覽",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                            )
                        }
                    }
                }
                2 -> {
                    // Preview 3: Today Focus & Weekly Overview Widget
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
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = dateText,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
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

                            // Focus item
                            if (nextClassStatus != null) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF0F7FF),
                                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = SapphirePrimary
                                        ) {
                                            Text(
                                                text = if (nextClassStatus.isOngoing) "⚡ 進行中" else "⏱️ 下一堂",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = nextClassStatus.course.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${nextClassStatus.timeDisplay} • ${nextClassStatus.course.location.ifBlank { "教室未定" }}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // 5-day mini overview cards
                            Image(
                                bitmap = weeklyOverviewBitmap.asImageBitmap(),
                                contentDescription = "本週摘要預覽",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                            )
                        }
                    }
                }
            }

            // Quick Actions Card
            SectionHeader(title = "快速操作")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                WidgetActionRow(
                    icon = Icons.Default.CalendarMonth,
                    title = "前往編輯課表與上課時間",
                    subtitle = "新增課程、設定節次時間與開學週次",
                    iconTint = TealSecondary,
                    onClick = onNavigateToTimetable
                )
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
