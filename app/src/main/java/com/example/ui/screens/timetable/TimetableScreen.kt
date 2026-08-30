package com.example.ui.screens.timetable

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Course
import com.example.ui.theme.SapphirePrimary
import com.example.ui.viewmodel.StudentViewModel
import com.example.util.TimetableImageGenerator
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    viewModel: StudentViewModel,
    modifier: Modifier = Modifier,
    onNavigateToGrades: () -> Unit = {}
) {
    val context = LocalContext.current
    val selectedSemester by viewModel.selectedSemester.collectAsStateWithLifecycle()
    val allSemesters by viewModel.allSemesters.collectAsStateWithLifecycle()
    val courses by viewModel.currentSemesterCourses.collectAsStateWithLifecycle()
    val allCoursesList by viewModel.allCourses.collectAsStateWithLifecycle(emptyList())
    val graduationPlan by viewModel.graduationPlan.collectAsStateWithLifecycle()
    val semesterTimeConfigVersion by viewModel.semesterTimeConfigVersion.collectAsStateWithLifecycle()

    val currentStartDateStr = remember(selectedSemester, semesterTimeConfigVersion) {
        viewModel.getSemesterStartDate(selectedSemester)
    }
    val currentTotalWeeks = remember(selectedSemester, semesterTimeConfigVersion) {
        viewModel.getSemesterTotalWeeks(selectedSemester)
    }

    val countdownBadgeText = remember(currentStartDateStr, currentTotalWeeks) {
        try {
            val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
            val startDate = LocalDate.parse(currentStartDateStr, formatter)
            val today = LocalDate.now()
            val daysDiff = ChronoUnit.DAYS.between(today, startDate)
            when {
                daysDiff > 0 -> "開學 D-$daysDiff"
                daysDiff == 0L -> "今日開學"
                else -> {
                    val daysPassed = -daysDiff
                    val weekNum = (daysPassed / 7).toInt() + 1
                    if (weekNum <= currentTotalWeeks) "第 $weekNum 週" else "學期結束"
                }
            }
        } catch (_: Exception) {
            "設定開學日"
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var showImageImportDialog by remember { mutableStateOf(false) }
    var editingCourse by remember { mutableStateOf<Course?>(null) }
    var selectedCourseDetail by remember { mutableStateOf<Course?>(null) }
    var showSemesterManageDialog by remember { mutableStateOf(false) }
    var showTimeSettingsSheet by remember { mutableStateOf(false) }
    var isGridView by remember { mutableStateOf(true) }
    var isFabExpanded by remember { mutableStateOf(false) }
    val isWeeklyMode by viewModel.isWeeklyMode.collectAsStateWithLifecycle()
    val showWeekend by viewModel.showWeekend.collectAsStateWithLifecycle()
    val showTimeInsteadOfPeriod by viewModel.showTimeInsteadOfPeriod.collectAsStateWithLifecycle()

    val initialWeekIndex = remember(currentStartDateStr, currentTotalWeeks) {
        try {
            val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
            val startDate = LocalDate.parse(currentStartDateStr, formatter)
            val today = LocalDate.now()
            val daysDiff = ChronoUnit.DAYS.between(startDate, today)
            if (daysDiff >= 0) {
                val weekNum = (daysDiff / 7).toInt()
                weekNum.coerceIn(0, (currentTotalWeeks - 1).coerceAtLeast(0))
            } else 0
        } catch (_: Exception) { 0 }
    }
    val weekPagerState = rememberPagerState(initialPage = initialWeekIndex) { currentTotalWeeks }
    val currentWeek = (weekPagerState.currentPage + 1).coerceAtMost(currentTotalWeeks)

    val daysCount = if (showWeekend) 7 else 5
    val dayNames = if (showWeekend) {
        listOf("週一", "週二", "週三", "週四", "週五", "週六", "週日")
    } else {
        listOf("週一", "週二", "週三", "週四", "週五")
    }

    val displayCourses = remember(courses, isWeeklyMode, currentWeek) {
        if (!isWeeklyMode) courses
        else courses.filter { isCourseInWeek(it, currentWeek) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Semester Switcher & View Mode Toggles
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Semester selector button
                OutlinedButton(
                    onClick = { showSemesterManageDialog = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = formatSemesterHeaderLabel(selectedSemester, graduationPlan.admissionSemester),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "選擇學期",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Toggle 5-day / 7-day week display button
                    FilledTonalIconButton(
                        onClick = {
                            viewModel.setShowWeekend(!showWeekend)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (showWeekend) SapphirePrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (showWeekend) SapphirePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarViewWeek,
                            contentDescription = if (showWeekend) "切換為平日 (五天)" else "切換為一週 (七天)",
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Share Timetable Image Button (Direct image share)
                    FilledTonalIconButton(
                        onClick = {
                            val semesterLabel = formatSemesterHeaderLabel(selectedSemester, graduationPlan.admissionSemester)
                            val currentWeekNum = if (isWeeklyMode) currentWeek else 0
                            val currentWeekDates = if (isWeeklyMode) getWeekDates(currentStartDateStr, currentWeek, daysCount) else null
                            val shareCourses = if (isWeeklyMode) courses.filter { isCourseInWeek(it, currentWeek) } else courses
                            TimetableImageGenerator.shareTimetableImage(
                                context = context,
                                semesterLabel = semesterLabel,
                                courses = shareCourses,
                                showWeekend = showWeekend,
                                showTimeInsteadOfPeriod = showTimeInsteadOfPeriod,
                                weekNum = currentWeekNum,
                                dates = currentWeekDates
                            )
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "分享課表圖片",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Grade Entry Button (Calculator icon)
                    FilledTonalIconButton(
                        onClick = onNavigateToGrades,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("grade_entry_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = "登錄成績",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Summary Row with Countdown & Credits Badges
            val totalCredits = courses.sumOf { it.credits }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Countdown Badge (開學 D-13 / 第 X 週)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showTimeSettingsSheet = true }
                    ) {
                        Text(
                            text = countdownBadgeText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    // Credits Badge (X 學分)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = "${totalCredits.toInt()} 學分",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Text(
                    text = if (isGridView) (if (isWeeklyMode) "左右滑動切換週次" else "點擊左上角切換模式") else "點擊列表查看詳細",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val cycleNextMode: () -> Unit = {
                when {
                    // 1. 各週 + 節次 -> 2. 各週 + 時間
                    isWeeklyMode && !showTimeInsteadOfPeriod -> {
                        viewModel.setShowTimeInsteadOfPeriod(true)
                    }
                    // 2. 各週 + 時間 -> 3. 整學期 + 節次
                    isWeeklyMode && showTimeInsteadOfPeriod -> {
                        viewModel.setIsWeeklyMode(false)
                        viewModel.setShowTimeInsteadOfPeriod(false)
                    }
                    // 3. 整學期 + 節次 -> 4. 整學期 + 時間
                    !isWeeklyMode && !showTimeInsteadOfPeriod -> {
                        viewModel.setShowTimeInsteadOfPeriod(true)
                    }
                    // 4. 整學期 + 時間 -> 1. 各週 + 節次
                    else -> {
                        viewModel.setIsWeeklyMode(true)
                        viewModel.setShowTimeInsteadOfPeriod(false)
                    }
                }
            }

            if (!isGridView) {
                // List View Mode
                if (courses.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "這個學期還沒有排定課程",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(displayCourses.sortedWith(compareBy({ it.dayOfWeek }, { it.startPeriod }))) { course ->
                            CourseListItemCard(
                                course = course,
                                onClick = { selectedCourseDetail = course }
                            )
                        }
                    }
                }
            } else if (!isWeeklyMode) {
                // 整學期課表模式：不可左右滑動，顯示整學期全部課程
                WeeklyTimetableGrid(
                    courses = courses,
                    daysCount = daysCount,
                    dayNames = dayNames,
                    selectedWeek = 0,
                    onModeToggle = cycleNextMode,
                    onCourseClick = { selectedCourseDetail = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    showTimeInsteadOfPeriod = showTimeInsteadOfPeriod,
                    dates = null
                )
            } else {
                // 各週課表模式：可在第 1 ~ 18 週之間左右滑動
                HorizontalPager(
                    state = weekPagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) { page ->
                    val weekNum = page + 1
                    val pageCourses = remember(courses, weekNum) {
                        courses.filter { isCourseInWeek(it, weekNum) }
                    }
                    val pageDates = remember(currentStartDateStr, weekNum, daysCount) {
                        getWeekDates(currentStartDateStr, weekNum, daysCount)
                    }

                    WeeklyTimetableGrid(
                        courses = pageCourses,
                        daysCount = daysCount,
                        dayNames = dayNames,
                        selectedWeek = weekNum,
                        onModeToggle = cycleNextMode,
                        onCourseClick = { selectedCourseDetail = it },
                        modifier = Modifier.fillMaxSize(),
                        showTimeInsteadOfPeriod = showTimeInsteadOfPeriod,
                        dates = pageDates
                    )
                }
            }
        }

        // Tap-outside backdrop scrim when speed dial is open
        if (isFabExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        isFabExpanded = false
                    }
            )
        }

        // Floating Action Button with Speed Dial Popup Menu
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnimatedVisibility(
                visible = isFabExpanded,
                enter = fadeIn() + slideInVertically { it / 2 } + scaleIn(transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 1f)),
                exit = fadeOut() + slideOutVertically { it / 2 } + scaleOut(transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 1f))
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    shadowElevation = 10.dp,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.clip(RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier.width(IntrinsicSize.Max)
                    ) {
                        // Option 1: 手動輸入 (Manual Input)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    isFabExpanded = false
                                    editingCourse = null
                                    showAddDialog = true
                                }
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.EditCalendar,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "手動輸入",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                            thickness = 0.8.dp,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        // Option 2: 拍照/圖片導入 (Photo/Image AI Import)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    isFabExpanded = false
                                    showImageImportDialog = true
                                }
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PhotoCamera,
                                contentDescription = null,
                                tint = SapphirePrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "照片導入",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = { isFabExpanded = !isFabExpanded },
                shape = CircleShape,
                containerColor = SapphirePrimary,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .size(56.dp)
                    .testTag("add_course_button")
            ) {
                Icon(
                    imageVector = if (isFabExpanded) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = if (isFabExpanded) "關閉選單" else "新增課程",
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }

    // Dialogs & Bottom Sheet
    if (showImageImportDialog) {
        TimetableImageImportDialog(
            initialSemester = selectedSemester,
            allSemesters = allSemesters,
            allCourses = allCoursesList,
            onDismiss = { showImageImportDialog = false },
            onConfirmImport = { importedCourses ->
                viewModel.addCourses(importedCourses)
                showImageImportDialog = false
            }
        )
    }

    if (showAddDialog) {
        AddEditCourseDialog(
            initialCourse = editingCourse,
            defaultSemester = selectedSemester,
            allCourses = allCoursesList,
            plan = graduationPlan,
            onDismiss = { showAddDialog = false },
            onSave = { course ->
                if (editingCourse == null) {
                    viewModel.addCourse(course)
                } else {
                    viewModel.updateCourse(course)
                }
                showAddDialog = false
            },
            onSaveMultiple = { coursesToSave ->
                if (editingCourse == null) {
                    viewModel.addCourses(coursesToSave)
                } else {
                    coursesToSave.firstOrNull()?.let { viewModel.updateCourse(it) }
                    coursesToSave.drop(1).forEach { viewModel.addCourse(it, sendNotify = false) }
                }
                showAddDialog = false
            }
        )
    }

    selectedCourseDetail?.let { course ->
        val attendanceMap = remember(course.id, semesterTimeConfigVersion) {
            viewModel.getCourseAttendance(course.id)
        }
        val notesList = remember(course.id, semesterTimeConfigVersion) {
            viewModel.getCourseNotes(course.id)
        }
        CourseDetailBottomSheet(
            course = course,
            semesterStartDate = currentStartDateStr,
            totalWeeks = currentTotalWeeks,
            attendanceMap = attendanceMap,
            notesList = notesList,
            onUpdateAttendance = { week, status ->
                val newMap = attendanceMap.toMutableMap()
                if (newMap[week] == status) {
                    newMap.remove(week)
                } else {
                    newMap[week] = status
                }
                viewModel.saveCourseAttendance(course.id, newMap)
            },
            onAddNote = { content, category, week ->
                val currentNotes = viewModel.getCourseNotes(course.id).toMutableList()
                currentNotes.add(0, com.example.data.model.CourseNote(content = content, category = category, week = week))
                viewModel.saveCourseNotes(course.id, currentNotes)
            },
            onDeleteNote = { noteId ->
                val currentNotes = viewModel.getCourseNotes(course.id).filter { it.id != noteId }
                viewModel.saveCourseNotes(course.id, currentNotes)
            },
            onDismiss = { selectedCourseDetail = null },
            onEdit = {
                selectedCourseDetail = null
                editingCourse = course
                showAddDialog = true
            },
            onDelete = {
                viewModel.deleteCourse(course)
                selectedCourseDetail = null
            }
        )
    }

    if (showSemesterManageDialog) {
        SemesterManageDialog(
            selectedSemester = selectedSemester,
            primarySemester = graduationPlan.currentSemester,
            allSemesters = allSemesters,
            admissionSemester = graduationPlan.admissionSemester,
            onSelectSemester = { sem ->
                viewModel.setSelectedSemester(sem)
            },
            onSetPrimarySemester = { sem ->
                viewModel.setPrimarySemester(sem)
            },
            onDismiss = { showSemesterManageDialog = false }
        )
    }

    val currentEndDateStr = remember(selectedSemester, semesterTimeConfigVersion) {
        viewModel.getSemesterEndDate(selectedSemester)
    }

    if (showTimeSettingsSheet) {
        SemesterTimeSettingsBottomSheet(
            initialStartDate = currentStartDateStr,
            initialEndDate = currentEndDateStr,
            initialShowTime = showTimeInsteadOfPeriod,
            onDismiss = { showTimeSettingsSheet = false },
            onSave = { startDate, endDate, totalWeeks, showTime ->
                viewModel.saveSemesterTimeConfig(selectedSemester, startDate, endDate, totalWeeks)
                viewModel.setShowTimeInsteadOfPeriod(showTime)
                showTimeSettingsSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SemesterTimeSettingsBottomSheet(
    initialStartDate: String,
    initialEndDate: String,
    initialShowTime: Boolean,
    onDismiss: () -> Unit,
    onSave: (startDate: String, endDate: String, totalWeeks: Int, showTime: Boolean) -> Unit
) {
    val context = LocalContext.current
    var tempStartDate by remember(initialStartDate) { mutableStateOf(initialStartDate) }
    var tempEndDate by remember(initialEndDate, initialStartDate) {
        mutableStateOf(
            initialEndDate.ifBlank {
                try {
                    val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
                    val s = LocalDate.parse(initialStartDate, formatter)
                    s.plusWeeks(18).minusDays(1).format(formatter)
                } catch (_: Exception) { "" }
            }
        )
    }
    var tempShowTime by remember(initialShowTime) { mutableStateOf(initialShowTime) }

    val calculatedDuration = remember(tempStartDate, tempEndDate) {
        try {
            val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
            val start = LocalDate.parse(tempStartDate, formatter)
            val end = LocalDate.parse(tempEndDate, formatter)
            val totalDays = ChronoUnit.DAYS.between(start, end) + 1
            if (totalDays > 0) {
                val weeks = maxOf(1, ((totalDays + 6) / 7).toInt())
                Triple(totalDays, weeks, null)
            } else {
                Triple(0L, 1, "結束日不可早於開學日")
            }
        } catch (_: Exception) {
            Triple(0L, 18, null)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "設定",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Section 1: 開學日
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "開學日",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            val curYear = tempStartDate.substringBefore(".").toIntOrNull() ?: 2026
                            val curMonth = tempStartDate.split(".").getOrNull(1)?.toIntOrNull()?.minus(1) ?: 8
                            val curDay = tempStartDate.substringAfterLast(".").toIntOrNull() ?: 7
                            android.app.DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    tempStartDate = String.format(Locale.US, "%04d.%02d.%02d", y, m + 1, d)
                                },
                                curYear,
                                curMonth,
                                curDay
                            ).show()
                        }
                ) {
                    Text(
                        text = tempStartDate,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

            // Section 2: 結束日
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "結束日",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            val curYear = tempEndDate.substringBefore(".").toIntOrNull() ?: 2027
                            val curMonth = tempEndDate.split(".").getOrNull(1)?.toIntOrNull()?.minus(1) ?: 0
                            val curDay = tempEndDate.substringAfterLast(".").toIntOrNull() ?: 15
                            android.app.DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    tempEndDate = String.format(Locale.US, "%04d.%02d.%02d", y, m + 1, d)
                                },
                                curYear,
                                curMonth,
                                curDay
                            ).show()
                        }
                ) {
                    Text(
                        text = tempEndDate.ifBlank { "選擇日期" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

            // Section 3: 側邊欄顯示方式 (節次 / 時間)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "側邊欄顯示",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (!tempShowTime) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { tempShowTime = false }
                    ) {
                        Text(
                            text = "節次 (0,1,2)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (!tempShowTime) FontWeight.Bold else FontWeight.Normal,
                            color = if (!tempShowTime) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (tempShowTime) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { tempShowTime = true }
                    ) {
                        Text(
                            text = "時間 (08:10)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (tempShowTime) FontWeight.Bold else FontWeight.Normal,
                            color = if (tempShowTime) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Duration Summary or Error Banner
            if (calculatedDuration.third != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = calculatedDuration.third!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "學期總長度：共 ${calculatedDuration.first} 天 (約 ${calculatedDuration.second} 週)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Save Button
            Button(
                onClick = {
                    if (calculatedDuration.third == null) {
                        onSave(tempStartDate, tempEndDate, calculatedDuration.second, tempShowTime)
                    }
                },
                enabled = calculatedDuration.third == null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SapphirePrimary)
            ) {
                Text(
                    text = "儲存",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun WeeklyTimetableGrid(
    courses: List<Course>,
    daysCount: Int,
    dayNames: List<String>,
    selectedWeek: Int,
    onModeToggle: () -> Unit,
    onCourseClick: (Course) -> Unit,
    modifier: Modifier = Modifier,
    showTimeInsteadOfPeriod: Boolean = false,
    dates: List<String>? = null
) {
    val scrollState = rememberScrollState()
    val minPeriod = 0
    val defaultMaxPeriod = 10 // A 節 (17:10~18:00)
    val maxEndPeriod = courses.maxOfOrNull { it.endPeriod } ?: defaultMaxPeriod
    val maxPeriodToDisplay = remember(maxEndPeriod) {
        maxOf(defaultMaxPeriod, maxEndPeriod)
    }
    val totalPeriods = maxPeriodToDisplay - minPeriod + 1
    val hourHeight = 60.dp
    val timeColumnWidth = if (showTimeInsteadOfPeriod) 46.dp else 36.dp

    fun getPeriodCode(period: Int): String = when (period) {
        0 -> "0"
        10 -> "A"
        11 -> "B"
        12 -> "C"
        13 -> "D"
        14 -> "E"
        15 -> "F"
        else -> "$period"
    }

    fun getPeriodTimeRange(period: Int): Pair<String, String> = when (period) {
        0 -> "07:10" to "08:00"
        1 -> "08:10" to "09:00"
        2 -> "09:10" to "10:00"
        3 -> "10:10" to "11:00"
        4 -> "11:10" to "12:00"
        5 -> "13:10" to "14:00"
        6 -> "14:10" to "15:00"
        7 -> "15:10" to "16:00"
        8 -> "16:10" to "17:00"
        9 -> "17:10" to "18:00"
        10 -> "18:20" to "19:10"
        11 -> "19:15" to "20:05"
        12 -> "20:10" to "21:00"
        13 -> "21:05" to "21:55"
        14 -> "22:00" to "22:50"
        else -> String.format(Locale.US, "%02d:00", (7 + period)) to String.format(Locale.US, "%02d:50", (7 + period))
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
    ) {
        // Table Header: Mode toggle icon & Day of week columns
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mode toggle corner button (一鍵循環切換四模式：各週節次 -> 各週時間 -> 整學期節次 -> 整學期時間)
            Box(
                modifier = Modifier
                    .width(timeColumnWidth)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onModeToggle() },
                contentAlignment = Alignment.Center
            ) {
                val icon = when {
                    selectedWeek > 0 && !showTimeInsteadOfPeriod -> Icons.Default.CalendarToday
                    selectedWeek > 0 && showTimeInsteadOfPeriod -> Icons.Default.CalendarMonth
                    selectedWeek == 0 && !showTimeInsteadOfPeriod -> Icons.Default.GridView
                    else -> Icons.Default.Schedule
                }
                val desc = when {
                    selectedWeek > 0 && !showTimeInsteadOfPeriod -> "各週節次模式"
                    selectedWeek > 0 && showTimeInsteadOfPeriod -> "各週時間模式"
                    selectedWeek == 0 && !showTimeInsteadOfPeriod -> "整學期節次模式"
                    else -> "整學期時間模式"
                }
                Icon(
                    imageVector = icon,
                    contentDescription = desc,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            dayNames.forEachIndexed { index, dayName ->
                val dateStr = dates?.getOrNull(index)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = dayName.removePrefix("週"),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (dateStr != null) {
                        Text(
                            text = dateStr,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))

        // Scrollable area combining timeline hour labels and day columns
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            // Timeline Period Numbers / Time Column
            Box(
                modifier = Modifier
                    .width(timeColumnWidth)
                    .height(hourHeight * totalPeriods)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .clip(RoundedCornerShape(topStart = 0.dp, bottomStart = 12.dp))
            ) {
                for (p in minPeriod..maxPeriodToDisplay) {
                    val periodIndex = p - minPeriod
                    Box(
                        modifier = Modifier
                            .offset(y = hourHeight * periodIndex)
                            .width(timeColumnWidth)
                            .height(hourHeight)
                            .border(
                                width = 0.5.dp,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (showTimeInsteadOfPeriod) {
                            val timeRange = getPeriodTimeRange(p)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxHeight().padding(horizontal = 2.dp)
                            ) {
                                Text(
                                    text = timeRange.first,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = timeRange.second,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Normal
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                                )
                            }
                        } else {
                            Text(
                                text = getPeriodCode(p),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // Day Columns & Grid
            for (day in 1..daysCount) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(hourHeight * totalPeriods)
                ) {
                    // Background horizontal grid lines for each period
                    Column {
                        repeat(totalPeriods) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(hourHeight)
                                    .border(
                                        width = 0.5.dp,
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    )
                            )
                        }
                    }

                    // Courses for this day
                    val dayCourses = courses.filter { it.dayOfWeek == day }
                    dayCourses.forEach { course ->
                        val startPeriodOffset = (course.startPeriod - minPeriod).coerceAtLeast(0)
                        val duration = (course.endPeriod - course.startPeriod + 1).coerceAtLeast(1)
                        val courseColor = runCatching { Color(course.colorHex.toColorInt()) }
                            .getOrDefault(SapphirePrimary)

                        Box(
                            modifier = Modifier
                                .padding(horizontal = 2.dp, vertical = 2.dp)
                                .offset(y = hourHeight * startPeriodOffset)
                                .fillMaxWidth()
                                .height(hourHeight * duration - 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(courseColor)
                                .clickable { onCourseClick(course) }
                                .padding(horizontal = 5.dp, vertical = 5.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.Start,
                                verticalArrangement = Arrangement.spacedBy(1.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                if (course.location.isNotBlank()) {
                                    Text(
                                        text = course.location,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = Color.White.copy(alpha = 0.9f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = course.name,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 13.sp
                                    ),
                                    color = Color.White,
                                    maxLines = if (duration > 1) 3 else 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (course.teacher.isNotBlank() && duration > 1) {
                                    Text(
                                        text = course.teacher,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Normal
                                        ),
                                        color = Color.White.copy(alpha = 0.85f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
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

@Composable
private fun CourseListItemCard(
    course: Course,
    onClick: () -> Unit
) {
    val courseColor = runCatching { Color(course.colorHex.toColorInt()) }
        .getOrDefault(SapphirePrimary)

    val days = listOf("星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日")
    val dayName = days.getOrElse(course.dayOfWeek - 1) { "星期一" }

    val formatPeriod = { p: Int ->
        when (p) {
            0 -> "0"
            10 -> "A"
            11 -> "B"
            12 -> "C"
            13 -> "D"
            14 -> "E"
            15 -> "F"
            else -> "$p"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                    .width(4.dp)
                    .height(42.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(courseColor)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = course.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Badge(
                        containerColor = course.category.badgeColor.copy(alpha = 0.15f),
                        contentColor = course.category.badgeColor
                    ) {
                        Text(text = course.category.shortLabel)
                    }
                }
                Text(
                    text = "$dayName 第 ${formatPeriod(course.startPeriod)} ~ ${formatPeriod(course.endPeriod)} 節" +
                            if (course.location.isNotBlank()) "・${course.location}" else "" +
                            if (course.teacher.isNotBlank()) "・${course.teacher}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${course.credits} 學分",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = SapphirePrimary
                )
                if (course.score != null || course.letterGrade != null) {
                    Text(
                        text = course.letterGrade ?: "${course.score?.toInt()}分",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatSemesterHeaderLabel(sem: String, admissionSemester: String): String {
    val startYear = admissionSemester.substringBefore("-").filter { it.isDigit() }.toIntOrNull()
    val year = sem.substringBefore("-").filter { it.isDigit() }.toIntOrNull()
    val term = sem.substringAfter("-").filter { it.isDigit() }.toIntOrNull() ?: 1
    if (startYear != null && year != null) {
        val grade = when (val diff = year - startYear) {
            0 -> "大一"
            1 -> "大二"
            2 -> "大三"
            3 -> "大四"
            else -> if (diff > 3) "延畢" else ""
        }
        val termStr = if (term == 1) "上" else "下"
        if (grade.isNotEmpty()) {
            return "$grade$termStr"
        }
    }
    return sem
}

private fun isCourseInWeek(course: Course, week: Int): Boolean {
    if (course.repeatMode == "單週") return week % 2 != 0
    if (course.repeatMode == "雙週") return week % 2 == 0
    if (course.repeatMode == "每週" || course.repeatWeeks == "1-18" || course.repeatWeeks.isBlank()) return true
    val weeks = course.repeatWeeks.split(",").mapNotNull { it.trim().toIntOrNull() }
    return week in weeks
}

private fun getWeekDates(startDateStr: String, week: Int, count: Int): List<String>? {
    return try {
        val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
        val startDate = LocalDate.parse(startDateStr, formatter)
        // 課表第一欄為週一，故先校準至開學週的週一 (DayOfWeek: MONDAY=1, SUNDAY=7)
        val mondayOfFirstWeek = startDate.minusDays((startDate.dayOfWeek.value - 1).toLong())
        val mondayOfWeek = mondayOfFirstWeek.plusWeeks((week - 1).toLong())
        (0 until count).map { dayOffset ->
            val date = mondayOfWeek.plusDays(dayOffset.toLong())
            "${date.monthValue}/${date.dayOfMonth}"
        }
    } catch (_: Exception) {
        null
    }
}
