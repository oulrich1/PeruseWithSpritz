package com.oriahulrich.perusalwithspritz.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.oriahulrich.perusalwithspritz.R;
import com.oriahulrich.perusalwithspritz.pojos.Perusal;

import java.util.ArrayList;

/**
 * Created by oriahulrich on 12/18/14.
 */
public class RecentPerusalsAdapter extends ArrayAdapter<Perusal> {

    private static final String TAG = "***INGREDIENT ADAPTER***: ";
    private ArrayList<Perusal> recentPerusalArrayList; // used for the view
    private ArrayList<Perusal> recentPerusalArrayListView;  // used for the data
    private StringBuffer recentPerusalTitles;

    public StringBuffer getRecentPerusalTitles() {
        return recentPerusalTitles;
    }

    public RecentPerusalsAdapter( Context context, int resource,
                                  ArrayList<Perusal> recentPerusalArrayListView)
    {
        super(context, resource, recentPerusalArrayListView);
        this.recentPerusalArrayListView = recentPerusalArrayListView;
        recentPerusalArrayList = new ArrayList<Perusal>(recentPerusalArrayListView); // original data
        recentPerusalTitles = new StringBuffer();
    }

    /**
     * ViewHolder: caches our TextView and CheckBox
     */
    static class ViewHolderItem {
        TextView recentPerusalTitle;
    }

    @Override
    public void add(Perusal ingredient) {
        /* all ingredients */
        recentPerusalArrayList.add(ingredient);

        recentPerusalArrayListView.clear();
        for (Perusal i : recentPerusalArrayList) {
            recentPerusalArrayListView.add(i);
            Log.d(TAG, " - - - - - " + i.getTitle() + ", ");
        }
    }

    @Override
    public void remove(Perusal object) {
        super.remove(object);
        recentPerusalArrayList.remove(object);
    }

    // removes all items from the adapter
    public void removeAll() {
        super.clear();
        recentPerusalArrayList.clear();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        final ViewHolderItem viewHolder;

        if (convertView == null) {
            // inflate the layout
            LayoutInflater inflater
                    = (LayoutInflater) getContext()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            convertView = inflater.inflate(R.layout.list_item_recent, null);

            // set up the ViewHolder
            viewHolder = new ViewHolderItem();
            viewHolder.recentPerusalTitle
                    = (TextView) convertView.findViewById(R.id.recentPerusalItemTitle);

            // store the holder with the view
            convertView.setTag(viewHolder);
        } else {
            // we've just avoided calling findViewById() on the resource file
            // every time just use the viewHolder
            viewHolder = (ViewHolderItem) convertView.getTag();
        }

        // set the indicator based on the perusal "speed" state
        TextView perusalStateIndicator
                = (TextView) convertView
                    .findViewById(R.id.recentPerusalItemStateIndicator);

        Perusal perusal = recentPerusalArrayListView.get(position);

        switch (perusal.getSpeedState()) {
            case SLOW:
                perusalStateIndicator.setBackgroundColor(
                        getContext().getResources().getColor(R.color.green_weak));
                break;
            case MODERATE:
                perusalStateIndicator.setBackgroundColor(
                        getContext().getResources().getColor(R.color.green_med));
                break;
            case FAST:
                perusalStateIndicator.setBackgroundColor(
                        getContext().getResources().getColor(R.color.green_strong));
                break;
            case VERY_FAST:
            default:
                perusalStateIndicator.setBackgroundColor(
                        getContext().getResources().getColor(R.color.blue_weak));
                break;
        }

        // finally assign the perusal values to the view through the "View holder"
        if (perusal != null) {
            viewHolder.recentPerusalTitle.setText(perusal.getTitle());
        }

        return convertView;
    }

//    public void changeIngredientSelectedState(View view, int position){
//        CheckBox checkBox = (CheckBox) view.findViewById(R.id.ingredientCheckbox);
//        Ingredient ingredient = ingredientArrayListView.get(position);
//        ingredient.setSelected(checkBox.isChecked());
//    }

//    public void itemClickListener(View view, int position) {
//
//        Log.d(TAG, "position: " + position + "<- itemClickListener");
//        Log.d(TAG, "itemClickListener in the adapter!");
//
//        // Manually check the checkbox and select the ingredient
//        CheckBox checkBox = (CheckBox) view.findViewById(R.id.ingredientCheckbox);
//        checkBox.toggle();
//
//        Ingredient ingredient = ingredientArrayListView.get(position);
//        ingredient.setSelected(checkBox.isChecked());
//
//        Log.d(TAG, "INGERDIENT SELECTED: " + ingredient.getIngredientTitle());
//
//        // set the list of ingredient titles that we want in the url */
//        ingredientTitles.delete(0, ingredientTitles.length());
//
//        for (Ingredient i : ingredientArrayList) {
//            if (i.isSelected()) {
//                String ingredientTitlePrioritized =  i.getIngredientTitle();
//                switch (i.getSelectedState()){
//                    case EXCLUDE_STATE:
//                        ingredientTitlePrioritized = "-" + ingredientTitlePrioritized;
//                        break;
//                    case REQUIRED_STATE:
//                        ingredientTitlePrioritized = "+" + ingredientTitlePrioritized;
//                        break;
//                }
//
//                ingredientTitlePrioritized = ingredientTitlePrioritized.trim().replace(" ", "+");
//
//                if (ingredientTitles.length() == 0) {
//                    ingredientTitles.append(ingredientTitlePrioritized);
//                } else if (ingredientTitles.length() > 0) {
//                    ingredientTitles.append("," + ingredientTitlePrioritized);
//                }
//            }
//        }
//        Log.d(TAG, "ingredient titles in Checkbox: = " + ingredientTitles);
//    }

//    @Override
//    public Filter getFilter() {
//        return new Filter() {
//            @Override
//            protected FilterResults performFiltering(CharSequence constraint) {
//                Log.d(TAG, "performFiltering");
//                constraint = constraint.toString().toLowerCase();
//                FilterResults results = new FilterResults();
//
//                if (constraint.toString().length() > 0) {
//                    for (Ingredient anIngredient : ingredientArrayList) {
//                        Log.d(TAG, " Original Data.. " + anIngredient.getIngredientTitle() + "... ");
//                    }
//
//                    ArrayList<Ingredient> found = new ArrayList<Ingredient>();
//                    for (Ingredient ingredient : ingredientArrayList) {
//                        if (ingredient.getIngredientTitle().toLowerCase().contains(constraint)) {
//                            found.add(ingredient);
//                            Log.d(TAG, " View List we want " + ingredient.getIngredientTitle() + ", ");
//                        }
//                    }
//                    results.values = new ArrayList<Ingredient>(found);
//                    results.count = found.size();
//                } else {
//                    results.values = new ArrayList<Ingredient>(ingredientArrayList);
//                    results.count = ingredientArrayList.size();
//                }
//                return results;
//            }
//
//            @Override
//            protected void publishResults(CharSequence constraint, FilterResults results) {
//                Log.d(TAG, "publishResults");
//                ingredientArrayListView.clear();
//                try {
//                    for (Ingredient ingredient : (ArrayList<Ingredient>) results.values) {
//                        Log.d(TAG, " + + + + + " + ingredient.getIngredientTitle() + ", ");
//                        ingredientArrayListView.add(ingredient);
//                    }
//                } catch (NullPointerException e) {
//                    Log.d(TAG, "HEY " + e.getMessage());
//                }
//                notifyDataSetChanged();
//            }
//        };
//    }

}
