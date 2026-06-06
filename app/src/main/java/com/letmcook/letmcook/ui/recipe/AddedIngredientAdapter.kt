package com.letmcook.letmcook.ui.recipe

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.letmcook.letmcook.databinding.ItemAddedIngredientBinding
import com.letmcook.letmcook.models.IngredientModel

class AddedIngredientAdapter(
    private var items: MutableList<Pair<IngredientModel, Double>>,
    private val onRemoveClick: (Int) -> Unit
) : RecyclerView.Adapter<AddedIngredientAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemAddedIngredientBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAddedIngredientBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (ingredient, quantity) = items[position]
        holder.binding.tvIngredientName.text = ingredient.name
        holder.binding.tvQuantity.text = "${quantity}${ingredient.unitOfMeasure ?: ""}"
        
        holder.binding.btnRemove.setOnClickListener {
            onRemoveClick(position)
        }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<Pair<IngredientModel, Double>>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
