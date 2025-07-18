package com.krosskinetic.notisentry.ui.screens

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.krosskinetic.notisentry.AppViewModel
import com.krosskinetic.notisentry.ui.theme.NotiSentryTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp


@HiltAndroidApp
class MyApp : Application()

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NotiSentryTheme {
                MainAppScreen()
            }
        }
    }
}

sealed class Screen(val route: String, val icon: ImageVector) {
    object Welcome : Screen("Welcome", Icons.AutoMirrored.Filled.List)
    object NotiPerm : Screen("Notification Permission", Icons.AutoMirrored.Filled.List)
    object AllDone : Screen("All Done", Icons.AutoMirrored.Filled.List)
    object FocusRules : Screen("Focus Rules", Icons.AutoMirrored.Filled.List)
    object Play : Screen("Start", Icons.Default.PlayCircle)
    object Summaries : Screen("Summaries", Icons.Default.Info)
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(appViewModel: AppViewModel = viewModel()) {
    val uiState by appViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val navController = rememberNavController()
    val items = listOf(
        Screen.FocusRules,
        Screen.Play,
        Screen.Summaries,
    )

    Scaffold(
        topBar = {
            if (uiState.introDone) {
                // Use the TopAppBar composable here
                TopAppBar(
                    title = {
                        Text(text = "NotiSentry")
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        },
        bottomBar = {
            if (uiState.introDone){
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = null) },
                            label = { Text(screen.route) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (uiState.introDone) Screen.FocusRules.route else Screen.Welcome.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            /**
             * Below are the 3 one-time view intro screen
             * */
            composable(Screen.Welcome.route) {
                IntroScreenWelcome({ navController.navigate(Screen.NotiPerm.route) })
            }
            composable(Screen.NotiPerm.route) {
                IntroScreenPermission(
                    appViewModel.isNotificationAccessGranted(context),
                    { navController.navigate(Screen.AllDone.route) })
            }
            composable(Screen.AllDone.route) {
                IntroScreenAllDone(
                    { navController.navigate(Screen.FocusRules.route) },
                    { appViewModel.updateIntroDone() })
            }

            /**
             * Below are the 3 of the actual screens
             * */
            composable(Screen.FocusRules.route) {
                FocusRules(
                    whitelistedApps = uiState.whitelistedApps,
                    appDetailList = uiState.appDetailList,
                    updateListOfAppDetails = { appViewModel.updateListOfAppDetails(context) },
                    updateWhitelistedApps = { appViewModel.updateWhitelistedApps(it) })
            }
            composable(Screen.Play.route) {
                StartScreen(
                    startStopFnc = {appViewModel.startStopFunc(context)}
                )
            }
            composable(Screen.Summaries.route) {
                SummariesScreen(
                    { appViewModel.updateSummaries() },
                    summaryToday = uiState.savedTodaySummaries,
                    summaryYesterday = uiState.savedYesterdaySummaries,
                    summaryArchive = uiState.savedArchiveSummaries,
                    savedSummaries = uiState.savedSummaries,
                    timeConverter = { appViewModel.formatTimestampToTime(it) }
                )
            }
        }
    }
}