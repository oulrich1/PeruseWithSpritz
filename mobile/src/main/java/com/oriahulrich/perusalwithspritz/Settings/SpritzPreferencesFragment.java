package com.oriahulrich.perusalwithspritz.Settings;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.oriahulrich.perusalwithspritz.R;

public class SpritzPreferencesFragment extends PreferenceFragment {

    static String TAG = "SpritzPreferencesFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.spritz_preferences);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
//        Preference preference = findPreference("pref_color");
//        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
//            @Override
//            public boolean onPreferenceClick(Preference preference) {
//                String key = preference.getKey();
//                if(key.equalsIgnoreCase("pref_color")){
//                    showColorPicker();
//                }
//                return false;
//            }
//        });
        return v;
    }

//    private void showColorPicker() {
//        StringPickerDialog dialog = new StringPickerDialog();
//        Bundle bundle = new Bundle();
//        String[] values = new String[] {"a","b", "c", "d", "e", "f"};
//        bundle.putStringArray(getString(R.string.pref_select_spritz_color_reticle), values);
//        dialog.setArguments(bundle);
//        dialog.show(getActivity().getSupportFragmentManager(), TAG);
//    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("WORKAROUND_FOR_BUG_19917_KEY", "WORKAROUND_FOR_BUG_19917_VALUE");
        super.onSaveInstanceState(outState);
    }
}