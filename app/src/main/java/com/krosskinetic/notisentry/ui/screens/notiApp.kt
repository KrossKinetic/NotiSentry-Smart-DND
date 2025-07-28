package com.krosskinetic.notisentry.ui.screens

import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import coil.compose.AsyncImage
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.krosskinetic.notisentry.data.AppDetails
import com.krosskinetic.notisentry.data.AppNotificationSummary
import com.krosskinetic.notisentry.data.AppNotifications
import compose_util.BannerAd
import com.krosskinetic.notisentry.BuildConfig
import com.krosskinetic.notisentry.data.AppBlacklist
import kotlin.math.roundToInt

@Composable
fun FocusRules(updateBlacklistedApps: (String) -> Unit, updateListOfAppDetails: () -> Unit, blacklistedApps: List<AppBlacklist>, appDetailList: List<AppDetails>, modifier: Modifier = Modifier){
    // Search
    var searchQuery by remember { mutableStateOf("") }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        updateListOfAppDetails() /** Updates uiState to get list of new apps everytime the user backs out and app resumes, triggering recomposition */
        searchQuery = ""
    }

    val whitelistedSet = remember(blacklistedApps) { // As long as blacklistedApps doesn't change, no need to recompose
        blacklistedApps.map { it.packageName }.toSet()
    }


    // Filtered list based on search query
    val appDetailsList = remember(searchQuery, appDetailList) {
        if (searchQuery.isBlank()) {
            appDetailList
        } else {
            appDetailList.filter {
                it.appName.contains(searchQuery, ignoreCase = true)
            }
        }
    }


    Column (modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            placeholder = {Text(text="eg. Instagram")},
            label = {Text(text="Search Apps")}
        )

        if (appDetailsList.isNotEmpty()){
            LazyColumn(modifier = modifier) {
                items(appDetailsList) { item ->

                    val isChecked = whitelistedSet.contains(item.packageName)

                    AppCard(
                        appName = item.appName,
                        image = item.icon,
                        anExecutableFunction = { updateBlacklistedApps(item.packageName) },
                        checked = isChecked
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun AppCard(appName: String, image: Drawable, anExecutableFunction: () -> Unit, checked: Boolean, modifier: Modifier = Modifier){
    Card (modifier = modifier
        .padding(10.dp)
        .fillMaxWidth()
        .height(70.dp),
        shape = RoundedCornerShape(8.dp)) {

        Row (modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = image,
                contentDescription = "$appName icon",
                modifier = Modifier
                    .size(40.dp)
                    .padding(start = 10.dp)
                    .align(Alignment.CenterVertically),
            )
            Text(
                text = appName,
                modifier = Modifier
                    .padding(start = 10.dp)
                    .align(Alignment.CenterVertically)
                    .fillMaxWidth(0.8f)
            )
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = checked,
                onCheckedChange = {
                    anExecutableFunction()
                                  },
                modifier = Modifier
                    .padding(10.dp)
                    .align(Alignment.CenterVertically)
            )
        }
    }
}

/**
 *
 * Below is the UI for Summaries Screen
 *
 * */

@Composable
fun SummariesScreen(anExecutableFunction: () -> Unit,
                    timeConverter: (Long) -> String,
                    savedSummaries: List<AppNotificationSummary>,
                    summaryToday: List<AppNotificationSummary>,
                    summaryYesterday: List<AppNotificationSummary>,
                    summaryArchive: List<AppNotificationSummary>,
                    modifier: Modifier = Modifier,
                    allNotifFunc: (startTime: Long, endTime: Long) -> Unit,
                    newScreen: () -> Unit,
                    deleteSummary: (Int) -> Unit){
    LaunchedEffect(savedSummaries) { /** LaunchedEffect only processes the change when key changes, key = unit so it only happens once because unit == void and it never changes */
        anExecutableFunction() /** Updates uiState, triggering recomposition */
    }

    var showSummaryToday by remember { mutableStateOf(true) }
    var showSummaryYesterday by remember { mutableStateOf(false) }
    var showSummaryArchive by remember { mutableStateOf(false) }

    if (!savedSummaries.isEmpty()){
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            // --- TODAY'S SECTION ---
            if (summaryToday.isNotEmpty()) {
                item {
                    Row(modifier = Modifier.padding(top = 5.dp)) {
                        Text(
                            text = "Today",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                        Spacer(modifier = Modifier.weight(0.8f))
                        IconButton(
                            onClick = {
                                showSummaryToday = !showSummaryToday
                                showSummaryYesterday = false
                                showSummaryArchive = false
                            },
                        ) {
                            Icon(
                                imageVector = if (showSummaryToday) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = "Show/Hide"
                            )
                        }
                    }
                }
                items(summaryToday) { summary ->
                    AnimatedVisibility(
                        visible = showSummaryToday,
                        enter = expandVertically(
                            expandFrom = Alignment.Top,
                            animationSpec = tween(durationMillis = 300)
                        ),
                        exit = shrinkVertically(
                            shrinkTowards = Alignment.Top,
                            animationSpec = tween(durationMillis = 300)
                        )
                    ) {
                        Column {
                            AppCardSummary(
                                notiText = summary.summaryText,
                                timestampStart = timeConverter(summary.startTimestamp),
                                timestampEnd = timeConverter(summary.endTimestamp),
                                allNotifFunc = {
                                    allNotifFunc(
                                        summary.startTimestamp,
                                        summary.endTimestamp
                                    )
                                },
                                newScreen = newScreen,
                                deleteSummary = {
                                    deleteSummary(summary.id)
                                }
                            )
                        }
                    }
                }
            }

            // --- YESTERDAY'S SECTION ---
            if (summaryYesterday.isNotEmpty()) {
                item {
                    HorizontalDivider(thickness = 1.dp, modifier = Modifier.padding(top = 8.dp))
                    Row(modifier = Modifier.padding(top = 5.dp)) {
                        Text(
                            text = "Yesterday",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                        Spacer(modifier = Modifier.weight(0.8f))
                        IconButton(
                            onClick = {
                                showSummaryYesterday = !showSummaryYesterday
                                showSummaryToday = false
                                showSummaryArchive = false
                            },
                        ) {
                            Icon(
                                imageVector = if (showSummaryYesterday) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = "Show/Hide"
                            )
                        }
                    }
                }
                items(summaryYesterday) { summary ->
                    AnimatedVisibility(
                        visible = showSummaryYesterday,
                        enter = expandVertically(
                            expandFrom = Alignment.Top,
                            animationSpec = tween(durationMillis = 300)
                        ),
                        exit = shrinkVertically(
                            shrinkTowards = Alignment.Top,
                            animationSpec = tween(durationMillis = 300)
                        )
                    ) {
                        Column {
                            AppCardSummary(
                                notiText = summary.summaryText,
                                timestampStart = timeConverter(summary.startTimestamp),
                                timestampEnd = timeConverter(summary.endTimestamp),
                                allNotifFunc = {
                                    allNotifFunc(
                                        summary.startTimestamp,
                                        summary.endTimestamp
                                    )
                                },
                                newScreen = newScreen,
                                deleteSummary = {
                                    deleteSummary(summary.id)
                                }
                            )
                        }
                    }
                }
            }

            // --- ARCHIVE SECTION ---
            if (summaryArchive.isNotEmpty()) {
                item {
                    HorizontalDivider(thickness = 1.dp, modifier = Modifier.padding(top = 8.dp))
                    Row(modifier = Modifier.padding(top = 5.dp)) {
                        Text(
                            text = "Archive",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                        Spacer(modifier = Modifier.weight(0.8f))
                        IconButton(
                            onClick = {
                                showSummaryArchive = !showSummaryArchive
                                showSummaryToday = false
                                showSummaryYesterday = false
                            },
                        ) {
                            Icon(
                                imageVector = if (showSummaryArchive) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = "Show/Hide"
                            )
                        }
                    }
                }
                items(summaryArchive) { summary ->
                    AnimatedVisibility(
                        visible = showSummaryArchive,
                        enter = expandVertically(
                            expandFrom = Alignment.Top,
                            animationSpec = tween(durationMillis = 300)
                        ),
                        exit = shrinkVertically(
                            shrinkTowards = Alignment.Top,
                            animationSpec = tween(durationMillis = 300)
                        )
                    ) {
                        Column {
                            AppCardSummary(
                                notiText = summary.summaryText,
                                timestampStart = timeConverter(summary.startTimestamp),
                                timestampEnd = timeConverter(summary.endTimestamp),
                                allNotifFunc = {
                                    allNotifFunc(
                                        summary.startTimestamp,
                                        summary.endTimestamp
                                    )
                                },
                                newScreen = newScreen,
                                deleteSummary = {
                                    deleteSummary(summary.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "It's so quiet here...")
        }
    }
}

@Composable
fun AppCardSummary(notiText: String, timestampStart: String, timestampEnd: String, modifier: Modifier = Modifier, allNotifFunc: () -> Unit, newScreen: () -> Unit, deleteSummary: () -> Unit){
    Card (modifier = modifier
        .fillMaxWidth()
        .padding(top = 15.dp),
        shape = RoundedCornerShape(8.dp)) {
        Row (modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceAround) {
            AssistChip(
                onClick = {},
                label = {Text(text = "From: $timestampStart")},
            )
            AssistChip(
                onClick = {},
                label = {Text(text = "End: $timestampEnd")},
            )
        }
        Text(
            text = notiText,
            modifier = Modifier
                .padding(10.dp)
                .padding(bottom = 0.dp)
                .fillMaxWidth()
        )
        Row (modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp), horizontalArrangement = Arrangement.SpaceAround) {
            Button(onClick = {
                allNotifFunc()
                newScreen()
            }, modifier = Modifier.padding(5.dp), shape = RoundedCornerShape(8.dp)) {
                Text(text = "See All Notifications")
            }
            Button(onClick = {
                deleteSummary()
            }, modifier = Modifier.padding(5.dp), shape = RoundedCornerShape(8.dp)) {
                Text(text = "Delete Summary")
            }
        }
    }
}

@Composable
fun FilteredNotifsScreen(
    modifier: Modifier = Modifier,
    notifs: List<AppNotifications>,
    timeConverter: (Long) -> String,
    newScreen: () -> Unit,
    getAppIcon: (String) -> Drawable?
) {
    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp)
                .weight(1f) // Let the list take available space
        ) {
            items(notifs) { notif ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // --- Header: Icon, App Name, and Time ---
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = getAppIcon(notif.packageName),
                                contentDescription = "${notif.appName} icon",
                                modifier = Modifier.size(32.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            // Use the pre-fetched appName from the notif object
                            Text(
                                text = "${notif.appName} • ${timeConverter(notif.timestamp)}",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val displayTitle = notif.conversationTitle.ifBlank { notif.title }

                        if (displayTitle.isNotBlank()) {
                            Text(
                                text = displayTitle,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        // **Correct Logic:** Prioritize messages list, then fallback to text
                        if (notif.messages.isNotEmpty()) {
                            // Display each message for chat-style notifications
                            Column {
                                notif.messages.forEach { message ->
                                    Text(text = "${message.sender}: ${message.text}")
                                }
                            }
                        } else if (notif.text.isNotBlank()) {
                            // Display the main text for standard notifications
                            Text(text = notif.text)
                        } else {
                            // Fallback if there is no displayable content
                            Text(text = "No content to display.")
                        }
                    }
                }
            }
        }

        // --- Bottom Button ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(onClick = { newScreen() }) {
                Text(text = "Go Back")
            }
        }
    }
}

/**
 *
 * Below is the UI for Start Screen
 *
 * */

@Composable
fun StartScreen(
    startStopFnc: () -> Unit,
    start: Boolean,
    modifier: Modifier = Modifier,
    useSmartCategorization: () -> Unit,
    useSmartBoolean: Boolean,
    goToSmartScreenCategorization: () -> Unit,
    updateAutoDelete: (Int) -> Unit,
    autoDelete: Float
){

    val scrollState = rememberScrollState()

    Column(modifier = modifier
        .fillMaxSize()
        .verticalScroll(scrollState), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Top) {

        val context = LocalContext.current

        val adUnitIdOther = if (BuildConfig.DEBUG) {
            BuildConfig.ADMOB_BANNER_UNIT_ID_DEBUG
        } else {
            BuildConfig.ADMOB_BANNER_UNIT_ID_RELEASE
        }

        // Init the adview
        val adView = remember {
            AdView(context).apply {
                adUnitId = adUnitIdOther
                setAdSize(AdSize.BANNER)
                loadAd(AdRequest.Builder().build())
            }
        }

        // Kill when the ad goes out of picture to save battery
        DisposableEffect(Unit) {
            onDispose {
                adView.destroy() // Crucial for releasing resources and preventing memory leaks
            }
        }


        BannerAd(adView= adView, modifier = Modifier.padding(10.dp))

        UseSmartNotificationFilter(modifier, useSmartCategorization, useSmartBoolean, navScreen = goToSmartScreenCategorization)

        SetAutoDelete(curValue = autoDelete, updateAutoDelete = {updateAutoDelete(it)})

        Spacer(modifier = Modifier.weight(1f))

        Button(onClick = {
            startStopFnc()
        },
            modifier = Modifier.padding(60.dp)
        ) {
            Text(text = if (!start) "Start NotiSentry" else "Stop NotiSentry")
        }

    }
}

@Composable
fun UseSmartNotificationFilter(modifier: Modifier = Modifier, updateSmartCategorization: () -> Unit, useSmartBoolean: Boolean, navScreen: () -> Unit){
    Card (modifier = modifier
        .padding(10.dp)
        .fillMaxWidth()
        .height(70.dp),
        shape = RoundedCornerShape(8.dp)) {

        Row (modifier = Modifier.fillMaxSize()) {

            Row (modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clickable(onClick = {
                    navScreen()
                })) {
                Text(
                    text = "Use Smart Notification Categorization",
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .align(Alignment.CenterVertically)
                )

                Spacer(modifier = Modifier.weight(1f))

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "Expand",
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .align(Alignment.CenterVertically)
                        .size(18.dp)
                )
            }

            VerticalDivider(modifier = Modifier
                .padding(all = 10.dp)
                .padding(start = 0.dp))

            Switch(
                checked = useSmartBoolean,
                onCheckedChange = {
                    updateSmartCategorization()
                },
                modifier = Modifier
                    .padding(end = 10.dp)
                    .align(Alignment.CenterVertically)
            )
        }
    }
}

@Composable
fun SetAutoDelete(modifier: Modifier = Modifier, updateAutoDelete: (Int) -> Unit, curValue: Float){

    var sliderPosition = curValue

    val haptics = LocalHapticFeedback.current

    Card (modifier = modifier
        .padding(10.dp)
        .fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)) {

        Row (modifier = Modifier
            .fillMaxWidth()
            .height(240.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(0.75f)) {
                Text(
                    text = "Set Auto-Delete Time in Days\nNotifications and Summaries older than this will be deleted automatically.",
                    modifier = Modifier.padding(10.dp),
                    textAlign = TextAlign.Center,
                )

                Text(
                    text = sliderPosition.toInt().toString(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 120.sp
                )
            }

            Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(0.25f)) {
                VerticalSlider(
                    value = sliderPosition,
                    onValueChange = { newValue ->
                        updateAutoDelete(newValue.roundToInt())
                        val oldIntValue = sliderPosition.roundToInt()
                        val newIntValue = newValue.roundToInt()
                        sliderPosition = newValue
                        if (newIntValue != oldIntValue) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    },
                    valueRange = 1f..30f,
                    steps = 28,
                    modifier = Modifier.width(220.dp)
                )
            }
        }
    }
}

// VerticalSlider
@Composable
fun VerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: SliderColors = SliderDefaults.colors()
){
    Slider(
        colors = colors,
        interactionSource = interactionSource,
        onValueChangeFinished = onValueChangeFinished,
        steps = steps,
        valueRange = valueRange,
        enabled = enabled,
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .graphicsLayer {
                rotationZ = 270f
                transformOrigin = TransformOrigin(0f, 0f)
            }
            .layout { measurable, constraints ->
                val placeable = measurable.measure(
                    Constraints(
                        minWidth = constraints.minHeight,
                        maxWidth = constraints.maxHeight,
                        minHeight = constraints.minWidth,
                        maxHeight = constraints.maxHeight,
                    )
                )
                layout(placeable.height, placeable.width) {
                    placeable.place(-placeable.width, 0)
                }
            }
            .then(modifier)
    )
}


/*
*
* Below is the screen for setting up the text for smart-categorization
*
*  */

@Composable
fun SmartCategorizationScreen(modifier: Modifier = Modifier, goBack: () -> Unit, text: String, updateText: (String) -> Unit){
    Column (horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Top, modifier = Modifier.fillMaxSize() ) {

        var textLocal by remember { mutableStateOf(text) }

        OutlinedTextField(
            value = textLocal,
            onValueChange = { textLocal = it },
            label = { Text("Enter Intent for filtering")},
            placeholder = { Text("eg. 'Don't let any apps through except Clash of Clans', 'Do not let Facebook through, rest are fine.' ") },
            modifier = modifier
                .padding(10.dp)
                .fillMaxWidth()
        )

        Spacer(modifier = modifier.weight(0.8f))

        Button(onClick = {
            updateText(textLocal)
            goBack()}, modifier = modifier.padding(10.dp)) {
            Text(text = "Done")
        }
    }
}

