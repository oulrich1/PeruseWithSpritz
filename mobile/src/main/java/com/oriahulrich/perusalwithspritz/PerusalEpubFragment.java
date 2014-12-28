package com.oriahulrich.perusalwithspritz;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.net.Uri;
import android.os.Bundle;
import android.renderscript.Element;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import com.oriahulrich.perusalwithspritz.pojos.Perusal;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.Resources;
import nl.siegmann.epublib.domain.Spine;
import nl.siegmann.epublib.domain.SpineReference;

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
    Book     mBook;
    WebView  mWebView;
    TextView mTextView;

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
        View rootView = inflater.inflate(R.layout.fragment_epub_view,
                            container, false);

        setHasOptionsMenu(true);

        mWebView = (WebView) rootView.findViewById(R.id.epubWebView);
//        mTextView = (TextView) rootView.findViewById(R.id.epubTextView);
        mBook = (Book) getArguments().getSerializable(ARG_BOOK);

        if ( mBook != null ) {
            onCreateEpubView(mBook);
        }

        return rootView;
    }

    private void onCreateEpubView( Book book ) {
        if ( book == null )
            return;

        // get the contents and append them to the views
        String chapter = getChapter(book, -1);
        int chapterLength = chapter.length();

        if ( mWebView != null ) {
            mWebView.loadData( chapter, "text/html", "utf-8" );
        }

//        if ( mTextView != null ) {
//            int count = book.getSpine().getSpineReferences().size();
//            mTextView.setText(Integer.toString(count));
//        }
    }

    private String getChapter( Book book, int chapterIdx ) {
        // https://github.com/psiegman/epublib/blob/master/epublib-
        // tools/src/test/java/nl/siegmann/epublib/search/SearchIndexTest.java
        // mText = book.getTableOfContents().getTocReferences().toString();
//        Resources resources = book.getResources();
//        Map<String, Resource> resourceMap = resources.getResourceMap();
        Spine spine = book.getSpine();
        List<SpineReference> spineList = spine.getSpineReferences() ;
        int count = spineList.size();

        String line;
        String allLines = "";

        StringBuilder strBuilder = new StringBuilder();
        for (int i = 0; count > i; i++) {
            Resource res = spine.getResource(i);
            try {
                InputStream is = res.getInputStream();
                BufferedReader reader = new BufferedReader(
                                            new InputStreamReader(is)
                                        );
                try {
                    while ((line = reader.readLine()) != null) {
                        allLines = strBuilder
                                    .append(line)
                                    .append("\n")
                                    .toString();
                    }
                } catch (IOException e) {e.printStackTrace();}

                //do something with stream
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return allLines;
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
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "FRAGMENT onOptionsItemSelected");
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_perform_spritz_on_text) {
            Toast.makeText( getActivity(),
                            "Reading all text..",
                            Toast.LENGTH_LONG ).show();

            // arguments to the spritz fragment
            int textState = Perusal.Mode.TEXT.ordinal();
            String text = "";

            // parse the book, "quickleee"
            String chapter = getChapter( mBook, -1 );
            Document htmlDoc = Jsoup.parse(chapter);
            Elements els = htmlDoc.select("p");
            for (org.jsoup.nodes.Element el : els) {
                // only use inner text if it is a leaf paragraph
                // node (reason: assumed to be the only type that
                // contains book text)
                if ( el.children().size() == 0
                      && el.hasText() )
                {
                    text += el.text();
                    // just in case each line does not
                    // have a new line or seperator
                    text += " ";
                }
            }

            ((MainActivity)getActivity())
                    .navigateToSpritzFragment(textState, text);

            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(TAG, "onAttach");
        ((MainActivity) activity).onSectionAttached(
                getArguments().getInt(ARG_SECTION_NUMBER));
    }

}
