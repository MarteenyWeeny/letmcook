package com.letmcook.letmcook.ui.recipe

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.letmcook.letmcook.R
import com.letmcook.letmcook.databinding.ItemRecipeBinding
import com.letmcook.letmcook.models.RecipeModel

class RecipeAdapter(
    private var items: List<Pair<RecipeModel, Double>>, // Recipe and Match Score
    private val onItemClick: (RecipeModel) -> Unit,
) : RecyclerView.Adapter<RecipeAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemRecipeBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecipeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (recipe, score) = items[position]
        holder.binding.tvRecipeTitle.text = recipe.title
        
        if (recipe.imageUrl != null) {
            holder.binding.ivRecipeImage.setImageURI(android.net.Uri.parse(recipe.imageUrl))
            holder.binding.ivRecipeImage.imageTintList = null
        } else {
            holder.binding.ivRecipeImage.setImageResource(android.R.drawable.ic_menu_gallery)
            holder.binding.ivRecipeImage.imageTintList = android.content.res.ColorStateList.valueOf(
                holder.itemView.context.getColor(R.color.slate_light)
            )
        }
        
        val percent = (score * 100).toInt()
        holder.binding.cpMatchScore.progress = percent
        holder.binding.tvMatchScorePercent.text = holder.itemView.context.getString(R.string.percent_format, percent)

        // Change color based on match percentage
        val color = when {
            percent >= 80 -> holder.itemView.context.getColor(R.color.food_secondary)
            percent >= 50 -> holder.itemView.context.getColor(R.color.food_tertiary)
            else -> holder.itemView.context.getColor(R.color.error_red)
        }
        holder.binding.cpMatchScore.setIndicatorColor(color)

        holder.binding.tvCalories.text = holder.itemView.context.getString(R.string.calories_unit_format, recipe.totalCalories.toInt())
        holder.binding.tvMacros.text = holder.itemView.context.getString(
            R.string.macro_summary_format,
            recipe.totalProtein.toInt(),
            recipe.totalCarbs.toInt(),
            recipe.totalFat.toInt()
        )
        
        holder.itemView.setOnClickListener { onItemClick(recipe) }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<Pair<RecipeModel, Double>>) {
        items = newItems
        notifyDataSetChanged()
    }
}
