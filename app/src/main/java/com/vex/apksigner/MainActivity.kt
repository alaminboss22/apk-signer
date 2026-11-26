package com.vex.apksigner

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var zipUri by remember { mutableStateOf<Uri?>(null) }
                var zipUrl by remember { mutableStateOf("") }
                var token by remember { mutableStateOf("") }
                var format by remember { mutableStateOf("both") }
                var status by remember { mutableStateOf("Enter a direct HTTPS ZIP download link and a GitHub token with Actions: write permission.") }
                var busy by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()
                val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                    zipUri = uri
                    if (uri != null) status = "Local ZIP selected. For this free workflow, provide a direct HTTPS download URL for the same ZIP below."
                }

                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("VEX APK Signer", style = MaterialTheme.typography.headlineMedium)
                    Text("Free build using GitHub Actions. The workflow runs in GitHub's cloud runner.")
                    Button(onClick = { picker.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")) }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (zipUri == null) "Choose Project ZIP (optional)" else "Choose Another ZIP")
                    }
                    Text(if (zipUri == null) "No local ZIP selected" else "A local ZIP is selected. It is not uploaded automatically.")
                    OutlinedTextField(
                        value = zipUrl, onValueChange = { zipUrl = it },
                        label = { Text("Direct HTTPS ZIP download URL") },
                        placeholder = { Text("https://.../project.zip") },
                        modifier = Modifier.fillMaxWidth(), singleLine = false
                    )
                    Text("Output format", style = MaterialTheme.typography.titleMedium)
                    listOf("apk" to "APK", "aab" to "AAB", "both" to "APK + AAB").forEach { item ->
                        Row {
                            RadioButton(selected = format == item.first, onClick = { format = item.first })
                            Text(item.second, modifier = Modifier.padding(top = 12.dp))
                        }
                    }
                    OutlinedTextField(
                        value = token, onValueChange = { token = it },
                        label = { Text("GitHub Fine-grained Personal Access Token") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    Text("Create a token limited to this repository with Actions: Read and write. The token is used only for this request and is not saved by the app.", style = MaterialTheme.typography.bodySmall)
                    Button(
                        enabled = !busy && zipUrl.startsWith("https://") && token.isNotBlank(),
                        onClick = {
                            busy = true
                            status = "Sending build request to GitHub..."
                            scope.launch {
                                val result = withContext(Dispatchers.IO) { dispatchWorkflow(zipUrl.trim(), token.trim(), format) }
                                busy = false
                                status = result
                                if (result.startsWith("Build request accepted")) {
                                    token = ""
                                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/alaminboss22/apk-signer/actions/workflows/build-project-zip.yml")))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (busy) "Starting..." else "Start Cloud Build") }
                    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(status, style = MaterialTheme.typography.bodyMedium)
                    Text("After the request is accepted, GitHub Actions runs the build. Open the Actions page, wait for completion, then download the VEX-Android-Project-Outputs artifact.", style = MaterialTheme.typography.bodySmall)
                    Text("Important: the ZIP URL must be a direct HTTPS link accessible without login. The workflow currently creates unsigned release APK/AAB files. Signing and in-app artifact download are not implemented yet.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }

    private fun dispatchWorkflow(zipUrl: String, token: String, format: String): String {
        return try {
            val uri = Uri.parse(zipUrl)
            if (uri.scheme != "https" || uri.host.isNullOrBlank()) return "Please enter a valid direct HTTPS ZIP URL."
            val connection = (URL("https://api.github.com/repos/alaminboss22/apk-signer/actions/workflows/build-project-zip.yml/dispatches").openConnection() as HttpURLConnection)
            connection.requestMethod = "POST"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.doOutput = true
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Content-Type", "application/json")
            val body = JSONObject().put("ref", "main").put(
                "inputs", JSONObject().put("project_zip_url", zipUrl).put("output_format", format)
            ).toString()
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            connection.disconnect()
            when (code) {
                204 -> "Build request accepted by GitHub. Opening Actions page..."
                401, 403 -> "GitHub rejected the token. Check that it belongs to this repository and has Actions: Read and write permission."
                404 -> "Workflow or repository not found. Confirm the repository and workflow are public and the workflow exists on main."
                else -> "GitHub returned HTTP $code. Check token permissions and try again."
            }
        } catch (e: Exception) {
            "Could not contact GitHub: ${e.message ?: "network error"}"
        }
    }
}
