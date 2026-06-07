package com.letmcook.letmcook.ui.grocery

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.letmcook.letmcook.R
import com.letmcook.letmcook.models.GroceryItemModel
import com.letmcook.letmcook.models.IngredientModel
import com.google.android.material.bottomsheet.BottomSheetDialog

class GroceryAdapter(
    private var items: List<Pair<GroceryItemModel, IngredientModel?>>,
    private val onEditClick: (GroceryItemModel) -> Unit,
    private val onDeleteClick: (GroceryItemModel) -> Unit,
    private val onMoveClick: (GroceryItemModel) -> Unit,
    private val onMoveAllClick: (GroceryItemModel) -> Unit,
) : RecyclerView.Adapter<GroceryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvIngredientName)
        val tvCategory: TextView = view.findViewById(R.id.tvCategory)
        val tvQuantity: TextView = view.findViewById(R.id.tvQuantity)
        val btnMore: View = view.findViewById(R.id.btnMore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_grocery, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (item, ing) = items[position]
        val context = holder.itemView.context

        holder.tvName.text = ing?.name ?: context.getString(R.string.unknown)
        holder.tvCategory.text = ing?.category ?: context.getString(R.string.other)
        holder.tvQuantity.text = context.getString(
            R.string.qty_format,
            item.quantity.toString(),
            ing?.unitOfMeasure ?: ""
        )

        holder.btnMore.setOnClickListener { view ->
            showActionSheet(view.context, item, ing)
        }
    }

    private fun showActionSheet(context: Context, item: GroceryItemModel, ing: IngredientModel?) {
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

        // ── Item header (name + category) ──
        root.addView(buildHeader(context, ing))

        // ── Divider ──
        root.addView(divider(context))

        // ── Menu options ──
        root.addView(buildOption(
            context,
            iconRes    = R.drawable.ic_pencil,
            iconBg     = 0xFFE8F4ED.toInt(),            // Very light green
            iconTint   = 0xFF2C5F3A.toInt(),            // Forest green
            title      = context.getString(R.string.edit_amount),
            subtitle   = "Change quantity"
        ) { onEditClick(item); bottomSheet.dismiss() })

        root.addView(buildOption(
            context,
            iconRes    = R.drawable.ic_building_store,
            iconBg     = 0xFFFFF4E0.toInt(),            // Very light amber
            iconTint   = 0xFF855E28.toInt(),            // Dark amber
            title      = context.getString(R.string.move_amount_to_pantry),
            subtitle   = "Transfer ${item.quantity} ${ing?.unitOfMeasure ?: ""}"
        ) { onMoveClick(item); bottomSheet.dismiss() })

        root.addView(buildOption(
            context,
            iconRes    = R.drawable.ic_transfer,
            iconBg     = 0xFFE8F4ED.toInt(),
            iconTint   = 0xFF2C5F3A.toInt(),
            title      = context.getString(R.string.move_all_to_pantry_menu),
            subtitle   = "Move entire stock"
        ) { onMoveAllClick(item); bottomSheet.dismiss() })

        // ── Divider before delete ──
        root.addView(divider(context))

        root.addView(buildOption(
            context,
            iconRes    = R.drawable.ic_trash,
            iconBg     = 0xFFFCE8E8.toInt(),            // Very light red
            iconTint   = 0xFFC0392B.toInt(),            // Danger red
            title      = context.getString(R.string.delete_item),
            subtitle   = "Remove from list",
            titleColor = 0xFFC0392B.toInt()
        ) { onDeleteClick(item); bottomSheet.dismiss() })

        bottomSheet.setContentView(root)
        bottomSheet.show()
    }

    // ── Header showing item name + category ──────────────────────────────────
    private fun buildHeader(context: Context, ing: IngredientModel?): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(context, 24), dpToPx(context, 8), dpToPx(context, 24), dpToPx(context, 16))

            addView(TextView(context).apply {
                text = ing?.name ?: context.getString(R.string.unknown)
                textSize = 18f
                typeface = Typeface.defaultFromStyle(Typeface.BOLD)
                setTextColor(0xFF2E2E2E.toInt())
            })
            addView(TextView(context).apply {
                text = "${ing?.category ?: context.getString(R.string.other)}"
                textSize = 14f
                setTextColor(0xFF7A7066.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.topMargin = dpToPx(context, 2) }
            })
        }
    }

    // ── Single action row ────────────────────────────────────────────────────
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
            setPadding(
                dpToPx(context, 24), dpToPx(context, 12),
                dpToPx(context, 24), dpToPx(context, 12)
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            // Ripple on tap
            val ripple = TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackground, ripple, true)
            foreground = ContextCompat.getDrawable(context, ripple.resourceId)
            isClickable = true
            isFocusable  = true

            // ── Icon box ──
            addView(LinearLayout(context).apply {
                gravity = Gravity.CENTER
                background = roundedBackground(iconBg, cornerPx)
                layoutParams = LinearLayout.LayoutParams(iconSizePx, iconSizePx).also {
                    it.marginEnd = dpToPx(context, 16)
                }
                addView(android.widget.ImageView(context).apply {
                    setImageResource(iconRes)
                    setColorFilter(iconTint)
                    layoutParams = LinearLayout.LayoutParams(
                        dpToPx(context, 24), dpToPx(context, 24)
                    )
                })
            })

            // ── Text block ──
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

            // ── Chevron ──
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

    // ── Helpers ──────────────────────────────────────────────────────────────
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

    private fun roundedBackground(color: Int, radiusPx: Float): android.graphics.drawable.GradientDrawable {
        return android.graphics.drawable.GradientDrawable().apply {
            setColor(color)
            cornerRadius = radiusPx
        }
    }

    private fun dpToPx(context: Context, dp: Int): Int =
        (dp * context.resources.displayMetrics.density).toInt()

    // ── Adapter boilerplate ──────────────────────────────────────────────────
    override fun getItemCount() = items.size

    fun updateData(newItems: List<Pair<GroceryItemModel, IngredientModel?>>) {
        items = newItems
        notifyDataSetChanged()
    }
}