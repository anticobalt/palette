package iced.egret.palette.recyclerview_component

import android.graphics.Color
import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible
import iced.egret.palette.R
import iced.egret.palette.model.Coverable

abstract class CoverableItem(private val obj: Coverable) : AbstractFlexibleItem<CoverViewHolder>() {

    var viewHolder: CoverViewHolder? = null

    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
                                holder: CoverViewHolder,
                                position: Int,
                                payloads: MutableList<Any>?) {
        viewHolder = holder
        obj.loadCoverInto(holder)
        holder.tvItem?.text = obj.name
    }

    override fun equals(other: Any?): Boolean {
        return this === other
    }

    override fun hashCode(): Int {
        return obj.hashCode()
    }

    fun toggleSelection() {
        val statusView = viewHolder?.itemView?.findViewById<ImageView>(R.id.ivCollectionItemSelectStatus) ?: return

        if (statusView.visibility == View.GONE) {
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

    fun clearSelection() {
        val statusView = viewHolder?.itemView?.findViewById<ImageView>(R.id.ivCollectionItemSelectStatus) ?: return
        statusView.visibility = View.GONE
        viewHolder!!.ivItem?.colorFilter = null
    }

}