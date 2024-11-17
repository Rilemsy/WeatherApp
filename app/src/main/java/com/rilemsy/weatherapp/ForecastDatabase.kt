package com.rilemsy.weatherapp
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.*



@Dao
interface ForecastDao {
    @Query("SELECT * FROM forecast_table")
    fun getAllForecasts(): List<Forecast>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertForecasts(forecasts: List<Forecast>)

    @Query("DELETE FROM forecast_table")
    fun clearForecasts()
}

@Database(entities = [Forecast::class], version = 1, exportSchema = false)
abstract class ForecastDatabase : RoomDatabase() {
    abstract fun forecastDao(): ForecastDao
}

object DatabaseProvider {
    @Volatile
    private var INSTANCE: ForecastDatabase? = null

    fun getDatabase(context: Context): ForecastDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                ForecastDatabase::class.java,
                "forecast_database"
            ).allowMainThreadQueries().build()
            INSTANCE = instance
            instance
        }
    }
}

class ForecastViewModelFactory(private val database: ForecastDatabase) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ForecastViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ForecastViewModel(database) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}