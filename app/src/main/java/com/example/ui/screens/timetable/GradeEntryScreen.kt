package com.example.ui.screens.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Course
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradeEntryScreen(
    viewModel: StudentViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToGraduation: () -> Unit = {}
) {
    val selectedSemester by viewModel.selectedSemester.collectAsStateWithLifecycle()
    val allSemesters by viewModel.allSemesters.collectAsStateWithLifecycle()
    val courses by viewModel.currentSemesterCourses.collectAsStateWithLifecycle()
    val plan by viewModel.graduationPlan.collectAsStateWithLifecycle()
    val locale = LocalConfiguration.current.locales[0]
    val focusManager = LocalFocusManager.current

    var showSemesterManageDialog by remember { mutableStateOf(false) }

    // Map of courseId -> Pair<Double? (score), String? (letterGrade)>
    val draftGrades = remember(courses) {
        mutableStateMapOf<Long, Pair<Double?, String?>>().apply {
            courses.forEach { c ->
                put(c.id, Pair(c.score, c.letterGrade))
            }
        }
    }

    val displayCourses = remember(courses, draftGrades.toMap()) {
        courses.map { c ->
            val draft = draftGrades[c.id] ?: Pair(c.score, c.letterGrade)
            val isCompleted = when {
                draft.first != null -> draft.first!! >= plan.minPassingScore
                draft.second in listOf("抵免", "通過", "免修") -> true
                draft.second == "不通過" -> false
                else -> false
            }
            c.copy(
                score = draft.first,
                letterGrade = draft.second,
                isCompleted = isCompleted
            )
        }
    }

    val changedCourses = remember(courses, displayCourses) {
        displayCourses.filter { displayCourse ->
            val original = courses.firstOrNull { it.id == displayCourse.id }
            original != null && (original.score != displayCourse.score || original.letterGrade != displayCourse.letterGrade)
        }
    }
    val hasChanges = changedCourses.isNotEmpty()

    // Calculate semester average score & earned credits dynamically
    val scoredCourses = displayCourses.filter { it.score != null }
    val totalGradedCredits = scoredCourses.sumOf { it.credits }
    val weightedScoreSum = scoredCourses.sumOf { (it.score ?: 0.0) * it.credits }
    val semesterAverageScore = if (totalGradedCredits > 0) weightedScoreSum / totalGradedCredits else 0.0
    val totalSemesterCredits = displayCourses.sumOf { it.credits }
    val passedCredits = displayCourses.filter {
        (it.score != null && it.score >= plan.minPassingScore) ||
        (it.letterGrade in listOf("抵免", "通過", "免修")) ||
        (it.isCompleted && it.letterGrade != "不通過")
    }.sumOf { it.credits }
    val loggedCount = displayCourses.count { it.score != null || it.letterGrade != null }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "學期成績登錄",
                        fontWeight = FontWeight.Bold
                    )
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
                    IconButton(
                        onClick = onNavigateToGraduation
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = "畢業審查"
                        )
                    }
                    Box(modifier = Modifier.padding(end = 8.dp)) {
                        OutlinedButton(
                            onClick = { showSemesterManageDialog = true },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "$selectedSemester 學期",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding()
                ) {
                    Button(
                        onClick = {
                            if (hasChanges) {
                                viewModel.saveCourseGradesBatch(
                                    changedCourses = changedCourses,
                                    semester = selectedSemester,
                                    originalCourses = courses
                                )
                                focusManager.clearFocus()
                            }
                        },
                        enabled = hasChanges,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SapphirePrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Text(
                            text = if (hasChanges) "儲存成績變更 (${changedCourses.size} 筆)" else "儲存",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
        ) {
            // Semester Stats Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (semesterAverageScore > 0) String.format(locale, "%.1f", semesterAverageScore) else "—",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = TealSecondary
                            )
                            Text(
                                text = "本學期平均分",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(32.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${passedCredits.toInt()} / ${totalSemesterCredits.toInt()}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = SapphirePrimary
                            )
                            Text(
                                text = "獲得 / 總學分",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(32.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$loggedCount / ${displayCourses.size}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = EmeraldAccent
                            )
                            Text(
                                text = "已登錄門數",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (displayCourses.isEmpty()) {
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
                                text = "本學期尚無課程記錄，請先至課表新增課程",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(displayCourses.sortedWith(compareBy({ it.dayOfWeek }, { it.startPeriod })), key = { it.id }) { course ->
                    val draft = draftGrades[course.id] ?: Pair(course.score, course.letterGrade)
                    GradeEntryCourseCard(
                        course = course,
                        draftScore = draft.first,
                        draftLetterGrade = draft.second,
                        onScoreChanged = { newScore ->
                            draftGrades[course.id] = Pair(newScore, null)
                        },
                        onNonGradedChanged = { status ->
                            draftGrades[course.id] = Pair(null, status)
                        },
                        onClear = {
                            draftGrades[course.id] = Pair(null, null)
                        }
                    )
                }
            }
        }
    }

    if (showSemesterManageDialog) {
        SemesterManageDialog(
            selectedSemester = selectedSemester,
            primarySemester = plan.currentSemester,
            allSemesters = allSemesters,
            admissionSemester = plan.admissionSemester,
            onSelectSemester = { sem ->
                viewModel.setSelectedSemester(sem)
            },
            onSetPrimarySemester = { sem ->
                viewModel.setPrimarySemester(sem)
            },
            onDeleteSemester = { sem ->
                viewModel.deleteSemester(sem)
            },
            onDismiss = { showSemesterManageDialog = false }
        )
    }
}

@Composable
private fun GradeEntryCourseCard(
    course: Course,
    draftScore: Double?,
    draftLetterGrade: String?,
    onScoreChanged: (Double?) -> Unit,
    onNonGradedChanged: (String) -> Unit,
    onClear: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var scoreInputText by remember(draftScore) {
        mutableStateOf(draftScore?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "")
    }
    val focusManager = LocalFocusManager.current
    val isNonGraded = draftLetterGrade in listOf("抵免", "通過", "不通過", "免修")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Course Info (Left)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = course.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = course.category.badgeColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${course.category.shortLabel}・${course.requirementType.shortLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = course.category.badgeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "${course.credits} 學分" + if (course.teacher.isNotBlank()) "・${course.teacher}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Grade Controls (Right)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isNonGraded) {
                    val currentStatus = draftLetterGrade ?: "通過"
                    val (bgColor, textColor) = when (currentStatus) {
                        "通過" -> EmeraldAccent to Color.White
                        "不通過" -> RoseAccent to Color.White
                        "抵免" -> PurpleAccent to Color.White
                        "免修" -> TealSecondary to Color.White
                        else -> MaterialTheme.colorScheme.primary to Color.White
                    }

                    Box {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = bgColor,
                            modifier = Modifier
                                .height(38.dp)
                                .clickable { showMenu = true }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = currentStatus,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "切換狀態",
                                    tint = textColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            listOf("抵免", "通過", "不通過", "免修").forEach { opt ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = opt,
                                            fontWeight = if (draftLetterGrade == opt) FontWeight.Bold else FontWeight.Normal,
                                            color = when (opt) {
                                                "通過" -> EmeraldAccent
                                                "不通過" -> RoseAccent
                                                "抵免" -> PurpleAccent
                                                "免修" -> TealSecondary
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    },
                                    onClick = {
                                        onNonGradedChanged(opt)
                                        showMenu = false
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("🔢 改為填寫分數 (0~100)") },
                                onClick = {
                                    onClear()
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("✕ 清除紀錄", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    onClear()
                                    showMenu = false
                                }
                            )
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = scoreInputText,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.toDoubleOrNull() != null) {
                                scoreInputText = input
                                onScoreChanged(input.toDoubleOrNull())
                            }
                        },
                        placeholder = { Text("0~100", style = MaterialTheme.typography.labelSmall) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                            }
                        ),
                        singleLine = true,
                        modifier = Modifier.width(84.dp)
                    )

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "不採成績選項",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            Text(
                                text = "不採計成績選項",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                            listOf("抵免", "通過", "不通過", "免修").forEach { opt ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = opt,
                                            color = when (opt) {
                                                "通過" -> EmeraldAccent
                                                "不通過" -> RoseAccent
                                                "抵免" -> PurpleAccent
                                                "免修" -> TealSecondary
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    },
                                    onClick = {
                                        onNonGradedChanged(opt)
                                        showMenu = false
                                    }
                                )
                            }
                            if (draftScore != null) {
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("✕ 清除成績", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        scoreInputText = ""
                                        onClear()
                                        showMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
