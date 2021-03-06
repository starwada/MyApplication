package com.example.wada.myapplication;

import android.app.ActionBar;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileOutputStream;
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

        // グラフビューにタッチリスナー設定
        final LunarBaseGraphView graph = (LunarBaseGraphView) findViewById(R.id.soragraph);
        graph.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getActionMasked() == MotionEvent.ACTION_MOVE ) {
                    float px = event.getX(0);
                    int sum = event.getPointerCount();
                    graph.Touch(px);
                }
                else if(event.getActionMasked() == MotionEvent.ACTION_UP){
                    graph.showToast();
                    ListView listView = (ListView)findViewById(android.R.id.list);
                    listView.setSelection(graph.getPos());
                }
                return true;
            }
        });

        // 表示データ種別の設定
        Spinner spinner = (Spinner)findViewById(R.id.spinner2);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                LunarBaseGraphView graph = (LunarBaseGraphView) findViewById(R.id.soragraph);
                graph.setMode(position);
                // データリストビューの更新
                if(mAdapter != null){
                    // 指定種別データを表示するように
                    mAdapter.setMode(position);
                    // 以下にてリストの表示を更新
                    ListView listView = (ListView)findViewById(android.R.id.list);
                    listView.invalidateViews();
                }

                graph.Capture();

//                graph.setDrawingCacheEnabled(true);
//                // Viewのキャッシュを取得
//                Bitmap cache = graph.getDrawingCache();
//                Bitmap screenShot = Bitmap.createBitmap(cache);
//                graph.setDrawingCacheEnabled(false);
//                // 読み書きするファイル名を指定
//                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/capture.jpeg");
//                // 指定したファイル名が無ければ作成する。
//                file.getParentFile().mkdir();
//                FileOutputStream fos = null;
//                try {
//                    fos = new FileOutputStream(file, false);
//                    // 画像のフォーマットと画質と出力先を指定して保存
//                    screenShot.compress(Bitmap.CompressFormat.JPEG, 100, fos);
//                    fos.flush();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                } finally {
//                    if (fos != null) {
//                        try {
//                            fos.close();
//                        } catch (IOException ie) {
//                            fos = null;
//                        }
//                    }
//                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // 表示日数の設定
        Spinner spinnerDay = (Spinner)findViewById(R.id.spinnerDay);
        spinnerDay.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                LunarBaseGraphView graph = (LunarBaseGraphView) findViewById(R.id.soragraph);
                graph.setDispDay(position);
            }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {

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
        int count = 0;
        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
            mSoradata.clearData();
            mProgressDialog = new ProgressDialog(DisplayMessageActivity.this);
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

            //
            ArrayList<String> dataList = new ArrayList<String>();
            dataList.add("PM2.5");
            dataList.add("OX(光化学オキシダント)");
            dataList.add("WS(風速)");
            ArrayAdapter<String> pref = new ArrayAdapter<String>(DisplayMessageActivity.this, android.R.layout.simple_spinner_item, dataList);
            pref.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            // スピナーリスト設定
            Spinner prefSpinner = (Spinner)findViewById(R.id.spinner2);
            prefSpinner.setAdapter(pref);
            prefSpinner.setSelection(0);

            ArrayList<String> dayList = new ArrayList<String>();
            dayList.add("１日");
            dayList.add("２日");
            dayList.add("３日");
            dayList.add("４日");
            dayList.add("５日");
            dayList.add("６日");
            dayList.add("７日");
            dayList.add("最大");
            ArrayAdapter<String> day = new ArrayAdapter<String>(DisplayMessageActivity.this, android.R.layout.simple_spinner_item, dayList);
            day.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            // スピナーリスト設定
            Spinner DaySpinner = (Spinner)findViewById(R.id.spinnerDay);
            DaySpinner.setAdapter(day);
            DaySpinner.setSelection(2);

            mProgressDialog.dismiss();
        }
    }
}
