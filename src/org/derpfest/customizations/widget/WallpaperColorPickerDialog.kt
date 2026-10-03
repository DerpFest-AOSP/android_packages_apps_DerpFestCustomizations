/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.widget

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.android.settings.R
import com.android.settingslib.spa.framework.theme.SettingsDimension
import com.android.settingslib.spa.framework.theme.SettingsShape
import com.android.settingslib.spa.framework.theme.SettingsTheme
import com.github.skydoves.colorpicker.compose.AlphaTile
import com.github.skydoves.colorpicker.compose.ImageColorPicker
import com.github.skydoves.colorpicker.compose.PaletteContentScale
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import java.util.function.Consumer

private val PickerShape = SettingsShape.CornerExtraLarge1
private val HeroShape = SettingsShape.CornerExtraLarge1

/** Wallpaper-backed picker: the selected color is the wallpaper pixel, not a brightness-adjusted one. */
object WallpaperColorPickerDialog {

    @JvmStatic
    fun show(
        context: Context,
        wallpaper: Drawable,
        initialColor: Int,
        title: CharSequence,
        onColorPicked: Consumer<Int>,
    ) {
        showComposeColorPickerDialog(context, initialColor, title, onColorPicked) { onColorChanged ->
            val controller = rememberColorPickerController()
            val palette = remember(wallpaper) { wallpaper.toImageBitmap() }
            var selected by remember { mutableIntStateOf(initialColor) }

            Column {
                ImageColorPicker(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .pickerSurface(),
                    controller = controller,
                    paletteImageBitmap = palette,
                    paletteContentScale = PaletteContentScale.CROP,
                    onColorChanged = { envelope ->
                        selected = envelope.color.toArgb()
                        onColorChanged(selected)
                    },
                )
                ColorHero(
                    color = Color(selected),
                    showAlpha = false,
                    modifier = Modifier.padding(top = 20.dp),
                )
            }
        }
    }
}

@Composable
private fun ColorHero(
    color: Color,
    showAlpha: Boolean,
    modifier: Modifier = Modifier,
) {
    val animatedColor by animateColorAsState(targetValue = color, label = "hero")
    val hex = if (showAlpha) {
        animatedColor.toArgb().toHexArgb()
    } else {
        animatedColor.toArgb().toHexRgb()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(HeroShape),
    ) {
        if (showAlpha) {
            AlphaTile(
                modifier = Modifier.fillMaxSize(),
                selectedColor = animatedColor,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(animatedColor),
            )
        }
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp),
            shape = SettingsShape.CornerMedium,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        ) {
            Text(
                text = hex,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun Modifier.pickerSurface(): Modifier {
    val outline = MaterialTheme.colorScheme.outlineVariant
    return clip(PickerShape).border(1.dp, outline, PickerShape)
}

private fun showComposeColorPickerDialog(
    context: Context,
    initialColor: Int,
    title: CharSequence,
    onColorPicked: Consumer<Int>,
    content: @Composable (onColorChanged: (Int) -> Unit) -> Unit,
) {
    val selectedColor = intArrayOf(initialColor)
    val dialog = AlertDialog.Builder(context, R.style.QsTileIconShapeDialogTheme)
        .setTitle(title)
        .setNegativeButton(android.R.string.cancel, null)
        .setPositiveButton(android.R.string.ok) { _, _ ->
            onColorPicked.accept(selectedColor[0])
        }
        .create()
    val composeView = ComposeView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        context.findLifecycleOwner()?.let { owner ->
            setViewTreeLifecycleOwner(owner)
            if (owner is SavedStateRegistryOwner) {
                setViewTreeSavedStateRegistryOwner(owner)
            }
        }
        setContent {
            SettingsTheme {
                Box(
                    modifier = Modifier.padding(
                        horizontal = 32.dp,
                        vertical = SettingsDimension.paddingSmall,
                    ),
                ) {
                    content { selectedColor[0] = it }
                }
            }
        }
    }
    dialog.setView(composeView)
    dialog.window?.let { window ->
        val density = context.resources.displayMetrics.density
        val maxWidthPx = (400 * density + 0.5f).toInt()
        val screenWidth = context.resources.displayMetrics.widthPixels
        val lp = window.attributes
        lp.width = minOf(maxWidthPx, (screenWidth * 0.92f).toInt())
        window.attributes = lp
    }
    val surfaceBlur = DialogSurfaceBlur(context)
    dialog.setOnShowListener {
        surfaceBlur.attach(dialog)
        dialog.window?.decorView?.let { clearOpaqueBackgrounds(it, composeView) }
    }
    dialog.setOnDismissListener {
        surfaceBlur.detach()
    }
    dialog.show()
}

private fun clearOpaqueBackgrounds(root: View, excludeSubtree: View) {
    if (root !is ViewGroup) {
        return
    }
    for (i in 0 until root.childCount) {
        clearOpaqueBackgroundsRecursive(root.getChildAt(i), excludeSubtree)
    }
}

private fun clearOpaqueBackgroundsRecursive(view: View, excludeSubtree: View) {
    if (view === excludeSubtree || view is android.widget.Button) {
        return
    }
    view.setBackgroundResource(android.R.color.transparent)
    if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            clearOpaqueBackgroundsRecursive(view.getChildAt(i), excludeSubtree)
        }
    }
}

private fun Int.toHexRgb(): String = "#%06X".format(0xFFFFFF and this)

private fun Int.toHexArgb(): String = "#%08X".format(this)

private fun Drawable.toImageBitmap() = when {
    this is BitmapDrawable && bitmap != null -> bitmap.asImageBitmap()
    else -> {
        val width = intrinsicWidth.coerceAtLeast(1)
        val height = intrinsicHeight.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
        bitmap.asImageBitmap()
    }
}

private tailrec fun Context.findLifecycleOwner(): LifecycleOwner? = when (this) {
    is LifecycleOwner -> this
    is ContextWrapper -> baseContext.findLifecycleOwner()
    else -> null
}
