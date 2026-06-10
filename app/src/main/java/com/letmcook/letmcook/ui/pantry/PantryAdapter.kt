package com.letmcook.letmcook.ui.pantry

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.letmcook.letmcook.R
import com.letmcook.letmcook.databinding.ItemPantryBinding
import com.letmcook.letmcook.models.IngredientModel
import com.letmcook.letmcook.models.PantryItemModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ImageView
import androidx.core.content.ContextCompat
import android.view.Gravity
import android.view.View
import android.graphics.drawable.GradientDrawable
import android.graphics.Typeface
import android.util.TypedValue
import android.content.Context

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
            showActionSheet(view.context, item, ingredient)
        }
    }

    private fun showActionSheet(context: Context, item: PantryItemModel, ing: IngredientModel) {
        val bottomSheet = BottomSheetDialog(context, R.style.AppBottomSheetDialogTheme)

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFFFFFFF.toInt())
            setPadding(0, 12, 0, 32)
        }

        // ── Handle bar ──
        root.addView(View(context).apply {
            setBackgroundColor(0xFFE0E0E0.toInt())
            layoutParams = LinearLayout.LayoutParams(dpToPx(context, 40), dpToPx(context, 4)).also {
                it.gravity = Gravity.CENTER_HORIZONTAL
                it.bottomMargin = dpToPx(context, 16)
            }
            background = roundedBackground(0xFFE0E0E0.toInt(), 99f)
        })

        // ── Item header ──
        root.addView(buildHeader(context, ing))

        // ── Divider ──
        root.addView(divider(context))

        // ── Options ──
        root.addView(buildOption(
            context,
            iconRes    = R.drawable.ic_pencil,
            iconBg     = 0xFFE8F4ED.toInt(),
            iconTint   = 0xFF2C5F3A.toInt(),
            title      = context.getString(R.string.edit_amount),
            subtitle   = "Change quantity"
        ) { onEditClick(item); bottomSheet.dismiss() })

        // Divider before delete
        root.addView(divider(context))

        root.addView(buildOption(
            context,
            iconRes    = R.drawable.ic_trash,
            iconBg     = 0xFFFCE8E8.toInt(),
            iconTint   = 0xFFC0392B.toInt(),
            title      = context.getString(R.string.delete_item),
            subtitle   = "Remove from list",
            titleColor = 0xFFC0392B.toInt()
        ) { onDeleteClick(item); bottomSheet.dismiss() })

        bottomSheet.setContentView(root)
        bottomSheet.show()
    }

    private fun buildHeader(context: Context, ing: IngredientModel): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(context, 24), dpToPx(context, 8), dpToPx(context, 24), dpToPx(context, 16))

            addView(TextView(context).apply {
                text = ing.name
                textSize = 18f
                typeface = Typeface.defaultFromStyle(Typeface.BOLD)
                setTextColor(0xFF2E2E2E.toInt())
            })
            addView(TextView(context).apply {
                text = ing.category ?: context.getString(R.string.other)
                textSize = 14f
                setTextColor(0xFF7A7066.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.topMargin = dpToPx(context, 2) }
            })
        }
    }

    private fun buildOption(
        context: Context,
        iconRes: Int,
        iconBg: Int,
        iconTint: Int,
        title: String,
        subtitle: String,
        titleColor: Int = 0xFF2E2E2E.toInt(),
        onClick: () -> Unit
    ): LinearLayout {
        val iconSizePx  = dpToPx(context, 48)
        val cornerPx    = dpToPx(context, 12).toFloat()

        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity     = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(context, 24), dpToPx(context, 12), dpToPx(context, 24), dpToPx(context, 12))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            val ripple = TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackground, ripple, true)
            foreground = ContextCompat.getDrawable(context, ripple.resourceId)
            isClickable = true
            isFocusable  = true

            // Icon box
            addView(LinearLayout(context).apply {
                gravity = Gravity.CENTER
                background = roundedBackground(iconBg, cornerPx)
                layoutParams = LinearLayout.LayoutParams(iconSizePx, iconSizePx).also {
                    it.marginEnd = dpToPx(context, 16)
                }
                addView(ImageView(context).apply {
                    setImageResource(iconRes)
                    setColorFilter(iconTint)
                    layoutParams = LinearLayout.LayoutParams(dpToPx(context, 24), dpToPx(context, 24))
                })
            })

            // Text block
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

                addView(TextView(context).apply {
                    text     = title
                    textSize = 16f
                    typeface = Typeface.defaultFromStyle(Typeface.BOLD)
                    setTextColor(titleColor)
                })
                addView(TextView(context).apply {
                    text     = subtitle
                    textSize = 13f
                    setTextColor(0xFF7A7066.toInt())
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).also { it.topMargin = dpToPx(context, 2) }
                })
            })

            // Chevron
            addView(TextView(context).apply {
                text      = "›"
                textSize  = 24f
                setTextColor(0xFFC4BAA8.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.marginStart = dpToPx(context, 12) }
            })

            setOnClickListener { onClick() }
        }
    }

    private fun divider(context: Context) = View(context).apply {
        setBackgroundColor(0xFFF0EBE1.toInt())
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(context, 1)
        ).also { 
            it.marginStart = dpToPx(context, 24)
            it.marginEnd = dpToPx(context, 24)
            it.topMargin = dpToPx(context, 4)
            it.bottomMargin = dpToPx(context, 4) 
        }
    }

    private fun roundedBackground(color: Int, radiusPx: Float): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radiusPx
        }
    }

    private fun dpToPx(context: Context, dp: Int): Int =
        (dp * context.resources.displayMetrics.density).toInt()

    override fun getItemCount() = items.size

    fun updateData(newItems: List<Pair<PantryItemModel, IngredientModel>>) {
        items = newItems
        notifyDataSetChanged()
    }
}
