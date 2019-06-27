package iced.egret.palette.recyclerview_component

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import iced.egret.palette.R
import iced.egret.palette.model.Coverable

class CollectionViewItem(private val obj: Coverable) : CoverableItem(obj) {

    override fun getLayoutRes(): Int {
        return R.layout.item_view_collection
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): CoverViewHolder {
        return CoverViewHolder(view, adapter, imageViewId = R.id.ivCollectionItemImage, textViewId = R.id.tvCollectionItemText)
    }

}