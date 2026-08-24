package com.example.ui.screens.graduation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CourseCategory
import com.example.ui.components.CategoryCreditProgressBar
import com.example.ui.theme.*
import com.example.ui.viewmodel.CreditCategorySummary
import com.example.ui.viewmodel.StudentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraduationScreen(
    viewModel: StudentViewModel,
    onNavigateToThresholds: () -> Unit,
    onNavigateToCourseAudit: () -> Unit,
    onNavigateToPlanSetting: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null
) {
    val auditSummary by viewModel.graduationAudit.collectAsStateWithLifecycle()
    val allCourses by viewModel.allCourses.collectAsStateWithLifecycle()

    val animatedOverallProgress by animateFloatAsState(
        targetValue = (auditSummary.overallPercentage / 100f).coerceIn(0f, 1f),
        animationSpec = tween(700),
        label = "overall_progress"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (onNavigateBack != null) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "畢業學分審查",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "畢業審查檢核表",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                FilledTonalButton(
                    onClick = onNavigateToPlanSetting,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("edit_plan_button")
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("設定門檻")
                }
            }
        }

        // Main Graduation Gauge Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(SapphirePrimary.copy(alpha = 0.07f), TealSecondary.copy(alpha = 0.04f))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "總畢業學分達成率",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "已修通過 ${auditSummary.totalEarnedCredits.toInt()} / 目標 ${auditSummary.totalTargetCredits.toInt()} 學分",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            // Completion Badge
                            Badge(
                                containerColor = if (auditSummary.isEligibleToGraduate) EmeraldLight else SapphireLight,
                                contentColor = if (auditSummary.isEligibleToGraduate) EmeraldAccent else SapphireDark
                            ) {
                                Text(
                                    text = if (auditSummary.isEligibleToGraduate) "已達畢業標準" else "學分累積中",
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // Big Progress Bar & Percentage
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                LinearProgressIndicator(
                                    progress = { animatedOverallProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(12.dp)
                                        .clip(RoundedCornerShape(6.dp)),
                                    color = if (auditSummary.overallPercentage >= 100f) EmeraldAccent else SapphirePrimary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "尚缺 ${(auditSummary.totalTargetCredits - auditSummary.totalEarnedCredits).coerceAtLeast(0.0).toInt()} 學分",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (auditSummary.totalInProgressCredits > 0) {
                                        Text(
                                            text = "本學期修習中: +${auditSummary.totalInProgressCredits.toInt()} 學分",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TealSecondary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${auditSummary.overallPercentage.toInt()}%",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = SapphirePrimary
                            )
                        }
                    }
                }
            }
        }

        // Category Breakdown Card
        item {
            var showSubcategories by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "學分分項進度",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        TextButton(
                            onClick = { showSubcategories = !showSubcategories },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = if (showSubcategories) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (showSubcategories) "收合修別" else "展開修別",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // 1. General Education
                    ModuleProgressItem(
                        categoryName = "通識教育課程",
                        summary = auditSummary.generalSummary,
                        accentColor = CourseCategory.GENERAL_EDU.badgeColor,
                        showSubcategories = showSubcategories
                    )

                    // 2. College Core
                    ModuleProgressItem(
                        categoryName = "院共同課程",
                        summary = auditSummary.collegeCoreSummary,
                        accentColor = CourseCategory.COLLEGE_CORE.badgeColor,
                        showSubcategories = showSubcategories
                    )

                    // 3. Basic Module
                    ModuleProgressItem(
                        categoryName = "基礎模組",
                        summary = auditSummary.basicModuleSummary,
                        accentColor = CourseCategory.BASIC_MODULE.badgeColor,
                        showSubcategories = showSubcategories
                    )

                    // 4. Core Module
                    ModuleProgressItem(
                        categoryName = "核心模組",
                        summary = auditSummary.coreModuleSummary,
                        accentColor = CourseCategory.CORE_MODULE.badgeColor,
                        showSubcategories = showSubcategories
                    )

                    // 5. Professional Module
                    ModuleProgressItem(
                        categoryName = "專業模組",
                        summary = auditSummary.professionalModuleSummary,
                        accentColor = CourseCategory.PROFESSIONAL_MODULE.badgeColor,
                        showSubcategories = showSubcategories
                    )

                    // 6. Free Elective
                    ModuleProgressItem(
                        categoryName = "自由選修",
                        summary = auditSummary.freeSummary,
                        accentColor = CourseCategory.FREE_ELECTIVE.badgeColor,
                        showSubcategories = showSubcategories
                    )
                }
            }
        }

        // 1. 畢業門檻與專題檢核 按鈕卡片
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToThresholds() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SapphireLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.FactCheck,
                                contentDescription = null,
                                tint = SapphirePrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "額外條件筆記",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "已完成 ${auditSummary.thresholdsCompletedCount} / ${auditSummary.thresholdsTotalCount} 項",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // 2. 歷年修課審查清單 按鈕卡片
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToCourseAudit() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(TealLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ListAlt,
                                contentDescription = null,
                                tint = TealSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "歷年修課審查清單",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "共 ${allCourses.size} 門課程記錄",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ModuleProgressItem(
    categoryName: String,
    summary: CreditCategorySummary,
    accentColor: Color,
    showSubcategories: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        CategoryCreditProgressBar(
            categoryName = categoryName,
            earnedCredits = summary.earnedCredits,
            targetCredits = summary.targetCredits,
            inProgressCredits = summary.inProgressCredits,
            accentColor = accentColor
        )

        AnimatedVisibility(
            visible = showSubcategories && (summary.requiredSummary != null || summary.electiveSummary != null),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, top = 2.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                summary.requiredSummary?.let { req ->
                    if (req.targetCredits > 0.0 || req.earnedCredits > 0.0) {
                        SubcategoryProgressBar(
                            label = "[ 必修 ]",
                            earnedCredits = req.earnedCredits,
                            targetCredits = req.targetCredits,
                            inProgressCredits = req.inProgressCredits,
                            accentColor = accentColor
                        )
                    }
                }
                summary.electiveSummary?.let { ele ->
                    if (ele.targetCredits > 0.0 || ele.earnedCredits > 0.0) {
                        SubcategoryProgressBar(
                            label = "[ 選修 ]",
                            earnedCredits = ele.earnedCredits,
                            targetCredits = ele.targetCredits,
                            inProgressCredits = ele.inProgressCredits,
                            accentColor = accentColor.copy(alpha = 0.75f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubcategoryProgressBar(
    label: String,
    earnedCredits: Double,
    targetCredits: Double,
    inProgressCredits: Double = 0.0,
    accentColor: Color
) {
    val percentage = if (targetCredits > 0.0) ((earnedCredits / targetCredits) * 100.0).coerceIn(0.0, 100.0).toFloat() else if (earnedCredits > 0.0) 100f else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = (percentage / 100f).coerceIn(0f, 1f),
        animationSpec = tween(500),
        label = "sub_progress"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp)
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
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (inProgressCredits > 0) {
                    Text(
                        text = "(+${inProgressCredits.toInt()})",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = TealSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Text(
                text = "${earnedCredits.toInt()} / ${targetCredits.toInt()} 學分 (${percentage.toInt()}%)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (percentage >= 100f) EmeraldAccent else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = accentColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    }
}
