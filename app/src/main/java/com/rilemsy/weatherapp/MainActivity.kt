package com.rilemsy.weatherapp

import NotificationWorker
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.rilemsy.weatherapp.databinding.ActivityMainBinding
import isOnline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import url
import java.net.URL
import java.util.concurrent.TimeUnit

var forecastDay : Int = 0

class MainActivity : AppCompatActivity() {
    var result : String =""
    private lateinit var binding: ActivityMainBinding
    private lateinit var forecastAdapter: ForecastAdapter
    private lateinit var forecastViewModel: ForecastViewModel //by viewModels()
    private lateinit var database: ForecastDatabase



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

        database = DatabaseProvider.getDatabase(this)
        
        runBlocking {
            val json = pullAndStore(url)
        }

        val recyclerView: RecyclerView = findViewById(R.id.recycler_view)
        forecastAdapter = ForecastAdapter(emptyList())
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = forecastAdapter
        println("Check adapter list")
        forecastViewModel = ViewModelProvider(this,ForecastViewModelFactory(database)).get(ForecastViewModel::class.java)
        forecastViewModel.forecastList.observe(this, Observer { forecasts ->

            Log.d("myTag", "Forecasts ${forecasts.size}")
            forecastAdapter.updateData(forecasts)

        })

        val buttonNotification = findViewById<Button>(R.id.buttonNotification)
        buttonNotification.setOnClickListener{
            forecastViewModel.loadForecasts()
        }

        val buttonSettings = findViewById<Button>(R.id.buttonSettings)
        buttonSettings.setOnClickListener{
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }

    suspend fun viewResult(view: View?)
    {
        dataStore.data.collect{userData ->
            val notificationsEnabled = userData[DataStoreKeys.NOTIFICATIONS_ENABLED] ?: false
        }
        Log.d("myTag", "View");
    }

    private fun scheduleWeatherCheck(context: Context){
        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("getForecast", ExistingPeriodicWorkPolicy.KEEP,workRequest)
    }

    private suspend fun downloadJsonContent(urlString: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                URL(urlString).openStream().bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun pullAndStore(url : String): Boolean
    {
        if (isOnline(applicationContext))
        {
            val content = downloadJsonContent(url)
            if (content != null)
            {
                val jsonObject = Gson().fromJson(content, JsonObject::class.java)
                val hourly = jsonObject["hourly"].asJsonObject
                val timeArray = hourly["time"].asJsonArray
                val temperatureArray = hourly["temperature_2m"].asJsonArray
                val rainArray = hourly["rain"].asJsonArray

                val forecastList = timeArray.mapIndexed { index, timeElement ->
                    Forecast(
                        time = timeElement.asString,
                        temperature_2m = temperatureArray[index].asDouble,
                        rain = rainArray[index].asDouble
                    )
                }

                println(" Before db ${forecastList.size}")
                database.forecastDao().clearForecasts()
                database.forecastDao().insertForecasts(forecastList)
                println(" After db ${forecastList.size}")

                forecastList.forEachIndexed { index, forecast ->
                    if (index < 3) {
                        result = forecast.temperature_2m.toString()
                        Log.d("myTag", result)
                    }
                }
            }
        }
        return true
    }
}