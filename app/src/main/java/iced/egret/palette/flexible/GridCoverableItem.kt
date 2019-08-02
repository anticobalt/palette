package iced.egret.palette.flexible

import iced.egret.palette.R
import iced.egret.palette.model.Coverable

class GridCoverableItem(obj: Coverable) : CoverableItem(obj, null) {

    override fun getLayoutRes(): Int {
        return R.layout.item_coverable_grid
    }

}