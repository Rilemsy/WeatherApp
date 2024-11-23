import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.activity.viewModels
import androidx.core.app.NotificationCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.work.CoroutineWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.rilemsy.weatherapp.DataStoreKeys
import com.rilemsy.weatherapp.DatabaseProvider
import com.rilemsy.weatherapp.R
import com.rilemsy.weatherapp.Forecast
import com.rilemsy.weatherapp.ForecastDatabase
import com.rilemsy.weatherapp.ForecastViewModel
import com.rilemsy.weatherapp.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.net.URL
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class NotificationWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {

    var result : String =""
    private val CHANNEL_ID = "channel_id_example_01"
    private val notificationId = 101
    //private val forecastViewModel: ForecastViewModel by viewModels()
    private val db : ForecastDatabase = DatabaseProvider.getDatabase(applicationContext)
    private var forecastList : List<Forecast> = emptyList()
    private var preferencesMap : Map<String, Any?> = emptyMap()

    override suspend fun doWork(): Result {
        // Download JSON
//        runBlocking {
//            launch {
//                applicationContext.dataStore.data.collect { userData ->
//                    val notificationsEnabled = userData[DataStoreKeys.NOTIFICATIONS_ENABLED] ?: false
//                }
//            }
//        }

        val url = "https://api.open-meteo.com/v1/forecast?latitude=52.52&longitude=13.41&hourly=temperature_2m,rain&forecast_days=14&models=best_match"
        println("Before")
        runBlocking {
            val json = pullAndStore(url)
            preferencesMap = collectPreferences()
            println("Middle ${forecastList.size}")
        }
        println("After")
        checkTimeForNotifications()

        return Result.success()

//        if (json) {
//            sendNotification("Yeah$result")
//            return Result.success()
//        }
//        else {
//            sendNotification("Not lucky")
//            return Result.failure()
//        }
    }

    suspend fun downloadJsonContent(urlString: String): String? {
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
        Log.d("myTag", "Pull");

        result = "1"
        Log.d("myTag", Thread.currentThread().name)
        val content = downloadJsonContent(url)
        if (content != null)
        {
                result = "44"
                if (content != null) {
                    //val weatherForecast = Gson().fromJson(content, Forecast::class.java)

                    val jsonObject = Gson().fromJson(content, JsonObject::class.java)
                    val hourly = jsonObject["hourly"].asJsonObject
                    val timeArray = hourly["time"].asJsonArray
                    val temperatureArray = hourly["temperature_2m"].asJsonArray
                    val rainArray = hourly["rain"].asJsonArray

                    forecastList = timeArray.mapIndexed { index, timeElement ->
                        Forecast(
                            time = timeElement.asString,
                            temperature_2m = temperatureArray[index].asDouble,
                            rain = rainArray[index].asDouble
                        )
                    }

                    //val forecastList : List<Forecast> = Gson().fromJson(jsonObject["hourly"], Array<Forecast>::class.java).asList()

                    result = "4"
                    println(" Before db ${forecastList.size}")
                    db.forecastDao().clearForecasts()
                    db.forecastDao().insertForecasts(forecastList)
                    println(" After db ${forecastList.size}")

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

    private suspend fun collectPreferences(): Map<String, Any?> {
        val preferences = applicationContext.dataStore.data.first()

        // Create a map of preferences
        return mapOf(
            "temperature_drop_notifications" to (preferences[DataStoreKeys.TEMPERATURE_DROP_NOTIFICATIONS] ?: false),
            "rain_notifications" to (preferences[DataStoreKeys.RAIN_NOTIFICATIONS] ?: false),

            "worker_days" to (preferences[DataStoreKeys.WORKER_DAYS] ?: 0),
            "worker_hours" to (preferences[DataStoreKeys.WORKER_HOURS] ?: 0),
            "rain_days" to (preferences[DataStoreKeys.RAIN_DAYS] ?: 0),
            "rain_hours" to (preferences[DataStoreKeys.RAIN_HOURS] ?: 0),
            "temperature_drop_days" to (preferences[DataStoreKeys.TEMPERATURE_DROP_DAYS] ?: 0),
            "temperature_drop_hours" to (preferences[DataStoreKeys.TEMPERATURE_DROP_HOURS] ?: 0)

        )
    }

    fun checkTimeForNotifications()
    {
        Log.d("myTag","CheckTime ${forecastList.size}")
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        forecastList.forEachIndexed { index, forecast ->

            if (true)
            //if (preferencesMap["notification_time_before_rain"] as? Boolean ?: false)
            {
                //if (forecast.rain!! >= 0.3) {
                if (forecast.rain!! >= 0.0) {
                    val rainTime = LocalDateTime.parse(forecast.time.replace('T',' '),formatter)
                    val timeNow = LocalDateTime.parse(LocalDateTime.now().toString().replace('T',' ').dropLast(7), formatter)  //.format(formatter)

                    val duration = Duration.between(timeNow, rainTime)
                    val hoursDifference = duration.toHours()
                    //Log.d("myTag", "RainTime: $rainTime | $timeNow | $duration | $hoursDifference ")
                    //println("$rainTime | $timeNow | $duration | $hoursDifference ")

                    //result = forecast.temperature_2m.toString()
                    //Log.d("myTag", result)
                    //result += "Time: $time, Temperature: ${weatherData.hourly.temperature_2m[index]}"
                    //println("Time: $time, Temperature: ${weatherData.hourly.temperature_2m[index]}")
                }
            }
        }
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
            .setContentTitle("Weather Notification")
            .setContentText(output)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        notificationManager.notify(notificationId, builder)
    }
}