package com.example.wada.myapplication;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Environment;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Handler;
import android.widget.RemoteViews;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link SoraAppWidgetConfigureActivity SoraAppWidgetConfigureActivity}
 */
public class SoraAppWidget extends AppWidgetProvider {
    private static Timer timer;
    private static int nCount=0;
    private static final String ACTION_START_MY_ALARM = "com.example.wada.myapplication.ACTION_START_MY_ALARM";
    private final long interval = 1 * 60 * 1000;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            updateAppWidget(context, appWidgetManager, appWidgetIds[i]);
        }
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
        CharSequence widgetText = SoraAppWidgetConfigureActivity.loadTitlePref(context, appWidgetId) + String.format("%d", nCount);
        // Construct the RemoteViews object
//        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.sora_app_widget);
//        views.setTextViewText(R.id.appwidget_text, widgetText);

        RemoteViews image = new RemoteViews(context.getPackageName(), R.layout.sora_app_widget);
        //BitmapFactory.Options options = new BitmapFactory.Options();
        //options.inJustDecodeBounds = true;
        // optionsの設定を間違うと、以下の関数ではBitmapが作成されない。
        //Bitmap bmap = BitmapFactory.decodeFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/capture.jpeg", options);
        // とりあえず、optionsは未設定（規定値）の以下にて表示されるようになった。
        Bitmap bmap = BitmapFactory.decodeFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/capture.png");
//        Canvas cv = new Canvas( bmap );
//        Paint mOX = new Paint();
//        mOX.setColor(Color.argb(75, 255, 0, 0));
//        mOX.setStrokeWidth(2.4f);
//        cv.drawText("Wada", 10.0f, 10.0f, mOX);
        image.setImageViewBitmap(R.id.appwidget_image, bmap);
        image.setTextViewText(R.id.appwidget_text, widgetText);

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

    private void setAlarm(Context context) {
        Intent alarmIntent = new Intent(context, SoraAppWidget.class);
        alarmIntent.setAction(ACTION_START_MY_ALARM);
        PendingIntent operation = PendingIntent.getBroadcast(context, 0, alarmIntent, 0);
        AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        long now = System.currentTimeMillis() + 1; // + 1 は確実に未来時刻になるようにする保険
//        long oneHourAfter = now + interval - now % (interval);
        long oneHourAfter = now + interval;
        am.set(AlarmManager.RTC, oneHourAfter, operation);
    }

    public static class MyService extends Service {
        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            ComponentName thisWidget = new ComponentName(this, SoraAppWidget.class);
            AppWidgetManager manager = AppWidgetManager.getInstance(this);
            int appWidgetIds[] = manager.getAppWidgetIds(thisWidget);

            nCount++;
            final int N = appWidgetIds.length;
            for (int i = 0; i < N; i++) {
                updateAppWidget(this, manager, appWidgetIds[i]);
            }

            return 0;
        }

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }

    private TimerTask createTimerTask(final Context context) {

        return new TimerTask() {
            @Override
            public void run() {
                Message message = new Message();
                createHandler(context).sendMessage(message);
            }
        };
    }

     // Handler
    private Handler createHandler(final Context context) {
        return new Handler(Looper.getMainLooper()) {
            public void handleMessage(Message msg) {

                updateWidget(context);
            }
        };
    }

    // ウィジット更新
    private void updateWidget(Context context) {
        ComponentName thisWidget = new ComponentName(context, SoraAppWidget.class);
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int appWidgetIds[] = manager.getAppWidgetIds(thisWidget);

        nCount++;
        onUpdate(context, manager, appWidgetIds);
    }

}

