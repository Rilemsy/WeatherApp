package com.rilemsy.weatherapp

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import java.lang.ref.WeakReference
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.concurrent.Executor

data class  WeatherData(
    val hourly: Hourly
)
data class Hourly(
    val time: List<String>,
    val temperature_2m: List<Double>
)

class NotificationWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    var result : String =""
    private val CHANNEL_ID = "channel_id_example_01"
    private val notificationId = 101


    override fun doWork(): Result {
        // Download JSON
        val url = "https://api.open-meteo.com/v1/forecast?latitude=52.52&longitude=13.41&hourly=temperature_2m&models=best_match"
        val json = pullAndStore(url)


        if (json) {
                sendNotification("Yeah$result")
                return Result.success()
        }
        else {
            sendNotification("Not lucky")
            return Result.failure()
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

        runBlocking {
            job.join()
        }
    }

    fun pullAndStore(url : String): Boolean
    {
        Log.d("myTag", "Pull");

        result = "1"

        downloadJsonContent(url) { content ->
            if (content != null) {
                val weatherData = Gson().fromJson(content, WeatherData::class.java)
                result = "4"

                // Print hourly temperatures
                weatherData.hourly.time.forEachIndexed { index, time ->
                    if (index < 1) {
                        result = weatherData.hourly.temperature_2m[index].toString()
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

    fun createNotificationChannel(){
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES) {
        val name = "Notification Title"
        val descriptionText = "Notification Description"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        //}
    }

    @SuppressLint("MissingPermission")
    fun sendNotification(output: String ) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Default Channel", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }
        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle("Example Title")
            .setContentText(output)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        notificationManager.notify(notificationId, builder)
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
        Log.d("myTag", "MainActivityOnCreate");

        scheduleWeatherCheck(this)

//        weakActivity = WeakReference(this@MainActivity)
//        createNotificationChannel()
//
//        val buttonNotification: Button = findViewById(R.id.buttonNotification)
//        buttonNotification.setOnClickListener{
//            sendNotification()
//        }

//        val service = Executors.newSingleThreadScheduledExecutor()
//        val handler = Handler(Looper.getMainLooper())
//        service.scheduleWithFixedDelay({
//            handler.run {
//                // Do your stuff here, It gets loop every 15 Minutes
//                sendNotification()
//            }
//        }, 0, 1, TimeUnit.MINUTES);



//        val networkChangeReceiver = NetworkChangeReceiver()
//        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
//        registerReceiver(networkChangeReceiver, filter)

    }


    fun viewResult(view: View?)
    {
        val textFetchResult: TextView = findViewById(R.id.textFetchResult)
        textFetchResult.text = result
        Log.d("myTag", "View");
    }


    private fun scheduleWeatherCheckOnce() {
        val sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val isTaskScheduled = sharedPreferences.getBoolean("is_task_scheduled", false)

        if (!isTaskScheduled) {
            // Schedule the WorkManager task
            scheduleWeatherCheck(this)

            // Mark the task as scheduled to avoid scheduling it again
            sharedPreferences.edit().putBoolean("is_task_scheduled", true).apply()
        }
    }

    private fun scheduleWeatherCheck(context: Context){
        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("getForecast", ExistingPeriodicWorkPolicy.KEEP,workRequest)
    }

}