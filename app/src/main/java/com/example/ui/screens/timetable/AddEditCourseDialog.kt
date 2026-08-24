package com.example.ui.screens.timetable

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
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
import com.example.data.model.GraduationPlan
import com.example.ui.components.ColorPickerRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCourseDialog(
    initialCourse: Course? = null,
    defaultSemester: String,
    allCourses: List<Course> = emptyList(),
    plan: GraduationPlan? = null,
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
    var colorHex by remember { mutableStateOf(initialCourse?.colorHex ?: "#2563EB") }
    var notes by remember { mutableStateOf(initialCourse?.notes ?: "") }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var requirementDropdownExpanded by remember { mutableStateOf(false) }
    var dayDropdownExpanded by remember { mutableStateOf(false) }
    var startPeriodDropdownExpanded by remember { mutableStateOf(false) }
    var endPeriodDropdownExpanded by remember { mutableStateOf(false) }
    var generalSubtypeDropdownExpanded by remember { mutableStateOf(false) }

    val days = listOf("星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日")

    // 計算各模組與修別的已修/通過學分與門檻
    val relevantCourses = remember(allCourses, initialCourse) {
        allCourses.filter { it.id != initialCourse?.id }
    }

    // 檢查同一學期、同一星期是否有其他課程在節次上有重疊 (衝堂檢查)
    val conflictingCourse = remember(relevantCourses, semester, dayOfWeek, startPeriod, endPeriod) {
        relevantCourses.firstOrNull { other ->
            other.semester == semester.trim() &&
            other.dayOfWeek == dayOfWeek &&
            maxOf(startPeriod, other.startPeriod) <= minOf(endPeriod, other.endPeriod)
        }
    }

    val minScore = plan?.minPassingScore ?: 60.0

    fun isPassed(c: Course): Boolean {
        return c.isCompleted || (c.score != null && c.score >= minScore)
    }

    // 取得特定類別在目標設定中的 (必修門檻, 選修門檻, 總門檻)
    fun getCategoryTargets(cat: CourseCategory): Triple<Double, Double, Double> {
        if (plan == null) return Triple(0.0, 0.0, 0.0)
        return when (cat) {
            CourseCategory.GENERAL_EDU -> {
                val total = plan.targetGeneralCredits
                val req = if (plan.targetGeneralRequiredCredits == 0.0 && plan.targetGeneralElectiveCredits == 0.0 && total > 0.0) total else plan.targetGeneralRequiredCredits
                val ele = plan.targetGeneralElectiveCredits
                Triple(req, ele, if (total > 0.0) total else (req + ele))
            }
            CourseCategory.COLLEGE_CORE -> {
                val total = plan.targetCollegeCoreCredits
                val req = if (plan.targetCollegeCoreRequiredCredits > 0.0) plan.targetCollegeCoreRequiredCredits else total
                Triple(req, 0.0, if (total > 0.0) total else req)
            }
            CourseCategory.BASIC_MODULE -> {
                val total = plan.targetBasicModuleCredits
                val req = if (plan.targetBasicModuleRequiredCredits == 0.0 && plan.targetBasicModuleElectiveCredits == 0.0 && total > 0.0) total else plan.targetBasicModuleRequiredCredits
                val ele = plan.targetBasicModuleElectiveCredits
                Triple(req, ele, if (total > 0.0) total else (req + ele))
            }
            CourseCategory.CORE_MODULE -> {
                val total = plan.targetCoreModuleCredits
                val req = if (plan.targetCoreModuleRequiredCredits == 0.0 && plan.targetCoreModuleElectiveCredits == 0.0 && total > 0.0) total else plan.targetCoreModuleRequiredCredits
                val ele = plan.targetCoreModuleElectiveCredits
                Triple(req, ele, if (total > 0.0) total else (req + ele))
            }
            CourseCategory.PROFESSIONAL_MODULE -> {
                val total = plan.targetProfessionalModuleCredits
                val req = if (plan.targetProfessionalModuleRequiredCredits == 0.0 && plan.targetProfessionalModuleElectiveCredits == 0.0 && total > 0.0) total else plan.targetProfessionalModuleRequiredCredits
                val ele = plan.targetProfessionalModuleElectiveCredits
                Triple(req, ele, if (total > 0.0) total else (req + ele))
            }
            CourseCategory.FREE_ELECTIVE -> {
                val total = plan.targetFreeCredits
                val ele = if (plan.targetFreeElectiveCredits > 0.0) plan.targetFreeElectiveCredits else total
                Triple(0.0, ele, if (total > 0.0) total else ele)
            }
            else -> Triple(0.0, 0.0, 0.0)
        }
    }

    // 取得特定類別目前已取得的 (必修學分, 選修學分, 總學分)
    fun getCategoryEarned(cat: CourseCategory): Triple<Double, Double, Double> {
        val catCourses = relevantCourses.filter { it.category == cat && isPassed(it) }
        val earnedReq = catCourses.filter { it.requirementType == CourseRequirementType.REQUIRED || it.requirementType == CourseRequirementType.REQUIRED_ELECTIVE }.sumOf { it.credits }
        val earnedEle = catCourses.filter { it.requirementType == CourseRequirementType.ELECTIVE }.sumOf { it.credits }
        return Triple(earnedReq, earnedEle, earnedReq + earnedEle)
    }

    // 判斷某類別在某修別下是否已額滿
    fun isRequirementFull(cat: CourseCategory, req: CourseRequirementType): Boolean {
        if (plan == null) return false
        val (targetReq, targetEle, _) = getCategoryTargets(cat)
        val (earnedReq, earnedEle, _) = getCategoryEarned(cat)
        return when (req) {
            CourseRequirementType.REQUIRED -> targetReq > 0.0 && earnedReq >= targetReq
            CourseRequirementType.ELECTIVE -> targetEle > 0.0 && earnedEle >= targetEle
            CourseRequirementType.REQUIRED_ELECTIVE -> (targetReq > 0.0 && earnedReq >= targetReq) && (targetEle > 0.0 && earnedEle >= targetEle)
        }
    }

    // 判斷某類別是否已完全額滿
    fun isCategoryFull(cat: CourseCategory): Boolean {
        if (plan == null) return false
        val (targetReq, targetEle, targetTotal) = getCategoryTargets(cat)
        val (earnedReq, earnedEle, earnedTotal) = getCategoryEarned(cat)

        if (targetTotal > 0.0 && earnedTotal >= targetTotal) return true

        val reqSatisfied = if (targetReq > 0.0) earnedReq >= targetReq else true
        val eleSatisfied = if (targetEle > 0.0) earnedEle >= targetEle else true
        val hasAnyTarget = targetReq > 0.0 || targetEle > 0.0 || targetTotal > 0.0

        return hasAnyTarget && reqSatisfied && eleSatisfied
    }

    // 可選的學分屬性清單（過濾已滿額的模組，但保留目前編輯中課程的屬性）
    val availableCategories = remember(relevantCourses, plan, initialCourse) {
        val baseCategories = CourseCategory.entries.filter {
            it != CourseCategory.REQUIRED && it != CourseCategory.ELECTIVE && it != CourseCategory.PE
        }
        val uncompleted = baseCategories.filter { cat ->
            !isCategoryFull(cat) || (initialCourse != null && initialCourse.category == cat)
        }
        uncompleted.ifEmpty { baseCategories }
    }

    // 依據目前選擇的 category，過濾可選的修別 (必修 / 選修 / 必選修)
    val availableRequirementTypes = remember(category, relevantCourses, plan, initialCourse) {
        val selectedCat = category ?: return@remember CourseRequirementType.entries.toList()
        val (targetReq, targetEle, _) = getCategoryTargets(selectedCat)

        val types = mutableListOf<CourseRequirementType>()

        if (targetReq > 0.0) {
            val isFull = isRequirementFull(selectedCat, CourseRequirementType.REQUIRED)
            if (!isFull || (initialCourse != null && initialCourse.category == selectedCat && initialCourse.requirementType == CourseRequirementType.REQUIRED)) {
                types.add(CourseRequirementType.REQUIRED)
            }
        }

        if (targetEle > 0.0 || selectedCat == CourseCategory.FREE_ELECTIVE) {
            val isFull = isRequirementFull(selectedCat, CourseRequirementType.ELECTIVE)
            if (!isFull || (initialCourse != null && initialCourse.category == selectedCat && initialCourse.requirementType == CourseRequirementType.ELECTIVE)) {
                types.add(CourseRequirementType.ELECTIVE)
            }
        }

        if (targetReq > 0.0 && targetEle > 0.0) {
            val isFull = isRequirementFull(selectedCat, CourseRequirementType.REQUIRED_ELECTIVE)
            if (!isFull || (initialCourse != null && initialCourse.category == selectedCat && initialCourse.requirementType == CourseRequirementType.REQUIRED_ELECTIVE)) {
                types.add(CourseRequirementType.REQUIRED_ELECTIVE)
            }
        }

        if (types.isEmpty()) {
            if (targetEle > 0.0 || selectedCat == CourseCategory.FREE_ELECTIVE) {
                listOf(CourseRequirementType.ELECTIVE)
            } else {
                listOf(CourseRequirementType.REQUIRED)
            }
        } else {
            types
        }
    }

    LaunchedEffect(availableCategories) {
        if (category != null && category !in availableCategories) {
            category = availableCategories.firstOrNull()
        }
    }

    LaunchedEffect(availableRequirementTypes) {
        if (requirementType !in availableRequirementTypes && availableRequirementTypes.isNotEmpty()) {
            requirementType = availableRequirementTypes.first()
        }
    }

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

                // 衝堂警告提示 (Conflicting Course Alert)
                if (conflictingCourse != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "課程時間重疊（衝堂）：與「${conflictingCourse.name}」(第 ${conflictingCourse.startPeriod}~${conflictingCourse.endPeriod} 節) 衝突，無法儲存",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.SemiBold
                            )
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
                            availableCategories.forEach { cat ->
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
                            availableRequirementTypes.forEach { req ->
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
                            score = initialCourse?.score,
                            letterGrade = initialCourse?.letterGrade,
                            isCompleted = initialCourse?.isCompleted ?: false,
                            colorHex = colorHex,
                            notes = notes.trim()
                        )
                        onSave(course)
                    }
                },
                enabled = name.isNotBlank() && category != null && conflictingCourse == null && startPeriod <= endPeriod,
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
