package iced.egret.palette

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.collection_recyclerview_item.view.*

class CollectionRecyclerViewAdapter(private val items: MutableList<Collection>, private val listener: OnItemClickListener) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {


    open class OnItemClickListener {
        open fun onItemClick(item: Collection) {}
    }


    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivItem : ImageView? = view.iv_item
        val tvItem : TextView? = view.tv_item
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.collection_recyclerview_item, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder) {
            val item = CollectionManager.getCollectionByPosition(position)
            holder.ivItem?.setImageResource(items[position].mCoverId)
            holder.tvItem?.text = items[position].name
            holder.itemView.setOnClickListener{
                listener.onItemClick(item)
            }
        }
    }

}