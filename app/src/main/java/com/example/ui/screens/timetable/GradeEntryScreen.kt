package com.example.ui.screens.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
    modifier: Modifier = Modifier
) {
    val selectedSemester by viewModel.selectedSemester.collectAsStateWithLifecycle()
    val allSemesters by viewModel.allSemesters.collectAsStateWithLifecycle()
    val courses by viewModel.currentSemesterCourses.collectAsStateWithLifecycle()
    val plan by viewModel.graduationPlan.collectAsStateWithLifecycle()
    val locale = LocalConfiguration.current.locales[0]

    var showSemesterManageDialog by remember { mutableStateOf(false) }

    // Calculate semester average score & earned credits
    val gradedCourses = courses.filter { it.score != null }
    val totalGradedCredits = gradedCourses.sumOf { it.credits }
    val weightedScoreSum = gradedCourses.sumOf { (it.score ?: 0.0) * it.credits }
    val semesterAverageScore = if (totalGradedCredits > 0) weightedScoreSum / totalGradedCredits else 0.0
    val totalSemesterCredits = courses.sumOf { it.credits }
    val passedCredits = courses.filter { it.score != null && it.score >= plan.minPassingScore }.sumOf { it.credits }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "學期成績登錄",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
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
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
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
                                text = "${gradedCourses.size} / ${courses.size}",
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

            if (courses.isEmpty()) {
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
                items(courses.sortedWith(compareBy({ it.dayOfWeek }, { it.startPeriod })), key = { it.id }) { course ->
                    GradeEntryCourseCard(
                        course = course,
                        onSaveScore = { newScore ->
                            val isCompleted = newScore != null && newScore >= plan.minPassingScore
                            viewModel.updateCourse(
                                course.copy(
                                    score = newScore,
                                    isCompleted = isCompleted
                                )
                            )
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
            onDismiss = { showSemesterManageDialog = false }
        )
    }
}

@Composable
private fun GradeEntryCourseCard(
    course: Course,
    onSaveScore: (Double?) -> Unit
) {
    var scoreInput by remember(course.score) {
        mutableStateOf(course.score?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "")
    }
    val focusManager = LocalFocusManager.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
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
            // Course Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = course.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Badge(
                        containerColor = course.category.badgeColor.copy(alpha = 0.15f),
                        contentColor = course.category.badgeColor
                    ) {
                        Text(text = "${course.category.shortLabel}・${course.requirementType.shortLabel}")
                    }
                }
                Text(
                    text = "${course.credits} 學分" + if (course.teacher.isNotBlank()) "・${course.teacher}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Score Input & Action
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = scoreInput,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.toDoubleOrNull() != null) {
                            scoreInput = input
                        }
                    },
                    placeholder = { Text("0~100", style = MaterialTheme.typography.labelSmall) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val parsed = scoreInput.toDoubleOrNull()
                            onSaveScore(parsed)
                            focusManager.clearFocus()
                        }
                    ),
                    singleLine = true,
                    modifier = Modifier.width(84.dp)
                )

                // Save or Clear Button
                IconButton(
                    onClick = {
                        val parsed = scoreInput.toDoubleOrNull()
                        onSaveScore(parsed)
                        focusManager.clearFocus()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "儲存成績",
                        tint = EmeraldAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (course.score != null) {
                    IconButton(
                        onClick = {
                            scoreInput = ""
                            onSaveScore(null)
                            focusManager.clearFocus()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "清除成績",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
