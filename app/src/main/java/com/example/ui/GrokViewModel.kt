package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.WebConfigRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface WebAction {
  object Reload : WebAction
  object GoBack : WebAction
  data class LoadUrl(val url: String) : WebAction
  object ClearCache : WebAction
}

class GrokViewModel(application: Application) : AndroidViewModel(application) {
  private val repository = WebConfigRepository(application)

  private val _currentUrl = MutableStateFlow(repository.getSavedUrl())
  val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()

  private val _pageTitle = MutableStateFlow("Grok AI")
  val pageTitle: StateFlow<String> = _pageTitle.asStateFlow()

  private val _loadingProgress = MutableStateFlow(0)
  val loadingProgress: StateFlow<Int> = _loadingProgress.asStateFlow()

  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  private val _hasError = MutableStateFlow(false)
  val hasError: StateFlow<Boolean> = _hasError.asStateFlow()

  private val _errorMessage = MutableStateFlow<String?>(null)
  val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

  private val _isDesktopMode = MutableStateFlow(repository.isDesktopMode())
  val isDesktopMode: StateFlow<Boolean> = _isDesktopMode.asStateFlow()

  private val _canGoBack = MutableStateFlow(false)
  val canGoBack: StateFlow<Boolean> = _canGoBack.asStateFlow()

  private val _isSettingsDialogOpen = MutableStateFlow(false)
  val isSettingsDialogOpen: StateFlow<Boolean> = _isSettingsDialogOpen.asStateFlow()

  private val _webAction = MutableSharedFlow<WebAction>()
  val webAction: SharedFlow<WebAction> = _webAction.asSharedFlow()

  fun onProgressChanged(progress: Int) {
    _loadingProgress.value = progress
    _isLoading.value = progress in 1..99
    if (progress == 100) {
      _isLoading.value = false
    }
  }

  fun onPageStarted(url: String?) {
    _isLoading.value = true
    _hasError.value = false
    _errorMessage.value = null
    url?.let { _currentUrl.value = it }
  }

  fun onPageFinished(url: String?) {
    _isLoading.value = false
    url?.let { _currentUrl.value = it }
  }

  fun onReceivedTitle(title: String?) {
    if (!title.isNullOrBlank()) {
      _pageTitle.value = title
    }
  }

  fun onReceivedError(description: String) {
    _isLoading.value = false
    _hasError.value = true
    _errorMessage.value = description
  }

  fun updateCanGoBack(canGo: Boolean) {
    _canGoBack.value = canGo
  }

  fun reload() {
    viewModelScope.launch {
      _hasError.value = false
      _webAction.emit(WebAction.Reload)
    }
  }

  fun goBack() {
    viewModelScope.launch {
      _webAction.emit(WebAction.GoBack)
    }
  }

  fun saveCustomUrl(newUrl: String) {
    repository.saveUrl(newUrl)
    val saved = repository.getSavedUrl()
    _currentUrl.value = saved
    _isSettingsDialogOpen.value = false
    _hasError.value = false
    viewModelScope.launch {
      _webAction.emit(WebAction.LoadUrl(saved))
    }
  }

  fun resetToDefaultUrl() {
    repository.resetToDefault()
    val defaultUrl = repository.getSavedUrl()
    _currentUrl.value = defaultUrl
    _isSettingsDialogOpen.value = false
    _hasError.value = false
    viewModelScope.launch {
      _webAction.emit(WebAction.LoadUrl(defaultUrl))
    }
  }

  fun toggleDesktopMode() {
    val newMode = !_isDesktopMode.value
    _isDesktopMode.value = newMode
    repository.setDesktopMode(newMode)
    reload()
  }

  fun clearCache() {
    viewModelScope.launch {
      _webAction.emit(WebAction.ClearCache)
    }
  }

  fun openSettingsDialog() {
    _isSettingsDialogOpen.value = true
  }

  fun dismissSettingsDialog() {
    _isSettingsDialogOpen.value = false
  }
}
