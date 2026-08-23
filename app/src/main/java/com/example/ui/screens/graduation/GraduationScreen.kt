package com.example.ui.screens.graduation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Course
import com.example.data.model.CourseCategory
import com.example.data.model.GraduationThreshold
import com.example.ui.components.CategoryCreditProgressBar
import com.example.ui.components.SectionHeader
import com.example.ui.screens.timetable.AddEditCourseDialog
import com.example.ui.screens.timetable.CourseDetailBottomSheet
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraduationScreen(
    viewModel: StudentViewModel,
    modifier: Modifier = Modifier
) {
    val plan by viewModel.graduationPlan.collectAsStateWithLifecycle()
    val auditSummary by viewModel.graduationAudit.collectAsStateWithLifecycle()
    val allCourses by viewModel.allCourses.collectAsStateWithLifecycle()
    val thresholds by viewModel.graduationThresholds.collectAsStateWithLifecycle()

    var showPlanDialog by remember { mutableStateOf(false) }
    var showAddThresholdDialog by remember { mutableStateOf(false) }
    var showEditCourseDialog by remember { mutableStateOf(false) }
    var editingCourse by remember { mutableStateOf<Course?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf<CourseCategory?>(null) }
    var selectedCourseDetail by remember { mutableStateOf<Course?>(null) }

    val animatedOverallProgress by animateFloatAsState(
        targetValue = (auditSummary.overallPercentage / 100f).coerceIn(0f, 1f),
        animationSpec = tween(700),
        label = "overall_progress"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "畢業學分審查",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${plan.department}・畢業審查檢核表",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalButton(
                    onClick = { showPlanDialog = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("edit_plan_button")
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("設定門檻")
                }
            }
        }

        // Main Graduation Gauge Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(SapphirePrimary.copy(alpha = 0.07f), TealSecondary.copy(alpha = 0.04f))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "總畢業學分達成率",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "已修通過 ${auditSummary.totalEarnedCredits.toInt()} / 目標 ${auditSummary.totalTargetCredits.toInt()} 學分",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            // Completion Badge
                            Badge(
                                containerColor = if (auditSummary.isEligibleToGraduate) EmeraldLight else SapphireLight,
                                contentColor = if (auditSummary.isEligibleToGraduate) EmeraldAccent else SapphireDark
                            ) {
                                Text(
                                    text = if (auditSummary.isEligibleToGraduate) "已達畢業標準" else "學分累積中",
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // Big Progress Bar & Percentage
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                LinearProgressIndicator(
                                    progress = { animatedOverallProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(12.dp)
                                        .clip(RoundedCornerShape(6.dp)),
                                    color = if (auditSummary.overallPercentage >= 100f) EmeraldAccent else SapphirePrimary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "尚缺 ${(auditSummary.totalTargetCredits - auditSummary.totalEarnedCredits).coerceAtLeast(0.0).toInt()} 學分",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (auditSummary.totalInProgressCredits > 0) {
                                        Text(
                                            text = "本學期修習中: +${auditSummary.totalInProgressCredits.toInt()} 學分",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TealSecondary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${auditSummary.overallPercentage.toInt()}%",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = SapphirePrimary
                            )
                        }
                    }
                }
            }
        }

        // Category Breakdown Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "學分分項進度",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // General
                    CategoryCreditProgressBar(
                        categoryName = "通識教育課程",
                        earnedCredits = auditSummary.generalSummary.earnedCredits,
                        targetCredits = auditSummary.generalSummary.targetCredits,
                        inProgressCredits = auditSummary.generalSummary.inProgressCredits,
                        accentColor = CourseCategory.GENERAL_EDU.badgeColor
                    )

                    // College Core
                    CategoryCreditProgressBar(
                        categoryName = "院共同課程",
                        earnedCredits = auditSummary.collegeCoreSummary.earnedCredits,
                        targetCredits = auditSummary.collegeCoreSummary.targetCredits,
                        inProgressCredits = auditSummary.collegeCoreSummary.inProgressCredits,
                        accentColor = CourseCategory.COLLEGE_CORE.badgeColor
                    )

                    // Basic Module
                    CategoryCreditProgressBar(
                        categoryName = "基礎模組",
                        earnedCredits = auditSummary.basicModuleSummary.earnedCredits,
                        targetCredits = auditSummary.basicModuleSummary.targetCredits,
                        inProgressCredits = auditSummary.basicModuleSummary.inProgressCredits,
                        accentColor = CourseCategory.BASIC_MODULE.badgeColor
                    )

                    // Core Module
                    CategoryCreditProgressBar(
                        categoryName = "核心模組",
                        earnedCredits = auditSummary.coreModuleSummary.earnedCredits,
                        targetCredits = auditSummary.coreModuleSummary.targetCredits,
                        inProgressCredits = auditSummary.coreModuleSummary.inProgressCredits,
                        accentColor = CourseCategory.CORE_MODULE.badgeColor
                    )

                    // Professional Module
                    CategoryCreditProgressBar(
                        categoryName = "專業模組",
                        earnedCredits = auditSummary.professionalModuleSummary.earnedCredits,
                        targetCredits = auditSummary.professionalModuleSummary.targetCredits,
                        inProgressCredits = auditSummary.professionalModuleSummary.inProgressCredits,
                        accentColor = CourseCategory.PROFESSIONAL_MODULE.badgeColor
                    )

                    // Free
                    CategoryCreditProgressBar(
                        categoryName = "自由選修",
                        earnedCredits = auditSummary.freeSummary.earnedCredits,
                        targetCredits = auditSummary.freeSummary.targetCredits,
                        inProgressCredits = auditSummary.freeSummary.inProgressCredits,
                        accentColor = CourseCategory.FREE_ELECTIVE.badgeColor
                    )
                }
            }
        }

        // Graduation Thresholds Checklist
        item {
            SectionHeader(
                title = "畢業門檻與專題檢核",
                subtitle = "已完成 ${auditSummary.thresholdsCompletedCount} / ${auditSummary.thresholdsTotalCount} 項",
                actionText = "新增項目",
                onActionClick = { showAddThresholdDialog = true }
            )
        }

        if (thresholds.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text(
                        text = "目前無自訂畢業門檻項目",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(thresholds) { threshold ->
                ThresholdItemCard(
                    threshold = threshold,
                    onToggle = { viewModel.toggleThreshold(threshold) },
                    onDelete = { viewModel.deleteThreshold(threshold) }
                )
            }
        }

        // Course Audit History & Filter Chips
        item {
            SectionHeader(
                title = "歷年修課審查清單",
                subtitle = "點擊課程可檢視或編輯成績"
            )
        }

        // Category Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategoryFilter == null,
                    onClick = { selectedCategoryFilter = null },
                    label = { Text("全部 (${allCourses.size})") }
                )
                CourseCategory.entries
                    .filter { it != CourseCategory.REQUIRED && it != CourseCategory.ELECTIVE && it != CourseCategory.PE }
                    .forEach { cat ->
                        FilterChip(
                            selected = selectedCategoryFilter == cat,
                            onClick = {
                                selectedCategoryFilter = if (selectedCategoryFilter == cat) null else cat
                            },
                            label = { Text(cat.label.removeSuffix("課程")) }
                        )
                    }
            }
        }

        val filteredCourses = if (selectedCategoryFilter != null) {
            allCourses.filter { it.category == selectedCategoryFilter }
        } else {
            allCourses
        }

        if (filteredCourses.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text(
                        text = "此類別尚無課程記錄",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(filteredCourses.sortedWith(compareByDescending<Course> { it.semester }.thenBy { it.name })) { course ->
                AuditCourseCard(
                    course = course,
                    minPassingScore = plan.minPassingScore,
                    onClick = { selectedCourseDetail = course }
                )
            }
        }
    }

    if (showPlanDialog) {
        GraduationPlanDialog(
            currentPlan = plan,
            onDismiss = { showPlanDialog = false },
            onSave = { updatedPlan ->
                viewModel.updateGraduationPlan(updatedPlan)
                showPlanDialog = false
            }
        )
    }

    if (showAddThresholdDialog) {
        AddThresholdDialog(
            onDismiss = { showAddThresholdDialog = false },
            onSave = { title, desc, note ->
                viewModel.addThreshold(
                    GraduationThreshold(
                        title = title,
                        description = desc,
                        proofNote = note
                    )
                )
                showAddThresholdDialog = false
            }
        )
    }

    if (showEditCourseDialog && editingCourse != null) {
        AddEditCourseDialog(
            initialCourse = editingCourse,
            defaultSemester = editingCourse?.semester ?: plan.currentSemester,
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
        CourseDetailBottomSheet(
            course = course,
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
private fun ThresholdItemCard(
    threshold: GraduationThreshold,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (threshold.isCompleted) EmeraldLight.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Checkbox(
                checked = threshold.isCompleted,
                onCheckedChange = { onToggle() }
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = threshold.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (threshold.isCompleted) EmeraldAccent else MaterialTheme.colorScheme.onSurface
                )
                if (threshold.description.isNotBlank()) {
                    Text(
                        text = threshold.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (threshold.proofNote.isNotBlank()) {
                    Text(
                        text = "備註：${threshold.proofNote}" + if (threshold.completedDate.isNotBlank()) " (${threshold.completedDate} 完成)" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (threshold.isCompleted) EmeraldAccent else SapphirePrimary
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "刪除門檻",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun AuditCourseCard(
    course: Course,
    minPassingScore: Double,
    onClick: () -> Unit
) {
    val isPassed = course.isCompleted || (course.score != null && course.score >= minPassingScore)
    val isInProgress = !isPassed && course.score == null && course.letterGrade == null

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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                    text = "${course.semester} 學期",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "${course.credits} 學分",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = SapphirePrimary
                )
                Badge(
                    containerColor = when {
                        isPassed -> EmeraldLight
                        isInProgress -> SapphireLight
                        else -> RoseLight
                    },
                    contentColor = when {
                        isPassed -> EmeraldAccent
                        isInProgress -> SapphireDark
                        else -> RoseAccent
                    }
                ) {
                    Text(
                        text = when {
                            isPassed -> if (course.score != null) "${course.score.toInt()}分 通過" else if (course.letterGrade != null) "${course.letterGrade} 通過" else "通過"
                            isInProgress -> "修習中"
                            else -> "未通過"
                        },
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
