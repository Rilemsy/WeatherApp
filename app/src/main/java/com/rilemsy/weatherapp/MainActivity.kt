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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import url
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

var forecastDay : Int = 0

class MainActivity : AppCompatActivity() {
    var result : String =""
    private lateinit var binding: ActivityMainBinding
    private lateinit var forecastAdapter: ForecastAdapter
    private lateinit var forecastViewModel: ForecastViewModel //by viewModels()
    private lateinit var database: ForecastDatabase
    private var forecastList : List<Forecast> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val monthList = listOf("января","февраля","марта","апреля","мая","июня","июля","августа","сентября","октября","ноября","декабря")

        Log.d("myTag", "MainActivityOnCreate");

        database = DatabaseProvider.getDatabase(this)

        runBlocking {
            println("0st runBlock Main $url")
            val userData = dataStore.data.first()
            url = url.replace("(?<=latitude=)[\\-0-9.a-z]+".toRegex(),((userData[DataStoreKeys.LATITUDE])?.toString() ?: "0.0"))
            url = url.replace("(?<=longitude=)[\\-0-9.a-z]+".toRegex(),((userData[DataStoreKeys.LONGITUDE])?.toString() ?: "0.0"))
            println("1st runBlock Main $url")
            val json = pullAndStore(url)
        }
        println("After Main $url")

        val recyclerView: RecyclerView = findViewById(R.id.recycler_view)
        forecastAdapter = ForecastAdapter(emptyList())
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = forecastAdapter
        forecastViewModel = ViewModelProvider(this,ForecastViewModelFactory(database)).get(ForecastViewModel::class.java)

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

        //.format(formatter)

        forecastViewModel.forecastList.observe(this, Observer { forecasts ->

            Log.d("myTag", "Forecasts ${forecasts.size}")
            forecastList = forecasts
            val strTimeNow = LocalDateTime.now().toString().replace('T',' ').dropLast(9) + "00"
            println("strTimeNow ${LocalDateTime.now().toString()} $strTimeNow")
            val timeNow = LocalDateTime.parse(strTimeNow, formatter)
            binding.currentTemperatureView.text = forecastList.find { forecast -> forecast.time.replace('T',' ') == strTimeNow }?.temperature_2m?.roundToInt().toString()
            binding.forecastDayView.text = "${timeNow.dayOfMonth} ${monthList[timeNow.month.ordinal]}"
            forecastAdapter.updateData(forecasts)
        })

        forecastViewModel.loadForecasts()



        binding.buttonSettings.setOnClickListener{
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        binding.nextForecastButton.setOnClickListener {
            if (forecastDay < 13) {
                forecastDay++
                val time = LocalDateTime.parse(forecastList[forecastDay*24].time.replace('T',' '),formatter)
                binding.forecastDayView.text = "${time.dayOfMonth} ${monthList[time.month.ordinal]}"
                forecastAdapter.notifyDataSetChanged()
            }
        }

        binding.prevForecastButton.setOnClickListener {
            if (forecastDay > 0) {
                forecastDay--
                val time = LocalDateTime.parse(forecastList[forecastDay*24].time.replace('T',' '),formatter)
                binding.forecastDayView.text = "${time.dayOfMonth} ${monthList[time.month.ordinal]}"
                forecastAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        runBlocking {
            println("0st runBlock Main $url")
            val userData = dataStore.data.first()
            url = url.replace("(?<=latitude=)[\\-0-9.a-z]+".toRegex(),((userData[DataStoreKeys.LATITUDE])?.toString() ?: "0.0"))
            url = url.replace("(?<=longitude=)[\\-0-9.a-z]+".toRegex(),((userData[DataStoreKeys.LONGITUDE])?.toString() ?: "0.0"))
            println("1st runBlock Main $url")
            val json = pullAndStore(url)
        }
        forecastViewModel.loadForecasts()
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