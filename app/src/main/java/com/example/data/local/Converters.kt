package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.CourseCategory
import com.example.data.model.ExpenseCategory
import com.example.data.model.ExpenseType
import com.example.data.model.GeneralEduSubtype
import com.example.data.model.GpaScale
import com.example.data.model.PaymentMethod

class Converters {
    @TypeConverter
    fun fromCourseCategory(value: CourseCategory?): String? = value?.name

    @TypeConverter
    fun toCourseCategory(value: String?): CourseCategory? =
        value?.let { runCatching { CourseCategory.valueOf(it) }.getOrDefault(CourseCategory.REQUIRED) }

    @TypeConverter
    fun fromGeneralEduSubtype(value: GeneralEduSubtype?): String? = value?.name

    @TypeConverter
    fun toGeneralEduSubtype(value: String?): GeneralEduSubtype? =
        value?.let { runCatching { GeneralEduSubtype.valueOf(it) }.getOrDefault(GeneralEduSubtype.NONE) }

    @TypeConverter
    fun fromExpenseType(value: ExpenseType?): String? = value?.name

    @TypeConverter
    fun toExpenseType(value: String?): ExpenseType? =
        value?.let { runCatching { ExpenseType.valueOf(it) }.getOrDefault(ExpenseType.EXPENSE) }

    @TypeConverter
    fun fromExpenseCategory(value: ExpenseCategory?): String? = value?.name

    @TypeConverter
    fun toExpenseCategory(value: String?): ExpenseCategory? =
        value?.let { runCatching { ExpenseCategory.valueOf(it) }.getOrDefault(ExpenseCategory.FOOD) }

    @TypeConverter
    fun fromPaymentMethod(value: PaymentMethod?): String? = value?.name

    @TypeConverter
    fun toPaymentMethod(value: String?): PaymentMethod? =
        value?.let { runCatching { PaymentMethod.valueOf(it) }.getOrDefault(PaymentMethod.CASH) }

    @TypeConverter
    fun fromGpaScale(value: GpaScale?): String? = value?.name

    @TypeConverter
    fun toGpaScale(value: String?): GpaScale? =
        value?.let { runCatching { GpaScale.valueOf(it) }.getOrDefault(GpaScale.SCALE_4_3) }
}
