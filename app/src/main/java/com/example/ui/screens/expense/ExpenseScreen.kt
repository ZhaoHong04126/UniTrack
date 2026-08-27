package com.example.ui.screens.expense

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
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
    var showYearMonthPicker by remember { mutableStateOf(false) }
    var showFilterBottomSheet by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showDeleteAccountsBottomSheet by remember { mutableStateOf(false) }
    var accountToDelete by remember { mutableStateOf<PaymentAccount?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf<ExpenseCategory?>(null) }
    var currentTab by remember { mutableIntStateOf(0) } // 0: 收支明細, 1: 帳戶管理, 2: 統計圖表
    var chartType by remember { mutableIntStateOf(0) } // 0: 圓餅圖, 1: 折線圖
    var chartExpenseType by remember { mutableStateOf<ExpenseType?>(ExpenseType.EXPENSE) }
    var viewingAccount by remember { mutableStateOf<PaymentAccount?>(null) }
    var draggingAccountIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val currentAccountsSnapshot by rememberUpdatedState(customAccounts)
    val density = LocalDensity.current
    var cardHeightWithSpacingPx by remember { mutableFloatStateOf(0f) }

    val locale = LocalConfiguration.current.locales[0]
    val todayDateString = remember(locale) {
        SimpleDateFormat("yyyy-MM-dd", locale).format(Date())
    }

    var isCalendarView by rememberSaveable { mutableStateOf(false) }
    var selectedCalendarDate by rememberSaveable(selectedMonth) {
        mutableStateOf(
            if (todayDateString.startsWith(selectedMonth)) todayDateString else "$selectedMonth-01"
        )
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
            if (currentTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = {
                        editingExpense = null
                        showAddDialog = true
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("記一筆") },
                    containerColor = TealSecondary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_expense_fab")
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
            // Month Selector Header with View Toggle (List vs Calendar)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showYearMonthPicker = true }
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "$selectedMonth 月記帳",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "選擇年月",
                            tint = SapphirePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // 列表 / 月曆 視圖切換鈕
                        IconButton(
                            onClick = { isCalendarView = !isCalendarView },
                            modifier = Modifier.testTag("toggle_expense_view_button")
                        ) {
                            Icon(
                                imageVector = if (isCalendarView) Icons.AutoMirrored.Filled.FormatListBulleted else Icons.Default.CalendarMonth,
                                contentDescription = if (isCalendarView) "切換至列表視圖" else "切換至月曆視圖",
                                tint = SapphirePrimary
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
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "-$${summary.totalExpense.toInt()}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = RoseAccent
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "本月總餘額",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (summary.netBalance >= 0) "$${summary.netBalance.toInt()}" else "-$${kotlin.math.abs(summary.netBalance).toInt()}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (summary.netBalance >= 0) EmeraldAccent else RoseAccent
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "本月總收入",
                                    style = MaterialTheme.typography.labelSmall,
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
                        else -> Unit
                    }
                }
            }

            // Tab 0: Expense List Items (List View vs Calendar View)
            when (currentTab) {
                0 -> {
                    if (isCalendarView) {
                        // Monthly Calendar Card
                        item {
                            ExpenseMonthlyCalendarCard(
                                selectedMonth = selectedMonth,
                                selectedDate = selectedCalendarDate,
                                expenses = allMonthExpensesNoFilter,
                                onSelectDate = { selectedCalendarDate = it }
                            )
                        }

                        // Selected Date Header
                        val selectedDayRecords = allExpenses.filter { it.dateString == selectedCalendarDate }
                        val dayTotalExp = selectedDayRecords.filter { it.type == ExpenseType.EXPENSE }.sumOf { it.amount }
                        val dayTotalInc = selectedDayRecords.filter { it.type == ExpenseType.INCOME }.sumOf { it.amount }

                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 4.dp),
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
                                        tint = SapphirePrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "$selectedCalendarDate 收支明細",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (dayTotalExp > 0) {
                                        Text(
                                            text = "支出 -$${dayTotalExp.toInt()}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = RoseAccent
                                        )
                                    }
                                    if (dayTotalInc > 0) {
                                        Text(
                                            text = "收入 +$${dayTotalInc.toInt()}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldAccent
                                        )
                                    }
                                }
                            }
                        }

                        if (selectedDayRecords.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "當日尚無記帳收支記錄",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        FilledTonalButton(
                                            onClick = {
                                                editingExpense = null
                                                showAddDialog = true
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = SapphirePrimary.copy(alpha = 0.12f),
                                                contentColor = SapphirePrimary
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("補記此日收支", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        } else {
                            items(selectedDayRecords, key = { it.id }) { record ->
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
                        // Original List View
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
                            items(monthExpenses, key = { it.id }) { record ->
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
                1 -> {
                    // Tab 1: Account Management (Payment Accounts) with Continuous Long-Press Drag Reordering
                    itemsIndexed(customAccounts, key = { _, acc -> acc.id }) { index, account ->
                        val methodExpenses = allMonthExpensesNoFilter.filter { it.paymentMethod == account.method }
                        val totalExp = methodExpenses.filter { it.type == ExpenseType.EXPENSE }.sumOf { it.amount }
                        val totalInc = methodExpenses.filter { it.type == ExpenseType.INCOME }.sumOf { it.amount }

                        val isBeforeStart = selectedMonth < account.startYearMonth
                        val cumulativeExpenses = allExpenses.filter {
                            it.paymentMethod == account.method && it.dateString.substringBeforeLast("-") <= selectedMonth
                        }
                        val cumExp = cumulativeExpenses.filter { it.type == ExpenseType.EXPENSE }.sumOf { it.amount }
                        val cumInc = cumulativeExpenses.filter { it.type == ExpenseType.INCOME }.sumOf { it.amount }
                        val currentBalance = if (isBeforeStart) 0.0 else account.initialBalance + (cumInc - cumExp)
                        val isDragging = draggingAccountIndex == index

                        PaymentAccountCard(
                            account = account,
                            expenseAmount = totalExp,
                            incomeAmount = totalInc,
                            balance = currentBalance,
                            recordCount = methodExpenses.size,
                            isBeforeStart = isBeforeStart,
                            isDragging = isDragging,
                            translationY = if (isDragging) dragOffsetY else 0f,
                            onClick = {
                                if (draggingAccountIndex == null) {
                                    viewingAccount = account
                                }
                            },
                            modifier = Modifier
                                .zIndex(if (isDragging) 10f else 1f)
                                .onGloballyPositioned { coordinates ->
                                    if (coordinates.size.height > 0) {
                                        cardHeightWithSpacingPx = coordinates.size.height.toFloat() + with(density) { 12.dp.toPx() }
                                    }
                                }
                                .pointerInput(account.id) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            val currentIdx = currentAccountsSnapshot.indexOfFirst { it.id == account.id }
                                            draggingAccountIndex = if (currentIdx != -1) currentIdx else index
                                            dragOffsetY = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffsetY += dragAmount.y
                                            val defaultStep = with(density) { 88.dp.toPx() + 12.dp.toPx() }
                                            val step = if (cardHeightWithSpacingPx > 0f) cardHeightWithSpacingPx else defaultStep
                                            val threshold = step / 2f
                                            val listSize = currentAccountsSnapshot.size
                                            while (dragOffsetY > threshold && draggingAccountIndex != null && draggingAccountIndex!! < listSize - 1) {
                                                val from = draggingAccountIndex!!
                                                val to = from + 1
                                                viewModel.moveAccount(from, to)
                                                draggingAccountIndex = to
                                                dragOffsetY -= step
                                            }
                                            while (dragOffsetY < -threshold && draggingAccountIndex != null && draggingAccountIndex!! > 0) {
                                                val from = draggingAccountIndex!!
                                                val to = from - 1
                                                viewModel.moveAccount(from, to)
                                                draggingAccountIndex = to
                                                dragOffsetY += step
                                            }
                                        },
                                        onDragEnd = {
                                            if (draggingAccountIndex != null) {
                                                viewModel.onAccountMoved()
                                            }
                                            draggingAccountIndex = null
                                            dragOffsetY = 0f
                                        },
                                        onDragCancel = {
                                            draggingAccountIndex = null
                                            dragOffsetY = 0f
                                        }
                                    )
                                }
                        )
                    }

                    // Add Account Button at the bottom of the list
                    item {
                        Button(
                            onClick = { showAddAccountDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 12.dp)
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                contentColor = SapphirePrimary
                            ),
                            border = BorderStroke(1.dp, SapphirePrimary.copy(alpha = 0.35f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = SapphirePrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "＋ 新增自訂帳戶",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = SapphirePrimary
                            )
                        }
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
            initialDateString = if (editingExpense == null && isCalendarView) selectedCalendarDate else null,
            accounts = customAccounts,
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
        val currentAccount = customAccounts.find { it.id == account.id } ?: account
        AccountDetailBottomSheet(
            account = currentAccount,
            monthExpenses = allMonthExpensesNoFilter,
            allExpenses = allExpenses,
            selectedMonth = selectedMonth,
            onDismiss = { viewingAccount = null },
            onEditExpense = { record ->
                editingExpense = record
                viewingAccount = null
                showAddDialog = true
            },
            onUpdateAccount = { updatedAccount ->
                viewModel.updateAccount(updatedAccount)
                viewingAccount = updatedAccount
            },
            onDeleteAccount = if (!account.id.startsWith("default_")) {
                {
                    accountToDelete = currentAccount
                }
            } else null
        )
    }

    if (showDeleteAccountsBottomSheet) {
        ManageAccountsBottomSheet(
            accounts = customAccounts,
            onDismiss = { showDeleteAccountsBottomSheet = false },
            onAddNewAccount = {
                showDeleteAccountsBottomSheet = false
                showAddAccountDialog = true
            },
            onMoveUp = { viewModel.moveAccountUp(it) },
            onMoveDown = { viewModel.moveAccountDown(it) },
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

    if (showYearMonthPicker) {
        YearMonthPickerDialog(
            currentYearMonth = selectedMonth,
            onDismiss = { showYearMonthPicker = false },
            onConfirm = { newYearMonth ->
                viewModel.setSelectedExpenseMonth(newYearMonth)
                showYearMonthPicker = false
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
    balance: Double,
    recordCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isBeforeStart: Boolean = false,
    isDragging: Boolean = false,
    translationY: Float = 0f
) {
    Box(modifier = modifier.fillMaxWidth()) {
        // 拖曳中顯示的落點影子 / 槽位預覽 (Drop Target Placeholder Shadow Slot)
        if (isDragging) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SapphirePrimary.copy(alpha = 0.08f))
                    .border(
                        BorderStroke(1.5.dp, SapphirePrimary.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = null,
                        tint = SapphirePrimary.copy(alpha = 0.75f),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "放開後移至此位置",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = SapphirePrimary.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // 浮動跟隨手指的帳戶卡片 (Floating Draggable Card)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    this.translationY = translationY
                    if (isDragging) {
                        this.shadowElevation = 16f
                        this.scaleX = 1.02f
                        this.scaleY = 1.02f
                    }
                }
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDragging) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 8.dp else 0.dp)
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
                        text = if (isBeforeStart) "尚未啟用" else "當前餘額",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isBeforeStart) "$0" else if (balance >= 0) "$${balance.toInt()}" else "-$${kotlin.math.abs(balance).toInt()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isBeforeStart) MaterialTheme.colorScheme.onSurfaceVariant else if (balance >= 0) EmeraldAccent else RoseAccent
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountDetailBottomSheet(
    account: PaymentAccount,
    monthExpenses: List<ExpenseRecord>,
    allExpenses: List<ExpenseRecord>,
    selectedMonth: String,
    onDismiss: () -> Unit,
    onEditExpense: (ExpenseRecord) -> Unit,
    onUpdateAccount: (PaymentAccount) -> Unit,
    onDeleteAccount: (() -> Unit)? = null
) {
    val methodExpenses = remember(monthExpenses, account) {
        monthExpenses.filter { it.paymentMethod == account.method }.sortedByDescending { it.dateString }
    }
    val totalExpense = methodExpenses.filter { it.type == ExpenseType.EXPENSE }.sumOf { it.amount }
    val totalIncome = methodExpenses.filter { it.type == ExpenseType.INCOME }.sumOf { it.amount }
    val net = totalIncome - totalExpense

    val isBeforeStart = selectedMonth < account.startYearMonth
    val cumulativeExpenses = remember(allExpenses, account, selectedMonth) {
        allExpenses.filter {
            it.paymentMethod == account.method && it.dateString.substringBeforeLast("-") <= selectedMonth
        }
    }
    val cumExp = cumulativeExpenses.filter { it.type == ExpenseType.EXPENSE }.sumOf { it.amount }
    val cumInc = cumulativeExpenses.filter { it.type == ExpenseType.INCOME }.sumOf { it.amount }
    val currentBalance = if (isBeforeStart) 0.0 else account.initialBalance + (cumInc - cumExp)

    var showEditInitialBalanceDialog by remember { mutableStateOf(false) }

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
            // Header: Account Title, Type & Delete Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SapphirePrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getPaymentMethodIcon(account.method),
                            contentDescription = null,
                            tint = SapphirePrimary,
                            modifier = Modifier.size(22.dp)
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

                if (onDeleteAccount != null) {
                    IconButton(
                        onClick = onDeleteAccount,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "刪除此帳戶",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Summary Card (Initial Balance with Edit, Net Income/Expense, Current Balance)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Initial Balance with Edit Button
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showEditInitialBalanceDialog = true }
                            .padding(vertical = 2.dp, horizontal = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "起始餘額",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "設定初始餘額",
                                tint = if (isBeforeStart) MaterialTheme.colorScheme.onSurfaceVariant else SapphirePrimary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Text(
                            text = if (isBeforeStart) "$0" else "$${account.initialBalance.toInt()}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isBeforeStart) MaterialTheme.colorScheme.onSurfaceVariant else SapphirePrimary
                        )
                    }

                    // Month Net
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "本月收支",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${if (net >= 0) "+" else ""}$${net.toInt()}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (net >= 0) EmeraldAccent else RoseAccent
                        )
                    }

                    // Current Balance
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = if (isBeforeStart) "尚未啟用" else "當前餘額",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isBeforeStart) "$0" else if (currentBalance >= 0) "$${currentBalance.toInt()}" else "-$${kotlin.math.abs(currentBalance).toInt()}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isBeforeStart) MaterialTheme.colorScheme.onSurfaceVariant else if (currentBalance >= 0) EmeraldAccent else RoseAccent
                        )
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
                        .heightIn(max = 380.dp),
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

                    if (onDeleteAccount != null) {
                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = onDeleteAccount,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("刪除此自訂帳戶", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditInitialBalanceDialog) {
        var newBalanceText by remember {
            mutableStateOf(
                if (isBeforeStart) "0"
                else if (account.initialBalance > 0) account.initialBalance.toInt().toString()
                else ""
            )
        }
        AlertDialog(
            onDismissRequest = { showEditInitialBalanceDialog = false },
            title = { Text("設定「${account.name}」起始餘額", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "設定此帳戶的起始基礎餘額，APP 將自動結合收支計算出即時當前餘額：",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = newBalanceText,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.all { it.isDigit() }) {
                                newBalanceText = input
                            }
                        },
                        label = { Text("起始餘額 ($)") },
                        placeholder = { Text("0") },
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
                        val amount = newBalanceText.toDoubleOrNull() ?: 0.0
                        onUpdateAccount(
                            account.copy(
                                initialBalance = amount,
                                startYearMonth = if (isBeforeStart && amount > 0) selectedMonth else account.startYearMonth
                            )
                        )
                        showEditInitialBalanceDialog = false
                    }
                ) {
                    Text("儲存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditInitialBalanceDialog = false }) {
                    Text("取消")
                }
            }
        )
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
                    "學生證" to PaymentMethod.IC_CARD,
                    "一卡通" to PaymentMethod.IC_CARD,
                    "悠遊卡" to PaymentMethod.IC_CARD,
                    "郵局" to PaymentMethod.TRANSFER,
                    "全支付" to PaymentMethod.MOBILE_PAY,
                    "LINE Pay" to PaymentMethod.MOBILE_PAY,
                    "Google Pay" to PaymentMethod.MOBILE_PAY,

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
                            val inferredMethod = when {
                                name.contains("卡") || name.contains("Card", ignoreCase = true) || name.contains("簽帳") -> PaymentMethod.CARD
                                name.contains("悠遊") || name.contains("一卡通") -> PaymentMethod.IC_CARD
                                name.contains("Pay", ignoreCase = true) || name.contains("街口") || name.contains("行動") || name.contains("支付") -> PaymentMethod.MOBILE_PAY
                                name.contains("銀行") || name.contains("轉帳") || name.contains("郵局") || name.contains("帳戶") || name.contains("活存") -> PaymentMethod.TRANSFER
                                else -> selectedMethod
                            }
                            val account = PaymentAccount(
                                name = name.trim(),
                                method = inferredMethod,
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
private fun ManageAccountsBottomSheet(
    accounts: List<PaymentAccount>,
    onDismiss: () -> Unit,
    onAddNewAccount: () -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "管理與排序支付帳戶",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Text(
                text = "可使用上下箭頭調整帳戶順序，或刪除自訂帳戶",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Button(
                onClick = onAddNewAccount,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SapphirePrimary)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("＋ 新增自訂支付帳戶", fontWeight = FontWeight.Bold)
            }

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
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(accounts) { index, account ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
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

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    IconButton(
                                        onClick = { onMoveUp(index) },
                                        enabled = index > 0,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowUp,
                                            contentDescription = "上移",
                                            tint = if (index > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { onMoveDown(index) },
                                        enabled = index < accounts.size - 1,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "下移",
                                            tint = if (index < accounts.size - 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    if (!account.id.startsWith("default_")) {
                                        IconButton(
                                            onClick = { onDeleteRequest(account) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "刪除 ${account.name}",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
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
                        text = "-$${totalExp.toInt()}",
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

private data class CalendarGridCell(
    val dayNumber: Int?,
    val dateString: String?,
    val isToday: Boolean,
    val isSelected: Boolean,
    val totalExpense: Double,
    val totalIncome: Double
)

@Composable
private fun ExpenseMonthlyCalendarCard(
    selectedMonth: String,
    selectedDate: String,
    expenses: List<ExpenseRecord>,
    onSelectDate: (String) -> Unit
) {
    val locale = LocalConfiguration.current.locales[0]
    val todayString = remember(locale) {
        SimpleDateFormat("yyyy-MM-dd", locale).format(Date())
    }

    val gridCells = remember(selectedMonth, selectedDate, expenses, todayString) {
        val parts = selectedMonth.split("-")
        val year = parts.getOrNull(0)?.toIntOrNull() ?: 2026
        val month = parts.getOrNull(1)?.toIntOrNull() ?: 8

        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        // Mon=0, Tue=1, Wed=2, Thu=3, Fri=4, Sat=5, Sun=6
        val leadingEmpty = (firstDayOfWeek - Calendar.MONDAY + 7) % 7

        val dailyMap = expenses.groupBy { it.dateString }
        val cells = mutableListOf<CalendarGridCell>()

        // Leading empty slots
        repeat(leadingEmpty) {
            cells.add(CalendarGridCell(null, null, isToday = false, isSelected = false, totalExpense = 0.0, totalIncome = 0.0))
        }

        // Days in month
        for (d in 1..daysInMonth) {
            val dateStr = String.format(Locale.US, "%04d-%02d-%02d", year, month, d)
            val dayRecords = dailyMap[dateStr].orEmpty()
            val exp = dayRecords.filter { it.type == ExpenseType.EXPENSE }.sumOf { it.amount }
            val inc = dayRecords.filter { it.type == ExpenseType.INCOME }.sumOf { it.amount }
            cells.add(
                CalendarGridCell(
                    dayNumber = d,
                    dateString = dateStr,
                    isToday = dateStr == todayString,
                    isSelected = dateStr == selectedDate,
                    totalExpense = exp,
                    totalIncome = inc
                )
            )
        }

        // Trailing empty slots to complete rows of 7
        val trailingEmpty = (7 - (cells.size % 7)) % 7
        repeat(trailingEmpty) {
            cells.add(CalendarGridCell(null, null, isToday = false, isSelected = false, totalExpense = 0.0, totalIncome = 0.0))
        }

        cells
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Weekday Headers: 一 二 三 四 五 六 日
            val weekDays = listOf("一", "二", "三", "四", "五", "六", "日")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                weekDays.forEachIndexed { idx, day ->
                    val isWeekend = idx >= 5
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isWeekend) RoseAccent.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

            // Calendar Rows
            val rows = gridCells.chunked(7)
            rows.forEach { rowCells ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    rowCells.forEach { cell ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                        ) {
                            if (cell.dayNumber != null && cell.dateString != null) {
                                val isSelected = cell.isSelected
                                val isToday = cell.isToday
                                val hasExpense = cell.totalExpense > 0
                                val hasIncome = cell.totalIncome > 0

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            when {
                                                isSelected -> SapphirePrimary.copy(alpha = 0.18f)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .then(
                                            when {
                                                isSelected -> Modifier.border(
                                                    1.5.dp,
                                                    SapphirePrimary,
                                                    RoundedCornerShape(10.dp)
                                                )
                                                isToday -> Modifier.border(
                                                    1.dp,
                                                    SapphirePrimary.copy(alpha = 0.55f),
                                                    RoundedCornerShape(10.dp)
                                                )
                                                else -> Modifier
                                            }
                                        )
                                        .clickable { onSelectDate(cell.dateString) }
                                        .padding(vertical = 2.dp, horizontal = 1.dp),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        // Day Number with Today Dot Indicator
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "${cell.dayNumber}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = if (isSelected || isToday) FontWeight.ExtraBold else FontWeight.Medium,
                                                color = when {
                                                    isSelected -> SapphirePrimary
                                                    isToday -> SapphirePrimary
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                }
                                            )
                                            if (isToday) {
                                                Box(
                                                    modifier = Modifier
                                                        .padding(start = 2.dp)
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(SapphirePrimary)
                                                )
                                            }
                                        }

                                        // Amount Tag
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        ) {
                                            if (hasExpense) {
                                                Text(
                                                    text = "-$${cell.totalExpense.toInt()}",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                    fontWeight = FontWeight.Bold,
                                                    color = RoseAccent,
                                                    maxLines = 1
                                                )
                                            } else if (hasIncome) {
                                                Text(
                                                    text = "+$${cell.totalIncome.toInt()}",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                    fontWeight = FontWeight.Bold,
                                                    color = EmeraldAccent,
                                                    maxLines = 1
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
        }
    }
}

@Composable
private fun YearMonthPickerDialog(
    currentYearMonth: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val parts = currentYearMonth.split("-")
    val initialYear = parts.getOrNull(0)?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
    val initialMonth = parts.getOrNull(1)?.toIntOrNull() ?: (Calendar.getInstance().get(Calendar.MONTH) + 1)

    var selectedYear by remember { mutableIntStateOf(initialYear) }
    var selectedMonth by remember { mutableIntStateOf(initialMonth) }

    val currentCal = Calendar.getInstance()
    val thisYear = currentCal.get(Calendar.YEAR)
    val thisMonth = currentCal.get(Calendar.MONTH) + 1

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "選擇記帳年月",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = {
                        selectedYear = thisYear
                        selectedMonth = thisMonth
                    }
                ) {
                    Text("回到本月", color = SapphirePrimary, fontWeight = FontWeight.Bold)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Year Switcher Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { selectedYear-- }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "上一年")
                    }
                    Text(
                        text = "$selectedYear 年",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { selectedYear++ }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "下一年")
                    }
                }

                // 12 Months Grid
                val months = (1..12).toList().chunked(4)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    months.forEach { rowMonths ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowMonths.forEach { m ->
                                val isSelected = selectedMonth == m
                                val isCurrentActualMonth = selectedYear == thisYear && m == thisMonth

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) SapphirePrimary
                                            else if (isCurrentActualMonth) SapphirePrimary.copy(alpha = 0.12f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                        )
                                        .then(
                                            if (isCurrentActualMonth && !isSelected) {
                                                Modifier.border(1.dp, SapphirePrimary, RoundedCornerShape(10.dp))
                                            } else Modifier
                                        )
                                        .clickable { selectedMonth = m },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${m}月",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected || isCurrentActualMonth) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else if (isCurrentActualMonth) SapphirePrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val formatted = String.format(Locale.US, "%04d-%02d", selectedYear, selectedMonth)
                    onConfirm(formatted)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SapphirePrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("確定", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
