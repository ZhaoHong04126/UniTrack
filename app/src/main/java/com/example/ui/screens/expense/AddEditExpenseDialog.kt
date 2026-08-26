package com.example.ui.screens.expense

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExpenseCategory
import com.example.data.model.ExpenseRecord
import com.example.data.model.ExpenseType
import com.example.data.model.PaymentAccount
import com.example.data.model.PaymentMethod
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditExpenseDialog(
    initialExpense: ExpenseRecord? = null,
    accounts: List<PaymentAccount> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (ExpenseRecord) -> Unit,
    onDelete: ((ExpenseRecord) -> Unit)? = null
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val dateFormat = remember(locale) { SimpleDateFormat("yyyy-MM-dd", locale) }

    var title by remember { mutableStateOf(initialExpense?.title ?: "") }
    var amountText by remember { mutableStateOf(initialExpense?.amount?.toInt()?.toString() ?: "") }
    var type by remember { mutableStateOf(initialExpense?.type ?: ExpenseType.EXPENSE) }
    var category by remember { mutableStateOf(initialExpense?.category ?: ExpenseCategory.FOOD) }
    var paymentMethod by remember {
        mutableStateOf(
            initialExpense?.paymentMethod ?: accounts.firstOrNull()?.method ?: PaymentMethod.CASH
        )
    }
    var dateString by remember { mutableStateOf(initialExpense?.dateString ?: dateFormat.format(Date())) }
    var note by remember { mutableStateOf(initialExpense?.note ?: "") }

    var showCategorySheet by remember { mutableStateOf(false) }
    var showPaymentSheet by remember { mutableStateOf(false) }

    val expensePresets = remember {
        listOf(
            "學餐午餐" to ExpenseCategory.FOOD,
            "咖啡手搖" to ExpenseCategory.FOOD,
            "超商點心" to ExpenseCategory.FOOD,
            "教科書講義" to ExpenseCategory.BOOKS_STUDY,
            "公車捷運" to ExpenseCategory.TRANSPORT,
            "機車加油" to ExpenseCategory.TRANSPORT,
            "房租水電" to ExpenseCategory.RENT_UTILITY,
            "生活日用" to ExpenseCategory.DAILY,
            "聚餐娛樂" to ExpenseCategory.ENTERTAINMENT,
            "其他支出" to ExpenseCategory.OTHER
        )
    }

    val incomePresets = remember {
        listOf(
            "打工薪資" to ExpenseCategory.SALARY_JOB,
            "家教收入" to ExpenseCategory.SALARY_JOB,
            "獎助學金" to ExpenseCategory.SCHOLARSHIP,
            "生活費補貼" to ExpenseCategory.OTHER,
            "實習津貼" to ExpenseCategory.SALARY_JOB,
            "二手出清" to ExpenseCategory.OTHER,
            "投資回饋" to ExpenseCategory.OTHER,
            "其他收入" to ExpenseCategory.OTHER
        )
    }

    val currentPresets = if (type == ExpenseType.EXPENSE) expensePresets else incomePresets

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
                text = if (initialExpense == null) "記一筆收支" else "編輯收支記錄",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            // Type Switcher (Expense vs Income)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = type == ExpenseType.EXPENSE,
                    onClick = {
                        type = ExpenseType.EXPENSE
                        if (category == ExpenseCategory.SALARY_JOB || category == ExpenseCategory.SCHOLARSHIP) {
                            category = ExpenseCategory.FOOD
                        }
                    },
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
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                )
                FilterChip(
                    selected = type == ExpenseType.INCOME,
                    onClick = {
                        type = ExpenseType.INCOME
                        if (category != ExpenseCategory.SALARY_JOB && category != ExpenseCategory.SCHOLARSHIP && category != ExpenseCategory.OTHER) {
                            category = ExpenseCategory.SALARY_JOB
                        }
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
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                )
            }

            // Amount Input
            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    if (input.isEmpty() || input.all { it.isDigit() }) {
                        amountText = input
                    }
                },
                label = { Text("金額 ($) *") },
                placeholder = { Text("0") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.AttachMoney,
                        contentDescription = null,
                        tint = if (type == ExpenseType.EXPENSE) RoseAccent else EmeraldAccent
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("expense_amount_input")
            )

            // Title Field
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("項目說明 *") },
                placeholder = { Text("例如：午餐排骨飯、微積分課本") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("expense_title_input")
            )

            // Quick Presets Row
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (type == ExpenseType.EXPENSE) "常用支出項目" else "常用收入項目",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    currentPresets.forEach { (presetTitle, presetCat) ->
                        SuggestionChip(
                            onClick = {
                                title = presetTitle
                                category = presetCat
                            },
                            label = { Text(presetTitle, style = MaterialTheme.typography.bodySmall) },
                            shape = RoundedCornerShape(8.dp),
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }

            // Category & Payment Method Selectors
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Category Card Selector
                Surface(
                    onClick = { showCategorySheet = true },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "分類",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Icon(
                                    imageVector = getCategoryIcon(category),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = category.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Payment Method Card Selector
                Surface(
                    onClick = { showPaymentSheet = true },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "支付方式",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val currentAccountName = accounts.firstOrNull { it.method == paymentMethod }?.name ?: paymentMethod.label
                            Text(
                                text = currentAccountName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Date Picker Card Field
            Surface(
                onClick = {
                    val parts = dateString.split("-")
                    val curYear = parts.getOrNull(0)?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
                    val curMonth = (parts.getOrNull(1)?.toIntOrNull() ?: (Calendar.getInstance().get(Calendar.MONTH) + 1)) - 1
                    val curDay = parts.getOrNull(2)?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

                    DatePickerDialog(
                        context,
                        { _, y, m, d ->
                            dateString = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)
                        },
                        curYear,
                        curMonth,
                        curDay
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
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "記帳日期",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = dateString,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Text(
                        text = "變更",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Note Input
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("備註 (選填)") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Bottom Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (initialExpense != null && onDelete != null) {
                    OutlinedButton(
                        onClick = { onDelete(initialExpense) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("delete_expense_button")
                    ) {
                        Text("刪除", style = MaterialTheme.typography.bodyLarge)
                    }
                }
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
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(if (initialExpense != null && onDelete != null) 1.2f else 1.5f)
                        .height(48.dp)
                        .testTag("save_expense_button")
                ) {
                    Text(
                        if (initialExpense == null) "儲存記錄" else "完成",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Category Selection Bottom Sheet
    if (showCategorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showCategorySheet = false },
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val displayCategories = if (type == ExpenseType.EXPENSE) {
                    listOf(
                        ExpenseCategory.FOOD,
                        ExpenseCategory.BOOKS_STUDY,
                        ExpenseCategory.TRANSPORT,
                        ExpenseCategory.RENT_UTILITY,
                        ExpenseCategory.ENTERTAINMENT,
                        ExpenseCategory.DAILY,
                        ExpenseCategory.OTHER
                    )
                } else {
                    listOf(
                        ExpenseCategory.SALARY_JOB,
                        ExpenseCategory.SCHOLARSHIP,
                        ExpenseCategory.OTHER
                    )
                }

                Text(
                    text = if (type == ExpenseType.EXPENSE) "選擇支出分類" else "選擇收入分類",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(displayCategories) { cat ->
                        val isSelected = category == cat
                        Card(
                            onClick = {
                                category = cat
                                showCategorySheet = false
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    SapphirePrimary.copy(alpha = 0.12f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                }
                            ),
                            border = if (isSelected) {
                                BorderStroke(1.5.dp, SapphirePrimary)
                            } else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(76.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = getCategoryIcon(cat),
                                    contentDescription = cat.label,
                                    tint = if (isSelected) SapphirePrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = cat.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) SapphirePrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Payment Method Selection Bottom Sheet
    if (showPaymentSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPaymentSheet = false },
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "選擇支付方式",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (accounts.isNotEmpty()) {
                    accounts.forEach { account ->
                        val isSelected = paymentMethod == account.method
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) SapphirePrimary.copy(alpha = 0.12f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                )
                                .clickable {
                                    paymentMethod = account.method
                                    showPaymentSheet = false
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = account.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) SapphirePrimary else MaterialTheme.colorScheme.onSurface
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = SapphirePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                } else {
                    PaymentMethod.entries.forEach { method ->
                        val isSelected = paymentMethod == method
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) SapphirePrimary.copy(alpha = 0.12f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                )
                                .clickable {
                                    paymentMethod = method
                                    showPaymentSheet = false
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = method.label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) SapphirePrimary else MaterialTheme.colorScheme.onSurface
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = SapphirePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getCategoryIcon(category: ExpenseCategory): ImageVector {
    return when (category) {
        ExpenseCategory.FOOD -> Icons.Default.Restaurant
        ExpenseCategory.BOOKS_STUDY -> Icons.AutoMirrored.Filled.MenuBook
        ExpenseCategory.TRANSPORT -> Icons.Default.DirectionsBus
        ExpenseCategory.RENT_UTILITY -> Icons.Default.Home
        ExpenseCategory.ENTERTAINMENT -> Icons.Default.SportsEsports
        ExpenseCategory.DAILY -> Icons.Default.ShoppingBag
        ExpenseCategory.SALARY_JOB -> Icons.Default.Work
        ExpenseCategory.SCHOLARSHIP -> Icons.Default.School
        ExpenseCategory.OTHER -> Icons.Default.MoreHoriz
    }
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
                    shape = RoundedCornerShape(12.dp),
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
