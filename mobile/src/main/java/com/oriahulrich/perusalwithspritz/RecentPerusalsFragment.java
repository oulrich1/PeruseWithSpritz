package com.oriahulrich.perusalwithspritz;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ListFragment;
import android.os.Bundle;
import android.util.Log;
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
//        listView.setTextFilterEnabled(true);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id)
            {
                Log.d(TAG, "onItemClick");
                Toast.makeText(getActivity(), "About to Spritz!",
                        Toast.LENGTH_SHORT).show();
//                recentPerusalsAdapter.itemClickListener(view, position);
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