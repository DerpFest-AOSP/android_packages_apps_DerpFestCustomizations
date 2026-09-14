/*
 * SPDX-FileCopyrightText: 2026 kenway214
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.fragment

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settingslib.spa.framework.theme.SettingsTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

private fun gpuAccent(key: String): Color = when {
    key.startsWith("adreno")    -> Color(0xFF6200EE)
    key.startsWith("mali")      -> Color(0xFF1565C0)
    key.startsWith("maleoon")   -> Color(0xFFD84315)
    key.startsWith("xclipse")   -> Color(0xFF00897B)
    key.startsWith("apple")     -> Color(0xFF37474F)
    else                        -> Color(0xFF546E7A)
}

private fun cpuAccent(key: String): Color = when {
    key.startsWith("sd")                                 -> Color(0xFF6200EE)
    key.startsWith("dimensity")                          -> Color(0xFF00695C)
    key.startsWith("kirin")                              -> Color(0xFFD84315)
    key.startsWith("xuanjie") || key.startsWith("xring") -> Color(0xFFFF6D00)
    else                                                 -> Color(0xFF546E7A)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AdvancedSpoofContent(context: Context) {
    val pm             = context.packageManager
    val activityMgr    = context.getSystemService(ActivityManager::class.java)
    val haptic         = LocalHapticFeedback.current
    val scope          = rememberCoroutineScope()

    var enabled by remember { mutableStateOf(true) }
    var rules   by remember { mutableStateOf(listOf<SpoofRule>()) }
    var allApps by remember { mutableStateOf(listOf<AppEntry>()) }

    var showAddDialog     by remember { mutableStateOf(false) }
    var showClearConfirm  by remember { mutableStateOf(false) }
    var editRule          by remember { mutableStateOf<SpoofRule?>(null) }

    fun reload() {
        enabled = readEnabled(context)
        rules   = readRules(context)
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
        val idx  = list.indexOfFirst { it.pkg == rule.pkg }
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
                        label       = ai.loadLabel(pm).toString(),
                        icon        = ai.loadIcon(pm),
                        isSystem    = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    )
                }
                .distinctBy { it.packageName }
                .sortedBy { it.label.lowercase(Locale.getDefault()) }
        }
    }

    if (showAddDialog || editRule != null) {
        val initial = editRule
        AddRuleDialog(
            allApps        = allApps,
            existingPkgs   = if (initial != null) emptySet() else rules.map { it.pkg }.toSet(),
            initialRule    = initial,
            onDismiss      = { showAddDialog = false; editRule = null },
            onSave         = { rule ->
                upsertRule(rule)
                showAddDialog = false
                editRule      = null
            }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.advanced_spoof_clear_all)) },
            text  = { Text(stringResource(R.string.advanced_spoof_clear_all_confirm)) },
            confirmButton = {
                Button(
                    onClick = {
                        rules.forEach { stopApp(it.pkg) }
                        rules = emptyList()
                        persist()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text(stringResource(R.string.advanced_spoof_clear_all)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(containerColor = Color.Transparent) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(inner)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(24.dp),
                colors   = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceBright
                )
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier        = Modifier.size(48.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Memory, null,
                                tint     = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(26.dp))
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                text      = stringResource(R.string.advanced_app_spoof_title),
                                style     = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text  = if (enabled)
                                    stringResource(R.string.advanced_spoof_count, rules.size)
                                else
                                    stringResource(R.string.advanced_spoof_disabled),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked         = enabled,
                        onCheckedChange = { v ->
                            scope.launch {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            enabled = v
                            writeEnabled(context, v)
                            rules.forEach { stopApp(it.pkg) }
                        },
                        thumbContent = {
                            Crossfade(
                                targetState  = enabled,
                                animationSpec = MaterialTheme.motionScheme.slowEffectsSpec(),
                                label        = "adv_spoof_thumb"
                            ) { on ->
                                if (on) Icon(Icons.Rounded.Check, null, Modifier.size(16.dp))
                                else    Icon(Icons.Rounded.Close,  null, Modifier.size(16.dp))
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            AnimatedVisibility(
                visible = enabled,
                enter   = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                          expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec(), Alignment.Top),
                exit    = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()) +
                          shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec(), Alignment.Top)
            ) {
                Column {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick  = { showAddDialog = true },
                            modifier = Modifier.weight(1f),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                stringResource(R.string.advanced_spoof_add_app),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        OutlinedButton(
                            onClick  = { showClearConfirm = true },
                            modifier = Modifier.weight(1f),
                            colors   = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Delete, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                stringResource(R.string.advanced_spoof_clear_all),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    if (rules.isNotEmpty()) {
                        Text(
                            text       = stringResource(R.string.advanced_spoof_configured_apps),
                            style      = MaterialTheme.typography.labelMedium,
                            color      = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier   = Modifier.padding(start = 4.dp, bottom = 8.dp)
                        )
                        Column {
                            rules.forEach { rule ->
                                val app = allApps.find { it.packageName == rule.pkg }
                                if (app != null) {
                                    AnimatedVisibility(
                                        visible = true,
                                        enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                                                expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
                                        exit  = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()) +
                                                shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec())
                                    ) {
                                        Column {
                                            RuleCard(
                                                app    = app,
                                                rule   = rule,
                                                onEdit = { editRule = rule },
                                                onRemove = { removeRule(rule.pkg) }
                                            )
                                            Spacer(Modifier.height(8.dp))
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        EmptyState()
                    }
                }
            }

            Spacer(Modifier.height(100.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RuleCard(
    app: AppEntry,
    rule: SpoofRule,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
) {
    var expanded       by remember { mutableStateOf(false) }
    var showDelConfirm by remember { mutableStateOf(false) }
    val iconBmp = remember(app.packageName) { app.icon.toBitmap(96, 96).asImageBitmap() }

    if (showDelConfirm) {
        AlertDialog(
            onDismissRequest = { showDelConfirm = false },
            icon  = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.advanced_spoof_remove_title)) },
            text  = { Text(stringResource(R.string.advanced_spoof_remove_confirm, app.label)) },
            confirmButton = {
                Button(
                    onClick = { showDelConfirm = false; onRemove() },
                    colors  = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text(stringResource(R.string.remove)) }
            },
            dismissButton = {
                TextButton(onClick = { showDelConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(MaterialTheme.motionScheme.defaultSpatialSpec())
            .clickable { expanded = !expanded },
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    bitmap            = iconBmp,
                    contentDescription = null,
                    modifier           = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text       = app.label,
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                    Text(
                        text     = app.packageName,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (rule.gpuKey.isNotEmpty()) {
                    PresetChip(
                        label  = GPU_DISPLAY[rule.gpuKey]?.substringBefore("·")?.trim() ?: rule.gpuKey,
                        accent = gpuAccent(rule.gpuKey)
                    )
                    Spacer(Modifier.width(6.dp))
                }
                if (rule.cpuKey.isNotEmpty()) {
                    PresetChip(
                        label  = CPU_DISPLAY[rule.cpuKey]?.substringBefore("(")?.trim() ?: rule.cpuKey,
                        accent = cpuAccent(rule.cpuKey)
                    )
                    Spacer(Modifier.width(6.dp))
                }
            }

            if (expanded) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    color    = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        DetailRow(
                            label = stringResource(R.string.advanced_spoof_label_gpu),
                            value = GPU_DISPLAY[rule.gpuKey] ?: stringResource(R.string.advanced_spoof_none)
                        )
                        HorizontalDivider(Modifier.padding(vertical = 6.dp))
                        DetailRow(
                            label = stringResource(R.string.advanced_spoof_label_cpu),
                            value = CPU_DISPLAY[rule.cpuKey] ?: stringResource(R.string.advanced_spoof_none)
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick  = { onEdit() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Edit, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.edit))
                            }
                            OutlinedButton(
                                onClick  = { showDelConfirm = true },
                                modifier = Modifier.weight(1f),
                                colors   = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(Icons.Default.Delete, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.remove))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetChip(label: String, accent: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(accent.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text       = label,
            style      = MaterialTheme.typography.labelSmall,
            color      = accent,
            fontWeight = FontWeight.SemiBold,
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text       = "$label:",
            style      = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier   = Modifier.width(44.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text     = value,
            style    = MaterialTheme.typography.bodySmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EmptyState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier             = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment  = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Memory, null,
                modifier = Modifier.size(48.dp),
                tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text  = stringResource(R.string.advanced_spoof_no_rules),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddRuleDialog(
    allApps: List<AppEntry>,
    existingPkgs: Set<String>,
    initialRule: SpoofRule?,
    onDismiss: () -> Unit,
    onSave: (SpoofRule) -> Unit,
) {
    val isEdit        = initialRule != null
    var selectedApp   by remember { mutableStateOf(allApps.find { it.packageName == initialRule?.pkg }) }
    var selectedGpu   by remember { mutableStateOf(initialRule?.gpuKey ?: "") }
    var selectedCpu   by remember { mutableStateOf(initialRule?.cpuKey ?: "") }

    var showGpuPicker by remember { mutableStateOf(false) }
    var showCpuPicker by remember { mutableStateOf(false) }
    var appSearch     by remember { mutableStateOf("") }
    var showPolicyStep by remember { mutableStateOf(isEdit) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isEdit && showPolicyStep) {
                    TextButton(onClick = { showPolicyStep = false }) {
                        Text(stringResource(R.string.advanced_spoof_back_to_apps))
                    }
                }
                Text(if (isEdit) stringResource(R.string.advanced_spoof_edit_rule)
                     else if (showPolicyStep) stringResource(R.string.advanced_spoof_select_policy)
                     else stringResource(R.string.advanced_spoof_add_rule))
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                if (!isEdit && !showPolicyStep) {
                    AppPickerDropdown(
                        allApps = allApps,
                        excludePkgs = existingPkgs,
                        selectedPkg = selectedApp?.packageName,
                        search = appSearch,
                        onSearch = { appSearch = it },
                        onPick = { selectedApp = it },
                    )
                    Button(
                        onClick = { showPolicyStep = true },
                        enabled = selectedApp != null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.advanced_spoof_next_set_policy))
                    }
                } else {

                if (!isEdit && showPolicyStep) {
                    Text(
                        text = selectedApp?.label ?: stringResource(R.string.advanced_spoof_pick_app),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Text(
                        text  = initialRule!!.pkg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider()

                Text(
                    stringResource(R.string.advanced_spoof_label_gpu),
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(
                    onClick  = { showGpuPicker = !showGpuPicker },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        GPU_DISPLAY[selectedGpu] ?: stringResource(R.string.advanced_spoof_none),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                DropdownMenu(
                    expanded        = showGpuPicker,
                    onDismissRequest = { showGpuPicker = false }
                ) {
                    GPU_DISPLAY.forEach { (key, label) ->
                        DropdownMenuItem(
                            text    = { Text(label, style = MaterialTheme.typography.bodySmall) },
                            onClick = { selectedGpu = key; showGpuPicker = false }
                        )
                    }
                }

                Text(
                    stringResource(R.string.advanced_spoof_label_cpu),
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(
                    onClick  = { showCpuPicker = !showCpuPicker },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        CPU_DISPLAY[selectedCpu] ?: stringResource(R.string.advanced_spoof_none),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                DropdownMenu(
                    expanded        = showCpuPicker,
                    onDismissRequest = { showCpuPicker = false }
                ) {
                    CPU_DISPLAY.forEach { (key, label) ->
                        DropdownMenuItem(
                            text    = { Text(label, style = MaterialTheme.typography.bodySmall) },
                            onClick = { selectedCpu = key; showCpuPicker = false }
                        )
                    }
                }
                }
            }
        },
        confirmButton = {
            val canSave = (isEdit || selectedApp != null) &&
                          (selectedGpu.isNotEmpty() || selectedCpu.isNotEmpty())
            Button(
                onClick  = {
                    val pkg = if (isEdit) initialRule!!.pkg else selectedApp!!.packageName
                    onSave(SpoofRule(pkg = pkg, gpuKey = selectedGpu, cpuKey = selectedCpu))
                },
                enabled  = canSave
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppPickerDropdown(
    allApps: List<AppEntry>,
    excludePkgs: Set<String>,
    selectedPkg: String?,
    search: String,
    onSearch: (String) -> Unit,
    onPick: (AppEntry) -> Unit,
) {
    var showSystem by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val filtered = allApps
        .filter { it.packageName !in excludePkgs }
        .filter { showSystem || !it.isSystem }
        .filter { search.isBlank() || it.label.contains(search, ignoreCase = true) ||
                  it.packageName.contains(search, ignoreCase = true) }
        .take(80)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(500.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.OutlinedTextField(
                value = search, onValueChange = onSearch,
                label = { Text(stringResource(R.string.search_apps)) },
                modifier = Modifier.weight(1f), singleLine = true,
            )
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, null)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(if (showSystem) stringResource(R.string.hide_system_apps) else stringResource(R.string.show_system_apps)) },
                        onClick = { showSystem = !showSystem; showMenu = false }
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            filtered.forEach { app ->
                val iconBmp = remember(app.packageName) { app.icon.toBitmap(48, 48).asImageBitmap() }
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(app) }
                        .background(if (app.packageName == selectedPkg)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else Color.Transparent, RoundedCornerShape(12.dp))
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        bitmap             = iconBmp,
                        contentDescription = null,
                            modifier           = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text     = app.label,
                            style    = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text     = app.packageName,
                            style    = MaterialTheme.typography.bodySmall,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (app.packageName == selectedPkg) {
                        Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
