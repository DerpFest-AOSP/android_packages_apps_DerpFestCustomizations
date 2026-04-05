/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.fragment

import com.android.internal.logging.nano.MetricsProto.MetricsEvent

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import android.widget.Toast

import java.util.function.Consumer

import androidx.preference.Preference
import androidx.preference.PreferenceCategory

import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import com.android.settingslib.widget.LayoutPreference

class About : SettingsPreferenceFragment(), Preference.OnPreferenceClickListener {

    private var mDerpLogo: LayoutPreference? = null
    private var mDeviceLinks: PreferenceCategory? = null
    private var mDeviceFW: Preference? = null
    private var mDeviceRecovery: Preference? = null
    private var mDerpHitToast: Toast? = null
    private var mEasterEggDialog: Dialog? = null
    private var mEasterEggWindowBg: Drawable? = null
    private var mEasterEggDecorBg: Drawable? = null
    private var mEasterEggBlurListener: Consumer<Boolean>? = null

    private var mDerpLogoHitCountdown = 10

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.about)

        val res = resources

        mDeviceLinks = findPreference("about_device_links")
        mDeviceFW = findPreference("device_fw")
        mDeviceRecovery = findPreference("device_recovery")

        val hasFWLink = res.getString(R.string.about_device_fw_link).isNotEmpty()
        if (!hasFWLink) {
            mDeviceFW?.isVisible = false
        }

        val hasRecoveryLink = res.getString(R.string.about_device_recovery_link).isNotEmpty()
        if (!hasRecoveryLink) {
            mDeviceRecovery?.isVisible = false
        }

        if (!hasFWLink && !hasRecoveryLink) {
            mDeviceLinks?.isVisible = false
        } else {
            var defaultBrowser: String? = null
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://"))
                val resolveInfo = activity?.packageManager?.resolveActivity(
                    browserIntent,
                    PackageManager.MATCH_DEFAULT_ONLY
                )
                defaultBrowser = resolveInfo?.activityInfo?.packageName
            } catch (e: Exception) {
                // nothing to do. defaultBrowser already set to null
            }

            // if we have no default browser set we disable the buttons and let the user know
            if (defaultBrowser == null) {
                mDeviceFW?.isEnabled = false
                mDeviceFW?.summary = res.getString(R.string.no_browser)
                mDeviceRecovery?.isEnabled = false
                mDeviceRecovery?.summary = res.getString(R.string.no_browser)
            }
        }

        mDerpLogo = findPreference("derp_logo")
        mDerpLogo?.onPreferenceClickListener = this
        mDerpHitToast = null
    }

    override fun onDestroyView() {
        mEasterEggDialog?.dismiss()
        mEasterEggDialog = null
        super.onDestroyView()
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        if (mDerpLogoHitCountdown > 0) {
            mDerpLogoHitCountdown--
            if (mDerpLogoHitCountdown == 0) {
                mDerpHitToast?.cancel()
                showEasterEggCelebration()
            } else {
                mDerpHitToast?.cancel()
                mDerpHitToast = Toast.makeText(
                    context,
                    resources.getQuantityString(
                        R.plurals.show_derped_countdown,
                        mDerpLogoHitCountdown,
                        mDerpLogoHitCountdown
                    ),
                    Toast.LENGTH_SHORT
                )
                mDerpHitToast?.show()
            }
        }
        return true
    }

    private fun showEasterEggCelebration() {
        if (!isAdded) {
            return
        }
        val ctx = context ?: return
        mEasterEggDialog?.dismiss()

        val content = LayoutInflater.from(ctx).inflate(R.layout.about_easter_egg_celebration, null)
        val title = content.findViewById<TextView>(R.id.easter_egg_title)
        val message = content.findViewById<TextView>(R.id.easter_egg_message)
        val gotIt = content.findViewById<View>(R.id.easter_egg_got_it)

        message.text = getString(R.string.derpd_done)

        val dialog = Dialog(ctx)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(content)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)
        dialog.window?.let { window ->
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            window.setBackgroundDrawable(ColorDrawable(0x00000000))
            setupEasterEggWindowBlur(window, ctx)
        }

        title.alpha = 0f
        title.scaleX = 0.5f
        title.scaleY = 0.5f
        message.alpha = 0f
        gotIt.alpha = 0f

        dialog.setOnShowListener {
            title.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(380)
                .setInterpolator(OvershootInterpolator(1.15f))
                .start()
            message.animate()
                .alpha(1f)
                .setStartDelay(120)
                .setDuration(280)
                .start()
            gotIt.animate()
                .alpha(1f)
                .setStartDelay(280)
                .setDuration(220)
                .start()
        }

        gotIt.setOnClickListener { view ->
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            dialog.dismiss()
        }

        dialog.setOnDismissListener {
            mEasterEggDialog?.window?.let { w ->
                mEasterEggBlurListener?.let { listener ->
                    w.windowManager.removeCrossWindowBlurEnabledListener(listener)
                }
            }
            mEasterEggBlurListener = null
            mEasterEggWindowBg = null
            mEasterEggDecorBg = null
            mEasterEggDialog = null
        }

        mEasterEggDialog = dialog
        dialog.show()
    }

    private fun setupEasterEggWindowBlur(window: Window, ctx: Context) {
        mEasterEggWindowBg = ctx.getDrawable(R.drawable.about_easter_egg_window_background)?.mutate()
        mEasterEggDecorBg = ctx.getDrawable(R.drawable.about_easter_egg_window_background)?.mutate()
        mEasterEggWindowBg?.let { window.setBackgroundDrawable(it) }
        mEasterEggDecorBg?.let { bg ->
            val decor = window.decorView
            decor.background = bg
            decor.clipToOutline = true
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.setBackgroundBlurRadius(EASTER_EGG_BG_BLUR_RADIUS)
        window.setDimAmount(DIM_AMOUNT_WITH_BLUR)
        mEasterEggWindowBg?.alpha = WINDOW_BG_ALPHA_WITH_BLUR
        mEasterEggDecorBg?.alpha = WINDOW_BG_ALPHA_WITH_BLUR
        val listener = Consumer<Boolean> { enabled -> updateEasterEggWindowForBlur(window, enabled) }
        mEasterEggBlurListener = listener
        window.windowManager.addCrossWindowBlurEnabledListener(listener)
        updateEasterEggWindowForBlur(window, window.windowManager.isCrossWindowBlurEnabled)
    }

    private fun updateEasterEggWindowForBlur(window: Window, blursEnabled: Boolean) {
        if (mEasterEggDialog?.isShowing != true) {
            return
        }
        val alpha = if (blursEnabled && EASTER_EGG_BG_BLUR_RADIUS > 0) {
            WINDOW_BG_ALPHA_WITH_BLUR
        } else {
            WINDOW_BG_ALPHA_NO_BLUR
        }
        mEasterEggWindowBg?.alpha = alpha
        mEasterEggDecorBg?.alpha = alpha
        window.setDimAmount(
            if (blursEnabled && EASTER_EGG_BG_BLUR_RADIUS > 0) {
                DIM_AMOUNT_WITH_BLUR
            } else {
                DIM_AMOUNT_NO_BLUR
            },
        )
        window.setBackgroundBlurRadius(EASTER_EGG_BG_BLUR_RADIUS)
        window.attributes = window.attributes
    }

    override fun getMetricsCategory(): Int = MetricsEvent.DERPFEST

    companion object {
        const val TAG = "DerpFestCustomizations"

        /** Same as [org.derpfest.customizations.preference.QsTileIconShapePreference] blur tuning. */
        private const val EASTER_EGG_BG_BLUR_RADIUS = 80
        private const val WINDOW_BG_ALPHA_WITH_BLUR = 105
        private const val WINDOW_BG_ALPHA_NO_BLUR = 255
        private const val DIM_AMOUNT_WITH_BLUR = 0.1f
        private const val DIM_AMOUNT_NO_BLUR = 0.4f
    }
}

