package iced.egret.palette

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kotlinx.android.synthetic.main.collection_recyclerview_item.view.*

class CollectionRecyclerViewAdapter(private val context: Context, private val items: ArrayList<String>) :
        RecyclerView.Adapter<ViewHolder>() {

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context)
                .inflate(R.layout.collection_recyclerview_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.tvItem?.text = items[position]
    }

}

class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val tvItem : TextView? = view.tv_item
}