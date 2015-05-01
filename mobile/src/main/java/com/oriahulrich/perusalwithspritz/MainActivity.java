package com.oriahulrich.perusalwithspritz;

// Standard Includes

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.oriahulrich.perusalwithspritz.database.SQLiteDAO;
import com.oriahulrich.perusalwithspritz.lib.Helpers;
import com.spritzinc.android.sdk.SpritzSDK;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.epub.EpubReader;

// library includes
//import com.github.amlcurran.showcaseview.ApiUtils;
//import com.github.amlcurran.showcaseview.OnShowcaseEventListener;
//import com.github.amlcurran.showcaseview.ShowcaseView;
//import com.github.amlcurran.showcaseview.targets.ActionViewTarget;
//import com.github.amlcurran.showcaseview.targets.ViewTarget;
// local project includes
//import org.opencv.android.BaseLoaderCallback;
//import org.opencv.android.LoaderCallbackInterface;
//import org.opencv.android.OpenCVLoader;

// siegmann epublib: http://www.siegmann.nl/epublib/android
//import nl.siegmann.epublib.domain.Book;
//import nl.siegmann.epublib.domain.TOCReference;
//import nl.siegmann.epublib.epub.EpubReader;
//import org.slf4j.LoggerFactory;



// inspiration : http://www.pageturner-reader.org/

public class MainActivity extends FragmentActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    // Not important but for leaning purposes
    static {
        System.loadLibrary("TessWrapperNDKModule");
    }

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

//    private ShowcaseView sv;

    // used to identify this class
    static private String TAG = "Main Activity";

    public enum InputMethodState {
        URL_WEBVIEW,
        TEXT_EDIT,
        URL_SPRITZ,
        IMAGE_SHARE,
        READ_EPUB
    }

    // argument to the web view fragment
    private String mURL;

    // argument to the edit text fragment
    private String mText;

    // argument to the epub fragment
    private Book mBook;

    // arguments to the ocr functionality //
    private boolean mCameraDetected;
    private Uri mImageUri;
    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
    private Uri fileUri;

    // TODO: Seriously -> defer these things AFTER spritz has finished loading
    // but no need to check that logic twice.. just check it on resume probably
    private boolean m_doOcrAndSpritzAfterViewLoads;
    private boolean m_doDeferedUrlShareSpritz;

    // TODO: rename this to PerusaDataInputState or just InputState
    private InputMethodState mInputMethodState;

    public SQLiteDAO getSqLiteDAO() {
        return sqLiteDAO;
    }

    public void setActionBarTitle(String title) {
        getActionBar().setTitle(title);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "ACTIVITY onCreate");

        mCameraDetected = Helpers.checkCameraHardware(this);

        // init spritz first
        try {
            SpritzSDK.init(this,
                    "a54c147382cb5ce21",
                    "fa4157e0-b591-4183-a6fc-78f21a692dbf",
                    "https://sdk.spritzinc.com/android/examples/login_success.html"
            );
            Log.d(TAG, "Spritz successfully initialized..");
        } catch ( Exception e ) {
            Log.d(TAG, "Spritz failed to init..");
        }

        // initialize one and only DAO
        sqLiteDAO = new SQLiteDAO(this);
        sqLiteDAO.open();

