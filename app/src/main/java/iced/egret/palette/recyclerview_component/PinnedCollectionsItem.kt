package iced.egret.palette.recyclerview_component

import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import iced.egret.palette.R
import iced.egret.palette.model.Coverable
import iced.egret.palette.util.Painter

class PinnedCollectionsItem(obj: Coverable) : CoverableItem(obj) {

    override fun getLayoutRes(): Int {
        return R.layout.item_pinned_collections
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): CoverViewHolder {
        return CoverViewHolder(view, adapter, imageViewId = R.id.ivPinnedCollectionCover, textViewId = R.id.tvPinnedCollectionLabel)
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
                                holder: CoverViewHolder,
                                position: Int,
                                payloads: MutableList<Any>?) {

        // bind holder as normal
        super.bindViewHolder(adapter, holder, position, payloads)

        // Darken a little, so that white text is readable
        Painter.darken(holder.ivItem ?: return, Painter.DARKEN_SLIGHT)
    }

    /**
     * Turn indicator on or off based on current visibility
     */
    override fun toggleSelection() {
        val selectView = viewHolder?.itemView?.findViewById<ImageView>(R.id.ivPinnedCollectionSelectStatus) ?: return
        if (selectView.visibility == View.INVISIBLE) setSelection(true)
        else setSelection(false)
    }

    /**
     * Update isSelected property, and try to draw selection indicator.
     * If can't draw (e.g. after rotation b/c Views not re-created yet),
     * they will be drawn in onBindViewHolder()
     *
     * @param selected turn on (true) or off (false)
     */
    override fun setSelection(selected: Boolean) {
        isSelected = selected
        val selectView = viewHolder?.itemView?.findViewById<ImageView>(R.id.ivPinnedCollectionSelectStatus) ?: return

        if (selected) {
            selectView.visibility = View.VISIBLE
            Painter.darken(viewHolder?.ivItem ?: return, Painter.DARKEN_MODERATE)
        } else {
            selectView.visibility = View.INVISIBLE
            Painter.darken(viewHolder?.ivItem ?: return, Painter.DARKEN_SLIGHT)
        }
    }

    /**
     * Set the little icon that indicates the type of the Coverable.
     */
    override fun setIcon() {
        val typeView = viewHolder?.itemView?.findViewById<ImageView>(R.id.ivPinnedCollectionType)
                ?: return
        if (obj.icon == null) {
            typeView.setImageDrawable(null)
        } else {
            typeView.setImageResource(obj.icon ?: return)
        }
    }

}