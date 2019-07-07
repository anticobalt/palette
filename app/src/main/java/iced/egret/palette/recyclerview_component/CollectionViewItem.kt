package iced.egret.palette.recyclerview_component

import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import iced.egret.palette.R
import iced.egret.palette.model.Coverable
import iced.egret.palette.util.Painter

class CollectionViewItem(obj: Coverable) : CoverableItem(obj) {

    override fun getLayoutRes(): Int {
        return R.layout.item_view_collection
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): CoverViewHolder {
        return CoverViewHolder(view, adapter, imageViewId = R.id.ivCollectionItemImage, textViewId = R.id.tvCollectionItemText)
    }

    /**
     * Turn indicator on or off based on current visibility
     */
    override fun toggleSelection() {
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
    override fun setSelection(selected: Boolean) {
        isSelected = selected
        val statusView = viewHolder?.itemView?.findViewById<ImageView>(R.id.ivCollectionItemSelectStatus)
                ?: return

        if (selected) {
            statusView.visibility = View.VISIBLE
            Painter.darken(viewHolder?.ivItem ?: return, Painter.DARKEN_MODERATE)
        } else {
            statusView.visibility = View.GONE
            Painter.darken(viewHolder?.ivItem ?: return, null)
        }
    }

}