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
        new SoraDesc().execute(url);

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

    private class SoraDesc extends AsyncTask<String, Void, Void>
    {
        Soramame soradata;
        ProgressDialog mProgressDialog;
        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
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

                soradata = new Soramame(40103120, "", "");

                for( Element ta : tables)
                {
                    Elements data = ta.getElementsByTag("td");
                    soradata.setData(data.get(0).text(), data.get(1).text(), data.get(2).text(), data.get(3).text(), data.get(14).text());
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
            mList = soradata.getData();
            if(mList != null)
            {
                mAdapter = new SoramameAdapter(DisplayMessageActivity.this, mList);
                setListAdapter(mAdapter);
            }

            mProgressDialog.dismiss();
        }
    }
}
