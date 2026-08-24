package com.example.ui.screens.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    viewModel: StudentViewModel,
    modifier: Modifier = Modifier,
    onNavigateToGrades: () -> Unit = {},
    onNavigateToGraduation: () -> Unit = {}
) {
    val selectedSemester by viewModel.selectedSemester.collectAsStateWithLifecycle()
    val allSemesters by viewModel.allSemesters.collectAsStateWithLifecycle()
    val courses by viewModel.currentSemesterCourses.collectAsStateWithLifecycle()
    val graduationPlan by viewModel.graduationPlan.collectAsStateWithLifecycle()

    fun formatSemesterLabel(sem: String): String {
        val startYear = graduationPlan.admissionSemester.substringBefore("-").filter { it.isDigit() }.toIntOrNull()
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
                return "$sem 學期 ($grade$termStr)"
            }
        }
        return "$sem 學期"
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingCourse by remember { mutableStateOf<Course?>(null) }
    var selectedCourseDetail by remember { mutableStateOf<Course?>(null) }
    var showSemesterManageDialog by remember { mutableStateOf(false) }
    var isGridView by remember { mutableStateOf(true) }
    val showWeekend by viewModel.showWeekend.collectAsStateWithLifecycle()

    val daysCount = if (showWeekend) 7 else 5
    val dayNames = if (showWeekend) {
        listOf("週一", "週二", "週三", "週四", "週五", "週六", "週日")
    } else {
        listOf("週一", "週二", "週三", "週四", "週五")
    }

    val maxPeriod = remember(courses) {
        val courseMax = courses.maxOfOrNull { it.endPeriod } ?: 8
        maxOf(8, courseMax)
    }

    Column(
        modifier = modifier
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
                        text = formatSemesterLabel(selectedSemester),
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
                    // Graduation Audit Button (School icon)
                    FilledTonalIconButton(
                        onClick = onNavigateToGraduation,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("graduation_audit_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = "畢業審查",
                            modifier = Modifier.size(22.dp)
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

                    // Add Course Button (Icon only)
                    FilledIconButton(
                        onClick = {
                            editingCourse = null
                            showAddDialog = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = SapphirePrimary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("add_course_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "新增課程",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Summary Chip
            val totalCredits = courses.sumOf { it.credits }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "本學期共 ${courses.size} 門課・合計 ${totalCredits.toInt()} 學分",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (isGridView) "點擊課表格子查看詳細" else "點擊列表查看詳細",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (courses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "這個學期還沒有排定課程",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = {
                                editingCourse = null
                                showAddDialog = true
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("新增第一門課")
                        }
                    }
                }
            } else if (!isGridView) {
                // List View Mode
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(courses.sortedWith(compareBy({ it.dayOfWeek }, { it.startPeriod }))) { course ->
                        CourseListItemCard(
                            course = course,
                            onClick = { selectedCourseDetail = course }
                        )
                    }
                }
            } else {
                // Weekly Grid View Mode
                WeeklyTimetableGrid(
                    courses = courses,
                    daysCount = daysCount,
                    dayNames = dayNames,
                    maxPeriod = maxPeriod,
                    onCourseClick = { selectedCourseDetail = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }

    // Dialogs & Bottom Sheet
    if (showAddDialog) {
        AddEditCourseDialog(
            initialCourse = editingCourse,
            defaultSemester = selectedSemester,
            onDismiss = { showAddDialog = false },
            onSave = { course ->
                if (editingCourse == null) {
                    viewModel.addCourse(course)
                } else {
                    viewModel.updateCourse(course)
                }
                showAddDialog = false
            }
        )
    }

    selectedCourseDetail?.let { course ->
        CourseDetailBottomSheet(
            course = course,
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
}

@Composable
private fun WeeklyTimetableGrid(
    courses: List<Course>,
    daysCount: Int,
    dayNames: List<String>,
    maxPeriod: Int,
    onCourseClick: (Course) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val periodHeight = 64.dp

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
    ) {
        // Table Header: Day of week columns
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Period label corner
            Text(
                text = "節次",
                modifier = Modifier.width(40.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            dayNames.forEach { dayName ->
                Text(
                    text = dayName,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        // Scrollable area combining period numbers and day columns
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            // Period Number Column
            Column(modifier = Modifier.width(40.dp)) {
                for (period in 1..maxPeriod) {
                    val periodLabel = when (period) {
                        10 -> "A"
                        11 -> "B"
                        12 -> "C"
                        13 -> "D"
                        14 -> "E"
                        else -> "$period"
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(periodHeight)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f))
                            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = periodLabel,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Day Columns
            for (day in 1..daysCount) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(periodHeight * maxPeriod)
                ) {
                    // Background horizontal grid lines
                    Column {
                        repeat(maxPeriod) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(periodHeight)
                                    .border(0.5.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            )
                        }
                    }

                    // Courses for this day
                    val dayCourses = courses.filter { it.dayOfWeek == day }
                    dayCourses.forEach { course ->
                        val duration = (course.endPeriod - course.startPeriod + 1).coerceAtLeast(1)
                        val courseColor = runCatching { Color(course.colorHex.toColorInt()) }
                            .getOrDefault(SapphirePrimary)

                        Box(
                            modifier = Modifier
                                .padding(1.dp)
                                .offset(y = periodHeight * (course.startPeriod - 1))
                                .fillMaxWidth()
                                .height(periodHeight * duration)
                                .clip(RoundedCornerShape(8.dp))
                                .background(courseColor.copy(alpha = 0.2f))
                                .border(1.5.dp, courseColor.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                                .clickable { onCourseClick(course) }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = course.name,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        lineHeight = 13.sp
                                    ),
                                    fontWeight = FontWeight.Bold,
                                    color = courseColor,
                                    maxLines = duration * 2,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                                if (course.location.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = course.location,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
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
            10 -> "A"
            11 -> "B"
            12 -> "C"
            13 -> "D"
            14 -> "E"
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
