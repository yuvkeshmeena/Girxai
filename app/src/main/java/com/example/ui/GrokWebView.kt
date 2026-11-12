package com.example.ui

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.PermissionRequest
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.flow.SharedFlow

private const val DESKTOP_USER_AGENT =
  "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GrokWebView(
  initialUrl: String,
  isDesktopMode: Boolean,
  webActions: SharedFlow<WebAction>,
  onProgressChanged: (Int) -> Unit,
  onPageStarted: (String?) -> Unit,
  onPageFinished: (String?) -> Unit,
  onReceivedTitle: (String?) -> Unit,
  onReceivedError: (String) -> Unit,
  onCanGoBackChanged: (Boolean) -> Unit,
  onRequestAudioPermission: (PermissionRequest) -> Unit,
  onOpenFileChooser: (ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val webView = remember {
    WebView(context).apply {
      setBackgroundColor(android.graphics.Color.parseColor("#080B10"))
    }
  }

  // Handle actions sent from ViewModel (Reload, Back, LoadUrl, ClearCache)
  LaunchedEffect(webActions) {
    webActions.collect { action ->
      when (action) {
        is WebAction.Reload -> webView.reload()
        is WebAction.GoBack -> {
          if (webView.canGoBack()) {
            webView.goBack()
          }
        }
        is WebAction.LoadUrl -> webView.loadUrl(action.url)
        is WebAction.ClearCache -> {
          webView.clearCache(true)
          CookieManager.getInstance().removeAllCookies(null)
          CookieManager.getInstance().flush()
          Toast.makeText(context, "Cache & Cookies Cleared", Toast.LENGTH_SHORT).show()
        }
      }
    }
  }

  // Configure User-Agent when desktop mode changes
  LaunchedEffect(isDesktopMode) {
    val settings = webView.settings
    if (isDesktopMode) {
      settings.userAgentString = DESKTOP_USER_AGENT
      settings.useWideViewPort = true
      settings.loadWithOverviewMode = true
    } else {
      settings.userAgentString = null // Reset to default mobile user-agent
      settings.useWideViewPort = true
      settings.loadWithOverviewMode = true
    }
  }

  AndroidView(
    factory = {
      webView.apply {
        settings.apply {
          javaScriptEnabled = true
          domStorageEnabled = true
          allowFileAccess = true
          allowContentAccess = true
          mediaPlaybackRequiresUserGesture = false
          builtInZoomControls = true
          displayZoomControls = false
          setSupportZoom(true)
          cacheMode = WebSettings.LOAD_DEFAULT
          mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
          loadsImagesAutomatically = true
          setGeolocationEnabled(true)
        }

        // Enable third-party cookies for seamless auth & cross-origin AI logic
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

        webChromeClient = object : WebChromeClient() {
          override fun onProgressChanged(view: WebView?, newProgress: Int) {
            onProgressChanged(newProgress)
            onCanGoBackChanged(view?.canGoBack() == true)
          }

          override fun onReceivedTitle(view: WebView?, title: String?) {
            onReceivedTitle(title)
          }

          override fun onPermissionRequest(request: PermissionRequest?) {
            if (request == null) return
            onRequestAudioPermission(request)
          }

          override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?,
          ): Boolean {
            onOpenFileChooser(filePathCallback, fileChooserParams)
            return true
          }
        }

        webViewClient = object : WebViewClient() {
          override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?,
          ): Boolean {
            val uri = request?.url ?: return false
            val scheme = uri.scheme?.lowercase()

            // Keep http & https in WebView
            if (scheme == "http" || scheme == "https") {
              return false
            }

            // Open intent:, mailto:, tel: externally
            try {
              val intent = Intent(Intent.ACTION_VIEW, uri)
              context.startActivity(intent)
            } catch (_: Exception) {
              // Ignore unsupported schemes gracefully
            }
            return true
          }

          override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
            super.onPageStarted(view, url, favicon)
            onPageStarted(url)
            onCanGoBackChanged(view?.canGoBack() == true)
          }

          override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            onPageFinished(url)
            onCanGoBackChanged(view?.canGoBack() == true)
          }

          override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?,
          ) {
            super.onReceivedError(view, request, error)
            if (request?.isForMainFrame == true) {
              val desc = error?.description?.toString() ?: "Failed to load page"
              onReceivedError(desc)
            }
          }
        }

        // Handle image / file downloads
        setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
          try {
            val filename = URLUtil.guessFileName(url, contentDisposition, mimetype)
            val request = DownloadManager.Request(Uri.parse(url)).apply {
              setMimeType(mimetype)
              val cookies = CookieManager.getInstance().getCookie(url)
              addRequestHeader("cookie", cookies)
              addRequestHeader("User-Agent", userAgent)
              setDescription("Downloading $filename from Grok")
              setTitle(filename)
              setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
              setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
            }
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            Toast.makeText(context, "Downloading $filename", Toast.LENGTH_SHORT).show()
          } catch (e: Exception) {
            Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
          }
        })

        loadUrl(initialUrl)
      }
    },
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF080B10))
  )

  DisposableEffect(Unit) {
    onDispose {
      webView.stopLoading()
      webView.destroy()
    }
  }
}