//        Log.d(TAG, " Native string: " + getStringFromNative());

        // Get intent with the text: (1) raw text, (2) url, (3) image
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        m_doOcrAndSpritzAfterViewLoads = false;

        // TODO: what if the user wants to spritz a PDF or EPUB..
        // pdf might not be as easy as epub.. checkout epublib

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                handleSendText(intent); // Handle text being sent
            } else if (type.startsWith("image/")) {
                handleSendImage(intent); // Handle single image being sent
            } else if ( "*/*".equals(type)
                        || type.startsWith("application/epub+zip")) {
                Log.d(TAG, "We have recieved an epub.");
                handleSendEpub(intent);
            } else {
                handleDefaultIntent(intent);
            }
        } else {
            // Handle other intents, such as being started from the home screen
            handleDefaultIntent(intent);
        }

        // importance:
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d( TAG, "Finished determining state: "
                    + mInputMethodState + " "
                    + mText + " "
                    + mURL );

        // set up the navbar
        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        // https://github.com/amlcurran/ShowcaseView
        if ( Helpers.checkAppStart(this) == Helpers.AppStart.FIRST_TIME) {
//            ActionItemTarget target = new ActionItemTarget(this, R.id.action_perform_spritz_on_text);
//            ShowcaseView sv = new ShowcaseView.Builder(this)
//                    .setTarget(target)
//                    .setContentTitle("Press to 'Spritz'")
//                    .setContentText("")
//                    .hideOnTouchOutside()
//                    .build();
//            sv.show();
        }

        int currentOrientation = getResources().getConfiguration().orientation;
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private Book parseEPUB(String path) {
        Book book = null;
        try {
            InputStream epubInputStream = new URL(path).openStream();
            book = (new EpubReader()).readEpub(epubInputStream);
            if ( book != null ) {
                Log.i("epublib", "title: " + book.getTitle());
            }
        } catch (IOException e) {
            Log.e("epublib", e.getMessage());
        }

        return book;
    }

//    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
//        @Override
//        public void onManagerConnected(int status) {
//            switch (status) {
//                case LoaderCallbackInterface.SUCCESS:
//                {
//                    Log.d(TAG, "OpenCV loaded successfully");
//
//                    // TODO: ?
//                    /// not sure if this is the right way to do this
//                    /// but since we shouldnt ocr a shared image as soon
//                    /// as it is sent but wait for loading to finish.
//                    /// specifically, wait until the view is drawn AND
//                    /// when opencv is loaded.. (at LEAST the latter)
//
//                } break;
//                default:
//                {
//                    super.onManagerConnected(status);
//                } break;
//            }
//        }
//    };

    @Override
    protected void onResume() {
        super.onResume();
//        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
        Log.d(TAG, "onResume");
//
//        ShowcaseView showcaseView = (new ShowcaseView.Builder(this))
//                .setTarget(new ActionViewTarget(this, ActionViewTarget.Type.HOME))
//                .setContentTitle("ShowcaseView")
//                .setContentText("This is highlighting the Home button")
//                .hideOnTouchOutside()
//                .build();


        /// TODO: the following assumes API level 20 (min was 15, but cur set to 20)
        // to do : wearables


//        /** Sample Notification - creating an intent for wearable  */
//        Intent notificationIntent = new Intent(this, MainActivity.class);
//        PendingIntent displayPendingIntent
//                = PendingIntent.getActivity(this, 0, notificationIntent,
//                    PendingIntent.FLAG_UPDATE_CURRENT);
//
//        // expect to extend the default builder with wearable specific intent stuff
//        Notification.WearableExtender wearableExtender;
//        wearableExtender = new Notification.WearableExtender()
//                .setDisplayIntent(displayPendingIntent)
//                .setCustomSizePreset(Notification.WearableExtender.SIZE_MEDIUM);
//
//        // the notification that is actually a wearable notification specifically
//        // to set the wearable to display the activity upon completion of notification
//        Notification notif
//                = new Notification.Builder( this )
//                        .extend(wearableExtender)
//                        .setContentTitle("Spritz This!") /// this might conflict with default extender behavior.
//                        .build();
//
//        notif.notify();

        /** Sample notification - using notification compat */
//        int notificationId = 001;
//// Build intent for notification content
//        Intent viewIntent = new Intent(this, MainActivity.class);
//        String EXTRA_INTENT_ID = "extra_intent_id";
//        int eventId = 123;
//        viewIntent.putExtra(EXTRA_INTENT_ID, eventId);
//        PendingIntent viewPendingIntent =
//                PendingIntent.getActivity(this, 0, viewIntent, 0);
//
//        NotificationCompat.Builder notificationBuilder =
//                new NotificationCompat.Builder(this)
//                        .setSmallIcon(R.drawable.ic_perusal_dark)
//                        .setContentTitle("Event Title")
//                        .setContentText("Event Location")
//                        .setContentIntent(viewPendingIntent);
//
//// Get an instance of the NotificationManager service
//        NotificationManagerCompat notificationManager =
//                NotificationManagerCompat.from(this);

// Build the notification and issues it with notification manager.
//        notificationManager.notify(notificationId, notificationBuilder.build());
    }

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
                mInputMethodState = InputMethodState.URL_SPRITZ;
            }
            else
            {
                mText = sharedText;
                mInputMethodState = InputMethodState.TEXT_EDIT;
            }
