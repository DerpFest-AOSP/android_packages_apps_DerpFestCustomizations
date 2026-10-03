/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.widget

import android.content.Context
import android.util.AttributeSet
import org.derpfest.support.colorpicker.HsvColorPickerSystemPreference as SupportHsvColorPickerSystemPreference

/** Settings XML name for [SupportHsvColorPickerSystemPreference]. */
class HsvColorPickerSystemPreference(
    context: Context,
    attrs: AttributeSet?,
) : SupportHsvColorPickerSystemPreference(context, attrs)
