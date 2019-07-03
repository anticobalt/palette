package iced.egret.palette.fragment

import androidx.fragment.app.Fragment

abstract class MainFragment : Fragment() {
    abstract fun setClicksBlocked(doBlock: Boolean)
    abstract fun onFragmentCreationFinished(fragment: MainFragment)
    abstract fun onBackPressed() : Boolean
}