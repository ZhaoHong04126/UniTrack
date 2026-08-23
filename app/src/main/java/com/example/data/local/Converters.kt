package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.CourseCategory
import com.example.data.model.CourseRequirementType
import com.example.data.model.ExpenseCategory
import com.example.data.model.ExpenseType
import com.example.data.model.GeneralEduSubtype
import com.example.data.model.GpaScale
import com.example.data.model.PaymentMethod

@Suppress("unused")
class Converters {
    @TypeConverter
    fun fromCourseCategory(value: CourseCategory?): String? = value?.name

    @TypeConverter
    fun toCourseCategory(value: String?): CourseCategory? =
        value?.let { enumValueOfOrDefault(it, CourseCategory.REQUIRED) }

    @TypeConverter
    fun fromCourseRequirementType(value: CourseRequirementType?): String? = value?.name

    @TypeConverter
    fun toCourseRequirementType(value: String?): CourseRequirementType? =
        value?.let { enumValueOfOrDefault(it, CourseRequirementType.REQUIRED) }

    @TypeConverter
    fun fromGeneralEduSubtype(value: GeneralEduSubtype?): String? = value?.name

    @TypeConverter
    fun toGeneralEduSubtype(value: String?): GeneralEduSubtype? =
        value?.let {
            when (it) {
                "HUMANITIES" -> GeneralEduSubtype.CORE_HUMANITIES
                "SOCIAL_SCIENCE" -> GeneralEduSubtype.CORE_SOCIAL
                "NATURAL_SCIENCE" -> GeneralEduSubtype.CORE_NATURAL
                "CORE" -> GeneralEduSubtype.CHINESE
                "INTERDISCIPLINARY" -> GeneralEduSubtype.CORE_HUMANITIES
                else -> enumValueOfOrDefault(it, GeneralEduSubtype.NONE)
            }
        }

    @TypeConverter
    fun fromExpenseType(value: ExpenseType?): String? = value?.name

    @TypeConverter
    fun toExpenseType(value: String?): ExpenseType? =
        value?.let { enumValueOfOrDefault(it, ExpenseType.EXPENSE) }

    @TypeConverter
    fun fromExpenseCategory(value: ExpenseCategory?): String? = value?.name

    @TypeConverter
    fun toExpenseCategory(value: String?): ExpenseCategory? =
        value?.let { enumValueOfOrDefault(it, ExpenseCategory.FOOD) }

    @TypeConverter
    fun fromPaymentMethod(value: PaymentMethod?): String? = value?.name

    @TypeConverter
    fun toPaymentMethod(value: String?): PaymentMethod? =
        value?.let { enumValueOfOrDefault(it, PaymentMethod.CASH) }

    @TypeConverter
    fun fromGpaScale(value: GpaScale?): String? = value?.name

    @TypeConverter
    fun toGpaScale(value: String?): GpaScale? =
        value?.let { enumValueOfOrDefault(it, GpaScale.SCALE_4_3) }

    private inline fun <reified T : Enum<T>> enumValueOfOrDefault(name: String, defaultValue: T): T =
        runCatching { enumValueOf<T>(name) }.getOrDefault(defaultValue)
}
