package com.oriahulrich.perusalwithspritz;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.renderscript.Element;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.oriahulrich.perusalwithspritz.pojos.Perusal;

/** Jsoup is awesome! */
// http://jsoup.org/
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/** epublib references: */
// http://www.siegmann.nl/epublib/faq
// https://github.com/psiegman/epublib/blob/master/epublib-
// tools/src/test/java/nl/siegmann/epublib/search/SearchIndexTest.java
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

    String   mBookTextHTML;

    Button   mPrevPageButton;
    Button   mNextPageButton;


    // Helper class for holding the text in a page
    // and it's starting and stopping positions
    // relative to the book's spline's resource
    private class Page {
        // Page start in resource coordinates
        public int startCharIdx;
        public int startResId;

        // Page end in resource coordinates
        public int stopCharIdx;
        public int stopResId;

        public String text;
    }

    // list of pages fom which we can move about and extract the text at runtime
    ArrayList<Page> mPages;

    // keeps track of which "page" we are viewing
    int mCurPageIdx;

    /**
     * For getting the content of the book, it takes a while
     * appearantly.. Purpose for thread: would like to complete
     * drawing the view. This thread is joined onResume.. if on
     * resume is called then it can be safely assumed that the
     * book and data has already been initialized
     * */
    Thread mBookProcessingThread = new Thread() {
        @Override
        public void run() {
            try {

                // get only a few lines of the book
//                int maxLines = 20;
//                mBookTextHTML = getBookChapterHTML(mBook, maxLines);
//                int chapterLength = mBookTextHTML.length();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };


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
        mPages = new ArrayList<Page>();
    }


    View.OnClickListener onClickPrevPage = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if ( mCurPageIdx == 0 ) {
                return;
            }
            // get the previous page, just update idx (prev pages are stored)
            mCurPageIdx--;
            updateWebView();
        }
    };

    View.OnClickListener onClickNextPage = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if ( (mCurPageIdx+1) >= mPages.size() ) {

                // get the next page
                Page curPage = mPages.get(mCurPageIdx);
                Page offset = new Page();
                offset.startResId = curPage.stopResId;
                offset.startCharIdx = curPage.stopCharIdx;
                Page newPage = getNextPage(mBook, 20, offset);

                boolean isEmptyPage = (newPage.startCharIdx == newPage.stopCharIdx)
                                      && (newPage.startResId == newPage.stopResId);
                isEmptyPage = isEmptyPage || newPage.text.isEmpty();

                if ( !isEmptyPage ) {
                    // add the page and update, only if valid new page
                    mPages.add( newPage );
                    mCurPageIdx++;
                }
            } else {
                // update the current idx..
                mCurPageIdx++;
            }

            updateWebView();
        }
    };


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.d(TAG, "FRAGMENT onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_epub_view,
                            container, false);

        setHasOptionsMenu(true);

        // set up the views and click handlers for the view
        mWebView = (WebView) rootView.findViewById(R.id.epubWebView);
        mPrevPageButton = (Button) rootView.findViewById(R.id.prevPageButton);
        mNextPageButton = (Button) rootView.findViewById(R.id.nextPageButton);
        mPrevPageButton.setOnClickListener(onClickPrevPage);
        mNextPageButton.setOnClickListener(onClickNextPage);

        // get the intent arguments
        mBook = (Book) getArguments().getSerializable(ARG_BOOK);

        // perform the book processing (to get the first
        // first pages) right away if possible
        if ( mBook != null ) {
//            mBookProcessingThread.start();

            // initialize the first page offset to the
            // first "bit of words" in the text. bit of words
            // is arbitrary and is set to some "number of lines"
            // worth of words.. for now..

            Page bookPage = getFirstPage( mBook, 20 );
            mPages.clear();
            mPages.add(bookPage);
            mCurPageIdx = 0;

            updateWebView();
        }

        return rootView;
    }


    @Override
    public void onResume() {
        super.onResume();

        // before we update the web view with
        // the text we need to know for sure
        // that the processing thread is finish
//        try {
//            mBookProcessingThread.join();
//        } catch (Exception e) {
//            Log.d(TAG, "Thread join failed: " + e.getMessage());
//        }
    }

    // returns the text associated to the current page
    private String getCurrentText() {
        if ( mCurPageIdx < 0 || mCurPageIdx >= mPages.size() )
            return ""; // out of range( this should NEVER happen )
        return mPages.get(mCurPageIdx).text;
    }

    private void updateWebView() {
        // update the view with the first page
        if ( mBook != null
             && mWebView != null
             && mPages != null
             && (mPages.size() > mCurPageIdx) )
        {
            // load ONE page, current page into the web view
            mWebView.loadData( getCurrentText(), "text/html", "utf-8" );
        }
    }

    // called at least once
    private Page getFirstPage( Book book, int maxLines ) {
        Page offset = new Page();
        offset.startCharIdx = 0;
        offset.startResId = 0;

        return getNextPage(book, maxLines, offset);
    }

    private Page getNextPage( Book book, int maxLines, Page offset ) {
        // mText = book.getTableOfContents().getTocReferences().toString();
//        Resources resources = book.getResources();
//        Map<String, Resource> resourceMap = resources.getResourceMap();

        // the spine might not contain all resources..
        Spine spine = book.getSpine();
        List<SpineReference> spineList = spine.getSpineReferences();
        int count = spineList.size();
        int lineCount = 0;

        // maybe something like this? or.. the internal map at least?
//        Collection<Resource> resources = book.getResources().getAll();

        // temporary buffer when reading from the reader
        String line;

        // the book text is build and stored here, then returned as String
        StringBuilder strBuilder = new StringBuilder();

        int curResCharOffset = 0;
        int i;  // we need to know which res the reader stopped
        for ( i = offset.startResId;
              i < count && lineCount < maxLines;
              i++ )
        {
            Resource res = spine.getResource(i);
            InputStream is = null;
            BufferedReader reader = null;

            // get the stream
            try {
                is = res.getInputStream();
                reader = new BufferedReader( new InputStreamReader(is) );
            } catch (IOException e) {
                e.printStackTrace();
            }

            if ( is == null || reader == null ) {
                continue;
            }

            // get the text from the stream
            try {

                // if first res, then skip num chars implied by offset
                if ( i == offset.startResId ) {
                    reader.skip( (long) offset.startCharIdx );
                }

                curResCharOffset = 0; // keep track of offset in cur res
                // read from the buffered reader the lines from the book
                // stop when we have read enough lines that constitutes
                // a "page"
                while ( (line = reader.readLine()) != null
                         && lineCount < maxLines )
                {
                    curResCharOffset += line.length();
                    strBuilder.append(line).append("\n");

                    lineCount += 1;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Build a new "Page" for keeping track of the pages
        // read so far and where to start reading next pages

        Page page = new Page();
        page.text = strBuilder.toString();

        // iteration started where the offset told it to start
        page.startResId = offset.startResId;
        page.startCharIdx = offset.startCharIdx;

        // iteration stopped at (i-1)-th spine resource and num chars
        // (assuming the loop is never broken out of before i is updated..
        page.stopResId = i-1;
        if ( page.stopResId == offset.startResId ) {
            // then same page.. get the total chars recieved
            page.stopCharIdx = page.text.length() + page.startCharIdx;
        } else {
            // different page.. just get the total length of chars from that res
            page.stopCharIdx = curResCharOffset;
        }

        return page;
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        Log.d(TAG, "onCreateOptionsMenu");
        inflater.inflate(R.menu.epub_settings, menu);
        inflater.inflate(R.menu.edit_text_and_selection, menu);
    }


    // using jsoup to parse the current page's text, which
    // is actually HTML. The parse returns the text content
    // of the selector given.. Then, with the text, navigates
    // to the spritzing fragment.. Spritzes on Text
    private void parseTextFromHtmlAndDoSpritzing(String selector) {

        String curTextHtml = getCurrentText();

        if ( curTextHtml.isEmpty() ) {
            Toast.makeText( getActivity(),
                    "There is no text..",
                    Toast.LENGTH_LONG ).show();
            return;
        }

        // arguments to the spritz fragment
        int textState = Perusal.Mode.TEXT.ordinal();
        String text = "";

        // parse the book, "quickleee"
        // String chapter = getBookContentXmlString( mBook, -1 );
        Document htmlDoc = Jsoup.parse(curTextHtml);
        Elements els = htmlDoc.select(selector);
        for (org.jsoup.nodes.Element el : els) {

            // only use inner text if it is a leaf paragraph
            // node (reason: assumed to be the only type that
            // contains book text) (Update: this assumption
            // does not hold)
            // if ( el.children().size() == 0 )

            if ( el.hasText() )
            {
                text += el.text();
                // just in case each line does not
                // have a new line or seperator
                text += " ";
            }
        }

        ((MainActivity)getActivity())
                .navigateToSpritzFragment(textState, text);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "FRAGMENT onOptionsItemSelected");
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_perform_spritz_on_text) {
            // select text from paragraph tags
            parseTextFromHtmlAndDoSpritzing("p");
            return true;
        } else if (id == R.id.action_epub_settings) {
            String msg = "Not implemented yet. TODO: give the user ability to: " +
                         "(1) change words per group, (2) fontsize, (3) html " +
                         "selector tags.";
            Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();
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
