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
import androidx.datastore.preferences.core.edit
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
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

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

        val displayTemperature = findViewById<CheckBox>(R.id.temperatureDisplayCheckBox).isChecked
        val displayRain = findViewById<CheckBox>(R.id.rainDisplayCheckBox).isChecked
        //val workerDays = findViewById<EditText>(R.id.workerDaysEdit).text.toString().toInt()
        //val workerHours = findViewById<EditText>(R.id.workerHoursEdit).text.toString().toInt()
        val temperatureNotification = findViewById<CheckBox>(R.id.temperatureDropTrackCheckBox).isChecked
        val rainNotification = findViewById<CheckBox>(R.id.rainTrackCheckBox).isChecked
        //val rainDays = findViewById<EditText>(R.id.rainNotificationDaysEdit).text.toString().toInt()
        //val rainHours = findViewById<EditText>(R.id.rainNotificationHoursEdit).text.toString().toInt()

        CoroutineScope(Dispatchers.IO).launch {
            dataStore.edit { userData ->
                userData[DataStoreKeys.DISPLAY_TEMPERATURE] = displayTemperature
                userData[DataStoreKeys.DISPLAY_RAIN] = displayRain
                //userData[DataStoreKeys.WORKER_DAYS] = workerDays
                //userData[DataStoreKeys.WORKER_HOURS] = workerHours
                userData[DataStoreKeys.TEMPERATURE_DROP_NOTIFICATIONS] = temperatureNotification
                userData[DataStoreKeys.RAIN_NOTIFICATIONS] = rainNotification
                //userData[DataStoreKeys.RAIN_DAYS] = rainDays
                //userData[DataStoreKeys.RAIN_HOURS] = rainHours
            }
        }
    }

    suspend fun extractSettings(){
        dataStore.data.collect { userData ->
            val notificationsEnabled = userData[DataStoreKeys.NOTIFICATIONS_ENABLED] ?: false
            val temperatureDisplay = userData[DataStoreKeys.DISPLAY_TEMPERATURE] ?: false
            val temperatureDropNotifications = userData[DataStoreKeys.TEMPERATURE_DROP_NOTIFICATIONS] ?: false
            val rainDisplay = userData[DataStoreKeys.DISPLAY_RAIN] ?: false
            val rainNotifications = userData[DataStoreKeys.RAIN_NOTIFICATIONS] ?: false
            runOnUiThread {
                findViewById<CheckBox>(R.id.notificationPermissionCheckBox).isChecked = notificationsEnabled
                findViewById<CheckBox>(R.id.temperatureDisplayCheckBox).isChecked = temperatureDisplay
                findViewById<CheckBox>(R.id.temperatureDropTrackCheckBox).isChecked = temperatureDropNotifications
                findViewById<CheckBox>(R.id.rainDisplayCheckBox).isChecked = rainDisplay
                findViewById<CheckBox>(R.id.rainTrackCheckBox).isChecked = rainNotifications
            }
        }
    }

    private suspend fun editNotificationsEnabled(value: Boolean) {
        dataStore.edit { userData ->
            userData[DataStoreKeys.NOTIFICATIONS_ENABLED] = value

            runOnUiThread {
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
    }

    private fun scheduleWeatherCheck(context: Context){
        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("getForecast", ExistingPeriodicWorkPolicy.REPLACE,workRequest)
    }
}