package com.oriahulrich.perusalwithspritz;

/**
 * Created by oriahulrich on 12/14/14.
 */

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.oriahulrich.perusalwithspritz.pojos.Perusal;

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
    private static final String ARG_TEXT = "arg_text";

    private String mText;           // text, which could be initialized with share via feature
    private EditText mEditText;     // the editable text view which will update mText, when necessary

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */

    static private String TAG = "Edit Text Fragment";

    public static PerusalEditTextFragment newInstance(int sectionNumber, String text) {
        Log.d(TAG, " newInstance");
        PerusalEditTextFragment fragment = new PerusalEditTextFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        args.putString(ARG_TEXT, text);
        fragment.setArguments(args);
        return fragment;
    }

    public PerusalEditTextFragment() {
        mText = "";
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.d(TAG, " onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_edit_text, container, false);
        setHasOptionsMenu(true);

        mText = getArguments().getString(ARG_TEXT);
        mEditText = (EditText) rootView.findViewById(R.id.editTextPerusal);

        if ( mText != null && !mText.isEmpty() ) {
            mEditText.setText(mText);
        }

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        Log.d(TAG, "onCreateOptionsMenu");
        inflater.inflate(R.menu.edit_text_and_selection, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "FRAGMENT onOptionsItemSelected");
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_example) {

            if (mEditText != null)
            {
                mText = mEditText.getText().toString();
            }

//            Toast.makeText(getActivity(), "Spritzing..", Toast.LENGTH_SHORT).show();
            int textState = Perusal.Mode.TEXT.ordinal();
            int peruseSectionNumber = 1;
            Fragment fragment = PerusalSpritzFragment
                    .newInstance(peruseSectionNumber, textState, mText, true);

            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit();

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(TAG, " onAttach");
        ((MainActivity) activity).onSectionAttached(
                getArguments().getInt(ARG_SECTION_NUMBER));
    }
}

