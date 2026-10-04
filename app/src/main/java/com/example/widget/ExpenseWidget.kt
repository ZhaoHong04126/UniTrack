package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.model.ExpenseCategory
import com.example.data.model.ExpenseType
import com.example.util.NotificationHelper
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@Suppress("SpellCheckingInspection")
class ExpenseWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH_EXPENSE = "com.example.widget.ACTION_REFRESH_EXPENSE"
        const val EXTRA_QUICK_ACTION = "quick_action"
        const val QUICK_ACTION_ADD_EXPENSE = "add_expense"

        fun getCategoryEmoji(category: ExpenseCategory, type: ExpenseType): String {
            if (type == ExpenseType.INCOME) return "💰"
            return when (category) {
                ExpenseCategory.FOOD -> "🍱"
                ExpenseCategory.BOOKS_STUDY -> "📚"
                ExpenseCategory.TRANSPORT -> "🚌"
                ExpenseCategory.RENT_UTILITY -> "🏠"
                ExpenseCategory.ENTERTAINMENT -> "🎮"
                ExpenseCategory.DAILY -> "🛍️"
                ExpenseCategory.SALARY_JOB -> "💼"
                ExpenseCategory.SCHOLARSHIP -> "🎓"
                ExpenseCategory.OTHER -> "💳"
            }
        }

        fun formatRelativeDate(dateString: String, todayStr: String): String {
            if (dateString.isBlank()) return ""
            return when (dateString) {
                todayStr -> "今天"
                LocalDate.now().minusDays(1).toString() -> "昨天"
                else -> {
                    try {
                        val parts = dateString.split("-")
                        if (parts.size == 3) "${parts[1]}/${parts[2]}" else dateString
                    } catch (_: Exception) {
                        dateString
                    }
                }
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_EXPENSE || intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
            if (ids != null && ids.isNotEmpty()) {
                for (id in ids) {
                    updateWidget(context, appWidgetManager, id)
                }
            } else {
                val componentName = ComponentName(context, ExpenseWidget::class.java)
                val allIds = appWidgetManager.getAppWidgetIds(componentName)
                if (allIds != null) {
                    for (id in allIds) {
                        updateWidget(context, appWidgetManager, id)
                    }
                }
            }
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_expense)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = context.getSharedPreferences("unitrack_prefs", Context.MODE_PRIVATE)
                val lastUid = prefs.getString("last_logged_in_uid", null)
                val isFirebaseLoggedIn = try {
                    FirebaseApp.getApps(context).isNotEmpty() &&
                        FirebaseAuth.getInstance().currentUser != null
                } catch (_: Exception) {
                    false
                }
                val isLoggedIn = isFirebaseLoggedIn || !lastUid.isNullOrBlank()

                // 1. 綁定點擊整個 Widget 跳轉至記帳本或登入頁面
                val mainIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(NotificationHelper.EXTRA_NAV_ROUTE, if (isLoggedIn) "expense" else "auth")
                }
                val mainPendingIntent = PendingIntent.getActivity(
                    context,
                    101,
                    mainIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, mainPendingIntent)

                // 2. 綁定點擊「記一筆」按鈕直達新增記帳
                val addExpenseIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(NotificationHelper.EXTRA_NAV_ROUTE, if (isLoggedIn) "expense" else "auth")
                    if (isLoggedIn) {
                        putExtra(EXTRA_QUICK_ACTION, QUICK_ACTION_ADD_EXPENSE)
                    }
                }
                val addPendingIntent = PendingIntent.getActivity(
                    context,
                    102,
                    addExpenseIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.btn_widget_add_expense, addPendingIntent)

                // 3. 綁定手動重新整理按鈕
                val refreshIntent = Intent(context, ExpenseWidget::class.java).apply {
                    action = ACTION_REFRESH_EXPENSE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
                }
                val refreshPendingIntent = PendingIntent.getBroadcast(
                    context,
                    appWidgetId,
                    refreshIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.btn_widget_refresh, refreshPendingIntent)

                val today = LocalDate.now()
                val currentYearMonth = today.format(DateTimeFormatter.ofPattern("yyyy-MM"))
                val todayDateStr = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                val monthNum = today.monthValue

                views.setTextViewText(R.id.tv_widget_month_badge, "${monthNum}月")

                // 若尚未登入，顯示專屬未登入狀態提示
                if (!isLoggedIn) {
                    views.setTextViewText(R.id.tv_widget_subtitle, "尚未登入帳號")
                    views.setViewVisibility(R.id.layout_budget_card, View.GONE)
                    views.setViewVisibility(R.id.layout_transactions_container, View.GONE)
                    views.setViewVisibility(R.id.layout_empty_state, View.VISIBLE)
                    views.setTextViewText(R.id.tv_empty_title, "🔒 尚未登入帳號")
                    views.setTextViewText(R.id.tv_empty_desc, "點擊此處登入以查看收支明細與預算進度")
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                    return@launch
                }

                val db = AppDatabase.getDatabase(context)
                val allExpenses = db.expenseDao().getAllExpensesOnce()
                val monthExpenses = allExpenses.filter { it.dateString.startsWith(currentYearMonth) }

                // 計算當月支出總額與今日支出
                var totalMonthExpense = 0.0
                for (item in monthExpenses) {
                    if (item.type == ExpenseType.EXPENSE || item.type == ExpenseType.TRANSFER_OUT) {
                        totalMonthExpense += item.amount
                    }
                }

                var todayExpense = 0.0
                for (item in allExpenses) {
                    if (item.dateString == todayDateStr &&
                        (item.type == ExpenseType.EXPENSE || item.type == ExpenseType.TRANSFER_OUT)
                    ) {
                        todayExpense += item.amount
                    }
                }

                // 取得當月預算
                val budget = db.expenseDao().getBudgetForMonthOnce(currentYearMonth)
                val budgetAmount = budget?.budgetAmount ?: 12000.0

                // 更新今日支出副標題
                views.setTextViewText(
                    R.id.tv_widget_subtitle,
                    String.format(Locale.getDefault(), "今日已支出 $%,d", todayExpense.toLong())
                )

                // 綁定本月預算卡片資訊
                views.setViewVisibility(R.id.layout_budget_card, View.VISIBLE)
                views.setTextViewText(
                    R.id.tv_month_expense,
                    String.format(Locale.getDefault(), "$ %,d", totalMonthExpense.toLong())
                )

                val remainingBudget = budgetAmount - totalMonthExpense
                val percent = if (budgetAmount > 0) {
                    ((totalMonthExpense / budgetAmount) * 100).toInt()
                } else {
                    0
                }

                if (remainingBudget < 0) {
                    // 超支警示狀態
                    views.setTextViewText(R.id.tv_remaining_label, "已超支")
                    views.setTextViewText(
                        R.id.tv_remaining_budget,
                        String.format(Locale.getDefault(), "-$ %,d ⚠️", abs(remainingBudget).toLong())
                    )
                    views.setTextColor(
                        R.id.tv_remaining_budget,
                        ContextCompat.getColor(context, R.color.widget_expense_danger)
                    )
                    views.setInt(
                        R.id.layout_budget_card,
                        "setBackgroundResource",
                        R.drawable.widget_expense_budget_warning_bg
                    )
                    views.setProgressBar(R.id.pb_expense_budget, 100, 100, false)
                    views.setTextViewText(
                        R.id.tv_budget_percent,
                        String.format(Locale.getDefault(), "已使用 %d%% (超出預算)", percent)
                    )
                } else {
                    // 正常預算狀態
                    views.setTextViewText(R.id.tv_remaining_label, "剩餘預算")
                    views.setTextViewText(
                        R.id.tv_remaining_budget,
                        String.format(Locale.getDefault(), "$ %,d", remainingBudget.toLong())
                    )
                    views.setTextColor(
                        R.id.tv_remaining_budget,
                        ContextCompat.getColor(context, R.color.widget_primary_text)
                    )
                    views.setInt(
                        R.id.layout_budget_card,
                        "setBackgroundResource",
                        R.drawable.widget_expense_budget_bg
                    )
                    views.setProgressBar(
                        R.id.pb_expense_budget,
                        100,
                        percent.coerceIn(0, 100),
                        false
                    )
                    views.setTextViewText(
                        R.id.tv_budget_percent,
                        String.format(Locale.getDefault(), "已使用 %d%%", percent)
                    )
                }

                views.setTextViewText(
                    R.id.tv_budget_limit,
                    String.format(Locale.getDefault(), "預算上限 $%,d", budgetAmount.toLong())
                )

                // 綁定近期明細
                if (monthExpenses.isEmpty()) {
                    views.setViewVisibility(R.id.layout_transactions_container, View.GONE)
                    views.setViewVisibility(R.id.layout_empty_state, View.VISIBLE)
                    views.setTextViewText(R.id.tv_empty_title, "📝 本月尚無記帳記錄")
                    views.setTextViewText(R.id.tv_empty_desc, "點擊右上角「記一筆」快速記錄收支")
                } else {
                    views.setViewVisibility(R.id.layout_empty_state, View.GONE)
                    views.setViewVisibility(R.id.layout_transactions_container, View.VISIBLE)

                    val recent = monthExpenses.take(3)
                    val itemLayoutIds = listOf(
                        R.id.item_expense_1,
                        R.id.item_expense_2,
                        R.id.item_expense_3
                    )
                    val iconViewIds = listOf(
                        R.id.tv_expense_icon_1,
                        R.id.tv_expense_icon_2,
                        R.id.tv_expense_icon_3
                    )
                    val titleViewIds = listOf(
                        R.id.tv_expense_title_1,
                        R.id.tv_expense_title_2,
                        R.id.tv_expense_title_3
                    )
                    val detailViewIds = listOf(
                        R.id.tv_expense_detail_1,
                        R.id.tv_expense_detail_2,
                        R.id.tv_expense_detail_3
                    )
                    val amountViewIds = listOf(
                        R.id.tv_expense_amount_1,
                        R.id.tv_expense_amount_2,
                        R.id.tv_expense_amount_3
                    )

                    for (i in itemLayoutIds.indices) {
                        if (i < recent.size) {
                            val exp = recent[i]
                            views.setViewVisibility(itemLayoutIds[i], View.VISIBLE)
                            views.setTextViewText(iconViewIds[i], getCategoryEmoji(exp.category, exp.type))
                            views.setTextViewText(
                                titleViewIds[i],
                                exp.title.ifBlank { exp.category.label }
                            )

                            val dateRelative = formatRelativeDate(exp.dateString, todayDateStr)
                            val detailStr = listOf(
                                exp.category.label,
                                exp.paymentMethod.label,
                                dateRelative
                            ).filter { it.isNotBlank() }.joinToString(" • ")
                            views.setTextViewText(detailViewIds[i], detailStr)

                            val isExpense = exp.type == ExpenseType.EXPENSE || exp.type == ExpenseType.TRANSFER_OUT
                            val sign = if (isExpense) "-" else "+"
                            views.setTextViewText(
                                amountViewIds[i],
                                String.format(Locale.getDefault(), "%s $%,d", sign, exp.amount.toLong())
                            )
                            views.setTextColor(
                                amountViewIds[i],
                                ContextCompat.getColor(
                                    context,
                                    if (isExpense) R.color.widget_expense_danger else R.color.widget_expense_success
                                )
                            )
                        } else {
                            views.setViewVisibility(itemLayoutIds[i], View.GONE)
                        }
                    }

                    if (monthExpenses.size > 3) {
                        views.setViewVisibility(R.id.tv_more_expenses, View.VISIBLE)
                        views.setTextViewText(
                            R.id.tv_more_expenses,
                            String.format(Locale.getDefault(), "+ 還有 %d 筆收支，點擊查看全部", monthExpenses.size - 3)
                        )
                    } else {
                        views.setViewVisibility(R.id.tv_more_expenses, View.GONE)
                    }
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (_: Exception) {
                // 容錯防護
            }
        }
    }
}
