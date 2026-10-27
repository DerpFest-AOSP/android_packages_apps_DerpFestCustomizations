/*
 * Copyright (C) 2017-2018 The OmniROM Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.derpfest.customizations.fragment;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.MediaStore;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.search.SearchIndexable;

import org.derpfest.customizations.utils.ImageUtils;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@SearchIndexable
public class QsHeaderImageSettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String CUSTOM_HEADER_BROWSE = "custom_header_browse";
    private static final String DAYLIGHT_HEADER_PACK = "daylight_header_pack";
    private static final String CUSTOM_HEADER_PROVIDER = "qs_header_provider";
    private static final String STATUS_BAR_CUSTOM_HEADER = "status_bar_custom_header";
    private static final String FILE_HEADER_SELECT = "file_header_select";
    private static final int REQUEST_PICK_IMAGE = 10001;

    private static final String PROVIDER_DAYLIGHT = "daylight";
    private static final String PROVIDER_FILE = "file";

    private static final String OMNISTYLE_PACKAGE = "org.omnirom.omnistyle";
    private static final String OMNISTYLE_BROWSE = OMNISTYLE_PACKAGE + ".BrowseHeaderActivity";
    private static final String OMNISTYLE_PICK = OMNISTYLE_PACKAGE + ".PickHeaderActivity";
    /** Same key OmniStyle reads: "package" or "package/activity". */
    private static final String EXTRA_HEADER_PACK = "header_pack";

    private Preference mHeaderBrowse;
    private ListPreference mDaylightHeaderPack;
    private ListPreference mHeaderProvider;
    private Preference mFileHeader;
    private boolean mOmniStyleAvailable;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.qs_header_image_settings);

        mHeaderBrowse = findPreference(CUSTOM_HEADER_BROWSE);
        mHeaderProvider = (ListPreference) findPreference(CUSTOM_HEADER_PROVIDER);
        mDaylightHeaderPack = (ListPreference) findPreference(DAYLIGHT_HEADER_PACK);
        mFileHeader = findPreference(FILE_HEADER_SELECT);

        mHeaderProvider.setOnPreferenceChangeListener(this);
        mDaylightHeaderPack.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mOmniStyleAvailable = isOmniStyleAvailable();
        refreshHeaderPacks();

        String provider = Settings.System.getString(getContext().getContentResolver(),
                Settings.System.STATUS_BAR_CUSTOM_HEADER_PROVIDER);
        if (provider == null || mHeaderProvider.findIndexOfValue(provider) < 0) {
            provider = PROVIDER_DAYLIGHT;
        }
        mHeaderProvider.setValue(provider);
        mHeaderProvider.setSummary(mHeaderProvider.getEntry());
        updateProviderUi(provider);
    }

    private void refreshHeaderPacks() {
        List<String> entries = new ArrayList<>();
        List<String> values = new ArrayList<>();
        getAvailableHeaderPacks(entries, values);
        mDaylightHeaderPack.setEntries(entries.toArray(new String[0]));
        mDaylightHeaderPack.setEntryValues(values.toArray(new String[0]));

        String selected = Settings.System.getString(getContext().getContentResolver(),
                Settings.System.STATUS_BAR_DAYLIGHT_HEADER_PACK);
        int index = mDaylightHeaderPack.findIndexOfValue(selected);
        if (index >= 0) {
            mDaylightHeaderPack.setValueIndex(index);
            mDaylightHeaderPack.setSummary(mDaylightHeaderPack.getEntry());
        }
    }

    /**
     * Daylight opens a read-only browser. Tapping a header in the picker writes the static
     * provider, so that activity is only used when static mode is already selected.
     */
    private void updateProviderUi(String provider) {
        boolean daylight = PROVIDER_DAYLIGHT.equals(provider);
        boolean file = PROVIDER_FILE.equals(provider);

        mDaylightHeaderPack.setEnabled(daylight);
        mFileHeader.setEnabled(file);
        mHeaderBrowse.setEnabled(mOmniStyleAvailable && !file);
        mHeaderBrowse.setTitle(daylight
                ? R.string.qs_header_browse_title : R.string.qs_header_pick_title);
        mHeaderBrowse.setSummary(daylight
                ? R.string.qs_header_browse_summary : R.string.qs_header_pick_summary);

        if (file || !mOmniStyleAvailable) {
            mHeaderBrowse.setIntent(null);
            return;
        }
        Intent intent = new Intent(Intent.ACTION_MAIN)
                .setClassName(OMNISTYLE_PACKAGE, daylight ? OMNISTYLE_BROWSE : OMNISTYLE_PICK);
        String pack = mDaylightHeaderPack.getValue();
        if (daylight && !TextUtils.isEmpty(pack)) {
            intent.putExtra(EXTRA_HEADER_PACK, pack);
        }
        mHeaderBrowse.setIntent(intent);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getContext().getContentResolver();
        switch (preference.getKey()) {
            case DAYLIGHT_HEADER_PACK:
                String pack = (String) newValue;
                Settings.System.putString(resolver,
                        Settings.System.STATUS_BAR_DAYLIGHT_HEADER_PACK, pack);
                int packIndex = mDaylightHeaderPack.findIndexOfValue(pack);
                if (packIndex >= 0) {
                    mDaylightHeaderPack.setValueIndex(packIndex);
                    mDaylightHeaderPack.setSummary(mDaylightHeaderPack.getEntry());
                }
                updateProviderUi(mHeaderProvider.getValue());
                return true;

            case CUSTOM_HEADER_PROVIDER:
                String provider = (String) newValue;
                Settings.System.putString(resolver,
                        Settings.System.STATUS_BAR_CUSTOM_HEADER_PROVIDER, provider);
                mHeaderProvider.setValue(provider);
                mHeaderProvider.setSummary(mHeaderProvider.getEntry());
                updateProviderUi(provider);
                return true;

            default:
                return false;
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == mFileHeader) {
            try {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.setType("image/*");
                startActivityForResult(intent, REQUEST_PICK_IMAGE);
            } catch (Exception e) {
                Toast.makeText(getContext(), R.string.qs_header_needs_gallery, Toast.LENGTH_LONG).show();
            }
            return true;
        }
        if (preference == mHeaderBrowse && !mOmniStyleAvailable) {
            Toast.makeText(getContext(), R.string.qs_header_needs_omnistyle, Toast.LENGTH_LONG).show();
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    private boolean isOmniStyleAvailable() {
        Intent pick = new Intent().setClassName(OMNISTYLE_PACKAGE, OMNISTYLE_PICK);
        return getContext().getPackageManager().resolveActivity(pick, 0) != null;
    }

    private void getAvailableHeaderPacks(List<String> entries, List<String> values) {
        Map<String, String> headerMap = new HashMap<>();
        PackageManager packageManager = getContext().getPackageManager();
        Intent intent = new Intent("org.omnirom.DaylightHeaderPack");
        for (ResolveInfo info : packageManager.queryIntentActivities(intent, 0)) {
            String packageName = info.activityInfo.packageName;
            CharSequence loaded = info.activityInfo.loadLabel(packageManager);
            String label = loaded != null ? loaded.toString() : packageName;
            headerMap.put(label, packageName);
        }
        intent.setAction("org.omnirom.DaylightHeaderPack1");
        for (ResolveInfo info : packageManager.queryIntentActivities(intent, 0)) {
            if (info.activityInfo.name.endsWith(".theme")) {
                continue;
            }
            String packageName = info.activityInfo.packageName;
            CharSequence loaded = info.activityInfo.loadLabel(packageManager);
            String label = loaded != null ? loaded.toString() : packageName;
            headerMap.put(label, packageName + "/" + info.activityInfo.name);
        }
        List<String> labels = new ArrayList<>(headerMap.keySet());
        Collections.sort(labels, String.CASE_INSENSITIVE_ORDER);
        for (String label : labels) {
            entries.add(label);
            values.add(headerMap.get(label));
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.DERPFEST;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            final Uri imageUri = result.getData();
            if (imageUri != null) {
                String savedImagePath = ImageUtils.saveImageToInternalStorage(getContext(), imageUri, "qs_header_image", "QS_HEADER_IMAGE");
                if (savedImagePath != null) {
                    ContentResolver resolver = getContext().getContentResolver();
                    Settings.System.putStringForUser(resolver, Settings.System.STATUS_BAR_FILE_HEADER_IMAGE, savedImagePath, UserHandle.USER_CURRENT);
                }
            }
        }
    }

    /**
     * For search
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.qs_header_image_settings;
                    result.add(sir);
                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    ArrayList<String> result = new ArrayList<String>();
                    return result;
                }
            };
}
