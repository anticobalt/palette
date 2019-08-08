package iced.egret.palette.activity.inherited

import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import iced.egret.palette.R

/**
 * Basic activity that has clickable RecyclerView items. Implementations need to declare/build these
 * items themselves and respond to them.
 */
abstract class RecyclerViewActivity : SlideActivity(), FlexibleAdapter.OnItemClickListener {

    protected lateinit var mRecyclerView: RecyclerView
    protected lateinit var mToolbar: Toolbar

    abstract fun fetchContents()
    abstract fun buildToolbar()
    abstract fun buildRecyclerView()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recycler_view)
        mToolbar = findViewById(R.id.toolbar)

        fetchContents()
        buildToolbar()
        buildRecyclerView()

        colorStandardElements(mToolbar)
    }

}