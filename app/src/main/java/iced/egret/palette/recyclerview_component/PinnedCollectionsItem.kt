package iced.egret.palette.recyclerview_component

import android.graphics.Color
import android.view.View
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import iced.egret.palette.R
import iced.egret.palette.model.Coverable

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
        // https://stackoverflow.com/a/15896811
        holder.ivItem?.setColorFilter(
                Color.rgb(200, 200, 200),
                android.graphics.PorterDuff.Mode.MULTIPLY
        )
    }

    /**
     * Turn indicator on or off based on current visibility
     */
    override fun toggleSelection() {
        val button = viewHolder?.itemView?.findViewById<ImageButton>(R.id.btnSelect) ?: return
        if (button.visibility == View.INVISIBLE) setSelection(true)
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
        val button = viewHolder?.itemView?.findViewById<ImageButton>(R.id.btnSelect) ?: return

        if (selected) {
            button.visibility = View.VISIBLE
        } else {
            button.visibility = View.INVISIBLE
        }
    }

}