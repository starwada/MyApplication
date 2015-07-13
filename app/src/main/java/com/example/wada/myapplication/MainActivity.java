package com.example.wada.myapplication;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;

import com.example.wada.myapplication.Soramame;


public class MainActivity extends ActionBarActivity {
    public final static String EXTRA_MESSAGE = "com.example.wada.myapplication.MESSAGE" ;

    private static  final  String SORABASEURL="http://soramame.taiki.go.jp/";
    private static final String SORASUBURL ="MstItiran.php";
    private static final String SORADATAURL = "DataList.php?MstCode=";


    // 指定都道府県の測定局一覧取得
    private static final String SORAPREFURL ="MstItiranFrame.php?Pref=";

    ProgressDialog mProgressDialog;
    String m_strMstURL;     // 測定局のURL
    int mPref ;
    int mMstCode = 0;
    boolean mFlag = false;

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
        // SORASUBURLから都道府県名とコードを取得、スピナーに設定
        Spinner prefspinner = (Spinner)findViewById(R.id.spinner);
        prefspinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                String strPref = parent.getItemAtPosition(position).toString();

                mPref = position+1;
                if(!mFlag){ mFlag = true; return ; }
                new Title().execute();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
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

    // 都道府県
    private class PrefSpinner extends AsyncTask<Void, Void, Void>
    {
        String url;
        ArrayAdapter<String> pref;

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
                url = String.format("%s%s", SORABASEURL, SORASUBURL);
                Document doc = Jsoup.connect(url).get();
                Elements elements = doc.getElementsByTag("option");
                ArrayList<String> prefList = new ArrayList<String>();
                for( Element element : elements) {
                    if (new Integer(element.attr("value")) != 0) {
                        prefList.add(element.text());
                    }
                }
                pref = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_item, prefList);
                pref.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
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
            // スピナーリスト設定
            Spinner prefSpinner = (Spinner)findViewById(R.id.spinner);
            prefSpinner.setAdapter(pref);
        }

    }

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
                                    mList.add( new Soramame(new Integer(data.get(0).text()), data.get(1).text(), data.get(2).text()));

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
        String url;

        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
//            mProgressDialog = new ProgressDialog(MainActivity.this);
//            mProgressDialog.setTitle( "そらまめ（データ取得）");
//            mProgressDialog.setMessage("Loading...");
//            mProgressDialog.setIndeterminate(false);
//            mProgressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... params)
        {
            try
            {
                // 本来、ここに測定局コードを指定する。
                url = String.format("%s%s%d", SORABASEURL, SORADATAURL, mMstCode );
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
            TextView desc_view = (TextView)findViewById(R.id.desc_text);
            desc_view.setText(url);
//            mProgressDialog.dismiss();

            Intent intent = new Intent(MainActivity.this, DisplayMessageActivity.class);
            intent.setData(Uri.parse(m_strMstURL));
            startActivity(intent);
        }
    }

}
