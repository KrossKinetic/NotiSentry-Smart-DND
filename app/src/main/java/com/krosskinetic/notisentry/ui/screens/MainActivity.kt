package com.krosskinetic.notisentry.ui.screens

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
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

    private val appViewModel: AppViewModel by viewModels()

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

    override fun onResume() {
        super.onResume()
        appViewModel.deleteOldNotificationsSummaries() // Deletes old notifications and summaries
    }
}

sealed class Screen(val route: String, val icon: ImageVector, val showNavBars: Boolean = false, val showTopBar: Boolean = true) {
    object Welcome : Screen("Welcome", Icons.AutoMirrored.Filled.List, showTopBar = false)
    object NotiPerm : Screen("Notification Permission", Icons.AutoMirrored.Filled.List, showTopBar = false)
    object NotiPerm2 : Screen("Notification Permission 2", Icons.AutoMirrored.Filled.List, showTopBar = false)
    object AllDone : Screen("All Done", Icons.AutoMirrored.Filled.List, showTopBar = false)
    object FocusRules : Screen("Focus Rules", Icons.AutoMirrored.Filled.List, showNavBars = true)
    object Play : Screen("Start", Icons.Default.PlayCircle, showNavBars = true)
    object Summaries : Screen("Summaries", Icons.Default.Info, showNavBars = true)
    object FilteredNotifs : Screen("Filtered Notifications", Icons.Default.Info)

    object Loading : Screen("Loading", Icons.Default.Info)

    object OnBoarding : Screen("OnBoarding", Icons.Default.Info)

    object FilteredNotifs2 : Screen("FilteredNotifs2", Icons.Default.Info)
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
        Screen.Summaries
    )

    val anotherItems = listOf(
        Screen.FocusRules,
        Screen.Play,
        Screen.Summaries,
        Screen.NotiPerm,
        Screen.NotiPerm2,
        Screen.AllDone,
        Screen.Welcome,
        Screen.Loading,
        Screen.FilteredNotifs,
        Screen.OnBoarding,
        Screen.FilteredNotifs2
    )

    Scaffold(
        topBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            val shouldShowBars = anotherItems.find { it.route == currentRoute }?.showTopBar == true

            AnimatedVisibility(
                visible = shouldShowBars,
                enter = fadeIn(animationSpec = tween(durationMillis = 100)),
                exit = fadeOut(animationSpec = tween(durationMillis = 100))
            ) {
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
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            val shouldShowBars = anotherItems.find { it.route == currentRoute }?.showNavBars == true

            AnimatedVisibility(
                visible = shouldShowBars,
                enter = fadeIn(animationSpec = tween(durationMillis = 100)),
                exit = fadeOut(animationSpec = tween(durationMillis = 100))
            ) {
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
             * Below are the 4 one-time view intro screen
             * */
            composable(Screen.Welcome.route,
                enterTransition = {
                    fadeIn(
                        animationSpec = tween(durationMillis = 100)
                    )
                },
                exitTransition = { fadeOut(
                    animationSpec = tween(durationMillis = 100)
                )}) {
                IntroScreenWelcome({ navController.navigate(Screen.NotiPerm.route) })
            }

            composable(Screen.NotiPerm.route,
                enterTransition = {
                    fadeIn(
                        animationSpec = tween(durationMillis = 100)
                    )
                },
                exitTransition = { fadeOut(
                    animationSpec = tween(durationMillis = 100)
                )}) {
                IntroScreenPermission(
                    {appViewModel.isNotificationAccessGranted(context)},
                    { navController.navigate(Screen.NotiPerm2.route) },
                    fn = { appViewModel.sendAndCancelTestNotification(context) })
            }

            composable(Screen.NotiPerm2.route,
                enterTransition = {
                    fadeIn(
                        animationSpec = tween(durationMillis = 100)
                    )
                },
                exitTransition = { fadeOut(
                    animationSpec = tween(durationMillis = 100)
                )}) {
                IntroScreenPermission2(
                    { navController.navigate(Screen.OnBoarding.route) })
            }

            composable(route=Screen.OnBoarding.route,
                enterTransition = {
                    fadeIn(
                        animationSpec = tween(durationMillis = 100)
                    )
                },
                exitTransition = { fadeOut(
                    animationSpec = tween(durationMillis = 100)
                )}) {
                OnBoarding({ navController.navigate(Screen.AllDone.route) })
            }

            composable(Screen.AllDone.route,
                enterTransition = {
                    fadeIn(
                        animationSpec = tween(durationMillis = 100)
                    )
                },
                exitTransition = { fadeOut(
                    animationSpec = tween(durationMillis = 100)
                )}) {
                IntroScreenAllDone(
                    { navController.navigate(Screen.FocusRules.route) },
                    { appViewModel.updateIntroDone() })
            }

            /**
             * Below are the 3 of the actual screens
             * */
            composable(Screen.FocusRules.route,
                enterTransition = {
                    fadeIn(
                        animationSpec = tween(durationMillis = 100)
                    )
                },
                exitTransition = { fadeOut(
                    animationSpec = tween(durationMillis = 100)
                )}) {
                FocusRules(
                    blacklistedApps = uiState.blacklistedApps,
                    appDetailList = uiState.appDetailList,
                    updateListOfAppDetails = { appViewModel.updateListOfAppDetails(context) },
                    updateBlacklistedApps = { appViewModel.updateBlacklistedApps(it) })
            }
            composable(Screen.Play.route,
                enterTransition = {
                    fadeIn(
                        animationSpec = tween(durationMillis = 100)
                    )
                },
                exitTransition = { fadeOut(
                    animationSpec = tween(durationMillis = 100)
                )}) {
                StartScreen(
                    startStopFnc = { appViewModel.startStopFunc(context) },
                    start = uiState.startService,
                    text = uiState.smartCategorizationString,
                    updateText = { appViewModel.updateSmartCategorizationString(it) },
                    updateAutoDelete = { appViewModel.updateAutoDeleteKey(it) },
                    autoDelete = uiState.autoDeleteValue.toFloat(),
                    counter = uiState.notificationCaptured,
                    updateNotifFilters = {appViewModel.updateFilteredNotifsNoEnd(uiState.startServiceTime)},
                    notifFilters =  { navController.navigate(Screen.FilteredNotifs2.route) }
                )
            }
            composable(Screen.Summaries.route,
                enterTransition = {
                    fadeIn(
                        animationSpec = tween(durationMillis = 100)
                    )
                },
                exitTransition = { fadeOut(
                    animationSpec = tween(durationMillis = 100)
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
                    newScreen = {navController.navigate(Screen.FilteredNotifs.route)},
                    deleteSummary = {appViewModel.deleteSummaryWithNotificationFromId(it)}
                )
            }
            composable(Screen.FilteredNotifs.route,
                enterTransition = {
                    fadeIn(
                        animationSpec = tween(durationMillis = 100)
                    )
                },
                exitTransition = { fadeOut(
                    animationSpec = tween(durationMillis = 100)
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
            composable(Screen.FilteredNotifs2.route,
                enterTransition = {
                    fadeIn(
                        animationSpec = tween(durationMillis = 100)
                    )
                },
                exitTransition = { fadeOut(
                    animationSpec = tween(durationMillis = 100)
                )}) {
                FilteredNotifsScreen(
                    notifs = uiState.filteredNotifs,
                    timeConverter = {
                        appViewModel.formatTimestampToTime(it)
                    },
                    newScreen = { navController.navigate(Screen.Play.route) },
                    getAppIcon = { appViewModel.getAppIconByPackageName(context, it) },
                )
            }
        }
    }
}