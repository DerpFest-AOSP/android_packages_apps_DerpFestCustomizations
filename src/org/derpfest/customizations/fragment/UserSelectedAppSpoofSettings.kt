/*
 * SPDX-FileCopyrightText: 2026 kenway214
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package org.derpfest.customizations.fragment

import android.app.ActivityManager
import android.content.Context
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
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class UserSelectedAppSpoofSettings : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().title = getString(R.string.user_selectable_app_spoofing_title)
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
                    AppSpoofingContent(requireContext())
                }
            }
        }
    }
}

private const val SPOOFED_APPS_SETTING = Settings.Secure.PER_APPS_DEVICE_SPOOF
private const val SPOOFED_APPS_CACHE_SETTING = Settings.Secure.PER_APPS_DEVICE_SPOOF_CACHE
private const val SPOOFED_APPS_ENABLED_SETTING = Settings.Secure.PER_APPS_DEVICE_SPOOF_ENABLED
private const val CUSTOM_SPOOF_PROFILES_SETTING = Settings.Secure.CUSTOM_SPOOF_PROFILES

private data class AppItem(
    val packageName: String,
    val label: String,
    val icon: Drawable,
    val isSystem: Boolean
)

private data class CustomSpoofProfile(
    val id: String,
    val name: String,
    val brand: String,
    val manufacturer: String,
    val device: String,
    val model: String,
    val fingerprint: String,
    val product: String
)

private data class ProfileChoice(
    val id: String,
    val label: String,
    val custom: CustomSpoofProfile?,
)

@Composable
private fun AppSpoofingContent(context: Context) {
    val pm = context.packageManager
    val activityManager = context.getSystemService(ActivityManager::class.java)
    val profileValues = context.resources.getStringArray(R.array.perapp_spoof_profile_values)
    val profileLabels = context.resources.getStringArray(R.array.perapp_spoof_profile_labels)
    val profileLabelMap = remember(profileValues.contentToString(), profileLabels.contentToString()) {
        profileValues.indices.associate { idx ->
            profileValues[idx] to profileLabels.getOrElse(idx) { profileValues[idx] }
        }
    }

    val haptic = LocalHapticFeedback.current

    var allApps by remember { mutableStateOf(listOf<AppItem>()) }
    var configuredMap by remember { mutableStateOf(linkedMapOf<String, String>()) }
    var spoofEnabled by remember { mutableStateOf(true) }

    var showAddDialog by remember { mutableStateOf(false) }
    var showModelDialog by remember { mutableStateOf(false) }
    var showAddCustomProfileDialog by remember { mutableStateOf(false) }
    var showClearAllConfirm by remember { mutableStateOf(false) }
    var customProfileToEdit by remember { mutableStateOf<CustomSpoofProfile?>(null) }
    var editTarget by remember { mutableStateOf<AppItem?>(null) }
    var customProfiles by remember { mutableStateOf(listOf<CustomSpoofProfile>()) }
    var pendingRemovePkg by remember { mutableStateOf<String?>(null) }

    fun loadState() {
        spoofEnabled = readEnabled(context)
        configuredMap = linkedMapOf<String, String>().apply {
            putAll(readConfigured(context, spoofEnabled))
        }
        customProfiles = readCustomProfiles(context)
    }

    fun persistConfigured() {
        writeConfigured(context, configuredMap, spoofEnabled)
    }

    fun stopPackage(pkg: String) {
        try {
            activityManager?.forceStopPackage(pkg)
        } catch (_: Exception) {}
    }

    fun allKnownConfiguredPackages(): Set<String> {
        val keys = linkedSetOf<String>()
        keys.addAll(configuredMap.keys)
        keys.addAll(readMapSetting(context, SPOOFED_APPS_SETTING).keys)
        keys.addAll(readMapSetting(context, SPOOFED_APPS_CACHE_SETTING).keys)
        return keys
    }

    fun setMasterEnabled(enabled: Boolean) {
        val targets = allKnownConfiguredPackages()
        spoofEnabled = enabled
        writeEnabled(context, enabled)
        if (enabled) {
            val cached = readMapSetting(context, SPOOFED_APPS_CACHE_SETTING)
            writeMapSetting(context, SPOOFED_APPS_SETTING, cached)
        } else {
            val active = readMapSetting(context, SPOOFED_APPS_SETTING)
            writeMapSetting(context, SPOOFED_APPS_CACHE_SETTING, active)
            writeMapSetting(context, SPOOFED_APPS_SETTING, emptyMap())
        }
        targets.forEach { stopPackage(it) }
    }

    fun clearAllConfigured() {
        val targets = allKnownConfiguredPackages()
        configuredMap = linkedMapOf()
        writeMapSetting(context, SPOOFED_APPS_CACHE_SETTING, emptyMap())
        writeMapSetting(context, SPOOFED_APPS_SETTING, emptyMap())
        targets.forEach { stopPackage(it) }
    }

    fun applyProfile(pkg: String, profile: String) {
        configuredMap = LinkedHashMap(configuredMap).apply { put(pkg, profile) }
        persistConfigured()
        if (spoofEnabled) stopPackage(pkg)
    }

    LaunchedEffect(Unit) {
        loadState()
        allApps = withContext(Dispatchers.IO) {
            pm.getInstalledPackages(PackageManager.MATCH_ANY_USER)
                .mapNotNull { pkg ->
                    val ai = pkg.applicationInfo ?: return@mapNotNull null
                    AppItem(
                        packageName = pkg.packageName,
                        label = ai.loadLabel(pm).toString(),
                        icon = ai.loadIcon(pm),
                        isSystem = ai.isSystemApp
                    )
                }
                .distinctBy { it.packageName }
                .sortedBy { it.label.lowercase(Locale.getDefault()) }
        }
    }

    if (showAddDialog) {
        AddAppSheet(
            allApps = allApps,
            configuredPackages = configuredMap.keys,
            profileValues = profileValues,
            profileLabels = profileLabels,
            customProfiles = customProfiles,
            onDismiss = { showAddDialog = false },
            onAppAdded = { app, profile ->
                applyProfile(app.packageName, profile)
                showAddDialog = false
            },
            onWriteCustomProfiles = { updated ->
                writeCustomProfiles(context, updated)
                customProfiles = updated
            },
        )
    }

    if (showModelDialog) {
        ProfilePickerSheet(
            title = stringResource(R.string.user_select_spoofing_profile_title),
            profileValues = profileValues,
            profileLabels = profileLabels,
            customProfiles = customProfiles,
            includeNone = true,
            selectedId = editTarget?.let { configuredMap[it.packageName] },
            onDismiss = { showModelDialog = false },
            onSelect = { profileId ->
                val target = editTarget ?: return@ProfilePickerSheet
                applyProfile(target.packageName, profileId)
                showModelDialog = false
            },
            onEditCustom = { profile ->
                customProfileToEdit = profile
                showAddCustomProfileDialog = true
                showModelDialog = false
            },
            onDeleteCustom = { profile ->
                val newList = customProfiles.filter { it.id != profile.id }
                writeCustomProfiles(context, newList)
                customProfiles = newList
                val newConfigured = configuredMap.filterValues { it != profile.id }
                if (newConfigured.size != configuredMap.size) {
                    configuredMap = LinkedHashMap(newConfigured)
                    persistConfigured()
                }
            },
            onAddCustom = {
                customProfileToEdit = null
                showModelDialog = false
                showAddCustomProfileDialog = true
            },
        )
    }

    if (showAddCustomProfileDialog) {
        val passedId = customProfileToEdit?.id ?: ("CUSTOM_" + System.currentTimeMillis())
        AddCustomProfileDialog(
            initialProfile = customProfileToEdit,
            onDismiss = { showAddCustomProfileDialog = false },
            onSave = { newProfile ->
                val updatedProfiles = if (customProfileToEdit != null) {
                    customProfiles.map { if (it.id == newProfile.id) newProfile else it }
                } else {
                    customProfiles + newProfile
                }
                writeCustomProfiles(context, updatedProfiles)
                customProfiles = updatedProfiles
                showAddCustomProfileDialog = false
                customProfileToEdit = null
                showModelDialog = true
            },
            id = passedId
        )
    }

    if (showClearAllConfirm) {
        DestructiveConfirmDialog(
            title = stringResource(R.string.app_spoofing_clear_all),
            text = stringResource(R.string.app_spoofing_clear_all_confirm),
            confirmLabel = stringResource(R.string.app_spoofing_clear_all),
            dismissLabel = stringResource(R.string.cancel),
            onConfirm = {
                clearAllConfigured()
                showClearAllConfirm = false
            },
            onDismiss = { showClearAllConfirm = false },
        )
    }

    pendingRemovePkg?.let { pkg ->
        val label = allApps.find { it.packageName == pkg }?.label ?: pkg
        DestructiveConfirmDialog(
            title = stringResource(R.string.app_spoofing_remove_title),
            text = stringResource(R.string.app_spoofing_remove_confirm, label),
            confirmLabel = stringResource(R.string.remove),
            dismissLabel = stringResource(R.string.cancel),
            onConfirm = {
                configuredMap = LinkedHashMap(configuredMap).apply { remove(pkg) }
                persistConfigured()
                stopPackage(pkg)
                pendingRemovePkg = null
            },
            onDismiss = { pendingRemovePkg = null },
        )
    }

    val switchTitle = stringResource(R.string.app_spoofing_header_title)
    Scaffold(containerColor = Color.Transparent) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding),
        ) {
            MainSwitchPreference(
                object : SwitchPreferenceModel {
                    override val title = switchTitle
                    override val checked = { spoofEnabled }
                    override val onCheckedChange: (Boolean) -> Unit = { newValue ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        setMasterEnabled(newValue)
                    }
                },
            )

            AnimatedVisibility(
                visible = spoofEnabled,
                enter = fadeIn(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()) +
                    expandVertically(
                        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                        expandFrom = Alignment.Top,
                    ),
                exit = fadeOut(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()) +
                    shrinkVertically(
                        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
                        shrinkTowards = Alignment.Top,
                    ),
            ) {
                Column {
                    Text(
                        text = stringResource(
                            R.string.app_spoofing_configured_count,
                            configuredMap.size,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = SettingsSpace.small1),
                    )
                    ActionButtons(
                        listOf(
                            ActionButton(
                                text = stringResource(R.string.app_spoofing_add_apps),
                                imageVector = Icons.Outlined.Add,
                                onClick = { showAddDialog = true },
                            ),
                            ActionButton(
                                text = stringResource(R.string.app_spoofing_spoofed_model),
                                imageVector = Icons.Outlined.Refresh,
                                onClick = { showModelDialog = true },
                            ),
                            ActionButton(
                                text = stringResource(R.string.app_spoofing_clear_all),
                                imageVector = Icons.Outlined.Delete,
                                enabled = configuredMap.isNotEmpty(),
                                onClick = { showClearAllConfirm = true },
                            ),
                        ),
                    )
                    if (configuredMap.isEmpty()) {
                        ZeroStatePreference(
                            icon = Icons.Outlined.Apps,
                            text = stringResource(R.string.app_spoofing_no_apps_configured),
                        )
                    } else {
                        Category(title = stringResource(R.string.app_spoofing_configured_apps)) {
                            for ((pkg, profile) in configuredMap) {
                                val app = allApps.find { it.packageName == pkg }
                                val customProfile = customProfiles.find { it.id == profile }
                                val displayLabel = customProfile?.name ?: profileLabelMap[profile] ?: profile
                                TwoTargetButtonPreference(
                                    title = app?.label ?: pkg,
                                    summary = { "$displayLabel\n$profile" },
                                    icon = if (app != null) {
                                        { SettingsAppIcon(app.icon) }
                                    } else {
                                        null
                                    },
                                    onClick = {
                                        if (app != null) {
                                            editTarget = app
                                            showModelDialog = true
                                        }
                                    },
                                    buttonIcon = Icons.Outlined.Delete,
                                    buttonIconDescription = stringResource(R.string.remove),
                                    onButtonClick = { pendingRemovePkg = pkg },
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
private fun AddAppSheet(
    allApps: List<AppItem>,
    configuredPackages: Set<String>,
    profileValues: Array<String>,
    profileLabels: Array<String>,
    customProfiles: List<CustomSpoofProfile>,
    onDismiss: () -> Unit,
    onAppAdded: (AppItem, String) -> Unit,
    onWriteCustomProfiles: (List<CustomSpoofProfile>) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    var showSystemApps by remember { mutableStateOf(false) }
    var selectedApp by remember { mutableStateOf<AppItem?>(null) }
    var selectedProfile by remember { mutableStateOf<String?>(null) }
    var showProfileSelector by remember { mutableStateOf(false) }
    var showAddCustomProfileDialog by remember { mutableStateOf(false) }
    var customProfileToEdit by remember { mutableStateOf<CustomSpoofProfile?>(null) }
    var customProfilesList by remember { mutableStateOf(customProfiles) }

    val addableProfiles = profileValues.filter { it != "None" }
    val profileLabelMap = profileValues.indices.associate { idx ->
        profileValues[idx] to profileLabels.getOrElse(idx) { profileValues[idx] }
    }
    val filteredApps = allApps.filter { app ->
        if (configuredPackages.contains(app.packageName)) return@filter false
        if (!showSystemApps && app.isSystem) return@filter false
        if (searchQuery.isBlank()) return@filter true
        app.label.contains(searchQuery, true) || app.packageName.contains(searchQuery, true)
    }
    val choices = profileChoices(addableProfiles, profileLabelMap, customProfilesList)

    ExpressiveSheet(onDismiss = onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showProfileSelector) {
                TextButton(onClick = { showProfileSelector = false }) {
                    Text(stringResource(R.string.app_spoofing_change_app))
                }
            }
            Text(
                text = if (showProfileSelector) {
                    stringResource(R.string.app_spoofing_select_spoof_model)
                } else {
                    stringResource(R.string.app_spoofing_add_apps)
                },
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
        }

        if (!showProfileSelector) {
            AppPickerSearchField(query = searchQuery, onQueryChange = { searchQuery = it })
            FilterChip(
                selected = showSystemApps,
                onClick = { showSystemApps = !showSystemApps },
                label = {
                    Text(
                        if (showSystemApps) stringResource(R.string.hide_system_apps)
                        else stringResource(R.string.show_system_apps),
                    )
                },
                leadingIcon = if (showSystemApps) {
                    { Icon(Icons.Filled.Check, contentDescription = null) }
                } else {
                    null
                },
                modifier = Modifier.padding(vertical = SettingsSpace.extraSmall4),
            )
            if (allApps.isEmpty()) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    LoadingIndicator()
                }
            } else if (filteredApps.isEmpty()) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) {
                            stringResource(R.string.app_spoofing_no_apps_available)
                        } else {
                            stringResource(R.string.app_spoofing_no_apps_found, searchQuery)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (searchQuery.isNotBlank()) {
                        TextButton(onClick = { searchQuery = "" }) {
                            Text(stringResource(R.string.app_spoofing_clear_search))
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                ) {
                    itemsIndexed(filteredApps, key = { _, app -> app.packageName }) { index, app ->
                        SegmentedListItem(
                            selected = selectedApp?.packageName == app.packageName,
                            onClick = { selectedApp = app },
                            shapes = ListItemDefaults.segmentedShapes(index, filteredApps.size),
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
            if (selectedApp != null) {
                Button(
                    onClick = { showProfileSelector = true },
                    modifier = Modifier.fillMaxWidth().padding(top = SettingsSpace.extraSmall4),
                ) {
                    Text(
                        if (selectedProfile != null) {
                            stringResource(R.string.app_spoofing_spoof_model) + ": " +
                                (profileLabelMap[selectedProfile] ?: selectedProfile ?: "")
                        } else {
                            stringResource(R.string.app_spoofing_select_spoof_model)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                TextButton(
                    onClick = {
                        val app = selectedApp ?: return@TextButton
                        onAppAdded(app, selectedProfile ?: "None")
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.add))
                }
            }
        } else {
            Text(
                text = selectedApp?.label.orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = SettingsSpace.extraSmall4),
            )
            ProfileChoiceList(
                choices = choices,
                selectedId = selectedProfile,
                modifier = Modifier.weight(1f),
                onSelect = { choice ->
                    selectedProfile = choice.id
                    showProfileSelector = false
                },
                onEditCustom = { profile ->
                    customProfileToEdit = profile
                    showAddCustomProfileDialog = true
                },
                onDeleteCustom = { profile ->
                    val newList = customProfilesList.filter { it.id != profile.id }
                    onWriteCustomProfiles(newList)
                    customProfilesList = newList
                    if (selectedProfile == profile.id) selectedProfile = null
                },
            )
            TextButton(
                onClick = {
                    customProfileToEdit = null
                    showAddCustomProfileDialog = true
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.add_new_spoof_profile))
            }
        }
    }

    if (showAddCustomProfileDialog) {
        val passedId = customProfileToEdit?.id ?: ("CUSTOM_" + System.currentTimeMillis())
        AddCustomProfileDialog(
            initialProfile = customProfileToEdit,
            onDismiss = { showAddCustomProfileDialog = false },
            onSave = { newProfile ->
                val updatedProfiles = if (customProfileToEdit != null) {
                    customProfilesList.map { if (it.id == newProfile.id) newProfile else it }
                } else {
                    customProfilesList + newProfile
                }
                onWriteCustomProfiles(updatedProfiles)
                customProfilesList = updatedProfiles
                showAddCustomProfileDialog = false
                customProfileToEdit = null
            },
            id = passedId,
        )
    }
}

@Composable
private fun ProfilePickerSheet(
    title: String,
    profileValues: Array<String>,
    profileLabels: Array<String>,
    customProfiles: List<CustomSpoofProfile>,
    includeNone: Boolean,
    selectedId: String?,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    onEditCustom: (CustomSpoofProfile) -> Unit,
    onDeleteCustom: (CustomSpoofProfile) -> Unit,
    onAddCustom: () -> Unit,
) {
    val labelMap = profileValues.indices.associate { idx ->
        profileValues[idx] to profileLabels.getOrElse(idx) { profileValues[idx] }
    }
    val ids = profileValues.filter { includeNone || it != "None" }
    val choices = profileChoices(ids, labelMap, customProfiles)

    ExpressiveSheet(onDismiss = onDismiss) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = SettingsSpace.extraSmall4),
        )
        ProfileChoiceList(
            choices = choices,
            selectedId = selectedId,
            modifier = Modifier.weight(1f),
            onSelect = { choice -> onSelect(choice.id) },
            onEditCustom = onEditCustom,
            onDeleteCustom = onDeleteCustom,
        )
        TextButton(onClick = onAddCustom, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.add_new_spoof_profile))
        }
    }
}

@Composable
private fun ProfileChoiceList(
    choices: List<ProfileChoice>,
    selectedId: String?,
    modifier: Modifier,
    onSelect: (ProfileChoice) -> Unit,
    onEditCustom: (CustomSpoofProfile) -> Unit,
    onDeleteCustom: (CustomSpoofProfile) -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        itemsIndexed(choices, key = { _, choice -> choice.id }) { index, choice ->
            SegmentedListItem(
                selected = choice.id == selectedId,
                onClick = { onSelect(choice) },
                shapes = ListItemDefaults.segmentedShapes(index, choices.size),
                supportingContent = {
                    Text(choice.id, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                trailingContent = choice.custom?.let { custom ->
                    {
                        Row {
                            IconButton(onClick = { onEditCustom(custom) }) {
                                Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.edit))
                            }
                            IconButton(onClick = { onDeleteCustom(custom) }) {
                                Icon(
                                    Icons.Outlined.Delete,
                                    contentDescription = stringResource(R.string.remove),
                                )
                            }
                        }
                    }
                },
            ) {
                Text(choice.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

private fun profileChoices(
    ids: List<String>,
    labelMap: Map<String, String>,
    customProfiles: List<CustomSpoofProfile>,
): List<ProfileChoice> {
    val builtin = ids.map { id -> ProfileChoice(id, labelMap[id] ?: id, null) }
    val custom = customProfiles.map { ProfileChoice(it.id, it.name, it) }
    return builtin + custom
}

private fun readMapSetting(context: Context, key: String): Map<String, String> {
    val stored = Settings.Secure.getString(context.contentResolver, key) ?: return emptyMap()
    if (stored.isBlank()) return emptyMap()
    val map = linkedMapOf<String, String>()
    stored.split(",").forEach { entry ->
        val parts = entry.split(":")
        if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
            map[parts[0]] = parts[1]
        }
    }
    return map
}

private fun writeMapSetting(context: Context, key: String, values: Map<String, String>) {
    val encoded = values.entries.joinToString(",") { "${it.key}:${it.value}" }
    Settings.Secure.putString(context.contentResolver, key, encoded)
}

private fun readEnabled(context: Context): Boolean {
    return Settings.Secure.getInt(context.contentResolver, SPOOFED_APPS_ENABLED_SETTING, 1) == 1
}

private fun writeEnabled(context: Context, enabled: Boolean) {
    Settings.Secure.putInt(context.contentResolver, SPOOFED_APPS_ENABLED_SETTING, if (enabled) 1 else 0)
}

private fun readConfigured(context: Context, enabled: Boolean): Map<String, String> {
    return if (enabled) {
        val active = readMapSetting(context, SPOOFED_APPS_SETTING)
        if (active.isNotEmpty()) {
            writeMapSetting(context, SPOOFED_APPS_CACHE_SETTING, active)
        }
        active.ifEmpty { readMapSetting(context, SPOOFED_APPS_CACHE_SETTING) }
    } else {
        readMapSetting(context, SPOOFED_APPS_CACHE_SETTING)
    }
}

private fun writeConfigured(context: Context, values: Map<String, String>, enabled: Boolean) {
    writeMapSetting(context, SPOOFED_APPS_CACHE_SETTING, values)
    if (enabled) {
        writeMapSetting(context, SPOOFED_APPS_SETTING, values)
    } else {
        writeMapSetting(context, SPOOFED_APPS_SETTING, emptyMap())
    }
}

private fun readCustomProfiles(context: Context): List<CustomSpoofProfile> {
    val jsonStr = Settings.Secure.getString(context.contentResolver, CUSTOM_SPOOF_PROFILES_SETTING)
    if (jsonStr.isNullOrBlank()) return emptyList()
    return try {
        val jsonArray = JSONArray(jsonStr)
        val profiles = mutableListOf<CustomSpoofProfile>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            profiles.add(
                CustomSpoofProfile(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    brand = obj.getString("brand"),
                    manufacturer = obj.getString("manufacturer"),
                    device = obj.getString("device"),
                    model = obj.getString("model"),
                    fingerprint = obj.optString("fingerprint", ""),
                    product = obj.optString("product", "")
                )
            )
        }
        profiles
    } catch (e: Exception) {
        emptyList()
    }
}

private fun writeCustomProfiles(context: Context, profiles: List<CustomSpoofProfile>) {
    val jsonArray = JSONArray()
    profiles.forEach { profile ->
        val obj = JSONObject().apply {
            put("id", profile.id)
            put("name", profile.name)
            put("brand", profile.brand)
            put("manufacturer", profile.manufacturer)
            put("device", profile.device)
            put("model", profile.model)
            put("fingerprint", profile.fingerprint)
            put("product", profile.product)
        }
        jsonArray.put(obj)
    }
    Settings.Secure.putString(context.contentResolver, CUSTOM_SPOOF_PROFILES_SETTING, jsonArray.toString())
}

@Composable
private fun AddCustomProfileDialog(
    initialProfile: CustomSpoofProfile? = null,
    onDismiss: () -> Unit,
    onSave: (CustomSpoofProfile) -> Unit,
    id: String
) {
    var name by remember(initialProfile) { mutableStateOf(initialProfile?.name ?: "") }
    var brand by remember(initialProfile) { mutableStateOf(initialProfile?.brand ?: "") }
    var manufacturer by remember(initialProfile) { mutableStateOf(initialProfile?.manufacturer ?: "") }
    var device by remember(initialProfile) { mutableStateOf(initialProfile?.device ?: "") }
    var model by remember(initialProfile) { mutableStateOf(initialProfile?.model ?: "") }
    var fingerprint by remember(initialProfile) { mutableStateOf(initialProfile?.fingerprint ?: "") }
    var product by remember(initialProfile) { mutableStateOf(initialProfile?.product ?: "") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_custom_spoof_profile_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(SettingsSpace.extraSmall4),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.custom_spoof_profile_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                )
                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text(stringResource(R.string.custom_spoof_profile_brand)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                )
                OutlinedTextField(
                    value = manufacturer,
                    onValueChange = { manufacturer = it },
                    label = { Text(stringResource(R.string.custom_spoof_profile_manufacturer)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                )
                OutlinedTextField(
                    value = device,
                    onValueChange = { device = it },
                    label = { Text(stringResource(R.string.custom_spoof_profile_device)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                )
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text(stringResource(R.string.custom_spoof_profile_model)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                )
                OutlinedTextField(
                    value = fingerprint,
                    onValueChange = { fingerprint = it },
                    label = { Text(stringResource(R.string.custom_spoof_profile_fingerprint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                )
                OutlinedTextField(
                    value = product,
                    onValueChange = { product = it },
                    label = { Text(stringResource(R.string.custom_spoof_profile_product)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        CustomSpoofProfile(
                            id = id,
                            name = name,
                            brand = brand,
                            manufacturer = manufacturer,
                            device = device,
                            model = model,
                            fingerprint = fingerprint,
                            product = product,
                        ),
                    )
                },
                enabled = name.isNotBlank() && brand.isNotBlank() && manufacturer.isNotBlank() &&
                    device.isNotBlank() && model.isNotBlank(),
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
