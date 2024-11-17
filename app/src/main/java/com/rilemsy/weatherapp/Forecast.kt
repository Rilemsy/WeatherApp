package com.rilemsy.weatherapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "forecast_table")
data class  Forecast(
    //@PrimaryKey(autoGenerate = true) val id: Int = 0,
    @PrimaryKey val time: String,
    val temperature_2m: Double
)


//data class Hourly(
//    val time: List<String>,
//    val temperature_2m: List<Double>
//)