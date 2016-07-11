package com.example.wada.myapplication;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.w3c.dom.Text;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public class Main3Activity extends AppCompatActivity {
    private Soramame mSoradata;
    private static final String SORADATEFILE = "SoraDateFile";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);
        Intent intent = getIntent() ;
        String url;
        url = intent.getData().toString();
        mSoradata = intent.getParcelableExtra("mine");
        new SoraDesc().execute(url);

        getPrefInfo();
    }

    private int getPrefInfo()
    {
        int rc = 0 ;
        try
        {
            FileInputStream infile = openFileInput(SORADATEFILE);
            int byteCount = infile.available();
            if(byteCount < 1 ){ infile.close(); return rc; }

            byte[] readBytes = new byte[byteCount];
            rc = infile.read(readBytes, 0, byteCount) ;
            String strBytes = new String(readBytes);
            infile.close();

            TextView datefile = (TextView)findViewById(R.id.textView);
            datefile.setText(strBytes);
//            String Pref[] = strBytes.split(",");
//            Collections.addAll(prefList, Pref);
        }
        catch (FileNotFoundException e)
        {
            rc = 1;
        }
        catch(IOException e)
        {
            rc = -1;
        }

        return rc;
    }

    private class SoraDesc extends AsyncTask<String, Void, Void>
    {
        ProgressDialog mProgressDialog;
        int count = 0;
        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
            mSoradata.clearData();
            mProgressDialog = new ProgressDialog(Main3Activity.this);
            mProgressDialog.setTitle( "そらまめ データ取得");
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
                    // 0 西暦/1 月/2 日/3 時間
                    // 4 SO2/5 NO/6 NO2/7 NOX/8 CO/9 OX/10 NMHC/11 CH4/12 THC/13 SPM/14 PM2.5/15 SP/16 WD/17 WS

                    mSoradata.setData(data.get(0).text(), data.get(1).text(), data.get(2).text(), data.get(3).text(),
                            data.get(9).text(), data.get(14).text(), data.get(16).text(), data.get(17).text());
                    count++;
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
            Bitmap graph = GraphFactory.drawGraph(mSoradata, 0);
            ImageView img = (ImageView)findViewById(R.id.graph);
            img.setImageBitmap(graph);

            mProgressDialog.dismiss();
        }
    }
}
