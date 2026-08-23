package com.example.ui.screens.auth

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AuthState
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudentViewModel

@Composable
fun AuthScreen(
    viewModel: StudentViewModel,
    modifier: Modifier = Modifier,
    onAuthSuccess: () -> Unit = {}
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    var showAuthSheet by remember { mutableStateOf(false) }
    var authSheetInitialTab by remember { mutableIntStateOf(1) } // 0 = 登入, 1 = 註冊

    // If authenticated, navigate forward automatically
    val user = currentUser
    LaunchedEffect(user) {
        if (user != null) {
            showAuthSheet = false
            onAuthSuccess()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)) // Deep Slate / Campus Night Theme
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Center Content: Title, Slogan & Hero Mascot Illustration
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // Brand Title with Sapphire Dot
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "UniTrack",
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = "+",
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black,
                        color = SapphirePrimary
                    )
                }

                // Slogan
                Text(
                    text = "一起打造大學智慧生活",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.92f),
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Official UniTrack+ App Logo
                AuthLogoMascot()
            }

            // Bottom Actions (Two-Level Action System)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                // Primary Action Button ("免費開始")
                Button(
                    onClick = {
                        authSheetInitialTab = 1 // 註冊
                        showAuthSheet = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(
                            elevation = 12.dp,
                            shape = RoundedCornerShape(18.dp),
                            spotColor = SapphirePrimary.copy(alpha = 0.5f)
                        ),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SapphirePrimary
                    )
                ) {
                    Text(
                        text = "免費開始",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 17.sp
                    )
                }

                // Secondary Link ("已有帳號？ 登入")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "已有帳號？",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.65f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "登入",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF60A5FA),
                        modifier = Modifier.clickable {
                            authSheetInitialTab = 0 // 登入
                            showAuthSheet = true
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Auth Bottom Sheet Modal
        if (showAuthSheet) {
            AuthBottomSheet(
                viewModel = viewModel,
                initialTab = authSheetInitialTab,
                onDismiss = { showAuthSheet = false },
                onAuthSuccess = {
                    showAuthSheet = false
                    onAuthSuccess()
                }
            )
        }
    }
}

/**
 * UniTrack+ 官方 App Logo 迎賓視覺 (純透明懸浮 / 無方框)
 */
@Composable
private fun AuthLogoMascot() {
    Box(
        modifier = Modifier.size(240.dp),
        contentAlignment = Alignment.Center
    ) {
        // Soft Ambient Radial Glow
        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            SapphirePrimary.copy(alpha = 0.28f),
                            OceanBlue.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Official App Logo
        Image(
            painter = painterResource(id = com.example.R.drawable.ic_launcher_foreground),
            contentDescription = "UniTrack+ Logo",
            modifier = Modifier
                .size(230.dp)
                .scale(1.35f)
        )
    }
}

