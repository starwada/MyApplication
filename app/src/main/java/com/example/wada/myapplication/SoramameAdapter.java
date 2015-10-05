package com.example.wada.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.net.ContentHandler;
import java.util.List;

class ViewHolder
{
//    ImageView image;
    TextView date;
    TextView hour;
    TextView value;
}

/**
 * Created by Wada on 2015/07/07.
 */
public class SoramameAdapter extends ArrayAdapter<Soramame.SoramameData> {
    private LayoutInflater mInflater;
    private int mMode;      // 表示データモード 0 PM2.5/1 OX/2 風速（WS）

    public SoramameAdapter(Context context, List<Soramame.SoramameData> objects){
        super(context, 0, objects);
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mMode = 0;
    }

    // 表示モード設定
    public void setMode(int mode){
        mMode = mode;
    }

    public View getView(int position, View convertView, ViewGroup parent)
    {
        ViewHolder holder ;
        if(convertView == null )
        {
            convertView = mInflater.inflate(R.layout.layout, parent, false);
            holder = new ViewHolder();
//            holder.image = (ImageView)convertView.findViewById(R.id.image);
            holder.date = (TextView)convertView.findViewById(R.id.date);
            holder.hour = (TextView)convertView.findViewById(R.id.hour);
            holder.value = (TextView)convertView.findViewById(R.id.value);
            convertView.setTag(holder);
        }
        else
        {
            holder = (ViewHolder)convertView.getTag();
        }

        Soramame.SoramameData data = getItem(position);
        holder.date.setText(data.getDateString());
        holder.hour.setText(data.getHourString());
        switch(mMode){
            case 0:
                holder.value.setText(data.getPM25String());
                break;
            case 1:
                holder.value.setText(data.getOXString());
                break;
            case 2:
                holder.value.setText(data.getWSString());
                break;
        }

        return convertView;
    }
}
