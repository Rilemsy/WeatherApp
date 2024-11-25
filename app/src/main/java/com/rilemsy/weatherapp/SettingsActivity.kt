package com.rilemsy.weatherapp

import NotificationWorker
import android.content.Context
import android.os.Bundle
import android.widget.CheckBox
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class SettingsActivity : AppCompatActivity() {

    private lateinit var database: ForecastDatabase
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        CoroutineScope(Dispatchers.IO).launch {
            extractSettings()
        }

        val chkBoxNotificationEnabled = findViewById<CheckBox>(R.id.notificationPermissionCheckBox)
        chkBoxNotificationEnabled.setOnClickListener{
            CoroutineScope(Dispatchers.IO).launch {
                editNotificationsEnabled(chkBoxNotificationEnabled.isChecked)
            }
        }
    }

    override fun onStop() {
        super.onStop()

        val notificationsEnabled = findViewById<CheckBox>(R.id.notificationPermissionCheckBox).isChecked
        val displayTemperature = findViewById<CheckBox>(R.id.temperatureDisplayCheckBox).isChecked
        val displayRain = findViewById<CheckBox>(R.id.rainDisplayCheckBox).isChecked
        val rainNotification = findViewById<CheckBox>(R.id.rainTrackCheckBox).isChecked
        val rainDays = findViewById<EditText>(R.id.rainNotificationDaysEdit).text.toString().toInt()
        val rainHours = findViewById<EditText>(R.id.rainNotificationHoursEdit).text.toString().toInt()
        val rainMM = findViewById<EditText>(R.id.rainMMEdit).text.toString().toDouble()
        val temperatureNotification = findViewById<CheckBox>(R.id.temperatureDropTrackCheckBox).isChecked
        val temperatureDropDays = findViewById<EditText>(R.id.temperatureDropDaysEdit).text.toString().toInt()
        val temperatureDropValue = findViewById<EditText>(R.id.temperatureDropValueEdit).text.toString().toInt()
        val latitude = findViewById<EditText>(R.id.latitudeEdit).text.toString().toDouble()
        val longitude = findViewById<EditText>(R.id.longitudeEdit).text.toString().toDouble()

        CoroutineScope(Dispatchers.IO).launch {
            dataStore.edit { userData ->
                userData[DataStoreKeys.NOTIFICATIONS_ENABLED] = notificationsEnabled
                userData[DataStoreKeys.DISPLAY_TEMPERATURE] = displayTemperature
                userData[DataStoreKeys.DISPLAY_RAIN] = displayRain
                userData[DataStoreKeys.RAIN_NOTIFICATIONS] = rainNotification
                userData[DataStoreKeys.RAIN_DAYS] = rainDays
                userData[DataStoreKeys.RAIN_HOURS] = rainHours
                userData[DataStoreKeys.RAIN_MM] = rainMM
                userData[DataStoreKeys.TEMPERATURE_DROP_NOTIFICATIONS] = temperatureNotification
                userData[DataStoreKeys.TEMPERATURE_DROP_DAYS] = temperatureDropDays
                userData[DataStoreKeys.TEMPERATURE_DROP_VALUE] = temperatureDropValue
                userData[DataStoreKeys.LATITUDE] = latitude
                userData[DataStoreKeys.LONGITUDE] = longitude
            }
        }
    }

    private suspend fun extractSettings(){
        dataStore.data.collect { userData ->
            val notificationsEnabled = userData[DataStoreKeys.NOTIFICATIONS_ENABLED] ?: false
            val temperatureDisplay = userData[DataStoreKeys.DISPLAY_TEMPERATURE] ?: false
            val rainDisplay = userData[DataStoreKeys.DISPLAY_RAIN] ?: false
            val rainNotifications = userData[DataStoreKeys.RAIN_NOTIFICATIONS] ?: false
            val rainDays = userData[DataStoreKeys.RAIN_DAYS] ?: 1
            val rainHours = userData[DataStoreKeys.RAIN_HOURS] ?: 0
            val rainMM = userData[DataStoreKeys.RAIN_MM] ?: 0.3
            val temperatureDropNotifications = userData[DataStoreKeys.TEMPERATURE_DROP_NOTIFICATIONS] ?: false
            val temperatureDropDays = userData[DataStoreKeys.TEMPERATURE_DROP_DAYS] ?: 1
            val temperatureDropValue =  userData[DataStoreKeys.TEMPERATURE_DROP_VALUE] ?: 5
            val latitude = userData[DataStoreKeys.LATITUDE] ?: 0.0
            val longitude = userData[DataStoreKeys.LONGITUDE] ?: 0.0

            runOnUiThread {
                findViewById<CheckBox>(R.id.notificationPermissionCheckBox).isChecked = notificationsEnabled
                findViewById<CheckBox>(R.id.temperatureDisplayCheckBox).isChecked = temperatureDisplay
                findViewById<CheckBox>(R.id.rainDisplayCheckBox).isChecked = rainDisplay
                findViewById<CheckBox>(R.id.rainTrackCheckBox).isChecked = rainNotifications
                findViewById<EditText>(R.id.rainNotificationDaysEdit).setText("$rainDays")
                findViewById<EditText>(R.id.rainNotificationHoursEdit).setText("$rainHours")
                findViewById<EditText>(R.id.rainMMEdit).setText("$rainMM")
                findViewById<CheckBox>(R.id.temperatureDropTrackCheckBox).isChecked = temperatureDropNotifications
                findViewById<EditText>(R.id.temperatureDropDaysEdit).setText("$temperatureDropDays")
                findViewById<EditText>(R.id.temperatureDropValueEdit).setText("$temperatureDropValue")
                findViewById<EditText>(R.id.latitudeEdit).setText("$latitude")
                findViewById<EditText>(R.id.longitudeEdit).setText("$longitude")
            }
        }
    }

    private suspend fun editNotificationsEnabled(value: Boolean) {
        dataStore.edit { userData ->
            userData[DataStoreKeys.NOTIFICATIONS_ENABLED] = value
            if (value)
            {
                scheduleWeatherCheck(this)
            }
            else
            {
                WorkManager.getInstance(this).cancelUniqueWork("getForecast")
            }
        }
    }

    private suspend fun scheduleWeatherCheck(context: Context){
        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("getForecast", ExistingPeriodicWorkPolicy.REPLACE,workRequest)
    }
}