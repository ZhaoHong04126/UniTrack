package com.example.ui.screens.expense

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ExpenseCategory
import com.example.data.model.ExpenseRecord
import com.example.data.model.ExpenseType
import com.example.data.model.PaymentAccount
import com.example.data.model.PaymentMethod
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
    val customAccounts by viewModel.customAccounts.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<ExpenseRecord?>(null) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showFilterBottomSheet by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showDeleteAccountsBottomSheet by remember { mutableStateOf(false) }
    var accountToDelete by remember { mutableStateOf<PaymentAccount?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf<ExpenseCategory?>(null) }
    var currentTab by remember { mutableIntStateOf(0) } // 0: 收支明細, 1: 帳戶管理
    var viewingAccount by remember { mutableStateOf<PaymentAccount?>(null) }

    val locale = LocalConfiguration.current.locales[0]
    val monthFormat = remember(locale) { SimpleDateFormat("yyyy-MM", locale) }

    fun calTime(format: SimpleDateFormat, dateStr: String, offset: Int): String? {
        return runCatching {
            val cal = Calendar.getInstance()
            cal.time = format.parse(dateStr) ?: Date()
            cal.add(Calendar.MONTH, offset)
            format.format(cal.time)
        }.getOrNull()
    }

    val monthExpenses = remember(allExpenses, selectedMonth, selectedCategoryFilter) {
        allExpenses.filter { it.dateString.startsWith(selectedMonth) }
            .filter { selectedCategoryFilter == null || it.category == selectedCategoryFilter }
            .sortedByDescending { it.dateString }
    }

    val allMonthExpensesNoFilter = remember(allExpenses, selectedMonth) {
        allExpenses.filter { it.dateString.startsWith(selectedMonth) }
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
                    if (currentTab == 0) {
                        editingExpense = null
                        showAddDialog = true
                    } else {
                        showAddAccountDialog = true
                    }
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(if (currentTab == 0) "記一筆" else "新增帳戶") },
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
                                calTime(monthFormat, selectedMonth, -1)?.let {
                                    viewModel.setSelectedExpenseMonth(it)
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
                                calTime(monthFormat, selectedMonth, 1)?.let {
                                    viewModel.setSelectedExpenseMonth(it)
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

            // Monthly Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "本月總支出",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$${summary.totalExpense.toInt()}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "本月總收入",
                                    style = MaterialTheme.typography.bodySmall,
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

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

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
                                    val isOverBudget = summary.remainingBudget < 0
                                    Text(
                                        text = "預算執行率 ${summary.budgetUsagePercentage.toInt()}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "設定上限",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable { showBudgetDialog = true }
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = "剩餘 $${summary.remainingBudget.toInt()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (summary.remainingBudget < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            val isOver = summary.remainingBudget < 0
                            LinearProgressIndicator(
                                progress = { animatedBudgetProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (isOver) RoseAccent else TealSecondary,
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

            // Tab Switcher Header (收支明細 vs 帳戶管理)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pill Tab Switcher
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (currentTab == 0) SapphirePrimary else Color.Transparent)
                                .clickable { currentTab = 0 }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "收支明細",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (currentTab == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (currentTab == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (currentTab == 1) SapphirePrimary else Color.Transparent)
                                .clickable { currentTab = 1 }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "帳戶管理",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (currentTab == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (currentTab == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (currentTab == 0) {
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
                    } else {
                        FilterChip(
                            selected = false,
                            onClick = { showDeleteAccountsBottomSheet = true },
                            label = {
                                Text(
                                    text = "刪除帳戶",
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "刪除帳戶",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                labelColor = MaterialTheme.colorScheme.error
                            )
                        )
                    }
                }
            }

            // Tab 0: Expense List Items
            if (currentTab == 0) {
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
            } else {
                // Tab 1: Account Management (Payment Accounts)
                items(customAccounts) { account ->
                    val methodExpenses = allMonthExpensesNoFilter.filter { it.paymentMethod == account.method }
                    val totalExp = methodExpenses.filter { it.type == ExpenseType.EXPENSE }.sumOf { it.amount }
                    val totalInc = methodExpenses.filter { it.type == ExpenseType.INCOME }.sumOf { it.amount }
                    val net = totalInc - totalExp

                    PaymentAccountCard(
                        account = account,
                        expenseAmount = totalExp,
                        incomeAmount = totalInc,
                        netAmount = net,
                        recordCount = methodExpenses.size,
                        onClick = { viewingAccount = account }
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

    if (showAddAccountDialog) {
        AddEditAccountBottomSheet(
            onDismiss = { showAddAccountDialog = false },
            onSave = { newAccount ->
                viewModel.addAccount(newAccount)
                showAddAccountDialog = false
            }
        )
    }

    viewingAccount?.let { account ->
        AccountDetailBottomSheet(
            account = account,
            expenses = allMonthExpensesNoFilter,
            onDismiss = { viewingAccount = null },
            onEditExpense = { record ->
                editingExpense = record
                viewingAccount = null
                showAddDialog = true
            },
            onDeleteAccount = if (!account.id.startsWith("default_")) {
                {
                    viewModel.deleteAccount(account.id)
                    viewingAccount = null
                }
            } else null
        )
    }

    if (showDeleteAccountsBottomSheet) {
        DeleteAccountsBottomSheet(
            accounts = customAccounts,
            onDismiss = { showDeleteAccountsBottomSheet = false },
            onDeleteRequest = { account ->
                accountToDelete = account
            }
        )
    }

    accountToDelete?.let { account ->
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            title = { Text("確認刪除支付帳戶", fontWeight = FontWeight.Bold) },
            text = { Text("確定要刪除「${account.name}」支付帳戶嗎？此動作將會從帳戶列表中移除。") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAccount(account.id)
                        accountToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("確認刪除")
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToDelete = null }) {
                    Text("取消")
                }
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

@Composable
private fun PaymentAccountCard(
    account: PaymentAccount,
    expenseAmount: Double,
    incomeAmount: Double,
    netAmount: Double,
    recordCount: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SapphirePrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getPaymentMethodIcon(account.method),
                    contentDescription = null,
                    tint = SapphirePrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "$recordCount 筆",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "支出: -$${expenseAmount.toInt()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (expenseAmount > 0) RoseAccent else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "收入: +$${incomeAmount.toInt()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (incomeAmount > 0) EmeraldAccent else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "結算",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${if (netAmount >= 0) "+" else ""}$${netAmount.toInt()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (netAmount >= 0) EmeraldAccent else RoseAccent
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "查看明細",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountDetailBottomSheet(
    account: PaymentAccount,
    expenses: List<ExpenseRecord>,
    onDismiss: () -> Unit,
    onEditExpense: (ExpenseRecord) -> Unit,
    onDeleteAccount: (() -> Unit)? = null
) {
    val methodExpenses = remember(expenses, account) {
        expenses.filter { it.paymentMethod == account.method }.sortedByDescending { it.dateString }
    }
    val totalExpense = methodExpenses.filter { it.type == ExpenseType.EXPENSE }.sumOf { it.amount }
    val totalIncome = methodExpenses.filter { it.type == ExpenseType.INCOME }.sumOf { it.amount }
    val net = totalIncome - totalExpense

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SapphirePrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getPaymentMethodIcon(account.method),
                            contentDescription = null,
                            tint = SapphirePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = account.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "類型: ${account.method.label} · 本月 ${methodExpenses.size} 筆記錄",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "本月淨收支",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${if (net >= 0) "+" else ""}$${net.toInt()}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (net >= 0) EmeraldAccent else RoseAccent
                        )
                    }
                    if (onDeleteAccount != null) {
                        IconButton(onClick = onDeleteAccount) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "刪除自訂帳戶",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            if (methodExpenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "本月尚無使用此帳戶的收支記錄",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(methodExpenses) { record ->
                        ExpenseRecordItemCard(
                            record = record,
                            onClick = {
                                onEditExpense(record)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditAccountBottomSheet(
    onDismiss: () -> Unit,
    onSave: (PaymentAccount) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var initialBalanceText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

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
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "新增支付帳戶",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            // Account Name (支付帳號)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("支付帳號 *") },
                placeholder = { Text("輸入自訂名稱，或點擊下方快速輸入") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Quick Input Presets (快速輸入)
            val quickPresets = remember {
                listOf(
                    "現金" to PaymentMethod.CASH,
                    "LINE Pay" to PaymentMethod.MOBILE_PAY,
                    "街口支付" to PaymentMethod.MOBILE_PAY,
                    "全支付" to PaymentMethod.MOBILE_PAY,
                    "悠遊卡" to PaymentMethod.IC_CARD,
                    "一卡通" to PaymentMethod.IC_CARD,
                    "信用卡" to PaymentMethod.CARD,
                    "簽帳金融卡" to PaymentMethod.CARD,
                    "銀行轉帳" to PaymentMethod.TRANSFER,
                    "郵局活存" to PaymentMethod.TRANSFER,
                    "Richart" to PaymentMethod.TRANSFER,
                    "國泰世華" to PaymentMethod.TRANSFER,
                    "中國信託" to PaymentMethod.TRANSFER,
                    "玉山銀行" to PaymentMethod.TRANSFER
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "快速輸入",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickPresets.forEach { (presetName, method) ->
                        SuggestionChip(
                            onClick = {
                                name = presetName
                                selectedMethod = method
                            },
                            label = {
                                Text(
                                    text = presetName,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (name == presetName) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = getPaymentMethodIcon(method),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (name == presetName) SapphirePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (name == presetName) SapphirePrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                labelColor = if (name == presetName) SapphirePrimary else MaterialTheme.colorScheme.onSurface
                            ),
                            border = if (name == presetName) BorderStroke(1.dp, SapphirePrimary) else null
                        )
                    }
                }
            }

            // Payment Method Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "帳戶類型",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaymentMethod.entries.forEach { method ->
                        val isSelected = selectedMethod == method
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedMethod = method },
                            label = { Text(method.label) },
                            leadingIcon = {
                                Icon(
                                    imageVector = getPaymentMethodIcon(method),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SapphirePrimary.copy(alpha = 0.12f),
                                selectedLabelColor = SapphirePrimary,
                                selectedLeadingIconColor = SapphirePrimary
                            )
                        )
                    }
                }
            }

            // Initial Balance
            OutlinedTextField(
                value = initialBalanceText,
                onValueChange = { input ->
                    if (input.isEmpty() || input.all { it.isDigit() }) {
                        initialBalanceText = input
                    }
                },
                label = { Text("起始餘額 (選填)") },
                placeholder = { Text("0") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Note
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("備註 (選填)") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
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
                        if (name.isNotBlank()) {
                            val initBal = initialBalanceText.toDoubleOrNull() ?: 0.0
                            val account = PaymentAccount(
                                name = name.trim(),
                                method = selectedMethod,
                                initialBalance = initBal,
                                note = note.trim()
                            )
                            onSave(account)
                        }
                    },
                    enabled = name.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.5f)
                        .height(48.dp)
                ) {
                    Text("建立帳戶", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun getPaymentMethodIcon(method: PaymentMethod): ImageVector {
    return when (method) {
        PaymentMethod.CASH -> Icons.Default.Payments
        PaymentMethod.MOBILE_PAY -> Icons.Default.PhoneAndroid
        PaymentMethod.IC_CARD -> Icons.Default.CreditCard
        PaymentMethod.CARD -> Icons.Default.CreditCard
        PaymentMethod.TRANSFER -> Icons.Default.AccountBalance
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteAccountsBottomSheet(
    accounts: List<PaymentAccount>,
    onDismiss: () -> Unit,
    onDeleteRequest: (PaymentAccount) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
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
            Text(
                text = "刪除支付帳戶",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Text(
                text = "請選擇欲刪除的支付帳戶，點擊垃圾桶將彈出確認提醒",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            if (accounts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "目前無任何支付帳戶",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(accounts) { account ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(SapphirePrimary.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = getPaymentMethodIcon(account.method),
                                            contentDescription = null,
                                            tint = SapphirePrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = account.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = account.method.label,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { onDeleteRequest(account) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "刪除 ${account.name}",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
