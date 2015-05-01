package com.oriahulrich.perusalwithspritz.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.oriahulrich.perusalwithspritz.R;
import com.oriahulrich.perusalwithspritz.lib.Helpers;

import java.util.ArrayList;


/**
 * Created by oriahulrich on 4/21/15.
 */
public class TextPartitionsAdapter extends BaseAdapter {

    Context mContext;
    LayoutInflater mInflater;
    private int nDeviceWidth;
    ArrayList<String> mTextPartitions;
    int m_nCurSelectedPartitionIdx;

    public TextPartitionsAdapter(Context context, LayoutInflater inflater) {
        mContext = context;
        mInflater = inflater;
        nDeviceWidth = Helpers.getDeviceWidth(mContext);
        m_nCurSelectedPartitionIdx = 0;
        mTextPartitions = new ArrayList<String>();
    }

    /// im guessing that convert view is the recycled view..
    /// that the list view expects the adapter to populate
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        // indstantiate the holder, which is a member of convertview
        if ( convertView == null ) {
            convertView = mInflater.inflate(R.layout.row_text_partition, null);
            holder = new ViewHolder();
            holder.indicator = (TextView) convertView.findViewById(R.id.current_spritz_partition_indicator);
            holder.title = (TextView) convertView.findViewById(R.id.text_title);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // set the view holder, to hold the references to the
        // view items, which is tagged by the convert view
        holder.indicator.setVisibility(View.INVISIBLE);
        holder.title.setText(mTextPartitions.get(position));

        if ( position == m_nCurSelectedPartitionIdx ) {
            holder.indicator.setVisibility(View.VISIBLE);
        }

        // since convert view listens in on the holder and it's items,
        // the convert view will get the updates to the view
        int topBottomExtraPx = 20;
        int indicatorHeight = Helpers.getItemHeight(holder.title, nDeviceWidth) + topBottomExtraPx;
        holder.indicator.setHeight(indicatorHeight);

        return convertView;
    }

    @Override
    public int getCount() {
        return mTextPartitions.size();
    }

    @Override
    public Object getItem(int position) {
        return mTextPartitions.get(position);
    }

    @Override
    public long getItemId(int position) {
        return (long) position;
    }

    public void setCurrentSelection(int position) {
        m_nCurSelectedPartitionIdx = position;
        notifyDataSetChanged();
    }

    private static class ViewHolder {
        public TextView  indicator;
        public TextView  title;
    }

    public void updateData(ArrayList<String> textPartitions) {
        mTextPartitions = textPartitions;
        notifyDataSetChanged();
    }
}
