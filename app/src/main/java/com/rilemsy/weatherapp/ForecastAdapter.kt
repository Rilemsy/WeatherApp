package com.rilemsy.weatherapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ForecastAdapter(private var forecastList: List<Forecast>) :
    RecyclerView.Adapter<ForecastAdapter.ViewHolder>() {

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder)
     */


    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
//        val textView: TextView

        val forecastItemLayout : LinearLayout = itemView.findViewById(R.id.forecastItemLayout)

        init {
            // Define click listener for the ViewHolder's View
            //textView = view.findViewById(R.id.textView)

        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.forecast, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        //viewHolder.textView.text = dataSet[position]
        val forecastItem = forecastList[position]

        // Clear the container to avoid duplication when scrolling
        viewHolder.forecastItemLayout.removeAllViews()

        val textViewTime = TextView(viewHolder.itemView.context).apply {
            text = forecastItem.time
            textSize = 16f
            setPadding(8,4,8,4)
        }
        viewHolder.forecastItemLayout.addView(textViewTime)

        val textViewTemperature = TextView(viewHolder.itemView.context).apply {
            text = forecastItem.temperature_2m.toString()
            textSize = 16f
            setPadding(8,4,8,4)
        }
        viewHolder.forecastItemLayout.addView(textViewTemperature)

    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = forecastList.size

    fun updateData(newForecastList: List<Forecast>) {
        forecastList = newForecastList
        notifyDataSetChanged()
    }

}