package com.oriahulrich.perusalwithspritz;

// Standard Includes
import android.app.Activity;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.DrawerLayout;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

// library includes
import com.oriahulrich.perusalwithspritz.database.SQLiteDAO;
import com.oriahulrich.perusalwithspritz.pojos.Perusal;
import com.spritzinc.android.sdk.SpritzSDK;

// local project includes
import com.oriahulrich.perusalwithspritz.Helpers;
import com.oriahulrich.perusalwithspritz.PerusalSelectionFragment;
import com.oriahulrich.perusalwithspritz.PerusalEditTextFragment;
import com.oriahulrich.perusalwithspritz.PerusalSpritzFragment;


public class MainActivity extends Activity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    // database instance shared with spritz and recentperusals fragments
    private SQLiteDAO    sqLiteDAO;

    // used to identify this class
    static private String TAG = "Main Activity";

    public enum TextInputState {
        URL_WEB_VIEW,
        TEXT_EDIT,
        URL_SPRITZ
    }

    // argument to the webview fragment
    private String mURL;

    // argument to the edit text fragment
    private String mText;

    private TextInputState mTextInputState;

    public SQLiteDAO getSqLiteDAO() {
        return sqLiteDAO;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "ACTIVITY onCreate");

        // initialize one and only DAO
        sqLiteDAO = new SQLiteDAO(this);
        sqLiteDAO.open();

        // init spritz first
        try {
            SpritzSDK.init(this,
                    "a54c147382cb5ce21",
                    "fa4157e0-b591-4183-a6fc-78f21a692dbf",
                    "https://sdk.spritzinc.com/android/examples/login_success.html"
            );
        } catch ( Exception e ) {
//            Toast.makeText(this, "Spritz failed to contact server, falling back 20 years..", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Spritz failed to init..");
        }

        // Get intent with the text: (1) raw text, (2) url, (3) image (TODO)
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                handleSendText(intent); // Handle text being sent
            } else if (type.startsWith("image/")) {
                handleSendImage(intent); // Handle single image being sent
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
            if (type.startsWith("image/")) {
                handleSendMultipleImages(intent); // Handle multiple images being sent
            }
        } else {
            // Handle other intents, such as being started from the home screen
            handleDefaultIntent(intent);
        }

        // importance:
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d( TAG, "Finished determining state: " + mTextInputState + " " + mText + " " + mURL );

        // set up the navbar
        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
    }

    void handleDefaultIntent(Intent intent)
    {
        if( Intent.ACTION_MAIN == intent.getAction() )
        {
            Log.d(TAG,  "handleDefaultIntent: Action Main action is invoked.. " +
                        "will open up text edit fragment");
        }

        mText = "";
        mTextInputState = TextInputState.TEXT_EDIT;
//        doSelectDrawerItem( NavigationDrawerFragment.Position.PERUSAL.ordinal() );
    }

    /** TODO */
    /* Expect the text to be a URL String */
    void handleSendText(Intent intent) {
        Log.d(TAG, "ACTIVITY handleSendText");
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            // Update UI to reflect text being shared
            Log.d(TAG, "Received Text: " + sharedText);
            if (sharedText.contains("http://")
                || sharedText.contains("https://")
                || sharedText.contains(".com/"))
            {

                mURL = sharedText;
                mTextInputState = TextInputState.URL_SPRITZ;
            }
            else
            {
                mText = sharedText;
                mTextInputState = TextInputState.TEXT_EDIT;
            }
//            doSelectDrawerItem( NavigationDrawerFragment.Position.PERUSAL.ordinal() );
        }
    }

    /** TODO */
    /* Expect the image to contain text to be OCR'd */
    void handleSendImage(Intent intent) {
        Log.d(TAG, "ACTIVITY handleSendImage");
        Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageUri != null) {
            Log.d(TAG, "handleSendImage Intent Receive Not Implemented");
            Toast.makeText( this,
                            "Spritzing an image is not supported, yet. Really.",
                            Toast.LENGTH_LONG).show();
            // Update UI to reflect image being shared
        }
    }

    /** TODO */
    /* Expect the image to contain text to be OCR'd */
    void handleSendMultipleImages(Intent intent) {
        Log.d(TAG, "ACTIVITY handleSendMultipleImages");
        ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        if (imageUris != null) {
            Log.d(TAG, "handleSendMultipleImages Intent Receive Not Implemented");
            // Update UI to reflect multiple images being shared
        }
    }

    @Override
    /* on the event we select a drawer item */
    public void onNavigationDrawerItemSelected(int position) {
        Log.d(TAG, "ACTIVITY onNavigationDrawerItemSelected");

        /* do prepratory things here */

        /* If the position is 0 then do the following */
        switch (position)
        {
            case 0:
                doSelectPerusalDetermineSpritzingFragment(position);
                break;
            case 1:
                doSelectRecentListFragment(position);
                break;
            case 2:
                doSelectOcrFragment(position);
                break;
        }
    }

    /* Perform the transaction to the fragment */
    private void doSelectPerusalDetermineSpritzingFragment(int position)
    {
        // now since we want to "peruse", we must determine
        // which fragment to use exactly..
        Log.d(TAG, "ACTIVITY doSelectDrawerItem");
        Fragment fragment;
        if (mTextInputState == TextInputState.URL_WEB_VIEW)
        {
            fragment = PerusalSelectionFragment.newInstance(position + 1, mURL);
            Log.d(TAG, "Selection Fragment");
        }
        else if (mTextInputState == TextInputState.URL_SPRITZ)
        {
            fragment = PerusalSpritzFragment
                        .newInstance( position + 1,
                                      Perusal.Mode.URL.ordinal(),
                                      mURL,
                                      true );
            Log.d(TAG, "Spritz Direct URL SELECTION");
        }
        else
        {
            Log.d(TAG, "EditText Fragment");
            fragment = PerusalEditTextFragment.newInstance(position + 1, mText);
        }
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, fragment)
                .commit();
    }

    public void doSelectRecentListFragment(int position)
    {
        Log.d(TAG, "Recent List Fragment about to be loaded");
//        Toast.makeText(this, "Not implemented ye", Toast.LENGTH_SHORT).show();

        Fragment fragment = RecentPerusalsFragment.newInstance(position + 1);

        // update the main content by replacing fragments
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, fragment)
                .commit();
    }

    public void doSelectOcrFragment(int position)
    {
        Log.d(TAG, " OCR Fragment navigation and frag itself NOT IMPLEMENTED YET");
        Toast.makeText(this, "Not implemented yet!", Toast.LENGTH_SHORT).show();

//        Fragment fragment = RecentPerusalsFragment.newInstance(position + 1);
//
//        // update the main content by replacing fragments
//        FragmentManager fragmentManager = getFragmentManager();
//        fragmentManager.beginTransaction()
//                .replace(R.id.container, fragment)
//                .commit();
    }

    public void onSectionAttached(int number) {
        Log.d(TAG, "ACTIVITY onSectionAttached");
        switch (number) {
            case 1: // web view - to select text
                mTitle  = getString(R.string.title_section1);
                break;
            case 2:
//                mTitle = getString(R.string.title_section2);
                mTitle = "Recent Perusals";
                break;
            case 3:
                mTitle = getString(R.string.title_section3);
                break;
        }
    }

    public void restoreActionBar() {
        Log.d(TAG, "ACTIVITY restoreActionBar");
        ActionBar actionBar = getActionBar();
        try {
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        } catch ( Exception e ) {
           Log.e(TAG, "restoreActionBar: Couldnt set Navigation Mode.. ");
        }
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "ACTIVITY onCreateOptionsMenu");
        if ( !mNavigationDrawerFragment.isDrawerOpen() ) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "ACTIVITY onOptionsItemSelected");
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }



}
