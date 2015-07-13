package com.example.wada.myapplication;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by Wada on 2015/06/30.
 */
public class AsyncWeb extends AsyncTask<Void, Void, Void> {
    private static final String PROFILE_IMAGE_URL = "profile_image_url";
    private static final String SCREEN_NAME = "screen_name";
    private static final String TEXT = "text";
    private static final String STATUS = "status";
    private static final String TIMELINE=
            //"http://www.geocities.jp/chopper1250/public_timeline.xml";
    "http://soramame.taiki.go.jp/MstItiranHyou.php?Pref=40&Time=2015063012";

    public AsyncWeb()
    {

    }

    @Override
    protected Void doInBackground(Void... v)
    {
        ArrayList<com.example.wada.myapplication.Status> list = parseTimeline();
        if(list != null)
        {
            for(Iterator<com.example.wada.myapplication.Status> iterator = list.iterator(); iterator.hasNext();)
            {
                com.example.wada.myapplication.Status status = (com.example.wada.myapplication.Status)iterator.next();
                Log.d("parse", status.getScreenName());
            }
        }
        return null;
    }

    private ArrayList<com.example.wada.myapplication.Status> parseTimeline()
    {
        ArrayList<com.example.wada.myapplication.Status> list=null;
        try
        {
            XmlPullParser xmlPullParser = XmlPullParserFactory.newInstance().newPullParser();
            URL url = new URL(TIMELINE);
            URLConnection connection = url.openConnection();
            xmlPullParser.setInput(connection.getInputStream(), "UTF-8");

            String name;
            com.example.wada.myapplication.Status status = null;
            int eventType = xmlPullParser.getEventType();

            while(eventType != xmlPullParser.END_DOCUMENT)
            {
                switch (eventType)
                {
                    case XmlPullParser.START_DOCUMENT:
                        list = new ArrayList<com.example.wada.myapplication.Status>();
                        break;
                    case XmlPullParser.START_TAG:
                        name = xmlPullParser.getName();
                        if(STATUS.equalsIgnoreCase(name))
                        {
                            status = new com.example.wada.myapplication.Status();
                        }
                        else if(status != null)
                        {
                            if(TEXT.equalsIgnoreCase(name))
                            {
                                status.setText(xmlPullParser.nextText());
                            }
                            else if(SCREEN_NAME.equalsIgnoreCase(name))
                            {
                                status.setScreenName(xmlPullParser.nextText());
                            }
                            else if(PROFILE_IMAGE_URL.equalsIgnoreCase(name))
                            {
                                status.setProfileImageUrl(xmlPullParser.nextText());
                            }
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        name = xmlPullParser.getName();
                        if(STATUS.equalsIgnoreCase(name))
                        {
                            if(list != null && status != null)
                            {
                                list.add(status);
                                status = null;
                            }
                        }
                        break;
                    case XmlPullParser.TEXT:
                        name = xmlPullParser.getName();
                        break;
                }
                eventType = xmlPullParser.next();
            }
        }
        catch(MalformedURLException e)
        {
            Log.e("XmlPullParserSampleUrl", e.toString());
        }
        catch(XmlPullParserException e)
        {
            Log.e("XmlPullParserSampleUrl", e.toString());
        }
        catch(IOException e)
        {
            Log.e("XmlPullParserSampleUrl", e.toString());
        }
        return list;
    }
}
