package iced.egret.palette.itemdecoration

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Used to add uniform margin to PinnedCollectionItems. Assumes one column.
 */
class PinnedCollectionMargin(private val space : Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        outRect.left = space
        outRect.right = space
        outRect.top = space
    }

}