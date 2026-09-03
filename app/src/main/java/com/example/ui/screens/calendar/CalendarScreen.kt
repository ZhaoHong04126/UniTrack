package com.example.ui.screens.calendar

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CalendarEvent
import com.example.data.model.CalendarEventCategory
import com.example.data.model.Course
import com.example.ui.viewmodel.StudentViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields

enum class CalendarViewType {
    WEEK, MONTH
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: StudentViewModel,
    modifier: Modifier = Modifier,
    onNavigateToTimetable: () -> Unit = {}
) {
    val allCalendarEvents by viewModel.allCalendarEvents.collectAsStateWithLifecycle()
    val allCourses by viewModel.allCourses.collectAsStateWithLifecycle()
    val plan by viewModel.graduationPlan.collectAsStateWithLifecycle()
    val semesterTimeConfigVersion by viewModel.semesterTimeConfigVersion.collectAsStateWithLifecycle()

    val currentSemester = plan.currentSemester.ifBlank { "114-1" }

    val today = remember { LocalDate.now() }
    var viewType by remember { mutableStateOf(CalendarViewType.MONTH) }
    var selectedDate by remember { mutableStateOf(today) }
    var currentYearMonth by remember { mutableStateOf(YearMonth.from(today)) }

    var showAddEventDialog by remember { mutableStateOf(false) }
    var eventCategoryToCreate by remember { mutableStateOf<CalendarEventCategory?>(null) }
    var editingEvent by remember { mutableStateOf<CalendarEvent?>(null) }

    val selectedDateStr = remember(selectedDate) {
        selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    }

    // Courses for selected date
    val dayCourses = remember(selectedDateStr, currentSemester, allCourses, semesterTimeConfigVersion) {
        viewModel.getCoursesForDate(selectedDateStr, currentSemester)
    }

    // Events for selected date
    val dayEvents = remember(selectedDateStr, allCalendarEvents) {
        allCalendarEvents.filter { it.date == selectedDateStr }
    }

    // Week info for selected date
    val selectedWeekNum = remember(selectedDateStr, currentSemester, semesterTimeConfigVersion) {
        viewModel.getWeekNumberForDate(selectedDateStr, currentSemester)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingEvent = null
                    eventCategoryToCreate = null
                    showAddEventDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.testTag("fab_add_calendar_event")
            ) {
                Icon(Icons.Default.Add, contentDescription = "新增行程")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Calendar Header: Title, [週 | 月] Switcher, < >, Today
            CalendarUnifiedHeader(
                viewType = viewType,
                currentYearMonth = currentYearMonth,
                selectedDate = selectedDate,
                selectedWeekNum = selectedWeekNum,
                onViewTypeChange = { viewType = it },
                onPrevious = {
                    if (viewType == CalendarViewType.MONTH) {
                        currentYearMonth = currentYearMonth.minusMonths(1)
                    } else {
                        selectedDate = selectedDate.minusWeeks(1)
                        currentYearMonth = YearMonth.from(selectedDate)
                    }
                },
                onNext = {
                    if (viewType == CalendarViewType.MONTH) {
                        currentYearMonth = currentYearMonth.plusMonths(1)
                    } else {
                        selectedDate = selectedDate.plusWeeks(1)
                        currentYearMonth = YearMonth.from(selectedDate)
                    }
                }
            )

            // Weekday Title Header (日 一 二 三 四 五 六)
            WeekdayHeaderBar()

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                thickness = 0.5.dp
            )

            // Content Body: Animated transition between Month View and Week View
            AnimatedContent(
                targetState = viewType,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(150))
                },
                label = "CalendarViewSwitch"
            ) { targetView ->
                if (targetView == CalendarViewType.MONTH) {
                    // ---------------- MONTH VIEW ----------------
                    MonthCalendarView(
                        yearMonth = currentYearMonth,
                        selectedDate = selectedDate,
                        today = today,
                        events = allCalendarEvents,
                        courses = allCourses,
                        currentSemester = currentSemester,
                        viewModel = viewModel,
                        onDateSelected = { date ->
                            selectedDate = date
                            currentYearMonth = YearMonth.from(date)
                        },
                        dayCourses = dayCourses,
                        dayEvents = dayEvents,
                        onToggleEventComplete = { viewModel.toggleCalendarEventCompletion(it) },
                        onEditEvent = {
                            editingEvent = it
                            showAddEventDialog = true
                        },
                        onAddEventForDate = {
                            editingEvent = null
                            eventCategoryToCreate = null
                            showAddEventDialog = true
                        },
                        onNavigateToTimetable = onNavigateToTimetable
                    )
                } else {
                    // ---------------- WEEK VIEW ----------------
                    WeekCalendarView(
                        selectedDate = selectedDate,
                        today = today,
                        events = allCalendarEvents,
                        currentSemester = currentSemester,
                        viewModel = viewModel,
                        dayCourses = dayCourses,
                        dayEvents = dayEvents,
                        onDateSelected = { date ->
                            selectedDate = date
                            currentYearMonth = YearMonth.from(date)
                        },
                        onQuickCategoryClick = { cat ->
                            editingEvent = null
                            eventCategoryToCreate = cat
                            showAddEventDialog = true
                        },
                        onToggleEventComplete = { viewModel.toggleCalendarEventCompletion(it) },
                        onEditEvent = {
                            editingEvent = it
                            showAddEventDialog = true
                        },
                        onDeleteEvent = { viewModel.deleteCalendarEvent(it) },
                        onNavigateToTimetable = onNavigateToTimetable
                    )
                }
            }
        }
    }

    // Add / Edit Dialog
    if (showAddEventDialog) {
        AddEditCalendarEventDialog(
            initialEvent = editingEvent,
            initialCategory = eventCategoryToCreate,
            initialDate = selectedDateStr,
            courses = allCourses.filter { it.semester == currentSemester },
            onDismiss = { showAddEventDialog = false },
            onSave = { event ->
                if (editingEvent != null) {
                    viewModel.updateCalendarEvent(event)
                } else {
                    viewModel.insertCalendarEvent(event)
                }
            },
            onDelete = { event ->
                viewModel.deleteCalendarEvent(event)
            }
        )
    }
}

