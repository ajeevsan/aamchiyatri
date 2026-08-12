package com.amchiyatri.rider.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amchiyatri.rider.data.local.UserPreferences
import com.amchiyatri.rider.data.model.AppLanguage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
) : ViewModel() {

    // Eagerly, not WhileSubscribed: SplashScreen reads `.value` once without ever collecting,
    // so the DataStore-backed flow must start loading immediately or `.value` would never
    // advance past its default.
    val language: StateFlow<AppLanguage> = userPreferences.language
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppLanguage.ENGLISH)

    val hasChosenLanguage: StateFlow<Boolean> = userPreferences.hasChosenLanguage
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { userPreferences.setLanguage(language) }
    }
}
