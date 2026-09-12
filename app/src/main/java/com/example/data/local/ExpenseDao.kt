package com.example.data.local

import androidx.room.*
import com.example.data.model.ExpenseRecord
import com.example.data.model.MonthlyBudget
import kotlinx.coroutines.flow.Flow

@Suppress("unused")
@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpenses(): Flow<List<ExpenseRecord>>

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    suspend fun getAllExpensesOnce(): List<ExpenseRecord>

    @Query("SELECT * FROM expenses WHERE dateString LIKE :yearMonth || '%' ORDER BY timestamp DESC")
    fun getExpensesByMonth(yearMonth: String): Flow<List<ExpenseRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<ExpenseRecord>)

    @Update
    suspend fun updateExpense(expense: ExpenseRecord)

    @Delete
    suspend fun deleteExpense(expense: ExpenseRecord)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Long)

    @Query("DELETE FROM expenses")
    suspend fun deleteAllExpenses()

    // Monthly Budgets
    @Query("SELECT * FROM monthly_budgets")
    fun getAllBudgets(): Flow<List<MonthlyBudget>>

    @Query("SELECT * FROM monthly_budgets")
    suspend fun getAllBudgetsOnce(): List<MonthlyBudget>

    @Query("SELECT * FROM monthly_budgets WHERE yearMonth = :yearMonth")
    fun getBudgetForMonth(yearMonth: String): Flow<MonthlyBudget?>

    @Query("SELECT * FROM monthly_budgets WHERE yearMonth = :yearMonth")
    suspend fun getBudgetForMonthOnce(yearMonth: String): MonthlyBudget?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setBudget(budget: MonthlyBudget)

    @Delete
    suspend fun deleteBudget(budget: MonthlyBudget)

    @Query("DELETE FROM monthly_budgets")
    suspend fun deleteAllBudgets()

    @Transaction
    suspend fun syncAllExpenses(downloadedExpenses: List<ExpenseRecord>) {
        val downloadedIds = downloadedExpenses.map { it.id }.toSet()
        val local = getAllExpensesOnce()
        for (e in local) {
            if (e.id !in downloadedIds) {
                deleteExpense(e)
            }
        }
        if (downloadedExpenses.isNotEmpty()) {
            insertExpenses(downloadedExpenses)
        }
    }

    @Transaction
    suspend fun syncAllBudgets(downloadedBudgets: List<MonthlyBudget>) {
        val downloadedMonths = downloadedBudgets.map { it.yearMonth }.toSet()
        val local = getAllBudgetsOnce()
        for (b in local) {
            if (b.yearMonth !in downloadedMonths) {
                // Remove local budgets that don't exist on cloud
                deleteBudget(b)
            }
        }
        for (b in downloadedBudgets) {
            setBudget(b)
        }
    }
}
