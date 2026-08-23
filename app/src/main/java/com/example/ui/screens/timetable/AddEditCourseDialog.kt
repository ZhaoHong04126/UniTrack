package com.example.ui.screens.timetable

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Course
import com.example.data.model.CourseCategory
import com.example.data.model.CourseRequirementType
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
    var teacher by remember { mutableStateOf(initialCourse?.teacher ?: "") }
    var location by remember { mutableStateOf(initialCourse?.location ?: "") }
    var dayOfWeek by remember { mutableIntStateOf(initialCourse?.dayOfWeek ?: 1) }
    var startPeriod by remember { mutableIntStateOf(initialCourse?.startPeriod ?: 1) }
    var endPeriod by remember { mutableIntStateOf(initialCourse?.endPeriod ?: 2) }
    var creditsText by remember { mutableStateOf(initialCourse?.credits?.toString() ?: "3.0") }
    var category by remember { mutableStateOf(initialCourse?.category) }
    var requirementType by remember { mutableStateOf(initialCourse?.requirementType ?: CourseRequirementType.REQUIRED) }
    var generalEduSubtype by remember { mutableStateOf(initialCourse?.generalEduSubtype ?: GeneralEduSubtype.NONE) }
    var semester by remember { mutableStateOf(initialCourse?.semester ?: defaultSemester) }
    var scoreText by remember { mutableStateOf(initialCourse?.score?.toString() ?: "") }
    var colorHex by remember { mutableStateOf(initialCourse?.colorHex ?: "#2563EB") }
    var notes by remember { mutableStateOf(initialCourse?.notes ?: "") }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var requirementDropdownExpanded by remember { mutableStateOf(false) }
    var dayDropdownExpanded by remember { mutableStateOf(false) }
    var startPeriodDropdownExpanded by remember { mutableStateOf(false) }
    var endPeriodDropdownExpanded by remember { mutableStateOf(false) }
    var generalSubtypeDropdownExpanded by remember { mutableStateOf(false) }

    val days = listOf("星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (initialCourse == null) "新增課程" else "編輯課程",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (semester.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "$semester 學期",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
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

                // Day of Week & Credits
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
                            label = { Text("上課星期") },
                            singleLine = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayDropdownExpanded) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
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
                        value = creditsText,
                        onValueChange = { creditsText = it },
                        label = { Text("學分數") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                }

                // Start & End Periods
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val formatPeriodOption = { p: Int ->
                        when (p) {
                            10 -> "第 A 節 (10)"
                            11 -> "第 B 節 (11)"
                            12 -> "第 C 節 (12)"
                            13 -> "第 D 節 (13)"
                            14 -> "第 E 節 (14)"
                            else -> "第 $p 節"
                        }
                    }

                    // Start Period
                    ExposedDropdownMenuBox(
                        expanded = startPeriodDropdownExpanded,
                        onExpandedChange = { startPeriodDropdownExpanded = !startPeriodDropdownExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = formatPeriodOption(startPeriod),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("起始節") },
                            singleLine = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = startPeriodDropdownExpanded) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = startPeriodDropdownExpanded,
                            onDismissRequest = { startPeriodDropdownExpanded = false }
                        ) {
                            (1..14).forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(formatPeriodOption(p)) },
                                    onClick = {
                                        startPeriod = p
                                        if (endPeriod < p) endPeriod = p
                                        startPeriodDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // End Period
                    ExposedDropdownMenuBox(
                        expanded = endPeriodDropdownExpanded,
                        onExpandedChange = { endPeriodDropdownExpanded = !endPeriodDropdownExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = formatPeriodOption(endPeriod),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("結束節") },
                            singleLine = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = endPeriodDropdownExpanded) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = endPeriodDropdownExpanded,
                            onDismissRequest = { endPeriodDropdownExpanded = false }
                        ) {
                            (startPeriod..14).forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(formatPeriodOption(p)) },
                                    onClick = {
                                        endPeriod = p
                                        endPeriodDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Category & Requirement Type (必修 / 選修 / 必選修)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 學分屬性
                    ExposedDropdownMenuBox(
                        expanded = categoryDropdownExpanded,
                        onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded },
                        modifier = Modifier.weight(1.3f)
                    ) {
                        OutlinedTextField(
                            value = category?.label ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("學分屬性 *") },
                            placeholder = { Text("請選擇") },
                            singleLine = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = categoryDropdownExpanded,
                            onDismissRequest = { categoryDropdownExpanded = false }
                        ) {
                            CourseCategory.entries
                                .filter { it != CourseCategory.REQUIRED && it != CourseCategory.ELECTIVE && it != CourseCategory.PE }
                                .forEach { cat ->
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

                    // 修別 (必修 / 選修 / 必選修)
                    ExposedDropdownMenuBox(
                        expanded = requirementDropdownExpanded,
                        onExpandedChange = { requirementDropdownExpanded = !requirementDropdownExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = requirementType.label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("修別 *") },
                            singleLine = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = requirementDropdownExpanded) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = requirementDropdownExpanded,
                            onDismissRequest = { requirementDropdownExpanded = false }
                        ) {
                            CourseRequirementType.entries.forEach { req ->
                                DropdownMenuItem(
                                    text = { Text(req.label) },
                                    onClick = {
                                        requirementType = req
                                        requirementDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
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
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
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

                // Score (Optional for completed courses)
                OutlinedTextField(
                    value = scoreText,
                    onValueChange = { scoreText = it },
                    label = { Text("成績分數 (選填)") },
                    placeholder = { Text("例如：85 (0~100)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

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
                    if (name.isNotBlank() && category != null) {
                        val credits = creditsText.toDoubleOrNull() ?: 3.0
                        val score = scoreText.toDoubleOrNull()
                        val course = (initialCourse ?: Course(name = name)).copy(
                            name = name.trim(),
                            code = initialCourse?.code ?: "",
                            teacher = teacher.trim(),
                            location = location.trim(),
                            dayOfWeek = dayOfWeek,
                            startPeriod = startPeriod,
                            endPeriod = endPeriod,
                            credits = credits,
                            category = category!!,
                            requirementType = requirementType,
                            generalEduSubtype = if (category == CourseCategory.GENERAL_EDU) generalEduSubtype else GeneralEduSubtype.NONE,
                            semester = semester.trim(),
                            score = score,
                            letterGrade = initialCourse?.letterGrade,
                            isCompleted = score != null,
                            colorHex = colorHex,
                            notes = notes.trim()
                        )
                        onSave(course)
                    }
                },
                enabled = name.isNotBlank() && category != null,
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
