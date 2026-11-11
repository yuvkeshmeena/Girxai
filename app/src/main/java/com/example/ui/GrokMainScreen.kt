package com.example.ui

import android.content.Intent
import android.net.Uri
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.WebConfigRepository
import com.example.ui.theme.GrokAccentCyan
import com.example.ui.theme.GrokAccentGlow
import com.example.ui.theme.GrokAccentPurple
import com.example.ui.theme.GrokBlack
import com.example.ui.theme.GrokBorder
import com.example.ui.theme.GrokCardSurface
import com.example.ui.theme.GrokDarkSurface
import com.example.ui.theme.GrokTextPrimary
import com.example.ui.theme.GrokTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrokMainScreen(
  viewModel: GrokViewModel,
  onRequestAudioPermission: (PermissionRequest) -> Unit,
  onOpenFileChooser: (ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val currentUrl by viewModel.currentUrl.collectAsState()
  val pageTitle by viewModel.pageTitle.collectAsState()
  val loadingProgress by viewModel.loadingProgress.collectAsState()
  val isLoading by viewModel.isLoading.collectAsState()
  val hasError by viewModel.hasError.collectAsState()
  val errorMessage by viewModel.errorMessage.collectAsState()
  val isDesktopMode by viewModel.isDesktopMode.collectAsState()
  val isSettingsDialogOpen by viewModel.isSettingsDialogOpen.collectAsState()

  var showMenu by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      Column(modifier = Modifier.fillMaxWidth().background(GrokDarkSurface)) {
        TopAppBar(
          title = {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              // Glowing X emblem badge
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(
                    Brush.radialGradient(
                      colors = listOf(GrokAccentCyan.copy(alpha = 0.4f), Color(0xFF0F172A))
                    )
                  )
                  .border(1.dp, GrokAccentCyan.copy(alpha = 0.7f), CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "𝕏",
                  color = GrokAccentCyan,
                  fontSize = 18.sp,
                  fontWeight = FontWeight.Bold
                )
              }

              Column {
                Text(
                  text = "Grok AI",
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = GrokTextPrimary
                  )
                )
                Text(
                  text = Uri.parse(currentUrl).host ?: currentUrl,
                  style = MaterialTheme.typography.bodySmall.copy(
                    color = GrokAccentCyan,
                    fontSize = 11.sp
                  ),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          },
          actions = {
            // Quick Refresh Button
            IconButton(
              onClick = { viewModel.reload() },
              modifier = Modifier.testTag("refresh_button")
            ) {
              Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Reload Page",
                tint = GrokTextPrimary
              )
            }

            // Quick Configure / Space URL
            IconButton(
              onClick = { viewModel.openSettingsDialog() },
              modifier = Modifier.testTag("settings_button")
            ) {
              Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = "Configure URL / Space",
                tint = GrokAccentCyan
              )
            }

            // Dropdown Menu
            Box {
              IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.testTag("more_menu_button")
              ) {
                Icon(
                  imageVector = Icons.Default.MoreVert,
                  contentDescription = "More Options",
                  tint = GrokTextPrimary
                )
              }

              DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(GrokCardSurface)
              ) {
                DropdownMenuItem(
                  text = {
                    Text(
                      text = if (isDesktopMode) "Mobile Mode" else "Desktop Mode",
                      color = GrokTextPrimary
                    )
                  },
                  leadingIcon = {
                    Icon(
                      imageVector = if (isDesktopMode) Icons.Default.PhoneAndroid else Icons.Default.DesktopWindows,
                      contentDescription = null,
                      tint = GrokAccentCyan
                    )
                  },
                  onClick = {
                    showMenu = false
                    viewModel.toggleDesktopMode()
                  }
                )

                DropdownMenuItem(
                  text = { Text("Share Link", color = GrokTextPrimary) },
                  leadingIcon = {
                    Icon(
                      imageVector = Icons.Default.Share,
                      contentDescription = null,
                      tint = GrokAccentCyan
                    )
                  },
                  onClick = {
                    showMenu = false
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                      type = "text/plain"
                      putExtra(Intent.EXTRA_SUBJECT, pageTitle)
                      putExtra(Intent.EXTRA_TEXT, currentUrl)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Grok URL"))
                  }
                )

                DropdownMenuItem(
                  text = { Text("Open in External Browser", color = GrokTextPrimary) },
                  leadingIcon = {
                    Icon(
                      imageVector = Icons.Default.OpenInBrowser,
                      contentDescription = null,
                      tint = GrokAccentCyan
                    )
                  },
                  onClick = {
                    showMenu = false
                    try {
                      val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl))
                      context.startActivity(browserIntent)
                    } catch (_: Exception) {}
                  }
                )

                DropdownMenuItem(
                  text = { Text("Clear Cache & Cookies", color = GrokTextPrimary) },
                  leadingIcon = {
                    Icon(
                      imageVector = Icons.Default.Close,
                      contentDescription = null,
                      tint = Color(0xFFEF4444)
                    )
                  },
                  onClick = {
                    showMenu = false
                    viewModel.clearCache()
                  }
                )
              }
            }
          },
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = GrokDarkSurface,
            titleContentColor = GrokTextPrimary
          )
        )

        // Sleek cyan loading indicator
        AnimatedVisibility(
          visible = isLoading,
          enter = fadeIn(),
          exit = fadeOut()
        ) {
          LinearProgressIndicator(
            progress = { loadingProgress / 100f },
            modifier = Modifier
              .fillMaxWidth()
              .height(3.dp),
            color = GrokAccentCyan,
            trackColor = Color(0xFF1E293B)
          )
        }
      }
    },
    containerColor = GrokBlack,
    modifier = modifier.fillMaxSize()
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .background(GrokBlack)
    ) {
      if (hasError) {
        // Modern Error Screen
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = GrokCardSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, GrokBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier.padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(64.dp)
                  .clip(CircleShape)
                  .background(Color(0xFF331B24)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.SignalWifiOff,
                  contentDescription = null,
                  tint = Color(0xFFF87171),
                  modifier = Modifier.size(32.dp)
                )
              }

              Text(
                text = "Connection Failed",
                style = MaterialTheme.typography.titleLarge.copy(
                  fontWeight = FontWeight.Bold,
                  color = GrokTextPrimary
                )
              )

              Text(
                text = errorMessage ?: "Unable to load $currentUrl. Please check your internet connection.",
                style = MaterialTheme.typography.bodyMedium.copy(
                  color = GrokTextSecondary
                ),
                lineHeight = 20.sp
              )

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                OutlinedButton(
                  onClick = { viewModel.openSettingsDialog() },
                  modifier = Modifier.weight(1f).testTag("error_change_url_button"),
                  border = androidx.compose.foundation.BorderStroke(1.dp, GrokAccentCyan)
                ) {
                  Text("Change URL", color = GrokAccentCyan)
                }

                Button(
                  onClick = { viewModel.reload() },
                  modifier = Modifier.weight(1f).testTag("retry_button"),
                  colors = ButtonDefaults.buttonColors(containerColor = GrokAccentCyan)
                ) {
                  Text("Try Again", color = Color.Black, fontWeight = FontWeight.SemiBold)
                }
              }
            }
          }
        }
      } else {
        // Active WebView
        GrokWebView(
          initialUrl = currentUrl,
          isDesktopMode = isDesktopMode,
          webActions = viewModel.webAction,
          onProgressChanged = { viewModel.onProgressChanged(it) },
          onPageStarted = { viewModel.onPageStarted(it) },
          onPageFinished = { viewModel.onPageFinished(it) },
          onReceivedTitle = { viewModel.onReceivedTitle(it) },
          onReceivedError = { viewModel.onReceivedError(it) },
          onCanGoBackChanged = { viewModel.updateCanGoBack(it) },
          onRequestAudioPermission = onRequestAudioPermission,
          onOpenFileChooser = onOpenFileChooser,
          modifier = Modifier.fillMaxSize().testTag("grok_web_view")
        )
      }
    }
  }

  // URL / Space Settings Dialog
  if (isSettingsDialogOpen) {
    var urlInput by remember { mutableStateOf(currentUrl) }

    AlertDialog(
      onDismissRequest = { viewModel.dismissSettingsDialog() },
      containerColor = GrokCardSurface,
      titleContentColor = GrokTextPrimary,
      title = {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(Icons.Default.Language, contentDescription = null, tint = GrokAccentCyan)
          Text("Target Website / Space", fontWeight = FontWeight.Bold)
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text(
            text = "Enter your Grok Space, HF Space, or web app URL:",
            color = GrokTextSecondary,
            fontSize = 13.sp
          )

          OutlinedTextField(
            value = urlInput,
            onValueChange = { urlInput = it },
            label = { Text("Website URL") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("url_input_field"),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = GrokAccentCyan,
              unfocusedBorderColor = GrokBorder,
              focusedTextColor = GrokTextPrimary,
              unfocusedTextColor = GrokTextPrimary,
              focusedLabelColor = GrokAccentCyan,
              unfocusedLabelColor = GrokTextSecondary
            )
          )

          Text(
            text = "Presets:",
            color = GrokTextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
          )

          WebConfigRepository.PRESET_URLS.forEach { (presetUrl, name) ->
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (urlInput == presetUrl) Color(0xFF0C4A6E) else Color(0xFF0F172A),
              border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (urlInput == presetUrl) GrokAccentCyan else GrokBorder
              ),
              modifier = Modifier
                .fillMaxWidth()
                .clickable { urlInput = presetUrl }
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column {
                  Text(text = name, color = GrokTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                  Text(text = presetUrl, color = GrokTextSecondary, fontSize = 11.sp)
                }
                if (urlInput == presetUrl) {
                  Icon(Icons.Default.Check, contentDescription = null, tint = GrokAccentCyan, modifier = Modifier.size(18.dp))
                }
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = { viewModel.saveCustomUrl(urlInput) },
          colors = ButtonDefaults.buttonColors(containerColor = GrokAccentCyan),
          modifier = Modifier.testTag("save_url_button")
        ) {
          Text("Save & Open", color = Color.Black, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          TextButton(onClick = { viewModel.resetToDefaultUrl() }) {
            Text("Reset", color = GrokTextSecondary)
          }
          TextButton(onClick = { viewModel.dismissSettingsDialog() }) {
            Text("Cancel", color = GrokTextSecondary)
          }
        }
      }
    )
  }
}
