/*
 * SPDX-FileCopyrightText: 2026 kenway214
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package org.derpfest.customizations.fragment

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settingslib.spa.framework.theme.SettingsSpace
import com.android.settingslib.spa.framework.theme.SettingsTheme
import com.android.settingslib.spa.widget.button.ActionButton
import com.android.settingslib.spa.widget.button.ActionButtons
import com.android.settingslib.spa.widget.preference.MainSwitchPreference
import com.android.settingslib.spa.widget.preference.SwitchPreferenceModel
import com.android.settingslib.spa.widget.preference.TwoTargetButtonPreference
import com.android.settingslib.spa.widget.preference.ZeroStatePreference
import com.android.settingslib.spa.widget.ui.Category
import com.android.settingslib.spa.widget.ui.CategoryTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.LinkedHashMap
import java.util.Locale

class AdvancedAppSpoofSettings : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().title = getString(R.string.advanced_app_spoof_title)
    }

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View {
        return androidx.compose.ui.platform.ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                androidx.compose.ui.platform.ViewCompositionStrategy
                    .DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                SettingsTheme {
                    AdvancedSpoofContent(requireContext())
                }
            }
        }
    }
}

private const val KEY_ENABLED = Settings.Secure.ADVANCED_APP_SPOOF_ENABLED
private const val KEY_CONFIG  = Settings.Secure.ADVANCED_APP_SPOOF_CONFIG

private data class AppEntry(
    val packageName: String,
    val label: String,
    val icon: Drawable,
    val isSystem: Boolean,
)

private data class SpoofRule(
    val pkg: String,
    val gpuKey: String,
    val cpuKey: String,
)

private fun readEnabled(ctx: Context): Boolean =
    Settings.Secure.getInt(ctx.contentResolver, KEY_ENABLED, 0) != 0

private fun writeEnabled(ctx: Context, v: Boolean) =
    Settings.Secure.putInt(ctx.contentResolver, KEY_ENABLED, if (v) 1 else 0)

private fun readRules(ctx: Context): MutableList<SpoofRule> {
    val json = Settings.Secure.getString(ctx.contentResolver, KEY_CONFIG) ?: return mutableListOf()
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).mapTo(mutableListOf()) { i ->
            val o = arr.getJSONObject(i)
            SpoofRule(
                pkg    = o.optString("pkg"),
                gpuKey = o.optString("gpu"),
                cpuKey = o.optString("cpu"),
            )
        }
    } catch (_: Exception) { mutableListOf() }
}

private fun writeRules(ctx: Context, rules: List<SpoofRule>) {
    val arr = JSONArray()
    rules.forEach { r ->
        arr.put(JSONObject().apply {
            put("pkg", r.pkg)
            put("gpu", r.gpuKey)
            put("cpu", r.cpuKey)
        })
    }
    Settings.Secure.putString(ctx.contentResolver, KEY_CONFIG, arr.toString())
}

private val GPU_DISPLAY: LinkedHashMap<String, String> = linkedMapOf(
    ""             to "None",
    "adreno840"    to "Adreno 840  ·  Snapdragon 8 Elite Gen 5",
    "adreno830"    to "Adreno 830  ·  Snapdragon 8 Elite",
    "adreno750"    to "Adreno 750  ·  Snapdragon 8 Gen 3",
    "adreno740"    to "Adreno 740  ·  Snapdragon 8 Gen 2",
    "adreno735"    to "Adreno 735  ·  Snapdragon 8s Gen 3",
    "adreno730"    to "Adreno 730  ·  Snapdragon 8 Gen 1",
    "adreno720"    to "Adreno 720  ·  Snapdragon 7s Gen 3",
    "mali_g925"    to "Mali-G925 Immortalis  ·  Dimensity 9400",
    "mali_g920"    to "Mali-G920 Immortalis  ·  Dimensity 9400+",
    "mali_g720"    to "Mali-G720 Immortalis  ·  Dimensity 9300",
    "mali_g715"    to "Mali-G715 Immortalis  ·  Dimensity 9200+",
    "mali_g615"    to "Mali-G615  ·  Dimensity 8300 / 8350",
    "mali_g78"     to "Mali-G78 MP24  ·  Kirin 9000",
    "maleoon930"   to "Maleoon 930  ·  Kirin 9030 Pro / 9030S",
    "maleoon920"   to "Maleoon 920  ·  Kirin 9020",
    "maleoon910"   to "Maleoon 910  ·  Kirin 9010",
    "xclipse940"   to "Samsung Xclipse 940  ·  Exynos 2400",
    "xclipse920"   to "Samsung Xclipse 920  ·  Exynos 2200",
    "apple_a18pro" to "Apple A18 Pro GPU",
    "apple_a17pro" to "Apple A17 Pro GPU",
)

private val CPU_DISPLAY: LinkedHashMap<String, String> = linkedMapOf(
    ""                  to "None",
    "sd8elitegen5"      to "Snapdragon 8 Elite Gen 5 (SM8850)",
    "sd8elite"          to "Snapdragon 8 Elite (SM8750)",
    "sd8gen3"           to "Snapdragon 8 Gen 3 (SM8650)",
    "sd8gen2"           to "Snapdragon 8 Gen 2 (SM8550)",
    "dimensity9400plus" to "Dimensity 9400+ (MT6991)",
    "dimensity9400"     to "Dimensity 9400 (MT6989)",
    "dimensity8350"     to "Dimensity 8350 (MT6897)",
    "kirin9030pro"      to "Kirin 9030 Pro",
    "kirin9030s"        to "Kirin 9030S",
    "kirin9020"         to "Kirin 9020",
    "kirin9020a"        to "Kirin 9020A",
    "kirin9000s"        to "Kirin 9000S",
    "kirin9000"         to "Kirin 9000",
    "xuanjie_o1"        to "Xiaomi Xring O1",
    "xuanjie_o3"        to "Xiaomi Xring O3",
)

@Composable
private fun AdvancedSpoofContent(context: Context) {
    val pm = context.packageManager
    val activityMgr = context.getSystemService(ActivityManager::class.java)
    val haptic = LocalHapticFeedback.current

    var enabled by remember { mutableStateOf(true) }
    var rules by remember { mutableStateOf(listOf<SpoofRule>()) }
    var allApps by remember { mutableStateOf(listOf<AppEntry>()) }

    var showAddDialog by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var editRule by remember { mutableStateOf<SpoofRule?>(null) }
    var pendingRemove by remember { mutableStateOf<SpoofRule?>(null) }

    fun reload() {
        enabled = readEnabled(context)
        rules = readRules(context)
    }

    fun persist() = writeRules(context, rules)

    fun stopApp(pkg: String) {
        try { activityMgr?.forceStopPackage(pkg) } catch (_: Exception) {}
    }

    fun removeRule(pkg: String) {
        rules = rules.filter { it.pkg != pkg }
        persist()
        stopApp(pkg)
    }

    fun upsertRule(rule: SpoofRule) {
        val list = rules.toMutableList()
        val idx = list.indexOfFirst { it.pkg == rule.pkg }
        if (idx >= 0) list[idx] = rule else list.add(rule)
        rules = list
        persist()
        stopApp(rule.pkg)
    }

    LaunchedEffect(Unit) {
        reload()
        allApps = withContext(Dispatchers.IO) {
            pm.getInstalledPackages(PackageManager.MATCH_ANY_USER)
                .mapNotNull { pkg ->
                    val ai = pkg.applicationInfo ?: return@mapNotNull null
                    AppEntry(
                        packageName = pkg.packageName,
                        label = ai.loadLabel(pm).toString(),
                        icon = ai.loadIcon(pm),
                        isSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    )
                }
                .distinctBy { it.packageName }
                .sortedBy { it.label.lowercase(Locale.getDefault()) }
        }
    }

    if (showAddDialog || editRule != null) {
        val initial = editRule
        AddRuleSheet(
            allApps = allApps,
            existingPkgs = if (initial != null) emptySet() else rules.map { it.pkg }.toSet(),
            initialRule = initial,
            onDismiss = { showAddDialog = false; editRule = null },
            onSave = { rule ->
                upsertRule(rule)
                showAddDialog = false
                editRule = null
            },
        )
    }

    if (showClearConfirm) {
        DestructiveConfirmDialog(
            title = stringResource(R.string.advanced_spoof_clear_all),
            text = stringResource(R.string.advanced_spoof_clear_all_confirm),
            confirmLabel = stringResource(R.string.advanced_spoof_clear_all),
            dismissLabel = stringResource(R.string.cancel),
            onConfirm = {
                rules.forEach { stopApp(it.pkg) }
                rules = emptyList()
                persist()
                showClearConfirm = false
            },
            onDismiss = { showClearConfirm = false },
        )
    }

    pendingRemove?.let { rule ->
        val label = allApps.find { it.packageName == rule.pkg }?.label ?: rule.pkg
        DestructiveConfirmDialog(
            title = stringResource(R.string.advanced_spoof_remove_title),
            text = stringResource(R.string.advanced_spoof_remove_confirm, label),
            confirmLabel = stringResource(R.string.remove),
            dismissLabel = stringResource(R.string.cancel),
            onConfirm = {
                pendingRemove = null
                removeRule(rule.pkg)
            },
            onDismiss = { pendingRemove = null },
        )
    }

    val none = stringResource(R.string.advanced_spoof_none)
    val switchTitle = stringResource(R.string.advanced_app_spoof_title)
    val gpuLabel = stringResource(R.string.advanced_spoof_label_gpu)
    val cpuLabel = stringResource(R.string.advanced_spoof_label_cpu)
    Scaffold(containerColor = Color.Transparent) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(inner),
        ) {
            MainSwitchPreference(
                object : SwitchPreferenceModel {
                    override val title = switchTitle
                    override val checked = { enabled }
                    override val onCheckedChange: (Boolean) -> Unit = { value ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        enabled = value
                        writeEnabled(context, value)
                        rules.forEach { stopApp(it.pkg) }
                    }
                },
            )

            AnimatedVisibility(
                visible = enabled,
                enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                    expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec(), Alignment.Top),
                exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()) +
                    shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec(), Alignment.Top),
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.advanced_spoof_count, rules.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = SettingsSpace.small1),
                    )
                    ActionButtons(
                        listOf(
                            ActionButton(
                                text = stringResource(R.string.advanced_spoof_add_app),
                                imageVector = Icons.Outlined.Add,
                                onClick = { showAddDialog = true },
                            ),
                            ActionButton(
                                text = stringResource(R.string.advanced_spoof_clear_all),
                                imageVector = Icons.Outlined.Delete,
                                enabled = rules.isNotEmpty(),
                                onClick = { showClearConfirm = true },
                            ),
                        ),
                    )
                    if (rules.isEmpty()) {
                        ZeroStatePreference(
                            icon = Icons.Outlined.Memory,
                            text = stringResource(R.string.advanced_spoof_no_rules),
                        )
                    } else {
                        Category(title = stringResource(R.string.advanced_spoof_configured_apps)) {
                            for (rule in rules) {
                                val app = allApps.find { it.packageName == rule.pkg }
                                val gpu = GPU_DISPLAY[rule.gpuKey] ?: none
                                val cpu = CPU_DISPLAY[rule.cpuKey] ?: none
                                TwoTargetButtonPreference(
                                    title = app?.label ?: rule.pkg,
                                    summary = { "$gpuLabel: $gpu\n$cpuLabel: $cpu" },
                                    icon = if (app != null) {
                                        { SettingsAppIcon(app.icon) }
                                    } else {
                                        null
                                    },
                                    onClick = { editRule = rule },
                                    buttonIcon = Icons.Outlined.Delete,
                                    buttonIconDescription = stringResource(R.string.remove),
                                    onButtonClick = { pendingRemove = rule },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddRuleSheet(
    allApps: List<AppEntry>,
    existingPkgs: Set<String>,
    initialRule: SpoofRule?,
    onDismiss: () -> Unit,
    onSave: (SpoofRule) -> Unit,
) {
    val isEdit = initialRule != null
    var selectedApp by remember { mutableStateOf(allApps.find { it.packageName == initialRule?.pkg }) }
    var selectedGpu by remember { mutableStateOf(initialRule?.gpuKey ?: "") }
    var selectedCpu by remember { mutableStateOf(initialRule?.cpuKey ?: "") }
    var appSearch by remember { mutableStateOf("") }
    var showSystem by remember { mutableStateOf(false) }
    var showPolicyStep by remember { mutableStateOf(isEdit) }

    LaunchedEffect(allApps, initialRule?.pkg) {
        if (selectedApp == null && initialRule != null) {
            selectedApp = allApps.find { it.packageName == initialRule.pkg }
        }
    }

    val filtered = allApps
        .filter { it.packageName !in existingPkgs }
        .filter { showSystem || !it.isSystem }
        .filter {
            appSearch.isBlank() ||
                it.label.contains(appSearch, ignoreCase = true) ||
                it.packageName.contains(appSearch, ignoreCase = true)
        }

    ExpressiveSheet(onDismiss = onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!isEdit && showPolicyStep) {
                TextButton(onClick = { showPolicyStep = false }) {
                    Text(stringResource(R.string.advanced_spoof_back_to_apps))
                }
            }
            Text(
                text = if (isEdit) {
                    stringResource(R.string.advanced_spoof_edit_rule)
                } else if (showPolicyStep) {
                    stringResource(R.string.advanced_spoof_select_policy)
                } else {
                    stringResource(R.string.advanced_spoof_add_rule)
                },
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
        }

        if (!isEdit && !showPolicyStep) {
            AppPickerSearchField(query = appSearch, onQueryChange = { appSearch = it })
            FilterChip(
                selected = showSystem,
                onClick = { showSystem = !showSystem },
                label = {
                    Text(
                        if (showSystem) stringResource(R.string.hide_system_apps)
                        else stringResource(R.string.show_system_apps),
                    )
                },
                leadingIcon = if (showSystem) {
                    { Icon(Icons.Filled.Check, contentDescription = null) }
                } else {
                    null
                },
                modifier = Modifier.padding(vertical = SettingsSpace.extraSmall4),
            )
            if (allApps.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    LoadingIndicator()
                }
            } else if (filtered.isEmpty()) {
                Text(
                    text = if (appSearch.isBlank()) {
                        stringResource(R.string.app_spoofing_no_apps_available)
                    } else {
                        stringResource(R.string.app_spoofing_no_apps_found, appSearch)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(SettingsSpace.small1).weight(1f),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                ) {
                    itemsIndexed(filtered, key = { _, app -> app.packageName }) { index, app ->
                        SegmentedListItem(
                            selected = app.packageName == selectedApp?.packageName,
                            onClick = {
                                selectedApp = app
                                showPolicyStep = true
                            },
                            shapes = ListItemDefaults.segmentedShapes(index, filtered.size),
                            leadingContent = { SettingsAppIcon(app.icon) },
                            supportingContent = {
                                Text(
                                    app.packageName,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                        ) {
                            Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        } else {
            Text(
                text = selectedApp?.label ?: initialRule?.pkg
                    ?: stringResource(R.string.advanced_spoof_pick_app),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(vertical = SettingsSpace.extraSmall4),
            )
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                CategoryTitle(stringResource(R.string.advanced_spoof_label_gpu))
                val gpus = GPU_DISPLAY.entries.toList()
                for ((index, entry) in gpus.withIndex()) {
                    val (key, label) = entry
                    SegmentedListItem(
                        selected = selectedGpu == key,
                        onClick = { selectedGpu = key },
                        shapes = ListItemDefaults.segmentedShapes(index, gpus.size),
                    ) {
                        Text(label, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                CategoryTitle(stringResource(R.string.advanced_spoof_label_cpu))
                val cpus = CPU_DISPLAY.entries.toList()
                for ((index, entry) in cpus.withIndex()) {
                    val (key, label) = entry
                    SegmentedListItem(
                        selected = selectedCpu == key,
                        onClick = { selectedCpu = key },
                        shapes = ListItemDefaults.segmentedShapes(index, cpus.size),
                    ) {
                        Text(label, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            val canSave = (isEdit || selectedApp != null) &&
                (selectedGpu.isNotEmpty() || selectedCpu.isNotEmpty())
            Button(
                onClick = {
                    val pkg = if (isEdit) initialRule!!.pkg else selectedApp!!.packageName
                    onSave(SpoofRule(pkg = pkg, gpuKey = selectedGpu, cpuKey = selectedCpu))
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth().padding(top = SettingsSpace.extraSmall4),
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}
