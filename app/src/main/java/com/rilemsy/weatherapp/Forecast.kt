package com.rilemsy.weatherapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "forecast_table")
data class  Forecast(
    @PrimaryKey val time: String,
    val temperature_2m: Double?,
    val rain: Double?
)


//data class Hourly(
//    val time: List<String>,
//    val temperature_2m: List<Double>
//)