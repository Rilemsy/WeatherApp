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
