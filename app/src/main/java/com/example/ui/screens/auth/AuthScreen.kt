package com.example.ui.screens.auth

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import com.example.R
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AuthState
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudentViewModel
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

enum class AuthPage {
    SPLASH,
    WELCOME,
    LOGIN,
    REGISTER
}

@Composable
fun AuthScreen(
    viewModel: StudentViewModel,
    modifier: Modifier = Modifier,
    onAuthSuccess: () -> Unit = {}
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    var currentPage by remember { mutableStateOf(AuthPage.SPLASH) }

    val initialTaiwanYear = remember { java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) - 1911 }
    var admissionYear by remember { mutableStateOf("$initialTaiwanYear 學年度 ($initialTaiwanYear-1)") }

    // Form inputs
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var showResetDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }

    // Auto-navigate on auth success
    val user = currentUser
    LaunchedEffect(user, currentPage) {
        if (user != null && currentPage != AuthPage.SPLASH) {
            onAuthSuccess()
        }
    }

    // Android System Back Button Handler
    BackHandler(enabled = currentPage != AuthPage.WELCOME && currentPage != AuthPage.SPLASH) {
        viewModel.clearAuthError()
        when (currentPage) {
            AuthPage.REGISTER -> currentPage = AuthPage.WELCOME
            AuthPage.LOGIN -> currentPage = AuthPage.WELCOME
            AuthPage.WELCOME, AuthPage.SPLASH -> {}
        }
    }

    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val isAuthenticating = authState is AuthState.Loading && currentPage != AuthPage.SPLASH

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)) // Deep Slate / Campus Night Theme
    ) {
        if (isAuthenticating) {
            AuthLoadingPageView()
        } else {
            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    if (targetState.ordinal > initialState.ordinal) {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut()
                        )
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> width } + fadeOut()
                        )
                    }
                },
                label = "AuthPageTransition"
            ) { targetPage ->
            when (targetPage) {
                AuthPage.SPLASH -> {
                    AuthSplashLoadingView(
                        viewModel = viewModel,
                        onFinishLoading = {
                            if (currentUser != null) {
                                onAuthSuccess()
                            } else {
                                currentPage = AuthPage.WELCOME
                            }
                        }
                    )
                }
                AuthPage.WELCOME -> {
                    WelcomePageView(
                        onStartRegister = {
                            viewModel.clearAuthError()
                            currentPage = AuthPage.REGISTER
                        },
                        onStartLogin = {
                            viewModel.clearAuthError()
                            currentPage = AuthPage.LOGIN
                        }
                    )
                }
                AuthPage.LOGIN -> {
                    LoginPageView(
                        viewModel = viewModel,
                        email = email,
                        onEmailChange = { email = it },
                        password = password,
                        onPasswordChange = { password = it },
                        passwordVisible = passwordVisible,
                        onTogglePasswordVisible = { passwordVisible = !passwordVisible },
                        onBack = {
                            viewModel.clearAuthError()
                            currentPage = AuthPage.WELCOME
                        },
                        onNavigateToRegister = {
                            viewModel.clearAuthError()
                            currentPage = AuthPage.REGISTER
                        },
                        onForgotPassword = {
                            resetEmail = email
                            showResetDialog = true
                        },
                        onAuthSuccess = onAuthSuccess
                    )
                }
                AuthPage.REGISTER -> {
                    RegisterPageView(
                        viewModel = viewModel,
                        name = name,
                        onNameChange = { name = it },
                        email = email,
                        onEmailChange = { email = it },
                        password = password,
                        onPasswordChange = { password = it },
                        confirmPassword = confirmPassword,
                        onConfirmPasswordChange = { confirmPassword = it },
                        passwordVisible = passwordVisible,
                        onTogglePasswordVisible = { passwordVisible = !passwordVisible },
                        admissionYear = admissionYear,
                        onAdmissionYearChange = { admissionYear = it },
                        onBack = {
                            viewModel.clearAuthError()
                            currentPage = AuthPage.WELCOME
                        },
                        onNavigateToLogin = {
                            viewModel.clearAuthError()
                            currentPage = AuthPage.LOGIN
                        },
                        onAuthSuccess = onAuthSuccess
                    )
                }
            }
        }
    }
    }

    // Password Reset Dialog (Redesigned Modern Glassmorphism Card)
    if (showResetDialog) {
        Dialog(
            onDismissRequest = { showResetDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF1E293B),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Top Glowing Icon Badge
                    Surface(
                        shape = CircleShape,
                        color = SapphirePrimary.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, SapphirePrimary.copy(alpha = 0.35f)),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.LockReset,
                                contentDescription = null,
                                tint = Color(0xFF60A5FA),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    // Title & Subtitle
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "重設密碼",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "請輸入您註冊時使用的電子郵件，我們將立即寄送重設密碼連結至您的信箱。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }

                    // Email Input Field
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("電子郵件 (Email)") },
                        placeholder = { Text("example@email.com", color = Color.White.copy(alpha = 0.3f)) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = null,
                                tint = if (resetEmail.isNotBlank()) SapphirePrimary else Color.White.copy(alpha = 0.5f)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            keyboardType = KeyboardType.Email
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = SapphirePrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedLabelColor = SapphirePrimary,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                            focusedContainerColor = Color.White.copy(alpha = 0.04f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.04f)
                        )
                    )

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showResetDialog = false },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.White.copy(alpha = 0.06f)
                            ),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                        ) {
                            Text("取消", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Medium)
                        }

                        Button(
                            onClick = {
                                if (resetEmail.isNotBlank()) {
                                    viewModel.sendPasswordReset(resetEmail)
                                    showResetDialog = false
                                }
                            },
                            enabled = resetEmail.isNotBlank(),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SapphirePrimary,
                                disabledContainerColor = SapphirePrimary.copy(alpha = 0.35f)
                            )
                        ) {
                            Text("寄送重設信", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 獨立全螢幕驗證與載入處理頁面 (Dedicated Full-Page Auth Loading Screen)
 */
@Composable
private fun AuthLoadingPageView(
    message: String = "正在進行帳號驗證與登入..."
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            AuthLogoMascot()

            Spacer(modifier = Modifier.height(36.dp))

            CircularProgressIndicator(
                modifier = Modifier.size(38.dp),
                color = SapphirePrimary,
                strokeWidth = 3.dp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "正在同步雲端與本地端學業資料，請稍候...",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * 啟動與登入載入進度條畫面 (Splash / Login Progress Loading Screen)
 * 在中央 App Logo 正下方呈現平滑動態進度條與狀態提示
 */
@Composable
private fun AuthSplashLoadingView(
    viewModel: StudentViewModel,
    onFinishLoading: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    var progress by remember { mutableFloatStateOf(0.08f) }
    var statusText by remember { mutableStateOf("正在初始化系統環境...") }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "loginProgressBar"
    )

    LaunchedEffect(Unit) {
        delay(200.milliseconds)
        progress = 0.35f
        statusText = "檢查登入狀態與學生檔案..."
        if (currentUser != null) {
            viewModel.downloadFromCloud(silent = true)
        }
        delay(350.milliseconds)
        progress = 0.70f
        statusText = "載入課表與學業資料..."
        delay(350.milliseconds)
        progress = 1.0f
        statusText = if (currentUser != null) "登入成功，準備進入系統！" else "載入完成"
        delay(300.milliseconds)
        onFinishLoading()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            // App Logo
            AuthLogoMascot()

            Spacer(modifier = Modifier.height(28.dp))

            // Progress Bar Container (登入進度條區域)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 進度條外框與發光進度指示
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        SapphirePrimary,
                                        Color(0xFF38BDF8),
                                        EmeraldAccent
                                    )
                                )
                            )
                    )
                }

                // 狀態文字與百分比
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${(animatedProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF60A5FA),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

/**
 * UniTrack+ 官方 App Logo 迎賓視覺
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
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = "UniTrack+ Logo",
            modifier = Modifier
                .size(230.dp)
                .scale(1.35f)
        )
    }
}

/**
 * 迎賓首頁分頁 (Welcome Page)
 */
@Composable
private fun WelcomePageView(
    onStartRegister: () -> Unit,
    onStartLogin: () -> Unit
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

        // Bottom Actions
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        ) {
            Button(
                onClick = onStartRegister,
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
                    modifier = Modifier.clickable(onClick = onStartLogin)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

/**
 * 獨立整頁登入頁面 (Full-screen Login Page)
 */
@Composable
private fun LoginPageView(
    viewModel: StudentViewModel,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onTogglePasswordVisible: () -> Unit,
    onBack: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onForgotPassword: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 8.dp, bottom = 24.dp)
    ) {
        // Top Nav (Pinned at the top)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = Color.White
                )
            }

            TextButton(onClick = onNavigateToRegister) {
                Text(
                    text = "註冊新帳號",
                    color = Color(0xFF60A5FA),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Main Login Form (Vertically Centered in Screen)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Title Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .size(46.dp)
                        .scale(1.25f)
                )
                Column {
                    Text(
                        text = "歡迎回到 UniTrack+",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "登入以同步你的課表、學分與記帳資料",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.65f)
                    )
                }
            }

            // Form Fields
            val isAuthError = authState is AuthState.Error

            OutlinedTextField(
                value = email,
                onValueChange = {
                    onEmailChange(it)
                    if (authState is AuthState.Error) viewModel.clearAuthError()
                },
                isError = isAuthError,
                label = { Text("電子郵件 (Email)") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = if (isAuthError) RoseAccent else SapphirePrimary) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = SapphirePrimary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedLabelColor = SapphirePrimary,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                    errorBorderColor = RoseAccent,
                    errorLabelColor = RoseAccent,
                    errorLeadingIconColor = RoseAccent,
                    errorTextColor = Color.White,
                    errorContainerColor = RoseAccent.copy(alpha = 0.05f)
                )
            )

            OutlinedTextField(
                value = password,
                onValueChange = {
                    onPasswordChange(it)
                    if (authState is AuthState.Error) viewModel.clearAuthError()
                },
                isError = isAuthError,
                label = { Text("密碼") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = if (isAuthError) RoseAccent else SapphirePrimary) },
                trailingIcon = {
                    IconButton(onClick = onTogglePasswordVisible) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "隱藏密碼" else "顯示密碼",
                            tint = if (isAuthError) RoseAccent else Color.White.copy(alpha = 0.7f)
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    if (email.isNotBlank() && password.isNotBlank()) {
                        viewModel.signInWithEmail(email, password) { success, _ ->
                            if (success) onAuthSuccess()
                        }
                    }
                }),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = SapphirePrimary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedLabelColor = SapphirePrimary,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                    errorBorderColor = RoseAccent,
                    errorLabelColor = RoseAccent,
                    errorLeadingIconColor = RoseAccent,
                    errorTrailingIconColor = RoseAccent,
                    errorTextColor = Color.White,
                    errorContainerColor = RoseAccent.copy(alpha = 0.05f)
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onForgotPassword) {
                    Text("忘記密碼？", style = MaterialTheme.typography.bodySmall, color = Color(0xFF60A5FA), fontWeight = FontWeight.SemiBold)
                }
            }

            Button(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.signInWithEmail(email, password) { success, errMsg ->
                        if (success) {
                            onAuthSuccess()
                        } else if (errMsg?.contains("不存在") == true || errMsg?.contains("註冊") == true || errMsg?.contains("尚未註冊") == true) {
                            viewModel.showToast("此帳號尚未註冊，請先註冊新帳號")
                            onNavigateToRegister()
                        }
                    }
                },
                enabled = email.isNotBlank() && password.isNotBlank() && authState !is AuthState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SapphirePrimary)
            ) {
                Text("立即登入", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            }

            // Divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.15f))
                Text("  或使用其他方式  ", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.45f))
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.15f))
            }

            // Google One-Tap Sign In
            val context = LocalContext.current
            OutlinedButton(
                onClick = {
                    viewModel.signInWithGoogle(context) { success, _ ->
                        if (success) {
                            onAuthSuccess()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White.copy(alpha = 0.06f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_google_logo),
                    contentDescription = "Google",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("使用 Google 帳號一鍵登入", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}

/**
 * 獨立整頁註冊頁面 (Full-screen Register Page)
 */
@Composable
private fun RegisterPageView(
    viewModel: StudentViewModel,
    name: String,
    onNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onTogglePasswordVisible: () -> Unit,
    admissionYear: String,
    onAdmissionYearChange: (String) -> Unit,
    onBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    val currentTaiwanYear = remember { java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) - 1911 }
    val yearOptions = remember(currentTaiwanYear) {
        (0 until 5).map { offset ->
            val y = currentTaiwanYear - offset
            "$y 學年度 ($y-1)"
        }
    }
    var yearExpanded by remember { mutableStateOf(false) }

    val yearNum = remember(admissionYear) {
        admissionYear.filter { it.isDigit() }.take(3).ifBlank { currentTaiwanYear.toString() }
    }
    val admissionCode = "$yearNum-1"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 6.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Top Nav
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = Color.White
                )
            }

            Text(
                text = "註冊新帳號",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.size(40.dp))
        }

        // Header Title
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "建立您的 UniTrack+ 帳號",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "同步您的課表、學分與記帳資料，輕鬆掌握校園生活",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }

        // Error Banner
        AnimatedVisibility(
            visible = authState is AuthState.Error,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            if (authState is AuthState.Error) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = RoseAccent.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, RoseAccent.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = RoseAccent, modifier = Modifier.size(18.dp))
                        Text(
                            text = (authState as AuthState.Error).message,
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Form Fields
        OutlinedTextField(
            value = name,
            onValueChange = {
                onNameChange(it)
                if (authState is AuthState.Error) viewModel.clearAuthError()
            },
            label = { Text("你的名字 / 暱稱") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = EmeraldAccent) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = EmeraldAccent,
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                focusedLabelColor = EmeraldAccent,
                unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
            )
        )

        OutlinedTextField(
            value = email,
            onValueChange = {
                onEmailChange(it)
                if (authState is AuthState.Error) viewModel.clearAuthError()
            },
            label = { Text("電子郵件 (Email)") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = EmeraldAccent) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = EmeraldAccent,
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                focusedLabelColor = EmeraldAccent,
                unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
            )
        )

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("設定密碼 (至少 6 位)") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = EmeraldAccent) },
            trailingIcon = {
                IconButton(onClick = onTogglePasswordVisible) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "隱藏密碼" else "顯示密碼",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = EmeraldAccent,
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                focusedLabelColor = EmeraldAccent,
                unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
            )
        )

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = { Text("確認密碼") },
            leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = null, tint = EmeraldAccent) },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                if (email.isNotBlank() && password.length >= 6 && password == confirmPassword) {
                    viewModel.signUpWithEmail(
                        name = name,
                        email = email,
                        pass = password,
                        admissionSemester = admissionCode
                    ) { success, _ ->
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
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = EmeraldAccent,
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                focusedLabelColor = EmeraldAccent,
                unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
            )
        )

        // 入學學年度選擇
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("入學學年度", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
            OutlinedTextField(
                value = admissionYear.ifBlank { "$currentTaiwanYear 學年度 ($currentTaiwanYear-1)" },
                onValueChange = {},
                readOnly = true,
                leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, tint = EmeraldAccent) },
                trailingIcon = {
                    IconButton(onClick = { yearExpanded = !yearExpanded }) {
                        Icon(
                            imageVector = if (yearExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { yearExpanded = !yearExpanded },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = if (yearExpanded) EmeraldAccent else Color.White.copy(alpha = 0.2f),
                    unfocusedBorderColor = if (yearExpanded) EmeraldAccent else Color.White.copy(alpha = 0.2f),
                    focusedContainerColor = Color.White.copy(alpha = 0.04f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.04f)
                )
            )

            AnimatedVisibility(
                visible = yearExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        yearOptions.forEach { year ->
                            val isSelected = admissionYear == year
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onAdmissionYearChange(year)
                                        yearExpanded = false
                                    },
                                color = if (isSelected) EmeraldAccent.copy(alpha = 0.15f) else Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = year,
                                        color = if (isSelected) EmeraldAccent else Color.White,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Button(
            onClick = {
                focusManager.clearFocus()
                viewModel.signUpWithEmail(
                    name = name,
                    email = email,
                    pass = password,
                    admissionSemester = admissionCode
                ) { success, _ ->
                    if (success) onAuthSuccess()
                }
            },
            enabled = email.isNotBlank() && password.length >= 6 && password == confirmPassword && authState !is AuthState.Loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent)
        ) {
            Text("建立帳號並登入", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
        }

        // Divider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.15f))
            Text("  或使用其他方式  ", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.45f))
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.15f))
        }

        // Google One-Tap Sign In
        val context = LocalContext.current
        OutlinedButton(
            onClick = {
                viewModel.signInWithGoogle(context) { success, _ ->
                    if (success) {
                        val currentPlan = viewModel.graduationPlan.value
                        val currentActiveSemester = com.example.data.local.DefaultData.getCurrentAcademicSemester()
                        val updatedPlan = currentPlan.copy(
                            studentName = name.ifBlank { currentPlan.studentName },
                            admissionSemester = admissionCode.ifBlank { currentPlan.admissionSemester },
                            currentSemester = currentActiveSemester
                        )
                        viewModel.updateGraduationPlan(updatedPlan) {
                            onAuthSuccess()
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White.copy(alpha = 0.06f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_google_logo),
                contentDescription = "Google",
                tint = Color.Unspecified,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text("使用 Google 帳號一鍵登入", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = Color.White)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("已有帳號？", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.width(6.dp))
            Text("登入", color = Color(0xFF60A5FA), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onNavigateToLogin))
        }
    }
}
