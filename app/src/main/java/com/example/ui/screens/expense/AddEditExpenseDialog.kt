package com.example.ui.screens.expense

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.ExpenseCategory
import com.example.data.model.ExpenseRecord
import com.example.data.model.ExpenseType
import com.example.data.model.PaymentMethod
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditExpenseDialog(
    initialExpense: ExpenseRecord? = null,
    onDismiss: () -> Unit,
    onSave: (ExpenseRecord) -> Unit
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
                        label = { Text("支出") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = type == ExpenseType.INCOME,
                        onClick = {
                            type = ExpenseType.INCOME
                            if (category == ExpenseCategory.FOOD) category = ExpenseCategory.SALARY_JOB
                        },
                        label = { Text("收入") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Quick Presets
                Text(
                    text = "常用快捷項目：",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(quickExpensePresets) { (presetTitle, presetCat) ->
                        SuggestionChip(
                            onClick = {
                                title = presetTitle
                                category = presetCat
                                if (presetCat == ExpenseCategory.SALARY_JOB || presetCat == ExpenseCategory.SCHOLARSHIP) {
                                    type = ExpenseType.INCOME
                                }
                            },
                            label = { Text(presetTitle, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
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

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("項目說明 *") },
                    placeholder = { Text("例如：午餐排骨飯、微積分課本") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_title_input")
                )

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
            TextButton(onClick = onDismiss) {
                Text("取消")
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
    var budgetText by remember { mutableStateOf(currentBudget.toInt().toString()) }

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
                    onValueChange = { budgetText = it },
                    label = { Text("月預算額度 ($)") },
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
                }
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
