package com.example.ui.screens.graduation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.GraduationPlan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraduationPlanDialog(
    currentPlan: GraduationPlan,
    onDismiss: () -> Unit,
    onSave: (GraduationPlan) -> Unit
) {
    var department by remember { mutableStateOf(currentPlan.department) }
    var studentName by remember { mutableStateOf(currentPlan.studentName) }
    var totalTarget by remember { mutableStateOf(currentPlan.targetTotalCredits.toString()) }
    var genTarget by remember { mutableStateOf(currentPlan.targetGeneralCredits.toString()) }
    var colTarget by remember { mutableStateOf(currentPlan.targetCollegeCoreCredits.toString()) }
    var basTarget by remember { mutableStateOf(currentPlan.targetBasicModuleCredits.toString()) }
    var corTarget by remember { mutableStateOf(currentPlan.targetCoreModuleCredits.toString()) }
    var proTarget by remember { mutableStateOf(currentPlan.targetProfessionalModuleCredits.toString()) }
    var freeTarget by remember { mutableStateOf(currentPlan.targetFreeCredits.toString()) }
    var currentSemester by remember { mutableStateOf(currentPlan.currentSemester) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "設定畢業審查標準",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = studentName,
                        onValueChange = { studentName = it },
                        label = { Text("學生姓名/稱呼") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = currentSemester,
                        onValueChange = { currentSemester = it },
                        label = { Text("當前學期") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = department,
                    onValueChange = { department = it },
                    label = { Text("就讀科系所") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "畢業學分目標設定",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = totalTarget,
                    onValueChange = { totalTarget = it },
                    label = { Text("總畢業學分目標 *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("total_credits_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = genTarget,
                        onValueChange = { genTarget = it },
                        label = { Text("通識學分") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = colTarget,
                        onValueChange = { colTarget = it },
                        label = { Text("院共同學分") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = basTarget,
                        onValueChange = { basTarget = it },
                        label = { Text("基礎模組") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = corTarget,
                        onValueChange = { corTarget = it },
                        label = { Text("核心模組") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = proTarget,
                        onValueChange = { proTarget = it },
                        label = { Text("專業模組") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = freeTarget,
                        onValueChange = { freeTarget = it },
                        label = { Text("自由選修") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = currentPlan.copy(
                        department = department.trim(),
                        studentName = studentName.trim(),
                        targetTotalCredits = totalTarget.toDoubleOrNull() ?: 128.0,
                        targetRequiredCredits = currentPlan.targetRequiredCredits, // Keep these
                        targetElectiveCredits = currentPlan.targetElectiveCredits, // Keep these
                        targetGeneralCredits = genTarget.toDoubleOrNull() ?: 28.0,
                        targetCollegeCoreCredits = colTarget.toDoubleOrNull() ?: 9.0,
                        targetBasicModuleCredits = basTarget.toDoubleOrNull() ?: 24.0,
                        targetCoreModuleCredits = corTarget.toDoubleOrNull() ?: 24.0,
                        targetProfessionalModuleCredits = proTarget.toDoubleOrNull() ?: 23.0,
                        targetFreeCredits = freeTarget.toDoubleOrNull() ?: 20.0,
                        currentSemester = currentSemester.trim(),
                        gpaScale = currentPlan.gpaScale
                    )
                    onSave(updated)
                },
                modifier = Modifier.testTag("save_plan_button")
            ) {
                Text("儲存設定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Suppress("SpellCheckingInspection")
@Composable
fun AddThresholdDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var proofNote by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增額外條件項目", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("項目名稱 *") },
                    placeholder = { Text("例如：英文檢定") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("項目要求說明") },
                    placeholder = { Text("例如：TOEIC 750分以上") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = proofNote,
                    onValueChange = { proofNote = it },
                    label = { Text("備註 / 達成進度") },
                    placeholder = { Text("例如：未達目標") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(title.trim(), description.trim(), proofNote.trim())
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("新增項目")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
