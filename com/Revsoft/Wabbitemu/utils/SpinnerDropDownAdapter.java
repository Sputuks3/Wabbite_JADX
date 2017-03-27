package com.Revsoft.Wabbitemu.utils;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import java.util.List;

public class SpinnerDropDownAdapter extends BaseAdapter implements SpinnerAdapter {
    private final Context mContext;
    private final List<String> mItems;

    public SpinnerDropDownAdapter(Context context, List<String> items) {
        this.mContext = context;
        this.mItems = items;
    }

    public int getCount() {
        return this.mItems.size();
    }

    public Object getItem(int position) {
        return this.mItems.get(position);
    }

    public long getItemId(int position) {
        return (long) ((String) this.mItems.get(position)).hashCode();
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        TextView text = new TextView(this.mContext);
        text.setTextColor(-1);
        text.setText((CharSequence) this.mItems.get(position));
        text.setPadding(20, 20, 20, 20);
        return text;
    }
}
