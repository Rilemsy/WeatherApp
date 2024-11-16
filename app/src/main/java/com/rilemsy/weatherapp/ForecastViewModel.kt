package com.rilemsy.weatherapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class WeatherApp : Application() {
    val forecastViewModel: ForecastViewModel by lazy { ForecastViewModel(application = this) }
}

class ForecastViewModel(application: Application) : AndroidViewModel(application) {

    private val _forecastList = MutableLiveData<List<Forecast>>()
    val forecastList: LiveData<List<Forecast>> = _forecastList

    fun updateForecast(/*newForecastList: List<Forecast>*/) {
//        _visitorList.value?.add(visitor)
//        _visitorList.value = _visitorList.value


        //_forecastList.value = newForecastList
        val sampleData = listOf(
            Forecast("Monday", 20.0),
            Forecast("Tue", 25.0),
            Forecast("Sunday", 15.0)
        )
        _forecastList.value = sampleData
    }
}
