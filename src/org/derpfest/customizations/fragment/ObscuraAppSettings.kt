/*
 * SPDX-FileCopyrightText: 2026 kenway214
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package org.derpfest.customizations.fragment

import android.app.ActivityManager
import android.app.ObscuraManager
import android.app.role.RoleManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.os.UserHandle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import java.io.BufferedReader
import java.io.InputStreamReader
import org.json.JSONObject
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.internal.logging.nano.MetricsProto
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import com.android.settingslib.spa.framework.theme.SettingsDimension
import com.android.settingslib.spa.framework.theme.SettingsSpace
import com.android.settingslib.spa.framework.theme.SettingsTheme
import com.android.settingslib.spa.widget.button.ActionButton
import com.android.settingslib.spa.widget.button.ActionButtons
import com.android.settingslib.spa.widget.preference.IntroAppPreference
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spa.widget.preference.SwitchPreference
import com.android.settingslib.spa.widget.preference.SwitchPreferenceModel
import com.android.settingslib.spa.widget.preference.ZeroStatePreference
import com.android.settingslib.spa.widget.ui.Category
import com.android.settingslib.spa.widget.ui.CategoryTitle
import com.android.settingslib.spa.widget.ui.LazyCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ObscuraAppEntry(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val isSystem: Boolean = false,
    var isHidden: Boolean = false,
    var isLauncherHidden: Boolean = false,
    var isIsolated: Boolean = false,
    var isPlayStoreDetached: Boolean = false,
    var scopeMode: Int = ObscuraManager.SCOPE_MODE_DISABLED,
    var scopeList: Set<String> = emptySet(),
    var restrictInternet: Boolean = false,
    var restrictStorage: Boolean = false,
    var forceDataIsolation: Boolean = false,
    var spoofAdb: Boolean = false,
    var spoofDevOptions: Boolean = false,
    var spoofWirelessDebug: Boolean = false,
    var spoofPkgVerifier: Boolean = false,
    var spoofUsbVerify: Boolean = false,
    var spoofAccessibility: Boolean = false,
)

private val EXCLUDED_SUFFIXES = listOf(
    ".auto_generated", ".appsearch", ".backup", ".carrier",
    ".cellbroadcast", ".cts", ".federated", ".ims", ".overlay",
    ".qti", ".qualcomm", ".resources", ".systemui.clocks",
    ".systemui.plugin", ".theme", ".iconpack",
)

enum class PresetCategory(val labelRes: Int, val shortLabelRes: Int) {
    ALL(R.string.obscura_preset_all, R.string.obscura_preset_all_short),
    ROOT(R.string.obscura_preset_root, R.string.obscura_preset_root_short),
    PRIVACY_MODS(R.string.obscura_preset_privacy, R.string.obscura_preset_privacy_short),
    DETECTORS(R.string.obscura_preset_detectors, R.string.obscura_preset_detectors_short),
    TOOLS(R.string.obscura_preset_tools, R.string.obscura_preset_tools_short),
    SHIZUKU(R.string.obscura_preset_shizuku, R.string.obscura_preset_shizuku_short)
}

private val ROOT_MANAGERS_PACKAGES = setOf(
    "com.topjohnwu.magisk",
    "io.github.vvb2060.magisk",
    "io.github.huskydg.magisk",
    "me.weishu.kernelsu",
    "com.rifsxd.ksu",
    "com.sukisu.ultra",
    "io.github.a13e300.ksuwebui",
    "bmax.apatch",
    "me.bmax.apatch",
    "org.apatch",
    "com.dergoogler.mmrl",
    "com.fox2code.mmm",
    "org.lsposed.manager",
    "org.lsposed.lspd",
    "org.meowcat.edxposed.manager",
    "de.robv.android.xposed.installer",
    "com.solohsu.android.edxp.manager",
    "com.koushikdutta.superuser",
    "eu.chainfire.supersu",
    "com.noshufou.android.su",
    "me.jsonet.jshook",
    "com.simo.fhook",
    "com.omarea.vtools",
)

private val MODS_PRIVACY_PACKAGES = setOf(
    "com.drdisagree.iconify",
    "com.xayah.databackup",
    "com.machiav3lli.backup",
    "org.adaway",
    "me.itejo443.bindhosts",
    "be.mygod.vpnhotspot",
    "com.jhc.detach",
    "ua.polodarb.gmsflags",
    "ua.polodarb.gmsflags.reborn",
    "mattecarra.accapp",
    "com.softwarebakery.drivedroid",
    "me.twrp.twrpapp",
    "james.dsp",
    "flar2.exkernelmanager",
    "com.franco.kernel",
    "com.smartpack.kernelmanager",
    "com.smartpack.busyboxinstaller",
    "id.xms.xtrakernelmanager",
    "com.rve.rvkernelmanager",
    "ccc71.st.cpu",
    "com.lybxlpsv.kernelmanager",
    "com.html6405.boefflakernelconfig",
    "com.umang96.radon",
    "com.sunilpaulmathew.debloater",
    "com.paget96.lsandroid",
    "org.fdroid.fdroid.privileged",
    "chelpus.hook",
    "com.chelpus.lackypatch",
    "com.dimonvideo.luckypatcher",
    "com.formyhm.hma",
    "org.frknkrc44.hma_oss",
    "powersaver.pro",
    "io.chaldeaprjkt.gamespace",
    "org.droidspaces.app",
    "org.droidspaces",
    "com.ravindu.droidspaces",
    "io.github.ravindu644.droidspaces",
    "com.droidspace",
    "com.droidspaces",
    "com.oasisfeng.island",
    "net.typeblog.shelter",
    "com.lbe.parallel.intl",
    "com.parallel.space.lite",
    "com.valhalla.thor",
    "com.slash.batterychargelimit",
    "com.js.nowakelock",
    "com.mrsep.ttlchanger",
    "web1n.stopapp",
    "com.byyoung.setting",
    "x1125io.initdlight",
    "ru.nsu.bobrofon.easysshfs",
    "org.nuntius35.wrongpinshutdown",
    "com.bartixxx.opflashcontrol",
    "com.gitlab.giwiniswut.rwremount",
    "ca.mudar.fairphone.peaceofmind",
    "ru.evgeniy.dpitunnel",
    "simple.reboot.com",
    "de.buttercookie.simbadroid",
    "com.zinaro.cachecleanerwidget",
    "com.corphish.nightlight.generic",
    "tk.giesecke.phoenix",
    "at.or.at.plugoffairplane",
    "eu.roggstar.luigithehunter.batterycalibrate",
    "io.github.domi04151309.powerapp",
    "eu.roggstar.getmitokens",
    "com.garyodernichts.downgrader",
    "id.kuato.diskhealth",
)

private val DETECTOR_PACKAGES = setOf(
    "icu.nullptr.applistdetector",
    "com.zhenxi.hunter",
    "io.github.vvb2060.packagehunter",
    "io.github.vvb2060.mahoshojo",
    "io.github.huskydg.memorydetector",
    "org.akanework.checker",
    "com.reveny.nativecheck",
    "icu.nullptr.nativetest",
    "io.github.rabehx.securify",
    "com.byxiaorun.detector",
    "com.kimchangyoun.rootbeerFresh.sample",
    "com.androidfung.drminfo",
    "com.atominvention.rootchecker",
    "com.joeykrim.rootcheck",
    "com.longz.detector",
    "com.anycheck.app",
    "by.sheerboy.femboydetector",
    "com.lingqing.detector",
    "com.android.nativetest",
    "com.youhu.laifu",
    "wu.Rookie.Detector",
    "wu.Zygisk.Detector",
    "com.fkjc.zcro",
    "wu.keyChain.test",
    "at.persie0.root_detection_app",
    "at.austriao.fake_gps_detector_app",
    "io.ngankbakaa.lineage.detector",
    "com.dexprotector.detector.envchecks",
    "krypton.tbsafetychecker",
    "gr.nikolasspyr.integritycheck",
    "com.henrikherzig.playintegritychecker",
    "com.thend.integritychecker",
    "com.flinkapps.safteynet",
    "com.bryancandi.knoxcheck",
    "catch_.me_.if_.you_.can_",
    "com.chiteroman",
    "com.kikyps.crackme",
    "org.matrix.demo",
    "com.rem01gaming.disclosure",
    "luna.safe.luna",
    "com.AndroLua",
    "com.detect.mt",
    "io.liankong.riskdetector",
    "com.suisho.rc",
    "com.ahmed.security_tester",
    "id.my.pjm.qbcd_okr_dvii",
)

private val DEV_TOOLS_PACKAGES = setOf(
    "bin.mt.plus",
    "com.speedsoftware.rootexplorer",
    "com.lonelycatgames.Xplore",
    "me.zhanghai.android.files",
    "org.fossify.filemanager",
    "com.amaze.filemanager",
    "berserker.android.apps.sshdroid",
    "org.connectbot",
    "com.iamaner.oneclickfreeze",
    "com.shamanland.privatescreenshots",
    "com.devolutions.remotedesktopmanager",
    "com.anydesk.anydeskandroid",
    "com.carriez.flutter_hbb",
    "com.happymod.apk",
    "com.pd.pdhelper",
    "cm.aptoide.pt",
    "com.mayank.rucky",
    "org.csploit.android",
    "whid.usb.injector",
    "de.tu_darmstadt.seemoo.nexmon",
    "remote.hid.keyboard.client",
    "de.srlabs.snoopsnitch",
    "com.hijacker",
    "su.sniff.cepter",
    "com.rk.taskmanager",
    "com.emanuelef.remote_capture",
    "com.tortel.syslog",
    "com.tester.wpswpatester",
)

private val SHIZUKU_PACKAGES = setOf(
    "moe.shizuku.privileged.api",
    "com.rosan.dhizuku",
)

private fun getAppPresetCategory(packageName: String): PresetCategory? {
    if (ROOT_MANAGERS_PACKAGES.contains(packageName) ||
        packageName.endsWith(".magisk") ||
        packageName.contains(".apatch") ||
        packageName.startsWith("org.lsposed.") ||
        packageName.contains("kernelsu") ||
        packageName.contains(".ksu") ||
        packageName.contains("xposed")
    ) {
        return PresetCategory.ROOT
    }

    if (SHIZUKU_PACKAGES.contains(packageName) || packageName.startsWith("moe.shizuku.")) {
        return PresetCategory.SHIZUKU
    }

    if (DETECTOR_PACKAGES.contains(packageName) ||
        packageName.startsWith("me.garfieldhan.") ||
        packageName.contains("chunqiu") ||
        packageName.contains("chuqniu") ||
        packageName.endsWith(".duckdetector") ||
        packageName.endsWith(".keyattestation")
    ) {
        return PresetCategory.DETECTORS
    }

    if (DEV_TOOLS_PACKAGES.contains(packageName) ||
        packageName.startsWith("bin.mt.") ||
        packageName.startsWith("com.mixplorer") ||
        packageName.startsWith("nextapp.fx") ||
        packageName.startsWith("ru.zdevs.") ||
        packageName.startsWith("com.termux") ||
        packageName.startsWith("com.offsec.") ||
        packageName.startsWith("com.ghisler.")
    ) {
        return PresetCategory.TOOLS
    }

    if (MODS_PRIVACY_PACKAGES.contains(packageName) ||
        packageName.startsWith("dev.ukanth.ufirewall") ||
        packageName.endsWith(".viper4android") ||
        packageName.endsWith(".viperfx") ||
        packageName.startsWith("xzr.") ||
        packageName.startsWith("moe.xzr.") ||
        packageName.contains(".busybox") ||
        packageName.startsWith("com.drdisagree.iconify") ||
        packageName.startsWith("com.xayah.databackup") ||
        packageName.startsWith("com.smartpack.") ||
        packageName.contains("luckypatcher") ||
        packageName.contains("droidspace") ||
        packageName.contains("droidspaces")
    ) {
        return PresetCategory.PRIVACY_MODS
    }

    return null
}

val ObscuraAppEntry.isConfigured: Boolean
    get() = isHidden || isLauncherHidden || isIsolated || isPlayStoreDetached || scopeMode != ObscuraManager.SCOPE_MODE_DISABLED
            || spoofAdb || spoofDevOptions || spoofWirelessDebug || spoofPkgVerifier || spoofUsbVerify || spoofAccessibility

class ObscuraAppSettings : SettingsPreferenceFragment() {

    private val reloadTrigger = mutableStateOf(0)
    private var jsonToExport: String? = null

    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            val content = jsonToExport
            if (content != null) {
                performExport(it, content)
            }
        }
    }

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            performImport(uri)
        }
    }

    private fun readTextFromUri(uri: Uri): String {
        val stringBuilder = StringBuilder()
        requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    stringBuilder.append(line)
                    line = reader.readLine()
                }
            }
        }
        return stringBuilder.toString()
    }

    private fun performExport(uri: Uri, jsonContent: String) {
        try {
            requireContext().contentResolver.openOutputStream(uri)?.use { os ->
                os.write(jsonContent.toByteArray())
                os.flush()
            }
            Toast.makeText(
                requireContext(),
                R.string.obscura_backup_export_success,
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                R.string.obscura_backup_export_failed,
                Toast.LENGTH_SHORT
            ).show()
        } finally {
            jsonToExport = null
        }
    }

    private fun performImport(uri: Uri) {
        try {
            val content = readTextFromUri(uri)
            val json = JSONObject(content)
            Settings.Secure.putString(
                requireContext().contentResolver,
                ObscuraManager.SETTING_OBSCURA_CONFIG,
                json.toString()
            )
            reloadTrigger.value += 1
            Toast.makeText(
                requireContext(),
                R.string.obscura_backup_import_success,
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                R.string.obscura_backup_import_failed,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().title = getString(R.string.obscura_title)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        val importItem = menu.add(0, 1, 0, R.string.obscura_backup_menu_import)
        importItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)

        val exportItem = menu.add(0, 2, 0, R.string.obscura_backup_menu_export)
        exportItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            1 -> {
                importLauncher.launch(arrayOf("application/json", "*/*"))
                true
            }
            2 -> {
                val currentConfig = Settings.Secure.getString(
                    requireContext().contentResolver,
                    ObscuraManager.SETTING_OBSCURA_CONFIG
                ) ?: "{}"
                val formatted = try {
                    JSONObject(currentConfig).toString(2)
                } catch (e: Exception) {
                    "{}"
                }
                jsonToExport = formatted
                exportLauncher.launch("obscura_backup.json")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun getMetricsCategory() = MetricsProto.MetricsEvent.VIEW_UNKNOWN

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            SettingsTheme {
                ObscuraAppSettingsContent(
                    context = requireContext(),
                    reloadKey = reloadTrigger.value,
                )
            }
        }
    }
}

