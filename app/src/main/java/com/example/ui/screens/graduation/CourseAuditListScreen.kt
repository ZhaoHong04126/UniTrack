package com.example.ui.screens.graduation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.DefaultData
import com.example.data.model.Course
import com.example.data.model.CourseCategory
import com.example.ui.screens.timetable.AddEditCourseDialog
import com.example.ui.screens.timetable.CourseDetailBottomSheet
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.AmberLight
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.RoseAccent
import com.example.ui.theme.SapphireDark
import com.example.ui.theme.SapphireLight
import com.example.ui.viewmodel.StudentViewModel

private data class AuditCourseItem(
    val course: Course,
    val isRetake: Boolean,
    val originalSemester: String? = null
)

private fun isCoursePassed(course: Course, minPassingScore: Double = 60.0): Boolean {
    if (course.score != null && course.score >= minPassingScore) return true
    if (course.letterGrade in listOf("抵免", "通過", "免修")) return true
    if (course.isCompleted && course.letterGrade != "不通過" && (course.score == null || course.score >= minPassingScore)) return true
    if (course.letterGrade != null && course.letterGrade !in listOf("F", "E", "不通過")) {
        return true
    }
    return false
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseAuditListScreen(
    viewModel: StudentViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val plan by viewModel.graduationPlan.collectAsStateWithLifecycle()
    val allCourses by viewModel.allCourses.collectAsStateWithLifecycle()
    val allSemesters by viewModel.allSemesters.collectAsStateWithLifecycle()

    val existingSemesters = remember(allSemesters, allCourses) {
        (allCourses.filter { !it.isTutorial }.map { it.semester } + allSemesters).distinct()
    }

    var selectedSemesterFilter by remember { mutableStateOf<String?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf<CourseCategory?>(null) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var selectedCourseDetail by remember { mutableStateOf<Course?>(null) }
    var editingCourse by remember { mutableStateOf<Course?>(null) }
    var showEditCourseDialog by remember { mutableStateOf(false) }

    val auditCourseItems = remember(allCourses, plan.minPassingScore, selectedSemesterFilter, selectedCategoryFilter) {
        val nonTutorialCourses = allCourses.filter { !it.isTutorial }
        val groupedByName = nonTutorialCourses.groupBy { it.name.trim() }

        val baseItems = if (selectedSemesterFilter == null) {
            groupedByName.map { (_, coursesInGroup) ->
                val semList = coursesInGroup.map { it.semester }.distinct()
                val isRetake = semList.size > 1
                val primaryCourse = if (!isRetake) {
                    coursesInGroup.maxByOrNull { it.credits } ?: coursesInGroup.first()
                } else {
                    coursesInGroup.sortedWith(
                        compareByDescending<Course> { isCoursePassed(it, plan.minPassingScore) }
                            .thenByDescending { DefaultData.parseSemesterWeight(it.semester) }
                            .thenByDescending { it.score ?: 0.0 }
                    ).first()
                }
                val earlierSemester = if (isRetake) {
                    coursesInGroup.firstOrNull { it.semester != primaryCourse.semester }?.semester
                } else null
                AuditCourseItem(course = primaryCourse, isRetake = isRetake, originalSemester = earlierSemester)
            }
        } else {
            nonTutorialCourses.filter { it.semester == selectedSemesterFilter }
                .groupBy { it.name.trim() }
                .values
                .map { group ->
                    val course = group.maxByOrNull { it.credits } ?: group.first()
                    val allSemList = (groupedByName[course.name.trim()] ?: listOf(course)).map { it.semester }.distinct()
                    val isRetake = allSemList.size > 1
                    val earlierSemester = if (isRetake) {
                        allSemList.firstOrNull { it != course.semester }
                    } else null
                    AuditCourseItem(course = course, isRetake = isRetake, originalSemester = earlierSemester)
                }
        }

        baseItems.filter { item ->
            selectedCategoryFilter == null || item.course.category == selectedCategoryFilter
        }.sortedWith(
            compareBy(
                { DefaultData.parseSemesterWeight(it.course.semester) },
                { it.course.dayOfWeek },
                { it.course.startPeriod }
            )
        )
    }

    val filterButtonLabel = buildString {
        if (selectedSemesterFilter != null) append(selectedSemesterFilter)
        if (selectedCategoryFilter != null) {
            if (isNotEmpty()) append("・")
            append(selectedCategoryFilter!!.label.removeSuffix("課程"))
        }
        if (isEmpty()) append("篩選")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "歷年修課審查清單",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "點擊課程可檢視或編輯成績",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                windowInsets = WindowInsets(0.dp),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    OutlinedButton(
                        onClick = { showFilterDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = filterButtonLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
        ) {
            if (auditCourseItems.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (selectedCategoryFilter != null) "此類別尚無課程記錄" else "目前尚無任何課程記錄",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(auditCourseItems, key = { it.course.id }) { item ->
                    CourseAuditItemCard(
                        item = item,
                        minPassingScore = plan.minPassingScore,
                        onClick = { selectedCourseDetail = item.course }
                    )
                }
            }
        }
    }

    // Filter Dialog
    if (showFilterDialog) {
        val categories = CourseCategory.entries
            .filter { it != CourseCategory.REQUIRED && it != CourseCategory.ELECTIVE && it != CourseCategory.PE }

        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text("篩選修課清單", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Semester Section
                    Text(
                        text = "依學期篩選",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )

                    // 全部學期
                    val nonTutorialCourses = remember(allCourses) { allCourses.filter { !it.isTutorial } }
                    val totalAuditedCount = remember(nonTutorialCourses) { nonTutorialCourses.groupBy { it.name.trim() }.size }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedSemesterFilter = null }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "全部學期 ($totalAuditedCount)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selectedSemesterFilter == null) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedSemesterFilter == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        if (selectedSemesterFilter == null) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                    }

                    existingSemesters.forEach { sem ->
                        val count = nonTutorialCourses.filter { it.semester == sem }.groupBy { it.name.trim() }.size
                        val isSelected = selectedSemesterFilter == sem
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedSemesterFilter = sem }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "$sem 學期 ($count)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                    // Category Section
                    Text(
                        text = "依學分屬性篩選",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )

                    // 全部屬性
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedCategoryFilter = null }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "全部屬性",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selectedCategoryFilter == null) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedCategoryFilter == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        if (selectedCategoryFilter == null) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                    }

                    categories.forEach { cat ->
                        val count = nonTutorialCourses.filter { it.category == cat }.groupBy { it.name.trim() }.size
                        val isSelected = selectedCategoryFilter == cat
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedCategoryFilter = cat }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(cat.badgeColor)
                                )
                                Text(
                                    text = "${cat.label} ($count)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showFilterDialog = false }) {
                    Text("確定")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        selectedSemesterFilter = null
                        selectedCategoryFilter = null
                    }
                ) {
                    Text("重設全部")
                }
            }
        )
    }

    if (showEditCourseDialog && editingCourse != null) {
        AddEditCourseDialog(
            initialCourse = editingCourse,
            defaultSemester = editingCourse?.semester ?: plan.currentSemester,
            allCourses = allCourses,
            plan = plan,
            onDismiss = {
                showEditCourseDialog = false
                editingCourse = null
            },
            onSave = { updatedCourse ->
                viewModel.updateCourse(updatedCourse)
                showEditCourseDialog = false
                editingCourse = null
            }
        )
    }

    selectedCourseDetail?.let { course ->
        val attendanceMap = remember(course.id) {
            viewModel.getCourseAttendance(course.id)
        }
        val notesList = remember(course.id) {
            viewModel.getCourseNotes(course.id)
        }
        CourseDetailBottomSheet(
            course = course,
            semesterStartDate = viewModel.getSemesterStartDate(course.semester),
            totalWeeks = viewModel.getSemesterTotalWeeks(course.semester),
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
                editingCourse = course
                selectedCourseDetail = null
                showEditCourseDialog = true
            },
            onDelete = {
                viewModel.deleteCourse(course)
                selectedCourseDetail = null
            }
        )
    }
}

