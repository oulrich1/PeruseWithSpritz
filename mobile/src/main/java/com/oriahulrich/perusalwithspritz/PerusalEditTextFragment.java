package com.oriahulrich.perusalwithspritz;

/**
 * Created by oriahulrich on 12/14/14.
 */

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

//import com.github.amlcurran.showcaseview.ApiUtils;
//import com.github.amlcurran.showcaseview.OnShowcaseEventListener;
//import com.github.amlcurran.showcaseview.ShowcaseView;
//import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.github.amlcurran.showcaseview.ApiUtils;
import com.github.amlcurran.showcaseview.OnShowcaseEventListener;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.oriahulrich.perusalwithspritz.lib.Helpers;
import com.oriahulrich.perusalwithspritz.lib.Ocr;
import com.oriahulrich.perusalwithspritz.pojos.Perusal;

// TODO: call this the perusal Controller fragment or that nature..

/**
 * A Fragment to be used in the case when we want the user to paste text
 * from their clipboard into an edit text view to be spritzed.. hit play
 * to spritz the text
 */
public class PerusalEditTextFragment extends Fragment {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";

    private static final String ARG_INPUT_METHOD = "arg_input_method";
    private static final String ARG_TEXT = "arg_text";  // text to spritz
    private static final String ARG_URL = "arg_url";    // url to the page with text
    private static final String ARG_URI = "arg_uri";    // uri to the image to ocr and spritz

    private String mText;           // text, which could be initialized with share via feature
    private String mURL;
    private Uri    mUri;

    private EditText mEditText;     // the editable text view which will update mText, when necessary
    private boolean  mOcrEnabled;

    // true if this instance already did ocr on the given
    // image (new instances will get the chance to run ocr again)
    private boolean  m_didOcr;


    // note: not related to inputMEthodState
    private int mUsageState; // for fun (what usage message shall we show?!)

//    private ShowcaseView sv;


    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */

    static private String TAG = "Edit Text Fragment";

    public static PerusalEditTextFragment newInstance(int sectionNumber,
                                                      String text,
                                                      MainActivity.InputMethodState inputMethod)
    {
        Log.d(TAG, " newInstance");
        PerusalEditTextFragment fragment = new PerusalEditTextFragment();
        Bundle args = new Bundle();

        args.putInt(ARG_SECTION_NUMBER, sectionNumber);

        args.putInt(ARG_INPUT_METHOD, inputMethod.ordinal());
        if ( inputMethod.ordinal() == MainActivity.InputMethodState.TEXT_EDIT.ordinal() ) {
            args.putString(ARG_TEXT, text);
        } else if ( inputMethod.ordinal() == MainActivity.InputMethodState.URL_SPRITZ.ordinal() ) {
            args.putString(ARG_URL, text);
        }

        fragment.setArguments(args);
        return fragment;
    }

    public static PerusalEditTextFragment newInstance(int sectionNumber,
                                                      Uri uri,
                                                      MainActivity.InputMethodState inputMethod)
    {
        Log.d(TAG, " newInstance");
        PerusalEditTextFragment fragment = new PerusalEditTextFragment();
        Bundle args = new Bundle();

        args.putInt(ARG_SECTION_NUMBER, sectionNumber); // for the activity..

        args.putInt(ARG_INPUT_METHOD, inputMethod.ordinal());
        if ( inputMethod.ordinal() == MainActivity.InputMethodState.IMAGE_SHARE.ordinal() ) {
            args.putParcelable(ARG_URI, uri);
        }

        fragment.setArguments(args);
        return fragment;
    }

