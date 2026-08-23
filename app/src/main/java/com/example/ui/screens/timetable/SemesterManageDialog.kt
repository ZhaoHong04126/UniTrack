package com.example.ui.screens.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SemesterManageDialog(
    selectedSemester: String,
    primarySemester: String,
    allSemesters: List<String>,
    admissionSemester: String,
    onSelectSemester: (String) -> Unit,
    onSetPrimarySemester: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var showAddSection by remember { mutableStateOf(false) }
    var newYearInput by remember(admissionSemester) {
        mutableStateOf(admissionSemester.substringBefore("-").filter { it.isDigit() }.ifBlank { "114" })
    }
    var selectedTerm by remember { mutableStateOf("上學期") }
    var setAsPrimaryChecked by remember { mutableStateOf(false) }
    var termDropdownExpanded by remember { mutableStateOf(false) }
    val termOptions = listOf("上學期", "下學期", "暑期")

    fun formatSemesterLabel(sem: String): String {
        val startYear = admissionSemester.substringBefore("-").filter { it.isDigit() }.toIntOrNull() ?: 114
        val year = sem.substringBefore("-").filter { it.isDigit() }.toIntOrNull()
        val term = sem.substringAfter("-").filter { it.isDigit() }.toIntOrNull() ?: 1
        if (year != null) {
            val grade = when (val diff = year - startYear) {
                0 -> "大一"
                1 -> "大二"
                2 -> "大三"
                3 -> "大四"
                else -> if (diff > 3) "延畢" else ""
            }
            val termStr = if (term == 1) "上" else "下"
            if (grade.isNotEmpty()) {
                return "$sem 學期 ($grade$termStr)"
            }
        }
        return "$sem 學期"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "學期管理",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "共 ${allSemesters.size} 個學期",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // List of Semesters
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(allSemesters) { sem ->
                            val isSelected = sem == selectedSemester
                            val isPrimary = sem == primarySemester

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                    )
                                    .clickable {
                                        onSelectSemester(sem)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "查看中",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.size(18.dp))
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = formatSemesterLabel(sem),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                                else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isPrimary) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Filled.Star,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Text(
                                                            text = "主要",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Set as primary action
                                if (!isPrimary) {
                                    FilledTonalButton(
                                        onClick = { onSetPrimarySemester(sem) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.StarBorder,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "設為主要",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Add Semester Section
                if (!showAddSection) {
                    OutlinedButton(
                        onClick = { showAddSection = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("新增自訂學期")
                    }
                } else {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "新增自訂學期",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newYearInput,
                                    onValueChange = { str ->
                                        if (str.all { it.isDigit() } && str.length <= 4) {
                                            newYearInput = str
                                        }
                                    },
                                    label = { Text("學年") },
                                    placeholder = { Text("例如：114") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )

                                ExposedDropdownMenuBox(
                                    expanded = termDropdownExpanded,
                                    onExpandedChange = { termDropdownExpanded = !termDropdownExpanded },
                                    modifier = Modifier.weight(1.2f)
                                ) {
                                    OutlinedTextField(
                                        value = selectedTerm,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("學期") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = termDropdownExpanded) },
                                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = termDropdownExpanded,
                                        onDismissRequest = { termDropdownExpanded = false }
                                    ) {
                                        termOptions.forEach { term ->
                                            DropdownMenuItem(
                                                text = { Text(term) },
                                                onClick = {
                                                    selectedTerm = term
                                                    termDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { setAsPrimaryChecked = !setAsPrimaryChecked }
                                    .padding(vertical = 2.dp)
                            ) {
                                Checkbox(
                                    checked = setAsPrimaryChecked,
                                    onCheckedChange = { setAsPrimaryChecked = it }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "同時設為主要學期",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { showAddSection = false }) {
                                    Text("收起")
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Button(
                                    onClick = {
                                        if (newYearInput.isNotBlank()) {
                                            val termSuffix = when (selectedTerm) {
                                                "上學期" -> "1"
                                                "下學期" -> "2"
                                                "暑期" -> "暑"
                                                else -> "1"
                                            }
                                            val semesterCode = "${newYearInput.trim()}-$termSuffix"
                                            onSelectSemester(semesterCode)
                                            if (setAsPrimaryChecked) {
                                                onSetPrimarySemester(semesterCode)
                                            }
                                            showAddSection = false
                                        }
                                    },
                                    enabled = newYearInput.isNotBlank()
                                ) {
                                    Text("新增並切換")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("完成")
            }
        }
    )
}