//            doSelectDrawerItem( NavigationDrawerFragment.Position.PERUSAL.ordinal() );
        }
    }

    /* Expect the image to contain text to be OCR'd */
    void handleSendImage(Intent intent) {
        Log.d(TAG, "ACTIVITY handleSendImage");
        Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageUri != null) {
            Log.d(TAG, "handleSendImage.. got the image");

            // Update UI to reflect image being shared
            mText = "";
            mImageUri = imageUri;
            mInputMethodState = InputMethodState.IMAGE_SHARE;
        }
    }

    /* Expect the image to contain text to be OCR'd */
    void handleSendMultipleImages(Intent intent) {
        Log.d(TAG, "ACTIVITY handleSendMultipleImages");
        ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        if (imageUris != null) {
            Log.d(TAG, "handleSendMultipleImages Intent Receive Not Implemented");
            Toast.makeText( this,
                    "Spritzing multiple images is not supported, sorry..",
                    Toast.LENGTH_LONG).show();
            // Update UI to reflect multiple images being shared
        }
    }

    void handleSendEpub( Intent intent ) {
        Log.d(TAG, "ACTIVITY handleSendEpub");
        Uri epubUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (epubUri != null) {
            Log.d(TAG, " .. epub uri is valid!");

            // get ready to go into "read text" mode. The text source
            // is not from a url, or raw text really, its kind of in between.
            // in the sense we still need to parse the text from the epub
            // but we will use spritz as if we have raw text..
            // todo: handle just in time book parsing to read text and
            // populate the spritz reader with the text
            mText = "";
            mInputMethodState = InputMethodState.TEXT_EDIT;

            // parse the epub
            String urlString = epubUri.toString();
            Book book = parseEPUB( urlString );

            if ( book != null ) {
                // then o
                // pen up the epub viewing fragment from which
                // selections of the epub could be spritzed
                mBook = book;
                mInputMethodState = InputMethodState.READ_EPUB;
                Toast.makeText(this,
                               "'" + book.getTitle().toString() +  "'",
                               Toast.LENGTH_LONG).show();
            } else {
                // when spritz fragment is decided upon, just open up the raw text edit fragment
                Toast.makeText(this, "e-pub could not be opened, sorry.", Toast.LENGTH_LONG).show();
            }
        }
    }

    void handleDefaultIntent(Intent intent)
    {
        if( Intent.ACTION_MAIN == intent.getAction() )
        {
            Log.d(TAG,  "handleDefaultIntent: Action Main action is invoked.. " +
                    "will open up text edit fragment");
        }

        mText = "";
        mInputMethodState = InputMethodState.TEXT_EDIT;
//        doSelectDrawerItem( NavigationDrawerFragment.Position.PERUSAL.ordinal() );
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
                // handles spritzing raw text or URL, if special case:
                // then handle that within spritzing fragment
                doSelectPerusalDetermineSpritzingFragment(position);
                break;
            case 1:
                doSelectRecentListFragment(position);
                break;
            case 2:
                // then open camera intent, get image..
                // on return from camera, ask OCR to recognize
                // the text and then spritz
                doSelectOcrFragment(position);
                break;
        }
    }


    // perform transition to section number 1 ( the 'peruse' item )
    // which by default goes to the edit text fragment. the edit text
    // fragment figures out what the text represents, in its onResume:
    //      (1) text -> just stay in the fragment
    //      (2) url -> open the spritz fragment (replace edit text fragment)
    //      (3) image -> perform ocr and load the spritz fragment
    private void doSelectPerusalDetermineSpritzingFragment(int position)
    {
        Fragment fragment;

        /* Always load the 'peruse' fragment (aka editText Fragment)
         * but create an instance specific to each type */
        if (mInputMethodState == InputMethodState.URL_SPRITZ) {
            // the user shared a url
            fragment = PerusalEditTextFragment
                    .newInstance(position + 1, mURL, mInputMethodState );
        } else if ( mInputMethodState == InputMethodState.IMAGE_SHARE ) {
            // the user shared a raw image
            fragment = PerusalEditTextFragment
                    .newInstance(position + 1, mImageUri, mInputMethodState );
        } else if ( mInputMethodState == InputMethodState.TEXT_EDIT ) {
            // the user shared raw text..
            fragment = PerusalEditTextFragment
                    .newInstance(position + 1, mText, mInputMethodState);
        } else if ( mInputMethodState == InputMethodState.READ_EPUB ) {
            fragment = PerusalEpubFragment
                    .newInstance(position + 1, mBook, mInputMethodState);
        } else {
            // error
            fragment = new Fragment();
            Log.d(TAG, "ERROR: the input method state was not "
                        + "valid. The fragment in drawer is set to empty.." );
        }

        // update the main content by replacing fragments
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, fragment)
                .commitAllowingStateLoss();
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
        if (!mCameraDetected) {
            Toast.makeText(this, "Camera not detected..", Toast.LENGTH_SHORT).show();
            return;
        }

