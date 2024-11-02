package com.rilemsy.weatherapp

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


data class  WeatherData(
    val hourly: Hourly
)
data class Hourly(
    val time: List<String>,
    val temperature_2m: List<Double>
)

class NetworkChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        if (networkInfo != null && networkInfo.isConnected) {
            // Perform background task
            val service = Executors.newSingleThreadScheduledExecutor()
            val handler = Handler(Looper.getMainLooper())
            service.scheduleWithFixedDelay({
            handler.run {
                // Do your stuff here, It gets loop every 15 Minutes
                MainActivity.getInstanceActivity()?.sendNotification()
            }
            }, 0, 1, TimeUnit.MINUTES);

        }
    }
}

class MainActivity : AppCompatActivity() {
    var result : String =""
    private val CHANNEL_ID = "channel_id_example_01"
    private val notificationId = 101


    // etc..
    companion object{
        var weakActivity: WeakReference<MainActivity>? = null

        fun getInstanceActivity(): MainActivity? {
            return weakActivity!!.get()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        Log.d("myTag", "This is my message");

        weakActivity = WeakReference(this@MainActivity)
        createNotificationChannel()

        val buttonNotification: Button = findViewById(R.id.buttonNotification)
        buttonNotification.setOnClickListener{
            sendNotification()
        }

//        val service = Executors.newSingleThreadScheduledExecutor()
//        val handler = Handler(Looper.getMainLooper())
//        service.scheduleWithFixedDelay({
//            handler.run {
//                // Do your stuff here, It gets loop every 15 Minutes
//                sendNotification()
//            }
//        }, 0, 1, TimeUnit.MINUTES);



        val networkChangeReceiver = NetworkChangeReceiver()
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(networkChangeReceiver, filter)

    }

    fun pullAndStore(view: View?)
    {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=52.52&longitude=13.41&hourly=temperature_2m&models=best_match"
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        Log.d("myTag", "Pull");

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
        Log.d("myTag", "View");
    }

    private fun createNotificationChannel(){
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES) {
        val name = "Notification Title"
        val descriptionText = "Notification Description"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        //}
    }

    @SuppressLint("MissingPermission")
    public  fun sendNotification() {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Example Title")
            .setContentText("Example Description")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(this)){
            notify(notificationId, builder.build())
        }
    }
}