package com.example.wada.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Wada on 2015/07/08.
 */
public class SoramameStationAdapter extends ArrayAdapter<Soramame> {
    private LayoutInflater mInflater;

    public SoramameStationAdapter(Context context, List<Soramame> objects){
        super(context, 0, objects);
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public View getView(int position, View convertView, ViewGroup parent)
    {
        ViewHolder holder ;
        if(convertView == null )
        {
            convertView = mInflater.inflate(R.layout.stationlayout, parent, false);
            holder = new ViewHolder();
            holder.imagePM25 = (ImageView)convertView.findViewById(R.id.imagePM25);
            holder.imageOX = (ImageView)convertView.findViewById(R.id.imageOX);
            holder.imageWS = (ImageView)convertView.findViewById(R.id.imageWS);
            holder.date = (TextView)convertView.findViewById(R.id.name);
            holder.value = (TextView)convertView.findViewById(R.id.address);
            convertView.setTag(holder);
        }
        else
        {
            holder = (ViewHolder)convertView.getTag();
        }

        Soramame data = getItem(position);
        holder.date.setText(data.getMstName());
        holder.value.setText(data.getAddress());
        holder.imageOX.setImageResource(R.mipmap.ic_launcher_ox_off);
        if(data.getAllow(0)){
            holder.imageOX.setImageResource(R.mipmap.ic_launcher_ox_on);
        }
        holder.imagePM25.setImageResource(R.mipmap.ic_launcher_pm25_off);
        if(data.getAllow(1)){
            holder.imagePM25.setImageResource(R.mipmap.ic_launcher_pm25_on);
        }
        holder.imageWS.setImageResource(R.mipmap.ic_launcher_ws_off);
        if(data.getAllow(2)){
            holder.imageWS.setImageResource(R.mipmap.ic_launcher_ws_on);
        }
        return convertView;
    }
}
