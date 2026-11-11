package com.example.data

import android.content.Context
import android.content.SharedPreferences

class WebConfigRepository(context: Context) {
  private val prefs: SharedPreferences =
    context.getSharedPreferences("grok_web_prefs", Context.MODE_PRIVATE)

  companion object {
    const val DEFAULT_URL = "https://x.ai"
    private const val KEY_CURRENT_URL = "key_current_url"
    private const val KEY_DESKTOP_MODE = "key_desktop_mode"

    val PRESET_URLS = listOf(
      "https://x.ai" to "xAI Official / Grok Space",
      "https://grok.com" to "Grok Chat Portal",
      "https://x.com/i/grok" to "Grok on X"
    )
  }

  fun getSavedUrl(): String {
    val url = prefs.getString(KEY_CURRENT_URL, null)
    return if (url.isNullOrBlank()) DEFAULT_URL else url
  }

  fun saveUrl(url: String) {
    val cleanUrl = sanitizeUrl(url)
    prefs.edit().putString(KEY_CURRENT_URL, cleanUrl).apply()
  }

  fun resetToDefault() {
    prefs.edit().putString(KEY_CURRENT_URL, DEFAULT_URL).apply()
  }

  fun isDesktopMode(): Boolean {
    return prefs.getBoolean(KEY_DESKTOP_MODE, false)
  }

  fun setDesktopMode(enabled: Boolean) {
    prefs.edit().putBoolean(KEY_DESKTOP_MODE, enabled).apply()
  }

  private fun sanitizeUrl(input: String): String {
    val trimmed = input.trim()
    return if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
      "https://$trimmed"
    } else {
      trimmed
    }
  }
}