/**
 * Modern Onboarding & Auth Modal Bottom Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthBottomSheet(
    viewModel: StudentViewModel,
    initialTab: Int,
    onDismiss: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val graduationPlan by viewModel.graduationPlan.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    var selectedTab by remember { mutableIntStateOf(initialTab) }
    var regStep by remember { mutableIntStateOf(1) } // Step 1: 學業檔案, Step 2: 帳號安全

    // Registration Profile State (專屬固定 國立臺東大學)
    val university = "國立臺東大學"
    var department by remember { mutableStateOf("") }
    var admissionYear by remember { mutableStateOf("114 學年度 (114-1)") }

    // Dropdown expanded states
    var deptExpanded by remember { mutableStateOf(false) }
    var yearExpanded by remember { mutableStateOf(false) }

    // 國立臺東大學全校 26 個完整系所與學程
    val deptOptions = listOf(
        "體育學系",
        "競技與運動科學學系",
        "應用數學系",
        "應用科學系",
        "數位媒體與文教產業學系",
        "綠能與資訊科技學系",
        "綠色與資訊科技學士學位學程",
        "運動競技學士學位學程",
        "資訊管理學系產業管理與數位行銷進修學士班",
        "資訊管理學系",
        "資訊工程學系",
        "華語文學系",
        "教育學系",
        "高齡健康與照護管理原住民專班",
        "特殊教育學系",
        "音樂學系",
        "英美語文學系",
        "美術產業學系",
        "身心整合與運動休閒產業學系",
        "全校不分系學士學位學程",
        "生命科學系",
        "幼兒教育學系幼兒教育學士後學位學程教保員專班",
        "幼兒教育學系",
        "文化資源與休閒產業學系產業經營進修學士班",
        "文化資源與休閒產業學系",
        "公共與文化事務學系"
    )

    val yearOptions = listOf(
        "114 學年度 (114-1)",
        "113 學年度 (113-1)",
        "112 學年度 (112-1)",
        "111 學年度 (111-1)",
        "110 學年度 (110-1)"
    )

    // Form inputs
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var showResetDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Error Banner
            AnimatedVisibility(
                visible = authState is AuthState.Error,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val errorState = authState as? AuthState.Error
                if (errorState != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = RoseAccent.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, RoseAccent.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = RoseAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = errorState.message,
                                color = RoseAccent,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Tab Selector
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        viewModel.clearAuthError()
                    },
                    text = {
                        Text(
                            text = "已有帳號登入",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        viewModel.clearAuthError()
                    },
                    text = {
                        Text(
                            text = "新學生註冊",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
            }

            if (selectedTab == 0) {
                // ==================== 登入 ====================
                Text(
                    text = "歡迎回到 UniTrack+",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Google One-Tap Sign In
                OutlinedButton(
                    onClick = {
                        viewModel.signInWithGoogle { success, _ ->
                            if (success) onAuthSuccess()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Google",
                        tint = SapphirePrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "使用 Google 帳號一鍵繼續",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Divider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text(
                        text = "  或使用電子郵件  ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        if (authState is AuthState.Error) viewModel.clearAuthError()
                    },
                    label = { Text("電子郵件 (Email)") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = SapphirePrimary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        if (authState is AuthState.Error) viewModel.clearAuthError()
                    },
                    label = { Text("密碼") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = SapphirePrimary) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "隱藏密碼" else "顯示密碼"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        if (email.isNotBlank() && password.isNotBlank()) {
                            viewModel.signInWithEmail(email, password) { success, _ ->
                                if (success) onAuthSuccess()
                            }
                        }
                    }),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        resetEmail = email
                        showResetDialog = true
                    }) {
                        Text(
                            text = "忘記密碼？",
                            style = MaterialTheme.typography.bodySmall,
                            color = SapphirePrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.signInWithEmail(email, password) { success, _ ->
                            if (success) onAuthSuccess()
                        }
                    },
                    enabled = email.isNotBlank() && password.isNotBlank() && authState !is AuthState.Loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SapphirePrimary)
                ) {
                    if (authState is AuthState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
                    } else {
                        Text("立即登入", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            } else {
                // ==================== 多步驟註冊 (Multi-Step Onboarding) ====================

                // Top Progress Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (regStep == 2) {
                        IconButton(
                            onClick = { regStep = 1 },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "上一步",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(32.dp))
                    }

                    // Step Pills Indicator
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(32.dp)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(if (regStep == 1) SapphirePrimary else SapphirePrimary.copy(alpha = 0.3f))
                        )
                        Box(
                            modifier = Modifier
                                .width(32.dp)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(if (regStep == 2) SapphirePrimary else MaterialTheme.colorScheme.outlineVariant)
                        )
                    }

                    Text(
                        text = "步驟 $regStep / 2",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (regStep == 1) {
                    // ----- Step 1: 學業資料設定 -----
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Image(
                            painter = painterResource(id = com.example.R.drawable.ic_launcher_foreground),
                            contentDescription = "UniTrack Logo",
                            modifier = Modifier
                                .size(42.dp)
                                .scale(1.2f)
                        )
                        Text(
                            text = "就讀學校與系所設定",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // 1. 學校選擇 (固定 國立臺東大學)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "大學 / 學校名稱",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SapphirePrimary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "目前專屬支援",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SapphirePrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        OutlinedTextField(
                            value = university,
                            onValueChange = {},
                            readOnly = true,
                            leadingIcon = { Icon(Icons.Default.School, contentDescription = null, tint = SapphirePrimary) },
                            trailingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldAccent) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }

                    // 2. 科系選擇 (國立臺東大學各系所)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "主修科系",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        ExposedDropdownMenuBox(
                            expanded = deptExpanded,
                            onExpandedChange = { deptExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = department.ifBlank { "請選擇科系" },
                                onValueChange = {},
                                readOnly = true,
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = SapphirePrimary) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = deptExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = if (department.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = if (department.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = deptExpanded,
                                onDismissRequest = { deptExpanded = false }
                            ) {
                                deptOptions.forEach { dept ->
                                    DropdownMenuItem(
                                        text = { Text(dept) },
                                        onClick = {
                                            department = dept
                                            deptExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 3. 入學年度選擇
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "入學年度 / 學期",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        ExposedDropdownMenuBox(
                            expanded = yearExpanded,
                            onExpandedChange = { yearExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = admissionYear,
                                onValueChange = {},
                                readOnly = true,
                                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = SapphirePrimary) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
                                shape = RoundedCornerShape(14.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = yearExpanded,
                                onDismissRequest = { yearExpanded = false }
                            ) {
                                yearOptions.forEach { year ->
                                    DropdownMenuItem(
                                        text = { Text(year) },
                                        onClick = {
                                            admissionYear = year
                                            yearExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Notice Warning Box
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = AmberWarning.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = AmberWarning,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "學校、學系與入學年度將用於自動建立畢業審查學分門檻與課表排程。註冊後仍可在設定中調整。",
                                color = AmberWarning,
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = 18.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // 下一步按鈕
                    Button(
                        onClick = {
                            val semesterCode = if (admissionYear.contains("114")) "114-1"
                            else if (admissionYear.contains("113")) "113-1"
                            else if (admissionYear.contains("112")) "112-1"
                            else "111-1"

                            viewModel.updateGraduationPlan(
                                graduationPlan.copy(
                                    department = department.ifBlank { "資訊工程學系" },
                                    currentSemester = semesterCode
                                )
                            )
                            regStep = 2
                        },
                        enabled = department.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SapphirePrimary)
                    ) {
                        Text("下一步", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                } else {
                    // ----- Step 2: 建立帳號與安全密碼 -----
                    Text(
                        text = "建立 UniTrack+ 雲端帳號",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Google One-Tap Sign In
                    OutlinedButton(
                        onClick = {
                            viewModel.signInWithGoogle { success, _ ->
                                if (success) onAuthSuccess()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Google",
                            tint = SapphirePrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "使用 Google 帳號一鍵繼續",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Divider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f))
                        Text(
                            text = "  或使用電子郵件  ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f))
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("姓名 / 稱呼") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = EmeraldAccent) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            if (authState is AuthState.Error) viewModel.clearAuthError()
                        },
                        label = { Text("電子郵件 (Email)") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = EmeraldAccent) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("設定密碼 (至少 6 位)") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = EmeraldAccent) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "隱藏密碼" else "顯示密碼"
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("確認密碼") },
                        leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = null, tint = EmeraldAccent) },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            if (email.isNotBlank() && password.length >= 6 && password == confirmPassword) {
                                viewModel.signUpWithEmail(name, email, password) { success, _ ->
                                    if (success) onAuthSuccess()
                                }
                            }
                        }),
                        isError = confirmPassword.isNotBlank() && password != confirmPassword,
                        supportingText = {
                            if (confirmPassword.isNotBlank() && password != confirmPassword) {
                                Text("兩次輸入密碼不相符", color = RoseAccent)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.signUpWithEmail(name, email, password) { success, _ ->
                                if (success) onAuthSuccess()
                            }
                        },
                        enabled = email.isNotBlank() && password.length >= 6 && password == confirmPassword && authState !is AuthState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent)
                    ) {
                        if (authState is AuthState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
                        } else {
                            Text("建立帳號並登入", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }

    // Password Reset Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("重設密碼", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "請輸入您註冊時使用的電子郵件，我們將寄送重設密碼連結至您的信箱：",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("電子郵件") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            keyboardType = KeyboardType.Email
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (resetEmail.isNotBlank()) {
                            viewModel.sendPasswordReset(resetEmail)
                            showResetDialog = false
                        }
                    },
                    enabled = resetEmail.isNotBlank()
                ) {
                    Text("寄送重設信")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}
