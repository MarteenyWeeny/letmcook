package com.letmcook.letmcook.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.letmcook.letmcook.R
import java.text.SimpleDateFormat
import java.util.*

class DateAdapter(
    private var dates: List<Date>,
    private var selectedDate: Date,
    private val onDateSelected: (Date) -> Unit
) : RecyclerView.Adapter<DateAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val llItem: View = view.findViewById(R.id.llDateItem)
        val tvDayName: TextView = view.findViewById(R.id.tvDayName)
        val tvDayNumber: TextView = view.findViewById(R.id.tvDayNumber)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_date_selector, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val date = dates[position]
        val dayNameSdf = SimpleDateFormat("E", Locale.getDefault())
        val dayNumberSdf = SimpleDateFormat("d", Locale.getDefault())
        val fullDateSdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())

        holder.tvDayName.text = dayNameSdf.format(date).take(1).uppercase()
        holder.tvDayNumber.text = dayNumberSdf.format(date)

        val isSelected = fullDateSdf.format(date) == fullDateSdf.format(selectedDate)

        if (isSelected) {
            holder.llItem.setBackgroundResource(R.drawable.circle_dark)
            holder.tvDayName.setTextColor(holder.itemView.context.getColor(R.color.white))
            holder.tvDayNumber.setTextColor(holder.itemView.context.getColor(R.color.white))
        } else {
            holder.llItem.background = null
            holder.tvDayName.setTextColor(holder.itemView.context.getColor(R.color.slate_medium))
            holder.tvDayNumber.setTextColor(holder.itemView.context.getColor(R.color.slate_dark))
        }

        holder.itemView.setOnClickListener {
            selectedDate = date
            notifyDataSetChanged()
            onDateSelected(date)
        }
    }

    override fun getItemCount() = dates.size

    fun updateSelectedDate(date: Date) {
        selectedDate = date
        notifyDataSetChanged()
    }
    
    fun updateDates(newDates: List<Date>) {
        dates = newDates
        notifyDataSetChanged()
    }
}
