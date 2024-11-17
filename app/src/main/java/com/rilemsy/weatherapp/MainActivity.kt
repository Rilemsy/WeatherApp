package com.rilemsy.weatherapp

import NotificationWorker
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.rilemsy.weatherapp.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    var result : String =""
    private val CHANNEL_ID = "channel_id_example_01"
    private val notificationId = 101
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

        //scheduleWeatherCheck(this)

        //val dataset = arrayOf("January", "February", "March,","January", "February", "March,","January", "February", "March,","January", "February", "March,")
        //val forecastAdapter = ForecastAdapter(dataset)

        val url = "https://api.open-meteo.com/v1/forecast?latitude=52.52&longitude=13.41&hourly=temperature_2m&models=best_match"
        val json = pullAndStore(url)
        Log.d("myTag", json.toString());
        database = DatabaseProvider.getDatabase(this)

        val recyclerView: RecyclerView = findViewById(R.id.recycler_view)
        forecastAdapter = ForecastAdapter(emptyList())
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = forecastAdapter

        forecastViewModel = ViewModelProvider(this,ForecastViewModelFactory(database)).get(ForecastViewModel::class.java)
        forecastViewModel.forecastList.observe(this, Observer { forecasts ->
            forecastAdapter.updateData(forecasts)

        })


        forecastViewModel.updateForecast()

        val buttonNotification = findViewById<Button>(R.id.buttonNotification)
        buttonNotification.setOnClickListener{
            forecastViewModel.loadForecasts()
        }


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

    fun downloadJsonContent(urlString: String, callback: (String?) -> Unit) {
        val job = CoroutineScope(Dispatchers.IO).launch {
            val result = try {
                URL(urlString).openStream().bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }

            withContext(Dispatchers.Main) {
                callback(result)
            }
        }
    }

    fun pullAndStore(url : String): Boolean
    {
        Log.d("myTag", "Pull");

        result = "1"
        Log.d("myTag", Thread.currentThread().name)
        downloadJsonContent(url) { content ->
            result = "44"
            if (content != null) {
                //val weatherForecast = Gson().fromJson(content, Forecast::class.java)

                //val jsonObject = Gson().fromJson(content, JsonObject::class.java)
                //val forecast = Gson().fromJson(jsonObject["hourly"], Forecast::class.java)
                //val forecastList : List<Forecast> = GsonBuilder().create().fromJson(jsonObject["hourly"], Array<Forecast>::class.java).toList()
                val jsonObject = Gson().fromJson(content, JsonObject::class.java)
                val hourly = jsonObject["hourly"].asJsonObject
                val timeArray = hourly["time"].asJsonArray
                val temperatureArray = hourly["temperature_2m"].asJsonArray

                val forecastList = timeArray.zip(temperatureArray) { time, temp ->
                    Forecast(
                        time = time.asString,
                        temperature_2m = temp.asDouble
                    )
                }
                //val forecastList : List<Forecast> = Gson().fromJson(jsonObject["hourly"], Array<Forecast>::class.java).asList()

                result = "4"

                val db = DatabaseProvider.getDatabase(applicationContext)
                db.forecastDao().clearForecasts()
                db.forecastDao().insertForecasts(forecastList)


                // Print hourly temperatures
                forecastList.forEachIndexed { index, forecast ->
                    if (index < 3) {
                        result = forecast.temperature_2m.toString()
                        Log.d("myTag", result)
                        //result += "Time: $time, Temperature: ${weatherData.hourly.temperature_2m[index]}"
                        //println("Time: $time, Temperature: ${weatherData.hourly.temperature_2m[index]}")
                    }
                }
                result = "5"

            }
            else {
                result = "K"
            }
        }

        if (result.isEmpty())
            return false
        else
            return true

    }


}