//        Toast.makeText(this, "OCR Disabled", Toast.LENGTH_SHORT).show();
//        return;

//        Log.d(TAG, " About to start a camera intent and save the picture and OCR it.. ");

        // create picture file and take picture
        // when the picture is finished the rest of
        // the ocr/spritzing logic is handled..
        dispatchTakePictureIntent();
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, " Received activity result ");

        if ( mImageUri == null ) {
            Log.d(TAG, "on activity result image uri is null."
                        + "this is not possible in this method..");
            return;
        }

        if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
//                Toast.makeText(this, "Please wait a moment!", Toast.LENGTH_LONG).show();
                Helpers.globalAppsMediaScanIntent( this, mImageUri.getPath() );
//                doOcrAndSpritz( mImageUri );

                Fragment fragment = PerusalEditTextFragment
                        .newInstance(1, mImageUri, InputMethodState.IMAGE_SHARE );

                // update the main content by replacing fragments
                FragmentManager fragmentManager = getFragmentManager();
                fragmentManager.beginTransaction()
                        .replace(R.id.container, fragment)
                        .commit();

            } else if (resultCode == RESULT_CANCELED) {
            } else {
            }
        }
    }



    private boolean dispatchTakePictureIntent() {
        ///  - - - - - - - - - - - - - - - - - - - - - - - - - - -
        // create Intent to take a picture and return control to the calling application
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = Helpers.createUniqueImageFile();
            } catch (IOException e) {
                Log.d(TAG, "image file exception.. " + e.getMessage());
                photoFile = null;
            }

            // Continue only if the File was successfully created
            if (photoFile != null) {
                mImageUri = Uri.fromFile(photoFile);
                takePictureIntent.putExtra( MediaStore.EXTRA_OUTPUT, mImageUri );
                startActivityForResult(takePictureIntent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
            } else {
                mImageUri = null;
                return false;
            }

        } else {
            Toast.makeText(this, "Camera not detected..", Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }




    public void onSectionAttached(int number) {
        Log.d(TAG, "ACTIVITY onSectionAttached");
        switch (number) {
            case 1: // web view - to select text
                mTitle  = getString(R.string.title_section1);
                break;
            case 2:
                // mTitle = getString(R.string.title_section2);
                mTitle = "Peruse";
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


    /// AKA the action bar buttons and actions
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
//        if (id == R.id.action_settings) {
//            return true;
//        }
//        else
        if (id == R.id.action_about) {
            FragmentManager fragmentManager = this.getFragmentManager();
            DialogAboutFragment dialogAboutFragment
                    = new DialogAboutFragment();
            dialogAboutFragment.show(fragmentManager, "dialog about fragment");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // given the text state -> msg can be either text or a url..
    // NOTE: this is the only entry point to spritzing.. just FYI
    // BUT: epub fragment uses a custom navigation transaction to spritzing fragment
    public void navigateToSpritzFragment(int textState, String msg) {
        int peruseSectionNumber = 1; // force section nav number
        boolean shouldAttemptSavePerusal = true;
        Fragment fragment = PerusalSpritzFragment
                .newInstance( peruseSectionNumber, textState,
                        msg, shouldAttemptSavePerusal );

        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, fragment)
                .commitAllowingStateLoss();
    }



    public native String getStringFromNative();

}