@Composable
private fun ObscuraAppSettingsContent(
    context: Context,
    reloadKey: Int = 0,
) {
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var showSystemApps by remember { mutableStateOf(false) }
    var filterConfiguredOnly by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedAppPackage by remember { mutableStateOf<String?>(null) }
    var allApps by remember { mutableStateOf<List<ObscuraAppEntry>>(emptyList()) }

    val obscuraManager = remember {
        context.getSystemService(Context.OBSCURA_SERVICE) as? ObscuraManager
    }

    fun loadAppStates(): Map<String, ObscuraAppEntry> {
        if (obscuraManager == null) return emptyMap()
        val result = mutableMapOf<String, ObscuraAppEntry>()

        for (pkg in obscuraManager.hiddenPackages) {
            result.getOrPut(pkg) { ObscuraAppEntry(pkg, "", null) }.isHidden = true
        }
        for (pkg in obscuraManager.launcherHiddenPackages) {
            result.getOrPut(pkg) { ObscuraAppEntry(pkg, "", null) }.isLauncherHidden = true
        }
        for (pkg in obscuraManager.detachedPackages) {
            result.getOrPut(pkg) { ObscuraAppEntry(pkg, "", null) }.isPlayStoreDetached = true
        }
        for (pkg in obscuraManager.isolatedPackages) {
            val entry = result.getOrPut(pkg) { ObscuraAppEntry(pkg, "", null) }
            entry.isIsolated = true
            val restrictedGids = obscuraManager.getRestrictedGids(pkg)
            if (restrictedGids != null) {
                for (gid in restrictedGids) {
                    when (gid) {
                        ObscuraManager.GID_INET -> entry.restrictInternet = true
                        in ObscuraManager.STORAGE_GIDS -> entry.restrictStorage = true
                    }
                }
            }
            entry.forceDataIsolation = obscuraManager.isDataIsolationEnabled(pkg)
        }

        return result
    }

    fun saveEntry(entry: ObscuraAppEntry) {
        allApps = allApps.map { if (it.packageName == entry.packageName) entry else it }
        if (obscuraManager == null) return
        scope.launch(Dispatchers.IO) {
            val launcherToggled = entry.isHidden || entry.isLauncherHidden
            obscuraManager.setPackageHidden(entry.packageName, entry.isHidden)
            obscuraManager.setPackageLauncherHidden(entry.packageName, entry.isLauncherHidden)
            obscuraManager.setPackageDetached(entry.packageName, entry.isPlayStoreDetached)

            // Save Scope Mode & Target Scope List
            obscuraManager.setAppScopeMode(entry.packageName, entry.scopeMode)
            obscuraManager.setAppScopeList(entry.packageName, entry.scopeList.toList())

            // Master Isolation & Hardware/Storage restrictions
            if (entry.isIsolated) {
                obscuraManager.isolatePackage(entry.packageName)
                val gids = mutableListOf<Int>()
                if (entry.restrictInternet) gids.add(ObscuraManager.GID_INET)
                if (entry.restrictStorage) gids.addAll(ObscuraManager.STORAGE_GIDS.toList())
                obscuraManager.setRestrictedGids(entry.packageName, gids.toIntArray())
                obscuraManager.setDataIsolationEnabled(entry.packageName, entry.forceDataIsolation)
            } else {
                obscuraManager.unisolatePackage(entry.packageName)
                obscuraManager.setRestrictedGids(entry.packageName, intArrayOf())
                obscuraManager.setDataIsolationEnabled(entry.packageName, false)
            }

            // Spoof settings are managed independently
            obscuraManager.setSpoofSettingEnabled(entry.packageName, "adb_enabled", entry.spoofAdb)
            obscuraManager.setSpoofSettingEnabled(entry.packageName, "development_settings_enabled", entry.spoofDevOptions)
            obscuraManager.setSpoofSettingEnabled(entry.packageName, "adb_wifi_enabled", entry.spoofWirelessDebug)
            obscuraManager.setSpoofSettingEnabled(entry.packageName, "package_verifier_user_consent", entry.spoofPkgVerifier)
            obscuraManager.setSpoofSettingEnabled(entry.packageName, "verify_apps_over_usb", entry.spoofUsbVerify)
            obscuraManager.setSpoofSettingEnabled(entry.packageName, "accessibility_enabled", entry.spoofAccessibility)

            forceStopPackage(context, entry.packageName)
            if (launcherToggled) forceStopDefaultLauncher(context)
            forceStopPackage(context, "com.android.vending")
        }
    }

    LaunchedEffect(reloadKey) {
        isLoading = true
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val appStates = loadAppStates()
            val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { app ->
                    val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM != 0) &&
                            (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP == 0)
                    val isExcluded = EXCLUDED_SUFFIXES.any { app.packageName.endsWith(it) || app.packageName.contains(it) }
                    if (isSystem && isExcluded && !appStates.containsKey(app.packageName)) {
                        return@filter false
                    }
                    true
                }
                .map { app ->
                    val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM != 0) &&
                            (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP == 0)
                    val existing = appStates[app.packageName]
                    val scopeMode = obscuraManager?.getAppScopeMode(app.packageName) ?: ObscuraManager.SCOPE_MODE_DISABLED
                    val scopeList = if (scopeMode != ObscuraManager.SCOPE_MODE_DISABLED) {
                        obscuraManager?.getAppScopeList(app.packageName)?.toSet() ?: emptySet()
                    } else {
                        emptySet()
                    }
                    val enabledSpoofs = obscuraManager?.getEnabledSpoofSettings(app.packageName) ?: emptyList()

                    ObscuraAppEntry(
                        packageName = app.packageName,
                        label = runCatching { app.loadLabel(pm).toString() }.getOrDefault(app.packageName),
                        icon = runCatching { app.loadIcon(pm) }.getOrNull(),
                        isSystem = isSystem,
                        isHidden = existing?.isHidden ?: false,
                        isLauncherHidden = existing?.isLauncherHidden ?: false,
                        isIsolated = existing?.isIsolated ?: false,
                        isPlayStoreDetached = existing?.isPlayStoreDetached ?: false,
                        scopeMode = scopeMode,
                        scopeList = scopeList,
                        restrictInternet = existing?.restrictInternet ?: false,
                        restrictStorage = existing?.restrictStorage ?: false,
                        forceDataIsolation = existing?.forceDataIsolation ?: false,
                        spoofAdb = enabledSpoofs.contains("adb_enabled"),
                        spoofDevOptions = enabledSpoofs.contains("development_settings_enabled"),
                        spoofWirelessDebug = enabledSpoofs.contains("adb_wifi_enabled"),
                        spoofPkgVerifier = enabledSpoofs.contains("package_verifier_user_consent"),
                        spoofUsbVerify = enabledSpoofs.contains("verify_apps_over_usb"),
                        spoofAccessibility = enabledSpoofs.contains("accessibility_enabled"),
                    )
                }
                .sortedWith(compareBy(
                    { !it.isConfigured },
                    { it.isSystem },
                    { it.label.lowercase() }
                ))

            withContext(Dispatchers.Main) {
                allApps = installed
                isLoading = false
            }
        }
    }

    val selectedApp = allApps.find { it.packageName == selectedAppPackage }

    DisposableEffect(selectedAppPackage, selectedApp?.label) {
        val activity = context as? android.app.Activity
        activity?.title = selectedApp?.label ?: context.getString(R.string.obscura_title)
        onDispose {}
    }

    Crossfade(targetState = selectedApp != null, label = "ScreenTransition") { inDetailScreen ->
        if (inDetailScreen && selectedApp != null) {
            BackHandler { selectedAppPackage = null }
            ObscuraAppDetailScreen(
                app = selectedApp,
                allApps = allApps,
                onBack = { selectedAppPackage = null },
                onUpdate = { updated ->
                    saveEntry(updated)
                }
            )
        } else {
            ObscuraAppListScreen(
                allApps = allApps,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                showSystemApps = showSystemApps,
                onToggleShowSystemApps = { showSystemApps = !showSystemApps },
                filterConfiguredOnly = filterConfiguredOnly,
                onToggleFilterConfiguredOnly = { filterConfiguredOnly = !filterConfiguredOnly },
                isLoading = isLoading,
                onSelectApp = { selectedAppPackage = it.packageName }
            )
        }
    }
}

