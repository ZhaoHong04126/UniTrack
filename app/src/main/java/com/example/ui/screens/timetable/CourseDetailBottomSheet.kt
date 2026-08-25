package com.example.ui.screens.timetable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.example.data.model.Course
import com.example.data.model.CourseNote
import com.example.data.model.GeneralEduSubtype
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.RoseAccent
import com.example.ui.theme.SapphirePrimary
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

enum class CourseDetailTab(val title: String) {
    ATTENDANCE("出席"),
    NOTES("筆記"),
    INFO("資訊")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailBottomSheet(
    course: Course,
    semesterStartDate: String = "",
    totalWeeks: Int = 18,
    attendanceMap: Map<Int, String> = emptyMap(),
    notesList: List<CourseNote> = emptyList(),
    onUpdateAttendance: (week: Int, status: String) -> Unit = { _, _ -> },
    onAddNote: (content: String, category: String, week: Int?) -> Unit = { _, _, _ -> },
    onDeleteNote: (noteId: String) -> Unit = {},
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showConfirmDelete by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(CourseDetailTab.ATTENDANCE) }
    var showNoteComposer by remember { mutableStateOf(false) }

    val courseColor = runCatching { Color(course.colorHex.toColorInt()) }
        .getOrDefault(SapphirePrimary)

    val days = listOf("星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日")
    val dayName = days.getOrElse(course.dayOfWeek - 1) { "星期一" }
    val dayShort = listOf("一", "二", "三", "四", "五", "六", "日").getOrElse(course.dayOfWeek - 1) { "一" }

    // Attendance stats
    val presentCount = attendanceMap.values.count { it == "出席" }
    val lateCount = attendanceMap.values.count { it == "遲到" }
    val absentCount = attendanceMap.values.count { it == "曠課" }
    val leaveCount = attendanceMap.values.count { it == "請假" }

