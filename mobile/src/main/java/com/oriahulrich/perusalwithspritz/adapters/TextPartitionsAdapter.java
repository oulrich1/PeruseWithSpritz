package com.oriahulrich.perusalwithspritz.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.oriahulrich.perusalwithspritz.R;
import com.oriahulrich.perusalwithspritz.lib.Helpers;
import com.oriahulrich.perusalwithspritz.pojos.TextPartition;

import java.util.ArrayList;


/**
 * Created by oriahulrich on 4/21/15.
 */
public class TextPartitionsAdapter extends BaseAdapter {

    Context mContext;
    LayoutInflater mInflater;
    private int nDeviceWidth;
    ArrayList<TextPartition> mTextPartitions;
    int m_nCurSelectedPartitionIdx;

    public TextPartitionsAdapter(Context context, LayoutInflater inflater) {
        mContext = context;
        mInflater = inflater;
        nDeviceWidth = Helpers.getDeviceWidth(mContext);
        m_nCurSelectedPartitionIdx = 0;
        mTextPartitions = new ArrayList<TextPartition>();
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
        holder.title.setText(mTextPartitions.get(position).getText());

        if ( position == m_nCurSelectedPartitionIdx ) {
            holder.indicator.setVisibility(View.VISIBLE);
        }

        // since convert view listens in on the holder and it's items,
        // the convert view will get the updates to the view
        int topBottomExtraPx = 25;
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

    public void setItem(int position, TextPartition partition) {
        if(position < 0 || position > mTextPartitions.size()) return;
        mTextPartitions.set(position, partition);
        notifyDataSetChanged();
    }

    private void onRemoveItem() {
    }

    public void remove(TextPartition object) {
        remove(mTextPartitions.indexOf(object));
    }
    public void remove(int position) {
        mTextPartitions.remove(position);

        // true if the item to delete is the last item in the list
        boolean bIsRemoveCurSpritzLastItem =
                (m_nCurSelectedPartitionIdx == position) &&
                (position == mTextPartitions.size()-1);

        if(m_nCurSelectedPartitionIdx > position || bIsRemoveCurSpritzLastItem)  {
            m_nCurSelectedPartitionIdx -= 1;
        }
        // else don't decrement current spritz index if the item to delete is
        // the current spritzing item (Since we want the index to reference the
        // next item to read not the previous one) .. also definitely don't
        // decrement index if the item to delete is past the item current selected..

        notifyDataSetChanged();
    }

    // removes all items from the adapter
    public void removeAll() {
        mTextPartitions.clear();
        m_nCurSelectedPartitionIdx = -1;
        notifyDataSetChanged();
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

    public void updateData(ArrayList<TextPartition> textPartitions) {
        mTextPartitions = textPartitions;
        notifyDataSetChanged();
    }
}
