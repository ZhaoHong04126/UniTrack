package com.example.ui.screens.expense

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ExpenseCategory
import com.example.data.model.ExpenseRecord
import com.example.data.model.ExpenseType
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudentViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(
    viewModel: StudentViewModel,
    modifier: Modifier = Modifier
) {
    val selectedMonth by viewModel.selectedExpenseMonth.collectAsStateWithLifecycle()
    val allExpenses by viewModel.allExpenses.collectAsStateWithLifecycle()
    val summary by viewModel.monthlyExpenseSummary.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<ExpenseRecord?>(null) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showFilterBottomSheet by remember { mutableStateOf(false) }
    var selectedCategoryFilter by remember { mutableStateOf<ExpenseCategory?>(null) }

    val locale = LocalConfiguration.current.locales[0]
    val monthFormat = remember(locale) { SimpleDateFormat("yyyy-MM", locale) }

    val monthExpenses = remember(allExpenses, selectedMonth, selectedCategoryFilter) {
        allExpenses.filter { it.dateString.startsWith(selectedMonth) }
            .filter { selectedCategoryFilter == null || it.category == selectedCategoryFilter }
            .sortedByDescending { it.dateString }
    }

    val animatedBudgetProgress by animateFloatAsState(
        targetValue = (summary.budgetUsagePercentage / 100f).coerceIn(0f, 1f),
        animationSpec = tween(600),
        label = "budget_progress"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingExpense = null
                    showAddDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("記一筆") },
                containerColor = TealSecondary,
                contentColor = Color.White,
                modifier = Modifier
                    .testTag("add_expense_fab")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp)
        ) {
            // Month Selector Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                val cal = Calendar.getInstance()
                                runCatching {
                                    cal.time = monthFormat.parse(selectedMonth) ?: Date()
                                    cal.add(Calendar.MONTH, -1)
                                    viewModel.setSelectedExpenseMonth(monthFormat.format(cal.time))
                                }
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "上個月")
                        }

                        Text(
                            text = "$selectedMonth 月記帳",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(
                            onClick = {
                                val cal = Calendar.getInstance()
                                runCatching {
                                    cal.time = monthFormat.parse(selectedMonth) ?: Date()
                                    cal.add(Calendar.MONTH, 1)
                                    viewModel.setSelectedExpenseMonth(monthFormat.format(cal.time))
                                }
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "下個月")
                        }
                    }

                    if (allExpenses.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearConfirmDialog = true },
                            modifier = Modifier.testTag("clear_expenses_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "清空所有記帳記錄",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Monthly Financial Dashboard Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "本月總支出",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$${summary.totalExpense.toInt()}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (summary.budgetUsagePercentage > 90f) RoseAccent else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "本月總收入",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "+$${summary.totalIncome.toInt()}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldAccent
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                        // Budget Bar
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "預算執行率 ${summary.budgetUsagePercentage.toInt()}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    TextButton(
                                        onClick = { showBudgetDialog = true },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("設定上限", style = MaterialTheme.typography.labelSmall, color = SapphirePrimary)
                                    }
                                }
                                Text(
                                    text = "剩餘 $${summary.remainingBudget.toInt()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (summary.remainingBudget < 0) RoseAccent else TealDark
                                )
                            }

                            LinearProgressIndicator(
                                progress = { animatedBudgetProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp)),
                                color = when {
                                    summary.budgetUsagePercentage >= 100f -> RoseAccent
                                    summary.budgetUsagePercentage >= 80f -> AmberAccent
                                    else -> TealSecondary
                                },
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            Text(
                                text = "月預算額度：$${summary.budgetAmount.toInt()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Category Breakdown Cards
            if (summary.categoryBreakdown.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "支出分類佔比",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            summary.categoryBreakdown.entries.sortedByDescending { it.value }.take(5).forEach { (cat, amount) ->
                                val pct = if (summary.totalExpense > 0) ((amount / summary.totalExpense) * 100f).toInt() else 0
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = cat.label,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "$${amount.toInt()} ($pct%)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { (pct / 100f).coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = SapphirePrimary.copy(alpha = 0.8f),
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Category Filter Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "收支明細清單",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    FilterChip(
                        selected = selectedCategoryFilter != null,
                        onClick = { showFilterBottomSheet = true },
                        label = {
                            Text(
                                text = selectedCategoryFilter?.label ?: "篩選",
                                fontWeight = if (selectedCategoryFilter != null) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "篩選",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        trailingIcon = if (selectedCategoryFilter != null) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "清除篩選",
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { selectedCategoryFilter = null }
                                )
                            }
                        } else null,
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SapphirePrimary.copy(alpha = 0.12f),
                            selectedLabelColor = SapphirePrimary,
                            selectedLeadingIconColor = SapphirePrimary,
                            selectedTrailingIconColor = SapphirePrimary
                        )
                    )
                }
            }

            // Expense List Items
            if (monthExpenses.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "這個月尚無記帳收支明細",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(monthExpenses) { record ->
                    ExpenseRecordItemCard(
                        record = record,
                        onClick = {
                            editingExpense = record
                            showAddDialog = true
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddEditExpenseDialog(
            initialExpense = editingExpense,
            onDismiss = { showAddDialog = false },
            onSave = { expense ->
                if (editingExpense == null) {
                    viewModel.addExpense(expense)
                } else {
                    viewModel.updateExpense(expense)
                }
                showAddDialog = false
            },
            onDelete = { expense ->
                viewModel.deleteExpense(expense)
                showAddDialog = false
            }
        )
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("清空記帳明細", fontWeight = FontWeight.Bold) },
            text = { Text("確定要清空所有的記帳與收支明細嗎？此動作無法復原。") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllExpenses()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("確定清空")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showBudgetDialog) {
        BudgetDialog(
            currentBudget = summary.budgetAmount,
            onDismiss = { showBudgetDialog = false },
            onSave = { newBudget ->
                viewModel.setMonthlyBudget(newBudget)
                showBudgetDialog = false
            }
        )
    }

    if (showFilterBottomSheet) {
        ExpenseCategoryFilterBottomSheet(
            selectedCategory = selectedCategoryFilter,
            onSelectCategory = { cat ->
                selectedCategoryFilter = cat
            },
            onDismiss = { showFilterBottomSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseCategoryFilterBottomSheet(
    selectedCategory: ExpenseCategory?,
    onSelectCategory: (ExpenseCategory?) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "收支類別篩選",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (selectedCategory != null) {
                    TextButton(
                        onClick = {
                            onSelectCategory(null)
                            onDismiss()
                        }
                    ) {
                        Text("重設為全部", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            val allFilterItems = listOf<ExpenseCategory?>(null) + ExpenseCategory.entries
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(allFilterItems) { cat ->
                    val isSelected = selectedCategory == cat
                    val icon = when (cat) {
                        null -> Icons.Default.Apps
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
                    val label = cat?.label ?: "全部"

                    Card(
                        onClick = {
                            onSelectCategory(cat)
                            onDismiss()
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
                            .height(72.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (isSelected) SapphirePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) SapphirePrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpenseRecordItemCard(
    record: ExpenseRecord,
    onClick: () -> Unit
) {
    val isExpense = record.type == ExpenseType.EXPENSE

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isExpense) RoseLight else EmeraldLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (record.category) {
                        ExpenseCategory.FOOD -> Icons.Default.Restaurant
                        ExpenseCategory.BOOKS_STUDY -> Icons.AutoMirrored.Filled.MenuBook
                        ExpenseCategory.TRANSPORT -> Icons.Default.DirectionsBus
                        ExpenseCategory.RENT_UTILITY -> Icons.Default.Home
                        ExpenseCategory.ENTERTAINMENT -> Icons.Default.SportsEsports
                        ExpenseCategory.DAILY -> Icons.Default.ShoppingBag
                        ExpenseCategory.SALARY_JOB -> Icons.Default.Work
                        ExpenseCategory.SCHOLARSHIP -> Icons.Default.School
                        ExpenseCategory.OTHER -> Icons.Default.MoreHoriz
                    },
                    contentDescription = null,
                    tint = if (isExpense) RoseAccent else EmeraldAccent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = record.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = record.dateString,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "・${record.category.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "・${record.paymentMethod.label.split(" ").first()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (record.note.isNotBlank()) {
                    Text(
                        text = record.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "${if (isExpense) "-" else "+"}$${record.amount.toInt()}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (isExpense) RoseAccent else EmeraldAccent
            )
        }
    }
}
