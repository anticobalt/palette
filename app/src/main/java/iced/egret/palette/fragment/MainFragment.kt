package iced.egret.palette.fragment

import androidx.fragment.app.Fragment
import io.github.luizgrp.sectionedrecyclerviewadapter.StatelessSection

abstract class MainFragment : Fragment() {
    abstract fun onBackPressed() : Boolean
    abstract fun onAlternateModeActivated(section: StatelessSection)
    abstract fun onAlternateModeDeactivated(section: StatelessSection)
}