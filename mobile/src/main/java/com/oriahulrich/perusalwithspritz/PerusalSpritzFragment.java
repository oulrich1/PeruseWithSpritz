package com.oriahulrich.perusalwithspritz;

/**
 * Created by oriahulrich on 12/14/14.
 */

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.oriahulrich.perusalwithspritz.Settings.SetSpritzPreferencesActivity;
import com.oriahulrich.perusalwithspritz.adapters.TextPartitionsAdapter;
import com.oriahulrich.perusalwithspritz.database.SQLiteDAO;
import com.oriahulrich.perusalwithspritz.lib.Helpers;
import com.oriahulrich.perusalwithspritz.pojos.Perusal;
import com.oriahulrich.perusalwithspritz.pojos.TextPartition;
import com.spritzinc.android.SimpleSpritzSource;
import com.spritzinc.android.SpritzSource;
import com.spritzinc.android.UrlSpritzSource;
import com.spritzinc.android.sdk.SeekMode;
import com.spritzinc.android.sdk.SpritzSDK;
import com.spritzinc.android.sdk.SpritzViewListener;
import com.spritzinc.android.sdk.view.SpritzBaseView;
import com.spritzinc.android.sdk.view.SpritzControlView;
import com.spritzinc.android.sdk.view.SpritzFullControlView;
import com.spritzllc.api.common.model.Content;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    AdView mAdView;

    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final String ARG_MODE = "section_number";
    private static final String ARG_PERUSAL_ID = "perusal_id";
    private static final String ARG_SPRITZ_TEXT = "spritz_text";
    private static final String ARG_TEXT_PARTITION_IDX = "text_partition_idx";
    private static final String ARG_SHOULD_SAVE = "shouldSavePerusal";

    private String mTextSpritz;
    private View mRootView;
    private boolean mShouldSavePerusal;

    /** Spritz view specific member fields */
    private FrameLayout mFrameLayoutSpritzContainer;
    private SpritzSource   mSpritzSource;
    private SpritzBaseView mSpritzView;
    private int nSpritzViewId; // set kludge note for when preferencs activity is started

    /** shared preferences and settings persentence */
    private SharedPreferences mSharedPrefs;
    private static final String SHARED_PREFS_NAME = "main";
    private static final String PREF_SPEED = "speed";

    // Experimental:
    private boolean m_didDetectWearable;
    private boolean m_isWearableSpritzEnabled;

    private static final int SPRITZ_PREFERENCES_ACTIVITY_REQUEST_CODE = 124;
    private boolean bDidInitTextPartitions;     // used upon spritz load text event (spritz view lsitener to be exact)
    private int m_nCurTextPartitionIdx;         // index into the textPartitions
    private int m_nWordsPerChunk; // ie: per partition
    private ArrayList<TextPartition> m_textPartitions; // smaller portions of text that the TTS can handle (ie: < 1000 characters each partition)
    private boolean m_bToggleTextToSpeech;      // toggled by user to perform TTS on spritz text
    private boolean m_bTextToSpeechWasStopped;  // set if the user stopped TTS and used to prevent recuring start calls
    private boolean m_bIsTTSEngineInit;         // is the engine init
    private boolean m_bUserDidTryTTSBeforeInit; // user attempted TTS before engine init, let user know when ready
    private TextToSpeech m_textToSpeech;        // the TTS engine

    // get text from article at url, from the spritz view since it parsed it already..
    // but only do it once and save the text so we can partition it differently and modify it possibly
    private boolean m_bExtractedArticleTextFromSpritzView;
    private String m_sArticleText;

    // list view that contains the text partitions
    TextPartitionsAdapter mTextPartitionAdapter;
    ListView mTextPartitionList;

    // The perusal object to which modifications are made
    // when this fragment is about to end then the perusal
    // is used to update the DB
    Perusal mPerusal;

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

