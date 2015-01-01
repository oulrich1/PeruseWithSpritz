package com.oriahulrich.perusalwithspritz.Settings;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.oriahulrich.perusalwithspritz.R;

public class PreferencesFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }
}