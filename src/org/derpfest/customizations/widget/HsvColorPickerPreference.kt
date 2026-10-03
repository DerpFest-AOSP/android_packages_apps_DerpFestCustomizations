/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.widget

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import org.derpfest.support.colorpicker.ColorPickerPreference
import org.derpfest.support.colorpicker.HsvColorPickerDialog

internal fun ColorPickerPreference.openHsvPicker(color: Int) {
    if (!isEnabled) {
        return
    }
    HsvColorPickerDialog.show(
        context,
        color,
        title ?: "",
        { picked -> onColorChanged(picked) },
        /* alphaSlider */ false,
    )
}

/** [ColorPickerPreference] that opens [HsvColorPickerDialog] instead of the legacy picker. */
class HsvColorPickerPreference(
    context: Context,
    attrs: AttributeSet?,
) : ColorPickerPreference(context, attrs) {

    override fun showDialog(state: Bundle?) {
        openHsvPicker(displayColor)
    }
}
