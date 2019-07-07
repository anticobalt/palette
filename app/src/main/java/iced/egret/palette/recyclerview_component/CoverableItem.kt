package iced.egret.palette.recyclerview_component

import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible
import iced.egret.palette.model.Coverable

/**
 * An item for an expandable section.
 * Has IFlexible and ISectionable functionality.
 */
abstract class CoverableItem(protected val obj: Coverable) :
        AbstractFlexibleItem<CoverViewHolder>() {

    var viewHolder: CoverViewHolder? = null
    protected var isSelected = false

    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
                                holder: CoverViewHolder,
                                position: Int,
                                payloads: MutableList<Any>?) {
        viewHolder = holder
        obj.loadCoverInto(holder)
        holder.tvItem?.text = obj.name

        // set the icon to discern type
        setIcon()

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
    abstract fun toggleSelection()

    /**
     * Update isSelected property, and try to draw selection indicator.
     * If can't draw (e.g. after rotation b/c Views not re-created yet),
     * they will be drawn in onBindViewHolder()
     *
     * @param selected turn on (true) or off (false)
     */
    abstract fun setSelection(selected: Boolean)

    protected abstract fun setIcon()

}