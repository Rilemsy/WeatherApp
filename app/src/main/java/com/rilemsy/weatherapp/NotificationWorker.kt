import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.activity.viewModels
import androidx.core.app.NotificationCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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

var url = "https://api.open-meteo.com/v1/forecast?latitude=53.52&longitude=21.41&hourly=temperature_2m,rain&forecast_days=14&models=best_match&timezone=auto"

fun isOnline(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    if (connectivityManager != null) {
        val capabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        if (capabilities != null) {
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                Log.i("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
                return true
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
                return true
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                Log.i("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
                return true
            }
        }
    }
    return false
}

class NotificationWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {

    enum class NotificationEvent {
        RAIN,TEMPERATURE_DROP
    }

    var result : String =""
    private val CHANNEL_ID = "channel_id_example_01"
    private val notificationId = 101
    private val db : ForecastDatabase = DatabaseProvider.getDatabase(applicationContext)
    private var forecastList : List<Forecast> = emptyList()
    private var preferencesMap : MutableMap<String, Any?> = mutableMapOf()
    val monthList = listOf("января","февраля","марта","апреля","мая","июня","июля","августа","сентября","октября","ноября","декабря")

    override suspend fun doWork(): Result {
        runBlocking {
            preferencesMap = collectPreferences().toMutableMap()
            url = url.replace("(?<=latitude=)[\\-0-9.a-z]+".toRegex(),(preferencesMap["latitude"] as Double).toString())
            url = url.replace("(?<=longitude=)[\\-0-9.a-z]+".toRegex(),(preferencesMap["longitude"] as Double).toString())
            pullAndStore(url)
        }
        checkTimeForNotifications()
        return Result.success()
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

    suspend fun pullAndStore(url : String)
    {
        Log.d("myTag", "Pull");

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

                forecastList = timeArray.mapIndexed { index, timeElement ->
                    Forecast(
                        time = timeElement.asString,
                        temperature_2m = temperatureArray[index].asDouble,
                        rain = rainArray[index].asDouble
                    )
                }

                println(" Before db ${forecastList.size}")
                db.forecastDao().clearForecasts()
                db.forecastDao().insertForecasts(forecastList)
                println(" After db ${forecastList.size}")

                forecastList.forEachIndexed { index, forecast ->
                    if (index < 3) {
                        result = forecast.temperature_2m.toString()
                        Log.d("myTag", result)
                    }
                }
            }
        }
        else
        {
            forecastList = db.forecastDao().getAllForecasts()
        }
    }

    private suspend fun collectPreferences(): Map<String, Any?> {
        val preferences = applicationContext.dataStore.data.first()

        return mapOf(
            "temperature_drop_notifications" to (preferences[DataStoreKeys.TEMPERATURE_DROP_NOTIFICATIONS] ?: false),
            "rain_notifications" to (preferences[DataStoreKeys.RAIN_NOTIFICATIONS] ?: false),
            "latitude" to (preferences[DataStoreKeys.LATITUDE] ?: 0.0),
            "longitude" to (preferences[DataStoreKeys.LONGITUDE] ?: 0.0),

            "rain_days" to (preferences[DataStoreKeys.RAIN_DAYS] ?: 1),
            "rain_hours" to (preferences[DataStoreKeys.RAIN_HOURS] ?: 0),
            "temperature_drop_days" to (preferences[DataStoreKeys.TEMPERATURE_DROP_DAYS] ?: 1),
            "temperature_drop_value" to (preferences[DataStoreKeys.TEMPERATURE_DROP_VALUE] ?: 5),
            "rain_mm" to (preferences[DataStoreKeys.RAIN_MM] ?: 0.3),

            "rain_notification_sent" to (preferences[DataStoreKeys.RAIN_NOTIFICATION_SENT] ?: false),
            "temperature_drop_notification_sent" to (preferences[DataStoreKeys.TEMPERATURE_DROP_NOTIFICATION_SENT] ?: false),
            "notification_event_time_rain" to (preferences[DataStoreKeys.NOTIFICATION_EVENT_TIME_RAIN] ?: ""),
            "notification_event_time_temperature_drop" to (preferences[DataStoreKeys.NOTIFICATION_EVENT_TIME_TEMPERATURE_DROP] ?: "")
        )
    }

    private suspend fun checkTimeForNotifications()
    {
        Log.d("myTag","CheckTime ${forecastList.size}")
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        var returnFlag : Boolean = false
        var temperatureSum : Double = 0.0
        var averageTemperatureToday : Double = 0.0
        val timeNow = LocalDateTime.parse(LocalDateTime.now().toString().replace('T',' ').dropLast(7), formatter)  //.format(formatter)



        var notificationRainTime = preferencesMap["notification_event_time_rain"] as String
        println("notification_event_time_rain $notificationRainTime")
        var notificationRainLocalDateTime : LocalDateTime = timeNow.plusYears(2)
        if (!notificationRainTime.isNullOrEmpty())
            notificationRainLocalDateTime = LocalDateTime.parse(notificationRainTime.replace('T',' '),formatter)

        if (preferencesMap["rain_notifications"] as Boolean && (timeNow.isAfter(notificationRainLocalDateTime) || notificationRainTime.isNullOrEmpty()))
        {
            for (forecast in forecastList)
            {
                val forecastTime = LocalDateTime.parse(forecast.time.replace('T',' '),formatter)
                var hoursDifference = Duration.between(timeNow, forecastTime).toHours()
                if (forecast.rain!! >= preferencesMap["rain_mm"] as Double &&
                    (notificationRainTime.isNullOrEmpty() || (!notificationRainTime.isNullOrEmpty() && forecastTime.dayOfMonth != notificationRainLocalDateTime.dayOfMonth))
                    && hoursDifference >=0 && hoursDifference <= (preferencesMap["rain_days"] as Int)*24 + preferencesMap["rain_hours"] as Int)
                {
                    preferencesMap["notification_event_time_rain"] = forecast.time
                    val dataStore = applicationContext.dataStore
                    dataStore.edit { userData ->
                        userData[DataStoreKeys.NOTIFICATION_EVENT_TIME_RAIN] = forecast.time
                    }
                    val eventMessage = "${forecastTime.dayOfMonth} ${monthList[forecastTime.month.value-1]} в ${forecastTime.hour}.00 ожидается дождь"
                    sendNotification(eventMessage, NotificationEvent.RAIN)
                    break
                }
            }
        }

        var notificationTemperatureDropTime = preferencesMap["notification_event_time_temperature_drop"] as String
        println("notification_event_temperature_drop $notificationTemperatureDropTime")
        var notificationTemperatureDropLocalDateTime : LocalDateTime = timeNow.plusYears(2)
        if (!notificationTemperatureDropTime.isNullOrEmpty())
            notificationTemperatureDropLocalDateTime = LocalDateTime.parse(notificationTemperatureDropTime.replace('T',' '),formatter)

        if ((timeNow.isAfter(notificationTemperatureDropLocalDateTime) || notificationTemperatureDropTime.isNullOrEmpty() )&& forecastList.isNotEmpty())
        {
            preferencesMap["notification_event_time_temperature_drop"] = forecastList[24].time
            val dataStore = applicationContext.dataStore
            dataStore.edit { userData ->
                userData[DataStoreKeys.NOTIFICATION_EVENT_TIME_TEMPERATURE_DROP] = forecastList[24].time
            }
        }
        else
        {
            returnFlag = true
        }

        if (preferencesMap["temperature_drop_notifications"] as Boolean && !returnFlag)
        {
            var index : Int = 0
            for (forecast in forecastList)
            {
                temperatureSum += forecast.temperature_2m!!

                if ((index + 1) % 24 == 0 && index > 23)
                {
                    val averageTemperature = temperatureSum/24
                    println("Avrg $averageTemperature $averageTemperatureToday")
                    val forecastTime = LocalDateTime.parse(forecast.time.replace('T',' ').replaceRange(11,13,"00"),formatter)
                    var hoursDifference = Duration.between(timeNow, forecastTime).toHours()
                    println("forecasts hour difference $hoursDifference")
                    println("Check avg calc ${averageTemperatureToday - averageTemperature}   ${preferencesMap["temperature_drop_value"] as Int}")
                    if (averageTemperatureToday - averageTemperature >= preferencesMap["temperature_drop_value"] as Int
                        &&  hoursDifference >=0 && hoursDifference <= (preferencesMap["temperature_drop_days"] as Int)*24)
                    {
                        val editedForecastTime = forecast.time.replaceRange(11,13,"00") // start of day
                        preferencesMap["notification_event_time_temperature_drop"] = editedForecastTime
                        val dataStore = applicationContext.dataStore
                        dataStore.edit { userData ->
                            userData[DataStoreKeys.NOTIFICATION_EVENT_TIME_TEMPERATURE_DROP] = editedForecastTime
                        }
                        val eventMessage = "${forecastTime.dayOfMonth} ${monthList[forecastTime.month.value-1]} ожидается падение температуры"
                        sendNotification(eventMessage, NotificationEvent.TEMPERATURE_DROP)
                        break
                    }
                    temperatureSum = 0.0
                }
                else if (index == 23) {
                    averageTemperatureToday = temperatureSum / 24
                    temperatureSum = 0.0
                }
                index++
            }
        }
    }

    @SuppressLint("MissingPermission", "ObsoleteSdkInt")
    fun sendNotification(text: String, event: NotificationEvent ) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Default Channel", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }
        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        notificationManager.notify(notificationId + event.ordinal, builder)
    }
}