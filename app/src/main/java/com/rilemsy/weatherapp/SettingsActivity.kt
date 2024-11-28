package com.rilemsy.weatherapp

import NotificationWorker
import android.R.attr
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit


class SettingsActivity : AppCompatActivity() {

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

        val latitudeEdit = findViewById<EditText>(R.id.latitudeEdit)
        latitudeEdit.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s?.toString()?.toDoubleOrNull() == null) return
                val content = s?.toString()?.toDouble()
                val value =
                    BigDecimal(content ?: 1000.0).setScale(3, RoundingMode.HALF_UP).toDouble()

                if (!(value >= -90.0 && value <= 90)) {
                    latitudeEdit.error = "Значение должно быть\nмежду -90 и 90"
                    return;
                }
                if (content.toString().length - content.toString().indexOf('.', 0) - 1 > 3) {
                    latitudeEdit.error = "Слишком длинный ввод"
                    return;
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        )

        val longitudeEdit = findViewById<EditText>(R.id.longitudeEdit)
        longitudeEdit.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s?.toString()?.toDoubleOrNull() == null) return
                val content = s.toString().toDouble()
                val value =
                    BigDecimal(content ?: 1000.0).setScale(3, RoundingMode.HALF_UP).toDouble()
                if (!(value >= -180.0 && value <= 180)) {
                    longitudeEdit.error = "Значение должно быть\nмежду -180 и 180"
                    return;
                }
                if (content.toString().length - content.toString().indexOf('.', 0) - 1 > 3) {
                    longitudeEdit.error = "Слишком длинный ввод"
                    return;
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        )

        findViewById<Button>(R.id.backButton).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(intent)
        }

        findViewById<Button>(R.id.saveButton).setOnClickListener {
            runBlocking {
                saveSettings()
            }
        }
    }

    private suspend fun extractSettings() {
        dataStore.data.collect { userData ->
            val notificationsEnabled = userData[DataStoreKeys.NOTIFICATIONS_ENABLED] ?: false
            val temperatureDisplay = userData[DataStoreKeys.DISPLAY_TEMPERATURE] ?: false
            val rainDisplay = userData[DataStoreKeys.DISPLAY_RAIN] ?: false
            val rainNotifications = userData[DataStoreKeys.RAIN_NOTIFICATIONS] ?: false
            val rainDays = userData[DataStoreKeys.RAIN_DAYS] ?: 1
            val rainHours = userData[DataStoreKeys.RAIN_HOURS] ?: 0
            val rainMM = userData[DataStoreKeys.RAIN_MM] ?: 1.0
            val temperatureDropNotifications =
                userData[DataStoreKeys.TEMPERATURE_DROP_NOTIFICATIONS] ?: false
            val temperatureDropDays = userData[DataStoreKeys.TEMPERATURE_DROP_DAYS] ?: 1
            val temperatureDropValue = userData[DataStoreKeys.TEMPERATURE_DROP_VALUE] ?: 5
            val latitude = userData[DataStoreKeys.LATITUDE] ?: 0.0
            val longitude = userData[DataStoreKeys.LONGITUDE] ?: 0.0

            runOnUiThread {
                findViewById<CheckBox>(R.id.notificationPermissionCheckBox).isChecked =
                    notificationsEnabled
                findViewById<CheckBox>(R.id.rainTrackCheckBox).isChecked = rainNotifications
                findViewById<EditText>(R.id.rainNotificationDaysEdit).setText("$rainDays")
                findViewById<EditText>(R.id.rainNotificationHoursEdit).setText("$rainHours")
                findViewById<EditText>(R.id.rainMMEdit).setText("$rainMM")
                findViewById<CheckBox>(R.id.temperatureDropTrackCheckBox).isChecked =
                    temperatureDropNotifications
                findViewById<EditText>(R.id.temperatureDropDaysEdit).setText("$temperatureDropDays")
                findViewById<EditText>(R.id.temperatureDropValueEdit).setText("$temperatureDropValue")
                findViewById<EditText>(R.id.latitudeEdit).setText("$latitude")
                findViewById<EditText>(R.id.longitudeEdit).setText("$longitude")
            }
        }
    }

    private suspend fun saveSettings() {
        val notificationsEnabled =
            findViewById<CheckBox>(R.id.notificationPermissionCheckBox).isChecked
        val rainNotification = findViewById<CheckBox>(R.id.rainTrackCheckBox).isChecked
        val rainDays =
            findViewById<EditText>(R.id.rainNotificationDaysEdit).text.toString().toIntOrNull() ?: 0
        val rainHours =
            findViewById<EditText>(R.id.rainNotificationHoursEdit).text.toString().toIntOrNull()
                ?: 0
        val rainMM = findViewById<EditText>(R.id.rainMMEdit).text.toString().toDoubleOrNull() ?: 1.0
        val temperatureNotification =
            findViewById<CheckBox>(R.id.temperatureDropTrackCheckBox).isChecked
        val temperatureDropDays =
            findViewById<EditText>(R.id.temperatureDropDaysEdit).text.toString().toIntOrNull() ?: 1
        val temperatureDropValue =
            findViewById<EditText>(R.id.temperatureDropValueEdit).text.toString().toIntOrNull() ?: 5
        val latitude =
            findViewById<EditText>(R.id.latitudeEdit).text?.toString()?.toDoubleOrNull() ?: 0.0
        val longitude =
            findViewById<EditText>(R.id.longitudeEdit).text?.toString()?.toDoubleOrNull() ?: 0.0

        runBlocking {
            CoroutineScope(Dispatchers.IO).launch {
                dataStore.edit { userData ->
                    userData[DataStoreKeys.NOTIFICATIONS_ENABLED] = notificationsEnabled
                    userData[DataStoreKeys.RAIN_NOTIFICATIONS] = rainNotification
                    userData[DataStoreKeys.RAIN_DAYS] = rainDays
                    userData[DataStoreKeys.RAIN_HOURS] =
                        if (rainHours == 0 && rainDays == 0) 1 else rainHours
                    userData[DataStoreKeys.RAIN_MM] = if (rainMM <= 0) 0.1 else rainMM
                    userData[DataStoreKeys.TEMPERATURE_DROP_NOTIFICATIONS] = temperatureNotification
                    userData[DataStoreKeys.TEMPERATURE_DROP_DAYS] = if (temperatureDropDays <= 0) 1 else temperatureDropDays
                    userData[DataStoreKeys.TEMPERATURE_DROP_VALUE] = if (temperatureDropValue <=0) 1 else temperatureDropValue
                    userData[DataStoreKeys.LATITUDE] = latitude
                    userData[DataStoreKeys.LONGITUDE] = longitude
                }
            }
        }

        if (notificationsEnabled)
            scheduleWeatherCheck(this)
        else
            WorkManager.getInstance(this).cancelUniqueWork("getForecast")
    }

    private fun scheduleWeatherCheck(context: Context){
        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("getForecast", ExistingPeriodicWorkPolicy.REPLACE,workRequest)
    }
}