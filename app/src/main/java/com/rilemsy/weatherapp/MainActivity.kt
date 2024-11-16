package com.rilemsy.weatherapp

import NotificationWorker
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.rilemsy.weatherapp.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    var result : String =""
    private val CHANNEL_ID = "channel_id_example_01"
    private val notificationId = 101
    private lateinit var binding: ActivityMainBinding
    private lateinit var forecastAdapter: ForecastAdapter
    private val forecastViewModel: ForecastViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        Log.d("myTag", "MainActivityOnCreate");

        //scheduleWeatherCheck(this)

        //val dataset = arrayOf("January", "February", "March,","January", "February", "March,","January", "February", "March,","January", "February", "March,")
        //val forecastAdapter = ForecastAdapter(dataset)

        val recyclerView: RecyclerView = findViewById(R.id.recycler_view)
        forecastAdapter = ForecastAdapter(emptyList())
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = forecastAdapter

        forecastViewModel.forecastList.observe(this, Observer { forecasts ->
            forecastAdapter.updateData(forecasts)

        })

        forecastViewModel.updateForecast()

        CoroutineScope(Dispatchers.IO).launch {
            setNotificationsEnabled()
        }

        val chkBoxNotificationEnabled = findViewById<CheckBox>(R.id.notificationsEnabled)
        chkBoxNotificationEnabled.setOnClickListener{
            CoroutineScope(Dispatchers.IO).launch {
                editNotificationsEnabled(chkBoxNotificationEnabled.isChecked)
            }
        }

    }

    suspend fun viewResult(view: View?)
    {
        val textFetchResult: TextView = findViewById(R.id.textFetchResult)
//        val notificationsEnabled: Boolean
        dataStore.data.collect{userData ->
            val notificationsEnabled = userData[DataStoreKeys.NOTIFICATIONS_ENABLED] ?: false
            textFetchResult.text = notificationsEnabled.toString()
        }
        Log.d("myTag", "View");
    }

    private fun scheduleWeatherCheck(context: Context){
        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("getForecast", ExistingPeriodicWorkPolicy.KEEP,workRequest)
    }

    suspend fun editNotificationsEnabled(value: Boolean) {
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

    suspend fun setNotificationsEnabled(){
        dataStore.data.collect { userData ->
            val notificationsEnabled = userData[DataStoreKeys.NOTIFICATIONS_ENABLED] ?: false


            runOnUiThread {
                if (notificationsEnabled)
                {
                    scheduleWeatherCheck(this)
                }
                else
                {
                    WorkManager.getInstance(this).cancelUniqueWork("getForecast")
                }

                val view = findViewById<CheckBox>(R.id.notificationsEnabled)
                view.isChecked = notificationsEnabled
            }
        }
    }
}