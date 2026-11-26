package com.vex.apksigner

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var selectedZip by remember { mutableStateOf<Uri?>(null) }
                var buildType by remember { mutableStateOf("APK + AAB") }
                var status by remember { mutableStateOf("Select an Android project ZIP to get started.") }
                val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                    selectedZip = uri
                    status = if (uri != null) "ZIP selected. Cloud Build service is not connected yet." else "No ZIP selected."
                }
                Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("VEX APK Signer", style = MaterialTheme.typography.headlineMedium)
                    Text("Build signed Android APK and AAB files from a Gradle project ZIP.")
                    Button(onClick = { picker.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")) }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (selectedZip == null) "Choose Project ZIP" else "Choose Another ZIP")
                    }
                    Text(if (selectedZip == null) "No project selected" else "Project ZIP selected")
                    Text("Output format", style = MaterialTheme.typography.titleMedium)
                    listOf("APK", "AAB", "APK + AAB").forEach { option ->
                        Row {
                            RadioButton(selected = buildType == option, onClick = { buildType = option })
                            Text(option, modifier = Modifier.padding(top = 12.dp))
                        }
                    }
                    Button(onClick = { status = "Cloud Build Runner is not configured. Build is unavailable until a secure backend is deployed." }, enabled = selectedZip != null, modifier = Modifier.fillMaxWidth()) {
                        Text("Start Build")
                    }
                    Text(status, style = MaterialTheme.typography.bodyMedium)
                    Text("Initial support: Android Studio Java/Kotlin Gradle projects. Flutter and other project types need separate build support.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
