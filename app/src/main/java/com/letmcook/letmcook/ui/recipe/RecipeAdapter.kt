package com.letmcook.letmcook.ui.recipe

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.letmcook.letmcook.R
import com.letmcook.letmcook.databinding.ItemRecipeBinding
import com.letmcook.letmcook.models.RecipeModel

class RecipeAdapter(
    private var items: List<Pair<RecipeModel, Double>>, // Recipe and Match Score
    private val onItemClick: (RecipeModel) -> Unit
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
        holder.binding.tvMatchScorePercent.text = "$percent%"

        // Change color based on match percentage
        val color = when {
            percent >= 80 -> android.graphics.Color.parseColor("#10b981") // accent_green
            percent >= 50 -> android.graphics.Color.parseColor("#f59e0b") // accent_orange
            else -> android.graphics.Color.parseColor("#dc2626") // error_red
        }
        holder.binding.cpMatchScore.setIndicatorColor(color)

        holder.binding.tvCalories.text = "${recipe.totalCalories.toInt()} kcal"
        holder.binding.tvMacros.text = "P: ${recipe.totalProtein.toInt()}g | C: ${recipe.totalCarbs.toInt()}g | F: ${recipe.totalFat.toInt()}g"
        
        holder.itemView.setOnClickListener { onItemClick(recipe) }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<Pair<RecipeModel, Double>>) {
        items = newItems
        notifyDataSetChanged()
    }
}
