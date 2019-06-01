package iced.egret.palette

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import kotlinx.android.synthetic.main.collection_recyclerview_item.view.*

class CollectionRecyclerViewAdapter(private var mItems: MutableList<Collection>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class OnItemClickListener {
        fun onItemClick(item: Collection, adapter: CollectionRecyclerViewAdapter) {
            adapter.update(item.getCollections())
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivItem : ImageView? = view.iv_item
        val tvItem : TextView? = view.tv_item
    }

    private val mListener = OnItemClickListener()

    override fun getItemCount(): Int {
        return mItems.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.collection_recyclerview_item, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder) {
            val item = CollectionManager.getCollectionByPosition(position)
            holder.ivItem?.setImageResource(mItems[position].mCoverId)
            holder.tvItem?.text = mItems[position].name
            holder.itemView.setOnClickListener{
                mListener.onItemClick(item, this)
            }
        }
    }

    fun update(items: MutableList<Collection>) {
        // mItems = items doesn't work
        // Need to clear + allAll, or onclick will refer to old items
        mItems.clear()
        mItems.addAll(items)
        notifyDataSetChanged()
    }

}