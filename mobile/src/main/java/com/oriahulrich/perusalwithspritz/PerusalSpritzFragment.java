package com.oriahulrich.perusalwithspritz;

/**
 * Created by oriahulrich on 12/14/14.
 */

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.oriahulrich.perusalwithspritz.adapters.RecentPerusalsAdapter;
import com.oriahulrich.perusalwithspritz.database.SQLiteDAO;
import com.oriahulrich.perusalwithspritz.pojos.Perusal;
import com.spritzinc.android.SimpleSpritzSource;
import com.spritzinc.android.SpritzSource;
import com.spritzinc.android.UrlSpritzSource;
import com.spritzinc.android.sdk.SpritzSDK;
import com.spritzinc.android.sdk.SpritzUser;
import com.spritzinc.android.sdk.view.SpritzBaseView;
import com.spritzinc.android.sdk.view.SpritzControlView;
import com.spritzinc.android.sdk.view.SpritzFullControlView;
import com.spritzllc.api.common.model.Content;

import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.datatype.Duration;

/**
 * A fragment for holding the Spritzing view (used only after accepting
 * the selections made in PerusalSelectionFragment or PerusalEditTextFragment)
 * This will load text from either class into Spritz' engine for Speed Reading
 */
public class PerusalSpritzFragment
        extends Fragment
        implements
        SpritzSDK.LoginEventListener,
        SpritzSDK.LoginStatusChangeListener {

    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final String ARG_MODE = "section_number";
    private static final String ARG_SPRITZ_TEXT = "spritz_text";
    private static final String ARG_SHOULD_SAVE = "shouldSavePerusal";

    private String mTextSpritz;
    private View mRootView;

    /** Spritz view specific member fields */
    private FrameLayout mFrameLayoutSpritzContainer;
    private SpritzBaseView mSpritzView;

    /** shared preferences and settings persentence */
    private SharedPreferences mSharedPrefs;
    private static final String SHARED_PREFS_NAME = "main";
    private static final String PREF_SPEED = "speed";


    private boolean mShouldSavePerusal;

    /// For the purpose of extracting the domain name ///
    // http://www.mkyong.com/regular-expressions/domain-name-regular-expression-example/
    private static final String DOMAIN_NAME_PATTERN = "^((?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z]{2,6}$";
    private static Pattern pDomainNameOnly;
    static {
        pDomainNameOnly = Pattern.compile(DOMAIN_NAME_PATTERN);
    }
    public static boolean isValidDomainName(String domainName) {
        return pDomainNameOnly.matcher(domainName).find();
    }
    public static String extractDomainName(String url) {
        Matcher m = pDomainNameOnly.matcher("url");
        if (m.find())
            return m.group(1);
        return "";
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    static private String TAG = "Spritz Fragment";

    private SQLiteDAO sqLiteDAO;

    public static PerusalSpritzFragment newInstance(int sectionNumber,
                                                    int mode,
                                                    String textSpritz,
                                                    boolean shouldSavePerusal )
    {
        Log.d(TAG, "FRAGMENT newInstance");

        Bundle args = new Bundle();
        args.putBoolean(ARG_SHOULD_SAVE, shouldSavePerusal); // should save new perusal if not already
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        args.putString(ARG_SPRITZ_TEXT, textSpritz);         // url or text
        args.putInt(ARG_MODE, mode);                         // URL or TEXT

        PerusalSpritzFragment fragment = new PerusalSpritzFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public PerusalSpritzFragment() {
        common_construct();
    }

    private void common_construct() {
        mTextSpritz = "";
        mShouldSavePerusal = false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, " onCreate");
        super.onCreate(savedInstanceState);
        sqLiteDAO = ((MainActivity)getActivity()).getSqLiteDAO();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.d(TAG, "FRAGMENT onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_spritz, container, false);
        mRootView = rootView;


        mSpritzView = setupSpritzView(inflater, rootView);
        mTextSpritz = getArguments().getString(ARG_SPRITZ_TEXT);

//        if (savedInstanceState == null) {
//            updateSpritzSpeed(mSpritzView); // from shared preferences
//        }

        return rootView;
    }

    private void updateSpritzSpeed(SpritzBaseView view) {
        int savedSpeed = getSharedPrefs().getInt(PREF_SPEED, -1);
        if (savedSpeed != -1 && view != null) {
            view.setSpeed(savedSpeed);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(TAG, "FRAGMENT onAttach");
        ((MainActivity) activity).onSectionAttached(
                getArguments().getInt(ARG_SECTION_NUMBER));
    }

    @Override
    public void onDestroy() {
        if ( mSpritzView != null
             && mSpritzView.getSpeed()
             != getSharedPrefs().getInt(PREF_SPEED, -1))
        {
            SharedPreferences.Editor editor = getSharedPrefs().edit();
            editor.putInt(PREF_SPEED, mSpritzView.getSpeed());
            editor.apply(); // (async) vs commit() (syncronous)
            mSpritzView = null;
        }
        super.onDestroy();
    }

    @Override
    public void onPause() {
        // Pause the spritz view.
        if(mSpritzView != null) {
            mSpritzView.pause();
        }

        SpritzSDK.getInstance().removeLoginStatusChangeListener(this);
        SpritzSDK.getInstance().removeLoginEventListener(this);

        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        SpritzSDK.getInstance().addLoginEventListener(this);
        SpritzSDK.getInstance().addLoginStatusChangeListener(this);

        if (mTextSpritz != null && !mTextSpritz.isEmpty()) {
            doSpritzing( mTextSpritz );
            mShouldSavePerusal = getArguments().getBoolean(ARG_SHOULD_SAVE);
            if ( mShouldSavePerusal ) {
                createAddPerusalToDB(sqLiteDAO, mTextSpritz);
            }
        }
    }

    /* Creates a peruse object from just text and stores it in the DB */
    // dao is the data storage db. @param text can be the url or the raw text
    private void createAddPerusalToDB( SQLiteDAO dao, String text )
    {
        String title = "<No title>";

        // create title from URL or first three words of the text
        int mode = getArguments().getInt(ARG_MODE);
        if (mode == Perusal.Mode.URL.ordinal()) {
            title = makeTitleFromURL( text );
        } else if (mode == Perusal.Mode.TEXT.ordinal()) {
            title = makeTitleFromText(text);
        }

        // create the perusal and add it to the database
        Perusal perusal = new Perusal();
        perusal.setTitle(title);
        perusal.setText(text);
        perusal.setSpeed(mSpritzView.getSpeed());
        perusal.setModeInt(getArguments().getInt(ARG_MODE));
        perusal = dao.createPerusal( perusal );

        // if success then toast it's success!
        if (perusal != null) {
            Log.d(TAG, "success! added perusal to DB");
            Toast.makeText(getActivity().getBaseContext(), "'" + title + "' saved!",
                    Toast.LENGTH_SHORT).show();
        } else {
            Log.d(TAG, "error.. couldnt add perusal to DB");
//            Toast.makeText(getActivity().getBaseContext(),  + " was not added..",
//                    Toast.LENGTH_SHORT).show();
        }
    }

    /// attempts to create a title from the give url string..
    /// returns a string which might be the domain name of the url
    public String makeTitleFromURL( String text ) {
        String title = extractDomainName( text );

        if ( !title.isEmpty() ) {
            return title;
        }

        /// otherwise we need to come up with a way
        /// of extracting some title from the url
        String[] firstWords = text.split("//");

        if ( firstWords.length >= 2 ) {
            title = firstWords[1];
        } else if ( firstWords.length == 1 ) {
            title = firstWords[0];
        } else {
            title = text;
        }

        return title;
    }

    /// takes a string and attempts to extract the
    /// first 3 words from it (returned and deliminated
    /// by whitespaces)
    public String makeTitleFromText( String text ) {
        String[] firstWords = text.split(" ");
        String title = "<Untitled>";
        Arrays.copyOfRange( firstWords, 0, 3 );
        if ( firstWords.length > 0 )
            title = Helpers.capitalize(firstWords[0]);
        if ( firstWords.length > 1 )
            title += " " + Helpers.capitalize(firstWords[1]);
        if ( firstWords.length > 2 )
            title += " " + Helpers.capitalize(firstWords[2]);
        return title;
    }


    private SharedPreferences getSharedPrefs() {
        if (mSharedPrefs == null)
            mSharedPrefs = getActivity()
                    .getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        return mSharedPrefs;
    }

    /// ---

    public boolean doSpritzing(String text) {
        boolean success = true;

        if ( mSpritzView == null ){
            Log.d(TAG, "Spritz View is null..");
            return false;
        }

        String testUrl = "http://sdk.spritzinc.com/sampleText/HelloWorld.html";

        String sampleMessage =  "Go back to perusal page, then paste " +
                                "some text and hit the play button!";
        try {

            SpritzSource source;
            int mode = getArguments().getInt(ARG_MODE);

            if (mode == Perusal.Mode.URL.ordinal()) {
                source = new UrlSpritzSource( text, Content.SelectorType.CSS, "p",
                                              new Locale("en", "US") );
            } else if (mode == Perusal.Mode.TEXT.ordinal()) {
                source = new SimpleSpritzSource( text, new Locale("en", "US") );
            } else {
                source = new SimpleSpritzSource( sampleMessage, new Locale("en", "US") );
            }

            mSpritzView.rewind();
            mSpritzView.start(source);

        } catch (Exception e) {
            Log.d(TAG, "doSpritzing failed. " +  e.getMessage());
            success = false;
        }

        return success;
    }

    private SpritzBaseView setupSpritzView(LayoutInflater inflater, View rootView) {
        mFrameLayoutSpritzContainer
                = (FrameLayout) rootView.findViewById(R.id.frameLayoutSpritzContainer);
                // destination view

        inflater.inflate(R.layout.fragment_spritz_full,
                mFrameLayoutSpritzContainer, true);
                // append source view to dst

        mSpritzView = (SpritzBaseView) rootView.findViewById(R.id.spritzView);

        if (mSpritzView instanceof SpritzFullControlView) {
            ((SpritzControlView)mSpritzView).setPopupMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    boolean handled = false;
                    Toast.makeText(getActivity(), "Not Sure?", Toast.LENGTH_SHORT).show();
                    return handled;
                }
            });
        }
        return mSpritzView;
    }

    public void onBtnPauseClick(View view) {
        mSpritzView.pause();
    }

    public void onBtnPlayClick(View view) {
        mSpritzView.resume();
    }

    private void spritz(SpritzSource source) {
        mSpritzView.start(source);
    }


    @Override
    public void onLoginFail(String errorMessage, Throwable throwable) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Login Failed");

        StringBuilder message = new StringBuilder();
        message.append(errorMessage);

        if (throwable != null) {
            message.append(": ");
            message.append(throwable.getMessage());
        }

        builder.setMessage(message.toString());
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Ignore
            }
        });
        builder.show();
    }

    @Override
    public void onLoginStart() {
        // ignore
    }

    @Override
    public void onLoginSuccess() {
        // ignore
    }

    @Override
    public void onUserLoginStatusChanged(boolean b) {
//        updateLoggedInUser();
    }


    private void updateLoggedInUser() {

//        SpritzUser user = SpritzSDK.getInstance().getLoggedInUser();
//
//        Button btnLogin = (Button) mRootView.findViewById(R.id.btnLogin);
//        TextView tvLoggedInUser = (TextView) mRootView.findViewById(R.id.tvLoggedInUser);
//
//        if (user == null) {
//
//            tvLoggedInUser.setText("None");
//            btnLogin.setText("Login");
//
//        } else {
//
//            StringBuilder userInfo = new StringBuilder();
//            userInfo.append(user.getUserId());
//
//            if (user.getNickname() != null) {
//                userInfo.append(" (");
//                userInfo.append(user.getNickname());
//                userInfo.append(")");
//            }
//
//            tvLoggedInUser.setText(userInfo.toString());
//            btnLogin.setText("Logout");
//        }
    }
}
