package com.oriahulrich.perusalwithspritz;

import android.app.Activity;
import android.app.Fragment;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import java.util.Map;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.Resources;

/**
 *  The user should be able to share e-pubs with the app. When the epub is shared
 *  the app should NOT go directly to spritzing.. the user should be able to read
 *  the epub as they normally would read an epub on an ordinary epub app. They
 *  should be able to navigate to a page, read, place a cursor where they would like
 *  to start spritzing, then the user can press "play" to spritz everything after
 *  the location of the cursor. When the play is pressed. this fragment will get the
 *  all text up to, say, the next chapter and spritz it. When it is complete.. ?
 */
public class PerusalEpubFragment extends Fragment {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final String ARG_BOOK = "arg_book";

    static private String TAG = "Epub Controller Fragment";

    // the book from which to read
    Book mBook;

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */

    public static PerusalEpubFragment newInstance(int sectionNumber,
                                                  Book book,
                                                  MainActivity.InputMethodState inputMethod) {
        Log.d(TAG, "FRAGMENT newInstance");
        PerusalEpubFragment fragment = new PerusalEpubFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        args.putSerializable(ARG_BOOK, book);
        fragment.setArguments(args);
        return fragment;
    }

    public PerusalEpubFragment() {
        mBook = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.d(TAG, "FRAGMENT onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_epub_view, container, false);
        setHasOptionsMenu(true);

        mBook = (Book) getArguments().getSerializable(ARG_BOOK);

        // https://github.com/psiegman/epublib/blob/master/epublib-
        // tools/src/test/java/nl/siegmann/epublib/search/SearchIndexTest.java
        // mText = book.getTableOfContents().getTocReferences().toString();

        // resources contain all of the content of the book
        Resources resources = mBook.getResources();

        // the inner data structure that holds the content
        Map<String, Resource> resourceMap = resources.getResourceMap();

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
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
        Log.d(TAG, "onAttach");
        ((MainActivity) activity).onSectionAttached(
                getArguments().getInt(ARG_SECTION_NUMBER));
    }

}
