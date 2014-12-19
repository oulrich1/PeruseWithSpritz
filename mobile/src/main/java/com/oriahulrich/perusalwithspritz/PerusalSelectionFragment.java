package com.oriahulrich.perusalwithspritz;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

/**
 * Created by oriahulrich on 12/14/14.
 */

 /**
 * A fragment for letting the user select the text from the view
 * which they would like to spritz. They just need to click on any
 * html elements which seemlessly adds that html block to a queue
 * which will be spritzed when they hit the main "play" button
 */
public class PerusalSelectionFragment extends Fragment {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";
     private static final String ARG_URL = "arg_url";

    static private String TAG = "Text Selection Fragment";


     // The URL from which the WebView will render.
     private String mURL;


    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */

    public static PerusalSelectionFragment newInstance(int sectionNumber, String url) {
        Log.d(TAG, "FRAGMENT newInstance");
        PerusalSelectionFragment fragment = new PerusalSelectionFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        args.putString(ARG_URL, url);
        fragment.setArguments(args);
        return fragment;
    }

    public PerusalSelectionFragment() {
        mURL = "";
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.d(TAG, "FRAGMENT onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_web_view, container, false);
        setHasOptionsMenu(true);

        WebView webView = (WebView) rootView.findViewById(R.id.webViewPerusal);
        webView.loadUrl( getArguments().getString(ARG_URL) );

        return rootView;
    }

     @Override
     public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
         super.onCreateOptionsMenu(menu, inflater);
         Log.d(TAG, "onCreateOptionsMenu");
         inflater.inflate(R.menu.edit_text_and_selection, menu);
     }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(TAG, "FRAGMENT onAttach");
        ((MainActivity) activity).onSectionAttached(
                getArguments().getInt(ARG_SECTION_NUMBER));
    }
}

