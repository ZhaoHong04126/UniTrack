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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Course
import com.example.data.model.CourseCategory
import com.example.ui.screens.timetable.AddEditCourseDialog
import com.example.ui.screens.timetable.CourseDetailBottomSheet
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.SapphireDark
import com.example.ui.theme.SapphireLight
import com.example.ui.viewmodel.StudentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseAuditListScreen(
    viewModel: StudentViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val plan by viewModel.graduationPlan.collectAsStateWithLifecycle()
    val allCourses by viewModel.allCourses.collectAsStateWithLifecycle()

    var selectedCategoryFilter by remember { mutableStateOf<CourseCategory?>(null) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var selectedCourseDetail by remember { mutableStateOf<Course?>(null) }
    var editingCourse by remember { mutableStateOf<Course?>(null) }
    var showEditCourseDialog by remember { mutableStateOf(false) }

    val filteredCourses = if (selectedCategoryFilter != null) {
        allCourses.filter { it.category == selectedCategoryFilter }
    } else {
        allCourses
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
                            text = if (selectedCategoryFilter == null) "篩選" else selectedCategoryFilter!!.label.removeSuffix("課程"),
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
            if (filteredCourses.isEmpty()) {
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
                items(filteredCourses.sortedWith(compareBy({ it.semester }, { it.dayOfWeek }, { it.startPeriod }))) { course ->
                    CourseAuditItemCard(
                        course = course,
                        onClick = { selectedCourseDetail = course }
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
            title = { Text("篩選學分屬性", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Option: 全部
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                selectedCategoryFilter = null
                                showFilterDialog = false
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "全部 (${allCourses.size})",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (selectedCategoryFilter == null) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedCategoryFilter == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        if (selectedCategoryFilter == null) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    categories.forEach { cat ->
                        val count = allCourses.count { it.category == cat }
                        val isSelected = selectedCategoryFilter == cat
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedCategoryFilter = cat
                                    showFilterDialog = false
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
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
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFilterDialog = false }) {
                    Text("關閉")
                }
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
private fun CourseAuditItemCard(
    course: Course,
    onClick: () -> Unit
) {
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
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                }
                Text(
                    text = "${course.semester} 學期",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${course.credits} 學分",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (course.score != null) {
                    Text(
                        text = "${course.score.toInt()}分 通過",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldAccent,
                        fontWeight = FontWeight.Bold
                    )
                } else if (course.letterGrade != null) {
                    Text(
                        text = "${course.letterGrade} 通過",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldAccent,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Badge(
                        containerColor = SapphireLight,
                        contentColor = SapphireDark
                    ) {
                        Text("修習中", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
