package com.oriahulrich.perusalwithspritz.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.oriahulrich.perusalwithspritz.R;
import com.oriahulrich.perusalwithspritz.lib.Helpers;
import com.oriahulrich.perusalwithspritz.pojos.Perusal;

import java.util.ArrayList;

/**
 * Created by oriahulrich on 12/18/14.
 */
public class RecentPerusalsAdapter extends ArrayAdapter<Perusal> {

    private static final String TAG = "***INGREDIENT ADAPTER ";
    private ArrayList<Perusal> recentPerusalArrayList; // used for the view
    private ArrayList<Perusal> recentPerusalArrayListView;  // used for the data
    private StringBuffer recentPerusalTitles;
    private int nDeviceWidth;

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
        nDeviceWidth = Helpers.getDeviceWidth(context);
    }

    /**
     * ViewHolder: caches our TextView and CheckBox
     */
    private static class ViewHolderItem {
        public TextView recentPerusalTitle;
        public TextView indicator;
    }

    @Override
    public void add(Perusal ingredient) {
        /* all ingredients */
        recentPerusalArrayList.add(ingredient);

        recentPerusalArrayListView.clear();
        for (Perusal i : recentPerusalArrayList) {
            recentPerusalArrayListView.add(i);
            Log.d(TAG, " " + i.getTitle() + ", ");
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
        ViewHolderItem viewHolder;

        if (convertView == null) {
            // inflate the layout
            LayoutInflater inflater
                    = (LayoutInflater) getContext()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            convertView = inflater.inflate(R.layout.list_item_recent, null);

            // set up the ViewHolder
            viewHolder = new ViewHolderItem();
            viewHolder.indicator = (TextView) convertView.findViewById(R.id.recentPerusalItemStateIndicator);
            viewHolder.recentPerusalTitle
                    = (TextView) convertView.findViewById(R.id.recentPerusalItemTitle);

            // store the holder with the view
            convertView.setTag(viewHolder);
        } else {
            // we've just avoided calling findViewById() on the resource file
            // every time just use the viewHolder
            viewHolder = (ViewHolderItem) convertView.getTag();
        }

        Perusal perusal = recentPerusalArrayListView.get(position);

        switch (perusal.getSpeedState()) {
            case SLOW:
                viewHolder.indicator.setBackgroundColor(
                        getContext().getResources().getColor(R.color.green_weak));
                break;
            case MODERATE:
                viewHolder.indicator.setBackgroundColor(
                        getContext().getResources().getColor(R.color.green_med));
                break;
            case FAST:
                viewHolder.indicator.setBackgroundColor(
                        getContext().getResources().getColor(R.color.green_strong));
                break;
            case VERY_FAST:
            default:
                viewHolder.indicator.setBackgroundColor(
                        getContext().getResources().getColor(R.color.blue_weak));
                break;
        }

        // finally assign the perusal values to the view through the "View holder"
        if (perusal != null) {
            viewHolder.recentPerusalTitle.setText(perusal.getTitle());
        }

        int topBottomExtraPx = 10;
        int indicatorHeight = Helpers.getItemHeight(viewHolder.recentPerusalTitle, nDeviceWidth) + topBottomExtraPx;
        viewHolder.indicator.setHeight(indicatorHeight);

        return convertView;
    }
}
