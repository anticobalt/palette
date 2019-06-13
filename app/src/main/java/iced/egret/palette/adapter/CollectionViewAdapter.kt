package iced.egret.palette.adapter

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import iced.egret.palette.R
import iced.egret.palette.model.Coverable
import iced.egret.palette.util.CollectionManager
import java.lang.ref.WeakReference


class CollectionViewAdapter :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class OnItemClickListener {
        fun onItemClick(item: Coverable, adapter: CollectionViewAdapter, position: Int) {
            CollectionManager.launch(item, adapter, position)
        }
    }

    private var mItems: MutableList<Coverable> = CollectionManager.getContents()
    private val mListener = OnItemClickListener()
    private lateinit var mContextReference : WeakReference<Context>

    override fun getItemCount(): Int {
        return mItems.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CoverViewHolder {
        mContextReference = WeakReference(parent.context)
        return CoverViewHolder(
                LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_view_collection, parent, false),
                imageViewId = R.id.ivCollectionItemImage,
                textViewId = R.id.tvCollectionItemText
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val context = mContextReference.get()
        if (holder is CoverViewHolder && context != null) {
            val item = mItems[position]
            item.loadCoverInto(holder)
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