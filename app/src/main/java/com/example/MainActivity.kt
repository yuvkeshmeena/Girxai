package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.ui.GrokMainScreen
import com.example.ui.GrokViewModel
import com.example.ui.theme.GrokAppTheme

class MainActivity : ComponentActivity() {
  private val viewModel: GrokViewModel by viewModels()

  // Track pending permission request and file chooser callback
  private var pendingPermissionRequest: PermissionRequest? = null
  private var pendingFilePathCallback: ValueCallback<Array<Uri>>? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      GrokAppTheme {
        val canGoBack by viewModel.canGoBack.collectAsState()

        // Handle Back navigation inside WebView
        BackHandler(enabled = canGoBack) {
          viewModel.goBack()
        }

        // Launcher for audio microphone permission (required for Grok Voice Chat)
        val micPermissionLauncher = rememberLauncherForActivityResult(
          contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
          val req = pendingPermissionRequest
          if (req != null) {
            if (isGranted) {
              req.grant(req.resources)
            } else {
              req.deny()
              Toast.makeText(this, "Microphone permission is required for Voice Mode", Toast.LENGTH_SHORT).show()
            }
            pendingPermissionRequest = null
          }
        }

        // Launcher for file/image chooser (required for image upload & generation prompts)
        val fileChooserLauncher = rememberLauncherForActivityResult(
          contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
          val callback = pendingFilePathCallback
          if (callback != null) {
            val dataIntent = result.data
            val results: Array<Uri>? = when {
              result.resultCode == RESULT_OK && dataIntent?.data != null -> {
                arrayOf(dataIntent.data!!)
              }
              result.resultCode == RESULT_OK && dataIntent?.clipData != null -> {
                val clip = dataIntent.clipData!!
                val list = ArrayList<Uri>()
                for (i in 0 until clip.itemCount) {
                  list.add(clip.getItemAt(i).uri)
                }
                list.toTypedArray()
              }
              else -> null
            }
            callback.onReceiveValue(results)
            pendingFilePathCallback = null
          }
        }

        GrokMainScreen(
          viewModel = viewModel,
          onRequestAudioPermission = { request ->
            pendingPermissionRequest = request
            val audioResource = PermissionRequest.RESOURCE_AUDIO_CAPTURE
            val hasAudioReq = request.resources.contains(audioResource)

            if (hasAudioReq) {
              val isAudioPermitted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
              ) == PackageManager.PERMISSION_GRANTED

              if (isAudioPermitted) {
                request.grant(request.resources)
                pendingPermissionRequest = null
              } else {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
              }
            } else {
              request.grant(request.resources)
              pendingPermissionRequest = null
            }
          },
          onOpenFileChooser = { filePathCallback, fileChooserParams ->
            pendingFilePathCallback?.onReceiveValue(null)
            pendingFilePathCallback = filePathCallback

            val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
              type = "*/*"
              addCategory(Intent.CATEGORY_OPENABLE)
            }

            try {
              fileChooserLauncher.launch(intent)
            } catch (e: Exception) {
              val fallbackIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
              }
              fileChooserLauncher.launch(fallbackIntent)
            }
          },
          modifier = Modifier.fillMaxSize()
        )
      }
    }
  }
}
