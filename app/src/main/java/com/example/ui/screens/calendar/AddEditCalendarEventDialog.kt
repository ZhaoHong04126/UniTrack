package com.example.ui.screens.calendar

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.toColorInt
import com.example.data.model.CalendarEvent
import com.example.data.model.CalendarEventCategory
import com.example.data.model.Course
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCalendarEventDialog(
    initialEvent: CalendarEvent? = null,
    initialCategory: CalendarEventCategory? = null,
    initialDate: String = "",
    courses: List<Course> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (CalendarEvent) -> Unit,
    onDelete: ((CalendarEvent) -> Unit)? = null
) {
    val context = LocalContext.current
    val isEditMode = initialEvent != null

    val todayStr = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    var title by remember { mutableStateOf(initialEvent?.title ?: "") }
    var selectedCategory by remember {
        mutableStateOf(initialCategory ?: initialEvent?.category ?: CalendarEventCategory.STUDY)
    }
    var dateString by remember {
        mutableStateOf(
            initialEvent?.date ?: initialDate.ifBlank { todayStr }
        )
    }
    var isAllDay by remember { mutableStateOf(initialEvent?.isAllDay ?: true) }
    var startTime by remember { mutableStateOf(initialEvent?.startTime?.ifBlank { "09:00" } ?: "09:00") }
    var endTime by remember { mutableStateOf(initialEvent?.endTime?.ifBlank { "10:00" } ?: "10:00") }
    var location by remember { mutableStateOf(initialEvent?.location ?: "") }
    var notes by remember { mutableStateOf(initialEvent?.notes ?: "") }
    var selectedCourseId by remember { mutableStateOf(initialEvent?.courseId) }
    var showCourseMenu by remember { mutableStateOf(false) }

    var titleError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Dialog Title Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditMode) "編輯行程事項" else "新增行程事項",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "關閉")
                    }
                }

                // Event Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) titleError = false
                    },
                    label = { Text("事項名稱") },
                    placeholder = { Text("例如：期中考、專題簡報、迎新宿營") },
                    isError = titleError,
                    supportingText = if (titleError) {
                        { Text("請輸入事項名稱", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Chips Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "事項類別",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CalendarEventCategory.entries.forEach { cat ->
                            val isSelected = selectedCategory == cat
                            val catColor = try {
                                Color(cat.defaultColorHex.toColorInt())
                            } catch (_: Exception) {
                                MaterialTheme.colorScheme.primary
                            }

                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat.displayName) },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(catColor)
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = catColor.copy(alpha = 0.2f),
                                    selectedLabelColor = MaterialTheme.colorScheme.onSurface
                                ),
                                border = if (isSelected) BorderStroke(1.5.dp, catColor) else null
                            )
                        }
                    }
                }

                // Date Picker Card
                Surface(
                    onClick = {
                        val parts = dateString.split("-")
                        val cal = Calendar.getInstance()
                        val y = parts.getOrNull(0)?.toIntOrNull() ?: cal.get(Calendar.YEAR)
                        val m = (parts.getOrNull(1)?.toIntOrNull() ?: (cal.get(Calendar.MONTH) + 1)) - 1
                        val d = parts.getOrNull(2)?.toIntOrNull() ?: cal.get(Calendar.DAY_OF_MONTH)

                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                dateString = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                            },
                            y, m, d
                        ).show()
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = "日期",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = dateString,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // All Day Toggle Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "整天行程",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Switch(
                        checked = isAllDay,
                        onCheckedChange = { isAllDay = it }
                    )
                }

                // Time Pickers (if not All Day)
                if (!isAllDay) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Start Time
                        Surface(
                            onClick = {
                                val parts = startTime.split(":")
                                val cal = Calendar.getInstance()
                                val h = parts.getOrNull(0)?.toIntOrNull() ?: cal.get(Calendar.HOUR_OF_DAY)
                                val m = parts.getOrNull(1)?.toIntOrNull() ?: cal.get(Calendar.MINUTE)
                                TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        startTime = String.format(Locale.US, "%02d:%02d", hourOfDay, minute)
                                    },
                                    h, m, true
                                ).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "開始時間",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = startTime,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // End Time
                        Surface(
                            onClick = {
                                val parts = endTime.split(":")
                                val cal = Calendar.getInstance()
                                val h = parts.getOrNull(0)?.toIntOrNull() ?: (cal.get(Calendar.HOUR_OF_DAY) + 1)
                                val m = parts.getOrNull(1)?.toIntOrNull() ?: cal.get(Calendar.MINUTE)
                                TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        endTime = String.format(Locale.US, "%02d:%02d", hourOfDay, minute)
                                    },
                                    h, m, true
                                ).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "結束時間",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = endTime,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Course Association (Optional)
                if (courses.isNotEmpty()) {
                    val selectedCourse = courses.find { it.id == selectedCourseId }
                    Box {
                        Surface(
                            onClick = { showCourseMenu = true },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "關聯課程 (選填)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = selectedCourse?.name ?: "無關聯課程",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (selectedCourse != null) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (selectedCourse != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }

                        DropdownMenu(
                            expanded = showCourseMenu,
                            onDismissRequest = { showCourseMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("無關聯課程") },
                                onClick = {
                                    selectedCourseId = null
                                    showCourseMenu = false
                                }
                            )
                            courses.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(c.name) },
                                    onClick = {
                                        selectedCourseId = c.id
                                        showCourseMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Location Input (Optional)
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("地點 (選填)") },
                    placeholder = { Text("例如：工三 101、圖書館 2F") },
                    leadingIcon = { Icon(Icons.Outlined.Place, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Notes Input (Optional)
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("備註 / 說明 (選填)") },
                    placeholder = { Text("補充注意事項或攜帶物品...") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Notes, contentDescription = null) },
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (initialEvent != null && onDelete != null) {
                        OutlinedButton(
                            onClick = {
                                onDelete(initialEvent)
                                onDismiss()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("刪除")
                        }
                    }

                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                titleError = true
                                return@Button
                            }
                            val matchedCourse = courses.find { it.id == selectedCourseId }
                            val eventToSave = CalendarEvent(
                                id = initialEvent?.id ?: 0L,
                                title = title.trim(),
                                date = dateString,
                                startTime = if (isAllDay) "" else startTime,
                                endTime = if (isAllDay) "" else endTime,
                                isAllDay = isAllDay,
                                category = selectedCategory,
                                location = location.trim(),
                                notes = notes.trim(),
                                courseId = selectedCourseId,
                                courseName = matchedCourse?.name ?: "",
                                isCompleted = initialEvent?.isCompleted ?: false,
                                colorHex = selectedCategory.defaultColorHex
                            )
                            onSave(eventToSave)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(if (isEditMode && onDelete != null) 1.5f else 1f)
                    ) {
                        Text(if (isEditMode) "儲存變更" else "新增事項", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
