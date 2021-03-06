package com.oriahulrich.perusalwithspritz;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ListFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Parcelable;
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
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.oriahulrich.perusalwithspritz.adapters.RecentPerusalsAdapter;
import com.oriahulrich.perusalwithspritz.database.SQLiteDAO;
import com.oriahulrich.perusalwithspritz.pojos.Perusal;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by oriahulrich on 12/18/14.
 */
public class RecentPerusalsFragment extends ListFragment {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final String ARG_TEXT = "arg_text";

    private String mText;           // text, which could be initialized with share via feature
    private EditText mEditText;     // the editable text view which will update mText, when necessary

    private SQLiteDAO sqLiteDAO;
    private RecentPerusalsAdapter recentPerusalsAdapter;

    private AdView mAdView;

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */

    static private String TAG = "Recents Fragment";

    public static RecentPerusalsFragment newInstance(int sectionNumber) {
        Log.d(TAG, " newInstance");
        RecentPerusalsFragment fragment = new RecentPerusalsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public RecentPerusalsFragment() {
        mText = "";
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

        Log.d(TAG, " onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_recent_perusals, container, false);

        // enabled getting notified when action bar item is clicked
        setHasOptionsMenu(true);

        mAdView = (AdView) rootView.findViewById(R.id.adViewRecentPerusals);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.d(TAG, "onActivityCreated Start");
        super.onActivityCreated(savedInstanceState);
        final ArrayList<Perusal> recentPerusalsArrayList = sqLiteDAO.getAllPerusals();

        Log.d(TAG, " onActivityCreated End " + recentPerusalsArrayList.size());

        final ListView listView = getListView();
        recentPerusalsAdapter = new RecentPerusalsAdapter( getActivity(),
                android.R.layout.simple_list_item_multiple_choice,
                recentPerusalsArrayList );
        listView.setAdapter(recentPerusalsAdapter);
        setUpListViewItemClickHandler(listView);
        registerForContextMenu(listView);
    }

    private void setUpListViewItemClickHandler( ListView listView ) {
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id)
            {
                Log.d(TAG, " listview.onItemClick");
//                Toast.makeText(getActivity(), "About to Spritz!",
//                        Toast.LENGTH_SHORT).show();
                // recentPerusalsAdapter.itemClickListener(view, position);
                Perusal perusal = recentPerusalsAdapter.getItem(position);
                Fragment fragment = PerusalSpritzFragment.newInstance(position + 1, perusal);

                // update the main content by replacing fragments
                FragmentManager fragmentManager = getFragmentManager();
                fragmentManager.beginTransaction()
                        .replace(R.id.container, fragment)
                        .commit();
            }
        });
    }


    /* created when we long hold a specific item in the 'recents' list */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.recent_perusals_fragment_context_menu, menu);
    }

    /* When an item is selected in the context menu */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo itemInfo
                = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        if (!getUserVisibleHint()) {
            return false;
        }

        Perusal recentPerusal = recentPerusalsAdapter.getItem(itemInfo.position);
        switch (item.getItemId()) {
            case R.id.actionSharePerusal:
                try {
                    shareTextPerusal(itemInfo);
                } catch (NullPointerException e) {
                    Log.d(TAG, "action.sharePerusal " + e.toString());
                }
                return true;
            case R.id.actionRemovePerusal:
                return removePerusal( recentPerusal );
            case R.id.actionRenameTitle:
                return showRenamePerusalDialog(recentPerusal,itemInfo.position);
            default:
                return super.onContextItemSelected(item);
        }
    }


    private boolean showRenamePerusalDialog( Perusal perusal, int position ) {
        /* open a dialog that asks the user for a new name for this perusal */
//        Toast.makeText( getActivity(),
//                "Not implemented yet!",
//                Toast.LENGTH_SHORT).show();

        final Perusal perusalFinal = perusal;
        final String oldTitle = perusal.getTitle();
        final int pos = position;

        /* show dialog - get new title */
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View  editPerusalTitleContentView = View.inflate(getActivity(),
                R.layout.edit_perusal_content_view, null);

        final EditText perusalTitleInputEditText =
                (EditText) editPerusalTitleContentView.findViewById(R.id.editPerusalTitleEditText);
        perusalTitleInputEditText.setText(perusal.getTitle());
        perusalTitleInputEditText.setSelection(perusalTitleInputEditText.getText().length());

        builder.setTitle("Rename Perusal Title");
        builder.setView(editPerusalTitleContentView);
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                // update the perusal title to the new one
                String newTitle = perusalTitleInputEditText.getText().toString();
                sqLiteDAO.updatePerusalTitle(oldTitle, newTitle);
                recentPerusalsAdapter.getItem(pos)
                        .setTitle(perusalTitleInputEditText.getText().toString());
                recentPerusalsAdapter.notifyDataSetChanged();
                // recentPerusalsAdapter.itemClickListener(v, pos);
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

    private boolean removePerusal( Perusal perusal ) {
        boolean isSuccess = true;
        try {
            if (perusal == null) {
                Toast.makeText(getActivity(),
                        "Tried to delete a non existent item!",
                        Toast.LENGTH_SHORT).show();
            } else {
                recentPerusalsAdapter.remove(perusal);
                sqLiteDAO.deletePerusal(perusal);
                recentPerusalsAdapter.notifyDataSetChanged();
                Toast.makeText(getActivity(),
                        "Removed '" + perusal.getTitle() + "'!  ",
                        Toast.LENGTH_SHORT).show();
            }
        } catch (NullPointerException e) {
            Log.d(TAG, e.toString());
            isSuccess = false;
        }
        return isSuccess;
    }

    private boolean removeAllPerusals() {
        boolean isSuccess = true;
        try {
            recentPerusalsAdapter.removeAll();
            sqLiteDAO.deleteAllPerusals();
            recentPerusalsAdapter.notifyDataSetChanged();
            Toast.makeText(getActivity(),
                    "Deleted all recent perusals",
                    Toast.LENGTH_SHORT).show();
        } catch (NullPointerException e) {
            Log.d(TAG, e.toString());
            isSuccess = false;
        }
        return isSuccess;
    }

    private void shareTextPerusal(AdapterView.AdapterContextMenuInfo itemInfo) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

        Perusal perusal = recentPerusalsAdapter.getItem(itemInfo.position);

        String textMessage = perusal.getText();
        String subjectMessage = "Perusal: " + perusal.getTitle() + "!";

        try {
            List<ResolveInfo> resolveInfoList = getActivity().getPackageManager()
                    .queryIntentActivities(shareIntent, 0);

            if (!resolveInfoList.isEmpty()) {
                List<Intent> targetedShareIntents = new ArrayList<Intent>();
                Intent targetedShareIntent;

                for (ResolveInfo resolveInfo : resolveInfoList) {
                    String packageName = resolveInfo.activityInfo.packageName;

                    targetedShareIntent = new Intent(Intent.ACTION_SEND);
                    targetedShareIntent.setType("text/plain");
                    targetedShareIntent.putExtra(Intent.EXTRA_SUBJECT, subjectMessage);
                    targetedShareIntent.putExtra(Intent.EXTRA_TEXT, textMessage);
                    targetedShareIntent.setPackage(packageName);

                    targetedShareIntents.add(targetedShareIntent);
                }

                Intent chooserIntent = Intent.createChooser(targetedShareIntents.remove(0),
                        getResources().getString(R.string.share_intent));
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS,
                        targetedShareIntents.toArray(new Parcelable[] {}));
                startActivityForResult(chooserIntent, 0);
            }
        } catch (NullPointerException e) {
            Log.d(TAG, "Exception: " + e.getMessage());
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        Log.d(TAG, " onCreateOptionsMenu");
        inflater.inflate(R.menu.recent_perusals_action_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "FRAGMENT onOptionsItemSelected");
        int id = item.getItemId();
        if (id == R.id.action_delete_all_recent_perusals) {
            removeAllPerusals(); // remove all recent perusals..
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



