package iced.egret.palette.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import iced.egret.palette.R
import iced.egret.palette.model.Collection
import iced.egret.palette.model.Coverable
import iced.egret.palette.util.CollectionManager
import kotlinx.android.synthetic.main.item_pinned_collections.view.*
import java.lang.ref.WeakReference

class PinnedCollectionsAdapter(private val mCollections : MutableList<Collection>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class OnItemClickListener {
        fun onItemClick(item: Coverable, adapter: PinnedCollectionsAdapter, position: Int) {
            Toast.makeText(adapter.mContextReference.get(), "hey", Toast.LENGTH_SHORT).show()
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvItem : TextView? = view.pinnedCollectionLabel
    }

    private val mListener = OnItemClickListener()
    private lateinit var mContextReference : WeakReference<Context>

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        mContextReference = WeakReference(parent.context)
        return ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_pinned_collections, parent, false))
    }

    override fun getItemCount(): Int {
        return mCollections.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val context = mContextReference.get()
        if (holder is ViewHolder && context != null) {
            val item = CollectionManager.getCollectionByPosition(position)
            holder.tvItem?.text = item.name
            holder.itemView.setOnClickListener{
                mListener.onItemClick(item, this, position)
            }
        }
    }

}