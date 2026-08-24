package com.example.ui.screens.graduation

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CourseCategory
import com.example.ui.components.SectionHeader
import com.example.ui.theme.SapphireLight
import com.example.ui.theme.SapphirePrimary
import com.example.ui.viewmodel.StudentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraduationPlanScreen(
    viewModel: StudentViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentPlan by viewModel.graduationPlan.collectAsStateWithLifecycle()

    var totalTarget by remember(currentPlan) { mutableStateOf(currentPlan.targetTotalCredits.toString()) }

    // 通識教育
    var genReqTarget by remember(currentPlan) { mutableStateOf(currentPlan.targetGeneralRequiredCredits.toString()) }
    var genEleTarget by remember(currentPlan) { mutableStateOf(currentPlan.targetGeneralElectiveCredits.toString()) }

    // 院共同
    var colReqTarget by remember(currentPlan) { mutableStateOf(currentPlan.targetCollegeCoreRequiredCredits.toString()) }
    var colEleTarget by remember(currentPlan) { mutableStateOf(currentPlan.targetCollegeCoreElectiveCredits.toString()) }

    // 基礎模組
    var basReqTarget by remember(currentPlan) { mutableStateOf(currentPlan.targetBasicModuleRequiredCredits.toString()) }
    var basEleTarget by remember(currentPlan) { mutableStateOf(currentPlan.targetBasicModuleElectiveCredits.toString()) }

    // 核心模組
    var corReqTarget by remember(currentPlan) { mutableStateOf(currentPlan.targetCoreModuleRequiredCredits.toString()) }
    var corEleTarget by remember(currentPlan) { mutableStateOf(currentPlan.targetCoreModuleElectiveCredits.toString()) }

    // 專業模組
    var proReqTarget by remember(currentPlan) { mutableStateOf(currentPlan.targetProfessionalModuleRequiredCredits.toString()) }
    var proEleTarget by remember(currentPlan) { mutableStateOf(currentPlan.targetProfessionalModuleElectiveCredits.toString()) }

    // 自由選修
    var freeEleTarget by remember(currentPlan) { mutableStateOf(currentPlan.targetFreeElectiveCredits.toString()) }

    fun savePlan() {
        val genReq = genReqTarget.toDoubleOrNull() ?: 0.0
        val genEle = genEleTarget.toDoubleOrNull() ?: 0.0
        val colReq = colReqTarget.toDoubleOrNull() ?: 0.0
        val colEle = 0.0
        val basReq = basReqTarget.toDoubleOrNull() ?: 0.0
        val basEle = basEleTarget.toDoubleOrNull() ?: 0.0
        val corReq = corReqTarget.toDoubleOrNull() ?: 0.0
        val corEle = corEleTarget.toDoubleOrNull() ?: 0.0
        val proReq = proReqTarget.toDoubleOrNull() ?: 0.0
        val proEle = proEleTarget.toDoubleOrNull() ?: 0.0
        val freeEle = freeEleTarget.toDoubleOrNull() ?: 0.0

        val updated = currentPlan.copy(
            targetTotalCredits = totalTarget.toDoubleOrNull() ?: 128.0,
            targetRequiredCredits = currentPlan.targetRequiredCredits,
            targetElectiveCredits = currentPlan.targetElectiveCredits,
            targetGeneralCredits = genReq + genEle,
            targetCollegeCoreCredits = colReq,
            targetBasicModuleCredits = basReq + basEle,
            targetCoreModuleCredits = corReq + corEle,
            targetProfessionalModuleCredits = proReq + proEle,
            targetFreeCredits = freeEle,
            targetGeneralRequiredCredits = genReq,
            targetGeneralElectiveCredits = genEle,
            targetCollegeCoreRequiredCredits = colReq,
            targetCollegeCoreElectiveCredits = colEle,
            targetBasicModuleRequiredCredits = basReq,
            targetBasicModuleElectiveCredits = basEle,
            targetCoreModuleRequiredCredits = corReq,
            targetCoreModuleElectiveCredits = corEle,
            targetProfessionalModuleRequiredCredits = proReq,
            targetProfessionalModuleElectiveCredits = proEle,
            targetFreeElectiveCredits = freeEle,
            gpaScale = currentPlan.gpaScale
        )
        viewModel.updateGraduationPlan(updated)
        Toast.makeText(context, "畢業審查標準已儲存更新", Toast.LENGTH_SHORT).show()
        onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "設定畢業審查標準",
                        fontWeight = FontWeight.Bold
                    )
                },
                windowInsets = WindowInsets(0.dp),
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = { savePlan() },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("save_plan_top_button"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("儲存")
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0.dp),
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Description Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
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
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SapphireLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = SapphirePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "請依據您所屬系所入學年度之修業規章，設定畢業總學分與各模組【必修】、【選修】之門檻要求。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Section 1: 總畢業學分
            SectionHeader(title = "總畢業學分目標")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = totalTarget,
                        onValueChange = { totalTarget = it },
                        label = { Text("總畢業學分目標 (必填) *") },
                        placeholder = { Text("例：128.0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.School, contentDescription = null, tint = SapphirePrimary)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("total_credits_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Section 2: 各模組與類別學分門檻 (必修 / 選修 子分類)
            SectionHeader(title = "各模組與類別學分門檻")

            // 1. 通識教育課程
            ModuleThresholdCard(
                title = "通識教育課程",
                badgeColor = CourseCategory.GENERAL_EDU.badgeColor,
                reqValue = genReqTarget,
                onReqChange = { genReqTarget = it },
                eleValue = genEleTarget,
                onEleChange = { genEleTarget = it }
            )

            // 2. 院共同課程 (只有必修)
            ModuleThresholdCard(
                title = "院共同課程",
                badgeColor = CourseCategory.COLLEGE_CORE.badgeColor,
                reqValue = colReqTarget,
                onReqChange = { colReqTarget = it },
                eleValue = "",
                onEleChange = {},
                showRequiredOnly = true
            )

            // 3. 基礎模組
            ModuleThresholdCard(
                title = "基礎模組",
                badgeColor = CourseCategory.BASIC_MODULE.badgeColor,
                reqValue = basReqTarget,
                onReqChange = { basReqTarget = it },
                eleValue = basEleTarget,
                onEleChange = { basEleTarget = it }
            )

            // 4. 核心模組
            ModuleThresholdCard(
                title = "核心模組",
                badgeColor = CourseCategory.CORE_MODULE.badgeColor,
                reqValue = corReqTarget,
                onReqChange = { corReqTarget = it },
                eleValue = corEleTarget,
                onEleChange = { corEleTarget = it }
            )

            // 5. 專業模組
            ModuleThresholdCard(
                title = "專業模組",
                badgeColor = CourseCategory.PROFESSIONAL_MODULE.badgeColor,
                reqValue = proReqTarget,
                onReqChange = { proReqTarget = it },
                eleValue = proEleTarget,
                onEleChange = { proEleTarget = it }
            )

            // 6. 自由選修 (只有選修)
            ModuleThresholdCard(
                title = "自由選修",
                badgeColor = CourseCategory.FREE_ELECTIVE.badgeColor,
                reqValue = "",
                onReqChange = {},
                eleValue = freeEleTarget,
                onEleChange = { freeEleTarget = it },
                showElectiveOnly = true
            )

            // Bottom Save Button
            Button(
                onClick = { savePlan() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_plan_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "儲存標準設定",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ModuleThresholdCard(
    title: String,
    badgeColor: Color,
    reqValue: String,
    onReqChange: (String) -> Unit,
    eleValue: String,
    onEleChange: (String) -> Unit,
    showRequiredOnly: Boolean = false,
    showElectiveOnly: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(badgeColor)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            if (showRequiredOnly) {
                OutlinedTextField(
                    value = reqValue,
                    onValueChange = onReqChange,
                    label = { Text("[ 必修 ] 目標學分") },
                    placeholder = { Text("例：9.0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            } else if (showElectiveOnly) {
                OutlinedTextField(
                    value = eleValue,
                    onValueChange = onEleChange,
                    label = { Text("[ 選修 ] 目標學分") },
                    placeholder = { Text("例：20.0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = reqValue,
                        onValueChange = onReqChange,
                        label = { Text("[ 必修 ] 目標學分") },
                        placeholder = { Text("0.0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = eleValue,
                        onValueChange = onEleChange,
                        label = { Text("[ 選修 ] 目標學分") },
                        placeholder = { Text("0.0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
    }
}
