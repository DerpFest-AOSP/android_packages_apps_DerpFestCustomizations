/*
 * Copyright (C) 2022 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.fragment

import android.os.Bundle
import android.provider.Settings

import com.android.settings.R

import android.content.pm.LauncherActivityInfo
import com.android.settings.core.BaseAppListSettingsFragment

class HeadsUpStoplistSettings : BaseAppListSettingsFragment() {

    override fun getTitleResId(): Int = R.string.heads_up_stoplist_title

    override fun appFilter(info: LauncherActivityInfo): Boolean {
        val whiteListedPackages = requireContext().resources.getStringArray(
            R.array.config_headsUpConfAllowedSystemApps)
        return !info.applicationInfo!!.isSystemApp() ||
            whiteListedPackages.contains(info.componentName.packageName)
    }

    override fun getInitialCheckedList(): List<String> {
        val packageList = Settings.System.getString(
            requireContext().contentResolver,
            Settings.System.HEADS_UP_STOPLIST_VALUES
        )
        return packageList?.takeIf { it.isNotBlank() }?.split("|") ?: emptyList()
    }

    override fun onListUpdate(packageName: String, isChecked: Boolean) {
        val current = getInitialCheckedList().toMutableSet()
        if (isChecked) current.add(packageName) else current.remove(packageName)
        Settings.System.putString(
            requireContext().contentResolver,
            Settings.System.HEADS_UP_STOPLIST_VALUES,
            current.joinToString("|")
        )
    }
}
