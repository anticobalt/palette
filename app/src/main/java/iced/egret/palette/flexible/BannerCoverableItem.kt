package iced.egret.palette.flexible

import iced.egret.palette.R
import iced.egret.palette.model.Coverable
import iced.egret.palette.util.Painter

class BannerCoverableItem(obj: Coverable) : CoverableItem(obj, Painter.DARKEN_SLIGHT) {

    override fun getLayoutRes(): Int {
        return R.layout.item_coverable_banner
    }

}