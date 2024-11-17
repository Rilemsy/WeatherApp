package com.rilemsy.weatherapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

//class WeatherApp : Application() {
//    val forecastViewModel: ForecastViewModel by lazy { ForecastViewModel(application = this) }
//}

class ForecastViewModel(private val database: ForecastDatabase) : ViewModel() {

    private val _forecastList = MutableLiveData<List<Forecast>>()
    val forecastList: LiveData<List<Forecast>> get()= _forecastList

    fun updateForecast(/*newForecastList: List<Forecast>*/) {
//        _visitorList.value?.add(visitor)
//        _visitorList.value = _visitorList.value


        //_forecastList.value = newForecastList
        val sampleData = listOf(
            Forecast("Monday1", 20.0),
            Forecast("Tue1", 25.0),
            Forecast("Sunday1", 15.0),
            Forecast("Monday2", 20.0),
            Forecast("Tue2", 25.0),
            Forecast("Sunday2", 15.0),
            Forecast("Monday3", 20.0),
            Forecast("Tue3", 25.0),
            Forecast("Sunday3", 15.0),
            Forecast("Monday4", 20.0),
            Forecast("Tue4", 25.0),
            Forecast("Sunday4", 15.0),
            Forecast("Monday5", 20.0),
            Forecast("Tue5", 25.0),
            Forecast("Sunday5", 15.0),
            Forecast("Monday6", 20.0),
            Forecast("Tue6", 25.0),
            Forecast("Sunday6", 15.0),
            Forecast("Monday7", 20.0),
            Forecast("Tue7", 25.0),
            Forecast("Sunday7", 15.0),
            Forecast("Monday8", 20.0)

        )
        _forecastList.value = sampleData
    }

    fun loadForecasts(){
        viewModelScope.launch{
            val forecasts = withContext(Dispatchers.IO){
                database.forecastDao().getAllForecasts()
            }
            _forecastList.value = forecasts
        }
    }

}
