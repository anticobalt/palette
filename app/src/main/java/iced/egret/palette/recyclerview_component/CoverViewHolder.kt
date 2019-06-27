package iced.egret.palette.recyclerview_component

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.viewholders.FlexibleViewHolder

// TODO: remove default value
class CoverViewHolder(view : View,
                      adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>> = FlexibleAdapter(null),
                      textViewId : Int,
                      imageViewId : Int) : FlexibleViewHolder(view, adapter) {

    val tvItem : TextView? = view.findViewById(textViewId)
    val ivItem : ImageView? = view.findViewById(imageViewId)

}