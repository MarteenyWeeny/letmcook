package com.letmcook.letmcook.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.letmcook.letmcook.R
import com.letmcook.letmcook.models.WorkoutModel

class WorkoutAdapter(
    private var workouts: List<WorkoutModel>,
    private val onWorkoutToggled: (WorkoutModel) -> Unit
) : RecyclerView.Adapter<WorkoutAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvWorkoutTitle)
        val tvType: TextView = view.findViewById(R.id.tvWorkoutType)
        val tvDuration: TextView = view.findViewById(R.id.tvWorkoutDuration)
        val cbCompleted: CheckBox = view.findViewById(R.id.cbCompleted)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_workout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val workout = workouts[position]
        holder.tvTitle.text = workout.title
        holder.tvType.text = workout.type
        holder.tvDuration.text = "${workout.durationMinutes} min"
        
        holder.cbCompleted.setOnCheckedChangeListener(null)
        holder.cbCompleted.isChecked = workout.isCompleted
        holder.cbCompleted.setOnCheckedChangeListener { _, isChecked ->
            workout.isCompleted = isChecked
            onWorkoutToggled(workout)
        }
    }

    override fun getItemCount() = workouts.size

    fun updateData(newWorkouts: List<WorkoutModel>) {
        workouts = newWorkouts
        notifyDataSetChanged()
    }
}
