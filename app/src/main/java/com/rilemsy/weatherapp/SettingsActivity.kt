package com.rilemsy.weatherapp

import NotificationWorker
import android.content.Context
import android.os.Bundle
import android.widget.CheckBox
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


    suspend fun extractSettings(){
        dataStore.data.collect { userData ->
            val notificationsEnabled = userData[DataStoreKeys.NOTIFICATIONS_ENABLED] ?: false
            val temperatureDisplay = userData[DataStoreKeys.DISPLAY_TEMPERATURE] ?: false
            val temperatureDropTrack = userData[DataStoreKeys.TRACK_TEMPERATURE_DROP] ?: false
            val rainDisplay = userData[DataStoreKeys.DISPLAY_RAIN] ?: false
            val rainTrack = userData[DataStoreKeys.TRACK_RAIN] ?: false
            runOnUiThread {
                findViewById<CheckBox>(R.id.notificationPermissionCheckBox).isChecked = notificationsEnabled
                findViewById<CheckBox>(R.id.temperatureDisplayCheckBox).isChecked = temperatureDisplay
                findViewById<CheckBox>(R.id.temperatureDropTrackCheckBox).isChecked = temperatureDropTrack
                findViewById<CheckBox>(R.id.rainDisplayCheckBox).isChecked = rainDisplay
                findViewById<CheckBox>(R.id.rainTrackCheckBox).isChecked = rainTrack
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
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("getForecast", ExistingPeriodicWorkPolicy.KEEP,workRequest)
    }
}