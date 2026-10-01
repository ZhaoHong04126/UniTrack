package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NavigationReproTest {

    @get:Rule
    val composeTestRule = androidx.compose.ui.test.junit4.v2.createComposeRule()

    private fun setupTestNav(
        onRouteUpdate: (String?) -> Unit
    ) {
        composeTestRule.setContent {
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            onRouteUpdate(currentRoute)

            val items = listOf("dashboard", "timetable", "calendar", "expense", "settings")

            Scaffold(
                bottomBar = {
                    NavigationBar {
                        items.forEach { destRoute ->
                            val selected = currentRoute == destRoute
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (currentRoute != destRoute) {
                                        navController.navigate(destRoute) {
                                            popUpTo("dashboard") {
                                                saveState = (destRoute != "dashboard")
                                            }
                                            launchSingleTop = true
                                            restoreState = (destRoute != "dashboard")
                                        }
                                    }
                                },
                                icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                                label = { Text(destRoute) },
                                modifier = Modifier.testTag("nav_$destRoute")
                            )
                        }
                    }
                }
            ) { padding ->
                NavHost(
                    navController = navController,
                    startDestination = "dashboard",
                    modifier = Modifier.padding(padding)
                ) {
                    composable("dashboard") {
                        Column {
                            // 1. "完整課表"
                            Button(
                                onClick = {
                                    navController.navigate("timetable") {
                                        popUpTo("dashboard") {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                modifier = Modifier.testTag("btn_timetable")
                            ) {
                                Text("完整課表")
                            }

                            // 2. "記帳本" 按鈕
                            Button(
                                onClick = {
                                    navController.navigate("expense") {
                                        popUpTo("dashboard") {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                modifier = Modifier.testTag("btn_expense")
                            ) {
                                Text("記帳本")
                            }

                            // 3. 預算卡片
                            Button(
                                onClick = {
                                    navController.navigate("expense") {
                                        popUpTo("dashboard") {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                modifier = Modifier.testTag("card_budget")
                            ) {
                                Text("預算卡片")
                            }

                            // 4. "詳細學分" 按鈕
                            Button(
                                onClick = {
                                    navController.navigate("graduation") {
                                        popUpTo("dashboard") {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                modifier = Modifier.testTag("btn_graduation")
                            ) {
                                Text("詳細學分")
                            }
                        }
                    }
                    composable("timetable") {
                        Text("課程表畫面")
                    }
                    composable("calendar") {
                        Text("行事曆畫面")
                    }
                    composable("expense") {
                        Text("記帳本畫面")
                    }
                    composable("settings") {
                        Text("設定畫面")
                    }
                    composable("graduation") {
                        Text("畢業審查畫面")
                    }
                }
            }
        }
    }

    @Test
    fun testReturnToDashboardFromTimetableButton() {
        var currentRoute: String? = null
        setupTestNav { currentRoute = it }

        composeTestRule.waitForIdle()
        assertEquals("dashboard", currentRoute)

        // 點擊「完整課表」
        composeTestRule.onNodeWithTag("btn_timetable").performClick()
        composeTestRule.waitForIdle()
        assertEquals("timetable", currentRoute)

        // 點擊下方「儀表板」按鈕
        composeTestRule.onNodeWithTag("nav_dashboard").performClick()
        composeTestRule.waitForIdle()
        assertEquals("dashboard", currentRoute)
    }

    @Test
    fun testReturnToDashboardFromExpenseButton() {
        var currentRoute: String? = null
        setupTestNav { currentRoute = it }

        composeTestRule.waitForIdle()
        assertEquals("dashboard", currentRoute)

        // 點擊「記帳本」按鈕
        composeTestRule.onNodeWithTag("btn_expense").performClick()
        composeTestRule.waitForIdle()
        assertEquals("expense", currentRoute)

        // 點擊下方「儀表板」按鈕
        composeTestRule.onNodeWithTag("nav_dashboard").performClick()
        composeTestRule.waitForIdle()
        assertEquals("dashboard", currentRoute)
    }

    @Test
    fun testReturnToDashboardFromBudgetCard() {
        var currentRoute: String? = null
        setupTestNav { currentRoute = it }

        composeTestRule.waitForIdle()
        assertEquals("dashboard", currentRoute)

        // 點擊預算卡片
        composeTestRule.onNodeWithTag("card_budget").performClick()
        composeTestRule.waitForIdle()
        assertEquals("expense", currentRoute)

        // 點擊下方「儀表板」按鈕
        composeTestRule.onNodeWithTag("nav_dashboard").performClick()
        composeTestRule.waitForIdle()
        assertEquals("dashboard", currentRoute)
    }

    @Test
    fun testReturnToDashboardFromGraduationButton() {
        var currentRoute: String? = null
        setupTestNav { currentRoute = it }

        composeTestRule.waitForIdle()
        assertEquals("dashboard", currentRoute)

        // 點擊「詳細學分」按鈕
        composeTestRule.onNodeWithTag("btn_graduation").performClick()
        composeTestRule.waitForIdle()
        assertEquals("graduation", currentRoute)

        // 點擊下方「儀表板」按鈕
        composeTestRule.onNodeWithTag("nav_dashboard").performClick()
        composeTestRule.waitForIdle()
        assertEquals("dashboard", currentRoute)
    }

    @Test
    fun testBottomBarTabSwitching() {
        var currentRoute: String? = null
        setupTestNav { currentRoute = it }

        composeTestRule.waitForIdle()
        assertEquals("dashboard", currentRoute)

        // 點擊下方「課程表」
        composeTestRule.onNodeWithTag("nav_timetable").performClick()
        composeTestRule.waitForIdle()
        assertEquals("timetable", currentRoute)

        // 點擊下方「行事曆」
        composeTestRule.onNodeWithTag("nav_calendar").performClick()
        composeTestRule.waitForIdle()
        assertEquals("calendar", currentRoute)

        // 點擊下方「儀表板」
        composeTestRule.onNodeWithTag("nav_dashboard").performClick()
        composeTestRule.waitForIdle()
        assertEquals("dashboard", currentRoute)
    }
}
