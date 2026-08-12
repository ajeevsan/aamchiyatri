package com.amchiyatri.rider.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.amchiyatri.rider.data.model.AppLanguage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "amchi_yatri_prefs")

/** Small on-device settings that should survive app restarts: language and onboarding state. */
@Singleton
class UserPreferences @Inject constructor(@ApplicationContext private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language_code")
        val HAS_ONBOARDED = stringPreferencesKey("has_onboarded")
    }

    val language = context.dataStore.data.map { prefs ->
        AppLanguage.entries.firstOrNull { it.code == prefs[Keys.LANGUAGE] } ?: AppLanguage.ENGLISH
    }

    val hasChosenLanguage = context.dataStore.data.map { prefs -> prefs[Keys.HAS_ONBOARDED] == "true" }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LANGUAGE] = language.code
            prefs[Keys.HAS_ONBOARDED] = "true"
        }
    }
}
