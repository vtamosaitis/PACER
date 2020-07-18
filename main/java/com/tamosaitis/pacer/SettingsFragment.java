package com.tamosaitis.pacer;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.preference.PreferenceFragmentCompat;

/**
 * A simple {@link Fragment} subclass.
 */
public class SettingsFragment extends PreferenceFragmentCompat {


    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }
}
