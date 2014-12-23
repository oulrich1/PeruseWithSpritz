package com.oriahulrich.perusalwithspritz.adapters;

import android.content.Context;

// http://stackoverflow.com/questions/16874826/how-to-add-
// icons-adjacent-to-titles-for-android-navigation-drawer

import java.util.ArrayList;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.oriahulrich.perusalwithspritz.pojos.NavDrawerItem;
import com.oriahulrich.perusalwithspritz.R;

public class NavigationDrawerAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<NavDrawerItem> navDrawerItems;

    public NavigationDrawerAdapter(Context context, ArrayList<NavDrawerItem> navDrawerItems){
        this.context = context;
        this.navDrawerItems = navDrawerItems;
    }

    @Override
    public int getCount() {
        return navDrawerItems.size();
    }

    @Override
    public Object getItem(int position) {
        return navDrawerItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater mInflater = (LayoutInflater)
                    context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            convertView = mInflater.inflate(R.layout.list_item_drawer, null);
        }

        ImageView imgIcon = (ImageView) convertView.findViewById(R.id.list_item_drawer_icon);
        TextView txtTitle = (TextView) convertView.findViewById(R.id.list_item_drawer_text1);

        imgIcon.setImageResource(navDrawerItems.get(position).getIcon());
        txtTitle.setText(navDrawerItems.get(position).getTitle());

        return convertView;
    }

}
