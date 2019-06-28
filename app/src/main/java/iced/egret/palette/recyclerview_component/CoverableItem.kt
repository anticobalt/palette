package iced.egret.palette.recyclerview_component

import android.graphics.Color
import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractSectionableItem
import eu.davidea.flexibleadapter.items.IFlexible
import iced.egret.palette.R
import iced.egret.palette.model.Coverable

/**
 * An item for an expandable section.
 * Has IFlexible and ISectionable functionality.
 */
abstract class CoverableItem(private val obj: Coverable, header: SectionHeaderItem) :
        AbstractSectionableItem<CoverViewHolder, SectionHeaderItem>(header) {

    var viewHolder: CoverViewHolder? = null
    private var isSelected = false

    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
                                holder: CoverViewHolder,
                                position: Int,
                                payloads: MutableList<Any>?) {
        viewHolder = holder
        obj.loadCoverInto(holder)
        holder.tvItem?.text = obj.name

        // Update selection indicator on recycling or re-creation
        setSelection(isSelected)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is CoverableItem) return false
        return this.obj == other.obj
    }

    override fun hashCode(): Int {
        return obj.hashCode()
    }

    /**
     * Turn indicator on or off based on current visibility
     */
    fun toggleSelection() {
        val statusView = viewHolder?.itemView?.findViewById<ImageView>(R.id.ivCollectionItemSelectStatus) ?: return
        if (statusView.visibility == View.GONE) setSelection(true)
        else setSelection(false)
    }

    /**
     * Update isSelected property, and try to draw selection indicator.
     * If can't draw (e.g. after rotation b/c Views not re-created yet),
     * they will be drawn in onBindViewHolder()
     *
     * @param selected turn on (true) or off (false)
     */
    fun setSelection(selected: Boolean) {
        isSelected = selected
        val statusView = viewHolder?.itemView?.findViewById<ImageView>(R.id.ivCollectionItemSelectStatus)
                ?: return

        if (selected) {
            statusView.visibility = View.VISIBLE
            viewHolder!!.ivItem?.setColorFilter(
                    Color.rgb(200, 200, 200),
                    android.graphics.PorterDuff.Mode.MULTIPLY
            )
        } else {
            statusView.visibility = View.GONE
            viewHolder!!.ivItem?.colorFilter = null
        }
    }

}