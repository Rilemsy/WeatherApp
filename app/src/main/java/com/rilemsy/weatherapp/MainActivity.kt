package com.rilemsy.weatherapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.view.View

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.types.annotations.PrimaryKey
import io.realm.kotlin.Realm


open class Forecast() : RealmObject {
    @PrimaryKey
    var id: Int = 0
    var name: String = ""
}

class MainActivity : AppCompatActivity() {

    companion object {
        lateinit var realm: Realm
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        realm = Realm.open(
            configuration = RealmConfiguration.create(
                schema = setOf(
                    Forecast::class
                )
            )
        )
    }

    fun pullAndStore(view: View?)
    {
        //val realm =

    }

}