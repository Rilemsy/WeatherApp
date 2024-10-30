package com.rilemsy.weatherapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.view.View
import android.widget.TextView

import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.Gson
import java.io.IOException

data class  WeatherData(
    val hourly: Hourly
)
data class Hourly(
    val time: List<String>,
    val temperature_2m: List<Double>
)

class MainActivity : AppCompatActivity() {
    var result : String =""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    fun pullAndStore(view: View?)
    {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=52.52&longitude=13.41&hourly=temperature_2m&models=best_match"
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.body?.let { responseBody ->
                    if (response.isSuccessful) {
                        val json = responseBody.string()

                        // Parse JSON to WeatherData object
                        val weatherData = Gson().fromJson(json, WeatherData::class.java)

                        // Print hourly temperatures
                        weatherData.hourly.time.forEachIndexed { index, time ->
                            if (index < 10) {
                                result += "Time: $time, Temperature: ${weatherData.hourly.temperature_2m[index]}"
                                println("Time: $time, Temperature: ${weatherData.hourly.temperature_2m[index]}")
                            }
                        }

                    }
                }
            }
        })
    }

    fun viewResult(view: View?)
    {
        val textFetchResult: TextView = findViewById(R.id.textFetchResult)
        textFetchResult.text = result

    }

}