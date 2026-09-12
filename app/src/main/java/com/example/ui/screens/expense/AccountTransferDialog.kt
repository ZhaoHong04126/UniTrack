package com.example.ui.screens.expense

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ExpenseRecord
import com.example.data.model.ExpenseType
import com.example.data.model.PaymentAccount
import com.example.data.model.PaymentMethod
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountTransferDialog(
    accounts: List<PaymentAccount>,
    getAccountBalance: (PaymentAccount) -> Double,
    onDismiss: () -> Unit,
    onConfirm: (
        fromAccount: PaymentAccount,
        toAccount: PaymentAccount,
        amount: Double,
        dateString: String,
        timestamp: Long,
        note: String
    ) -> Unit,
    initialFromAccount: PaymentAccount? = null,
    initialToAccount: PaymentAccount? = null,
    initialAmount: Double? = null,
    initialDateString: String? = null,
    initialTimeString: String? = null,
    initialNote: String? = null,
    isEditing: Boolean = false
) {
    if (accounts.size < 2) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = SapphirePrimary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "無法進行帳戶轉帳",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "目前帳戶數量少於兩個，無法進行內部轉帳。請先於帳戶管理中「新增自訂帳戶」後再使用本功能。",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = SapphirePrimary)
                ) {
                    Text("了解")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
        return
    }

    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val dateFormat = remember(locale) { SimpleDateFormat("yyyy-MM-dd", locale) }
    val timeFormat = remember(locale) { SimpleDateFormat("HH:mm", locale) }

    var fromAccount by remember {
        mutableStateOf(initialFromAccount ?: accounts[0])
    }
    var toAccount by remember {
        mutableStateOf(initialToAccount ?: (if (accounts.size > 1) accounts[1] else accounts[0]))
    }
    var amountText by remember {
        mutableStateOf(
            initialAmount?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: ""
        )
    }
    var note by remember { mutableStateOf(initialNote ?: "") }
    var dateString by remember { mutableStateOf(initialDateString ?: dateFormat.format(Date())) }
    var timeString by remember { mutableStateOf(initialTimeString ?: timeFormat.format(Date())) }

    var showFromAccountPicker by remember { mutableStateOf(false) }
    var showToAccountPicker by remember { mutableStateOf(false) }

    val fromBalance = getAccountBalance(fromAccount)
    val toBalance = getAccountBalance(toAccount)
    val amountVal = amountText.toDoubleOrNull() ?: 0.0
    val isValid = amountVal > 0 && fromAccount.id != toAccount.id

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
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row: Title & Close Button
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
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SapphirePrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = SapphirePrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = if (isEditing) "編輯轉帳" else "帳戶內轉帳",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isEditing) "修改轉帳內容，系統將自動同步成對記錄" else "帳戶間資金調度，不影響當月支出預算",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "關閉",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Account Selection Block (Side-by-Side: From <-> To)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 轉出帳戶 (Left)
                    Box(modifier = Modifier.weight(1f)) {
                        AccountSelectorBox(
                            title = "轉出帳戶",
                            account = fromAccount,
                            balance = fromBalance,
                            isSource = true,
                            onClick = { showFromAccountPicker = true }
                        )

                        DropdownMenu(
                            expanded = showFromAccountPicker,
                            onDismissRequest = { showFromAccountPicker = false },
                            modifier = Modifier.widthIn(min = 160.dp)
                        ) {
                            accounts.forEach { acc ->
                                val isSelected = acc.id == fromAccount.id
                                val bal = getAccountBalance(acc)
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(RoseLight),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = getAccountIcon(acc.method),
                                                    contentDescription = null,
                                                    tint = RoseAccent,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Column {
                                                Text(
                                                    text = acc.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                                Text(
                                                    text = "餘額: $${bal.toInt()}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    },
                                    trailingIcon = if (isSelected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = SapphirePrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    } else null,
                                    onClick = {
                                        fromAccount = acc
                                        if (toAccount.id == acc.id) {
                                            toAccount = accounts.firstOrNull { it.id != acc.id } ?: acc
                                        }
                                        showFromAccountPicker = false
                                    }
                                )
                            }
                        }
                    }

                    // 互換按鈕 (Center)
                    IconButton(
                        onClick = {
                            val temp = fromAccount
                            fromAccount = toAccount
                            toAccount = temp
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SapphirePrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "互換帳戶",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // 轉入帳戶 (Right)
                    Box(modifier = Modifier.weight(1f)) {
                        AccountSelectorBox(
                            title = "轉入帳戶",
                            account = toAccount,
                            balance = toBalance,
                            isSource = false,
                            onClick = { showToAccountPicker = true }
                        )

                        DropdownMenu(
                            expanded = showToAccountPicker,
                            onDismissRequest = { showToAccountPicker = false },
                            modifier = Modifier.widthIn(min = 160.dp)
                        ) {
                            accounts.forEach { acc ->
                                val isSelected = acc.id == toAccount.id
                                val bal = getAccountBalance(acc)
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(EmeraldLight),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = getAccountIcon(acc.method),
                                                    contentDescription = null,
                                                    tint = EmeraldAccent,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Column {
                                                Text(
                                                    text = acc.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                                Text(
                                                    text = "餘額: $${bal.toInt()}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    },
                                    trailingIcon = if (isSelected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = SapphirePrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    } else null,
                                    onClick = {
                                        toAccount = acc
                                        if (fromAccount.id == acc.id) {
                                            fromAccount = accounts.firstOrNull { it.id != acc.id } ?: acc
                                        }
                                        showToAccountPicker = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Amount Input
            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    if (input.isEmpty() || input.all { it.isDigit() }) {
                        amountText = input
                    }
                },
                label = { Text("轉帳金額 ($) *") },
                placeholder = { Text("0") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.AttachMoney,
                        contentDescription = null,
                        tint = SapphirePrimary
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Date & Time Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedCard(
                    onClick = {
                        val cal = Calendar.getInstance()
                        runCatching {
                            val parts = dateString.split("-")
                            cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
                        }
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                dateString = String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d)
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = SapphirePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = dateString,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                OutlinedCard(
                    onClick = {
                        val cal = Calendar.getInstance()
                        runCatching {
                            val parts = timeString.split(":")
                            cal.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                            cal.set(Calendar.MINUTE, parts[1].toInt())
                        }
                        TimePickerDialog(
                            context,
                            { _, hourOfDay, minute ->
                                timeString = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
                            },
                            cal.get(Calendar.HOUR_OF_DAY),
                            cal.get(Calendar.MINUTE),
                            true
                        ).show()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = SapphirePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = timeString,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Note Input
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("備註用途 (選填)") },
                placeholder = { Text("例如：ATM 提款、悠遊卡儲值、生活費...") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Transfer Summary Banner
            if (amountVal > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SapphirePrimary.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, SapphirePrimary.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SapphirePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "從【${fromAccount.name}】轉入【${toAccount.name}】 $${amountVal.toInt()}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = SapphirePrimary
                        )
                    }
                }
            }

            // Action Buttons (取消 & 確認轉帳)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("取消")
                }
                Button(
                    onClick = {
                        if (isValid) {
                            val cal = Calendar.getInstance()
                            runCatching {
                                val dParts = dateString.split("-")
                                val tParts = timeString.split(":")
                                cal.set(
                                    dParts[0].toInt(),
                                    dParts[1].toInt() - 1,
                                    dParts[2].toInt(),
                                    tParts[0].toInt(),
                                    tParts[1].toInt()
                                )
                            }
                            onConfirm(
                                fromAccount,
                                toAccount,
                                amountVal,
                                dateString,
                                cal.timeInMillis,
                                note
                            )
                            onDismiss()
                        }
                    },
                    enabled = isValid,
                    modifier = Modifier
                        .weight(2f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SapphirePrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (isEditing) Icons.Default.Save else Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isEditing) "儲存修改" else "確認轉帳", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AccountSelectorBox(
    title: String,
    account: PaymentAccount,
    balance: Double,
    isSource: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSource) RoseAccent else EmeraldAccent,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSource) RoseLight else EmeraldLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getAccountIcon(account.method),
                        contentDescription = null,
                        tint = if (isSource) RoseAccent else EmeraldAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$${balance.toInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

internal fun getAccountIcon(method: PaymentMethod): ImageVector {
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
fun TransferDetailDialog(
    record: ExpenseRecord,
    onDismiss: () -> Unit,
    onDelete: (ExpenseRecord) -> Unit,
    onEdit: (ExpenseRecord) -> Unit = {}
) {
    var showConfirmDelete by remember { mutableStateOf(false) }
    val cleanNote = remember(record.note) {
        record.note.replace(Regex("""\[(pair|from|to):[^]]+]"""), "").trim()
    }
    val isTransferOut = record.type == ExpenseType.TRANSFER_OUT

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
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row: Title & Close Button
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
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SapphirePrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = SapphirePrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = "轉帳記錄明細",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "關閉",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Big Amount Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SapphirePrimary.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isTransferOut) "-$${record.amount.toInt()}" else "+$${record.amount.toInt()}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isTransferOut) SapphirePrimary else Color(0xFF0284C7)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = record.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Details Block
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DetailRow(label = "交易類型", value = if (isTransferOut) "帳戶轉出" else "帳戶轉入")
                DetailRow(label = "關聯方式", value = record.paymentMethod.label)
                val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
                val timeStr = if (record.timestamp > 0) " " + timeFormat.format(Date(record.timestamp)) else ""
                DetailRow(label = "交易時間", value = "${record.dateString}$timeStr")
                if (cleanNote.isNotBlank()) {
                    DetailRow(label = "備註用途", value = cleanNote)
                }
            }

            // Action Buttons (刪除記錄 & 關閉)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { showConfirmDelete = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("刪除記錄")
                }

                Button(
                    onClick = {
                        onDismiss()
                        onEdit(record)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SapphirePrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("編輯轉帳")
                }
            }
        }
    }

    if (showConfirmDelete) {
        AlertDialog(
            onDismissRequest = { showConfirmDelete = false },
            title = { Text("確認刪除轉帳記錄", fontWeight = FontWeight.Bold) },
            text = { Text("確定要刪除這筆轉帳嗎？系統將同步刪除對應帳戶之成對記錄，並還原雙方帳戶餘額。") },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDelete = false
                        onDelete(record)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("確認刪除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDelete = false }) {
                    Text("取消")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}
