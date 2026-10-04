/*
 * SPDX-FileCopyrightText: The uwuAOSP Project
 * SPDX-FileCopyrightText: The DerpFest Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.fragment

import android.provider.Settings
import com.android.settings.R
import com.android.settings.core.BaseAppListSettingsFragment

/**
 * Apps listed here keep the device attestation implementation instead of the configured keybox.
 * The package list is stored as a colon-separated value, matching AttestationService.
 */
class KeyboxExcludedAppsSettings : BaseAppListSettingsFragment() {

    override fun getTitleResId(): Int = R.string.keybox_excluded_apps_title

    override fun excludeSystemApps(): Boolean = false

    override fun getInitialCheckedList(): List<String> {
        val context = context ?: return emptyList()
        val value = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.KEYBOX_EXCLUDED_PACKAGES,
        )
        if (value.isNullOrBlank()) {
            return emptyList()
        }
        return value.split(SEPARATOR).filter { it.isNotBlank() }.distinct()
    }

    override fun onListUpdate(packageName: String, isChecked: Boolean) {
        val context = context ?: return
        val current = getInitialCheckedList().toMutableSet()
        if (isChecked) {
            current.add(packageName)
        } else {
            current.remove(packageName)
        }
        val value = current.filter { it.isNotBlank() }.distinct().sorted().joinToString(SEPARATOR)
        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.KEYBOX_EXCLUDED_PACKAGES,
            value.ifEmpty { null },
        )
    }

    companion object {
        private const val SEPARATOR = ":"
    }
}
