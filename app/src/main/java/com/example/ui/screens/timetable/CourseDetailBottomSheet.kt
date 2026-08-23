package com.example.ui.screens.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.example.data.model.Course
import com.example.ui.theme.RoseAccent
import com.example.ui.theme.SapphirePrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailBottomSheet(
    course: Course,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val courseColor = runCatching { Color(course.colorHex.toColorInt()) }
        .getOrDefault(SapphirePrimary)

    val days = listOf("星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日")
    val dayName = days.getOrElse(course.dayOfWeek - 1) { "星期一" }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with color bar & title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(courseColor)
                    )
                    Column {
                        Text(
                            text = course.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (course.code.isNotBlank()) {
                            Text(
                                text = "課號：${course.code}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Badge(
                    containerColor = course.category.badgeColor.copy(alpha = 0.15f),
                    contentColor = course.category.badgeColor
                ) {
                    Text(
                        text = course.category.label,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            HorizontalDivider()

            // Info rows
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DetailRowItem(
                    icon = Icons.Default.AccessTime,
                    label = "上課時間",
                    value = "$dayName 第 ${course.startPeriod} ~ ${course.endPeriod} 節" +
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
                    label = "學分數",
                    value = "${course.credits} 學分 (${course.semester} 學期)"
                )

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

                if (course.notes.isNotBlank()) {
                    DetailRowItem(
                        icon = Icons.AutoMirrored.Filled.Notes,
                        label = "備註說明",
                        value = course.notes
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("delete_course_button"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseAccent)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("刪除課程")
                }

                Button(
                    onClick = onEdit,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("edit_course_button")
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("編輯課程")
                }
            }
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
                .size(32.dp)
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