@Composable
private fun ObscuraAppListScreen(
    allApps: List<ObscuraAppEntry>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    showSystemApps: Boolean,
    onToggleShowSystemApps: () -> Unit,
    filterConfiguredOnly: Boolean,
    onToggleFilterConfiguredOnly: () -> Unit,
    isLoading: Boolean,
    onSelectApp: (ObscuraAppEntry) -> Unit,
) {
    val filteredApps = remember(searchQuery, allApps, showSystemApps, filterConfiguredOnly) {
        val query = searchQuery.trim().lowercase()
        allApps.filter { app ->
            if (!showSystemApps && app.isSystem && !app.isConfigured) {
                return@filter false
            }
            if (filterConfiguredOnly && !app.isConfigured) {
                return@filter false
            }
            if (query.isNotEmpty() && !app.label.lowercase().contains(query) && !app.packageName.lowercase().contains(query)) {
                return@filter false
            }
            true
        }
    }

    val activeCount = allApps.count { it.isConfigured }
    val scopedCount = allApps.count { it.scopeMode != ObscuraManager.SCOPE_MODE_DISABLED }
    val hiddenCount = allApps.count { it.isHidden || it.isLauncherHidden }
    val summary = if (activeCount == 0) {
        stringResource(R.string.obscura_none_protected)
    } else {
        stringResource(R.string.obscura_protected_summary, activeCount, scopedCount, hiddenCount)
    }

    LazyCategory(
        count = if (isLoading) 0 else filteredApps.size,
        key = { index -> filteredApps[index].packageName },
        bottomPadding = SettingsSpace.medium5,
        header = {
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = SettingsSpace.extraSmall4),
            )
            AppPickerSearchField(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = SettingsSpace.extraSmall4),
                horizontalArrangement = Arrangement.spacedBy(SettingsSpace.extraSmall4),
            ) {
                FilterChip(
                    selected = showSystemApps,
                    onClick = onToggleShowSystemApps,
                    label = { Text(stringResource(R.string.obscura_system_apps)) },
                    leadingIcon = if (showSystemApps) {
                        { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) }
                    } else {
                        null
                    },
                )
                FilterChip(
                    selected = filterConfiguredOnly,
                    onClick = onToggleFilterConfiguredOnly,
                    label = { Text(stringResource(R.string.obscura_configured_only, activeCount)) },
                    leadingIcon = if (filterConfiguredOnly) {
                        { Icon(Icons.Default.FilterList, null, modifier = Modifier.size(18.dp)) }
                    } else {
                        null
                    },
                )
            }
        },
        footer = {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(SettingsSpace.medium1),
                        contentAlignment = Alignment.Center,
                    ) {
                        LoadingIndicator()
                    }
                }
                filteredApps.isEmpty() -> {
                    ZeroStatePreference(
                        icon = Icons.Filled.Shield,
                        text = stringResource(R.string.obscura_no_matching_apps),
                    )
                }
            }
        },
    ) { index ->
        val app = filteredApps[index]
        val summaryText = app.statusSummary()
        Preference(
            model = object : PreferenceModel {
                override val title = app.label
                override val summary = { summaryText }
                override val icon: @Composable (() -> Unit) = { SettingsAppIcon(app.icon) }
                override val onClick: () -> Unit = { onSelectApp(app) }
            },
            singleLineSummary = true,
        )
    }
}

