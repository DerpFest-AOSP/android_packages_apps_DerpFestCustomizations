/*
 * Copyright (C) 2022 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.fragment

import android.os.Bundle
import android.provider.Settings

import com.android.settings.R

import org.derpfest.support.preferences.AppListFragment

class HeadsUpBlacklistSettings : AppListFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setDisplayCategory(CATEGORY_BOTH)
        val whiteListedPackages = requireContext().resources.getStringArray(
            R.array.config_headsUpConfAllowedSystemApps)
        setCustomFilter {
            !it.applicationInfo!!.isSystemApp() || whiteListedPackages.contains(it.packageName)
        }
    }

    override protected fun getTitle(): Int {
        return R.string.heads_up_blacklist_title
    }

    override protected fun getInitialCheckedList(): List<String> {
        val packageList = Settings.System.getString(
            requireContext().contentResolver,
            Settings.System.HEADS_UP_BLACKLIST_VALUES
        )
        return packageList?.takeIf { it.isNotBlank() }?.split("|") ?: emptyList()
    }

    override protected fun onListUpdate(list: List<String>) {
        Settings.System.putString(
            requireContext().contentResolver,
            Settings.System.HEADS_UP_BLACKLIST_VALUES,
            list.joinToString("|")
        )
    }
}
