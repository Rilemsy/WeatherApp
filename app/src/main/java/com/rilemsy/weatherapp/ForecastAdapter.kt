package com.rilemsy.weatherapp

import android.content.res.ColorStateList
import android.content.res.Resources
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView

class ForecastAdapter(private var forecastList: List<Forecast>) :
    RecyclerView.Adapter<ForecastAdapter.ForecastViewHolder>() {

    /**
     * Provide a reference to the type of views that you are using
     * (custom ForecastViewHolder)
     */


    class ForecastViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        val textView: TextView

        //val forecastScroll : HorizontalScrollView = itemView.findViewById(R.id.forecastScroll)
        val forecastLayout : LinearLayout = itemView.findViewById(R.id.forecastLayout)
        init {
            // Define click listener for the ForecastViewHolder's View
            //textView = view.findViewById(R.id.textView)

        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForecastViewHolder {
        // Create a new view, which defines the UI of the list item
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.forecast, parent, false)

        return ForecastViewHolder(itemView)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(forecastViewHolder: ForecastViewHolder, position: Int) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        //forecastViewHolder.textView.text = dataSet[position]
        //val forecastItem = forecastList[position]
        //val forecastItemLayout : LinearLayout = findViewById(R.id.forecastItemLayout)
        // Clear the container to avoid duplication when scrolling


        //forecastViewHolder.itemParentLayout.removeAllViews()
        forecastViewHolder.forecastLayout.removeAllViews()
        //forecastViewHolder.itemParentLayout.removeAllViews()

        //val month = forecastList[position*24].time.substring(5,6)
//        forecastViewHolder.dayTextView.apply {
//            text = day
//            textSize = 18f
//            gravity = Gravity.CENTER_VERTICAL
//        }

        //println("ListSize:${forecastList.size / 24} Position: $position Day:$day")


//        val fieldsLayout = LinearLayout(forecastViewHolder.itemView.context)
//        fieldsLayout.layoutParams = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
//        fieldsLayout.orientation = LinearLayout.VERTICAL

        val textViewTimeDescription = TextView(forecastViewHolder.itemView.context).apply {
            text = "Время"
            textSize = 16f
            setPadding(8,16,8,16)
        }
        val textViewTemperatureDescription = TextView(forecastViewHolder.itemView.context).apply {
            text = "T, C°"
            gravity = Gravity.CENTER_HORIZONTAL
            textSize = 16f
            setPadding(8,16,8,16)
        }

        var strTime : String = "";
        var strTemperature : String = "";

        val verticalLayout = LinearLayout(forecastViewHolder.itemView.context)
        verticalLayout.layoutParams = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        verticalLayout.orientation = LinearLayout.VERTICAL
        val textViewTime = TextView(forecastViewHolder.itemView.context).apply {
            text = forecastList[position + 24 * forecastDay].time.substring(11)
            textSize = 16f
            setPadding(8,16,8,16)
        }
        textViewTime.setTextColor("#FFFFFF".toColorInt())

        //forecastViewHolder.forecastItemLayout.addView(textViewTime)

        val textViewTemperature = TextView(forecastViewHolder.itemView.context).apply {
            text = forecastList[position + 24 * forecastDay].temperature_2m.toString()
            textSize = 16f
            setPadding(8,16,8,16)
        }
        textViewTemperature.setTextColor("#FFFFFF".toColorInt())

        val textViewRain = TextView(forecastViewHolder.itemView.context).apply {
            text = forecastList[position + 24 * forecastDay].rain.toString()
            textSize = 16f
            setPadding(8,16,8,16)
        }
        textViewRain.setTextColor("#FFFFFF".toColorInt())

        //forecastViewHolder.forecastItemLayout.addView(textViewTemperature)
        verticalLayout.addView(textViewTime)
        verticalLayout.addView(textViewTemperature)
        verticalLayout.addView(textViewRain)
        forecastViewHolder.forecastLayout.addView(verticalLayout)



//        forecastList.forEach{ forecast ->
//
//            val verticalLayout = LinearLayout(forecastViewHolder.itemView.context)
//            verticalLayout.layoutParams = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
//            verticalLayout.orientation = LinearLayout.VERTICAL
//            val textViewTime = TextView(forecastViewHolder.itemView.context).apply {
//                text = forecastItem.time
//                textSize = 16f
//                setPadding(8,16,8,16)
//            }
//            //forecastViewHolder.forecastItemLayout.addView(textViewTime)
//
//            val textViewTemperature = TextView(forecastViewHolder.itemView.context).apply {
//                text = forecastItem.temperature_2m.toString()
//                textSize = 16f
//                setPadding(8,16,8,16)
//            }
//            //forecastViewHolder.forecastItemLayout.addView(textViewTemperature)
//            verticalLayout.addView(textViewTime)
//            verticalLayout.addView(textViewTemperature)
//            forecastViewHolder.forecastItemLayout.addView(verticalLayout)
//
//        }


//        val textViewTime = TextView(forecastViewHolder.itemView.context).apply {
//            text = forecastItem.time
//            textSize = 16f
//            setPadding(8,4,8,4)
//        }
//        forecastViewHolder.forecastItemLayout.addView(textViewTime)
//
//        val textViewTemperature = TextView(forecastViewHolder.itemView.context).apply {
//            text = forecastItem.temperature_2m.toString()
//            textSize = 16f
//            setPadding(8,4,8,4)
//        }
//        forecastViewHolder.forecastItemLayout.addView(textViewTemperature)

    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = if (forecastList.isEmpty()) 0 else 24

    fun updateData(newForecastList: List<Forecast>) {
        forecastList = newForecastList
        notifyDataSetChanged()
    }

}