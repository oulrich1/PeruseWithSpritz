package com.oriahulrich.perusalwithspritz;

// Standard Includes
import android.app.Activity;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v4.widget.DrawerLayout;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;

// library includes
import com.spritzinc.android.sdk.SpritzSDK;

// local project includes
import com.oriahulrich.perusalwithspritz.database.SQLiteDAO;
import com.oriahulrich.perusalwithspritz.lib.Ocr;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.engine.OpenCVEngineInterface;


public class MainActivity extends Activity
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

    // used to identify this class
    static private String TAG = "Main Activity";

    public enum TextInputState {
        URL_WEB_VIEW,
        TEXT_EDIT,
        URL_SPRITZ,
        IMAGE_OCR_SHARE,
    }

    // argument to the webview fragment
    private String mURL;

    // argument to the edit text fragment
    private String mText;

    // arguments to the ocr functionality //
    private boolean mOcrEnabled;
    private Uri mImageUri;
    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
    private Uri fileUri;

    // true when the user shared an image but shouldnt
    // process it until after the view finishes loading..
    private boolean m_doOcrAndSpritzAfterViewLoads;

    // TODO: rename this to PerusaDataInputState or just InputState
    private TextInputState mTextInputState;

    public SQLiteDAO getSqLiteDAO() {
        return sqLiteDAO;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "ACTIVITY onCreate");

        mOcrEnabled = Helpers.checkCameraHardware(this);

        // initialize one and only DAO
        sqLiteDAO = new SQLiteDAO(this);
        sqLiteDAO.open();

        Log.d(TAG, " Native string: " + getStringFromNative());

        // Get intent with the text: (1) raw text, (2) url, (3) image (TODO)
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        m_doOcrAndSpritzAfterViewLoads = false;

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

        Log.d( TAG, "Finished determining state: "
                    + mTextInputState + " "
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
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.d(TAG, "OpenCV loaded successfully");

                    /// not sure if this is the right way to do this
                    /// but since we shouldnt ocr a shared image as soon
                    /// as it is sent but wait for loading to finish.
                    /// specifically, wait until the view is drawn AND
                    /// when opencv is loaded.. (at LEAST the latter)
                    if ( m_doOcrAndSpritzAfterViewLoads ) {
                        doDeferedOcrAndSpritzOnSharedImage();
                        m_doOcrAndSpritzAfterViewLoads = false;
                        return;
                    }
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
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
            Log.d(TAG, "handleSendImage.. got the image");

            // Update UI to reflect image being shared
            mText = "";
            mImageUri = imageUri;
            mTextInputState = TextInputState.IMAGE_OCR_SHARE;
        }
    }

    /** TODO */
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


    /* Perform the transaction to the fragment */
    private void doSelectPerusalDetermineSpritzingFragment(int position)
    {
        // now since we want to "peruse", we must determine
        // which fragment to use exactly..
        Log.d(TAG, "ACTIVITY doSelectDrawerItem");
        Fragment fragment;
        if (mTextInputState == TextInputState.URL_WEB_VIEW)
        {
            /// THIS SHOULD NOT GET CALLED FOR NOW..
            fragment = PerusalSelectionFragment.newInstance(position + 1, mURL);
            Log.d(TAG, "Selection Fragment");
        }
        else if (mTextInputState == TextInputState.URL_SPRITZ)
        {
            // TODO: figure out why spritz does not load when is first fragment to load
//            fragment = PerusalSpritzFragment
//                        .newInstance( position + 1,
//                                      Perusal.Mode.URL.ordinal(),
//                                      mURL,
//                                      true );

            // falling back to going through the edit text fragment for no
            // reason other than to make spritz work on the case a url is shared
            boolean isForceLoadSpritz = true;
            fragment = PerusalEditTextFragment
                    .newInstance(position + 1, mText, isForceLoadSpritz);

            Log.d(TAG, "Spritz Direct URL SELECTION");
        }
        else if ( mTextInputState == TextInputState.IMAGE_OCR_SHARE )
        {
            m_doOcrAndSpritzAfterViewLoads = true;

            boolean isForceLoadSpritz = false;
            fragment = PerusalEditTextFragment
                    .newInstance(position + 1, mText, isForceLoadSpritz);
        }
        else // if ( mTextInputState == TextInputState.TEXT_EDIT )
        {   // should happen in any other case anyways..
            Log.d(TAG, "EditText Fragment");
            fragment = PerusalEditTextFragment
                    .newInstance(position + 1, mText, false);
        }

        // update the main content by replacing fragments
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, fragment)
                .commit();
    }


    /// when an image was shared but we should not ocr it until the app fully loads
    /// (at least when the view finishes loading i think)
    public void doDeferedOcrAndSpritzOnSharedImage() {

        if ( mOcrEnabled )
        {
            Log.d(TAG, "OCR and then run editText fragment on text");
            mText = doOcrGetText( mImageUri );
        }
        else {
            Toast.makeText( this,
                    "Ocr is disabled for now..",
                    Toast.LENGTH_SHORT).show();
            mText = "";
        }

        // true when we need to go straight to the spritzing screen
        // false when we want to show the user the text first before spritzing
        boolean isForceLoadSpritz = false;

        // then populate the edit text fragment with the text
        // and let the user press the "Spritz" action in the action bar
        Fragment fragment = PerusalEditTextFragment
                .newInstance(1, mText, isForceLoadSpritz);

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
        if (!mOcrEnabled) {
            Toast.makeText(this, "Camera not detected..", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, " About to start a camera intent and save the picture and OCR it.. ");

        // create picture file and take picture
        // when the picture is finished the rest of
        // the ocr/spritzing logic is handled..
        dispatchTakePictureIntent();
    }


    /// --- HELPERS --- ///

    // http://wolfpaulus.com/jounal/android-journal/android-and-ocr/
    private Bitmap reorientImage( Bitmap bitmap, Uri uri ) {

        try {
            ExifInterface exif = new ExifInterface( uri.getPath() );
            int exifOrientation = exif.getAttributeInt( ExifInterface.TAG_ORIENTATION,
                                                        ExifInterface.ORIENTATION_NORMAL );

            Log.v(TAG, "Orient: " + exifOrientation);

            int rotate = 0;
            switch (exifOrientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
            }

            Log.v(TAG, "Rotation: " + rotate);

            if (rotate != 0) {

                // Getting width & height of the given image.
                int w = bitmap.getWidth();
                int h = bitmap.getHeight();

                // Setting pre rotate
                Matrix mtx = new Matrix();
                mtx.preRotate(rotate);

                // Rotating Bitmap
                // tesseract req. ARGB_8888
                return Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, false)
                             .copy(Bitmap.Config.ARGB_8888, true);
            }

        } catch (Exception e) {
            Log.e(TAG, "Rotate or coversion failed: " + e.toString());
        }

        return bitmap;
    }

    public Bitmap getResizedBitmap(Bitmap bm, int newHeight, int newWidth) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
        return resizedBitmap;
    }

    private Bitmap resizeIfTooLarge(Bitmap bitmap) {
        if ( bitmap.getHeight() > 1000
                || bitmap.getWidth() > 1000 )
        {
            double ratio;
            int newHeight;
            int newWidth;
            if ( bitmap.getHeight() > 1000 ) {
                ratio = 1000.0/bitmap.getHeight();
                newHeight = 1000;
                newWidth = (int) (ratio * bitmap.getWidth());
            } else {
                ratio = 1000.0/bitmap.getWidth();
                newWidth = 1000;
                newHeight = (int) (ratio * bitmap.getHeight());
            }
            bitmap = getResizedBitmap( bitmap, newHeight, newWidth );
        }
        return bitmap;
    }

    public String doOcrGetText( Uri imageUri ) {
        if ( imageUri == null ) {
            return "";
        }

//        Toast.makeText(this, "This will take a few seconds..",
//                       Toast.LENGTH_LONG).show();

        Ocr ocr = new Ocr( this );

        Bitmap bitmap;
        try {
            bitmap = MediaStore.Images.Media
                    .getBitmap(this.getContentResolver(), imageUri);
        } catch (Exception e){
            Log.d(TAG, "Could not create image from URI");
            return "";
        }

        bitmap = resizeIfTooLarge( bitmap );
        bitmap = reorientImage( bitmap, imageUri );


        // remove the set background on release. they are here for testing purposes
//        findViewById(R.id.container).setBackgroundDrawable( new BitmapDrawable(bitmap) );

        ocr.setImage(bitmap);
        Ocr.Result result = ocr.performOcr();

//        bitmap = ocr.getImage();
//        findViewById(R.id.container).setBackgroundDrawable( new BitmapDrawable(bitmap) );

        if ( !result.isValid ) {
            Toast.makeText(this, "Ocr Failed", Toast.LENGTH_LONG).show();
            return ""; // invalid text
        } else {

            // text cleaning specific to perusing
            String text = result.text;
            String[] words = text.split(" ");
            text = "";

            // remove bad characters per word
            for ( int i = 0; i < words.length; ++i ) {
                words[i] = words[i].replaceAll("[^a-zA-Z0-9]+", "");
                text += words[i] + " ";
            }
            result.text = text;

            Toast.makeText(this, "Done", Toast.LENGTH_SHORT).show();
        }

        return result.text;
    }

    public void doOcrAndSpritz( Uri imageUri ) {

        if ( imageUri == null )
            return;

        mText = doOcrGetText( imageUri );

        ///  - - - - - - - - - - - - - - - - - - - - - - - - - - -
        // then populate the edit text fragment with the text
        // and let the user press the "Spritz" action in the action bar
        int position = 3;
        boolean isForceLoadSpritz = false;
        Fragment fragment = PerusalEditTextFragment
                .newInstance(position, mText, isForceLoadSpritz);

        // update the main content by replacing fragments
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, fragment)
                .commit();
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
                Toast.makeText(this, "Please wait a moment!", Toast.LENGTH_LONG).show();
                Helpers.globalAppsMediaScanIntent( this, mImageUri.getPath() );
                doOcrAndSpritz( mImageUri );
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


    public native String getStringFromNative();

}
