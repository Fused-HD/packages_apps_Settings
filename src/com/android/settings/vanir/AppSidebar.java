/*
 * Copyright (C) 2012 CyanogenMod
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.vanir;

import android.content.ContentResolver;
import android.content.ComponentName;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.widget.SeekBarPreference;

import com.vanir.util.Helpers;

public class AppSidebar extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener, Preference.OnPreferenceClickListener {
    private static final String TAG = "PowerMenu";

    private static final String KEY_ENABLED = "sidebar_enable";
    private static final String KEY_TRANSPARENCY = "sidebar_transparency";
    private static final String KEY_SETUP_ITEMS = "sidebar_setup_items";
    private static final String KEY_POSITION = "sidebar_position";
    private static final String KEY_HIDE_LABELS = "sidebar_hide_labels";
    private static final String KEY_TRIGGER_WIDTH = "trigger_width";
    private static final String KEY_TRIGGER_TOP = "trigger_top";
    private static final String KEY_TRIGGER_BOTTOM = "trigger_bottom";

    private SwitchPreference mEnabledPref;
    private SeekBarPreference mTransparencyPref;
    private ListPreference mPositionPref;
    private CheckBoxPreference mHideLabelsPref;
    private SeekBarPreference mTriggerWidthPref;
    private SeekBarPreference mTriggerTopPref;
    private SeekBarPreference mTriggerBottomPref;

    private SettingsObserver mObserver;

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        private void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.APP_SIDEBAR_ENABLED), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.APP_SIDEBAR_TRANSPARENCY), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.APP_SIDEBAR_POSITION), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.APP_SIDEBAR_DISABLE_LABELS), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.APP_SIDEBAR_TRIGGER_WIDTH), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.APP_SIDEBAR_TRIGGER_TOP), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.APP_SIDEBAR_TRIGGER_HEIGHT), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.APP_SIDEBAR_SHOW_TRIGGER), false, this);
        }

        private void unobserve() {
            mContext.getContentResolver().unregisterContentObserver(mObserver);
        }

        @Override
        public void onChange(boolean selfChange) {
            sendUpdateBroadcast();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.app_sidebar_settings);

        mEnabledPref = (SwitchPreference) findPreference(KEY_ENABLED);
        mEnabledPref.setChecked((Settings.System.getInt(getContentResolver(),
                Settings.System.APP_SIDEBAR_ENABLED, 0) == 1));
        mEnabledPref.setOnPreferenceChangeListener(this);

        mHideLabelsPref = (CheckBoxPreference) findPreference(KEY_HIDE_LABELS);
        mHideLabelsPref.setChecked((Settings.System.getInt(getContentResolver(),
                Settings.System.APP_SIDEBAR_DISABLE_LABELS, 0) == 1));

        PreferenceScreen prefSet = getPreferenceScreen();
        mPositionPref = (ListPreference) prefSet.findPreference(KEY_POSITION);
        mPositionPref.setOnPreferenceChangeListener(this);
        int position = Settings.System.getInt(getContentResolver(), Settings.System.APP_SIDEBAR_POSITION, 0);
        mPositionPref.setValue(String.valueOf(position));
        updatePositionSummary(position);

        int trans = Settings.System.getInt(getContentResolver(),
                Settings.System.APP_SIDEBAR_TRANSPARENCY, 0);
        mTransparencyPref = (SeekBarPreference) findPreference(KEY_TRANSPARENCY);
        mTransparencyPref.setInitValue((int) (trans));
        mTransparencyPref.setOnPreferenceChangeListener(this);

        int width = Settings.System.getInt(getContentResolver(),
                Settings.System.APP_SIDEBAR_TRIGGER_WIDTH, 10);
        mTriggerWidthPref = (SeekBarPreference) findPreference(KEY_TRIGGER_WIDTH);
        mTriggerWidthPref.setInitValue((int) (width));
        mTriggerWidthPref.setOnPreferenceChangeListener(this);

        int top = Settings.System.getInt(getContentResolver(),
                Settings.System.APP_SIDEBAR_TRIGGER_TOP, 0);
        mTriggerTopPref = (SeekBarPreference) findPreference(KEY_TRIGGER_TOP);
        mTriggerTopPref.setInitValue((int) (top));
        mTriggerTopPref.setOnPreferenceChangeListener(this);

        int bottom = Settings.System.getInt(getContentResolver(),
                Settings.System.APP_SIDEBAR_TRIGGER_HEIGHT, 100);
        mTriggerBottomPref = (SeekBarPreference) findPreference(KEY_TRIGGER_BOTTOM);
        mTriggerBottomPref.setInitValue((int) (bottom));
        mTriggerBottomPref.setOnPreferenceChangeListener(this);

        findPreference(KEY_SETUP_ITEMS).setOnPreferenceClickListener(this);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mTransparencyPref) {
            int transparency = Integer.parseInt((String) newValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.APP_SIDEBAR_TRANSPARENCY, transparency);
            return true;
        } else if (preference == mTriggerWidthPref) {
            int width = Integer.parseInt((String) newValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.APP_SIDEBAR_TRIGGER_WIDTH, width);
            return true;
        } else if (preference == mTriggerTopPref) {
            int top = Integer.parseInt((String) newValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.APP_SIDEBAR_TRIGGER_TOP, top);
            return true;
        } else if (preference == mTriggerBottomPref) {
            int bottom = Integer.parseInt((String) newValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.APP_SIDEBAR_TRIGGER_HEIGHT, bottom);
            return true;
        } else if (preference == mPositionPref) {
            int position = Integer.valueOf((String) newValue);
            updatePositionSummary(position);
            return true;
        } else if (preference == mEnabledPref) {
            boolean value = ((Boolean)newValue).booleanValue();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.APP_SIDEBAR_ENABLED,
                    value ? 1 : 0);
            Helpers.restartSystemUI();
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean value;

        if (preference == mHideLabelsPref) {
            value = mHideLabelsPref.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.APP_SIDEBAR_DISABLE_LABELS,
                    value ? 1 : 0);
        } else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if(preference.getKey().equals(KEY_SETUP_ITEMS)) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setComponent(new ComponentName("com.android.systemui",
                    "com.android.systemui.statusbar.sidebar.SidebarConfigurationActivity"));
            getActivity().startActivity(intent);
            return true;
        }
        return false;
    }

    private void updatePositionSummary(int value) {
        mPositionPref.setSummary(mPositionPref.getEntries()[mPositionPref.findIndexOfValue("" + value)]);
        Settings.System.putInt(getContentResolver(),
                Settings.System.APP_SIDEBAR_POSITION, value);
    }

    @Override
    public void onPause() {
        super.onPause();
        Settings.System.putInt(getContentResolver(),
                Settings.System.APP_SIDEBAR_SHOW_TRIGGER, 0);
        mObserver.unobserve();
    }

    @Override
    public void onDestroy() {
		super.onDestroy();
		Settings.System.putInt(getContentResolver(),
		        Settings.System.APP_SIDEBAR_SHOW_TRIGGER, 0);
        mObserver.unobserve();
    }

    @Override
    public void onResume() {
        super.onResume();    //To change body of overridden methods use File | Settings | File Templates.
        Settings.System.putInt(getContentResolver(),
                Settings.System.APP_SIDEBAR_SHOW_TRIGGER, 1);
        mObserver = new SettingsObserver(new Handler());
        mObserver.observe();
    }

    private void sendUpdateBroadcast() {
        Intent u = new Intent();
        u.setAction("com.android.appsidebar.ACTION_UPDATE");
        mContext.sendBroadcastAsUser(u, UserHandle.ALL);
    }
}
