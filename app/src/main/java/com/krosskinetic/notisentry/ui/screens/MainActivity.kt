package com.krosskinetic.notisentry.ui.screens

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.android.gms.ads.MobileAds
import com.krosskinetic.notisentry.AppViewModel
import com.krosskinetic.notisentry.ui.theme.NotiSentryTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@HiltAndroidApp
class MyApp : Application()

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Admob
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(this@MainActivity) {}
        }

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
    object FilteredNotifs : Screen("Filtered Notifications", Icons.Default.Info)

    object Loading : Screen("Loading", Icons.Default.Info)

    object SmartCategorizationScreen : Screen("Loading", Icons.Default.Info)
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
            if (uiState.introDone == true) {
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
            if (uiState.introDone == true){
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
            startDestination = if (uiState.introDone == null) Screen.Loading.route else if (uiState.introDone == false) Screen.Welcome.route else Screen.FocusRules.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Loading.route) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            /**
             * Below are the 3 one-time view intro screen
             * */
            composable(Screen.Welcome.route,
                enterTransition = { slideIntoContainer(
                    animationSpec = tween(durationMillis = 500),
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                )},
                exitTransition = { fadeOut(
                    animationSpec = tween(durationMillis = 400)
                )}) {
                IntroScreenWelcome({ navController.navigate(Screen.NotiPerm.route) })
            }
            composable(Screen.NotiPerm.route,
                enterTransition = { slideIntoContainer(
                    animationSpec = tween(durationMillis = 500),
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                )},
                exitTransition = { fadeOut(
                    animationSpec = tween(durationMillis = 400)
                )}) {
                IntroScreenPermission(
                    appViewModel.isNotificationAccessGranted(context),
                    { navController.navigate(Screen.AllDone.route) })
            }
            composable(Screen.AllDone.route,
                enterTransition = { slideIntoContainer(
                    animationSpec = tween(durationMillis = 500),
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                )},
                exitTransition = { fadeOut(
                    animationSpec = tween(durationMillis = 400)
                )}) {
                IntroScreenAllDone(
                    { navController.navigate(Screen.FocusRules.route) },
                    { appViewModel.updateIntroDone() })
            }

            /**
             * Below are the 3 of the actual screens
             * */
            composable(Screen.FocusRules.route,
                enterTransition = { slideIntoContainer(
                    animationSpec = tween(durationMillis = 500),
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                )},
                exitTransition = { fadeOut(
                    animationSpec = tween(durationMillis = 400)
                )}) {
                FocusRules(
                    blacklistedApps = uiState.blacklistedApps,
                    appDetailList = uiState.appDetailList,
                    updateListOfAppDetails = { appViewModel.updateListOfAppDetails(context) },
                    updateBlacklistedApps = { appViewModel.updateBlacklistedApps(it) })
            }
            composable(Screen.Play.route,
                enterTransition = { slideIntoContainer(
                    animationSpec = tween(durationMillis = 500),
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                )},
                exitTransition = { fadeOut(
                    animationSpec = tween(durationMillis = 400)
                )}) {
                StartScreen(
                    startStopFnc = { appViewModel.startStopFunc() },
                    start = uiState.startService,
                    useSmartCategorization = { appViewModel.updateSmartCategorization() },
                    useSmartBoolean = uiState.useSmartCategorization,
                    goToSmartScreenCategorization = {navController.navigate(Screen.SmartCategorizationScreen.route)}
                )
            }
            composable(Screen.Summaries.route,
                enterTransition = { slideIntoContainer(
                    animationSpec = tween(durationMillis = 500),
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                )},
                exitTransition = { fadeOut(
                    animationSpec = tween(durationMillis = 400)
                )}) {
                SummariesScreen(
                    { appViewModel.updateSummaries() },
                    summaryToday = uiState.savedTodaySummaries,
                    summaryYesterday = uiState.savedYesterdaySummaries,
                    summaryArchive = uiState.savedArchiveSummaries,
                    savedSummaries = uiState.savedSummaries,
                    timeConverter = {
                        appViewModel.formatTimestampToTime(it)
                    },
                    allNotifFunc = {startTime,endTime -> appViewModel.updateFilteredNotifs(startTime, endTime)},
                    newScreen = {navController.navigate(Screen.FilteredNotifs.route)}
                )
            }
            composable(Screen.FilteredNotifs.route,
                enterTransition = { slideIntoContainer(
                    animationSpec = tween(durationMillis = 500),
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                )},
                exitTransition = { fadeOut(
                    animationSpec = tween(durationMillis = 400)
                )}) {
                FilteredNotifsScreen(
                    notifs = uiState.filteredNotifs,
                    timeConverter = {
                        appViewModel.formatTimestampToTime(it)
                    },
                    newScreen = { navController.navigate(Screen.Summaries.route) },
                    getAppIcon = { appViewModel.getAppIconByPackageName(context, it) },
                )
            }
            composable(Screen.SmartCategorizationScreen.route,
                enterTransition = { slideIntoContainer(
                    animationSpec = tween(durationMillis = 500),
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                )},
                exitTransition = { fadeOut(
                    animationSpec = tween(durationMillis = 400)
                )}) {
                SmartCategorizationScreen(
                    goBack = { navController.navigate(Screen.Play.route) },
                    text = uiState.smartCategorizationString,
                    updateText = {appViewModel.updateSmartCategorizationString(it)}
                )
            }
        }
    }
}