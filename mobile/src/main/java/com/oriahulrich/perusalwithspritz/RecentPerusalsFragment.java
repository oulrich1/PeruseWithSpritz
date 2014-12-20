package com.oriahulrich.perusalwithspritz;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ListFragment;
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
//        sqLiteDAO = new SQLiteDAO(getActivity());
//        sqLiteDAO.open();
//        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.d(TAG, " onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_recent_perusals, container, false);

        // enabled getting notified when action bar item is clicked
        setHasOptionsMenu(true);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.d(TAG, "onActivityCreated Start");
        super.onActivityCreated(savedInstanceState);
        final ArrayList<Perusal> recentPerusalsArrayList = sqLiteDAO.getAllPerusals();

        Log.d(TAG, " onActivityCreated End " + recentPerusalsArrayList.size());

        final ListView listView = getListView();
        recentPerusalsAdapter = new RecentPerusalsAdapter(getActivity(),
                android.R.layout.simple_list_item_multiple_choice, recentPerusalsArrayList);
        listView.setAdapter(recentPerusalsAdapter);
        registerForContextMenu(listView);
        //        listView.setTextFilterEnabled(true);

        // set up the event handlers ( see implementation below
        setUpListViewItemClick( listView );
//        setUpListViewItemLongHold( listView );
    }

    // sets up the click event handler for the items in the listview
    private void setUpListViewItemClick( ListView listView ) {
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id)
            {
                Log.d(TAG, "onItemClick");
                Toast.makeText(getActivity(), "About to Spritz!",
                        Toast.LENGTH_SHORT).show();
                // recentPerusalsAdapter.itemClickListener(view, position);
                Perusal perusal = recentPerusalsAdapter.getItem(position);

                Fragment fragment = PerusalSpritzFragment
                        .newInstance( position + 1,
                                perusal.getMode().ordinal(),
                                perusal.getText(), false );

                // update the main content by replacing fragments
                FragmentManager fragmentManager = getFragmentManager();
                fragmentManager.beginTransaction()
                        .replace(R.id.container, fragment)
                        .commit();
            }
        });
    }

    // sets up the long hold event for the items in the listview
    private void setUpListViewItemLongHold( ListView listView ) {
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                                           int pos, long id) {
                // TODO Auto-generated method stub
//                Toast.makeText(getActivity(), "Long hold item!", Toast.LENGTH_LONG).show();

                return true;
            }
        });
    }

    /* created when we long hold a specific item in the recipe list */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.recent_perusals_fragment_context_menu, menu);
    }

    /* When an item is selected in the context menu */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo itemInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        if (!getUserVisibleHint())
            return false;

        Perusal recentPerusal = recentPerusalsAdapter.getItem(itemInfo.position);
        switch (item.getItemId()) {
            case R.id.actionShareRecipeFavorite:
                try {
                    shareTextPerusal(itemInfo);
                } catch (NullPointerException e) {
                    Log.d(TAG, e.toString());
                }
                return true;
            case R.id.actionRemoveFavoriteRecipeFavorite:
                try {
                    if (recentPerusal == null) {
                        Toast.makeText(getActivity(), "Tried to delete a non existant recipe, silly! ",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        recentPerusalsAdapter.remove(recentPerusal);
                        sqLiteDAO.deletePerusal(recentPerusal);
                        recentPerusalsAdapter.notifyDataSetChanged();
                        Toast.makeText(getActivity(),
                                "Removed '" + recentPerusal.getTitle() + "' from your favorites!  ",
                                Toast.LENGTH_SHORT).show();
                    }
                } catch (NullPointerException e) {
                    Log.d(TAG, e.toString());
                }
                return true;
            default:
                return super.onContextItemSelected(item);
        }
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

        /// Add Menu action items  here

//        inflater.inflate(R.menu.edit_text_and_selection, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "FRAGMENT onOptionsItemSelected");
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
//        if (id == R.id.action_example) {
//
//            if (mEditText != null)
//            {
//                mText = mEditText.getText().toString();
//            }
//
////            Toast.makeText(getActivity(), "Spritzing..", Toast.LENGTH_SHORT).show();
//            int textState = PerusalSpritzFragment.Mode.TEXT.ordinal();
//            Fragment fragment = PerusalSpritzFragment.newInstance(textState, mText);
//            FragmentManager fragmentManager = getFragmentManager();
//            fragmentManager.beginTransaction()
//                    .replace(R.id.container, fragment)
//                    .commit();

//            return true;
//        }
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




// add this to on activity created if we are going to implement
// something that uses long hold click:


/*
*
        // Edit the ingredient
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "onItemLongClick");

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                final Ingredient ingredient = (Ingredient) listView.getItemAtPosition(position);

                final String oldIngredientTitle;
                oldIngredientTitle = ingredient.getIngredientTitle();

                final int pos = position;
                final View v = view;

                View  editIngredientContentView = View.inflate(getActivity(),
                        R.layout.edit_ingredient_contentview, null);
                final EditText ingredientTitleInputEditText =
                        (EditText) editIngredientContentView.findViewById(R.id.editIngredientEditText);
                ingredientTitleInputEditText.setText(ingredient.getIngredientTitle());
                ingredientTitleInputEditText.setSelection(ingredientTitleInputEditText.getText().length());

                final CheckBox checkBoxExcludeIngredient = (CheckBox) editIngredientContentView.findViewById(R.id.checkboxExcludeIngredient);
                final CheckBox checkboxRequiredIngredient = (CheckBox) editIngredientContentView.findViewById(R.id.checkboxRequiredIngredient);

                switch (ingredient.getSelectedState()){
                    case EXCLUDE_STATE:
                        checkBoxExcludeIngredient.setChecked(true);
                        break;
                    case REQUIRED_STATE:
                        checkboxRequiredIngredient.setChecked(true);
                        break;
                }

                checkBoxExcludeIngredient.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if ( checkBoxExcludeIngredient.isChecked() ) {
                            checkboxRequiredIngredient.setChecked(false);
                            ingredient.setSelectedState(Ingredient.SelectedStateType.EXCLUDE_STATE);
                        } else {
                            ingredient.setSelectedState(Ingredient.SelectedStateType.NORMAL_STATE);
                        }
                    }
                });
                checkBoxExcludeIngredient.setText(" Exclude  ");

                checkboxRequiredIngredient.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if ( checkboxRequiredIngredient.isChecked() ) {
                            checkBoxExcludeIngredient.setChecked(false);
                            ingredient.setSelectedState(Ingredient.SelectedStateType.REQUIRED_STATE);
                        } else {
                            ingredient.setSelectedState(Ingredient.SelectedStateType.NORMAL_STATE);
                        }
                    }
                });
                checkboxRequiredIngredient.setText(" Require ");

                builder.setTitle("Edit Ingredient");
                builder.setView(editIngredientContentView);
                builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        boolean prevSelectedState;
                        CheckBox checkbox = (CheckBox) v.findViewById(R.id.ingredientCheckbox);
                        prevSelectedState = checkbox.isChecked();
                        checkbox.setChecked(false);
                        ingredientAdapter.changeIngredientSelectedState(v, pos);
                        sqLiteDAO.updateIngredientTitle(oldIngredientTitle, ingredientTitleInputEditText.getText().toString());
                        ingredientAdapter.getItem(pos).setIngredientTitle(ingredientTitleInputEditText.getText().toString());
                        ingredientAdapter.notifyDataSetChanged();

                        if (!prevSelectedState){
                            checkbox.toggle();
                        }
                        ingredientAdapter.itemClickListener(v, pos);
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
        });
* */