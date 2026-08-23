package com.example.ui.screens.expense

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.ExpenseCategory
import com.example.data.model.ExpenseRecord
import com.example.data.model.ExpenseType
import com.example.data.model.PaymentMethod
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditExpenseDialog(
    initialExpense: ExpenseRecord? = null,
    onDismiss: () -> Unit,
    onSave: (ExpenseRecord) -> Unit,
    onDelete: ((ExpenseRecord) -> Unit)? = null
) {
    val locale = LocalConfiguration.current.locales[0]
    val dateFormat = remember(locale) { SimpleDateFormat("yyyy-MM-dd", locale) }

    var title by remember { mutableStateOf(initialExpense?.title ?: "") }
    var amountText by remember { mutableStateOf(initialExpense?.amount?.toInt()?.toString() ?: "") }
    var type by remember { mutableStateOf(initialExpense?.type ?: ExpenseType.EXPENSE) }
    var category by remember { mutableStateOf(initialExpense?.category ?: ExpenseCategory.FOOD) }
    var paymentMethod by remember { mutableStateOf(initialExpense?.paymentMethod ?: PaymentMethod.CASH) }
    var dateString by remember { mutableStateOf(initialExpense?.dateString ?: dateFormat.format(Date())) }
    var note by remember { mutableStateOf(initialExpense?.note ?: "") }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var paymentDropdownExpanded by remember { mutableStateOf(false) }
    var presetDropdownExpanded by remember { mutableStateOf(false) }

    val quickExpensePresets = listOf(
        "學餐午餐" to ExpenseCategory.FOOD,
        "咖啡/手搖飲" to ExpenseCategory.FOOD,
        "教科書/講義" to ExpenseCategory.BOOKS_STUDY,
        "通勤公車/捷運" to ExpenseCategory.TRANSPORT,
        "房租水電" to ExpenseCategory.RENT_UTILITY,
        "生活用品" to ExpenseCategory.DAILY,
        "家教/工讀薪資" to ExpenseCategory.SALARY_JOB,
        "獎助學金" to ExpenseCategory.SCHOLARSHIP
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialExpense == null) "記一筆收支" else "編輯收支記錄",
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
                // Type Switcher (Expense vs Income)
                Row(modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = type == ExpenseType.EXPENSE,
                        onClick = { type = ExpenseType.EXPENSE },
                        label = {
                            Text(
                                "支出",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                fontWeight = if (type == ExpenseType.EXPENSE) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RoseLight,
                            selectedLabelColor = RoseAccent
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = type == ExpenseType.EXPENSE,
                            selectedBorderColor = RoseAccent
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = type == ExpenseType.INCOME,
                        onClick = {
                            type = ExpenseType.INCOME
                            if (category == ExpenseCategory.FOOD) category = ExpenseCategory.SALARY_JOB
                        },
                        label = {
                            Text(
                                "收入",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                fontWeight = if (type == ExpenseType.INCOME) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldLight,
                            selectedLabelColor = EmeraldAccent
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = type == ExpenseType.INCOME,
                            selectedBorderColor = EmeraldAccent
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("金額 ($) *") },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_amount_input")
                )

                // Title with Quick Presets Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("項目說明 *") },
                        placeholder = { Text("例如：午餐排骨飯、微積分課本") },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(
                                onClick = { presetDropdownExpanded = true },
                                modifier = Modifier.testTag("quick_presets_dropdown_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "常用快捷項目"
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("expense_title_input")
                    )

                    DropdownMenu(
                        expanded = presetDropdownExpanded,
                        onDismissRequest = { presetDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "常用快捷項目",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            onClick = {},
                            enabled = false
                        )
                        HorizontalDivider()
                        quickExpensePresets.forEach { (presetTitle, presetCat) ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(presetTitle, style = MaterialTheme.typography.bodyMedium)
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text(
                                            text = presetCat.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    title = presetTitle
                                    category = presetCat
                                    type = if (presetCat == ExpenseCategory.SALARY_JOB || presetCat == ExpenseCategory.SCHOLARSHIP) {
                                        ExpenseType.INCOME
                                    } else {
                                        ExpenseType.EXPENSE
                                    }
                                    presetDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Category & Payment Method
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = categoryDropdownExpanded,
                        onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = category.label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("分類") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = categoryDropdownExpanded,
                            onDismissRequest = { categoryDropdownExpanded = false }
                        ) {
                            ExpenseCategory.entries.forEach { cat ->
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

                    ExposedDropdownMenuBox(
                        expanded = paymentDropdownExpanded,
                        onExpandedChange = { paymentDropdownExpanded = !paymentDropdownExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = paymentMethod.label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("支付方式") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentDropdownExpanded) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = paymentDropdownExpanded,
                            onDismissRequest = { paymentDropdownExpanded = false }
                        ) {
                            PaymentMethod.entries.forEach { method ->
                                DropdownMenuItem(
                                    text = { Text(method.label) },
                                    onClick = {
                                        paymentMethod = method
                                        paymentDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Date
                OutlinedTextField(
                    value = dateString,
                    onValueChange = { dateString = it },
                    label = { Text("日期 (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Note
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("備註 (選填)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amt > 0) {
                        val record = (initialExpense ?: ExpenseRecord(title = title, amount = amt)).copy(
                            title = title.trim(),
                            amount = amt,
                            type = type,
                            category = category,
                            paymentMethod = paymentMethod,
                            dateString = dateString.trim(),
                            note = note.trim()
                        )
                        onSave(record)
                    }
                },
                enabled = title.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0,
                modifier = Modifier.testTag("save_expense_button")
            ) {
                Text("儲存記錄")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (initialExpense != null && onDelete != null) {
                    TextButton(
                        onClick = { onDelete(initialExpense) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("delete_expense_button")
                    ) {
                        Text("刪除")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        }
    )
}

@Composable
fun BudgetDialog(
    currentBudget: Double,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var budgetText by remember { mutableStateOf(if (currentBudget > 0) currentBudget.toInt().toString() else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("設定每月預算上限", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "設定當月可支配生活開銷預算，APP 將即時追蹤剩餘額度並提醒超支：",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = budgetText,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.all { it.isDigit() }) {
                            budgetText = input
                        }
                    },
                    label = { Text("月預算額度 ($)") },
                    placeholder = { Text("請輸入預算金額") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = budgetText.toDoubleOrNull() ?: 10000.0
                    onSave(amount)
                },
                enabled = (budgetText.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("確認設定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
