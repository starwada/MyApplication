package com.example.wada.myapplication;

import android.app.Activity;
import android.app.ProgressDialog;
import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

/**
 * The configuration screen for the {@link SoraAppWidget SoraAppWidget} AppWidget.
 * これはウィジットを貼り付ける際に表示される設定用のアクティビティ
 * ここで、貼り付ける測定局を選択する。
 */
public class SoraAppWidgetConfigureActivity extends Activity {

    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    EditText mAppWidgetText;
    private static final String PREFS_NAME = "com.example.wada.myapplication.SoraAppWidget";
    private static final String PREF_PREFIX_KEY = "appwidget_";
    private static final String SORAPREFFILE = "SoraPrefFile";

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private static  final  String SORABASEURL="http://soramame.taiki.go.jp/";
    private static final String SORASUBURL ="MstItiran.php";
    private static final String SORADATAURL = "DataList.php?MstCode=";
    // 指定都道府県の測定局一覧取得
    private static final String SORAPREFURL ="MstItiranFrame.php?Pref=";

    ProgressDialog mProgressDialog;
    String m_strMstURL;     // 測定局のURL
    int mPref ;                     // 都道府県コード
    private Soramame mSoramame;

//    private SoramameStationAdapter mAdapter;
    ArrayList<Soramame> mList;

    public SoraAppWidgetConfigureActivity() {
        super();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED);

        setContentView(R.layout.sora_app_widget_configure);
        mAppWidgetText = (EditText) findViewById(R.id.appwidget_text);
        // ボタンにクリックリスナーを設定
        findViewById(R.id.add_button).setOnClickListener(mOnClickListener);

        // Find the widget id from the intent.
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // If this activity was started with an intent without an app widget ID, finish with an error.
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        // エディットにテキストの初期値を設定
        mAppWidgetText.setText(loadTitlePref(SoraAppWidgetConfigureActivity.this, mAppWidgetId));

        mPref = 0;
        // 都道府県インデックスを取得
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        mPref = sharedPref.getInt("CurrentPref", 1);

        // SORASUBURLから都道府県名とコードを取得、スピナーに設定
        Spinner prefspinner = (Spinner)findViewById(R.id.spinner);
        prefspinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                String strPref = parent.getItemAtPosition(position).toString();

                mPref = position + 1;

                new SoraStation().execute();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // 都道府県取得
        new PrefSpinner().execute();

