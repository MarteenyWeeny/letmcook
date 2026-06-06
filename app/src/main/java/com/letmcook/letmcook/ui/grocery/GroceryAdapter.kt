package com.letmcook.letmcook.ui.grocery

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.letmcook.letmcook.R
import com.letmcook.letmcook.models.GroceryItemModel
import com.letmcook.letmcook.models.IngredientModel

class GroceryAdapter(
    private var items: List<Pair<GroceryItemModel, IngredientModel?>>,
    private val onEditClick: (GroceryItemModel) -> Unit,
    private val onDeleteClick: (GroceryItemModel) -> Unit
) : RecyclerView.Adapter<GroceryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvIngredientName)
        val tvCategory: TextView = view.findViewById(R.id.tvCategory)
        val tvQuantity: TextView = view.findViewById(R.id.tvQuantity)
        val btnMore: View = view.findViewById(R.id.btnMore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_grocery, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (item, ing) = items[position]
        holder.tvName.text = ing?.name ?: "Unknown"
        holder.tvCategory.text = ing?.category ?: "Other"
        holder.tvQuantity.text = "${item.quantity}${ing?.unitOfMeasure ?: ""}"
        
        holder.btnMore.setOnClickListener { view ->
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

    fun updateData(newItems: List<Pair<GroceryItemModel, IngredientModel?>>) {
        items = newItems
        notifyDataSetChanged()
    }
}
