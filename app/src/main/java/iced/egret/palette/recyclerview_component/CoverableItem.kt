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
    private var pendingSetSelectionOn = false

    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
                                holder: CoverViewHolder,
                                position: Int,
                                payloads: MutableList<Any>?) {
        viewHolder = holder
        obj.loadCoverInto(holder)
        holder.tvItem?.text = obj.name
        if (pendingSetSelectionOn) setSelection(true)
    }

    override fun equals(other: Any?): Boolean {
        return this === other
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
     * Turn indicator on or off, or flag to turn on on ViewHolder bind in the future.
     * Flagging occurs after device rotation (i.e. onViewStateRestored() is called in fragment).
     *
     * @param selected turn on (true) or off (false)
     */
    fun setSelection(selected: Boolean) {
        val statusView = viewHolder?.itemView?.findViewById<ImageView>(R.id.ivCollectionItemSelectStatus)
        if (statusView == null) {
            pendingSetSelectionOn = true
            return
        }

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