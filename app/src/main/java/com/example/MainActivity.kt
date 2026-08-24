package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.auth.AuthScreen
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.expense.ExpenseScreen
import com.example.ui.screens.graduation.CourseAuditListScreen
import com.example.ui.screens.graduation.GraduationPlanScreen
import com.example.ui.screens.graduation.GraduationScreen
import com.example.ui.screens.graduation.GraduationThresholdsScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.timetable.GradeEntryScreen
import com.example.ui.screens.timetable.TimetableScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.StudentViewModel

sealed class AppDestination(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    object Dashboard : AppDestination(
        route = "dashboard",
        title = "儀表板",
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard,
        testTag = "nav_dashboard"
    )

    object Timetable : AppDestination(
        route = "timetable",
        title = "課程表",
        selectedIcon = Icons.Filled.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth,
        testTag = "nav_timetable"
    )

    object Graduation : AppDestination(
        route = "graduation",
        title = "畢業審查",
        selectedIcon = Icons.Filled.School,
        unselectedIcon = Icons.Outlined.School,
        testTag = "nav_graduation"
    )

    object Expense : AppDestination(
        route = "expense",
        title = "記帳本",
        selectedIcon = Icons.Filled.AccountBalanceWallet,
        unselectedIcon = Icons.Outlined.AccountBalanceWallet,
        testTag = "nav_expense"
    )

    object Settings : AppDestination(
        route = "settings",
        title = "設定",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        testTag = "nav_settings"
    )
}

class MainActivity : ComponentActivity() {
    @Suppress("SpellCheckingInspection")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val studentViewModel: StudentViewModel = viewModel()
                val userMessage by studentViewModel.userMessage.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(userMessage) {
                    userMessage?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                        studentViewModel.clearUserMessage()
                    }
                }

                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val items = listOf(
                    AppDestination.Dashboard,
                    AppDestination.Timetable,
                    AppDestination.Expense,
                    AppDestination.Settings
                )

                val showBottomBar = items.any { it.route == currentRoute } || currentRoute == AppDestination.Graduation.route
                val currentUser by studentViewModel.currentUser.collectAsStateWithLifecycle()
                val graduationPlan by studentViewModel.graduationPlan.collectAsStateWithLifecycle()
                val isProfileReady = currentUser != null && graduationPlan.department.isNotBlank() && graduationPlan.department != "尚未設定系所"

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 8.dp
                            ) {
                                items.forEach { dest ->
                                    val selected = currentRoute == dest.route
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = {
                                            if (currentRoute != dest.route) {
                                                navController.navigate(dest.route) {
                                                    popUpTo(AppDestination.Dashboard.route) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                contentDescription = dest.title
                                            )
                                        },
                                        label = { Text(dest.title) },
                                        modifier = Modifier.testTag(dest.testTag)
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = if (isProfileReady) AppDestination.Dashboard.route else "auth",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        composable(AppDestination.Dashboard.route) {
                            DashboardScreen(
                                viewModel = studentViewModel,
                                onNavigateToTimetable = {
                                    navController.navigate(AppDestination.Timetable.route)
                                },
                                onNavigateToGraduation = {
                                    navController.navigate(AppDestination.Graduation.route)
                                },
                                onNavigateToExpense = {
                                    navController.navigate(AppDestination.Expense.route)
                                },
                                onNavigateToSettings = {
                                    navController.navigate(AppDestination.Settings.route)
                                }
                            )
                        }

                        composable(AppDestination.Timetable.route) {
                            TimetableScreen(
                                viewModel = studentViewModel,
                                onNavigateToGrades = {
                                    navController.navigate("grade_entry")
                                }
                            )
                        }

                        composable("grade_entry") {
                            GradeEntryScreen(
                                viewModel = studentViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToGraduation = {
                                    navController.navigate(AppDestination.Graduation.route)
                                }
                            )
                        }

                        composable(AppDestination.Graduation.route) {
                            GraduationScreen(
                                viewModel = studentViewModel,
                                onNavigateToThresholds = {
                                    navController.navigate("graduation_thresholds")
                                },
                                onNavigateToCourseAudit = {
                                    navController.navigate("course_audit_list")
                                },
                                onNavigateToPlanSetting = {
                                    navController.navigate("graduation_plan_setting")
                                },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("graduation_plan_setting") {
                            GraduationPlanScreen(
                                viewModel = studentViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("graduation_thresholds") {
                            GraduationThresholdsScreen(
                                viewModel = studentViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("course_audit_list") {
                            CourseAuditListScreen(
                                viewModel = studentViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(AppDestination.Expense.route) {
                            ExpenseScreen(viewModel = studentViewModel)
                        }

                        composable(AppDestination.Settings.route) {
                            SettingsScreen(
                                viewModel = studentViewModel,
                                onNavigateToAuth = {
                                    navController.navigate("auth")
                                },
                                onNavigateToPlanSetting = {
                                    navController.navigate("graduation_plan_setting")
                                }
                            )
                        }

                        composable("auth") {
                            AuthScreen(
                                viewModel = studentViewModel,
                                onAuthSuccess = {
                                    navController.navigate(AppDestination.Dashboard.route) {
                                        popUpTo("auth") { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
