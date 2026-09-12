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
import android.content.Intent
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.android.internal.logging.nano.MetricsProto
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import com.android.settingslib.spa.framework.theme.SettingsTheme
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

    Scaffold(containerColor = Color.Transparent) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Main Hero Dashboard Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceBright,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.obscura_header_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = if (activeCount == 0) stringResource(R.string.obscura_none_protected)
                            else stringResource(R.string.obscura_protected_summary, activeCount, scopedCount, hiddenCount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            AppPickerSearchField(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = showSystemApps,
                    onClick = onToggleShowSystemApps,
                    label = { Text(stringResource(R.string.obscura_system_apps), style = MaterialTheme.typography.labelMedium) },
                    leadingIcon = if (showSystemApps) {
                        { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                    } else null,
                )
                FilterChip(
                    selected = filterConfiguredOnly,
                    onClick = onToggleFilterConfiguredOnly,
                    label = { Text(stringResource(R.string.obscura_configured_only, activeCount), style = MaterialTheme.typography.labelMedium) },
                    leadingIcon = if (filterConfiguredOnly) {
                        { Icon(Icons.Default.FilterList, null, modifier = Modifier.size(16.dp)) }
                    } else null,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            } else if (filteredApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.obscura_no_matching_apps),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        ObscuraAppListItem(
                            entry = app,
                            onClick = { onSelectApp(app) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ObscuraAppListItem(
    entry: ObscuraAppEntry,
    onClick: () -> Unit,
) {
    val isScoped = entry.scopeMode != ObscuraManager.SCOPE_MODE_DISABLED
    val hasSpoof = entry.spoofAdb || entry.spoofDevOptions || entry.spoofWirelessDebug
            || entry.spoofPkgVerifier || entry.spoofUsbVerify || entry.spoofAccessibility
    val isProtected = isScoped || entry.isIsolated || entry.isHidden || entry.isLauncherHidden || entry.isPlayStoreDetached || hasSpoof

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isProtected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.30f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val iconBitmap = remember(entry.packageName) {
                runCatching { entry.icon?.toBitmap(96, 96)?.asImageBitmap() }.getOrNull()
            }
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = null,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = entry.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (isProtected) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isScoped) {
                            val modeLabel = if (entry.scopeMode == ObscuraManager.SCOPE_MODE_BLACKLIST)
                                stringResource(R.string.obscura_scope_blacklist_count, entry.scopeList.size)
                            else stringResource(R.string.obscura_scope_whitelist_count, entry.scopeList.size)
                            StatusBadge(text = modeLabel, color = MaterialTheme.colorScheme.primary)
                        }
                        if (entry.isIsolated) {
                            StatusBadge(text = stringResource(R.string.obscura_badge_isolated), color = MaterialTheme.colorScheme.secondary)
                        }
                        if (entry.isPlayStoreDetached) {
                            StatusBadge(text = stringResource(R.string.obscura_badge_detached), color = MaterialTheme.colorScheme.tertiary)
                        }
                        if (hasSpoof) {
                            StatusBadge(text = stringResource(R.string.obscura_badge_spoofed), color = MaterialTheme.colorScheme.tertiary)
                        }
                        if (entry.isHidden) {
                            StatusBadge(text = stringResource(R.string.obscura_badge_hidden), color = MaterialTheme.colorScheme.error)
                        }
                        if (entry.isLauncherHidden && !entry.isHidden) {
                            StatusBadge(text = stringResource(R.string.obscura_launcher_hidden), color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }

            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun StatusBadge(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.18f),
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
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

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.obscura_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Identity Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceBright)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val iconBitmap = remember(app.packageName) {
                            runCatching { app.icon?.toBitmap(128, 128)?.asImageBitmap() }.getOrNull()
                        }
                        if (iconBitmap != null) {
                            Image(
                                bitmap = iconBitmap,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = app.label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = app.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (app.isSystem) stringResource(R.string.obscura_system_application) else stringResource(R.string.obscura_user_installed),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (app.isSystem) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (app.isHidden || app.isLauncherHidden) {
                            Button(
                                onClick = { launchApp(context, app.packageName) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.obscura_launch_app))
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                forceStopPackage(context, app.packageName)
                                Toast.makeText(context, context.getString(R.string.obscura_force_stopped, app.label), Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.obscura_force_stop))
                        }
                    }
                }
            }

            // Section 1: Per-App Privacy Scope (Hide List / Whitelist)
            SectionCard(
                title = stringResource(R.string.obscura_scope_section_title),
                subtitle = stringResource(R.string.obscura_scope_subtitle)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Mode Selector Chips
                    Text(stringResource(R.string.obscura_scope_mode), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = app.scopeMode == ObscuraManager.SCOPE_MODE_DISABLED,
                            onClick = {
                                onUpdate(app.copy(scopeMode = ObscuraManager.SCOPE_MODE_DISABLED))
                            },
                            label = { Text(stringResource(R.string.obscura_disabled), style = MaterialTheme.typography.labelSmall) }
                        )
                        FilterChip(
                            selected = app.scopeMode == ObscuraManager.SCOPE_MODE_BLACKLIST,
                            onClick = {
                                onUpdate(app.copy(scopeMode = ObscuraManager.SCOPE_MODE_BLACKLIST))
                            },
                            label = { Text(stringResource(R.string.obscura_blacklist), style = MaterialTheme.typography.labelSmall) }
                        )
                        FilterChip(
                            selected = app.scopeMode == ObscuraManager.SCOPE_MODE_WHITELIST,
                            onClick = {
                                onUpdate(app.copy(scopeMode = ObscuraManager.SCOPE_MODE_WHITELIST))
                            },
                            label = { Text(stringResource(R.string.obscura_whitelist), style = MaterialTheme.typography.labelSmall) }
                        )
                    }

                    if (app.scopeMode != ObscuraManager.SCOPE_MODE_DISABLED) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        val isBlacklist = app.scopeMode == ObscuraManager.SCOPE_MODE_BLACKLIST
                        Text(
                            text = if (isBlacklist)
                                stringResource(R.string.obscura_blacklist_mode_summary, app.scopeList.size, app.label)
                            else
                                stringResource(R.string.obscura_whitelist_mode_summary, app.label, app.scopeList.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Scope Quick Presets
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showPresetDialog = true },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.obscura_add_presets), style = MaterialTheme.typography.labelSmall)
                            }

                            OutlinedButton(
                                onClick = {
                                    val userPkgs = otherApps.filter { !it.isSystem }.map { it.packageName }.toSet()
                                    onUpdate(app.copy(scopeList = userPkgs))
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(stringResource(R.string.obscura_add_all_user_apps), style = MaterialTheme.typography.labelSmall)
                            }

                            if (app.scopeList.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = { onUpdate(app.copy(scopeList = emptySet())) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.obscura_clear), modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        // Toggle Target Package List Picker
                        Button(
                            onClick = { showScopeTargetList = !showScopeTargetList },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(Icons.Default.Tune, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (showScopeTargetList) stringResource(R.string.obscura_hide_scope_picker) else stringResource(R.string.obscura_configure_scope, app.scopeList.size))
                        }

                        AnimatedVisibility(
                            visible = showScopeTargetList,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceContainerHigh,
                                        RoundedCornerShape(14.dp)
                                    )
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AppPickerSearchField(
                                    query = scopeSearchQuery,
                                    onQueryChange = { scopeSearchQuery = it }
                                )

                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(300.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(filteredOtherApps, key = { it.packageName }) { targetApp ->
                                        val isChecked = app.scopeList.contains(targetApp.packageName)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable {
                                                    val newSet = if (isChecked) {
                                                        app.scopeList - targetApp.packageName
                                                    } else {
                                                        app.scopeList + targetApp.packageName
                                                    }
                                                    onUpdate(app.copy(scopeList = newSet))
                                                }
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = { checked ->
                                                    val newSet = if (checked) {
                                                        app.scopeList + targetApp.packageName
                                                    } else {
                                                        app.scopeList - targetApp.packageName
                                                    }
                                                    onUpdate(app.copy(scopeList = newSet))
                                                }
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = targetApp.label,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = targetApp.packageName,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: Hardware & Network Isolation
            SectionCard(
                title = stringResource(R.string.obscura_hardware_section_title),
                subtitle = stringResource(R.string.obscura_hardware_subtitle)
            ) {
                Column {
                    DetailOptionSwitch(
                        icon = Icons.Default.Shield,
                        title = stringResource(R.string.obscura_master_isolation),
                        subtitle = stringResource(R.string.obscura_master_isolation_subtitle),
                        checked = app.isIsolated,
                        onCheckedChange = { onUpdate(app.copy(isIsolated = it)) }
                    )
                    AnimatedVisibility(visible = app.isIsolated) {
                        Column {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            DetailOptionSwitch(
                                icon = Icons.Default.Public,
                                title = stringResource(R.string.obscura_restrict_internet),
                                subtitle = stringResource(R.string.obscura_restrict_internet_subtitle),
                                checked = app.restrictInternet,
                                onCheckedChange = { onUpdate(app.copy(restrictInternet = it)) }
                            )
                            DetailOptionSwitch(
                                icon = Icons.Default.Storage,
                                title = stringResource(R.string.obscura_restrict_storage),
                                subtitle = stringResource(R.string.obscura_restrict_storage_subtitle),
                                checked = app.restrictStorage,
                                onCheckedChange = { onUpdate(app.copy(restrictStorage = it)) }
                            )
                            DetailOptionSwitch(
                                icon = Icons.Default.Security,
                                title = stringResource(R.string.obscura_data_isolation),
                                subtitle = stringResource(R.string.obscura_data_isolation_subtitle),
                                checked = app.forceDataIsolation,
                                onCheckedChange = { onUpdate(app.copy(forceDataIsolation = it)) }
                            )
                        }
                    }
                }
            }

            // Section 3: Anti-Detection & Settings Spoofing
            SectionCard(
                title = stringResource(R.string.obscura_spoof_section_title),
                subtitle = stringResource(R.string.obscura_spoof_section_subtitle)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = {
                                val allEnabled = app.spoofAdb && app.spoofDevOptions && app.spoofWirelessDebug
                                        && app.spoofPkgVerifier && app.spoofUsbVerify && app.spoofAccessibility
                                val toggleTo = !allEnabled
                                onUpdate(
                                    app.copy(
                                        spoofAdb = toggleTo,
                                        spoofDevOptions = toggleTo,
                                        spoofWirelessDebug = toggleTo,
                                        spoofPkgVerifier = toggleTo,
                                        spoofUsbVerify = toggleTo,
                                        spoofAccessibility = toggleTo,
                                    )
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(stringResource(R.string.obscura_toggle_all_spoofs), style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    DetailOptionSwitch(
                        icon = Icons.Default.Code,
                        title = stringResource(R.string.obscura_spoof_adb),
                        checked = app.spoofAdb,
                        onCheckedChange = { onUpdate(app.copy(spoofAdb = it)) }
                    )
                    DetailOptionSwitch(
                        icon = Icons.Default.Tune,
                        title = stringResource(R.string.obscura_spoof_dev_options),
                        checked = app.spoofDevOptions,
                        onCheckedChange = { onUpdate(app.copy(spoofDevOptions = it)) }
                    )
                    DetailOptionSwitch(
                        icon = Icons.Default.Code,
                        title = stringResource(R.string.obscura_spoof_wireless_debug),
                        checked = app.spoofWirelessDebug,
                        onCheckedChange = { onUpdate(app.copy(spoofWirelessDebug = it)) }
                    )
                    DetailOptionSwitch(
                        icon = Icons.Default.Security,
                        title = stringResource(R.string.obscura_spoof_pkg_verifier),
                        checked = app.spoofPkgVerifier,
                        onCheckedChange = { onUpdate(app.copy(spoofPkgVerifier = it)) }
                    )
                    DetailOptionSwitch(
                        icon = Icons.Default.Security,
                        title = stringResource(R.string.obscura_spoof_usb_verify),
                        checked = app.spoofUsbVerify,
                        onCheckedChange = { onUpdate(app.copy(spoofUsbVerify = it)) }
                    )
                    DetailOptionSwitch(
                        icon = Icons.Default.Security,
                        title = stringResource(R.string.obscura_spoof_accessibility),
                        checked = app.spoofAccessibility,
                        onCheckedChange = { onUpdate(app.copy(spoofAccessibility = it)) }
                    )
                }
            }

            // Section 4: System & Launcher Visibility
            SectionCard(
                title = stringResource(R.string.obscura_visibility_section_title),
                subtitle = stringResource(R.string.obscura_visibility_subtitle)
            ) {
                Column {
                    DetailOptionSwitch(
                        icon = Icons.Default.VisibilityOff,
                        title = stringResource(R.string.obscura_hide_launcher),
                        subtitle = if (app.isHidden) stringResource(R.string.obscura_included_in_global_hide) else stringResource(R.string.obscura_hide_launcher_subtitle),
                        checked = app.isHidden || app.isLauncherHidden,
                        enabled = !app.isHidden,
                        onCheckedChange = { onUpdate(app.copy(isLauncherHidden = it)) }
                    )
                    DetailOptionSwitch(
                        icon = Icons.Default.PlayArrow,
                        title = stringResource(R.string.obscura_detach_playstore_title),
                        subtitle = if (app.isHidden) stringResource(R.string.obscura_included_in_global_hide) else stringResource(R.string.obscura_detach_playstore_summary),
                        checked = app.isHidden || app.isPlayStoreDetached,
                        enabled = !app.isHidden,
                        onCheckedChange = { newState ->
                            if (!newState && app.isPlayStoreDetached) {
                                onUpdate(app.copy(isPlayStoreDetached = false))
                                showRestartDialog = true
                            } else {
                                onUpdate(app.copy(isPlayStoreDetached = newState))
                            }
                        }
                    )
                    DetailOptionSwitch(
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
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    if (showPresetDialog) {
        PresetPickerDialog(
            otherApps = otherApps,
            currentScope = app.scopeList,
            onDismiss = { showPresetDialog = false },
            onApply = { selectedFromPreset ->
                val newScope = app.scopeList + selectedFromPreset
                onUpdate(app.copy(scopeList = newScope))
            }
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
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.obscura_reboot_dialog_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(text = stringResource(R.string.obscura_reboot_dialog_message))
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestartDialog = false
                        rebootDevice(context)
                    }
                ) {
                    Text(stringResource(R.string.obscura_reboot_dialog_button_now))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showRestartDialog = false }
                ) {
                    Text(stringResource(R.string.obscura_reboot_dialog_button_later))
                }
            }
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceBright)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun DetailOptionSwitch(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    val alpha = if (enabled) 1f else 0.38f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = (if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = alpha),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
                )
            }
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
    }
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.obscura_presets_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (totalMatchedCount == 0)
                        stringResource(R.string.obscura_no_known_presets)
                    else
                        stringResource(R.string.obscura_matching_apps, totalMatchedCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (totalMatchedCount > 0) {
                    AppPickerSearchField(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.obscura_selected_count, currentSelectedCount),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val visiblePkgs = filteredApps.map { it.first.packageName }
                                    visiblePkgs.forEach { if (!selectedPackages.contains(it)) selectedPackages.add(it) }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(stringResource(R.string.obscura_select_all), style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = {
                                    val visiblePkgs = filteredApps.map { it.first.packageName }.toSet()
                                    selectedPackages.removeAll(visiblePkgs)
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(stringResource(R.string.obscura_deselect_all), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PresetCategory.values().forEach { cat ->
                            val catCount = if (cat == PresetCategory.ALL) totalMatchedCount
                            else presetMatchedApps.count { it.second == cat }

                            if (cat == PresetCategory.ALL || catCount > 0) {
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat },
                                    label = {
                                        Text(stringResource(R.string.obscura_preset_count, stringResource(cat.labelRes), catCount), style = MaterialTheme.typography.labelSmall)
                                    }
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    if (filteredApps.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                stringResource(R.string.obscura_no_filter_match),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(filteredApps, key = { it.first.packageName }) { (targetApp, category) ->
                                val isChecked = selectedPackages.contains(targetApp.packageName)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            if (isChecked) {
                                                selectedPackages.remove(targetApp.packageName)
                                            } else {
                                                selectedPackages.add(targetApp.packageName)
                                            }
                                        }
                                        .padding(horizontal = 6.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            if (checked) {
                                                if (!selectedPackages.contains(targetApp.packageName)) {
                                                    selectedPackages.add(targetApp.packageName)
                                                }
                                            } else {
                                                selectedPackages.remove(targetApp.packageName)
                                            }
                                        }
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    val iconBitmap = remember(targetApp.packageName) {
                                        runCatching { targetApp.icon?.toBitmap(64, 64)?.asImageBitmap() }.getOrNull()
                                    }
                                    if (iconBitmap != null) {
                                        Image(
                                            bitmap = iconBitmap,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = targetApp.label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = targetApp.packageName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(Modifier.width(6.dp))
                                    Surface(
                                        color = when (category) {
                                            PresetCategory.ROOT -> MaterialTheme.colorScheme.errorContainer
                                            PresetCategory.DETECTORS -> MaterialTheme.colorScheme.tertiaryContainer
                                            PresetCategory.SHIZUKU -> MaterialTheme.colorScheme.secondaryContainer
                                            PresetCategory.TOOLS -> MaterialTheme.colorScheme.surfaceVariant
                                            else -> MaterialTheme.colorScheme.primaryContainer
                                        },
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = stringResource(category.shortLabelRes),
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.obscura_no_root_tools),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApply(selectedPackages.toSet())
                    onDismiss()
                },
                enabled = totalMatchedCount > 0 && selectedPackages.isNotEmpty()
            ) {
                Text(stringResource(R.string.obscura_add_selected, selectedPackages.size))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.obscura_cancel))
            }
        }
    )
}
