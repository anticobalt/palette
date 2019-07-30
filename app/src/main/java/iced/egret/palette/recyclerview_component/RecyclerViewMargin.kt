package iced.egret.palette.recyclerview_component

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Used to add uniform margin to all recycler view items.
 * Copied from https://stackoverflow.com/a/44964515
 */
class RecyclerViewMargin(private val space : Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View,
                       parent: RecyclerView, state: RecyclerView.State) {

        outRect.left = space
        outRect.right = space
        outRect.bottom = space

        // Add top margin only for the first item to avoid double space between items
        if (parent.getChildLayoutPosition(view) == 0) {
            outRect.top = space
        } else {
            outRect.top = 0
        }
    }

}