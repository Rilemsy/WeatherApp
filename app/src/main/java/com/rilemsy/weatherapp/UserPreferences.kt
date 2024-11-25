package com.rilemsy.weatherapp

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

private const val USER_PREFERENCES_NAME = "userPreferences"

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = USER_PREFERENCES_NAME
)

object DataStoreKeys {
    val DISPLAY_TEMPERATURE = booleanPreferencesKey("display_temperature")
    val DISPLAY_RAIN = booleanPreferencesKey("display_rain")
    val LATITUDE = doublePreferencesKey("latitude")
    val LONGITUDE = doublePreferencesKey("longitude")

    val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    val TEMPERATURE_DROP_NOTIFICATIONS = booleanPreferencesKey("temperature_drop_notifications")
    val RAIN_NOTIFICATIONS = booleanPreferencesKey("rain_notifications")

    val RAIN_DAYS = intPreferencesKey("rain_days")
    val RAIN_HOURS = intPreferencesKey("rain_hours")
    val RAIN_MM = doublePreferencesKey("rain_mm")
    val TEMPERATURE_DROP_DAYS = intPreferencesKey("temperature_drop_days")
    val TEMPERATURE_DROP_VALUE = intPreferencesKey("temperature_drop_value")

    val RAIN_NOTIFICATION_SENT = booleanPreferencesKey("rain_notification_sent")
    val TEMPERATURE_DROP_NOTIFICATION_SENT = booleanPreferencesKey("temperature_drop_notification_sent")
    val NOTIFICATION_EVENT_TIME_RAIN = stringPreferencesKey("notification_event_time_rain")
    val NOTIFICATION_EVENT_TIME_TEMPERATURE_DROP = stringPreferencesKey("notification_event_time_temperature_drop")

}