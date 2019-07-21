package iced.egret.palette.fragment

import androidx.fragment.app.Fragment

abstract class MainFragment : Fragment() {
    abstract fun setClicksBlocked(doBlock: Boolean)
    abstract fun onAllFragmentsCreated()
    abstract fun onBackPressed() : Boolean
    abstract fun applyThemeToAppBar(color: Int)
}