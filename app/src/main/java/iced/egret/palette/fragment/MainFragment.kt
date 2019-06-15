package iced.egret.palette.fragment

import androidx.fragment.app.Fragment

abstract class MainFragment : Fragment() {
    abstract fun onBackPressed() : Boolean
    abstract fun onAlternateModeActivated()
    abstract fun onAlternateModeDeactivated()
}