@Composable
private fun CourseAuditItemCard(
    item: AuditCourseItem,
    minPassingScore: Double = 60.0,
    onClick: () -> Unit
) {
    val course = item.course
    val isRetake = item.isRetake

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
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                        Text(text = "${course.category.shortLabel}・${course.requirementType.shortLabel}")
                    }
                    if (isRetake) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = AmberLight
                        ) {
                            Text(
                                text = "重修",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = AmberAccent,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                            )
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${course.semester} 學期",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isRetake) {
                        Text(
                            text = "· 重修",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = AmberAccent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${course.credits} 學分",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                val isPassed = isCoursePassed(course, minPassingScore)

                if (course.score != null) {
                    val scoreInt = course.score.toInt()
                    val statusText = if (isPassed) {
                        if (isRetake) "${scoreInt}分 重修通過" else "${scoreInt}分 通過"
                    } else {
                        "${scoreInt}分 不通過"
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isPassed) EmeraldAccent else RoseAccent,
                        fontWeight = FontWeight.Bold
                    )
                } else if (course.letterGrade != null) {
                    val statusText = when (val grade = course.letterGrade) {
                        "通過" -> if (isRetake) "重修通過" else "通過"
                        "不通過" -> "不通過"
                        "抵免", "免修" -> grade
                        "F", "E" -> "$grade (不通過)"
                        else -> if (isPassed) (if (isRetake) "$grade 重修通過" else "$grade 通過") else "$grade 不通過"
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isPassed) EmeraldAccent else RoseAccent,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Badge(
                        containerColor = if (isRetake) AmberLight else SapphireLight,
                        contentColor = if (isRetake) AmberAccent else SapphireDark
                    ) {
                        Text(
                            text = if (isRetake) "重修中" else "修習中",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}
