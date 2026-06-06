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
    private val onDeleteClick: (PantryItemModel) -> Unit,
) : RecyclerView.Adapter<PantryAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemPantryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPantryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (item, ingredient) = items[position]
        val context = holder.itemView.context
        holder.binding.tvIngredientName.text = ingredient.name
        holder.binding.tvQuantity.text = context.getString(
            R.string.qty_format,
            item.currentQuantity.toString(),
            ingredient.unitOfMeasure ?: ""
        )
        holder.binding.tvCategory.text = ingredient.category ?: context.getString(R.string.other)
        holder.binding.tvExpDate.text = item.expirationDate?.let {
            context.getString(R.string.exp_date_format, it)
        } ?: ""
        
        holder.itemView.setOnClickListener { onItemClick(item) }

        holder.binding.btnMore.setOnClickListener { view ->
            val popup = android.widget.PopupMenu(view.context, view)
            popup.menu.add(context.getString(R.string.edit_amount))
            popup.menu.add(context.getString(R.string.delete_item))
            
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.title) {
                    context.getString(R.string.edit_amount) -> onEditClick(item)
                    context.getString(R.string.delete_item) -> onDeleteClick(item)
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
