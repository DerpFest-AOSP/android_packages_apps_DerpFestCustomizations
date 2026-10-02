/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.widget

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import org.derpfest.support.colorpicker.SecureSettingColorPickerPreference

/** Secure-settings color pref that opens [HsvColorPickerDialog] instead of the legacy picker. */
class HsvColorPickerSecurePreference(
    context: Context,
    attrs: AttributeSet?,
) : SecureSettingColorPickerPreference(context, attrs) {

    override fun showDialog(state: Bundle?) {
        openHsvPicker(displayColor)
    }
}
