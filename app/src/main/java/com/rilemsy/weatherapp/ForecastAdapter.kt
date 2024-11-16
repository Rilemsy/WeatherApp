package com.rilemsy.weatherapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.LinearLayout
import android.widget.TextView
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
        val forecastItemLayout : LinearLayout = itemView.findViewById(R.id.forecastItemLayout)

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
        val forecastItem = forecastList[position]
        //val forecastItemLayout : LinearLayout = findViewById(R.id.forecastItemLayout)
        // Clear the container to avoid duplication when scrolling
        forecastViewHolder.forecastItemLayout.removeAllViews()
        forecastList.forEach{ forecast ->

            val verticalLayout = LinearLayout(forecastViewHolder.itemView.context)
            verticalLayout.layoutParams = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            verticalLayout.orientation = LinearLayout.VERTICAL
            val textViewTime = TextView(forecastViewHolder.itemView.context).apply {
                text = forecastItem.time
                textSize = 16f
                setPadding(8,16,8,16)
            }
            //forecastViewHolder.forecastItemLayout.addView(textViewTime)

            val textViewTemperature = TextView(forecastViewHolder.itemView.context).apply {
                text = forecastItem.temperature_2m.toString()
                textSize = 16f
                setPadding(8,16,8,16)
            }
            //forecastViewHolder.forecastItemLayout.addView(textViewTemperature)
            verticalLayout.addView(textViewTime)
            verticalLayout.addView(textViewTemperature)
            forecastViewHolder.forecastItemLayout.addView(verticalLayout)

        }


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
    override fun getItemCount() = forecastList.size

    fun updateData(newForecastList: List<Forecast>) {
        forecastList = newForecastList
        notifyDataSetChanged()
    }

}