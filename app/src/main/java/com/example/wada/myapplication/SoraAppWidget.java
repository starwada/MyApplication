package com.example.wada.myapplication;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.RemoteViews;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link SoraAppWidgetConfigureActivity SoraAppWidgetConfigureActivity}
 * 測定結果のウィジットなので、１時間毎に更新させる。
 * タイマーを使用してみたが、うまく動作しなかった。途中でタイマーが止まってしまった（使い方が悪いのだろうが）。
 * タイマーのコードは残しておく。
 */
public class SoraAppWidget extends AppWidgetProvider {
//    private static Timer timer;
    private static int nCount=0;
    private static final String ACTION_START_MY_ALARM = "com.example.wada.myapplication.ACTION_START_MY_ALARM";
    private final long interval = 60 * 60 * 1000;

    private static  final  String SORABASEURL="http://soramame.taiki.go.jp/";
    private static final String SORADATAURL = "DataList.php?MstCode=";

    // 以下はシステムのタイミングで呼ばれる
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
//        final int N = appWidgetIds.length;
//        for (int i = 0; i < N; i++) {
//            updateAppWidget(context, appWidgetManager, appWidgetIds[i]);
//        }
        Intent serviceIntent = new Intent(context, MyService.class);
        context.startService(serviceIntent);
        // アラーム設定
        setAlarm(context);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // When the user deletes the widget, delete the preference associated with it.
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            SoraAppWidgetConfigureActivity.deleteTitlePref(context, appWidgetIds[i]);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
        // 初回に１度だけタイマー起動
//        if(timer == null) {
//            timer = new Timer();
//            timer.schedule(createTimerTask(context), 0, 1000 * 60);
//        }
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
        // ウィジットが無くなたらタイマーキャンセル
//        if(timer != null){
//            nCount = 0;
//            timer.cancel();
//            timer = null;
//        }
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        // ウィジット設定アクティビティ（画面）にて設定した文字列（Prefファイルに保持）をここで取得。
//        CharSequence widgetText = SoraAppWidgetConfigureActivity.loadTitlePref(context, appWidgetId) + String.format("%d", nCount);
        int nCode = SoraAppWidgetConfigureActivity.loadPref(context, appWidgetId);
        if( nCode == 0 ){ return ; }
        // Construct the RemoteViews object
//        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.sora_app_widget);
//        views.setTextViewText(R.id.appwidget_text, widgetText);

        RemoteViews image = new RemoteViews(context.getPackageName(), R.layout.sora_app_widget);
        //BitmapFactory.Options options = new BitmapFactory.Options();
        //options.inJustDecodeBounds = true;
        // optionsの設定を間違うと、以下の関数ではBitmapが作成されない。
        //Bitmap bmap = BitmapFactory.decodeFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/capture.jpeg", options);
        // とりあえず、optionsは未設定（規定値）の以下にて表示されるようになった。
        Bitmap bmap = BitmapFactory.decodeFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) +
                String.format("/soracapture_%d.png", appWidgetId));
//        Canvas cv = new Canvas( bmap );
//        Paint mOX = new Paint();
//        mOX.setColor(Color.argb(75, 255, 0, 0));
//        mOX.setStrokeWidth(2.4f);
//        cv.drawText("Wada", 10.0f, 10.0f, mOX);
        image.setImageViewBitmap(R.id.appwidget_image, bmap);
//        image.setTextViewText(R.id.appwidget_text, widgetText);

        // ここは、テキストをクリックしたらMainActivityが起動する仕組みを設定している。
        // Create an Intent to launch ExampleActivity
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        // Get the layout for the App Widget and attach an on-click listener
        // to the button
        image.setOnClickPendingIntent(R.id.appwidget_text, pendingIntent);

        // Instruct the widget manager to update the widget
