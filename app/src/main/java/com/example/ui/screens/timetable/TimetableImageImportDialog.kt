package com.example.ui.screens.timetable

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.core.graphics.toColorInt
import com.example.data.model.Course
import com.example.ui.theme.SapphirePrimary
import com.example.util.GeminiTimetableParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private enum class ImportStep {
    PICK_SOURCE,
    ANALYZING,
    REVIEW,
    ERROR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableImageImportDialog(
    initialSemester: String,
    allSemesters: List<String>,
    allCourses: List<Course> = emptyList(),
    onDismiss: () -> Unit,
    onConfirmImport: (List<Course>) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentStep by remember { mutableStateOf(ImportStep.PICK_SOURCE) }
    var selectedSemester by remember { mutableStateOf(initialSemester) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var recognizedCourses by remember { mutableStateOf<List<Course>>(emptyList()) }
    var selectedCourseIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var errorMessage by remember { mutableStateOf("") }
    var conflictAlertMessage by remember { mutableStateOf<String?>(null) }
    var editingCourseIndex by remember { mutableStateOf<Int?>(null) }

    // Helper: check if two courses have overlapping time slots
    fun isOverlapping(c1: Course, c2: Course): Boolean {
        if (c1.dayOfWeek != c2.dayOfWeek) return false
        return maxOf(c1.startPeriod, c2.startPeriod) <= minOf(c1.endPeriod, c2.endPeriod)
    }

    // Helper: auto select non-conflicting courses
    fun computeInitialSelection(courses: List<Course>, semester: String): Set<Int> {
        val selected = mutableSetOf<Int>()
        courses.forEachIndexed { idx, course ->
            val hasDbConflict = allCourses.any { it.semester == semester && isOverlapping(course, it) }
            val hasBatchConflict = selected.any { isOverlapping(course, courses[it]) }
            if (!hasDbConflict && !hasBatchConflict) {
                selected.add(idx)
            }
        }
        return selected
    }

    // Temporary camera image Uri
    val tempCameraUri = remember {
        val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
        val imageFile = File(imagesDir, "camera_timetable_temp_${System.currentTimeMillis()}.jpg")
        FileProvider.getUriForFile(context, "${context.packageName}.provider", imageFile)
    }

    // Process image function
    fun processImageUri(uri: Uri) {
        currentStep = ImportStep.ANALYZING
        errorMessage = ""
        coroutineScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                GeminiTimetableParser.loadAndResizeBitmap(context, uri)
            }
            if (bitmap == null) {
                errorMessage = "無法讀取所選圖片，請重試。"
                currentStep = ImportStep.ERROR
                return@launch
            }
            previewBitmap = bitmap

            val result = GeminiTimetableParser.parseTimetableImage(bitmap, selectedSemester)
            if (result.isSuccess) {
                val courses = result.getOrNull() ?: emptyList()
                recognizedCourses = courses
                selectedCourseIndices = computeInitialSelection(courses, selectedSemester)
                currentStep = ImportStep.REVIEW
            } else {
                errorMessage = result.exceptionOrNull()?.localizedMessage ?: "辨識失敗，請確認圖片清晰並重試。"
                currentStep = ImportStep.ERROR
            }
        }
    }

    // Photo Gallery Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            processImageUri(uri)
        } else if (currentStep == ImportStep.PICK_SOURCE) {
            onDismiss()
        }
    }

    // Camera Capture Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            processImageUri(tempCameraUri)
        } else if (currentStep == ImportStep.PICK_SOURCE) {
            onDismiss()
        }
    }

    when (currentStep) {
        ImportStep.PICK_SOURCE -> {
            ModalBottomSheet(
                onDismissRequest = onDismiss,
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📸 課表照片智慧導入",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "自動識別照片中的課表結構，快速匯入所有課程資訊",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Camera option
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(120.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    cameraLauncher.launch(tempCameraUri)
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoCamera,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "拍照",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Gallery option
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(120.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    galleryLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoLibrary,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "從相簿選取",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))
                }
            }
        }

        ImportStep.ANALYZING -> {
            Dialog(
                onDismissRequest = { /* Don't dismiss while analyzing */ },
                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        previewBitmap?.let { bmp ->
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(
                                        2.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                        RoundedCornerShape(16.dp)
                                    )
                            ) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "課表縮圖",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                        }

                        CircularProgressIndicator(
                            color = SapphirePrimary,
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(48.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "正在辨識課表...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "正在精準分析課程名稱、星期、節次、教室與教師資訊",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }

        ImportStep.REVIEW -> {
            val windowInfo = LocalWindowInfo.current
            val density = LocalDensity.current
            val halfScreenHeight = with(density) { (windowInfo.containerSize.height * 0.55f).toDp() }

            ModalBottomSheet(
                onDismissRequest = onDismiss,
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(halfScreenHeight)
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "✨ 課表辨識結果",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "共識別出 ${recognizedCourses.size} 門課程，可點擊鉛筆修改內容",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "關閉"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Target Semester Selector & Select All Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Semester dropdown
                        var semExpanded by remember { mutableStateOf(false) }
                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { semExpanded = true }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "學期：$selectedSemester",
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SapphirePrimary
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = SapphirePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = semExpanded,
                                onDismissRequest = { semExpanded = false }
                            ) {
                                allSemesters.forEach { sem ->
                                    DropdownMenuItem(
                                        text = { Text(sem) },
                                        onClick = {
                                            selectedSemester = sem
                                            semExpanded = false
                                            // Re-evaluate initial selection on semester switch
                                            selectedCourseIndices = computeInitialSelection(recognizedCourses, sem)
                                        }
                                    )
                                }
                            }
                        }

                        // Non-conflicting indices in current semester
                        val nonConflictingIndices = remember(recognizedCourses, selectedSemester, allCourses) {
                            recognizedCourses.indices.filter { idx ->
                                val c = recognizedCourses[idx]
                                !allCourses.any { it.semester == selectedSemester && isOverlapping(c, it) }
                            }.toSet()
                        }

                        // Select All toggle
                        val allNonConflictingSelected = nonConflictingIndices.isNotEmpty() && selectedCourseIndices.containsAll(nonConflictingIndices)
                        TextButton(
                            onClick = {
                                selectedCourseIndices = if (allNonConflictingSelected) {
                                    emptySet()
                                } else {
                                    nonConflictingIndices
                                }
                            },
                            enabled = nonConflictingIndices.isNotEmpty(),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (allNonConflictingSelected) "取消全選" else "全選",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Course List
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(recognizedCourses) { index, course ->
                            // Check conflict with DB courses
                            val dbConflict = allCourses.firstOrNull {
                                it.semester == selectedSemester && isOverlapping(course, it)
                            }
                            val isConflictedWithDb = dbConflict != null

                            val isSelected = selectedCourseIndices.contains(index) && !isConflictedWithDb

                            // Check conflict with other selected courses in this imported batch
                            val batchConflict = if (isSelected) {
                                recognizedCourses.filterIndexed { idx, other ->
                                    idx != index && selectedCourseIndices.contains(idx) && !allCourses.any { it.semester == selectedSemester && isOverlapping(other, it) } && isOverlapping(course, other)
                                }.firstOrNull()
                            } else null

                            val conflictReason = when {
                                dbConflict != null -> {
                                    val pStr = if (dbConflict.startPeriod == dbConflict.endPeriod) "第 ${dbConflict.startPeriod} 節" else "第 ${dbConflict.startPeriod}-${dbConflict.endPeriod} 節"
                                    "與現有「${dbConflict.name}」($pStr) 時間重疊"
                                }
                                batchConflict != null -> {
                                    val pStr = if (batchConflict.startPeriod == batchConflict.endPeriod) "第 ${batchConflict.startPeriod} 節" else "第 ${batchConflict.startPeriod}-${batchConflict.endPeriod} 節"
                                    "與所選「${batchConflict.name}」($pStr) 時間重疊"
                                }
                                else -> null
                            }

                            RecognizedCourseCard(
                                course = course,
                                isSelected = isSelected,
                                isSelectable = !isConflictedWithDb,
                                conflictReason = conflictReason,
                                onToggle = {
                                    if (isConflictedWithDb) {
                                        android.widget.Toast.makeText(context, "此課程與現有課表時間重疊，可點擊鉛筆修改時段", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        selectedCourseIndices = if (isSelected) {
                                            selectedCourseIndices - index
                                        } else {
                                            selectedCourseIndices + index
                                        }
                                    }
                                },
                                onEdit = {
                                    editingCourseIndex = index
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Bottom Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                currentStep = ImportStep.PICK_SOURCE
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("重新選取")
                        }

                        val validSelectedCourses = remember(recognizedCourses, selectedCourseIndices, selectedSemester, allCourses) {
                            recognizedCourses.filterIndexed { idx, course ->
                                selectedCourseIndices.contains(idx) && !allCourses.any { it.semester == selectedSemester && isOverlapping(course, it) }
                            }
                        }

                        Button(
                            onClick = {
                                // Check intra-batch conflicts
                                val conflictList = mutableListOf<String>()
                                for (i in validSelectedCourses.indices) {
                                    for (j in (i + 1) until validSelectedCourses.size) {
                                        if (isOverlapping(validSelectedCourses[i], validSelectedCourses[j])) {
                                            conflictList.add("「${validSelectedCourses[i].name}」與「${validSelectedCourses[j].name}」時段重疊")
                                        }
                                    }
                                }

                                if (conflictList.isNotEmpty()) {
                                    conflictAlertMessage = conflictList.distinct().joinToString("\n• ", prefix = "• ")
                                } else {
                                    val coursesToImport = validSelectedCourses.map { it.copy(semester = selectedSemester) }
                                    onConfirmImport(coursesToImport)
                                    onDismiss()
                                }
                            },
                            enabled = validSelectedCourses.isNotEmpty(),
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SapphirePrimary)
                        ) {
                            Text("確認導入 (${validSelectedCourses.size} 門課)")
                        }
                    }
                }
            }

            // Edit Dialog for recognized course (using the exact same AddEditCourseDialog)
            editingCourseIndex?.let { editIdx ->
                if (editIdx in recognizedCourses.indices) {
                    AddEditCourseDialog(
                        initialCourse = recognizedCourses[editIdx],
                        defaultSemester = selectedSemester,
                        allCourses = allCourses,
                        onDismiss = { editingCourseIndex = null },
                        onSave = { updatedCourse ->
                            val updatedList = recognizedCourses.toMutableList()
                            updatedList[editIdx] = updatedCourse
                            recognizedCourses = updatedList

                            // Check if edited course now has conflict with DB
                            val hasDbConflict = allCourses.any { it.semester == selectedSemester && isOverlapping(updatedCourse, it) }
                            selectedCourseIndices = if (!hasDbConflict) {
                                selectedCourseIndices + editIdx
                            } else {
                                selectedCourseIndices - editIdx
                            }
                            editingCourseIndex = null
                        }
                    )
                }
            }

            // Conflict Warning Alert Dialog
            conflictAlertMessage?.let { msg ->
                AlertDialog(
                    onDismissRequest = { conflictAlertMessage = null },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(32.dp)
                        )
                    },
                    title = {
                        Text(
                            text = "課程時間衝突",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = "發現以下選取課程與現有課表或彼此間時間重疊，系統禁止重疊排課：",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "請取消勾選衝突課程或點擊鉛筆修改節次後再行導入。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { conflictAlertMessage = null },
                            colors = ButtonDefaults.buttonColors(containerColor = SapphirePrimary)
                        ) {
                            Text("我知道了，我去調整")
                        }
                    }
                )
            }
        }

        ImportStep.ERROR -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        text = "辨識失敗",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = errorMessage.ifBlank { "未能成功從圖片中辨識出課表資訊，請確保課表清晰並重新嘗試。" },
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            currentStep = ImportStep.PICK_SOURCE
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SapphirePrimary)
                    ) {
                        Text("重新選取照片")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

@Composable
private fun RecognizedCourseCard(
    course: Course,
    isSelected: Boolean,
    isSelectable: Boolean = true,
    conflictReason: String? = null,
    onToggle: () -> Unit,
    onEdit: () -> Unit
) {
    val dayName = when (course.dayOfWeek) {
        1 -> "週一"
        2 -> "週二"
        3 -> "週三"
        4 -> "週四"
        5 -> "週五"
        6 -> "週六"
        7 -> "週日"
        else -> "週一"
    }

    val periodRange = if (course.startPeriod == course.endPeriod) {
        "第 ${course.startPeriod} 節"
    } else {
        "第 ${course.startPeriod}-${course.endPeriod} 節"
    }

    val cardColor = runCatching { course.colorHex.toColorInt() }.getOrDefault(android.graphics.Color.BLUE)
    val hasConflict = conflictReason != null

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onToggle() },
        shape = RoundedCornerShape(14.dp),
        color = when {
            !isSelectable -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
            hasConflict && isSelected -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
            isSelected -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        },
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = when {
                !isSelectable -> MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
                hasConflict && isSelected -> MaterialTheme.colorScheme.error
                isSelected -> SapphirePrimary
                hasConflict -> MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Checkbox(
                checked = isSelected && isSelectable,
                enabled = isSelectable,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = if (hasConflict) MaterialTheme.colorScheme.error else SapphirePrimary,
                    disabledUncheckedColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )

            // Color bar indicator
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(if (hasConflict) 64.dp else 48.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (hasConflict && isSelected) MaterialTheme.colorScheme.error else Color(cardColor))
            )

            // Course Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = course.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Credit tag
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "${course.credits.toInt()} 學分",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        // Edit icon button
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "修改課程",
                                tint = SapphirePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Time & Location Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$dayName $periodRange",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (course.location.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = course.location,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (course.teacher.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = course.teacher,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Conflict Alert Tag
                if (hasConflict) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = conflictReason,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

