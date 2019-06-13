package iced.egret.palette.adapter

import android.content.Context
import androidx.viewpager.widget.ViewPager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import iced.egret.palette.R
import iced.egret.palette.fragment.CollectionViewFragment
import iced.egret.palette.model.Collection
import iced.egret.palette.model.Coverable
import iced.egret.palette.util.CollectionManager
import iced.egret.palette.util.MainFragmentManager
import java.lang.ref.WeakReference

class PinnedCollectionsAdapter(private val mCollections : MutableList<Collection>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class OnItemClickListener {
        fun onItemClick(item: Coverable) {
            val fragmentIndex = MainFragmentManager.COLLECTION_CONTENTS
            val fragment =
                    MainFragmentManager.getFragmentByIndex(fragmentIndex) as CollectionViewFragment
            val viewPager = fragment.activity?.findViewById<ViewPager>(R.id.viewpagerMainFragments)
            // FIXME: animate slower e.g https://stackoverflow.com/a/28297483
            viewPager?.setCurrentItem(fragmentIndex, true)
            CollectionManager.launch(item, fragment.adapter)
        }
    }

    private val mListener = OnItemClickListener()
    private lateinit var mContextReference : WeakReference<Context>

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CoverViewHolder {
        mContextReference = WeakReference(parent.context)
        return CoverViewHolder(
                LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_pinned_collections, parent, false),
                textViewId = R.id.tvPinnedCollectionLabel,
                imageViewId = R.id.ivPinnedCollectionCover
        )
    }

    override fun getItemCount(): Int {
        return mCollections.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val context = mContextReference.get()
        if (holder is CoverViewHolder && context != null) {
            val item = CollectionManager.getCollectionByPosition(position)
            item.loadCoverInto(holder)
            holder.tvItem?.text = item.name
            holder.itemView.setOnClickListener{
                mListener.onItemClick(item)
            }
        }
    }

}