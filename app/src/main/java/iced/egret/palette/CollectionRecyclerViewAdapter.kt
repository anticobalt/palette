package iced.egret.palette

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import kotlinx.android.synthetic.main.item_view_collection.view.*
import java.lang.ref.WeakReference

class CollectionRecyclerViewAdapter(private var mItems: MutableList<Coverable>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class OnItemClickListener {
        fun onItemClick(item: Coverable, adapter: CollectionRecyclerViewAdapter, position: Int) {
            CollectionManager.launch(item, adapter, position)
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivItem : ImageView? = view.iv_item
        val tvItem : TextView? = view.tv_item
    }

    private val mListener = OnItemClickListener()
    private lateinit var mContextReference : WeakReference<Context>

    override fun getItemCount(): Int {
        return mItems.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        mContextReference = WeakReference(parent.context)
        return ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_view_collection, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val context = mContextReference.get()
        if (holder is ViewHolder && context != null) {
            val item = CollectionManager.getContentByPosition(position)
            item.loadCoverInto(holder, context)
            holder.tvItem?.text = item.name
            holder.itemView.setOnClickListener{
                mListener.onItemClick(item, this, position)
            }
        }
    }

    fun update(items: MutableList<Coverable>) {
        // mItems = items doesn't work
        // Need to clear + allAll, or onclick will refer to old items
        mItems.clear()
        mItems.addAll(items)
        notifyDataSetChanged()
    }

    fun getContext() : Context? {
        return mContextReference.get()
    }

}