package com.example.ui.screens.timetable

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Course
import com.example.data.model.CourseCategory
import com.example.data.model.GeneralEduSubtype
import com.example.ui.components.ColorPickerRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCourseDialog(
    initialCourse: Course? = null,
    defaultSemester: String,
    onDismiss: () -> Unit,
    onSave: (Course) -> Unit
) {
    var name by remember { mutableStateOf(initialCourse?.name ?: "") }
    var code by remember { mutableStateOf(initialCourse?.code ?: "") }
    var teacher by remember { mutableStateOf(initialCourse?.teacher ?: "") }
    var location by remember { mutableStateOf(initialCourse?.location ?: "") }
    var dayOfWeek by remember { mutableIntStateOf(initialCourse?.dayOfWeek ?: 1) }
    var startPeriod by remember { mutableIntStateOf(initialCourse?.startPeriod ?: 1) }
    var endPeriod by remember { mutableIntStateOf(initialCourse?.endPeriod ?: 2) }
    var creditsText by remember { mutableStateOf(initialCourse?.credits?.toString() ?: "3.0") }
    var category by remember { mutableStateOf(initialCourse?.category ?: CourseCategory.REQUIRED) }
    var generalEduSubtype by remember { mutableStateOf(initialCourse?.generalEduSubtype ?: GeneralEduSubtype.NONE) }
    var semester by remember { mutableStateOf(initialCourse?.semester ?: defaultSemester) }
    var scoreText by remember { mutableStateOf(initialCourse?.score?.toString() ?: "") }
    var letterGrade by remember { mutableStateOf(initialCourse?.letterGrade ?: "") }
    var colorHex by remember { mutableStateOf(initialCourse?.colorHex ?: "#2563EB") }
    var notes by remember { mutableStateOf(initialCourse?.notes ?: "") }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var dayDropdownExpanded by remember { mutableStateOf(false) }
    var generalSubtypeDropdownExpanded by remember { mutableStateOf(false) }

    val days = listOf("星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialCourse == null) "新增課程" else "編輯課程",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Course Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("課程名稱 *") },
                    placeholder = { Text("例如：演算法、資料結構") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("course_name_input")
                )

                // Teacher & Location
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = teacher,
                        onValueChange = { teacher = it },
                        label = { Text("授課教師") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("教室地點") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Semester & Course Code
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = semester,
                        onValueChange = { semester = it },
                        label = { Text("學期") },
                        placeholder = { Text("例如：113-2") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("課號 (選填)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Day of Week & Periods
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = dayDropdownExpanded,
                        onExpandedChange = { dayDropdownExpanded = !dayDropdownExpanded },
                        modifier = Modifier.weight(1.2f)
                    ) {
                        OutlinedTextField(
                            value = days.getOrElse(dayOfWeek - 1) { "星期一" },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("星期") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayDropdownExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = dayDropdownExpanded,
                            onDismissRequest = { dayDropdownExpanded = false }
                        ) {
                            days.forEachIndexed { index, dayName ->
                                DropdownMenuItem(
                                    text = { Text(dayName) },
                                    onClick = {
                                        dayOfWeek = index + 1
                                        dayDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = startPeriod.toString(),
                        onValueChange = { str ->
                            str.toIntOrNull()?.let {
                                if (it in 1..14) {
                                    startPeriod = it
                                    if (endPeriod < it) endPeriod = it
                                }
                            }
                        },
                        label = { Text("起始節") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )

                    OutlinedTextField(
                        value = endPeriod.toString(),
                        onValueChange = { str ->
                            str.toIntOrNull()?.let {
                                if (it in startPeriod..14) endPeriod = it
                            }
                        },
                        label = { Text("結束節") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                }

                // Category & Credits
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = categoryDropdownExpanded,
                        onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded },
                        modifier = Modifier.weight(1.2f)
                    ) {
                        OutlinedTextField(
                            value = category.label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("學分屬性") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = categoryDropdownExpanded,
                            onDismissRequest = { categoryDropdownExpanded = false }
                        ) {
                            CourseCategory.entries.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.label) },
                                    onClick = {
                                        category = cat
                                        categoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = creditsText,
                        onValueChange = { creditsText = it },
                        label = { Text("學分數") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                }

                // General Education Subtype if Category == GENERAL_EDU
                if (category == CourseCategory.GENERAL_EDU) {
                    ExposedDropdownMenuBox(
                        expanded = generalSubtypeDropdownExpanded,
                        onExpandedChange = { generalSubtypeDropdownExpanded = !generalSubtypeDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = generalEduSubtype.label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("通識領域") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = generalSubtypeDropdownExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = generalSubtypeDropdownExpanded,
                            onDismissRequest = { generalSubtypeDropdownExpanded = false }
                        ) {
                            GeneralEduSubtype.entries.forEach { subtype ->
                                DropdownMenuItem(
                                    text = { Text(subtype.label) },
                                    onClick = {
                                        generalEduSubtype = subtype
                                        generalSubtypeDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Grade / Score (Optional for completed courses)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = scoreText,
                        onValueChange = { scoreText = it },
                        label = { Text("成績分數 (選填)") },
                        placeholder = { Text("0~100") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = letterGrade,
                        onValueChange = { letterGrade = it.uppercase() },
                        label = { Text("等第 (選填)") },
                        placeholder = { Text("A+, A, B...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Color Picker
                Text(
                    text = "課表標記色彩",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                ColorPickerRow(
                    selectedColorHex = colorHex,
                    onColorSelected = { colorHex = it }
                )

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("備註 (考試日期、分組等)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val credits = creditsText.toDoubleOrNull() ?: 3.0
                        val score = scoreText.toDoubleOrNull()
                        val course = (initialCourse ?: Course(name = name)).copy(
                            name = name.trim(),
                            code = code.trim(),
                            teacher = teacher.trim(),
                            location = location.trim(),
                            dayOfWeek = dayOfWeek,
                            startPeriod = startPeriod,
                            endPeriod = endPeriod,
                            credits = credits,
                            category = category,
                            generalEduSubtype = generalEduSubtype,
                            semester = semester.trim(),
                            score = score,
                            letterGrade = if (letterGrade.isNotBlank()) letterGrade.trim() else null,
                            isCompleted = score != null || letterGrade.isNotBlank(),
                            colorHex = colorHex,
                            notes = notes.trim()
                        )
                        onSave(course)
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("save_course_button")
            ) {
                Text("儲存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
