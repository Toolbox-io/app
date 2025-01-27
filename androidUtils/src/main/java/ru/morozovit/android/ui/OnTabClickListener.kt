package ru.morozovit.android.ui

import com.google.android.material.tabs.TabLayout

fun interface OnTabClickListener: TabLayout.OnTabSelectedListener {
    override fun onTabSelected(tab: TabLayout.Tab)
    override fun onTabUnselected(tab: TabLayout.Tab) {}
    override fun onTabReselected(tab: TabLayout.Tab) = onTabSelected(tab)
}