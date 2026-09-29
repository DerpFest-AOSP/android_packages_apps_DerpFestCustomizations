/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.fragment

import android.content.Context
import android.graphics.Typeface
import android.graphics.fonts.FontManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import com.android.internal.logging.nano.MetricsProto.MetricsEvent
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.search.SearchIndexable
import com.android.settingslib.widget.LayoutPreference
import java.util.concurrent.Executors
import org.derpfest.customizations.font.CustomFontController
import org.derpfest.customizations.font.FontCandidate
import org.derpfest.customizations.font.FontImportResult
import org.derpfest.customizations.font.FontOperationResult

@SearchIndexable
class Fonts : SettingsPreferenceFragment() {

    private val ioExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var controller: CustomFontController

    private var fonts: List<FontCandidate> = emptyList()
    private var selectedId: String? = null
    private var activeFont: String? = null
    private var operationRunning = false

    private val picker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                importFont(uri)
            }
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.fonts)
        controller = CustomFontController(requireContext())
        selectedId = savedInstanceState?.getString(STATE_SELECTED_ID)
        findPreference<LayoutPreference>(KEY_IMPORT)
            ?.findViewById<View>(R.id.custom_font_import_row)
            ?.setOnClickListener {
                if (!operationRunning) {
                    picker.launch(FONT_MIME_TYPES)
                }
            }
        findPreference<Preference>(KEY_RESTORE)?.setOnPreferenceClickListener {
            if (!operationRunning) {
                restoreDefault()
            }
            true
        }
        findPreference<LayoutPreference>(KEY_APPLY)?.findViewById<View>(R.id.custom_font_apply)
            ?.setOnClickListener {
                if (!operationRunning) {
                    applySelected()
                }
            }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_SELECTED_ID, selectedId)
    }

    override fun onResume() {
        super.onResume()
        refreshFromController()
    }

    override fun onDestroy() {
        ioExecutor.shutdownNow()
        super.onDestroy()
    }

    override fun getMetricsCategory(): Int = MetricsEvent.DERPFEST

    private fun refreshFromController() {
        fonts = controller.preparedFonts()
        if (fonts.none { it.id == selectedId }) {
            selectedId = fonts.firstOrNull()?.id
        }
        activeFont = controller.activeFontName()
        bind()
    }

    private fun importFont(uri: Uri) {
        setOperationRunning(true)
        ioExecutor.execute {
            val result =
                runCatching { controller.prepare(uri) }.getOrDefault(FontImportResult.Failed)
            mainHandler.post {
                if (!isAdded) return@post
                setOperationRunning(false)
                when (result) {
                    is FontImportResult.Success -> {
                        fonts = result.fonts
                        selectedId = result.fonts.firstOrNull()?.id
                        bind()
                    }
                    FontImportResult.UnsupportedFile ->
                        toast(R.string.custom_font_file_invalid)
                    FontImportResult.NoFonts ->
                        toast(R.string.custom_font_archive_no_fonts)
                    FontImportResult.InvalidArchive ->
                        toast(R.string.custom_font_archive_invalid)
                    FontImportResult.TooLarge ->
                        toast(R.string.custom_font_archive_too_large)
                    FontImportResult.Failed ->
                        toast(R.string.custom_font_import_failed)
                }
            }
        }
    }

    private fun applySelected() {
        val font = fonts.firstOrNull { it.id == selectedId } ?: return
        setOperationRunning(true)
        ioExecutor.execute {
            val result =
                runCatching { controller.install(font) }
                    .getOrElse {
                        FontOperationResult.Failed(FontManager.RESULT_ERROR_INVALID_FONT_FILE)
                    }
            mainHandler.post {
                if (!isAdded) return@post
                setOperationRunning(false)
                when (result) {
                    is FontOperationResult.Success -> {
                        activeFont = result.displayName
                        bind()
                        toast(R.string.custom_font_apply_success)
                    }
                    is FontOperationResult.Failed ->
                        toast(fontOperationErrorMessage(result.errorCode), long = true)
                    FontOperationResult.InvalidFile ->
                        toast(R.string.custom_font_file_invalid)
                }
            }
        }
    }

    private fun restoreDefault() {
        setOperationRunning(true)
        ioExecutor.execute {
            val result =
                runCatching { controller.restoreDefault() }
                    .getOrDefault(FontManager.RESULT_ERROR_FAILED_UPDATE_CONFIG)
            mainHandler.post {
                if (!isAdded) return@post
                setOperationRunning(false)
                if (result == FontManager.RESULT_SUCCESS) {
                    activeFont = null
                    fonts = emptyList()
                    selectedId = null
                    bind()
                    toast(R.string.custom_font_restore_success)
                } else {
                    toast(fontOperationErrorMessage(result), long = true)
                }
            }
        }
    }

    private fun bind() {
        bindPreview()
        bindCandidates()
        val hasFonts = fonts.isNotEmpty()
        findPreference<Preference>(KEY_EMPTY)?.isVisible = !hasFonts
        findPreference<Preference>(KEY_AVAILABLE_CATEGORY)?.isVisible = hasFonts
        findPreference<Preference>(KEY_AVAILABLE)?.isVisible = hasFonts
        findPreference<LayoutPreference>(KEY_APPLY)?.isVisible = hasFonts
        val restore = findPreference<Preference>(KEY_RESTORE)
        val current = findPreference<Preference>(KEY_CURRENT)
        val hasActive = activeFont != null
        current?.isVisible = hasActive
        restore?.isVisible = hasActive
        restore?.summary =
            activeFont?.let { getString(R.string.custom_font_summary_active, it) }
                ?: getString(R.string.custom_font_restore_summary)
        findPreference<LayoutPreference>(KEY_IMPORT)
            ?.findViewById<View>(R.id.custom_font_import_row)
            ?.isEnabled = !operationRunning
        restore?.isEnabled = !operationRunning
        findPreference<LayoutPreference>(KEY_APPLY)?.findViewById<View>(R.id.custom_font_apply)
            ?.isEnabled = !operationRunning && selectedId != null
    }

    private fun bindPreview() {
        val preview = findPreference<LayoutPreference>(KEY_PREVIEW) ?: return
        val selected = fonts.firstOrNull { it.id == selectedId }
        val typeface = selected?.typeface ?: Typeface.DEFAULT
        preview.findViewById<TextView>(R.id.custom_font_preview_name)?.text =
            selected?.displayName ?: getString(R.string.custom_font_preview_default)
        listOf(
                R.id.custom_font_preview_headline,
                R.id.custom_font_preview_sample,
                R.id.custom_font_preview_digits,
                R.id.custom_font_preview_japanese,
            )
            .forEach { id ->
                preview.findViewById<TextView>(id)?.typeface = typeface
            }
    }

    private fun bindCandidates() {
        val listPreference = findPreference<LayoutPreference>(KEY_AVAILABLE) ?: return
        val container =
            listPreference.findViewById<LinearLayout>(R.id.custom_font_candidates) ?: return
        val inflater = LayoutInflater.from(container.context)
        container.removeAllViews()
        fonts.forEachIndexed { index, font ->
            val row =
                inflater.inflate(R.layout.custom_font_candidate, container, false)
            if (index > 0) {
                (row.layoutParams as LinearLayout.LayoutParams).topMargin =
                    resources.getDimensionPixelSize(R.dimen.custom_font_candidate_spacing)
            }
            row.findViewById<TextView>(R.id.custom_font_candidate_title).apply {
                text = font.displayName
                typeface = font.typeface
            }
            row.findViewById<TextView>(R.id.custom_font_candidate_summary).text =
                getString(
                    R.string.custom_font_candidate_summary,
                    font.sourceName,
                    font.weight,
                    getString(
                        if (font.italic) {
                            R.string.custom_font_style_italic
                        } else {
                            R.string.custom_font_style_normal
                        },
                    ),
                )
            row.findViewById<RadioButton>(R.id.custom_font_candidate_radio).isChecked =
                font.id == selectedId
            row.isEnabled = !operationRunning
            row.setOnClickListener {
                if (operationRunning) return@setOnClickListener
                selectedId = font.id
                bind()
            }
            container.addView(row)
        }
    }

    private fun setOperationRunning(running: Boolean) {
        operationRunning = running
        bind()
    }

    private fun toast(message: Int, long: Boolean = false) {
        Toast.makeText(
                requireContext(),
                message,
                if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT,
            )
            .show()
    }

    private fun fontOperationErrorMessage(errorCode: Int): Int {
        return when (errorCode) {
            FontManager.RESULT_ERROR_FAILED_TO_WRITE_FONT_FILE ->
                R.string.custom_font_error_storage
            FontManager.RESULT_ERROR_VERIFICATION_FAILURE ->
                R.string.custom_font_error_verification
            FontManager.RESULT_ERROR_INVALID_FONT_FILE ->
                R.string.custom_font_error_invalid_file
            FontManager.RESULT_ERROR_INVALID_FONT_NAME ->
                R.string.custom_font_error_invalid_name
            FontManager.RESULT_ERROR_DOWNGRADING ->
                R.string.custom_font_error_older_version
            FontManager.RESULT_ERROR_FAILED_UPDATE_CONFIG ->
                R.string.custom_font_error_update_config
            FontManager.RESULT_ERROR_FONT_UPDATER_DISABLED ->
                R.string.custom_font_error_unsupported
            FontManager.RESULT_ERROR_VERSION_MISMATCH ->
                R.string.custom_font_error_version_changed
            FontManager.RESULT_ERROR_FONT_NOT_FOUND ->
                R.string.custom_font_error_not_found
            else -> R.string.custom_font_error_rejected
        }
    }

    companion object {
        private const val KEY_PREVIEW = "custom_font_preview"
        private const val KEY_IMPORT = "custom_font_import"
        private const val KEY_AVAILABLE_CATEGORY = "custom_font_available_category"
        private const val KEY_AVAILABLE = "custom_font_available"
        private const val KEY_EMPTY = "custom_font_empty"
        private const val KEY_APPLY = "custom_font_apply"
        private const val KEY_CURRENT = "custom_font_current"
        private const val KEY_RESTORE = "custom_font_restore"
        private const val STATE_SELECTED_ID = "selected_id"

        private val FONT_MIME_TYPES =
            arrayOf(
                "font/ttf",
                "font/otf",
                "application/x-font-ttf",
                "application/x-font-opentype",
                "application/zip",
                "application/x-zip-compressed",
                "application/octet-stream",
            )

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER: BaseSearchIndexProvider =
            object : BaseSearchIndexProvider(R.xml.fonts) {
                override fun getNonIndexableKeys(context: Context): List<String> =
                    super.getNonIndexableKeys(context)
            }
    }
}
