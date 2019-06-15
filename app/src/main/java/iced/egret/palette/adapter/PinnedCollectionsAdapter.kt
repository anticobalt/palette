package iced.egret.palette.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import iced.egret.palette.R
import iced.egret.palette.fragment.CollectionViewFragment
import iced.egret.palette.model.Coverable
import iced.egret.palette.util.CollectionManager
import iced.egret.palette.util.MainFragmentManager
import java.lang.ref.WeakReference

class PinnedCollectionsAdapter(selector: LongClickSelector) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    /**
     * Responds to click actions, by changing display or launching items.
     */
    class ActionClickListener : CoverableClickListener() {

        private var item: Coverable? = null
        private var position: Int? = null
        private var holder: CoverViewHolder? = null

        private var ready = false

        override fun setup(item: Coverable, position: Int, holder: CoverViewHolder) {
            this.item = item
            this.position = position
            this.holder = holder
            this.ready = true
        }

        override fun tearDown() {
            this.item = null
            this.position = null
            this.holder = null
            this.ready = false
        }

        override fun onItemDefaultClick(selectedItemIds: ArrayList<Long>) {
            if (!ready) return

            val fragmentIndex = MainFragmentManager.COLLECTION_CONTENTS
            val fragment =
                    MainFragmentManager.getFragmentByIndex(fragmentIndex) as CollectionViewFragment
            val viewPager = fragment.activity?.findViewById<ViewPager>(R.id.viewpagerMainFragments)
            // FIXME: animate slower e.g https://stackoverflow.com/a/28297483
            viewPager?.setCurrentItem(fragmentIndex, true)
            CollectionManager.clearStack()
            CollectionManager.launch(item!!, fragment.adapter)
            fragment.setToolbarTitle()
        }

        override fun onItemDefaultLongClick(selectedItemIds: ArrayList<Long>) {
            if (!ready) return
            if (!item!!.deletable) return

            val positionLong = position!!.toLong()
            if (positionLong in selectedItemIds) {
                indicateSelection(holder!!, false)
                selectedItemIds.remove(positionLong)
            }
            else {
                indicateSelection(holder!!, true)
                selectedItemIds.add(positionLong)
            }
        }

        override fun onItemAlternateClick(selectedItemIds: ArrayList<Long>) {
            if (!ready) return
            onItemDefaultLongClick(selectedItemIds)
        }

        override fun onItemAlternateLongClick(selectedItemIds: ArrayList<Long>) {}

        private fun indicateSelection(holder: CoverViewHolder, selected: Boolean) {
            val button = holder.itemView.findViewById<ImageButton>(R.id.btnSelect)
            if (selected) button.visibility = View.VISIBLE
            else button.visibility = View.INVISIBLE
        }

    }

    private var mCollections = CollectionManager.getCollections()
    private lateinit var mContextReference : WeakReference<Context>

    private val mClickListener = ActionClickListener()
    private var mSelector = selector

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
            buildHolder(holder, item)
            indicateSelection(holder, position, item)
            holder.itemView.setOnClickListener{
                mSelector.onItemClick(item, position, holder, mClickListener)
            }
            holder.itemView.setOnLongClickListener {
                mSelector.onItemLongClick(item, position, holder, mClickListener)
            }
        }
    }

    /**
     * Fill holder with mandatory elements
     */
    private fun buildHolder(holder: CoverViewHolder, item: Coverable) {
        item.loadCoverInto(holder)
        holder.tvItem?.text = item.name
    }

    /**
     * Style holder to match various selection states (e.g. is selected, selectable, etc)
     */
    private fun indicateSelection(holder: CoverViewHolder, position: Int, item: Coverable) {

        val button = holder.itemView.findViewById<ImageButton>(R.id.btnSelect)

        if (position.toLong() in mSelector.selectedItemIds) {
            button.visibility = View.VISIBLE
        }
        else {
            button.visibility = View.INVISIBLE
        }

        if (mSelector.active && !item.deletable) {
            holder.itemView.alpha = 0.5F
        }
        else {
            holder.itemView.alpha = 1F
        }
    }

    fun updateCollections() {
        mCollections.clear()
        mCollections.addAll(CollectionManager.getCollections())
        notifyDataSetChanged()
    }

}