// ---------------------------------------------------------------------------------
// Sub-components: Top Calendar Controls & Headers
// ---------------------------------------------------------------------------------

@Composable
private fun CalendarUnifiedHeader(
    viewType: CalendarViewType,
    currentYearMonth: YearMonth,
    selectedDate: LocalDate,
    selectedWeekNum: Int?,
    onViewTypeChange: (CalendarViewType) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Title on the left: "2026年 9月" (Month) or "9月 第1週" (Week)
        Column {
            val locale = LocalConfiguration.current.locales[0]
            val titleText = if (viewType == CalendarViewType.MONTH) {
                "${currentYearMonth.year}年 ${currentYearMonth.monthValue}月"
            } else {
                val weekOfYear = selectedWeekNum?.let { "第${it}週" }
                    ?: "第${selectedDate.get(WeekFields.of(locale).weekOfMonth())}週"
                "${selectedDate.monthValue}月 $weekOfYear"
            }

            Text(
                text = titleText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Controls on the right: [ 週 | 月 ], < >, 今天
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // [ 週 | 月 ] Segmented Toggle Pill
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            ) {
                Row(modifier = Modifier.padding(2.dp)) {
                    // 週
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (viewType == CalendarViewType.WEEK) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .clickable { onViewTypeChange(CalendarViewType.WEEK) }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "週",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (viewType == CalendarViewType.WEEK) FontWeight.Bold else FontWeight.Normal,
                            color = if (viewType == CalendarViewType.WEEK) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 月
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (viewType == CalendarViewType.MONTH) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .clickable { onViewTypeChange(CalendarViewType.MONTH) }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "月",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (viewType == CalendarViewType.MONTH) FontWeight.Bold else FontWeight.Normal,
                            color = if (viewType == CalendarViewType.MONTH) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Navigation Arrows: < >
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrevious, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "前一個",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onNext, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "後一個",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekdayHeaderBar() {
    val weekDays = listOf("日", "一", "二", "三", "四", "五", "六")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        weekDays.forEachIndexed { index, label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = when (index) {
                    0 -> Color(0xFFEF4444) // Sunday in Red
                    6 -> Color(0xFF3B82F6) // Saturday in Blue
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ---------------------------------------------------------------------------------
// Month Calendar View (Full Grid with Dividers & Inline Event Chips)
// ---------------------------------------------------------------------------------

@Composable
private fun MonthCalendarView(
    yearMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    events: List<CalendarEvent>,
    courses: List<Course>,
    currentSemester: String,
    viewModel: StudentViewModel,
    onDateSelected: (LocalDate) -> Unit,
    dayCourses: List<Course>,
    dayEvents: List<CalendarEvent>,
    onToggleEventComplete: (CalendarEvent) -> Unit,
    onEditEvent: (CalendarEvent) -> Unit,
    onAddEventForDate: () -> Unit,
    onNavigateToTimetable: () -> Unit
) {
    val firstDayOfMonth = yearMonth.atDay(1)
    // Sunday as first day of week: Sunday = 7 -> 0, Monday = 1 -> 1, ..., Saturday = 6 -> 6
    val firstDayOfWeekIndex = (firstDayOfMonth.dayOfWeek.value % 7)
    val lengthOfMonth = yearMonth.lengthOfMonth()

    // Pre-calculate event map for this month
    val eventsByDate = remember(events, yearMonth) {
        events.groupBy { it.date }
    }

    val totalCells = firstDayOfWeekIndex + lengthOfMonth
    val totalRows = (totalCells + 6) / 7
    val firstCalendarDate = firstDayOfMonth.minusDays(firstDayOfWeekIndex.toLong())

    val weeksList = remember(yearMonth, totalRows) {
        (0 until totalRows).map { row ->
            (0..6).map { col ->
                val dayOffset = (row * 7 + col).toLong()
                val cellDate = firstCalendarDate.plusDays(dayOffset)
                object {
                    val date = cellDate
                    val dayNumber = cellDate.dayOfMonth
                    val isCurrentMonth = cellDate.monthValue == yearMonth.monthValue && cellDate.year == yearMonth.year
                    val dayOfWeekIndex = col
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 88.dp)
    ) {
        // Month Grid Rows
        weeksList.forEachIndexed { row, weekCells ->
            item(key = "month_row_$row") {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp)
                    ) {
                        weekCells.forEach { cell ->
                            val cellDateStr = cell.date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                            val cellEvents = eventsByDate[cellDateStr] ?: emptyList()
                            val hasClasses = remember(cellDateStr, currentSemester, courses) {
                                viewModel.getCoursesForDate(cellDateStr, currentSemester).isNotEmpty()
                            }

                            MonthDayCell(
                                dayNumber = cell.dayNumber,
                                isCurrentMonth = cell.isCurrentMonth,
                                isSelected = cell.date == selectedDate,
                                isToday = cell.date == today,
                                dayOfWeekIndex = cell.dayOfWeekIndex,
                                events = cellEvents,
                                hasClasses = hasClasses,
                                onClick = { onDateSelected(cell.date) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Horizontal divider separating each week
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        thickness = 0.5.dp
                    )
                }
            }
        }

        // Selected Date Bottom Agenda Drawer
        item(key = "selected_day_preview") {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header of Selected Date
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${selectedDate.monthValue}月${selectedDate.dayOfMonth}日 行程概覽",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (selectedDate == today) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "今天",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        FilledTonalButton(
                            onClick = onAddEventForDate,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("新增", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    // Classes for selected date
                    if (dayCourses.isNotEmpty()) {
                        Text(
                            text = "當日課程 (${dayCourses.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        dayCourses.forEach { course ->
                            CalendarCourseCard(course = course, onClick = onNavigateToTimetable)
                        }
                    }

                    // Events for selected date
                    if (dayEvents.isNotEmpty()) {
                        Text(
                            text = "待辦事項 (${dayEvents.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        dayEvents.forEach { event ->
                            CalendarEventItemRow(
                                event = event,
                                onToggleComplete = { onToggleEventComplete(event) },
                                onEdit = { onEditEvent(event) },
                                onDelete = {}
                            )
                        }
                    } else if (dayCourses.isEmpty()) {
                        Text(
                            text = "此日無安排課程或待辦事項 ✨",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthDayCell(
    dayNumber: Int,
    isCurrentMonth: Boolean,
    isSelected: Boolean,
    isToday: Boolean,
    dayOfWeekIndex: Int,
    events: List<CalendarEvent>,
    hasClasses: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 3.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Day Number Badge
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isSelected -> MaterialTheme.colorScheme.primary
                            isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            else -> Color.Transparent
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dayNumber.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimary
                        isToday -> MaterialTheme.colorScheme.primary
                        !isCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        dayOfWeekIndex == 0 -> Color(0xFFEF4444) // Sunday
                        dayOfWeekIndex == 6 -> Color(0xFF3B82F6) // Saturday
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Inline Event Banner / Chips
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Show up to 2 items
                events.take(2).forEach { event ->
                    val catColor = try {
                        Color(event.category.defaultColorHex.toColorInt())
                    } catch (_: Exception) {
                        MaterialTheme.colorScheme.primary
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(catColor.copy(alpha = 0.85f))
                            .padding(horizontal = 3.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = event.title,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // If class exists, show a small course dot or badge if room
                if (hasClasses && events.size < 2) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .padding(horizontal = 3.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "有課",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (events.size > 2) {
                    Text(
                        text = "+${events.size - 2}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// Week Calendar View (Single Week Strip + Quick Add Category Chips + Schedule)
// ---------------------------------------------------------------------------------

@Composable
private fun WeekCalendarView(
    selectedDate: LocalDate,
    today: LocalDate,
    events: List<CalendarEvent>,
    currentSemester: String,
    viewModel: StudentViewModel,
    dayCourses: List<Course>,
    dayEvents: List<CalendarEvent>,
    onDateSelected: (LocalDate) -> Unit,
    onQuickCategoryClick: (CalendarEventCategory) -> Unit,
    onToggleEventComplete: (CalendarEvent) -> Unit,
    onEditEvent: (CalendarEvent) -> Unit,
    onDeleteEvent: (CalendarEvent) -> Unit,
    onNavigateToTimetable: () -> Unit
) {
    // Determine start of current week (Sunday)
    val dayOfWeek = selectedDate.dayOfWeek
    val daysFromSunday = dayOfWeek.value % 7
    val sundayOfWeek = selectedDate.minusDays(daysFromSunday.toLong())
    val weekDates = (0..6).map { sundayOfWeek.plusDays(it.toLong()) }

    val eventsByDate = remember(events) {
        events.groupBy { it.date }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Single Week Day Selector Strip
        item(key = "week_strip") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                weekDates.forEachIndexed { index, date ->
                    val isSelected = date == selectedDate
                    val isToday = date == today
                    val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    val hasEvents = (eventsByDate[dateStr]?.isNotEmpty() == true)
                    val hasClasses = remember(dateStr, currentSemester) {
                        viewModel.getCoursesForDate(dateStr, currentSemester).isNotEmpty()
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onDateSelected(date) }
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Date circle
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                        else -> Color.Transparent
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = date.dayOfMonth.toString(),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                    isToday -> MaterialTheme.colorScheme.primary
                                    index == 0 -> Color(0xFFEF4444)
                                    index == 6 -> Color(0xFF3B82F6)
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        // Event Indicator Dot
                        if (hasEvents || hasClasses) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (hasEvents) Color(0xFFEC4899) else MaterialTheme.colorScheme.primary
                                    )
                            )
                        } else {
                            Spacer(modifier = Modifier.height(5.dp))
                        }
                    }
                }
            }
        }

        // Quick Category Adders (學習 +, 作業 +, 考試 +, 個人 +, 活動 +, 放假 +)
        item(key = "quick_category_chips") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CalendarEventCategory.entries.forEach { cat ->
                    val catColor = try {
                        Color(cat.defaultColorHex.toColorInt())
                    } catch (_: Exception) {
                        MaterialTheme.colorScheme.primary
                    }

                    Surface(
                        onClick = { onQuickCategoryClick(cat) },
                        shape = RoundedCornerShape(12.dp),
                        color = catColor.copy(alpha = 0.12f),
                        border = BorderStroke(0.8.dp, catColor.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = cat.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = catColor
                            )
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = catColor
                            )
                        }
                    }
                }
            }
        }

        // Section: Classes for this day
        if (dayCourses.isNotEmpty()) {
            item(key = "day_courses_header") {
                Text(
                    text = "當日課堂 (${dayCourses.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }
            items(dayCourses, key = { "week_course_${it.id}" }) { course ->
                CalendarCourseCard(course = course, onClick = onNavigateToTimetable)
            }
        }

        // Section: Events & Tasks for this day
        item(key = "day_events_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "行程與待辦 (${dayEvents.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (dayEvents.isEmpty()) {
            item(key = "empty_events") {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Event,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "今日無行程安排，點選上方標籤快速新增 ✍️",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        } else {
            items(dayEvents, key = { "week_event_${it.id}" }) { event ->
                CalendarEventItemRow(
                    event = event,
                    onToggleComplete = { onToggleEventComplete(event) },
                    onEdit = { onEditEvent(event) },
                    onDelete = { onDeleteEvent(event) }
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// Common Cards: Course Card & Event Item with Checkbox & Options Menu
// ---------------------------------------------------------------------------------

@Composable
private fun CalendarCourseCard(
    course: Course,
    onClick: () -> Unit
) {
    val courseColor = try {
        Color(course.colorHex.toColorInt())
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 3.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Color stripe
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(38.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(courseColor)
            )

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = course.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val timeDisplay = if (course.startTime.isNotBlank() && course.endTime.isNotBlank()) {
                        "${course.startTime} ~ ${course.endTime}"
                    } else {
                        "第 ${course.startPeriod}-${course.endPeriod} 節"
                    }
                    Text(
                        text = timeDisplay,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (course.location.isNotBlank()) {
                        Text(
                            text = "· ${course.location}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = courseColor.copy(alpha = 0.12f)
            ) {
                Text(
                    text = "${course.credits} 學分",
                    style = MaterialTheme.typography.labelSmall,
                    color = courseColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun CalendarEventItemRow(
    event: CalendarEvent,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val catColor = try {
        Color(event.category.defaultColorHex.toColorInt())
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 3.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        onClick = onEdit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Checkbox with rounded shape
            Checkbox(
                checked = event.isCompleted,
                onCheckedChange = { onToggleComplete() },
                colors = CheckboxDefaults.colors(
                    checkedColor = catColor,
                    checkmarkColor = Color.White
                )
            )

            // Category tag
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = catColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = event.category.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = catColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            // Title & Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (event.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (event.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f) else MaterialTheme.colorScheme.onSurface
                )
                if (!event.isAllDay && event.startTime.isNotBlank()) {
                    Text(
                        text = "${event.startTime} ~ ${event.endTime}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // [ ··· ] More Options Dropdown
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.MoreHoriz,
                        contentDescription = "更多選項",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("編輯事項") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("刪除事項", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

