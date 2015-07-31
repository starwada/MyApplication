package com.example.wada.myapplication;

import android.app.ActionBar;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class DisplayMessageActivity extends ListActivity {
    private Soramame mSoradata;
    private SoramameAdapter mAdapter;
    ArrayList<Soramame.SoramameData> mList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_message);

        if(mList != null){ mList.clear(); }
        Intent intent = getIntent() ;
        String url;
        url = intent.getData().toString();
        mSoradata = intent.getParcelableExtra("mine");
        new SoraDesc().execute(url);

        TextView tview = (TextView)findViewById(R.id.MstName);
        tview.setText(mSoradata.getStationInfo());

//        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
//            @Override
//            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
//                LunarBaseGraphView graph = (LunarBaseGraphView) findViewById(R.id.soragraph);
//                graph.setPos(position);
//                return false;
//            }
//        });

        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                LunarBaseGraphView graph = (LunarBaseGraphView) findViewById(R.id.soragraph);
                graph.setPos(position);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_display_message, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mList != null){ mList.clear(); }
    }

    private class SoraDesc extends AsyncTask<String, Void, Void>
    {
        ProgressDialog mProgressDialog;
        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
            mSoradata.clearData();
            mProgressDialog = new ProgressDialog(DisplayMessageActivity.this);
            mProgressDialog.setTitle( "そらまめ PM2.5データ取得");
            mProgressDialog.setMessage("Loading...");
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.show();
        }

        @Override
        protected Void doInBackground(String... urls)
        {
            try
            {
                Document sora = Jsoup.connect(urls[0]).get();
                Element body = sora.body();
                Elements tables = body.getElementsByAttributeValue("align", "right");

                for( Element ta : tables)
                {
                    Elements data = ta.getElementsByTag("td");
                    mSoradata.setData(data.get(0).text(), data.get(1).text(), data.get(2).text(), data.get(3).text(), data.get(14).text());
                }
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result)
        {
            mList = mSoradata.getData();
            if(mList != null)
            {
                mAdapter = new SoramameAdapter(DisplayMessageActivity.this, mList);
                setListAdapter(mAdapter);
            }
            LunarBaseGraphView view = (LunarBaseGraphView)findViewById(R.id.soragraph);
            view.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            view.setData(mSoradata);
            mSoradata = null;

            mProgressDialog.dismiss();
        }
    }
}
