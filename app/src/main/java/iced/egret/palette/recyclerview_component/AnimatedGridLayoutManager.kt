package iced.egret.palette.recyclerview_component

import android.content.Context
import android.util.Log
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Typically, the app will crash if an item is selected from a section
 * other than the first or last section. Specifically, the animations associated with
 * selection cause the crash.
 *
 * SectionedRecyclerViewAdapter and RecyclerView-Animations don't seem to play well with
 * each other.
 *
 * This class is a hack to resolve the issue.
 * https://stackoverflow.com/a/38611931
 */
class AnimatedGridLayoutManager(context: Context, spanSize: Int) : GridLayoutManager(context, spanSize) {

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State) {
        try {
            super.onLayoutChildren(recycler, state)
        } catch (e: IndexOutOfBoundsException) {
            Log.i("Error", "IndexOutOfBoundsException in RecyclerView happens")
        }
    }
}