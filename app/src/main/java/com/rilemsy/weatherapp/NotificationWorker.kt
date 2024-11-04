import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.rilemsy.weatherapp.R
import com.rilemsy.weatherapp.WeatherData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.net.URL

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