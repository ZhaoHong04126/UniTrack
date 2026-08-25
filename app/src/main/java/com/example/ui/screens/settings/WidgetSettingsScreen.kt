package com.example.ui.screens.settings

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import com.example.widget.WidgetUpdateHelper
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

    // Weekly Grid Bitmap
    val weeklyGridBitmap: Bitmap? = remember(currentSemesterCourses, dayOfWeekIndex, currentWeek, showWeekend) {
        if (currentSemesterCourses.isNotEmpty()) {
            WeeklyGridBitmapRenderer.renderWeeklyGrid(
                courses = currentSemesterCourses,
                currentDayOfWeek = dayOfWeekIndex,
                currentWeek = currentWeek,
                showWeekend = showWeekend,
                width = 1000,
                height = 1200
            )
        } else null
    }

    // Weekly Overview Mini Cards Bitmap
    val weeklyOverviewBitmap: Bitmap? = remember(currentSemesterCourses, dayOfWeekIndex, currentWeek, showWeekend) {
        if (currentSemesterCourses.isNotEmpty()) {
            WeeklyOverviewBitmapRenderer.renderOverviewCards(
                courses = currentSemesterCourses,
                currentDayOfWeek = dayOfWeekIndex,
                currentWeek = currentWeek,
                showWeekend = showWeekend,
                width = 1000,
                height = 600
            )
        } else null
    }

    Scaffold(
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
                .padding(16.dp),
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

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
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

                                    IconButton(
                                        onClick = {
                                            WidgetUpdateHelper.updateAllWidgets(context)
                                            Toast.makeText(context, "已推播刷新所有桌面小工具", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "重新整理",
                                            tint = SapphirePrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
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

                            if (weeklyGridBitmap != null) {
                                Image(
                                    bitmap = weeklyGridBitmap.asImageBitmap(),
                                    contentDescription = "一週網格課表預覽",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(260.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "本學期尚未排課，點擊下方前往新增",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
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
                            if (weeklyOverviewBitmap != null) {
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
            }

            // Quick Actions Card
            SectionHeader(title = "快速操作")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column {
                    WidgetActionRow(
                        icon = Icons.Default.Sync,
                        title = "立即推播刷新所有桌面小工具",
                        subtitle = "同步更新今日課表、一週網格與週概覽小工具",
                        iconTint = SapphirePrimary,
                        onClick = {
                            WidgetUpdateHelper.updateAllWidgets(context)
                            Toast.makeText(context, "已成功推播更新所有桌面小工具！", Toast.LENGTH_SHORT).show()
                        }
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        thickness = 0.8.dp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    WidgetActionRow(
                        icon = Icons.Default.CalendarMonth,
                        title = "前往編輯課表與上課時間",
                        subtitle = "新增課程、設定節次時間與開學週次",
                        iconTint = TealSecondary,
                        onClick = onNavigateToTimetable
                    )
                }
            }

            // Available Widgets List & Guide
            SectionHeader(title = "可用小工具類型")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    WidgetFeatureItem(
                        icon = Icons.Default.Today,
                        title = "1. 今日課表速覽 (2x2 / 4x2)",
                        description = "專注於當日課程、下節課高亮置頂與上課倒數提醒。"
                    )
                    WidgetFeatureItem(
                        icon = Icons.Default.GridOn,
                        title = "2. 一週全景網格課表 (4x4 / 全螢幕)",
                        description = "如彩色便利貼般在桌面完整展開週一至週五課表，當日自動高亮。"
                    )
                    WidgetFeatureItem(
                        icon = Icons.Default.ViewAgenda,
                        title = "3. 今日焦點與週概覽 (4x3 / 4x4)",
                        description = "上方呈現今日重要課堂焦點，下方展開一週每日課程迷你卡片清單。"
                    )
                }
            }

            // Step-by-Step Guide
            SectionHeader(title = "如何將小工具加入桌面？")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GuideStepItem(step = "1", text = "在 Android 手機主螢幕空白處「長按」")
                    GuideStepItem(step = "2", text = "在下方選單中點選「小工具 (Widgets)」或「微件」")
                    GuideStepItem(step = "3", text = "搜尋或滑動找到「UniTrack+」")
                    GuideStepItem(step = "4", text = "選擇「今日課表」、「一週網格」或「今日焦點與週概覽」拖曳至桌面")
                    GuideStepItem(step = "5", text = "長按小工具邊框可自由拉伸寬高以達到最佳排版視覺效果")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
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

@Composable
private fun WidgetFeatureItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = SapphirePrimary.copy(alpha = 0.12f),
            modifier = Modifier.size(32.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = SapphirePrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GuideStepItem(
    step: String,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = SapphirePrimary,
            modifier = Modifier.size(24.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = step,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