@Composable
private fun ObscuraAppEntry.statusSummary(): String {
    if (!isConfigured) return packageName
    val parts = mutableListOf<String>()
    if (scopeMode == ObscuraManager.SCOPE_MODE_BLACKLIST) {
        parts += stringResource(R.string.obscura_scope_blacklist_count, scopeList.size)
    } else if (scopeMode == ObscuraManager.SCOPE_MODE_WHITELIST) {
        parts += stringResource(R.string.obscura_scope_whitelist_count, scopeList.size)
    }
    if (isIsolated) parts += stringResource(R.string.obscura_badge_isolated)
    if (isPlayStoreDetached) parts += stringResource(R.string.obscura_badge_detached)
    val hasSpoof = spoofAdb || spoofDevOptions || spoofWirelessDebug ||
        spoofPkgVerifier || spoofUsbVerify || spoofAccessibility
    if (hasSpoof) parts += stringResource(R.string.obscura_badge_spoofed)
    if (isHidden) parts += stringResource(R.string.obscura_badge_hidden)
    else if (isLauncherHidden) parts += stringResource(R.string.obscura_launcher_hidden)
    return parts.joinToString(" · ").ifBlank { packageName }
}

@Composable
private fun ObscuraAppDetailScreen(
    app: ObscuraAppEntry,
    allApps: List<ObscuraAppEntry>,
    onBack: () -> Unit,
    onUpdate: (ObscuraAppEntry) -> Unit,
) {
    val context = LocalContext.current
    var scopeSearchQuery by remember { mutableStateOf("") }
    var showScopeTargetList by remember { mutableStateOf(false) }
    var showPresetDialog by remember { mutableStateOf(false) }
    var showRestartDialog by remember { mutableStateOf(false) }

    val otherApps = remember(allApps, app.packageName) {
        allApps.filter { it.packageName != app.packageName }
    }

    val filteredOtherApps = remember(otherApps, scopeSearchQuery) {
        val query = scopeSearchQuery.lowercase()
        otherApps.filter {
            query.isEmpty() || it.label.lowercase().contains(query) || it.packageName.lowercase().contains(query)
        }
    }

    val scopeModes = listOf(
        ObscuraManager.SCOPE_MODE_DISABLED to R.string.obscura_disabled,
        ObscuraManager.SCOPE_MODE_BLACKLIST to R.string.obscura_blacklist,
        ObscuraManager.SCOPE_MODE_WHITELIST to R.string.obscura_whitelist,
    )

    Scaffold(containerColor = Color.Transparent) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            FilledTonalIconButton(
                onClick = onBack,
                modifier = Modifier.padding(start = SettingsSpace.extraSmall4, top = SettingsSpace.extraSmall4),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.obscura_back),
                )
            }

            IntroAppPreference(
                title = app.label,
                descriptions = listOf(
                    app.packageName,
                    stringResource(
                        if (app.isSystem) R.string.obscura_system_application
                        else R.string.obscura_user_installed,
                    ),
                ),
                appIcon = { SettingsAppIcon(app.icon, size = SettingsDimension.itemIconContainerSize) },
            )

            val launchLabel = stringResource(R.string.obscura_launch_app)
            val forceStopLabel = stringResource(R.string.obscura_force_stop)
            val actions = buildList {
                if (app.isHidden || app.isLauncherHidden) {
                    add(
                        ActionButton(
                            text = launchLabel,
                            imageVector = Icons.Filled.PlayArrow,
                            onClick = { launchApp(context, app.packageName) },
                        ),
                    )
                }
                add(
                    ActionButton(
                        text = forceStopLabel,
                        imageVector = Icons.Filled.Clear,
                        onClick = {
                            forceStopPackage(context, app.packageName)
                            Toast.makeText(
                                context,
                                context.getString(R.string.obscura_force_stopped, app.label),
                                Toast.LENGTH_SHORT,
                            ).show()
                        },
                    ),
                )
            }
            ActionButtons(actions)

            SectionHeader(stringResource(R.string.obscura_scope_section_title))
            SectionDescription(stringResource(R.string.obscura_scope_subtitle))
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SettingsSpace.small1),
                ) {
                    for ((index, modeLabel) in scopeModes.withIndex()) {
                        val (mode, labelRes) = modeLabel
                        SegmentedButton(
                            selected = app.scopeMode == mode,
                            onClick = { onUpdate(app.copy(scopeMode = mode)) },
                            shape = SegmentedButtonDefaults.itemShape(index, scopeModes.size),
                            label = {
                                Text(
                                    stringResource(labelRes),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                        )
                    }
                }
                if (app.scopeMode != ObscuraManager.SCOPE_MODE_DISABLED) {
                    val isBlacklist = app.scopeMode == ObscuraManager.SCOPE_MODE_BLACKLIST
                    Text(
                        text = if (isBlacklist) {
                            stringResource(R.string.obscura_blacklist_mode_summary, app.scopeList.size, app.label)
                        } else {
                            stringResource(R.string.obscura_whitelist_mode_summary, app.label, app.scopeList.size)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = SettingsSpace.small1, vertical = SettingsSpace.extraSmall4),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = SettingsSpace.small1),
                        horizontalArrangement = Arrangement.spacedBy(SettingsSpace.extraSmall4),
                    ) {
                        FilledTonalButton(
                            onClick = { showPresetDialog = true },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(SettingsSpace.extraSmall2))
                            Text(stringResource(R.string.obscura_add_presets))
                        }
                        OutlinedButton(
                            onClick = {
                                val userPkgs = otherApps.filter { !it.isSystem }.map { it.packageName }.toSet()
                                onUpdate(app.copy(scopeList = userPkgs))
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.obscura_add_all_user_apps))
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = SettingsSpace.small1, vertical = SettingsSpace.extraSmall4),
                        horizontalArrangement = Arrangement.spacedBy(SettingsSpace.extraSmall4),
                    ) {
                        Button(
                            onClick = { showScopeTargetList = !showScopeTargetList },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                if (showScopeTargetList) {
                                    stringResource(R.string.obscura_hide_scope_picker)
                                } else {
                                    stringResource(R.string.obscura_configure_scope, app.scopeList.size)
                                },
                            )
                        }
                        if (app.scopeList.isNotEmpty()) {
                            OutlinedButton(onClick = { onUpdate(app.copy(scopeList = emptySet())) }) {
                                Text(stringResource(R.string.obscura_clear))
                            }
                        }
                    }
                    AnimatedVisibility(
                        visible = showScopeTargetList,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        Column(modifier = Modifier.padding(horizontal = SettingsSpace.small1)) {
                            AppPickerSearchField(
                                query = scopeSearchQuery,
                                onQueryChange = { scopeSearchQuery = it },
                            )
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(320.dp)
                                    .padding(top = SettingsSpace.extraSmall4),
                                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                            ) {
                                itemsIndexed(filteredOtherApps, key = { _, item -> item.packageName }) { index, targetApp ->
                                    val isChecked = app.scopeList.contains(targetApp.packageName)
                                    SegmentedListItem(
                                        onClick = {
                                            val newSet = if (isChecked) {
                                                app.scopeList - targetApp.packageName
                                            } else {
                                                app.scopeList + targetApp.packageName
                                            }
                                            onUpdate(app.copy(scopeList = newSet))
                                        },
                                        shapes = ListItemDefaults.segmentedShapes(index, filteredOtherApps.size),
                                        leadingContent = { SettingsAppIcon(targetApp.icon) },
                                        supportingContent = {
                                            Text(
                                                targetApp.packageName,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        },
                                        trailingContent = {
                                            Checkbox(checked = isChecked, onCheckedChange = null)
                                        },
                                    ) {
                                        Text(
                                            targetApp.label,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

            SectionHeader(stringResource(R.string.obscura_hardware_section_title))
            SectionDescription(stringResource(R.string.obscura_hardware_subtitle))
            Category {
                ObscuraSwitch(
                    icon = Icons.Default.Shield,
                    title = stringResource(R.string.obscura_master_isolation),
                    subtitle = stringResource(R.string.obscura_master_isolation_subtitle),
                    checked = app.isIsolated,
                    onCheckedChange = { onUpdate(app.copy(isIsolated = it)) },
                )
                if (app.isIsolated) {
                    ObscuraSwitch(
                        icon = Icons.Default.Public,
                        title = stringResource(R.string.obscura_restrict_internet),
                        subtitle = stringResource(R.string.obscura_restrict_internet_subtitle),
                        checked = app.restrictInternet,
                        onCheckedChange = { onUpdate(app.copy(restrictInternet = it)) },
                    )
                    ObscuraSwitch(
                        icon = Icons.Default.Storage,
                        title = stringResource(R.string.obscura_restrict_storage),
                        subtitle = stringResource(R.string.obscura_restrict_storage_subtitle),
                        checked = app.restrictStorage,
                        onCheckedChange = { onUpdate(app.copy(restrictStorage = it)) },
                    )
                    ObscuraSwitch(
                        icon = Icons.Default.Security,
                        title = stringResource(R.string.obscura_data_isolation),
                        subtitle = stringResource(R.string.obscura_data_isolation_subtitle),
                        checked = app.forceDataIsolation,
                        onCheckedChange = { onUpdate(app.copy(forceDataIsolation = it)) },
                    )
                }
            }

            SectionHeader(stringResource(R.string.obscura_spoof_section_title))
            SectionDescription(stringResource(R.string.obscura_spoof_section_subtitle))
            TextButton(
                    onClick = {
                        val allEnabled = app.spoofAdb && app.spoofDevOptions && app.spoofWirelessDebug &&
                            app.spoofPkgVerifier && app.spoofUsbVerify && app.spoofAccessibility
                        val toggleTo = !allEnabled
                        onUpdate(
                            app.copy(
                                spoofAdb = toggleTo,
                                spoofDevOptions = toggleTo,
                                spoofWirelessDebug = toggleTo,
                                spoofPkgVerifier = toggleTo,
                                spoofUsbVerify = toggleTo,
                                spoofAccessibility = toggleTo,
                            ),
                        )
                    },
                    modifier = Modifier.padding(horizontal = SettingsSpace.extraSmall4),
                ) {
                    Text(stringResource(R.string.obscura_toggle_all_spoofs))
                }
            Category {
                ObscuraSwitch(
                    icon = Icons.Default.Code,
                    title = stringResource(R.string.obscura_spoof_adb),
                    checked = app.spoofAdb,
                    onCheckedChange = { onUpdate(app.copy(spoofAdb = it)) },
                )
                ObscuraSwitch(
                    icon = Icons.Default.Tune,
                    title = stringResource(R.string.obscura_spoof_dev_options),
                    checked = app.spoofDevOptions,
                    onCheckedChange = { onUpdate(app.copy(spoofDevOptions = it)) },
                )
                ObscuraSwitch(
                    icon = Icons.Default.Code,
                    title = stringResource(R.string.obscura_spoof_wireless_debug),
                    checked = app.spoofWirelessDebug,
                    onCheckedChange = { onUpdate(app.copy(spoofWirelessDebug = it)) },
                )
                ObscuraSwitch(
                    icon = Icons.Default.Security,
                    title = stringResource(R.string.obscura_spoof_pkg_verifier),
                    checked = app.spoofPkgVerifier,
                    onCheckedChange = { onUpdate(app.copy(spoofPkgVerifier = it)) },
                )
                ObscuraSwitch(
                    icon = Icons.Default.Security,
                    title = stringResource(R.string.obscura_spoof_usb_verify),
                    checked = app.spoofUsbVerify,
                    onCheckedChange = { onUpdate(app.copy(spoofUsbVerify = it)) },
                )
                ObscuraSwitch(
                    icon = Icons.Default.Security,
                    title = stringResource(R.string.obscura_spoof_accessibility),
                    checked = app.spoofAccessibility,
                    onCheckedChange = { onUpdate(app.copy(spoofAccessibility = it)) },
                )
            }

            SectionHeader(stringResource(R.string.obscura_visibility_section_title))
            SectionDescription(stringResource(R.string.obscura_visibility_subtitle))
            Category {
                ObscuraSwitch(
                    icon = Icons.Default.VisibilityOff,
                    title = stringResource(R.string.obscura_hide_launcher),
                    subtitle = if (app.isHidden) {
                        stringResource(R.string.obscura_included_in_global_hide)
                    } else {
                        stringResource(R.string.obscura_hide_launcher_subtitle)
                    },
                    checked = app.isHidden || app.isLauncherHidden,
                    enabled = !app.isHidden,
                    onCheckedChange = { onUpdate(app.copy(isLauncherHidden = it)) },
                )
                ObscuraSwitch(
                    icon = Icons.Default.PlayArrow,
                    title = stringResource(R.string.obscura_detach_playstore_title),
                    subtitle = if (app.isHidden) {
                        stringResource(R.string.obscura_included_in_global_hide)
                    } else {
                        stringResource(R.string.obscura_detach_playstore_summary)
                    },
                    checked = app.isHidden || app.isPlayStoreDetached,
                    enabled = !app.isHidden,
                    onCheckedChange = { newState ->
                        if (!newState && app.isPlayStoreDetached) {
                            onUpdate(app.copy(isPlayStoreDetached = false))
                            showRestartDialog = true
                        } else {
                            onUpdate(app.copy(isPlayStoreDetached = newState))
                        }
                    },
                )
                ObscuraSwitch(
                    icon = Icons.Default.Lock,
                    title = stringResource(R.string.obscura_hide_global),
                    subtitle = stringResource(R.string.obscura_hide_global_subtitle),
                    checked = app.isHidden,
                    onCheckedChange = { newState ->
                        if (!newState && app.isHidden) {
                            onUpdate(app.copy(isHidden = false))
                            showRestartDialog = true
                        } else {
                            onUpdate(app.copy(isHidden = newState))
                        }
                    },
                )
            }
            Spacer(Modifier.height(SettingsSpace.small4))
        }
    }

    if (showPresetDialog) {
        PresetPickerDialog(
            otherApps = otherApps,
            currentScope = app.scopeList,
            onDismiss = { showPresetDialog = false },
            onApply = { selectedFromPreset ->
                onUpdate(app.copy(scopeList = app.scopeList + selectedFromPreset))
            },
        )
    }

    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { showRestartDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            title = { Text(stringResource(R.string.obscura_reboot_dialog_title)) },
            text = { Text(stringResource(R.string.obscura_reboot_dialog_message)) },
            confirmButton = {
                Button(onClick = {
                    showRestartDialog = false
                    rebootDevice(context)
                }) {
                    Text(stringResource(R.string.obscura_reboot_dialog_button_now))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestartDialog = false }) {
                    Text(stringResource(R.string.obscura_reboot_dialog_button_later))
                }
            },
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Box(Modifier.padding(horizontal = SettingsSpace.small1)) {
        CategoryTitle(title)
    }
}

@Composable
private fun SectionDescription(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(
            start = SettingsSpace.small4,
            end = SettingsSpace.small4,
            bottom = SettingsSpace.extraSmall4,
        ),
    )
}

@Composable
private fun ObscuraSwitch(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    SwitchPreference(
        model = object : SwitchPreferenceModel {
            override val title = title
            override val summary = { subtitle.orEmpty() }
            override val icon: @Composable (() -> Unit) = {
                Icon(icon, contentDescription = null)
            }
            override val checked = { checked }
            override val changeable = { enabled }
            override val onCheckedChange = onCheckedChange
        },
    )
}

private fun getDefaultLauncher(context: Context): String {
    val roleManager = context.getSystemService(RoleManager::class.java)
    return roleManager?.getRoleHolders(RoleManager.ROLE_HOME)?.firstOrNull() ?: ""
}

private fun forceStopDefaultLauncher(context: Context) {
    try {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return
        val launcher = getDefaultLauncher(context)
        if (launcher.isNotEmpty()) {
            am.forceStopPackageAsUser(launcher, UserHandle.USER_CURRENT)
        }
    } catch (e: Exception) {
        Log.e("Obscura", "Error force stopping launcher", e)
    }
}

private fun forceStopPackage(context: Context, packageName: String) {
    try {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return
        am.forceStopPackageAsUser(packageName, UserHandle.USER_CURRENT)
    } catch (e: Exception) {
        Log.e("Obscura", "Error force stopping $packageName", e)
    }
}

private fun launchApp(context: Context, packageName: String) {
    try {
        val obscuraManager = context.getSystemService(Context.OBSCURA_SERVICE) as? ObscuraManager
        obscuraManager?.launchHiddenApp(packageName)
    } catch (e: Exception) {
        Log.e("Obscura", "Error launching $packageName", e)
    }
}

private fun rebootDevice(context: Context) {
    try {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        pm?.reboot(null)
    } catch (e: Exception) {
        Log.e("Obscura", "Error rebooting device", e)
    }
}

@Composable
private fun PresetPickerDialog(
    otherApps: List<ObscuraAppEntry>,
    currentScope: Set<String>,
    onDismiss: () -> Unit,
    onApply: (Set<String>) -> Unit,
) {
    val presetMatchedApps = remember(otherApps) {
        otherApps.mapNotNull { app ->
            val cat = getAppPresetCategory(app.packageName)
            if (cat != null) app to cat else null
        }
    }

    var selectedCategory by remember { mutableStateOf(PresetCategory.ALL) }
    var searchQuery by remember { mutableStateOf("") }

    val selectedPackages = remember {
        mutableStateListOf<String>().apply {
            addAll(presetMatchedApps.map { it.first.packageName })
        }
    }

    val filteredApps = remember(presetMatchedApps, selectedCategory, searchQuery) {
        val query = searchQuery.trim().lowercase()
        presetMatchedApps.filter { (app, cat) ->
            (selectedCategory == PresetCategory.ALL || cat == selectedCategory) &&
                    (query.isEmpty() || app.label.lowercase().contains(query) || app.packageName.lowercase().contains(query))
        }
    }

    val totalMatchedCount = presetMatchedApps.size
    val currentSelectedCount = selectedPackages.size

    ExpressiveSheet(onDismiss = onDismiss) {
        Text(
            text = stringResource(R.string.obscura_presets_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = if (totalMatchedCount == 0) {
                stringResource(R.string.obscura_no_known_presets)
            } else {
                stringResource(R.string.obscura_matching_apps, totalMatchedCount)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = SettingsSpace.extraSmall4),
        )
        if (totalMatchedCount == 0) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.obscura_no_root_tools),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            AppPickerSearchField(query = searchQuery, onQueryChange = { searchQuery = it })
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = SettingsSpace.extraSmall4),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.obscura_selected_count, currentSelectedCount),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(SettingsSpace.extraSmall3)) {
                    TextButton(onClick = {
                        filteredApps.map { it.first.packageName }.forEach { pkg ->
                            if (!selectedPackages.contains(pkg)) selectedPackages.add(pkg)
                        }
                    }) { Text(stringResource(R.string.obscura_select_all)) }
                    TextButton(onClick = {
                        selectedPackages.removeAll(filteredApps.map { it.first.packageName }.toSet())
                    }) { Text(stringResource(R.string.obscura_deselect_all)) }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(SettingsSpace.extraSmall3),
            ) {
                for (cat in PresetCategory.values()) {
                    val catCount = if (cat == PresetCategory.ALL) totalMatchedCount
                    else presetMatchedApps.count { it.second == cat }
                    if (cat == PresetCategory.ALL || catCount > 0) {
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = {
                                Text(stringResource(R.string.obscura_preset_count, stringResource(cat.labelRes), catCount))
                            },
                        )
                    }
                }
            }
            if (filteredApps.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(R.string.obscura_no_filter_match),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(top = SettingsSpace.extraSmall4),
                    verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                ) {
                    itemsIndexed(filteredApps, key = { _, item -> item.first.packageName }) { index, (targetApp, category) ->
                        val isChecked = selectedPackages.contains(targetApp.packageName)
                        SegmentedListItem(
                            onClick = {
                                if (isChecked) selectedPackages.remove(targetApp.packageName)
                                else selectedPackages.add(targetApp.packageName)
                            },
                            shapes = ListItemDefaults.segmentedShapes(index, filteredApps.size),
                            leadingContent = { SettingsAppIcon(targetApp.icon) },
                            overlineContent = { Text(stringResource(category.shortLabelRes)) },
                            supportingContent = {
                                Text(
                                    targetApp.packageName,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            trailingContent = {
                                Checkbox(checked = isChecked, onCheckedChange = null)
                            },
                        ) {
                            Text(targetApp.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        Button(
            onClick = {
                onApply(selectedPackages.toSet())
                onDismiss()
            },
            enabled = totalMatchedCount > 0 && selectedPackages.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = SettingsSpace.extraSmall4),
        ) {
            Text(stringResource(R.string.obscura_add_selected, selectedPackages.size))
        }
    }
}
