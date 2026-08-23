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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
    modifier: Modifier = Modifier
) {
    val selectedSemester by viewModel.selectedSemester.collectAsStateWithLifecycle()
    val allSemesters by viewModel.allSemesters.collectAsStateWithLifecycle()
    val courses by viewModel.currentSemesterCourses.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingCourse by remember { mutableStateOf<Course?>(null) }
    var selectedCourseDetail by remember { mutableStateOf<Course?>(null) }
    var showNewSemesterDialog by remember { mutableStateOf(false) }
    var newYearInput by remember { mutableStateOf("114") }
    var selectedTerm by remember { mutableStateOf("上學期") }
    var termDropdownExpanded by remember { mutableStateOf(false) }
    var isGridView by remember { mutableStateOf(true) }
    var showWeekend by remember { mutableStateOf(true) }

    var semesterMenuExpanded by remember { mutableStateOf(false) }

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

    Scaffold(
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Semester Switcher & View Mode Toggles
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Semester selector button
                Box {
                    OutlinedButton(
                        onClick = { semesterMenuExpanded = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "$selectedSemester 學期",
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

                    DropdownMenu(
                        expanded = semesterMenuExpanded,
                        onDismissRequest = { semesterMenuExpanded = false }
                    ) {
                        allSemesters.forEach { sem ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "$sem 學期",
                                        fontWeight = if (sem == selectedSemester) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    viewModel.setSelectedSemester(sem)
                                    semesterMenuExpanded = false
                                }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("新增學期...")
                                }
                            },
                            onClick = {
                                semesterMenuExpanded = false
                                showNewSemesterDialog = true
                            }
                        )
                    }
                }

                // Add Course Button (Relocated from FAB)
                Button(
                    onClick = {
                        editingCourse = null
                        showAddDialog = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SapphirePrimary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("add_course_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "新增課程",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
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

    if (showNewSemesterDialog) {
        val termOptions = listOf("上學期", "下學期", "暑期")

        AlertDialog(
            onDismissRequest = { showNewSemesterDialog = false },
            title = { Text("新增學期", fontWeight = FontWeight.Bold) },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 學年
                    OutlinedTextField(
                        value = newYearInput,
                        onValueChange = { str ->
                            if (str.all { it.isDigit() } && str.length <= 4) {
                                newYearInput = str
                            }
                        },
                        label = { Text("學年") },
                        placeholder = { Text("例如：114") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    // 學期
                    ExposedDropdownMenuBox(
                        expanded = termDropdownExpanded,
                        onExpandedChange = { termDropdownExpanded = !termDropdownExpanded },
                        modifier = Modifier.weight(1.2f)
                    ) {
                        OutlinedTextField(
                            value = selectedTerm,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("學期") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = termDropdownExpanded) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = termDropdownExpanded,
                            onDismissRequest = { termDropdownExpanded = false }
                        ) {
                            termOptions.forEach { term ->
                                DropdownMenuItem(
                                    text = { Text(term) },
                                    onClick = {
                                        selectedTerm = term
                                        termDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newYearInput.isNotBlank()) {
                            val termSuffix = when (selectedTerm) {
                                "上學期" -> "1"
                                "下學期" -> "2"
                                "暑期" -> "暑"
                                else -> "1"
                            }
                            val semesterCode = "${newYearInput.trim()}-$termSuffix"
                            viewModel.setSelectedSemester(semesterCode)
                            showNewSemesterDialog = false
                        }
                    },
                    enabled = newYearInput.isNotBlank()
                ) {
                    Text("確定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewSemesterDialog = false }) {
                    Text("取消")
                }
            }
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
                .padding(bottom = 80.dp)
        ) {
            // Period Number Column
            Column(modifier = Modifier.width(40.dp)) {
                for (period in 1..maxPeriod) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(periodHeight)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f))
                            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$period",
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
                    text = "$dayName 第 ${course.startPeriod} ~ ${course.endPeriod} 節" +
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