    if (showConfirmDelete) {
        AlertDialog(
            onDismissRequest = { showConfirmDelete = false },
            title = {
                Text("確認刪除課程", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("確定要刪除「${course.name}」嗎？此動作將同時自課表與歷年紀錄中移除。")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDelete = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseAccent)
                ) {
                    Text("確定刪除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDelete = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showNoteComposer) {
        NoteComposerBottomSheet(
            course = course,
            startDateStr = semesterStartDate,
            totalWeeks = totalWeeks,
            onDismiss = { showNoteComposer = false },
            onSave = { content, category, week ->
                onAddNote(content, category, week)
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Name, Colored badge, Subtitle info, Action buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(courseColor)
                        )
                        Text(
                            text = course.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Action buttons in top right
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.clickable { onEdit() }
                        ) {
                            Text(
                                text = "編輯",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = RoseAccent.copy(alpha = 0.12f),
                            modifier = Modifier.clickable { showConfirmDelete = true }
                        ) {
                            Text(
                                text = "刪除",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = RoseAccent
                            )
                        }
                    }
                }

                // Subtitle Info
                val teacherText = course.teacher.ifBlank { "授課教師未定" }
                val locationText = course.location.ifBlank { "教室未定" }
                val formatPeriodCode = { p: Int ->
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
                val timeStr = if (course.startTime.isNotBlank() && course.endTime.isNotBlank()) {
                    "${course.startTime} - ${course.endTime}"
                } else {
                    "第 ${formatPeriodCode(course.startPeriod)} ~ ${formatPeriodCode(course.endPeriod)} 節"
                }

                Text(
                    text = "$teacherText · ${course.credits} 學分",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = dayShort,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "$timeStr · $locationText",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Tab Bar with SecondaryTabRow (出席 | 筆記 | 資訊)
            SecondaryTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = Color.Transparent,
                divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)) }
            ) {
                CourseDetailTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    Tab(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        text = {
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    CourseDetailTab.ATTENDANCE -> {
                        AttendanceTabView(
                            course = course,
                            startDateStr = semesterStartDate,
                            totalWeeks = totalWeeks,
                            attendanceMap = attendanceMap,
                            presentCount = presentCount,
                            lateCount = lateCount,
                            absentCount = absentCount,
                            leaveCount = leaveCount,
                            onUpdateAttendance = onUpdateAttendance
                        )
                    }
                    CourseDetailTab.NOTES -> {
                        NotesTabView(
                            notesList = notesList,
                            legacyNotes = course.notes,
                            onOpenComposer = { showNoteComposer = true },
                            onDeleteNote = onDeleteNote
                        )
                    }
                    CourseDetailTab.INFO -> {
                        InfoTabView(
                            course = course,
                            dayName = dayName
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tab 1: 出席紀錄
 */
@Composable
private fun AttendanceTabView(
    course: Course,
    startDateStr: String,
    totalWeeks: Int,
    attendanceMap: Map<Int, String>,
    presentCount: Int,
    lateCount: Int,
    absentCount: Int,
    leaveCount: Int,
    onUpdateAttendance: (week: Int, status: String) -> Unit
) {
    val today = LocalDate.now()
    val parsedStart = try {
        val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
        LocalDate.parse(startDateStr, formatter)
    } catch (_: Exception) {
        today
    }

    val isSemesterStarted = !today.isBefore(parsedStart)

    if (!isSemesterStarted && startDateStr.isNotBlank()) {
        val daysUntilStart = java.time.temporal.ChronoUnit.DAYS.between(today, parsedStart)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.EventBusy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "尚未開學",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (daysUntilStart > 0) "距離開學還有 $daysUntilStart 天（開學日：$startDateStr）\n開學後將自動開放出缺席紀錄功能。"
                           else "目前尚未到學期開學日\n開學後將自動開放出缺席紀錄功能。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Summary 4-Box Stats
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AttendanceStatItem(count = presentCount, label = "出席", color = EmeraldAccent)
                VerticalDivider(modifier = Modifier.height(30.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                AttendanceStatItem(count = lateCount, label = "遲到", color = Color(0xFFF59E0B))
                VerticalDivider(modifier = Modifier.height(30.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                AttendanceStatItem(count = absentCount, label = "曠課", color = RoseAccent)
                VerticalDivider(modifier = Modifier.height(30.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                AttendanceStatItem(count = leaveCount, label = "請假", color = Color(0xFF3B82F6))
            }
        }

        // Section Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "各堂紀錄",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "點按修改",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Weekly Session Items
        val dayChinese = listOf("一", "二", "三", "四", "五", "六", "日").getOrElse(course.dayOfWeek - 1) { "一" }
        val parsedStart = try {
            val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
            LocalDate.parse(startDateStr, formatter)
        } catch (_: Exception) {
            LocalDate.now()
        }
        val mondayOfFirstWeek = parsedStart.minusDays((parsedStart.dayOfWeek.value - 1).toLong())

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val statusOptions = listOf("出席", "遲到", "曠課", "請假", "停課")

            for (week in 1..totalWeeks) {
                val isRelevant = if (course.repeatMode == "每週" || course.repeatWeeks == "1-18" || course.repeatWeeks.isBlank()) {
                    true
                } else if (course.repeatMode == "單週") {
                    week % 2 != 0
                } else if (course.repeatMode == "雙週") {
                    week % 2 == 0
                } else {
                    val weeks = course.repeatWeeks.split(",").mapNotNull { it.trim().toIntOrNull() }
                    week in weeks
                }

                if (isRelevant) {
                    val currentStatus = attendanceMap[week] ?: ""
                    val mondayOfWeek = mondayOfFirstWeek.plusWeeks((week - 1).toLong())
                    val courseDate = mondayOfWeek.plusDays((course.dayOfWeek - 1).toLong())
                    val dateFormatted = "${courseDate.monthValue}月${courseDate.dayOfMonth}日 ($dayChinese)"

                    val borderColor: Color = when (currentStatus) {
                        "出席" -> EmeraldAccent.copy(alpha = 0.35f)
                        "遲到" -> Color(0xFFF59E0B).copy(alpha = 0.35f)
                        "曠課" -> RoseAccent.copy(alpha = 0.35f)
                        "請假" -> Color(0xFF3B82F6).copy(alpha = 0.35f)
                        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        border = BorderStroke(width = 1.dp, color = borderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dateFormatted,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "第 $week 週",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Status selection pills
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                statusOptions.forEach { status ->
                                    val isSelected = currentStatus == status
                                    val activeColor: Color = when (status) {
                                        "出席" -> EmeraldAccent
                                        "遲到" -> Color(0xFFF59E0B)
                                        "曠課" -> RoseAccent
                                        "請假" -> Color(0xFF3B82F6)
                                        else -> Color(0xFF64748B)
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) activeColor else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = if (isSelected) activeColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        ),
                                        modifier = Modifier.clickable {
                                            onUpdateAttendance(week, status)
                                        }
                                    ) {
                                        Text(
                                            text = status,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun AttendanceStatItem(
    count: Int,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (count > 0) color else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Tab 2: 筆記 (乾淨清單與空狀態，點擊後開啟獨立專屬頁面)
 */
@Composable
private fun NotesTabView(
    notesList: List<CourseNote>,
    legacyNotes: String,
    onOpenComposer: () -> Unit,
    onDeleteNote: (noteId: String) -> Unit
) {
    var filterCategory by remember { mutableStateOf("全部") }

    // Combine legacy note if exists and list is empty
    val allNotes = remember(notesList, legacyNotes) {
        if (notesList.isEmpty() && legacyNotes.isNotBlank()) {
            listOf(CourseNote(id = "legacy", category = "一般", content = legacyNotes, timestamp = System.currentTimeMillis()))
        } else {
            notesList
        }
    }

    if (allNotes.isEmpty()) {
        // Empty State matching reference screenshot
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                // Circle with pencil icon
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "還沒有筆記",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "每堂課的筆記和出缺勤會一起整理。\n為今天的課留下第一筆紀錄吧。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onOpenComposer,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(48.dp)
                ) {
                    Text(
                        text = "寫下第一則筆記",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    } else {
        // List of notes + Category Filters + Add button
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("全部", "一般", "作業", "考試", "公告", "重點").forEach { cat ->
                    val isSelected = filterCategory == cat
                    val count = if (cat == "全部") allNotes.size else allNotes.count { it.category == cat }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.clickable { filterCategory = cat }
                    ) {
                        Text(
                            text = if (count > 0) "$cat $count" else cat,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Filtered list
            val filteredNotes = remember(allNotes, filterCategory) {
                if (filterCategory == "全部") allNotes else allNotes.filter { it.category == filterCategory }
            }

            val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }

            filteredNotes.forEach { note ->
                val tagColor = when (note.category) {
                    "作業" -> Color(0xFFD97706)
                    "考試" -> RoseAccent
                    "公告" -> Color(0xFF6366F1)
                    "重點" -> Color(0xFF0D9488)
                    else -> SapphirePrimary
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Category Pill
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = tagColor.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = note.category,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = tagColor
                                    )
                                }

                                if (note.week != null) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                                    ) {
                                        Text(
                                            text = "第 ${note.week} 週",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Text(
                                    text = dateFormat.format(Date(note.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = { onDeleteNote(note.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "刪除",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Text(
                            text = note.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = onOpenComposer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("新增課堂紀錄")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * 獨立專屬頁面: 課堂紀錄編輯器 (NoteComposerBottomSheet)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteComposerBottomSheet(
    course: Course,
    startDateStr: String,
    totalWeeks: Int,
    onDismiss: () -> Unit,
    onSave: (content: String, category: String, week: Int?) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("一般") }
    var selectedWeek by remember { mutableStateOf<Int?>(1) }
    var noteContentText by remember { mutableStateOf("") }
    var showWeekPicker by remember { mutableStateOf(false) }

    val courseColor = runCatching { Color(course.colorHex.toColorInt()) }
        .getOrDefault(SapphirePrimary)

    val categories = listOf("一般", "作業", "考試", "公告", "重點")

    // Weekly date calculation
    val parsedStart = try {
        val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
        LocalDate.parse(startDateStr, formatter)
    } catch (_: Exception) {
        LocalDate.now()
    }
    val mondayOfFirstWeek = parsedStart.minusDays((parsedStart.dayOfWeek.value - 1).toLong())
    val dayChinese = listOf("一", "二", "三", "四", "五", "六", "日").getOrElse(course.dayOfWeek - 1) { "一" }

    if (showWeekPicker) {
        AlertDialog(
            onDismissRequest = { showWeekPicker = false },
            title = {
                Text("選擇上課週次", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedWeek == null) SapphirePrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = if (selectedWeek == null) BorderStroke(1.dp, SapphirePrimary) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedWeek = null
                                showWeekPicker = false
                            }
                    ) {
                        Text(
                            text = "全學期 / 一般紀錄",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selectedWeek == null) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedWeek == null) SapphirePrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    for (w in 1..totalWeeks) {
                        val mondayOfWeek = mondayOfFirstWeek.plusWeeks((w - 1).toLong())
                        val courseDate = mondayOfWeek.plusDays((course.dayOfWeek - 1).toLong())
                        val isSel = selectedWeek == w

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) SapphirePrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = if (isSel) BorderStroke(1.dp, SapphirePrimary) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedWeek = w
                                    showWeekPicker = false
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "第 $w 週",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) SapphirePrimary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${courseDate.monthValue}月${courseDate.dayOfMonth}日 ($dayChinese)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSel) SapphirePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showWeekPicker = false }) {
                    Text("關閉")
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: 取消 | 課堂紀錄 | 儲存
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismiss,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "取消",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "課堂紀錄",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                val hasContent = noteContentText.isNotBlank()
                Button(
                    onClick = {
                        if (hasContent) {
                            onSave(noteContentText.trim(), selectedCategory, selectedWeek)
                            onDismiss()
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasContent) SapphirePrimary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (hasContent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    ),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp)
                ) {
                    Text("儲存", fontWeight = FontWeight.Bold)
                }
            }

            // Meta Row: Course Badge + Week Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Course tag
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = courseColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, courseColor.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(courseColor)
                        )
                        Text(
                            text = course.name,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Week selector chip
                val weekLabel = if (selectedWeek != null) {
                    val mondayOfWeek = mondayOfFirstWeek.plusWeeks((selectedWeek!! - 1).toLong())
                    val courseDate = mondayOfWeek.plusDays((course.dayOfWeek - 1).toLong())
                    "下次上課 · ${courseDate.monthValue}月${courseDate.dayOfMonth}日 ($dayChinese) · 第 $selectedWeek 週"
                } else {
                    "全學期 / 一般紀錄"
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.clickable { showWeekPicker = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = weekLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Category Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "分類",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        val activeColor = when (cat) {
                            "作業" -> Color(0xFFD97706)
                            "考試" -> RoseAccent
                            "公告" -> Color(0xFF6366F1)
                            "重點" -> Color(0xFF0D9488)
                            else -> SapphirePrimary
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) activeColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.clickable { selectedCategory = cat }
                        ) {
                            Text(
                                text = cat,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Note Content: Clean borderless multi-line editor
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                TextField(
                    value = noteContentText,
                    onValueChange = { noteContentText = it },
                    placeholder = {
                        Text(
                            text = "記下課堂內容、作業、公告...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    },
                    modifier = Modifier.fillMaxSize(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
                )
            }

            // Bottom action bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "字數：${noteContentText.length}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Tab 3: 課程資訊
 */
@Composable
private fun InfoTabView(
    course: Course,
    dayName: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val formatPeriodCode = { p: Int ->
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

        DetailRowItem(
            icon = Icons.Default.AccessTime,
            label = "上課時間",
            value = "$dayName 第 ${formatPeriodCode(course.startPeriod)} ~ ${formatPeriodCode(course.endPeriod)} 節" +
                    if (course.startTime.isNotBlank()) " (${course.startTime}~${course.endTime})" else ""
        )

        if (course.location.isNotBlank()) {
            DetailRowItem(
                icon = Icons.Default.Place,
                label = "上課地點",
                value = course.location
            )
        }

        if (course.teacher.isNotBlank()) {
            DetailRowItem(
                icon = Icons.Default.Person,
                label = "授課教師",
                value = course.teacher
            )
        }

        DetailRowItem(
            icon = Icons.Default.School,
            label = "學分數與學期",
            value = "${course.credits} 學分 (${course.semester} 學期)"
        )

        val baseCategoryTitle = course.customCategory.ifBlank { course.category.label }
        val categoryLabel = buildString {
            append("$baseCategoryTitle・${course.requirementType.label}")
            if (course.subcategory.isNotBlank()) {
                append(" (${course.subcategory})")
            } else if (course.generalEduSubtype != GeneralEduSubtype.NONE) {
                append(" (${course.generalEduSubtype.label})")
            }
        }

        DetailRowItem(
            icon = Icons.Default.Category,
            label = "課程類別",
            value = categoryLabel
        )

        if (course.code.isNotBlank()) {
            DetailRowItem(
                icon = Icons.Default.QrCode,
                label = "課程代碼",
                value = course.code
            )
        }

        if (course.score != null || course.letterGrade != null) {
            DetailRowItem(
                icon = Icons.Default.Grade,
                label = "修習成績",
                value = buildString {
                    if (course.score != null) append("${course.score} 分 ")
                    if (course.letterGrade != null) append("(${course.letterGrade})")
                }
            )
        }
    }
}

@Composable
private fun DetailRowItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
