package com.letmcook.letmcook.ui.pantry

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.letmcook.letmcook.R
import com.letmcook.letmcook.databinding.ItemPantryBinding
import com.letmcook.letmcook.models.IngredientModel
import com.letmcook.letmcook.models.PantryItemModel

class PantryAdapter(
    private var items: List<Pair<PantryItemModel, IngredientModel>>,
    private val onItemClick: (PantryItemModel) -> Unit,
    private val onEditClick: (PantryItemModel) -> Unit,
    private val onDeleteClick: (PantryItemModel) -> Unit
) : RecyclerView.Adapter<PantryAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemPantryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPantryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (item, ingredient) = items[position]
        holder.binding.tvIngredientName.text = ingredient.name
        holder.binding.tvQuantity.text = "${item.currentQuantity}${ingredient.unitOfMeasure ?: ""}"
        holder.binding.tvCategory.text = ingredient.category ?: "Other"
        holder.binding.tvExpDate.text = item.expirationDate?.let { "Exp: $it" } ?: ""
        
        holder.itemView.setOnClickListener { onItemClick(item) }

        holder.binding.btnMore.setOnClickListener { view ->
            val popup = android.widget.PopupMenu(view.context, view)
            popup.menu.add("Edit Amount")
            popup.menu.add("Delete Item")
            
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.title) {
                    "Edit Amount" -> onEditClick(item)
                    "Delete Item" -> onDeleteClick(item)
                }
                true
            }
            popup.show()
        }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<Pair<PantryItemModel, IngredientModel>>) {
        items = newItems
        notifyDataSetChanged()
    }
}
