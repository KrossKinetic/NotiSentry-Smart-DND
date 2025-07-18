package com.krosskinetic.notisentry.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun IntroScreenWelcome(onGetStartedClicked: () -> Unit, modifier: Modifier = Modifier){
    Column(modifier = modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(
            text = "Take Back Your Focus",
            fontSize = MaterialTheme.typography.titleLarge.fontSize,
        )
        Spacer(modifier = Modifier.padding(10.dp))
        Text(
            text = "Welcome! Get ready to silence the noise and manage your notifications intelligently.",
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.padding(10.dp))
        Button(
            onClick = { onGetStartedClicked() },
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "Get Started"
            )
        }
    }
}

@Composable
fun IntroScreenPermission(isGranted: Boolean, onPermissionClicked: () -> Unit, modifier: Modifier = Modifier){

    val context = LocalContext.current

    Column(modifier = modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(
            text = "Enable the Magic \uD83E\uDE84",
            fontSize = MaterialTheme.typography.titleLarge.fontSize,
        )
        Spacer(modifier = Modifier.padding(10.dp))
        Text(
            text = "To intelligently filter notifications and create summaries, the app needs \"Notification Access\".\n" +
                    "\n" +
                    "This permission allows the app to see and manage incoming alerts. We take your privacy seriously—all processing happens securely on your phone and never leaves it. \n\n" +
                    "Once the permission is granted, a button will appear below to proceed.",
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.padding(10.dp))
        Row {
            if (isGranted){
                Button(
                    onClick = { onPermissionClicked() },
                    modifier = Modifier.padding(10.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Continue"
                    )
                }
            } else {
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.padding(10.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Go to Settings"
                    )
                }
            }
        }

    }
}

@Composable
fun IntroScreenAllDone(onGetAllDoneClicked: () -> Unit, updateIntroDone: () -> Unit, modifier: Modifier = Modifier){
    Column(modifier = modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(
            imageVector = Icons.TwoTone.CheckCircle,
            contentDescription = "Check Circle"
        )
        Spacer(modifier = Modifier.padding(10.dp))
        Text(
            text = "Permission Granted!",
            fontSize = MaterialTheme.typography.titleLarge.fontSize,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.padding(10.dp))
        Text(
            text = " You're all set to create a more focused and peaceful notification experience.",
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.padding(10.dp))
        Button(
            onClick = {
                onGetAllDoneClicked()
                updateIntroDone()
            },
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "Start Filtering Notifications"
            )
        }
    }
}