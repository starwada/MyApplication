package com.example.wada.myapplication;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Element;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;

import com.example.wada.myapplication.Soramame;


public class MainActivity extends ActionBarActivity {
    private static final String SORAPREFFILE = "SoraPrefFile";

    private static  final  String SORABASEURL="http://soramame.taiki.go.jp/";
    private static final String SORASUBURL ="MstItiran.php";
    private static final String SORADATAURL = "DataList.php?MstCode=";
    // 指定都道府県の測定局一覧取得
    private static final String SORAPREFURL ="MstItiranFrame.php?Pref=";

    ProgressDialog mProgressDialog;
    String m_strMstURL;     // 測定局のURL
    int mPref ;                     // 都道府県コード
    int mMstCode = 0;

    private SoramameStationAdapter mAdapter;
    ArrayList<Soramame> mList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // これで、住所から緯度経度を取得 以下テストコードなのでコメントとする
//        try {
//            Geocoder geo = new Geocoder(MainActivity.this, Locale.JAPAN);
//            List<Address> address = geo.getFromLocationName("北九州市八幡西区浅川学園台３－６－１", 1);
//
//            // getAdminArea()にて都道府県名、getSubAdminArea()はnullだった。
//            String strAd = String.format("Lati:%f Longi:%f AdminArea:%s SubAdmin:%s",
//                    address.get(0).getLatitude(), address.get(0).getLongitude(),
//                    address.get(0).getAdminArea(), address.get(0).getSubAdminArea());
//            TextView title_view = (TextView)findViewById(R.id.title_text);
//            title_view.setText(strAd);
//            return;
//        }
//        catch(IOException e) {
//
//        }

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

                new Title().execute();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // 都道府県取得
        new PrefSpinner().execute();

//        Button titlebutton = (Button)findViewById(R.id.title_button);
//        titlebutton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v)
//            {
//                new Title().execute();
//            }
//        });

        Button descbutton = (Button)findViewById(R.id.desc_button);
        descbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Desc().execute();
            }
        });

        ListView station = (ListView)findViewById(R.id.sorastation);
        station.setOnItemClickListener( new AdapterView.OnItemClickListener()
        {
            public void onItemClick( AdapterView<?> parent, View v, int pos, long id)
            {
                if(mList!=null) {
                    mMstCode = 0;
                    TextView desc_view = (TextView) findViewById(R.id.desc_text);
//                desc_view.setText(String.format("%d", pos));
                    desc_view.setText(mList.get(pos).getMstName());
                    mMstCode = mList.get(pos).getMstCode();

                    new Desc().execute();
                }
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
    public void onPause()
    {
        // 都道府県インデックスを保存する
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("CurrentPref", mPref);
//        editor.commit();
        editor.apply();

        super.onPause();
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
            ArrayAdapter<String> pref = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_item, prefList);
            pref.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
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
                byte[] readBytes = new byte[infile.available()];
                infile.read(readBytes);
                String strBytes = new String(readBytes);
                infile.close();

                prefList.clear();
                String Pref[] = strBytes.split(",");
                for( String ele : Pref)
                {
                   prefList.add(ele);
                }
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
    private class Title extends AsyncTask<Void, Void, Void>
    {
        String url;

        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(MainActivity.this);
            mProgressDialog.setTitle( "そらまめ（測定局取得） test");
            mProgressDialog.setMessage("Loading...");
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... params)
        {
            try
            {
                url = String.format("%s%s%d", SORABASEURL, SORAPREFURL, mPref);
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
                                String kyoku = data.get(13).text();
                                // 最後のデータが空なので
                                if(kyoku.length() < 1)
                                {
                                    break;
                                }
                                int nCode = kyoku.codePointAt(0);
                                // PM2.5測定局のみ
                                if( nCode == 9675 ) {
                                    mList.add( new Soramame(Integer.parseInt(data.get(0).text()), data.get(1).text(), data.get(2).text()));

                                }
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
            // 測定局データ取得後にリスト表示
//            TextView title_view = (TextView)findViewById(R.id.title_text);
//            title_view.setText(aSoramame.get(3).getStationInfo());
            if(mList != null)
            {
                mAdapter = new SoramameStationAdapter(MainActivity.this, mList);
                ListView station = (ListView)findViewById(R.id.sorastation);
                station.setAdapter(mAdapter);
            }
            mProgressDialog.dismiss();
        }
    }

    private class Desc extends AsyncTask<Void, Void, Void>
    {

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
                // 本来、ここに測定局コードを指定する。
                String url = String.format("%s%s%d", SORABASEURL, SORADATAURL, mMstCode );
                Document doc = Jsoup.connect(url).get();
                Elements elements = doc.getElementsByAttributeValue("name", "Hyou");
                Integer size = elements.size();
                for( Element element : elements)
                {
                    if( element.hasAttr("src"))
                    {
                        url = element.attr("src");
                        m_strMstURL = SORABASEURL + url;
                        // ここでは、測定局のURL解決まで、URLを次のアクティビティに渡す。

                        break;
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
            // リスト用アクティビティ
//            Intent intent = new Intent(MainActivity.this, DisplayMessageActivity.class);
            // グラフ用アクティビティ
            Intent intent = new Intent(MainActivity.this, GraphActivity.class);
            intent.setData(Uri.parse(m_strMstURL));
            startActivity(intent);
        }
    }
}