        RecyclerView station = (RecyclerView)findViewById(R.id.recycler_view);
        station.addOnItemTouchListener( new RecyclerView.SimpleOnItemTouchListener()
        {
            @Override
            public void onTouchEvent(RecyclerView rv, MotionEvent e) {
                super.onTouchEvent(rv, e);
                float x = e.getX();
                float y = e.getY();

                View mChildView = rv.findChildViewUnder(x, y);

                int pos = 0;
                if (mChildView != null) {
                    pos = rv.getChildAdapterPosition(mChildView);
                }
                if(mList!=null && pos != 0) {
                    final Context context = SoraAppWidgetConfigureActivity.this;
                    mSoramame = mList.get(pos);
                    savePref(context, mAppWidgetId, mSoramame.getMstCode());

                    // It is the responsibility of the configuration activity to update the app widget
                    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                    SoraAppWidget.updateAppWidget(context, appWidgetManager, mAppWidgetId);

                    // Make sure we pass back the original appWidgetId
                    Intent resultValue = new Intent();
                    resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                    setResult(RESULT_OK, resultValue);
                    finish();
                }
            }

        });
    }

    View.OnClickListener mOnClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            final Context context = SoraAppWidgetConfigureActivity.this;

            // When the button is clicked, store the string locally
            String widgetText = mAppWidgetText.getText().toString();
            saveTitlePref(context, mAppWidgetId, widgetText);

            // It is the responsibility of the configuration activity to update the app widget
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            SoraAppWidget.updateAppWidget(context, appWidgetManager, mAppWidgetId);

            // Make sure we pass back the original appWidgetId
            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            setResult(RESULT_OK, resultValue);
            finish();
        }
    };

    // Write the prefix to the SharedPreferences object for this widget
    static void saveTitlePref(Context context, int appWidgetId, String text) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(PREF_PREFIX_KEY + appWidgetId, text);
        prefs.apply();
    }

    static void savePref(Context context, int appWidgetId, int val) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putInt(PREF_PREFIX_KEY + appWidgetId, val);
        prefs.apply();
    }

    // Read the prefix from the SharedPreferences object for this widget.
    // If there is no preference saved, get the default from a resource
    static String loadTitlePref(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        String titleValue = prefs.getString(PREF_PREFIX_KEY + appWidgetId, null);
        if (titleValue != null) {
            return titleValue;
        } else {
            return context.getString(R.string.appwidget_text);
        }
    }

    static int loadPref(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        int titleValue = prefs.getInt(PREF_PREFIX_KEY + appWidgetId, 0);
        return titleValue;
    }

    static void deleteTitlePref(Context context, int appWidgetId) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.remove(PREF_PREFIX_KEY + appWidgetId);
        prefs.apply();
    }

    // 都道府県
    // 内部ストレージにファイル保存する
    // 都道府県名なので固定でも問題ないが。
    private class PrefSpinner extends AsyncTask<Void, Void, Void>
    {
        ArrayList<String> prefList = new ArrayList<String>();

        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params)
        {
            try
            {
                if( getPrefInfo() > 0) {
                    FileOutputStream outfile = openFileOutput(SORAPREFFILE, Context.MODE_PRIVATE);

                    String url = String.format("%s%s", SORABASEURL, SORASUBURL);
                    Document doc = Jsoup.connect(url).get();
                    Elements elements = doc.getElementsByTag("option");

                    String strPref;
                    for (Element element : elements) {
                        if (Integer.parseInt(element.attr("value")) != 0) {
                            strPref = element.text();
                            prefList.add(strPref);
                            // ファイルから取得時に分割できるようにセパレータを追加する
                            outfile.write((strPref + ",").getBytes());
                        }
                    }
                    outfile.close();
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
            // simple_spinner_itemはAndroidの初期設定
//            ArrayAdapter<String> pref = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_item, prefList);
//            pref.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            ArrayAdapter<String> pref = new ArrayAdapter<String>(SoraAppWidgetConfigureActivity.this, R.layout.prefspinner_item, prefList);
            pref.setDropDownViewResource(R.layout.prefspinner_drop_item);
            // スピナーリスト設定
            Spinner prefSpinner = (Spinner)findViewById(R.id.spinner);
            prefSpinner.setAdapter(pref);
            prefSpinner.setSelection(mPref-1);
        }

        private int getPrefInfo()
        {
            int rc = 0 ;
            try
            {
                FileInputStream infile = openFileInput(SORAPREFFILE);
                int byteCount = infile.available();
                byte[] readBytes = new byte[byteCount];
                rc = infile.read(readBytes, 0, byteCount) ;
                String strBytes = new String(readBytes);
                infile.close();

                prefList.clear();
                String Pref[] = strBytes.split(",");
                Collections.addAll(prefList, Pref);
//                for( String ele : Pref)
//                {
//                   prefList.add(ele);
//                }
            }
            catch (FileNotFoundException e)
            {
                // ファイルが無ければそらまめサイトにアクセス
                rc = 1;
            }
            catch(IOException e)
            {
                rc = -1;
            }

            return rc;
        }
    }

    // 都道府県の測定局データ取得
    private class SoraStation extends AsyncTask<Void, Void, Void>
    {
        String url;
        String strOX;           // OX
        String strPM25;     // PM2.5
        String strWD;       // 風向

        SoramameSQLHelper mDbHelper = new SoramameSQLHelper(SoraAppWidgetConfigureActivity.this);
        SQLiteDatabase mDb = null;

        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(SoraAppWidgetConfigureActivity.this);
            mProgressDialog.setTitle( "そらまめ（測定局取得）");
            mProgressDialog.setMessage("Loading...");
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... params)
        {
            try
            {
                // まず、DBをチェックする。
                mDb = mDbHelper.getReadableDatabase();
                if( !mDb.isOpen() ){ return null; }

                String[] selectionArgs = { String.valueOf(mPref)};
                Cursor c = mDb.query(SoramameContract.FeedEntry.TABLE_NAME, null,
                        SoramameContract.FeedEntry.COLUMN_NAME_PREFCODE + " = ?",  selectionArgs, null, null, null);
                if( c.getCount() > 0 )
                {
                    // DBにデータがあれば、DBから取得する。
                    if( c.moveToFirst() ) {
                        if(mList != null) {
                            mList.clear();
                        }
                        mList = new ArrayList<Soramame>();
                        while (true) {
                            Soramame mame = new Soramame(
                                    c.getInt(c.getColumnIndexOrThrow(SoramameContract.FeedEntry.COLUMN_NAME_CODE)),
                                    c.getString(c.getColumnIndexOrThrow( SoramameContract.FeedEntry.COLUMN_NAME_STATION)),
                                    c.getString(c.getColumnIndexOrThrow(SoramameContract.FeedEntry.COLUMN_NAME_ADDRESS)));
                            mame.setAllow(
                                    c.getInt(c.getColumnIndexOrThrow(SoramameContract.FeedEntry.COLUMN_NAME_OX)),
                                    c.getInt(c.getColumnIndexOrThrow(SoramameContract.FeedEntry.COLUMN_NAME_PM25)),
                                    c.getInt(c.getColumnIndexOrThrow(SoramameContract.FeedEntry.COLUMN_NAME_WD))
                            );
                            mame.setSelected(c.getInt(c.getColumnIndexOrThrow(SoramameContract.FeedEntry.COLUMN_NAME_SEL)));
                            mList.add(mame);

                            if( !c.moveToNext()){ break; }
                        }
                    }
                    c.close();
                    mDb.close();
                    return null;
                }
                c.close();
                mDb.close();

                // DBに無ければ、検索してDBに登録する。
                mDb = mDbHelper.getWritableDatabase();

                url = String.format(Locale.ENGLISH, "%s%s%d", SORABASEURL, SORAPREFURL, mPref);
                Document doc = Jsoup.connect(url).get();
                Elements elements = doc.getElementsByAttributeValue("name", "Hyou");
                for( Element element : elements)
                {
                    if( element.hasAttr("src")) {
                        url = element.attr("src");
                        String soraurl = SORABASEURL + url;

                        Document sora = Jsoup.connect(soraurl).get();
                        Element body = sora.body();
                        Elements tables = body.getElementsByTag("tr");
                        url = "";
                        Integer cnt = 0;
                        if(mList != null) {
                            mList.clear();
                        }
                        mList = new ArrayList<Soramame>();

                        for( Element ta : tables) {
                            if( cnt++ > 0) {
                                Elements data = ta.getElementsByTag("td");
                                // 測定対象取得 OX(8)、PM2.5(13)、風向(15)
                                // 想定は○か✕
                                strOX = data.get(8).text();
                                strPM25 = data.get(13).text();
                                strWD = data.get(15).text();
                                // 最後のデータが空なので
                                if(strPM25.length() < 1){ break; }

                                int nCode = strPM25.codePointAt(0);
                                // PM2.5測定局のみ ○のコード(9675)
                                //if( nCode == 9675 ) {
                                Soramame ent = new Soramame(Integer.parseInt(data.get(0).text()), data.get(1).text(), data.get(2).text());
                                if(ent.setAllow(strOX, strPM25, strWD)){
                                    mList.add(ent);

                                    // 測定局DBに保存
                                    ContentValues values = new ContentValues();
                                    values.put(SoramameContract.FeedEntry.COLUMN_NAME_IND, cnt);
                                    values.put(SoramameContract.FeedEntry.COLUMN_NAME_STATION, data.get(1).text());
                                    values.put(SoramameContract.FeedEntry.COLUMN_NAME_CODE, Integer.valueOf(data.get(0).text()));
                                    values.put(SoramameContract.FeedEntry.COLUMN_NAME_ADDRESS, data.get(2).text());
                                    values.put(SoramameContract.FeedEntry.COLUMN_NAME_PREFCODE, mPref);
                                    values.put(SoramameContract.FeedEntry.COLUMN_NAME_OX, ent.getAllow(0) ? 1: 0);
                                    values.put(SoramameContract.FeedEntry.COLUMN_NAME_PM25, ent.getAllow(1) ? 1 : 0);
                                    values.put(SoramameContract.FeedEntry.COLUMN_NAME_WD, ent.getAllow(2) ? 1 : 0);
                                    values.put(SoramameContract.FeedEntry.COLUMN_NAME_SEL, 0);
                                    // 重複は追加しない
                                    long newRowId = mDb.insertWithOnConflict(SoramameContract.FeedEntry.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE);
                                }
                                //}
                            }
                        }
                    }
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
            if( mDb.isOpen()){ mDb.close(); }
            // 測定局データ取得後にリスト表示
            mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
            if(mList != null && mRecyclerView != null)
            {
                mAdapter = mRecyclerView.getAdapter();
                if( mAdapter != null ){
                    mAdapter = null;
                }
                mAdapter = new SoramameStationRecyclerAdapter(SoraAppWidgetConfigureActivity.this, mList);

                mLayoutManager = new LinearLayoutManager(SoraAppWidgetConfigureActivity.this);
                mRecyclerView.setLayoutManager(mLayoutManager);
//        mRecyclerView.addItemDecoration(new GridSpacingItemDecoration(2, dpToPx(10), true));
                mRecyclerView.setItemAnimator(new DefaultItemAnimator());
                mRecyclerView.setAdapter(mAdapter);
            }
            mProgressDialog.dismiss();
        }
    }
}

