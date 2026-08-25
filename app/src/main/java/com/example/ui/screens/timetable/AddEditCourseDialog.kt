package com.example.ui.screens.timetable

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.example.data.model.Course
import com.example.data.model.CourseCategory
import com.example.data.model.CourseRequirementType
import com.example.data.model.CustomParentCategory
import com.example.data.model.GeneralEduSubtype
import com.example.data.model.GraduationPlan
import java.util.Locale

data class CategoryOption(
    val standardCat: CourseCategory? = null,
    val customCat: CustomParentCategory? = null,
    val label: String,
    val shortLabel: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCourseDialog(
    initialCourse: Course? = null,
    defaultSemester: String,
    allCourses: List<Course> = emptyList(),
    plan: GraduationPlan? = null,
    onDismiss: () -> Unit,
    onSave: (Course) -> Unit,
    onSaveMultiple: ((List<Course>) -> Unit)? = null
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initialCourse?.name ?: "") }
    var teacher by remember { mutableStateOf(initialCourse?.teacher ?: "") }
    var code by remember { mutableStateOf(initialCourse?.code ?: "") }
    var isTimeTBD by remember { mutableStateOf(false) }

    data class TimeSlotItem(
        val id: String = java.util.UUID.randomUUID().toString(),
        val dayOfWeek: Int = 1,
        val startTimeStr: String = "09:00",
        val endTimeStr: String = "10:30",
        val repeatMode: String = "每週",
        val selectedWeeks: Set<Int> = (1..18).toSet(),
        val location: String = ""
    )

    var timeSlots by remember {
        val initialStart = initialCourse?.startTime?.ifBlank { null }
            ?: String.format(Locale.US, "%02d:00", 7 + (initialCourse?.startPeriod ?: 2))
        val initialEnd = initialCourse?.endTime?.ifBlank { null }
            ?: String.format(Locale.US, "%02d:00", 8 + (initialCourse?.endPeriod ?: 3))
        val initialWeeks = if (initialCourse != null && initialCourse.repeatWeeks.isNotBlank()) {
            if (initialCourse.repeatWeeks == "1-18") (1..18).toSet()
            else initialCourse.repeatWeeks.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
        } else {
            (1..18).toSet()
        }
        mutableStateOf(
            listOf(
                TimeSlotItem(
                    dayOfWeek = initialCourse?.dayOfWeek ?: 1,
                    startTimeStr = initialStart,
                    endTimeStr = initialEnd,
                    repeatMode = initialCourse?.repeatMode ?: "每週",
                    selectedWeeks = initialWeeks,
                    location = initialCourse?.location ?: ""
                )
            )
        )
    }

    fun updateSlot(index: Int, transform: (TimeSlotItem) -> TimeSlotItem) {
        if (index in timeSlots.indices) {
            val updated = timeSlots.toMutableList()
            updated[index] = transform(updated[index])
            timeSlots = updated
        }
    }

    var activeRepeatSlotIndex by remember { mutableStateOf<Int?>(null) }
    var activeSpecificWeeksSlotIndex by remember { mutableStateOf<Int?>(null) }
    var activeTimePickerSlotIndex by remember { mutableStateOf<Int?>(null) }
    var isPickingStartTime by remember { mutableStateOf(true) }

    var creditsText by remember { mutableStateOf(initialCourse?.credits?.toString() ?: "3.0") }
    var category by remember { mutableStateOf(initialCourse?.category) }
    var customCategoryName by remember { mutableStateOf(initialCourse?.customCategory ?: "") }
    var requirementType by remember { mutableStateOf(initialCourse?.requirementType) }
    var subcategoryText by remember {
        mutableStateOf(
            initialCourse?.subcategory?.ifBlank { null }
                ?: if (initialCourse?.generalEduSubtype != null && initialCourse.generalEduSubtype != GeneralEduSubtype.NONE) initialCourse.generalEduSubtype.label else ""
        )
    }
    var semester by remember { mutableStateOf(initialCourse?.semester ?: defaultSemester) }
    var colorHex by remember { mutableStateOf(initialCourse?.colorHex ?: "#3B82F6") }
    var notes by remember { mutableStateOf(initialCourse?.notes ?: "") }

    var otherInfoExpanded by remember { mutableStateOf(false) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var requirementTypeDropdownExpanded by remember { mutableStateOf(false) }
    var subcategoryDropdownExpanded by remember { mutableStateOf(false) }

    val weekdays = listOf(
        1 to "一",
        2 to "二",
        3 to "三",
        4 to "四",
        5 to "五",
        6 to "六",
        7 to "日"
    )

    val colorPalette = listOf(
        "#EF4444", "#F43F5E", "#EC4899", "#D946EF",
        "#A855F7", "#8B5CF6", "#6366F1", "#3B82F6",
        "#0EA5E9", "#06B6D4", "#14B8A6", "#10B981",
        "#22C55E", "#84CC16", "#EAB308", "#F59E0B",
        "#F97316", "#94A3B8"
    )

    val relevantCourses = remember(allCourses, initialCourse) {
        allCourses.filter { it.id != initialCourse?.id }
    }

    val conflictingCourseInfo = remember(relevantCourses, semester, timeSlots, isTimeTBD) {
        if (isTimeTBD) return@remember null
        for (slot in timeSlots) {
            val sHour = slot.startTimeStr.substringBefore(":").toIntOrNull() ?: 9
            val eHour = slot.endTimeStr.substringBefore(":").toIntOrNull() ?: 10
            val eMin = slot.endTimeStr.substringAfter(":").toIntOrNull() ?: 30
            val startP = (sHour - 7).coerceIn(1, 14)
            val endP = (if (eMin > 0) eHour - 7 else eHour - 8).coerceIn(startP, 14)
            val conflict = relevantCourses.firstOrNull { other ->
                other.semester == semester.trim() &&
                other.dayOfWeek == slot.dayOfWeek &&
                maxOf(startP, other.startPeriod) <= minOf(endP, other.endPeriod)
            }
            if (conflict != null) {
                return@remember Pair(conflict, slot)
            }
        }
        null
    }

    val allCategoryOptions = remember(plan, initialCourse) {
        val list = mutableListOf<CategoryOption>()
        val baseCategories = CourseCategory.entries.filter {
            it != CourseCategory.REQUIRED && it != CourseCategory.ELECTIVE && it != CourseCategory.PE
        }
        baseCategories.forEach { cat ->
            list.add(CategoryOption(standardCat = cat, label = cat.label, shortLabel = cat.shortLabel))
        }
        plan?.getCustomCategories()?.forEach { customCat ->
            list.add(CategoryOption(customCat = customCat, label = customCat.name, shortLabel = customCat.name.take(2)))
        }
        list
    }

    val selectedCategoryOption = remember(category, customCategoryName, allCategoryOptions) {
        if (customCategoryName.isNotBlank()) {
            allCategoryOptions.firstOrNull { it.customCat?.name == customCategoryName }
        } else if (category != null) {
            allCategoryOptions.firstOrNull { it.standardCat == category }
        } else null
    }

    val availableRequirementTypes = remember {
        listOf(
            CourseRequirementType.REQUIRED,
            CourseRequirementType.ELECTIVE,
            CourseRequirementType.REQUIRED_ELECTIVE
        )
    }

    val activeSubcategories = remember(selectedCategoryOption, plan) {
        if (selectedCategoryOption?.customCat != null) {
            selectedCategoryOption.customCat.subcategories.map { it.name }
        } else if (selectedCategoryOption?.standardCat != null && plan != null) {
            plan.getSubcategories(selectedCategoryOption.standardCat)
        } else emptyList()
    }

    val otherInfoSummary = remember(teacher, code, creditsText, selectedCategoryOption, requirementType, subcategoryText) {
        val items = mutableListOf<String>()
        if (teacher.isNotBlank()) items.add(teacher)
        if (code.isNotBlank()) items.add(code)
        if (creditsText.isNotBlank()) items.add("${creditsText}學分")
        selectedCategoryOption?.let { items.add(it.shortLabel) }
        requirementType?.let { items.add(it.label) }
        if (subcategoryText.isNotBlank()) items.add(subcategoryText)
        if (items.isNotEmpty()) items.joinToString(" · ") else "教授 · 課程代碼 · 學分"
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (initialCourse == null) "新增課程" else "編輯課程",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "課程名稱",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("請輸入 課程名稱") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("course_name_input")
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { otherInfoExpanded = !otherInfoExpanded }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "其他資訊",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (!otherInfoExpanded) {
                                Text(
                                    text = if (teacher.isBlank() && code.isBlank()) "教授 · 課程代碼 · 學分" else otherInfoSummary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 160.dp)
                                )
                            }
                            Icon(
                                imageVector = if (otherInfoExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    AnimatedVisibility(visible = otherInfoExpanded) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = teacher,
                                    onValueChange = { teacher = it },
                                    label = { Text("授課教師") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = code,
                                    onValueChange = { code = it },
                                    label = { Text("課程代碼") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            OutlinedTextField(
                                value = creditsText,
                                onValueChange = { creditsText = it },
                                label = { Text("學分數") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ExposedDropdownMenuBox(
                                    expanded = categoryDropdownExpanded,
                                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded },
                                    modifier = Modifier.weight(1.3f)
                                ) {
                                    OutlinedTextField(
                                        value = selectedCategoryOption?.label ?: "請選擇屬性",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("學分屬性 *") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = if (selectedCategoryOption == null) {
                                            OutlinedTextFieldDefaults.colors(
                                                unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                focusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        } else {
                                            OutlinedTextFieldDefaults.colors()
                                        },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = categoryDropdownExpanded,
                                        onDismissRequest = { categoryDropdownExpanded = false }
                                    ) {
                                        allCategoryOptions.forEach { opt ->
                                            DropdownMenuItem(
                                                text = { Text(opt.label) },
                                                onClick = {
                                                    category = opt.standardCat ?: CourseCategory.FREE_ELECTIVE
                                                    customCategoryName = opt.customCat?.name ?: ""
                                                    subcategoryText = ""
                                                    categoryDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                ExposedDropdownMenuBox(
                                    expanded = requirementTypeDropdownExpanded,
                                    onExpandedChange = { requirementTypeDropdownExpanded = !requirementTypeDropdownExpanded },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    OutlinedTextField(
                                        value = requirementType?.label ?: "請選擇修別",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("修別 *") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = if (requirementType == null) {
                                            OutlinedTextFieldDefaults.colors(
                                                unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                focusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        } else {
                                            OutlinedTextFieldDefaults.colors()
                                        },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = requirementTypeDropdownExpanded) },
                                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = requirementTypeDropdownExpanded,
                                        onDismissRequest = { requirementTypeDropdownExpanded = false }
                                    ) {
                                        availableRequirementTypes.forEach { req ->
                                            DropdownMenuItem(
                                                text = { Text(req.label) },
                                                onClick = {
                                                    requirementType = req
                                                    requirementTypeDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            if (activeSubcategories.isNotEmpty()) {
                                ExposedDropdownMenuBox(
                                    expanded = subcategoryDropdownExpanded,
                                    onExpandedChange = { subcategoryDropdownExpanded = !subcategoryDropdownExpanded },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = subcategoryText.ifBlank { "請選擇子分類 / 類別 (選填)" },
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("子分類 / 類別") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = if (subcategoryText.isBlank()) {
                                            OutlinedTextFieldDefaults.colors(
                                                unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                focusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        } else {
                                            OutlinedTextFieldDefaults.colors()
                                        },
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = subcategoryDropdownExpanded)
                                        },
                                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = subcategoryDropdownExpanded,
                                        onDismissRequest = { subcategoryDropdownExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("未指定 / 無") },
                                            onClick = {
                                                subcategoryText = ""
                                                subcategoryDropdownExpanded = false
                                            }
                                        )
                                        activeSubcategories.forEach { sub ->
                                            DropdownMenuItem(
                                                text = { Text(sub) },
                                                onClick = {
                                                    subcategoryText = sub
                                                    subcategoryDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = { Text("備註") },
                                maxLines = 3,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "上課時間",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "時間未定",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Switch(
                            checked = isTimeTBD,
                            onCheckedChange = { isTimeTBD = it },
                            modifier = Modifier.height(28.dp)
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            ),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val nextDay = ((timeSlots.lastOrNull()?.dayOfWeek ?: 1) % 7) + 1
                                    timeSlots = timeSlots + TimeSlotItem(
                                        dayOfWeek = nextDay,
                                        startTimeStr = "09:00",
                                        endTimeStr = "10:30",
                                        repeatMode = "每週",
                                        selectedWeeks = (1..18).toSet(),
                                        location = timeSlots.lastOrNull()?.location ?: ""
                                    )
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "新增",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                if (!isTimeTBD) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        timeSlots.forEachIndexed { index, slot ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            weekdays.forEach { (day, label) ->
                                                val isSelected = slot.dayOfWeek == day
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .padding(horizontal = 2.dp)
                                                        .height(36.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(
                                                            if (isSelected) MaterialTheme.colorScheme.primary
                                                            else Color.Transparent
                                                        )
                                                        .clickable {
                                                            updateSlot(index) { it.copy(dayOfWeek = day) }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = label,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }

                                        if (timeSlots.size > 1) {
                                            Spacer(modifier = Modifier.width(8.dp))

                                            Box(
                                                modifier = Modifier
                                                    .size(26.dp)
                                                    .border(
                                                        1.5.dp,
                                                        Color(0xFFEF4444),
                                                        androidx.compose.foundation.shape.CircleShape
                                                    )
                                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                                    .clickable {
                                                        timeSlots = timeSlots.filterIndexed { i, _ -> i != index }
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .width(12.dp)
                                                        .height(2.dp)
                                                        .background(Color(0xFFEF4444))
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { activeRepeatSlotIndex = index }
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "重複",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = slot.repeatMode,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable {
                                                    activeTimePickerSlotIndex = index
                                                    isPickingStartTime = true
                                                }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = slot.startTimeStr,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }

                                        Text(
                                            text = "~",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable {
                                                    activeTimePickerSlotIndex = index
                                                    isPickingStartTime = false
                                                }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = slot.endTimeStr,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }

                                    OutlinedTextField(
                                        value = slot.location,
                                        onValueChange = { loc ->
                                            updateSlot(index) { it.copy(location = loc) }
                                        },
                                        placeholder = { Text("教室 (選填)") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (conflictingCourseInfo != null) {
                val (conflictCourse, conflictSlot) = conflictingCourseInfo
                val conflictDayLabel = weekdays.firstOrNull { it.first == conflictSlot.dayOfWeek }?.second ?: "${conflictSlot.dayOfWeek}"
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
                            text = "課程時間重疊（衝堂）：週$conflictDayLabel 與「${conflictCourse.name}」(第 ${conflictCourse.startPeriod}~${conflictCourse.endPeriod} 節) 衝突，無法儲存",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "背景顏色",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    colorPalette.forEach { hex ->
                        val color = Color(hex.toColorInt())
                        val isSelected = colorHex.equals(hex, ignoreCase = true)
                        // Increased touch target size to 48dp for accessibility
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clickable { colorHex = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(color),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("取消", style = MaterialTheme.typography.bodyLarge)
                }

                Button(
                    onClick = {
                        val finalCategory = category
                        val finalRequirementType = requirementType

                        if (name.isBlank()) {
                            return@Button
                        }
                        if (finalCategory == null) {
                            Toast.makeText(context, "請選擇學分屬性", Toast.LENGTH_SHORT).show()
                            otherInfoExpanded = true
                            return@Button
                        }
                        if (finalRequirementType == null) {
                            Toast.makeText(context, "請選擇修別", Toast.LENGTH_SHORT).show()
                            otherInfoExpanded = true
                            return@Button
                        }

                        val credits = creditsText.toDoubleOrNull() ?: 3.0
                        val matchedGeneralSubtype = GeneralEduSubtype.entries.firstOrNull { it.label == subcategoryText.trim() } ?: GeneralEduSubtype.NONE
                        val cleanSubcategory = subcategoryText.trim()

                        if (isTimeTBD) {
                            val course = (initialCourse ?: Course(name = name)).copy(
                                name = name.trim(),
                                code = code.trim(),
                                teacher = teacher.trim(),
                                location = "",
                                dayOfWeek = 1,
                                startPeriod = 1,
                                endPeriod = 1,
                                startTime = "",
                                endTime = "",
                                credits = credits,
                                category = finalCategory,
                                customCategory = customCategoryName.trim(),
                                requirementType = finalRequirementType,
                                generalEduSubtype = if (finalCategory == CourseCategory.GENERAL_EDU) matchedGeneralSubtype else GeneralEduSubtype.NONE,
                                subcategory = cleanSubcategory,
                                semester = semester.trim(),
                                score = initialCourse?.score,
                                letterGrade = initialCourse?.letterGrade,
                                isCompleted = initialCourse?.isCompleted ?: false,
                                colorHex = colorHex,
                                notes = notes.trim(),
                                repeatWeeks = "1-18",
                                repeatMode = "每週"
                            )
                            onSave(course)
                        } else {
                            val coursesToSave = timeSlots.mapIndexed { idx, slot ->
                                val sHour = slot.startTimeStr.substringBefore(":").toIntOrNull() ?: 9
                                val eHour = slot.endTimeStr.substringBefore(":").toIntOrNull() ?: 10
                                val eMin = slot.endTimeStr.substringAfter(":").toIntOrNull() ?: 30
                                val startP = (sHour - 7).coerceIn(0, 15)
                                val endP = (if (eMin > 10) eHour - 7 else eHour - 8).coerceIn(startP, 15)
                                val repWeeks = if (slot.repeatMode == "每週") "1-18" else slot.selectedWeeks.sorted().joinToString(",")

                                (if (idx == 0 && initialCourse != null) initialCourse else Course(name = name)).copy(
                                    name = name.trim(),
                                    code = code.trim(),
                                    teacher = teacher.trim(),
                                    location = slot.location.trim(),
                                    dayOfWeek = slot.dayOfWeek,
                                    startPeriod = startP,
                                    endPeriod = endP,
                                    startTime = slot.startTimeStr,
                                    endTime = slot.endTimeStr,
                                    credits = if (idx == 0) credits else 0.0,
                                    category = finalCategory,
                                    customCategory = customCategoryName.trim(),
                                    requirementType = finalRequirementType,
                                    generalEduSubtype = if (finalCategory == CourseCategory.GENERAL_EDU) matchedGeneralSubtype else GeneralEduSubtype.NONE,
                                    subcategory = cleanSubcategory,
                                    semester = semester.trim(),
                                    score = if (idx == 0) initialCourse?.score else null,
                                    letterGrade = if (idx == 0) initialCourse?.letterGrade else null,
                                    isCompleted = if (idx == 0) (initialCourse?.isCompleted ?: false) else false,
                                    colorHex = colorHex,
                                    notes = notes.trim(),
                                    repeatWeeks = repWeeks,
                                    repeatMode = slot.repeatMode
                                )
                            }
                            onSaveMultiple?.invoke(coursesToSave) ?: coursesToSave.forEach { onSave(it) }
                        }
                    },
                    enabled = name.isNotBlank() && (isTimeTBD || conflictingCourseInfo == null),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.5f)
                        .height(48.dp)
                        .testTag("save_course_button")
                ) {
                    Text("完成", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    activeRepeatSlotIndex?.let { slotIdx ->
        val currentSlot = timeSlots.getOrNull(slotIdx) ?: return@let
        ModalBottomSheet(
            onDismissRequest = { activeRepeatSlotIndex = null },
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RepeatOptionItem(
                    title = "每週",
                    subtitle = null,
                    isSelected = currentSlot.repeatMode == "每週",
                    onClick = {
                        updateSlot(slotIdx) { it.copy(repeatMode = "每週", selectedWeeks = (1..18).toSet()) }
                        activeRepeatSlotIndex = null
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                RepeatOptionItem(
                    title = "單週",
                    subtitle = "所有單數週 (1、3、5、7...)",
                    isSelected = currentSlot.repeatMode == "單週",
                    onClick = {
                        updateSlot(slotIdx) { it.copy(repeatMode = "單週", selectedWeeks = (1..18).filter { w -> w % 2 != 0 }.toSet()) }
                        activeRepeatSlotIndex = null
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                RepeatOptionItem(
                    title = "雙週",
                    subtitle = "所有雙數週 (2、4、6、8...)",
                    isSelected = currentSlot.repeatMode == "雙週",
                    onClick = {
                        updateSlot(slotIdx) { it.copy(repeatMode = "雙週", selectedWeeks = (1..18).filter { w -> w % 2 == 0 }.toSet()) }
                        activeRepeatSlotIndex = null
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            activeSpecificWeeksSlotIndex = slotIdx
                            activeRepeatSlotIndex = null
                        }
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "指定週次",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (currentSlot.repeatMode.startsWith("第") || currentSlot.repeatMode == "指定週次") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (currentSlot.repeatMode.startsWith("第")) currentSlot.repeatMode else "例如只上前八週",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    activeSpecificWeeksSlotIndex?.let { slotIdx ->
        val currentSlot = timeSlots.getOrNull(slotIdx) ?: return@let
        var tempWeeks by remember(currentSlot) { mutableStateOf(currentSlot.selectedWeeks) }

        ModalBottomSheet(
            onDismissRequest = { activeSpecificWeeksSlotIndex = null },
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "指定週次",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = {
                            tempWeeks = if (tempWeeks.size == 18) emptySet() else (1..18).toSet()
                        }
                    ) {
                        Text(if (tempWeeks.size == 18) "取消全選" else "全選")
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items((1..18).toList()) { w ->
                        val isSelected = w in tempWeeks
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .clickable {
                                    tempWeeks = if (isSelected) tempWeeks - w else tempWeeks + w
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$w",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        val newMode = if (tempWeeks.size == 18) {
                            "每週"
                        } else if (tempWeeks.isEmpty()) {
                            "未選週次"
                        } else {
                            "第 ${tempWeeks.sorted().joinToString(",")} 週"
                        }
                        updateSlot(slotIdx) { it.copy(repeatMode = newMode, selectedWeeks = tempWeeks) }
                        activeSpecificWeeksSlotIndex = null
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("確認", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }


    // Start / End Time Picker Dialog
    activeTimePickerSlotIndex?.let { slotIdx ->
        val currentSlot = timeSlots.getOrNull(slotIdx) ?: return@let
        val sHour = currentSlot.startTimeStr.substringBefore(":").toIntOrNull() ?: 9
        val sMin = currentSlot.startTimeStr.substringAfter(":").toIntOrNull() ?: 0
        val eHour = currentSlot.endTimeStr.substringBefore(":").toIntOrNull() ?: 10
        val eMin = currentSlot.endTimeStr.substringAfter(":").toIntOrNull() ?: 30

        if (isPickingStartTime) {
            TimeSelectPickerDialog(
                title = "選擇開始時間",
                initialHour = sHour,
                initialMinute = sMin,
                onDismiss = { activeTimePickerSlotIndex = null },
                onConfirm = { h, m ->
                    val newStartStr = String.format(Locale.US, "%02d:%02d", h, m)
                    updateSlot(slotIdx) { it.copy(startTimeStr = newStartStr) }
                    activeTimePickerSlotIndex = null
                }
            )
        } else {
            TimeSelectPickerDialog(
                title = "選擇結束時間",
                initialHour = eHour,
                initialMinute = eMin,
                onDismiss = { activeTimePickerSlotIndex = null },
                onConfirm = { h, m ->
                    val newEndStr = String.format(Locale.US, "%02d:%02d", h, m)
                    updateSlot(slotIdx) { it.copy(endTimeStr = newEndStr) }
                    activeTimePickerSlotIndex = null
                }
            )
        }
    }
}

@Composable
private fun RepeatOptionItem(
    title: String,
    subtitle: String?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeSelectPickerDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title, fontWeight = FontWeight.Bold)
        },
        text = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(state = state)
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(state.hour, state.minute) }) {
                Text("確定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
