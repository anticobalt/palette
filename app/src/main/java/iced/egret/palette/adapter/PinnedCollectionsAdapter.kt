package iced.egret.palette.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import iced.egret.palette.R
import iced.egret.palette.fragment.CollectionViewFragment
import iced.egret.palette.model.Coverable
import iced.egret.palette.util.CollectionManager
import iced.egret.palette.util.MainFragmentManager
import java.lang.ref.WeakReference

class PinnedCollectionsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class OnItemClickListener {

        fun onItemClick(item: Coverable) {
            val fragmentIndex = MainFragmentManager.COLLECTION_CONTENTS
            val fragment =
                    MainFragmentManager.getFragmentByIndex(fragmentIndex) as CollectionViewFragment
            val viewPager = fragment.activity?.findViewById<ViewPager>(R.id.viewpagerMainFragments)
            // FIXME: animate slower e.g https://stackoverflow.com/a/28297483
            viewPager?.setCurrentItem(fragmentIndex, true)
            CollectionManager.clearStack()
            CollectionManager.launch(item, fragment.adapter)
            fragment.setToolbarTitle()
        }

        fun onItemLongClick(position: Int, holder: CoverViewHolder, adapter: PinnedCollectionsAdapter) : Boolean {
            val positionLong = position.toLong()
            if (positionLong in adapter.mSelectedItemIds) {
                holder.ivItem?.clearColorFilter()
                adapter.mSelectedItemIds.remove(positionLong)
            }
            else {
                holder.ivItem?.setColorFilter(R.color.translucentGrey)
                adapter.mSelectedItemIds.add(positionLong)
            }
            return true
        }

    }

    private var mCollections = CollectionManager.getCollections()
    private val mListener = OnItemClickListener()
    private lateinit var mContextReference : WeakReference<Context>

    private var mSelectedItemIds = ArrayList<Long>()

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

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

            val item = mCollections[position]
            item.loadCoverInto(holder)
            holder.tvItem?.text = item.name

            holder.itemView.setOnClickListener{
                mListener.onItemClick(item)
            }
            holder.itemView.setOnLongClickListener {
                mListener.onItemLongClick(position, holder, this)
            }

        }
    }



}