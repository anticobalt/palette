package iced.egret.palette.flexible.item.inherited

import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible
import iced.egret.palette.R
import iced.egret.palette.flexible.viewholder.CoverViewHolder
import iced.egret.palette.model.inherited.Coverable
import iced.egret.palette.util.Painter

/**
 * An item with IFlexible functionality.
 */
abstract class CoverableItem(val coverable: Coverable, private var defaultTint: Int?,
                             private var iconRes: Int?) : AbstractFlexibleItem<CoverViewHolder>() {

    var viewHolder: CoverViewHolder? = null
    private var isSelected = false

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): CoverViewHolder {
        return CoverViewHolder(view, adapter)
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
                                holder: CoverViewHolder,
                                position: Int,
                                payloads: MutableList<Any>?) {
        viewHolder = holder
        coverable.loadCoverInto(holder)

        setLabel(holder)
        setIcon()  // set the icon to discern type

        // Update selection indicator on recycling or re-creation
        setSelection(isSelected)
    }

    abstract fun setLabel(holder: CoverViewHolder)

    override fun equals(other: Any?): Boolean {
        if (other !is CoverableItem) return false
        return this.coverable == other.coverable && this.layoutRes == other.layoutRes
    }

    override fun hashCode(): Int {
        return coverable.hashCode()
    }

    /**
     * Turn indicator on or off based on current visibility
     */
    fun toggleSelection() {
        val statusView = viewHolder?.itemView?.findViewById<ImageView>(R.id.selectCheckmark)
                ?: return
        if (statusView.visibility == View.GONE) setSelection(true)
        else setSelection(false)
    }

    /**
     * Update isSelected property, and try to draw selection indicator.
     * If can't draw (e.g. after rotation b/c Views not re-created yet) or won't,
     * they will be drawn in onBindViewHolder()
     *
     * @param selected turn on (true) or off (false)
     */
    fun setSelection(selected: Boolean, shouldDraw: Boolean = true) {
        isSelected = selected
        if (!shouldDraw) return
        val statusView = viewHolder?.itemView?.findViewById<ImageView>(R.id.selectCheckmark)
                ?: return

        if (selected) {
            statusView.visibility = View.VISIBLE
            Painter.darken(viewHolder?.ivItem ?: return, Painter.DARKEN_MODERATE)
        } else {
            statusView.visibility = View.GONE
            Painter.darken(viewHolder?.ivItem ?: return, defaultTint)
        }
    }

    /**
     * Set the little icon that is either provided, or indicates the type of the Coverable.
     */
    private fun setIcon() {
        val iconView = viewHolder?.itemView?.findViewById<ImageView>(R.id.icon)
                ?: return

        if (iconRes != null) {
            iconView.setImageResource(iconRes!!)
        } else {
            if (coverable.icon == null) {
                iconView.setImageDrawable(null)
            } else {
                iconView.setImageResource(coverable.icon ?: return)
            }
        }
    }
}