//    public static PerusalSpritzFragment newInstance(int sectionNumber,
//                                                    int mode,
//                                                    String textSpritz,
//                                                    boolean shouldSavePerusal )

    public static PerusalSpritzFragment newInstance(int sectionNumber, Perusal perusal)
    {
        Log.d(TAG, "FRAGMENT newInstance");

        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        args.putInt(ARG_MODE, perusal.getMode().ordinal());         // URL or TEXT mode
        args.putLong(ARG_PERUSAL_ID, perusal.getId());
        args.putBoolean(ARG_SHOULD_SAVE, perusal.bShouldSaveToDB);  // should save new perusal if not already
        args.putString(ARG_SPRITZ_TEXT, perusal.getText());         // url or text value
        args.putInt(ARG_TEXT_PARTITION_IDX, perusal.getTextPartitionIndex());   // where the user left off

        PerusalSpritzFragment fragment = new PerusalSpritzFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public PerusalSpritzFragment() {
        common_construct();
    }

    private void common_construct() {
        nSpritzViewId = -1;
        mTextSpritz = "";
        mShouldSavePerusal = false;

        m_didDetectWearable = false;        // if we detected a wearable
        m_isWearableSpritzEnabled = false;  // if the user wants to use their wearable if it is connected

        // text to speech
        m_bToggleTextToSpeech = false;        // toggled by the user
        m_bIsTTSEngineInit = false;           // flag for whether or not the engine is init
        m_bUserDidTryTTSBeforeInit = false;   // if true, in onInitListener, let the user know to try again
        m_bTextToSpeechWasStopped = false;
        m_textToSpeech = null;                // the text to speech engine
        m_nWordsPerChunk = 0;
        m_nCurTextPartitionIdx = 0;           // first text partition
        bDidInitTextPartitions = false;

        // url text gathering
        m_bExtractedArticleTextFromSpritzView = false;
        m_sArticleText = "";

        m_textPartitions = new ArrayList<TextPartition>();
        mPerusal = new Perusal();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, " onCreate");
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.d(TAG, "FRAGMENT onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_spritz, container, false);
//        ((MainActivity)getActivity()).setActionBarTitle("Peruse");

        // init members
        try {
            sqLiteDAO = ((MainActivity) getActivity()).getSqLiteDAO();
        } catch(Exception e) {
            Log.d(TAG, e.getMessage());
        }

        createTextToSpeechIfNotNull();
        updateTextToSpeechSettingsFromPreferences();

        // set up the spritzing views
        mRootView = rootView;
        updateWordsPerChunk();
        mSpritzView = setupSpritzView(inflater, rootView);
        updateSpritzColorsFromPreferences();
        mTextSpritz = getArguments().getString(ARG_SPRITZ_TEXT);
        setCurTextPartitionIdx(getArguments().getInt(ARG_TEXT_PARTITION_IDX));
        setHasOptionsMenu(true);

        // migrating to this..  from the above
        mPerusal.setId(getArguments().getLong(ARG_PERUSAL_ID));
        mPerusal.setText(getArguments().getString(ARG_SPRITZ_TEXT));
        mPerusal.setTextPartitionIndex(getArguments().getInt(ARG_TEXT_PARTITION_IDX));
        // ...

        // TODO: track what the user reads
//        String[] keywords = {"book", "books", "audio", "news", "technology", "recipe",
//                "food", "water", "security", "money"};
        String[] keywords = {};
        InitAdView(keywords); // sets member var and builds an ad request and starts the ad
        ShowAdView(); // sets visible

        // load the default preference values from the xml
        PreferenceManager.setDefaultValues(getActivity(), R.xml.spritz_preferences, false);


        // set custom settings using shared preference values as well
        setSpritzSpeedFromPrefs(mSpritzView);

        return rootView;
    }

    private void createTextToSpeechIfNotNull() {
        if ( m_textToSpeech != null ) return;
        createTextToSpeech();
    }

    private boolean isTextPartitionPlaybackFinished() {
        boolean bIsComplete = false;
        if (m_nCurTextPartitionIdx >= m_textPartitions.size()) {
            m_bToggleTextToSpeech = false;
            m_nCurTextPartitionIdx = 0;
            bIsComplete = true;
        }
        return bIsComplete;
    }

    // returns false if not iterating anymore.. otherwise returns true if there are text
    // partitions still left to play
    private boolean incrementTextPartitionIdxAndCheckIsFinished() {
        m_nCurTextPartitionIdx++;
        return isTextPartitionPlaybackFinished();
    }

    private void setCurTextPartitionIdx(int index) {
        m_nCurTextPartitionIdx = index;
    }

    private int getCurTextPartitionIdx() {
        return m_nCurTextPartitionIdx;
    }

    private void createTextToSpeech() {
        try {
            m_textToSpeech = new TextToSpeech(getActivity(), new TTS_OnInitListenerImpl());
            m_textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                String expectedid = TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID;

                @Override
                public void onStart(String utteranceId) {
                    assert (expectedid.equals(utteranceId));
                }

                @Override
                public void onDone(String utteranceId) {
                    if ( !m_bTextToSpeechWasStopped ) {
                        // then we are done because the user stopped TTS, so we dont want to play again
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                boolean bIsFinished = incrementTextPartitionIdxAndCheckIsFinished();
                                // the adapter is updated BEFORE doing another round of TTS.. since
                                // we are finished in this context
                                mTextPartitionAdapter.setCurrentSelection(getCurTextPartitionIdx());
                                if(bIsFinished) {
                                    OnFinishedPlayingTextPartitions();
                                } else {
                                    PerformTextToSpeech(); // knows to perform it on the on the next text partition
                                }

                            }
                        });
                    } else {
                        // handled TTS onDone from user-stop action
                        m_bTextToSpeechWasStopped = false;
                    }
                }

                @Override
                public void onError(String utteranceId) {
                }
            });
        } catch( Exception e ) {
            Log.d(TAG, e.getMessage());
        }
    }

    // attempts to getCurrentReticleLineColor and parse the string to an
    // int color id which spritz will use to set the reticle line color
    // if this fails then the color defaults to Black
    private void updateSpritzColorsFromPreferences() {
        // set the user's preference for the view coloring
        int color;
        try {
            String sColor = getCurrentReticleLineColor();
            color = Color.parseColor(sColor);
        } catch ( Exception e ) {
            color = Color.BLACK;
        }
        mSpritzView.setReticleLineColor(color);

        try {
            String sColor = getCurrentWordColor();
            color = Color.parseColor(sColor);
        } catch ( Exception e ) {
            color = Color.BLACK;
        }
        mSpritzView.setTextColor(color);

        try {
            String sColor = getCurrentTextHighlightColor();
            color = Color.parseColor(sColor);
        } catch ( Exception e ) {
            color = Color.RED;
        }
        mSpritzView.setTextHighlightColor(color);
        mSpritzView.setSweeperEnabled(getCurrentSweeperEnableState() == 1);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.d(TAG, "onActivityCreated Start");

        // create the text partitiosn list and attach the adapter
        mTextPartitionList = (ListView) getActivity().findViewById(R.id.spritz_text_sections_listview);
        mTextPartitionAdapter = new TextPartitionsAdapter(getActivity(), getActivity().getLayoutInflater());
        mTextPartitionList.setAdapter(mTextPartitionAdapter);
        mTextPartitionList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                doSpritzingTextPartitions(position);
            }
        });
        registerForContextMenu(mTextPartitionList);
        super.onActivityCreated(savedInstanceState);
    }

    /* created when we long hold a specific item */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.context_menu_item, menu);
    }

    /* When an item is selected in the context menu */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo itemInfo
                = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        if (!getUserVisibleHint()) {
            return false;
        }

        switch (item.getItemId()) {
            case R.id.actionItemEdit:
                return editTextPartition(itemInfo.position);
            case R.id.actionItemDelete:
                return removeTextPartition(itemInfo.position);
            default:
                return super.onContextItemSelected(item);
        }
    }
    private boolean editTextPartition(int position) {
        final TextPartition partition = (TextPartition) mTextPartitionAdapter.getItem(position);
        final String oldTitle = partition.getText();
        final int pos = position;

        /* show dialog - get new title */
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View edit = View.inflate(getActivity(), R.layout.edit_text, null);

        final EditText perusalTitleInputEditText = (EditText) edit.findViewById(R.id.item_edit_text);
        perusalTitleInputEditText.setText(oldTitle);
        perusalTitleInputEditText.setSelection(0);

        builder.setTitle("Edit Text");
        builder.setView(edit);
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String text = perusalTitleInputEditText.getText().toString();
                mTextPartitionAdapter.setItem(pos, new TextPartition(text));
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
        return true;
    }
    private boolean removeTextPartition(int position) {
        mTextPartitionAdapter.remove(position);
        return true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        Log.d(TAG, "onCreateOptionsMenu");
        inflater.inflate(R.menu.spritz_action_menu, menu);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
//        outState.putString("WORKAROUND_FOR_BUG_19917_KEY", "WORKAROUND_FOR_BUG_19917_VALUE");
//        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, " onOptionsItemSelected");
        int id = item.getItemId();
        if (id == R.id.action_text_to_speech) {
            OnClickTextToSpeech();
            return true;
        } else if ( id == R.id.action_spritz_custom_settings) {
            // Extreme kludge to ensure that Spritz api does not commit a transaction
            // right after it finishes onSaveInstanceState. Damn it Spritz..
            nSpritzViewId = mSpritzView.getId();
            mSpritzView.setId(-1);
            Intent intent = new Intent();
//            Set<Voice> voices;
//            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                // only for gingerbread and newer versions
//                try {
//                    voices = m_textToSpeech.getVoices();
//                } catch ( Exception e ) {
//                    Log.d(TAG, " Can't Get Voices.. " + e.getMessage()); // probably before init..
//                }
//                // intent.putExtra("voices", m_textToSpeech.getVoices());
//            }
            intent.setClass(getActivity().getBaseContext(), SetSpritzPreferencesActivity.class);
            startActivityForResult(intent, SPRITZ_PREFERENCES_ACTIVITY_REQUEST_CODE);
           return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SPRITZ_PREFERENCES_ACTIVITY_REQUEST_CODE) {
            // reset the view id.. kludge undo..
            mSpritzView.setId(nSpritzViewId);

            // gets the user's preference and tells spritz to set it's retical color to it
            updateSpritzColorsFromPreferences();

            // resize the page size (number of words per partition) based on user preference
            updateTextPartitionsAndAdapter();

            // update TTS settings
            updateTextToSpeechSettingsFromPreferences();
        }
    }

    private void updateTextToSpeechSettingsFromPreferences() {
        if (m_textToSpeech != null)
        {
            m_textToSpeech.setPitch(getCurrentTTSPitch());           // nominal = 1
            m_textToSpeech.setSpeechRate(getCurrentTTSSpeechRate()); // nominal = 1
        }
    }

    /// Preference Getters ///
    private String getDefaultSweeperEnableState() { // 0 or 1
        return getActivity().getResources().getString(
                R.string.pref_spritz_focus_sweeping_animation_default);
    }
    private int getCurrentSweeperEnableState() {
        Context context = getActivity();
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        String val = prefs.getString("pref_spritz_focus_sweeping_animation",
                getDefaultSweeperEnableState());

        if ( val != null ) {
            if (val.equals("Enable"))
                return 1;
            else if (val.equals("Disable"))
                return 0;
        }
        return -1;
    }

    private String getDefaultTTSPitch(){
        return getActivity().getResources().getString(R.string.pref_tts_pitch_default);
    }
    private float getCurrentTTSPitch() {
        Context context = getActivity();
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        String val = prefs.getString("pref_tts_pitch", getDefaultTTSPitch());
        return Float.parseFloat(val);
    }

    private String getDefaultTTSSpeechRate(){
        return getActivity().getResources().getString(R.string.pref_tts_speech_rate_default);
    }
    private float getCurrentTTSSpeechRate() {
        Context context = getActivity();
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        String val = prefs.getString("pref_tts_speech_rate", getDefaultTTSSpeechRate());
        return Float.parseFloat(val);
    }

    // Shared preferences specifically..
    private String getDefaultReticleLineColor(){
        Context context = getActivity();
        return context.getResources().getString(R.string.preference_spritz_reticle_line_color);
    }
    private String getCurrentReticleLineColor() {
        Context context = getActivity();
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        return  prefs.getString("pref_spritz_reticle_color", getDefaultReticleLineColor());
    }
    private String getDefaultWordColor(){
        Context context = getActivity();
        return context.getResources().getString(R.string.pref_spritz_default_word_color);
    }
    private String getCurrentWordColor() {
        Context context = getActivity();
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        return  prefs.getString("pref_spritz_word_color", getDefaultWordColor());
    }
    private String getDefaultHightlightColor(){
        Context context = getActivity();
        return context.getResources().getString(R.string.pref_spritz_default_highlight_color);
    }
    private String getCurrentTextHighlightColor() {
        Context context = getActivity();
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        return  prefs.getString("pref_spritz_text_highlight_color", getDefaultHightlightColor());
    }
    private String getDefaultWordsPerChunk(){
        return getResources().getString(R.string.pref_spritz_default_chunk_size);
    }
    private int getPrefWordsPerChunk() {
        Context context = getActivity();
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        int wordsPerChunk;
        try {
            wordsPerChunk = Integer.parseInt(prefs.getString("pref_spritz_chunk_size", getDefaultWordsPerChunk()));
        } catch ( NumberFormatException e ) {
            wordsPerChunk = 30; // just a nice number in case it fails
            Log.d(TAG, "Exception: " + e.getMessage());
        } catch ( Exception e ) {
            wordsPerChunk = 30; // just a nice number in case it fails
            Log.d(TAG, "Exception: " + e.getMessage());
        }
        return wordsPerChunk;
    }

    // simply updates this's member variable with the value returned from getPrefWordsPerChunk
    private int updateWordsPerChunk() {
        setWordsPerChunk(getPrefWordsPerChunk());
        return getWordsPerChunk();
    }

    // simply returns the member variable, regardless of what is in the preferences. make sure that
    // the variable had been updated with updateWordsPerChunk() or similar..
    private int getWordsPerChunk() {
        return m_nWordsPerChunk;
    }
    private void setWordsPerChunk(int nWords) {
       m_nWordsPerChunk = nWords;
    }

    private void OnClickTextToSpeech() {
        // TODO: toggle hook text to speech into the output of a spritz transmitter
        // OR Hook spritz into the text to speech input. Maybe a spritz callback per wordo?
        if ( !m_bIsTTSEngineInit ) {
            m_bUserDidTryTTSBeforeInit = true;
            Toast.makeText( getActivity(),
                    "Text to Speech is still initializing, we'll get back to you when it is ready.",
                    Toast.LENGTH_SHORT).show();
        } else {
            m_bToggleTextToSpeech = !m_bToggleTextToSpeech; // TOGGLE
            if ( m_bToggleTextToSpeech ) {
                Log.d(TAG, "Text to Speech Enabled");
                PerformTextToSpeech();
            } else {
                Log.d(TAG, "Text to Speech Disabled");
                StopTextToSpeech();
            }
        }
    }

    /// End Preference Getters ///


    private void TTSPostInit() { }


    // Returns a list of Partitions, each partition was extracted from the larger
    // text where each partition was deliminated from another one by a space. O(n)
    // also ensures one space at the most between each word
    private ArrayList<TextPartition> splitTextIntoParitions( String text, int nWordsPerChunk ) {
        int max_char_count = nWordsPerChunk * 5;                 // ie: max partition size
        TextPartition partition = null;
        ArrayList<TextPartition> partitions = new ArrayList<>(); // return array of partitions

        // trim out all of the unnecessary spaces so that:
        // article -> sentence article
        // sentence -> word space word
        text = text.trim();
        text = text.replaceAll("\\s+", " ");

        // split the text and store the cur partition into the list
        // working from the beginning of the string.. end_idx is at
        // the end of the current partition
        while ( text.length() > max_char_count ) {
            int end_idx = max_char_count;
            while(end_idx >= 0 && text.charAt(end_idx) != ' ') {
                end_idx -= 1; // go backwards until a space is found
            }

            // reference to a shiny new partition
            partition = new TextPartition();

            // push the current head partition:
            String substring = text.substring(0, end_idx).trim();
            if ( !substring.isEmpty() ) {
                // normal case
                partition.setText(substring);
            } else {
                // edge case ( partition required is too big, throw exception??)
                partition.setText(text.substring(0, max_char_count).trim());
                end_idx = max_char_count;
            }
            partitions.add(partition);

            // remove the cur partition 'duplicate' from the
            // text and continue on with the rest of the string
            text = text.substring(end_idx, text.length()).trim();
        }
        // push back the last bit of left over text
        if ( !text.isEmpty() ) {
            partition = new TextPartition();
            partition.setText(text);
            partitions.add(partition);
        }

        return partitions;
    }

    String getSpritzText() {
        final int mode = getArguments().getInt(ARG_MODE);
        String text;
        if (mode == Perusal.Mode.TEXT.ordinal()) {
            text = mTextSpritz;
        } else {
            if (!m_bExtractedArticleTextFromSpritzView) {
                m_sArticleText = mSpritzView.getText();
                m_bExtractedArticleTextFromSpritzView = true;
            }
            text = m_sArticleText;
        }
        return text;
    }

    // TODO: attach/stream the spritz to the text to speech engine.. somehow
    private void PerformTextToSpeech() {
        // split into partitions of text
        ttsPlayUtteranceText(m_textPartitions.get(m_nCurTextPartitionIdx).getText());
    }

    // TODO: halt text to speech
    private void StopTextToSpeech() {
        m_bTextToSpeechWasStopped = true;
        m_textToSpeech.stop();
    }

    // Returns true if successfully started speaking. Returns false if the previous
    // speech was interupted by the current, new, speech.. also returns false if the
    // new speech was short enough to not "register" or detect speaking was happening
    public boolean ttsPlayUtteranceText( String text ) {
        boolean bWasSpeaking = m_textToSpeech.isSpeaking();
        m_textToSpeech.stop();

        /// Backwards compatible
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put( TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
                     TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID );
        m_textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, hashMap);
        boolean bIsNowSpeaking = m_textToSpeech.isSpeaking();
        return bIsNowSpeaking && !bWasSpeaking;
    }

    // should go into spritz helper class
    private void setSpritzSpeedFromPrefs(SpritzBaseView view) {
        int savedSpeed = getSharedPrefs().getInt(PREF_SPEED, -1);
        if (savedSpeed != -1 && view != null) {
            view.setSpeed(savedSpeed);
        }
    }

    private void storeSpritzSpeedToPrefs(SpritzBaseView view) {
        if ((view != null) && view.getSpeed() != getSharedPrefs().getInt(PREF_SPEED, -1))
        {
            SharedPreferences.Editor editor = getSharedPrefs().edit();
            editor.putInt(PREF_SPEED, view.getSpeed());
            editor.apply(); // (async) vs commit() (syncronous)
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
        super.onDestroy();
        mSpritzView = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, " onResume, doing spritzing soon..");
        SpritzSDK.getInstance().addLoginEventListener(this);
        SpritzSDK.getInstance().addLoginStatusChangeListener(this);
        if (mTextSpritz != null && !mTextSpritz.isEmpty()) {
            if ( getArguments().getBoolean(ARG_SHOULD_SAVE) ) {
                createAddPerusalToDB(sqLiteDAO, mTextSpritz);
                Bundle args = getArguments();
                // should not create perusal anymore, simple update if necessary
                args.putBoolean(ARG_SHOULD_SAVE, false);
                setArguments(args);
            }

            /// TODO: wearables..
            if ( m_didDetectWearable && m_isWearableSpritzEnabled ){

            } else {
                doSpritzing(mTextSpritz, getArguments().getInt(ARG_MODE));
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
            title = makeTitleFromURL(text);
        } else if (mode == Perusal.Mode.TEXT.ordinal()) {
            title = makeTitleFromText(text);
        }

        // create the perusal and add it to the database
        Perusal perusal = new Perusal();
        perusal.setTitle(title);
        perusal.setText(text);
        perusal.setSpeed(mSpritzView.getSpeed());
        perusal.setModeInt(getArguments().getInt(ARG_MODE));
        perusal = dao.createPerusal(perusal);

        // if success then toast it's success!
        if (perusal != null) {
            Log.d(TAG, "success! added perusal to DB");
            Toast.makeText(getActivity().getBaseContext(), "'" + title + "' saved!",
                    Toast.LENGTH_SHORT).show();
        } else {
            Log.d(TAG, "error.. couldn't add perusal to DB");
        }
    }

    /// attempts to create a title from the give url string..
    /// returns a string which might be the domain name of the url
    public String makeTitleFromURL( String text ) {
        String title = extractDomainName(text);

        // if there was no noticable domain name
        // then extract the first few words
        if ( title.isEmpty() ) {
            String[] firstWords = text.split("//");
            if (firstWords.length >= 2) {
                title = firstWords[1];
            } else if (firstWords.length == 1) {
                title = firstWords[0];
            } else {
                title = text;
            }
        }

        return title;
    }

    /// takes a string and attempts to extract the
    /// first 3 words from it (returned and deliminated
    /// by whitespaces)
    public String makeTitleFromText( String text ) {
        String[] firstWords = text.split(" ");
        String title = "";
        firstWords = Arrays.copyOfRange( firstWords, 0, 3 );

        // validate the title strings ( remove bad unstorable characters )
        for (String word : firstWords) {
            if ( word == null || word.length() == 0 )
                continue;
            word = word.replace("\'", "");
            word = word.replace("\"", "");
            title += Helpers.capitalize( word ) + " ";
        }
        if ( title.isEmpty() )
            title = "<Untitled>";
        title = title.trim();
        return title;
    }


    private SharedPreferences getSharedPrefs() {
        if (mSharedPrefs == null)
            mSharedPrefs = getActivity()
                    .getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        return mSharedPrefs;
    }


    private boolean bIsAdViewVisible = false;

    // call whenever you stop playing the text partitions completely
    // (1) when the user spritzes all of the the text.
    // (2) when the user TTS all of the text..
    private void OnFinishedPlayingTextPartitions()
    {
        if (!bIsAdViewVisible) {
            ShowAdView();
        }
    }

    private void InitAdView(String[] keywords)
    {
        mAdView = (AdView) mRootView.findViewById(R.id.adViewTextPartitionItem);
        AdRequest.Builder adRequestBuilder = new AdRequest.Builder();
        for (String keyword : keywords ) {
            adRequestBuilder.addKeyword(keyword);
        }
        mAdView.loadAd(adRequestBuilder.build());
    }

    private void ShowAdView()
    {
        // pop up the ad banner
        mAdView.setVisibility(View.VISIBLE);
        bIsAdViewVisible = true;
    }

    private void HideAdView()
    {
        mAdView.setVisibility(View.INVISIBLE);
        bIsAdViewVisible = false;
    }

    /// ---
    // takes the index into text partitions array and starts spritzing that index-th text partition.
    // resets member variable m_nCurTextPartitionIdx to the partition_index specified and updates the adapter
    // with that selection
    private void doSpritzingTextPartitions(int partition_index ) {
        if ( partition_index < 0 ) {
            Log.d(TAG, "doSpritzingTextPartitions:: negative indecies are not valid");
            return; // negative indecies are not valid
        }
        setCurTextPartitionIdx(partition_index);
        if(isTextPartitionPlaybackFinished()){
            OnFinishedPlayingTextPartitions();
        } else {
            doSpritzing(m_textPartitions.get(m_nCurTextPartitionIdx).getText(),
                    Perusal.Mode.TEXT.ordinal());
        }
        // note: this is done after spritzing, since in this method we want to do spritzing..
        mTextPartitionAdapter.setCurrentSelection(m_nCurTextPartitionIdx);
    }

    // depending on arg_mode, text can be raw text or URL
    public boolean doSpritzing(String text, int mode) {
        boolean success = true;

        if ( mSpritzView == null ){
            Log.d(TAG, "Spritz View is null..");
            return false;
        }

        try {
            if (mode == Perusal.Mode.URL.ordinal()) {
                String cssSelectors = "p";
                mSpritzSource = new UrlSpritzSource( text, Content.SelectorType.CSS,
                                              cssSelectors, new Locale("en", "US") );
            } else if (mode == Perusal.Mode.TEXT.ordinal()) {
                mSpritzSource = new SimpleSpritzSource( text, new Locale("en", "US") );
            } else {
                String sampleMessage =
                        "Go back to perusal page, then paste " +
                        "some text and hit the play button!";
                mSpritzSource = new SimpleSpritzSource( sampleMessage, new Locale("en", "US") );
            }
            if ( mSpritzView.isStarted() ) {
                mSpritzView.pause();
            }
            mSpritzView.rewind();
            mSpritzView.start(mSpritzSource);
        } catch (Exception e) {
            Log.d(TAG, "doSpritzing failed. " +  e.getMessage());
            success = false;
        }

        return success;
    }

    private SpritzBaseView setupSpritzView(LayoutInflater inflater, View rootView) {
        mFrameLayoutSpritzContainer = (FrameLayout) rootView.findViewById(R.id.frameLayoutSpritzContainer);
        inflater.inflate(R.layout.fragment_spritz_full,
                mFrameLayoutSpritzContainer, true);

        mSpritzView = (SpritzBaseView) rootView.findViewById(R.id.spritzView);
        if (mSpritzView instanceof SpritzFullControlView) {
            ((SpritzControlView)mSpritzView).setPopupMenuItemClickListener(
                    new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            boolean handled = false;
                            Toast.makeText(getActivity(), "Not Sure?", Toast.LENGTH_SHORT).show();
                            return handled;
                        }
                    });
        }
        mSpritzView.setSpritzViewListener(new TTS_SpritzViewListenerImpl());

        return mSpritzView;
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "On Pause");
        m_textToSpeech.stop();

        // kludge to prevent a transaction during on save instance state.
        // Since a commit after onSaveInstance State is called will cause
        // an exception.. I set the view id to -1 so the spritz view does
        // not attempt to call onSaveInstance state when we should be pausing..
        nSpritzViewId = mSpritzView.getId();
        mSpritzView.setId(-1);

        if(mSpritzView != null) {
            mSpritzView.pause();
        }
        storeSpritzSpeedToPrefs(mSpritzView);
        storeCurTextPartitionIdxToDB(sqLiteDAO);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "On Stop");
        if ( mSpritzView != null ) {
            mSpritzView.pause();
        }
        storeSpritzSpeedToPrefs(mSpritzView);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        m_textToSpeech.shutdown();
        m_textToSpeech = null;
    }

    private void storeCurTextPartitionIdxToDB(SQLiteDAO dao) {
        mPerusal.setTextPartitionIndex(getCurTextPartitionIdx());
        int result = dao.updatePerusalCurSelectionIdx(mPerusal);
        Log.d(TAG, "Store Text Partition INDEX result: " + Integer.toString(result));
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


    private void updateLoggedInUser() {  }

    private void updateTextPartitionsAndAdapter() {
        int prevWordsPerChunk = m_nWordsPerChunk;
        updateWordsPerChunk();

        // check if spritz settings changed essentially
        if ( prevWordsPerChunk != m_nWordsPerChunk ) {
            resetPartitionsAndDoSpritzing();
        }
    }

    private void resetPartitionsAndDoSpritzing() {
        // make text partitions from the text and populate the list of partitions
        m_textPartitions = splitTextIntoParitions(getSpritzText(), getWordsPerChunk());
        mTextPartitionAdapter.updateData(m_textPartitions);
        mTextPartitionAdapter.setCurrentSelection(getCurTextPartitionIdx());
        // override spritz playback with the text partitions (sebsets of text). If the original
        // text was a url then spritz would have loaded that by now and the text is now available
        // in the partitions adapter
        doSpritzingTextPartitions(getCurTextPartitionIdx());
    }

    // Listens to spritz view and should receive notifications during spritzing events
// for the purpose of determining and capturing the current spritzed word which
// to TextToSpeech
    class TTS_SpritzViewListenerImpl implements SpritzViewListener {
        @Override
        public boolean onError(String s, Throwable throwable) {
            return false;
        }

        @Override
        public boolean onLoadStart() {
            return false;
        }

        @Override
        public boolean onLoadEnd() {
            return false;
        }

        @Override
        public boolean onLoadFail(String s, Throwable throwable) {
            return false;
        }

        @Override
        public void onStart(int i, int i2, float v, int i3) {

            // todo: check if specifically starting because of url
            if (!bDidInitTextPartitions) {
                // set to true before we update words per chunk and reset spritzing
                bDidInitTextPartitions = true;
                updateWordsPerChunk();
                resetPartitionsAndDoSpritzing();

                // just in case..
                int nTextPartitionListTop = mSpritzView.getBottom() + 1;
                mTextPartitionList.setTop(nTextPartitionListTop);
//                ShowAdView(); // show ads.. also below the list
            }
        }

        @Override
        public void onPause(int i, int i2, float v, int i3) {

        }

        @Override
        public void onResume(int i, int i2, float v, int i3) {

        }

        @Override
        public void onGoBackSentence(int i, int i2, float v, int i3, int i4) {

        }

        @Override
        public void onGoForwardSentence(int i, int i2, float v, int i3, int i4) {

        }

        @Override
        public void onSeek(int i, int i2, float v, int i3, int i4, SeekMode seekMode) {

        }

        @Override
        public void onSpritzComplete(int i, int i2, float v, int i3) {
            m_nCurTextPartitionIdx++;
            doSpritzingTextPartitions(m_nCurTextPartitionIdx);
        }

        @Override
        public void onReset(int i, int i2, float v, int i3) {

        }

        @Override
        public void onSpeedChange(int i, int i2, float v, int i3, int i4) {

        }
    };

    /* Callback for when the TTS is initialized */
    class TTS_OnInitListenerImpl implements TextToSpeech.OnInitListener
    {
        @Override
        public void onInit(int status) {
            m_bIsTTSEngineInit = (status == TextToSpeech.SUCCESS);
            if ( m_bUserDidTryTTSBeforeInit ) {
                m_bUserDidTryTTSBeforeInit = false; // now the variable is invalid anyways
                if ( m_bIsTTSEngineInit ) {
                    Toast.makeText(getActivity(),
                            "Text to Speech is Ready!",
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText( getActivity(),
                            "Text to Speech is Borked.. :(",
                            Toast.LENGTH_SHORT).show();
                }
            }

            // debugging
            if ( !m_bIsTTSEngineInit ) {
                Log.d(TAG, "Failed to init TTS engine..");
            } else {
                Log.d(TAG, "Text to Speech Initialized");
            }
            TTSPostInit();
        } // end on init
    };
}