    public PerusalEditTextFragment() {
        mText = "";
        mURL = "";
        mUsageState = 1;
        mOcrEnabled = true; // if camera is detected
        m_didOcr = false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.d(TAG, " onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_edit_text, container, false);
        setHasOptionsMenu(true);

        mEditText = (EditText) rootView.findViewById(R.id.editTextPerusal);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");

        // when we finish loading the view and everything
        int inputMethod = getArguments().getInt(ARG_INPUT_METHOD);
        if ( inputMethod == MainActivity.InputMethodState.TEXT_EDIT.ordinal() ) {
            mText = getArguments().getString(ARG_TEXT);
            if ( mText != null && !mText.isEmpty() ) {
                mEditText.setText(mText);
                // and let the user hit the actionbar play button to spritz the text
            }
        } else if ( inputMethod == MainActivity.InputMethodState.URL_SPRITZ.ordinal() ) {
            mURL = getArguments().getString(ARG_URL);

            // TODO: if the url is actually to a picture image of some sort then
            // download the image and do ocr functionality instead
            // (1) check if the url ends in the known image formats..
            // (2) just load the image and test its format
            // leaning against (1) as its faster and might actually
            // be cleaner code

            // TODO: if the url is actually a pdf then think about that someday..

            // load the spritz fragment
            int textState = Perusal.Mode.URL.ordinal();
            ((MainActivity)getActivity()).
                    navigateToSpritzFragment(textState, mURL);

        } else if ( inputMethod == MainActivity.InputMethodState.IMAGE_SHARE.ordinal() ) {
            mUri = getArguments().getParcelable(ARG_URI);

            // perform ocr and then spritz
            if ( mOcrEnabled )
            {
                if ( !m_didOcr ){
                    Log.d(TAG, "OCR and then run editText fragment on text");
                    mText = doOcrGetText( mUri );
                    m_didOcr = true;
                }
            } else  {
                Toast.makeText( getActivity(),
                        "Ocr is disabled for now..",
                        Toast.LENGTH_SHORT).show();
                mText = "";
            }

            int textState = Perusal.Mode.TEXT.ordinal();
            ((MainActivity)getActivity())
                    .navigateToSpritzFragment(textState, mText);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        Log.d(TAG, "onCreateOptionsMenu");
        inflater.inflate(R.menu.edit_text_and_selection, menu);
    }

    private void longToast( String msg ) {
        Toast.makeText( getActivity(), msg,
                Toast.LENGTH_LONG ).show();
    }

    public void usage() {
        // a playful way of displaying the usage..
        switch (mUsageState)
        {
            case 1:
                longToast("Type or paste some text first!");
                mUsageState = 2;
                break;
            case 2:
                longToast("You can also share text from your web browser");
                mUsageState = 3;
                break;
            case 3:
                longToast("Check out your 'recent perusals' list then?");
                mUsageState = 4;
                break;
            case 4:
                longToast("Just keep on trying until you run out of cake");
                mUsageState = 5;
                break;
            case 5:
                longToast("Or until you actually have something to read..");
                mUsageState = 1;
            default:
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "FRAGMENT onOptionsItemSelected");
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_perform_spritz_on_text) {

            if (mEditText != null) {
                mText = mEditText.getText().toString();
            }

            if ( mText == null || mText.isEmpty() ) {
                usage();
                return true;
            }

            int textState = Perusal.Mode.TEXT.ordinal();
            ((MainActivity)getActivity())
                .navigateToSpritzFragment(textState, mText);

            return true;
        }
        return super.onOptionsItemSelected(item);
    }


//    // given the text state -> msg can be either text or a url..
//    // NOTE: this is the only entry point to spritzing.. just FYI
//    private void navigateToSpritzFragment(int textState, String msg) {
//        int peruseSectionNumber = 1; // force section nav number
//        boolean shouldAttemptSavePerusal = true;
//        Fragment fragment = PerusalSpritzFragment
//                .newInstance( peruseSectionNumber, textState,
//                        msg, shouldAttemptSavePerusal );
//
//        FragmentManager fragmentManager = getFragmentManager();
//        fragmentManager.beginTransaction()
//                .replace(R.id.container, fragment)
//                .commit();
//    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(TAG, " onAttach");
        ((MainActivity) activity).onSectionAttached(
                getArguments().getInt(ARG_SECTION_NUMBER));
    }


    // algorithm stuff

    public String doOcrGetText( Uri imageUri ) {
        if ( imageUri == null ) {
            return "";
        }

//        Toast.makeText(this, "This will take a few seconds..",
//                       Toast.LENGTH_LONG).show();

        Ocr ocr = new Ocr( getActivity() );

        Bitmap bitmap;
        try {
            bitmap = MediaStore.Images.Media
                    .getBitmap(getActivity().getContentResolver(), imageUri);
        } catch (Exception e){
            Log.d(TAG, "Could not create image from URI");
            return "";
        }

        int maxDimLength = 1000;
        bitmap = Helpers.resizeIfTooLarge(bitmap, maxDimLength);
        bitmap = Helpers.reorientImage( bitmap, imageUri );

        // remove the set background on release. they are here for testing purposes
//        findViewById(R.id.container).setBackgroundDrawable( new BitmapDrawable(bitmap) );

        ocr.setImage(bitmap);
        Ocr.Result result = ocr.performOcr();

//        bitmap = ocr.getImage();
//        findViewById(R.id.container).setBackgroundDrawable( new BitmapDrawable(bitmap) );

        if ( !result.isValid ) {
            Toast.makeText(getActivity(), "Ocr Failed", Toast.LENGTH_LONG).show();
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

            Toast.makeText(getActivity(), "Done", Toast.LENGTH_SHORT).show();
        }

        return result.text;
    }


}

