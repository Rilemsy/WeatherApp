import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.activity.viewModels
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.rilemsy.weatherapp.R
import com.rilemsy.weatherapp.Forecast
import com.rilemsy.weatherapp.ForecastViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class NotificationWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    var result : String =""
    private val CHANNEL_ID = "channel_id_example_01"
    private val notificationId = 101
    //private val forecastViewModel: ForecastViewModel by viewModels()

    override fun doWork(): Result {
        // Download JSON
//        runBlocking {
//            launch {
//                applicationContext.dataStore.data.collect { userData ->
//                    val notificationsEnabled = userData[DataStoreKeys.NOTIFICATIONS_ENABLED] ?: false
//                }
//            }
//        }

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

                val jsonObject = Gson().fromJson(content, JsonObject::class.java)
                //val forecast = Gson().fromJson(jsonObject["hourly"], Forecast::class.java)
                val forecastList : List<Forecast> = GsonBuilder().create().fromJson(jsonObject["hourly"], Array<Forecast>::class.java).toList()
                result = "4"

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