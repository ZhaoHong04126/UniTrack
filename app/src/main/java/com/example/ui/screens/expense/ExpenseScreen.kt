package com.example.ui.screens.expense

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.roundToInt
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
    var currentTab by remember { mutableIntStateOf(0) } // 0: 收支明細, 1: 帳戶管理, 2: 統計圖表
    var chartType by remember { mutableIntStateOf(0) } // 0: 圓餅圖, 1: 折線圖
    var chartExpenseType by remember { mutableStateOf<ExpenseType?>(ExpenseType.EXPENSE) }
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
            if (currentTab != 2) {
                ExtendedFloatingActionButton(
                    onClick = {
                        if (currentTab == 1) {
                            showAddAccountDialog = true
                        } else {
                            editingExpense = null
                            showAddDialog = true
                        }
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(if (currentTab == 1) "新增帳戶" else "記一筆") },
                    containerColor = TealSecondary,
                    contentColor = Color.White,
                    modifier = Modifier
                        .testTag("add_expense_fab")
                )
            }
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
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (currentTab == 0) SapphirePrimary else Color.Transparent)
                                .clickable { currentTab = 0 }
                                .padding(horizontal = 11.dp, vertical = 6.dp)
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
                                .padding(horizontal = 11.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "帳戶管理",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (currentTab == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (currentTab == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (currentTab == 2) SapphirePrimary else Color.Transparent)
                                .clickable { currentTab = 2 }
                                .padding(horizontal = 11.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "統計圖表",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (currentTab == 2) FontWeight.Bold else FontWeight.Medium,
                                color = if (currentTab == 2) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    when (currentTab) {
                        0 -> {
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
                        1 -> {
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
                        else -> Unit
                    }
                }
            }

            // Tab 0: Expense List Items
            when (currentTab) {
                0 -> {
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
                                    Spacer(modifier = Modifier.height(4.dp))
                                    FilledTonalButton(
                                        onClick = { viewModel.seedMockExpenses() },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = SapphirePrimary.copy(alpha = 0.12f),
                                            contentColor = SapphirePrimary
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoFixHigh,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("一鍵產生 20 筆測試資料", fontWeight = FontWeight.Bold)
                                    }
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
                1 -> {
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
                else -> {
                    // Tab 2: Statistics Charts
                    item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Top Chart Controls Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 支出 / 收入 / 全部 Toggle
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .padding(2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (chartExpenseType == ExpenseType.EXPENSE) RoseAccent else Color.Transparent)
                                            .clickable { chartExpenseType = ExpenseType.EXPENSE }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = "支出",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (chartExpenseType == ExpenseType.EXPENSE) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (chartExpenseType == ExpenseType.INCOME) EmeraldAccent else Color.Transparent)
                                            .clickable { chartExpenseType = ExpenseType.INCOME }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = "收入",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (chartExpenseType == ExpenseType.INCOME) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (chartExpenseType == null) SapphirePrimary else Color.Transparent)
                                            .clickable { chartExpenseType = null }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = "全部",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (chartExpenseType == null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // 圓餅圖 / 折線圖 Switcher
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .padding(2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (chartType == 0) SapphirePrimary else Color.Transparent)
                                            .clickable { chartType = 0 }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PieChart,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = if (chartType == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "圓餅圖",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (chartType == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (chartType == 1) SapphirePrimary else Color.Transparent)
                                            .clickable { chartType = 1 }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ShowChart,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = if (chartType == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "折線圖",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (chartType == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                            val filteredRecords = remember(allMonthExpensesNoFilter, chartExpenseType) {
                                if (chartExpenseType == null) {
                                    allMonthExpensesNoFilter
                                } else {
                                    allMonthExpensesNoFilter.filter { it.type == chartExpenseType }
                                }
                            }

                            if (filteredRecords.isEmpty()) {
                                val emptyLabel = when (chartExpenseType) {
                                    ExpenseType.EXPENSE -> "支出"
                                    ExpenseType.INCOME -> "收入"
                                    null -> "收支"
                                }
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "本月尚無${emptyLabel}記錄",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    FilledTonalButton(
                                        onClick = { viewModel.seedMockExpenses() },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = SapphirePrimary.copy(alpha = 0.12f),
                                            contentColor = SapphirePrimary
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoFixHigh,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("一鍵產生 20 筆測試資料", fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                if (chartType == 0) {
                                    ExpenseDonutChart(
                                        records = filteredRecords,
                                        type = chartExpenseType
                                    )
                                } else {
                                    ExpenseLineTrendChart(
                                        records = filteredRecords,
                                        selectedMonth = selectedMonth,
                                        type = chartExpenseType
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

@Composable
private fun ExpenseDonutChart(
    records: List<ExpenseRecord>,
    type: ExpenseType?
) {
    val totalExp = remember(records) { records.filter { it.type == ExpenseType.EXPENSE }.sumOf { it.amount } }
    val totalInc = remember(records) { records.filter { it.type == ExpenseType.INCOME }.sumOf { it.amount } }
    val net = totalInc - totalExp

    val totalAmount = remember(records, type) {
        if (type == null) records.sumOf { it.amount } else records.filter { it.type == type }.sumOf { it.amount }
    }

    val categoryTotals = remember(records, type) {
        val filtered = if (type == null) records else records.filter { it.type == type }
        filtered.groupBy { Pair(it.category, it.type) }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                var startAngle = -90f
                val strokeWidth = 32.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2f
                val arcSize = Size(radius * 2, radius * 2)
                val topLeft = Offset((size.width - radius * 2) / 2f, (size.height - radius * 2) / 2f)

                categoryTotals.forEach { (catTypePair, amount) ->
                    val (cat, _) = catTypePair
                    val sweepAngle = if (totalAmount > 0) ((amount / totalAmount) * 360f).toFloat() else 0f
                    if (sweepAngle > 0f) {
                        drawArc(
                            color = getCategoryColor(cat),
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                        )
                        startAngle += sweepAngle
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (type == null) {
                    Text(
                        text = "本月收支結餘",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (net >= 0) "+$${net.toInt()}" else "-$${kotlin.math.abs(net).toInt()}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (net >= 0) EmeraldAccent else RoseAccent
                    )
                    Text(
                        text = "支$${totalExp.toInt()} | 收+$${totalInc.toInt()}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "總${type.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$${totalAmount.toInt()}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (type == ExpenseType.EXPENSE) RoseAccent else EmeraldAccent
                    )
                }
            }
        }

        // Category Breakdown List
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            categoryTotals.forEach { (catTypePair, amount) ->
                val (cat, itemType) = catTypePair
                val pct = if (totalAmount > 0) ((amount / totalAmount) * 100f).toInt() else 0
                val color = getCategoryColor(cat)

                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Text(
                                text = cat.label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            if (type == null) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (itemType == ExpenseType.EXPENSE) RoseAccent.copy(alpha = 0.15f) else EmeraldAccent.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = itemType.label,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = if (itemType == ExpenseType.EXPENSE) RoseAccent else EmeraldAccent,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Text(
                            text = "${if (itemType == ExpenseType.INCOME) "+" else ""}$${amount.toInt()}  ($pct%)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (itemType == ExpenseType.INCOME) EmeraldAccent else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    LinearProgressIndicator(
                        progress = { (pct / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = color,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpenseLineTrendChart(
    records: List<ExpenseRecord>,
    selectedMonth: String,
    type: ExpenseType?
) {
    val daysInMonth = remember(selectedMonth) {
        val cal = Calendar.getInstance()
        val format = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        runCatching {
            cal.time = format.parse(selectedMonth) ?: Date()
            cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        }.getOrDefault(30)
    }

    var selectedDay by remember { mutableStateOf<Int?>(null) }

    val totalExp = remember(records) { records.filter { it.type == ExpenseType.EXPENSE }.sumOf { it.amount } }
    val totalInc = remember(records) { records.filter { it.type == ExpenseType.INCOME }.sumOf { it.amount } }
    val net = totalInc - totalExp

    val dailyExpSums = remember(records, daysInMonth) {
        val map = (1..daysInMonth).associateWith { 0.0 }.toMutableMap()
        records.filter { it.type == ExpenseType.EXPENSE }.forEach { r ->
            val day = r.dateString.split("-").getOrNull(2)?.toIntOrNull()
            if (day != null && day in 1..daysInMonth) {
                map[day] = (map[day] ?: 0.0) + r.amount
            }
        }
        map
    }

    val dailyIncSums = remember(records, daysInMonth) {
        val map = (1..daysInMonth).associateWith { 0.0 }.toMutableMap()
        records.filter { it.type == ExpenseType.INCOME }.forEach { r ->
            val day = r.dateString.split("-").getOrNull(2)?.toIntOrNull()
            if (day != null && day in 1..daysInMonth) {
                map[day] = (map[day] ?: 0.0) + r.amount
            }
        }
        map
    }

    val maxAmount = remember(dailyExpSums, dailyIncSums, type) {
        val maxExp = dailyExpSums.values.maxOrNull() ?: 0.0
        val maxInc = dailyIncSums.values.maxOrNull() ?: 0.0
        when (type) {
            ExpenseType.EXPENSE -> maxExp.coerceAtLeast(100.0)
            ExpenseType.INCOME -> maxInc.coerceAtLeast(100.0)
            null -> maxOf(maxExp, maxInc).coerceAtLeast(100.0)
        }
    }

    val activeDays = remember(records) { records.map { it.dateString }.distinct().size }
    val dailyAvgExp = if (activeDays > 0) totalExp / activeDays else 0.0
    val dailyAvgInc = if (activeDays > 0) totalInc / activeDays else 0.0

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (type == null) {
                Column {
                    Text(
                        text = "本月總支出",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$${totalExp.toInt()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = RoseAccent
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "本月總收入",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "+$${totalInc.toInt()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldAccent
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "本月結餘",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (net >= 0) "+$${net.toInt()}" else "-$${kotlin.math.abs(net).toInt()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (net >= 0) EmeraldAccent else RoseAccent
                    )
                }
            } else {
                val curAvg = if (type == ExpenseType.EXPENSE) dailyAvgExp else dailyAvgInc
                val curSums = if (type == ExpenseType.EXPENSE) dailyExpSums else dailyIncSums
                val peakDay = curSums.maxByOrNull { it.value }

                Column {
                    Text(
                        text = "記帳天數",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$activeDays 天",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "平均每日${type.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$${curAvg.toInt()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (type == ExpenseType.EXPENSE) RoseAccent else EmeraldAccent
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "單日最高",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$${peakDay?.value?.toInt() ?: 0} (${peakDay?.key ?: 1}號)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (type == null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(RoseAccent))
                Spacer(Modifier.width(4.dp))
                Text("支出走勢", style = MaterialTheme.typography.labelSmall, color = RoseAccent, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(16.dp))
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(EmeraldAccent))
                Spacer(Modifier.width(4.dp))
                Text("收入走勢", style = MaterialTheme.typography.labelSmall, color = EmeraldAccent, fontWeight = FontWeight.Bold)
            }
        }

        // Line Chart Canvas with Touch Interaction
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(vertical = 8.dp)
                .pointerInput(daysInMonth) {
                    val paddingLeft = 10.dp.toPx()
                    val paddingRight = 10.dp.toPx()
                    val chartWidth = size.width - paddingLeft - paddingRight
                    val dayWidth = chartWidth / (daysInMonth - 1).coerceAtLeast(1)

                    detectTapGestures(
                        onTap = { offset ->
                            val clickedDay = ((offset.x - paddingLeft) / dayWidth).roundToInt() + 1
                            selectedDay = clickedDay.coerceIn(1, daysInMonth)
                        }
                    )
                }
                .pointerInput(daysInMonth) {
                    val paddingLeft = 10.dp.toPx()
                    val paddingRight = 10.dp.toPx()
                    val chartWidth = size.width - paddingLeft - paddingRight
                    val dayWidth = chartWidth / (daysInMonth - 1).coerceAtLeast(1)

                    detectDragGestures(
                        onDragStart = { offset ->
                            val clickedDay = ((offset.x - paddingLeft) / dayWidth).roundToInt() + 1
                            selectedDay = clickedDay.coerceIn(1, daysInMonth)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val clickedDay = ((change.position.x - paddingLeft) / dayWidth).roundToInt() + 1
                            selectedDay = clickedDay.coerceIn(1, daysInMonth)
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val paddingLeft = 10.dp.toPx()
                val paddingRight = 10.dp.toPx()
                val paddingTop = 15.dp.toPx()
                val paddingBottom = 24.dp.toPx()

                val chartWidth = size.width - paddingLeft - paddingRight
                val chartHeight = size.height - paddingTop - paddingBottom

                // Draw background horizontal grid lines
                val gridLines = 3
                for (i in 0..gridLines) {
                    val y = paddingTop + (chartHeight / gridLines) * i
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.15f),
                        start = Offset(paddingLeft, y),
                        end = Offset(size.width - paddingRight, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                fun drawTrendLine(dailySums: Map<Int, Double>, color: Color) {
                    val points = (1..daysInMonth).map { day ->
                        val x = paddingLeft + (day - 1) * (chartWidth / (daysInMonth - 1).coerceAtLeast(1))
                        val amount = dailySums[day] ?: 0.0
                        val y = paddingTop + chartHeight - ((amount / maxAmount) * chartHeight).toFloat()
                        Offset(x, y)
                    }

                    if (points.isNotEmpty()) {
                        val strokePath = Path()
                        val fillPath = Path()

                        strokePath.moveTo(points[0].x, points[0].y)
                        fillPath.moveTo(points[0].x, paddingTop + chartHeight)
                        fillPath.lineTo(points[0].x, points[0].y)

                        for (i in 0 until points.size - 1) {
                            val p0 = points[i]
                            val p1 = points[i + 1]
                            val controlPointX1 = p0.x + (p1.x - p0.x) / 2f
                            val controlPointY1 = p0.y
                            val controlPointX2 = p0.x + (p1.x - p0.x) / 2f
                            val controlPointY2 = p1.y

                            strokePath.cubicTo(controlPointX1, controlPointY1, controlPointX2, controlPointY2, p1.x, p1.y)
                            fillPath.cubicTo(controlPointX1, controlPointY1, controlPointX2, controlPointY2, p1.x, p1.y)
                        }

                        fillPath.lineTo(points.last().x, paddingTop + chartHeight)
                        fillPath.close()

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(color.copy(alpha = 0.28f), Color.Transparent),
                                startY = paddingTop,
                                endY = paddingTop + chartHeight
                            )
                        )

                        drawPath(
                            path = strokePath,
                            color = color,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )

                        points.forEachIndexed { index, point ->
                            val day = index + 1
                            val amount = dailySums[day] ?: 0.0
                            if (amount > 0) {
                                drawCircle(color = Color.White, radius = 3.5.dp.toPx(), center = point)
                                drawCircle(color = color, radius = 2.dp.toPx(), center = point)
                            }
                        }
                    }
                }

                when (type) {
                    null -> {
                        drawTrendLine(dailyExpSums, RoseAccent)
                        drawTrendLine(dailyIncSums, EmeraldAccent)
                    }
                    ExpenseType.EXPENSE -> drawTrendLine(dailyExpSums, RoseAccent)
                    ExpenseType.INCOME -> drawTrendLine(dailyIncSums, EmeraldAccent)
                }

                // Draw vertical indicator and highlighted points if touched
                selectedDay?.let { selDay ->
                    val selX = paddingLeft + (selDay - 1) * (chartWidth / (daysInMonth - 1).coerceAtLeast(1))
                    drawLine(
                        color = SapphirePrimary.copy(alpha = 0.65f),
                        start = Offset(selX, paddingTop),
                        end = Offset(selX, paddingTop + chartHeight),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )

                    val expAmount = dailyExpSums[selDay] ?: 0.0
                    val incAmount = dailyIncSums[selDay] ?: 0.0

                    if (type != ExpenseType.INCOME && expAmount > 0) {
                        val expY = paddingTop + chartHeight - ((expAmount / maxAmount) * chartHeight).toFloat()
                        drawCircle(RoseAccent.copy(alpha = 0.35f), radius = 9.dp.toPx(), center = Offset(selX, expY))
                        drawCircle(Color.White, radius = 5.dp.toPx(), center = Offset(selX, expY))
                        drawCircle(RoseAccent, radius = 3.5.dp.toPx(), center = Offset(selX, expY))
                    }
                    if (type != ExpenseType.EXPENSE && incAmount > 0) {
                        val incY = paddingTop + chartHeight - ((incAmount / maxAmount) * chartHeight).toFloat()
                        drawCircle(EmeraldAccent.copy(alpha = 0.35f), radius = 9.dp.toPx(), center = Offset(selX, incY))
                        drawCircle(Color.White, radius = 5.dp.toPx(), center = Offset(selX, incY))
                        drawCircle(EmeraldAccent, radius = 3.5.dp.toPx(), center = Offset(selX, incY))
                    }
                }
            }
        }

        // X-axis Day labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf(1, 5, 10, 15, 20, 25, daysInMonth).forEach { day ->
                Text(
                    text = "${day}日",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selectedDay == day) SapphirePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (selectedDay == day) FontWeight.Bold else FontWeight.Normal
                )
            }
        }

        // Touched Day Details Card
        selectedDay?.let { selDay ->
            val dayDateStr = "$selectedMonth-${selDay.toString().padStart(2, '0')}"
            val dayRecords = remember(records, selDay) {
                records.filter { it.dateString == dayDateStr }
            }
            val dayExp = dailyExpSums[selDay] ?: 0.0
            val dayInc = dailyIncSums[selDay] ?: 0.0

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = SapphirePrimary
                            )
                            Text(
                                text = "${selectedMonth}-${selDay.toString().padStart(2, '0')} 收支明細",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (type != ExpenseType.INCOME && dayExp > 0) {
                                Text(
                                    text = "支出 -$${dayExp.toInt()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = RoseAccent
                                )
                            }
                            if (type != ExpenseType.EXPENSE && dayInc > 0) {
                                Text(
                                    text = "收入 +$${dayInc.toInt()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldAccent
                                )
                            }
                            if (dayExp == 0.0 && dayInc == 0.0) {
                                Text(
                                    text = "當日無收支",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (dayRecords.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            dayRecords.forEach { record ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(getCategoryColor(record.category))
                                        )
                                        Text(
                                            text = record.title.ifBlank { record.category.label },
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (record.note.isNotBlank()) {
                                            Text(
                                                text = "(${record.note})",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${if (record.type == ExpenseType.INCOME) "+" else "-"}$${record.amount.toInt()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (record.type == ExpenseType.INCOME) EmeraldAccent else RoseAccent
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

private fun getCategoryColor(category: ExpenseCategory): Color {
    return when (category) {
        ExpenseCategory.FOOD -> Color(0xFFFF6B6B)
        ExpenseCategory.BOOKS_STUDY -> Color(0xFF4D7CFE)
        ExpenseCategory.TRANSPORT -> Color(0xFFFFA726)
        ExpenseCategory.RENT_UTILITY -> Color(0xFF20B2AA)
        ExpenseCategory.ENTERTAINMENT -> Color(0xFFAB47BC)
        ExpenseCategory.DAILY -> Color(0xFF26C6DA)
        ExpenseCategory.SALARY_JOB -> Color(0xFF10B981)
        ExpenseCategory.SCHOLARSHIP -> Color(0xFF5C6BC0)
        ExpenseCategory.OTHER -> Color(0xFF78909C)
    }
}