//        appWidgetManager.updateAppWidget(appWidgetId, views);
        appWidgetManager.updateAppWidget(appWidgetId, image);

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent.getAction().equals(ACTION_START_MY_ALARM)) {
            if (ACTION_START_MY_ALARM.equals(intent.getAction())) {
                Intent serviceIntent = new Intent(context, MyService.class);
                context.startService(serviceIntent);
            }
            setAlarm(context);
        }
    }

    // アラーム設定（これをその都度呼び出している）
    private void setAlarm(Context context) {
        Intent alarmIntent = new Intent(context, SoraAppWidget.class);
        alarmIntent.setAction(ACTION_START_MY_ALARM);
        PendingIntent operation = PendingIntent.getBroadcast(context, 0, alarmIntent, 0);
        AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        long now = System.currentTimeMillis() + 1; // + 1 は確実に未来時刻になるようにする保険
        long oneHourAfter = now + interval - now % (interval);
//        long oneHourAfter = now + interval;
        am.set(AlarmManager.RTC, oneHourAfter, operation);
    }

    public static class MyService extends Service {
        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            ComponentName thisWidget = new ComponentName(this, SoraAppWidget.class);
            AppWidgetManager manager = AppWidgetManager.getInstance(this);
            int appWidgetIds[] = manager.getAppWidgetIds(thisWidget);

            final int N = appWidgetIds.length;
            for (int i = 0; i < N; i++) {
                new SoraDesc().execute(appWidgetIds[i]);

                // updateAppWidget(this, manager, appWidgetIds[i]);
            }

            return 0;
        }

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        private class SoraDesc extends AsyncTask<Integer, Void, Integer>
        {
            int count = 0;
            int appWidgetId = 0;
            Soramame soramame = new Soramame();
            SoramameSQLHelper mDbHelper = new SoramameSQLHelper(MyService.this);
            SQLiteDatabase mDb = null;
            @Override
            protected void onPreExecute()
            {
                super.onPreExecute();
            }

            @Override
            protected Integer doInBackground(Integer... appWidgetIds)
            {
                try
                {
                    String strMstURL = "";
                    appWidgetId = appWidgetIds[0];
                    // 測定局コード取得
                    int nCode = SoraAppWidgetConfigureActivity.loadPref(MyService.this, appWidgetId);
                    if(nCode == 0){ return -1; }

                    mDb = mDbHelper.getReadableDatabase();
                    if (!mDb.isOpen()) {
                        return -2;
                    }
                    checkDB(soramame, mDb);

                    String url = String.format(Locale.ENGLISH, "%s%s%d", SORABASEURL, SORADATAURL, nCode);
                    Document doc = Jsoup.connect(url).get();
                    Elements elements = doc.getElementsByAttributeValue("name", "Hyou");
//                Integer size = elements.size();
                    for( Element element : elements)
                    {
                        if( element.hasAttr("src"))
                        {
                            url = element.attr("src");
                            strMstURL = SORABASEURL + url;
                            // ここでは、測定局のURL解決まで、URLを次のアクティビティに渡す。
                            break;
                        }
                    }

                    Document sora = Jsoup.connect(strMstURL).get();
                    Element body = sora.body();
                    Elements tables = body.getElementsByAttributeValue("align", "right");

                    for( Element ta : tables)
                    {
                        Elements data = ta.getElementsByTag("td");
                        // 0 西暦/1 月/2 日/3 時間
                        // 4 SO2/5 NO/6 NO2/7 NOX/8 CO/9 OX/10 NMHC/11 CH4/12 THC/13 SPM/14 PM2.5/15 SP/16 WD/17 WS

                        soramame.setData(data.get(0).text(), data.get(1).text(), data.get(2).text(), data.get(3).text(),
                                data.get(9).text(), data.get(14).text(), data.get(16).text(), data.get(17).text());
                        count++;
                    }
                }
                catch(IOException e)
                {
                    e.printStackTrace();
                }
                return 0;
            }

            @Override
            protected void onPostExecute(Integer result)
            {
                if(result < 0){ return ; }
                Bitmap graph = GraphFactory.drawGraph(soramame, appWidgetId);
                // ここでウィジット更新
                AppWidgetManager manager = AppWidgetManager.getInstance(MyService.this);
                updateAppWidget(MyService.this, manager, appWidgetId);
            }
        }

        // soramame 測定局データ
        // db DB
        // 返り値：0    正常終了/1　DBに指定測定局データが無い（サイトからデータを取得する）
        private int checkDB(Soramame soramame, SQLiteDatabase db) {
            int rc = 0;

            try {
                if (!db.isOpen()) {
                    return -1;
                }
                String strWhereArg[] = {String.valueOf(soramame.getMstCode())};
                // 日付でソート desc 降順（新しい->古い）
                Cursor c = db.query(SoramameContract.FeedEntry.TABLE_NAME, null,
                        SoramameContract.FeedEntry.COLUMN_NAME_CODE + " = ?", strWhereArg, null, null, null);
                if (c.getCount() > 0) {
                    soramame.clearData();

                    if (c.moveToFirst()) {
                        Soramame mame = new Soramame(
                                c.getInt(c.getColumnIndexOrThrow(SoramameContract.FeedEntry.COLUMN_NAME_CODE)),
                                c.getString(c.getColumnIndexOrThrow(SoramameContract.FeedEntry.COLUMN_NAME_STATION)),
                                c.getString(c.getColumnIndexOrThrow(SoramameContract.FeedEntry.COLUMN_NAME_ADDRESS)));
                        mame.setSelected(1);
                    }
                }
                c.close();
            } catch (SQLiteException e) {
                e.printStackTrace();
            }

            return rc;
        }

    }

    // 以下はタイマー用のコード。未使用だがコメント化しておく。
//    private TimerTask createTimerTask(final Context context) {
//
//        return new TimerTask() {
//            @Override
//            public void run() {
//                Message message = new Message();
//                createHandler(context).sendMessage(message);
//            }
//        };
//    }
//
//     // Handler
//    private Handler createHandler(final Context context) {
//        return new Handler(Looper.getMainLooper()) {
//            public void handleMessage(Message msg) {
//
//                updateWidget(context);
//            }
//        };
//    }
//
//    // ウィジット更新
//    private void updateWidget(Context context) {
//        ComponentName thisWidget = new ComponentName(context, SoraAppWidget.class);
//        AppWidgetManager manager = AppWidgetManager.getInstance(context);
//        int appWidgetIds[] = manager.getAppWidgetIds(thisWidget);
//
//        nCount++;
//        onUpdate(context, manager, appWidgetIds);
//    }


}

