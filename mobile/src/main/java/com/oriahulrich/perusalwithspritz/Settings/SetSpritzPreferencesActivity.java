package com.oriahulrich.perusalwithspritz.Settings;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;

public class SetSpritzPreferencesActivity extends FragmentActivity {
    final private static String TAG = "***SET SPRITZ PREF ACTIVITY***: ";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new SpritzPreferencesFragment()).commitAllowingStateLoss();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
}
