package com.example.ui.screens.graduation

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    var genTarget by remember(currentPlan) { mutableStateOf(currentPlan.targetGeneralCredits.toString()) }
    var colTarget by remember(currentPlan) { mutableStateOf(currentPlan.targetCollegeCoreCredits.toString()) }
    var basTarget by remember(currentPlan) { mutableStateOf(currentPlan.targetBasicModuleCredits.toString()) }
    var corTarget by remember(currentPlan) { mutableStateOf(currentPlan.targetCoreModuleCredits.toString()) }
    var proTarget by remember(currentPlan) { mutableStateOf(currentPlan.targetProfessionalModuleCredits.toString()) }
    var freeTarget by remember(currentPlan) { mutableStateOf(currentPlan.targetFreeCredits.toString()) }

    fun savePlan() {
        val updated = currentPlan.copy(
            targetTotalCredits = totalTarget.toDoubleOrNull() ?: 128.0,
            targetRequiredCredits = currentPlan.targetRequiredCredits,
            targetElectiveCredits = currentPlan.targetElectiveCredits,
            targetGeneralCredits = genTarget.toDoubleOrNull() ?: 28.0,
            targetCollegeCoreCredits = colTarget.toDoubleOrNull() ?: 9.0,
            targetBasicModuleCredits = basTarget.toDoubleOrNull() ?: 24.0,
            targetCoreModuleCredits = corTarget.toDoubleOrNull() ?: 24.0,
            targetProfessionalModuleCredits = proTarget.toDoubleOrNull() ?: 23.0,
            targetFreeCredits = freeTarget.toDoubleOrNull() ?: 20.0,
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
                        text = "請依據您所屬系所入學年度之修業規章，設定畢業總學分與各模組門檻要求。",
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

            // Section 2: 各模組與類別學分門檻
            SectionHeader(title = "各模組與類別學分門檻")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Row 1: 通識學分 & 院共同學分
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = genTarget,
                            onValueChange = { genTarget = it },
                            label = { Text("通識學分") },
                            placeholder = { Text("例：28.0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = colTarget,
                            onValueChange = { colTarget = it },
                            label = { Text("院共同學分") },
                            placeholder = { Text("例：9.0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Row 2: 基礎模組 & 核心模組
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = basTarget,
                            onValueChange = { basTarget = it },
                            label = { Text("基礎模組") },
                            placeholder = { Text("例：24.0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = corTarget,
                            onValueChange = { corTarget = it },
                            label = { Text("核心模組") },
                            placeholder = { Text("例：24.0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Row 3: 專業模組 & 自由選修
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = proTarget,
                            onValueChange = { proTarget = it },
                            label = { Text("專業模組") },
                            placeholder = { Text("例：23.0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = freeTarget,
                            onValueChange = { freeTarget = it },
                            label = { Text("自由選修") },
                            placeholder = { Text("例：20.0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

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
