package org.jointheleague.hackmashers;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

public class ArrayAdapterFirstHighlighted extends ArrayAdapter<String> {


    public ArrayAdapterFirstHighlighted(Context context, int resource, String[] objects) {
        super(context, resource, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView != null && position == 0) {
            convertView.setBackgroundColor(Color.LTGRAY);
        }
        return super.getView(position, convertView, parent);
    }
}
