package com.rilemsy.weatherapp

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

private const val USER_PREFERENCES_NAME = "userPreferences"

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = USER_PREFERENCES_NAME
)

object DataStoreKeys {
    val USER_NAME = stringPreferencesKey("user_name")
    val EMAIL = stringPreferencesKey("email")
    val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
}