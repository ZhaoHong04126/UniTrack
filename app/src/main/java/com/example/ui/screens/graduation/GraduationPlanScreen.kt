package com.example.ui.screens.graduation

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.core.graphics.toColorInt
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
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
import com.example.data.model.CustomParentCategory
import com.example.data.model.GraduationPlan
import com.example.data.model.SubcategoryRule
import com.example.ui.components.SectionHeader
import com.example.ui.theme.SapphireLight
import com.example.ui.theme.SapphirePrimary
import com.example.ui.viewmodel.StudentViewModel

data class SubcategoryRuleUIState(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val reqText: String = "0.0",
    val eleText: String = "0.0"
)

data class CustomCategoryUIState(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val colorHex: String = "#8B5CF6",
    val reqText: String = "0.0",
    val eleText: String = "0.0",
    val subcategories: List<SubcategoryRuleUIState> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraduationPlanScreen(
    viewModel: StudentViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentPlan by viewModel.graduationPlan.collectAsStateWithLifecycle()

    var isInitialized by remember { mutableStateOf(false) }

    var totalTarget by remember { mutableStateOf("") }
    var genReqTarget by remember { mutableStateOf("") }
    var genEleTarget by remember { mutableStateOf("") }
    var colReqTarget by remember { mutableStateOf("") }
    var colEleTarget by remember { mutableStateOf("") }
    var basReqTarget by remember { mutableStateOf("") }
    var basEleTarget by remember { mutableStateOf("") }
    var corReqTarget by remember { mutableStateOf("") }
    var corEleTarget by remember { mutableStateOf("") }
    var proReqTarget by remember { mutableStateOf("") }
    var proEleTarget by remember { mutableStateOf("") }
    var freeEleTarget by remember { mutableStateOf("") }

    var subcategoriesMap by remember {
        mutableStateOf<Map<CourseCategory, List<SubcategoryRuleUIState>>>(emptyMap())
    }

    var customCategories by remember {
        mutableStateOf<List<CustomCategoryUIState>>(emptyList())
    }

    LaunchedEffect(currentPlan) {
        if (!isInitialized) {
            totalTarget = currentPlan.targetTotalCredits.toString()
            genReqTarget = currentPlan.targetGeneralRequiredCredits.toString()
            genEleTarget = currentPlan.targetGeneralElectiveCredits.toString()
            colReqTarget = currentPlan.targetCollegeCoreRequiredCredits.toString()
            colEleTarget = currentPlan.targetCollegeCoreElectiveCredits.toString()
            basReqTarget = currentPlan.targetBasicModuleRequiredCredits.toString()
            basEleTarget = currentPlan.targetBasicModuleElectiveCredits.toString()
            corReqTarget = currentPlan.targetCoreModuleRequiredCredits.toString()
            corEleTarget = currentPlan.targetCoreModuleElectiveCredits.toString()
            proReqTarget = currentPlan.targetProfessionalModuleRequiredCredits.toString()
            proEleTarget = currentPlan.targetProfessionalModuleElectiveCredits.toString()
            freeEleTarget = currentPlan.targetFreeElectiveCredits.toString()
            subcategoriesMap = CourseCategory.entries.associateWith { cat ->
                currentPlan.getSubcategoryRules(cat).map {
                    SubcategoryRuleUIState(
                        id = it.id,
                        name = it.name,
                        reqText = it.requiredCredits.toString(),
                        eleText = it.electiveCredits.toString()
                    )
                }
            }
            customCategories = currentPlan.getCustomCategories().map { customCat ->
                CustomCategoryUIState(
                    id = customCat.id,
                    name = customCat.name,
                    colorHex = customCat.colorHex,
                    reqText = customCat.requiredCredits.toString(),
                    eleText = customCat.electiveCredits.toString(),
                    subcategories = customCat.subcategories.map {
                        SubcategoryRuleUIState(
                            id = it.id,
                            name = it.name,
                            reqText = it.requiredCredits.toString(),
                            eleText = it.electiveCredits.toString()
                        )
                    }
                )
            }
            isInitialized = true
        }
    }

    var showAddCustomCategoryDialog by remember { mutableStateOf(false) }
    var newCustomCatName by remember { mutableStateOf("") }
    var newCustomCatColor by remember { mutableStateOf("#8B5CF6") }
    var newCustomCatReq by remember { mutableStateOf("0.0") }
    var newCustomCatEle by remember { mutableStateOf("0.0") }

    fun addOrUpdateSubcategory(category: CourseCategory, rule: SubcategoryRuleUIState) {
        val currentList = subcategoriesMap[category]?.toMutableList() ?: mutableListOf()
        val idx = currentList.indexOfFirst { it.id == rule.id || it.name == rule.name }
        if (idx >= 0) {
            currentList[idx] = rule
        } else {
            currentList.add(rule)
        }
        val newMap = subcategoriesMap.toMutableMap()
        newMap[category] = currentList
        subcategoriesMap = newMap
    }

    fun removeSubcategory(category: CourseCategory, ruleId: String) {
        val currentList = subcategoriesMap[category] ?: emptyList()
        val newMap = subcategoriesMap.toMutableMap()
        newMap[category] = currentList.filter { it.id != ruleId }
        subcategoriesMap = newMap
    }

    fun addOrUpdateCustomCategorySubcategory(catId: String, rule: SubcategoryRuleUIState) {
        customCategories = customCategories.map { cat ->
            if (cat.id == catId) {
                val list = cat.subcategories.toMutableList()
                val idx = list.indexOfFirst { it.id == rule.id || it.name == rule.name }
                if (idx >= 0) {
                    list[idx] = rule
                } else {
                    list.add(rule)
                }
                cat.copy(subcategories = list)
            } else cat
        }
    }

    fun removeCustomCategorySubcategory(catId: String, ruleId: String) {
        customCategories = customCategories.map { cat ->
            if (cat.id == catId) {
                cat.copy(subcategories = cat.subcategories.filter { it.id != ruleId })
            } else cat
        }
    }

    fun updateCustomCategoryReq(catId: String, reqStr: String) {
        customCategories = customCategories.map { cat ->
            if (cat.id == catId) {
                cat.copy(reqText = reqStr)
            } else cat
        }
    }

    fun updateCustomCategoryEle(catId: String, eleStr: String) {
        customCategories = customCategories.map { cat ->
            if (cat.id == catId) {
                cat.copy(eleText = eleStr)
            } else cat
        }
    }

    fun deleteCustomCategory(catId: String) {
        customCategories = customCategories.filter { it.id != catId }
    }

    fun savePlan() {
        val genReq = genReqTarget.toDoubleOrNull() ?: 0.0
        val genEle = genEleTarget.toDoubleOrNull() ?: 0.0
        val colReq = colReqTarget.toDoubleOrNull() ?: 0.0
        val colEle = colEleTarget.toDoubleOrNull() ?: 0.0
        val basReq = basReqTarget.toDoubleOrNull() ?: 0.0
        val basEle = basEleTarget.toDoubleOrNull() ?: 0.0
        val corReq = corReqTarget.toDoubleOrNull() ?: 0.0
        val corEle = corEleTarget.toDoubleOrNull() ?: 0.0
        val proReq = proReqTarget.toDoubleOrNull() ?: 0.0
        val proEle = proEleTarget.toDoubleOrNull() ?: 0.0
        val freeEle = freeEleTarget.toDoubleOrNull() ?: 0.0

        val subcategoryRulesDomain = subcategoriesMap.mapValues { (_, list) ->
            list.map {
                SubcategoryRule(
                    id = it.id,
                    name = it.name.trim(),
                    requiredCredits = it.reqText.toDoubleOrNull() ?: 0.0,
                    electiveCredits = it.eleText.toDoubleOrNull() ?: 0.0
                )
            }
        }
        val encodedSubcategories = GraduationPlan.encodeSubcategoryRules(subcategoryRulesDomain)

        val customCatsDomain = customCategories.map {
            CustomParentCategory(
                id = it.id,
                name = it.name.trim(),
                colorHex = it.colorHex,
                requiredCredits = it.reqText.toDoubleOrNull() ?: 0.0,
                electiveCredits = it.eleText.toDoubleOrNull() ?: 0.0,
                subcategories = it.subcategories.map { sub ->
                    SubcategoryRule(
                        id = sub.id,
                        name = sub.name.trim(),
                        requiredCredits = sub.reqText.toDoubleOrNull() ?: 0.0,
                        electiveCredits = sub.eleText.toDoubleOrNull() ?: 0.0
                    )
                }
            )
        }
        val encodedCustomCategories = GraduationPlan.encodeCustomCategories(customCatsDomain)

        val updated = currentPlan.copy(
            targetTotalCredits = totalTarget.toDoubleOrNull() ?: 128.0,
            targetRequiredCredits = currentPlan.targetRequiredCredits,
            targetElectiveCredits = currentPlan.targetElectiveCredits,
            targetGeneralCredits = genReq + genEle,
            targetCollegeCoreCredits = colReq + colEle,
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
            gpaScale = currentPlan.gpaScale,
            subcategoriesJson = encodedSubcategories,
            customCategoriesJson = encodedCustomCategories
        )
        viewModel.updateGraduationPlan(updated) {
            Toast.makeText(context, "畢業審查標準已儲存更新", Toast.LENGTH_SHORT).show()
            onNavigateBack()
        }
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
                        text = "請依據您所屬系所入學年度之修業規章，設定畢業總學分與各模組【必修】、【選修】之門檻要求，子分類亦可自訂個別學分目標。",
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
                onEleChange = { genEleTarget = it },
                subcategories = subcategoriesMap[CourseCategory.GENERAL_EDU] ?: emptyList(),
                onSaveSubcategory = { addOrUpdateSubcategory(CourseCategory.GENERAL_EDU, it) },
                onRemoveSubcategory = { removeSubcategory(CourseCategory.GENERAL_EDU, it.id) }
            )

            // 2. 院共同課程
            ModuleThresholdCard(
                title = "院共同課程",
                badgeColor = CourseCategory.COLLEGE_CORE.badgeColor,
                reqValue = colReqTarget,
                onReqChange = { colReqTarget = it },
                eleValue = colEleTarget,
                onEleChange = { colEleTarget = it },
                subcategories = subcategoriesMap[CourseCategory.COLLEGE_CORE] ?: emptyList(),
                onSaveSubcategory = { addOrUpdateSubcategory(CourseCategory.COLLEGE_CORE, it) },
                onRemoveSubcategory = { removeSubcategory(CourseCategory.COLLEGE_CORE, it.id) }
            )

            // 3. 基礎模組
            ModuleThresholdCard(
                title = "基礎模組",
                badgeColor = CourseCategory.BASIC_MODULE.badgeColor,
                reqValue = basReqTarget,
                onReqChange = { basReqTarget = it },
                eleValue = basEleTarget,
                onEleChange = { basEleTarget = it },
                subcategories = subcategoriesMap[CourseCategory.BASIC_MODULE] ?: emptyList(),
                onSaveSubcategory = { addOrUpdateSubcategory(CourseCategory.BASIC_MODULE, it) },
                onRemoveSubcategory = { removeSubcategory(CourseCategory.BASIC_MODULE, it.id) }
            )

            // 4. 核心模組
            ModuleThresholdCard(
                title = "核心模組",
                badgeColor = CourseCategory.CORE_MODULE.badgeColor,
                reqValue = corReqTarget,
                onReqChange = { corReqTarget = it },
                eleValue = corEleTarget,
                onEleChange = { corEleTarget = it },
                subcategories = subcategoriesMap[CourseCategory.CORE_MODULE] ?: emptyList(),
                onSaveSubcategory = { addOrUpdateSubcategory(CourseCategory.CORE_MODULE, it) },
                onRemoveSubcategory = { removeSubcategory(CourseCategory.CORE_MODULE, it.id) }
            )

            // 5. 專業模組
            ModuleThresholdCard(
                title = "專業模組",
                badgeColor = CourseCategory.PROFESSIONAL_MODULE.badgeColor,
                reqValue = proReqTarget,
                onReqChange = { proReqTarget = it },
                eleValue = proEleTarget,
                onEleChange = { proEleTarget = it },
                subcategories = subcategoriesMap[CourseCategory.PROFESSIONAL_MODULE] ?: emptyList(),
                onSaveSubcategory = { addOrUpdateSubcategory(CourseCategory.PROFESSIONAL_MODULE, it) },
                onRemoveSubcategory = { removeSubcategory(CourseCategory.PROFESSIONAL_MODULE, it.id) }
            )

            // 6. 自由選修 (只有選修)
            ModuleThresholdCard(
                title = "自由選修",
                badgeColor = CourseCategory.FREE_ELECTIVE.badgeColor,
                reqValue = "",
                onReqChange = {},
                eleValue = freeEleTarget,
                onEleChange = { freeEleTarget = it },
                subcategories = subcategoriesMap[CourseCategory.FREE_ELECTIVE] ?: emptyList(),
                onSaveSubcategory = { addOrUpdateSubcategory(CourseCategory.FREE_ELECTIVE, it) },
                onRemoveSubcategory = { removeSubcategory(CourseCategory.FREE_ELECTIVE, it.id) },
                showElectiveOnly = true
            )

            // Custom Parent Categories (自訂母體分類)
            if (customCategories.isNotEmpty()) {
                SectionHeader(title = "自訂母體分類 (${customCategories.size})")
                customCategories.forEach { customCat ->
                    val catColor = runCatching {
                        Color(customCat.colorHex.toColorInt())
                    }.getOrDefault(Color(0xFF8B5CF6))

                    ModuleThresholdCard(
                        title = customCat.name,
                        badgeColor = catColor,
                        reqValue = customCat.reqText,
                        onReqChange = { str -> updateCustomCategoryReq(customCat.id, str) },
                        eleValue = customCat.eleText,
                        onEleChange = { str -> updateCustomCategoryEle(customCat.id, str) },
                        subcategories = customCat.subcategories,
                        onSaveSubcategory = { addOrUpdateCustomCategorySubcategory(customCat.id, it) },
                        onRemoveSubcategory = { removeCustomCategorySubcategory(customCat.id, it.id) },
                        onDelete = { deleteCustomCategory(customCat.id) }
                    )
                }
            }

            // Button to Add Custom Parent Category
            OutlinedButton(
                onClick = { showAddCustomCategoryDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("新增自訂母體分類 (例如：微學程、跨領域學程)", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(8.dp))

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

    if (showAddCustomCategoryDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddCustomCategoryDialog = false
                newCustomCatName = ""
            },
            title = { Text("新增自訂母體分類") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newCustomCatName,
                        onValueChange = { newCustomCatName = it },
                        label = { Text("母體分類名稱 *") },
                        placeholder = { Text("例：跨領域學程、第二專長、微學程") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newCustomCatReq,
                            onValueChange = { newCustomCatReq = it },
                            label = { Text("[ 必修 ] 目標") },
                            placeholder = { Text("0.0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newCustomCatEle,
                            onValueChange = { newCustomCatEle = it },
                            label = { Text("[ 選修 ] 目標") },
                            placeholder = { Text("0.0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCustomCatName.isNotBlank()) {
                            val newCat = CustomCategoryUIState(
                                name = newCustomCatName.trim(),
                                colorHex = newCustomCatColor,
                                reqText = newCustomCatReq.trim().ifBlank { "0.0" },
                                eleText = newCustomCatEle.trim().ifBlank { "0.0" }
                            )
                            customCategories = customCategories + newCat
                            showAddCustomCategoryDialog = false
                            newCustomCatName = ""
                            newCustomCatReq = "0.0"
                            newCustomCatEle = "0.0"
                        }
                    },
                    enabled = newCustomCatName.isNotBlank()
                ) {
                    Text("建立母體")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddCustomCategoryDialog = false
                    newCustomCatName = ""
                }) {
                    Text("取消")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModuleThresholdCard(
    title: String,
    badgeColor: Color,
    reqValue: String,
    onReqChange: (String) -> Unit,
    eleValue: String,
    onEleChange: (String) -> Unit,
    subcategories: List<SubcategoryRuleUIState> = emptyList(),
    onSaveSubcategory: (SubcategoryRuleUIState) -> Unit = {},
    onRemoveSubcategory: (SubcategoryRuleUIState) -> Unit = {},
    onDelete: (() -> Unit)? = null,
    showRequiredOnly: Boolean = false,
    showElectiveOnly: Boolean = false
) {
    var showAddOrEditDialog by remember { mutableStateOf(false) }
    var editingRuleId by remember { mutableStateOf<String?>(null) }
    var newSubcategoryName by remember { mutableStateOf("") }
    var newSubcategoryReq by remember { mutableStateOf("0.0") }
    var newSubcategoryEle by remember { mutableStateOf("0.0") }

    if (showAddOrEditDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddOrEditDialog = false
                editingRuleId = null
                newSubcategoryName = ""
                newSubcategoryReq = "0.0"
                newSubcategoryEle = "0.0"
            },
            title = {
                Text(if (editingRuleId == null) "新增「$title」子分類與學分" else "編輯「$newSubcategoryName」學分設定")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newSubcategoryName,
                        onValueChange = { newSubcategoryName = it },
                        label = { Text("子分類 / 領域名稱 *") },
                        placeholder = { Text("例：國語文能力、向度一") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newSubcategoryReq,
                            onValueChange = { newSubcategoryReq = it },
                            label = { Text("[ 必修 ] 目標") },
                            placeholder = { Text("0.0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newSubcategoryEle,
                            onValueChange = { newSubcategoryEle = it },
                            label = { Text("[ 選修 ] 目標") },
                            placeholder = { Text("0.0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newSubcategoryName.isNotBlank()) {
                            onSaveSubcategory(
                                SubcategoryRuleUIState(
                                    id = editingRuleId ?: java.util.UUID.randomUUID().toString(),
                                    name = newSubcategoryName.trim(),
                                    reqText = newSubcategoryReq.trim().ifBlank { "0.0" },
                                    eleText = newSubcategoryEle.trim().ifBlank { "0.0" }
                                )
                            )
                            showAddOrEditDialog = false
                            editingRuleId = null
                            newSubcategoryName = ""
                            newSubcategoryReq = "0.0"
                            newSubcategoryEle = "0.0"
                        }
                    },
                    enabled = newSubcategoryName.isNotBlank()
                ) {
                    Text(if (editingRuleId == null) "新增" else "儲存")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddOrEditDialog = false
                    editingRuleId = null
                    newSubcategoryName = ""
                    newSubcategoryReq = "0.0"
                    newSubcategoryEle = "0.0"
                }) {
                    Text("取消")
                }
            }
        )
    }

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
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
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
                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "刪除母體分類",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
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

            // Subcategories Management
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "子階級分類 / 領域 (${subcategories.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = {
                            editingRuleId = null
                            newSubcategoryName = ""
                            newSubcategoryReq = "0.0"
                            newSubcategoryEle = "0.0"
                            showAddOrEditDialog = true
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("新增子分類", style = MaterialTheme.typography.labelMedium)
                    }
                }

                if (subcategories.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        subcategories.forEach { sub ->
                            val rVal = sub.reqText.toDoubleOrNull() ?: 0.0
                            val eVal = sub.eleText.toDoubleOrNull() ?: 0.0
                            val creditBadge = when {
                                rVal > 0.0 && eVal > 0.0 -> "必${sub.reqText}/選${sub.eleText}"
                                rVal > 0.0 -> "必${sub.reqText}"
                                eVal > 0.0 -> "選${sub.eleText}"
                                else -> "0學分"
                            }

                            InputChip(
                                selected = false,
                                onClick = {
                                    editingRuleId = sub.id
                                    newSubcategoryName = sub.name
                                    newSubcategoryReq = sub.reqText
                                    newSubcategoryEle = sub.eleText
                                    showAddOrEditDialog = true
                                },
                                label = {
                                    Text(
                                        text = "${sub.name} ($creditBadge)",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "刪除",
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { onRemoveSubcategory(sub) }
                                    )
                                },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                } else {
                    Text(
                        text = "無設定子分類（排課時該母體將不顯示子分類選單）",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
