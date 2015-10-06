package com.example.wada.myapplication;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.Calendar;


/**
 * そらまめ PM2.5測定値用グラフカスタムビュー
 * UI Component/Custom View にて作成
 */
public class LunarBaseGraphView extends View {
    private String mExampleString; // TODO: use a default from R.string...
    private int mExampleColor = Color.RED; // TODO: use a default from R.color...
    private float mExampleDimension = 0; // TODO: use a default from R.dimen...
    private Drawable mExampleDrawable;

    private TextPaint mTextPaint;
    private float mTextWidth;
    private float mTextHeight;

    private Soramame mSoramame;     // 測定局のPM2.5データ
    private float mMax[] = new float[3];  // 表示データのMAX
    private Paint mBack;
    private Paint mLine ;
    private Paint mDot ;
    private RectF mRect;
//    private float[] mVert;

    private Paint mOX;
//    private float[] mOXLines;
    // 表示区分 PM2.5 OX（光化学オキシダント） WS（風速）
    private float mDotY[][] = { {10.0f, 15.0f, 35.0f, 50.0f, 70.0f, 100.0f },
        {0.02f, 0.04f, 0.06f, 0.12f, 0.24f, 0.34f },
        {4.0f, 7.0f, 10.0f, 13.0f, 15.0f, 25.0f}};

    private int mIndex;                     // 強調日時インデックス
    private int mMode;                      // 表示データモード 0 PM2.5/1 OX

    public LunarBaseGraphView(Context context) {
        super(context);
        init(null, 0);
    }

    public LunarBaseGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public LunarBaseGraphView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        try {
            // Load attributes
            final TypedArray a = getContext().obtainStyledAttributes(
                    attrs, R.styleable.LunarBaseGraphView, defStyle, 0);

            mExampleString = a.getString(
                    R.styleable.LunarBaseGraphView_exampleString);
            mExampleColor = a.getColor(
                    R.styleable.LunarBaseGraphView_exampleColor,
                    mExampleColor);
            // Use getDimensionPixelSize or getDimensionPixelOffset when dealing with
            // values that should fall on pixel boundaries.
            mExampleDimension = a.getDimension(
                    R.styleable.LunarBaseGraphView_exampleDimension,
                    mExampleDimension);

            if (a.hasValue(R.styleable.LunarBaseGraphView_exampleDrawable)) {
                mExampleDrawable = a.getDrawable(
                        R.styleable.LunarBaseGraphView_exampleDrawable);
                mExampleDrawable.setCallback(this);
            }

            a.recycle();

            // Set up a default TextPaint object
            mTextPaint = new TextPaint();
            mTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
            mTextPaint.setTextAlign(Paint.Align.LEFT);

            // Update TextPaint and text measurements from attributes
            invalidateTextPaintAndMeasurements();

            mMax[0] = mMax[1] = mMax[2] = 0.0f;
            mBack = new Paint();
            mBack.setColor(Color.argb(75, 0, 0, 255));
            mLine = new Paint();
            mLine.setColor(Color.argb(125, 0, 0, 0));
            mLine.setStrokeWidth(4);
            mDot = new Paint();
            mDot.setColor(Color.argb(255, 255, 0, 0));
            mDot.setStrokeWidth(2);
            mRect = new RectF();
            mIndex = 0;
            mMode = 0;
            // OX用のペイント情報
            mOX = new Paint();
            mOX.setColor(Color.argb(75, 255, 0, 0));
            mOX.setStrokeWidth(2.4f);
        }
        catch(java.lang.NullPointerException e){
            e.getMessage();
        }
    }

    private void invalidateTextPaintAndMeasurements() {
        mTextPaint.setTextSize(mExampleDimension);
        mTextPaint.setColor(mExampleColor);
        mTextWidth = mTextPaint.measureText(mExampleString);

        Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
//        mTextHeight = fontMetrics.bottom;
        // ほぼ文字高さのようなので、マイナスで返るので反転
        mTextHeight = -fontMetrics.ascent;
    }

    public void setData(Soramame sora){
        if(mSoramame != null){ mSoramame = null; }
        if( sora.getSize() < 1 ){ return ; }

        mMax[0] = mMax[1] = mMax[2] = 0.0f;
        mSoramame = new Soramame(sora.getMstCode(), sora.getMstName(), sora.getAddress());
        ArrayList<Soramame.SoramameData> list = sora.getData();
        for( Soramame.SoramameData data : list){
            // それぞれのMAX値を取得
            if( (float)data.getPM25() > mMax[0] ){ mMax[0] = (float)data.getPM25(); }
            if( data.getOX() > mMax[1] ){ mMax[1] = data.getOX(); }
            if( data.getWS() > mMax[2] ){ mMax[2] = data.getWS(); }
            mSoramame.setData(data);
        }

        // 再描画
        invalidate();
    }

    // 強調日時設定
    public void setPos(int position){
        mIndex = position;
        invalidate();
    }

    // 表示データ設定
    public void setMode(int mode){
        mMode = mode;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // TODO: consider storing these as member variables to reduce
        if(mSoramame == null){ return ; }
        // allocations per draw cycle.
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        int contentWidth = getWidth() - paddingLeft - paddingRight;
        int contentHeight = getHeight() - paddingTop - paddingBottom;

        mExampleDimension = 32.0f;
        invalidateTextPaintAndMeasurements();

        // グラフ描画
        // グラフ背景
        float y = (float)(paddingTop+contentHeight);
        float rh = (float)contentHeight/mDotY[mMode][5];

        // PM2.5/OX/WS
        // ～１０/0.0-0.02/0.2-3.9
        mRect.set( (float)paddingLeft, y-rh*mDotY[mMode][0], (float)(paddingLeft+contentWidth), y);
//        mBack.setColor(Color.argb(75, 0, 0, 255));
        mBack.setColor(0xFF2196F3);
        canvas.drawRect(mRect, mBack);
        // １１～１５/0.021-0.04/4.0-6.9
        mRect.set( (float)paddingLeft, y-rh*mDotY[mMode][1], (float)(paddingLeft+contentWidth), y-rh*mDotY[mMode][0]);
//        mBack.setColor(Color.argb(75, 0, 255,255));
        mBack.setColor(0xFF81D4FA);
        canvas.drawRect(mRect, mBack);
        // １６～３５/0.041-0.06/7.0-9.9
        mRect.set( (float)paddingLeft, y-rh*mDotY[mMode][2], (float)(paddingLeft+contentWidth), y-rh*mDotY[mMode][1]);
        mBack.setColor(Color.argb(75, 0, 255,128));
        canvas.drawRect(mRect, mBack);
        // ３６～５０/0.061-0.119/10.0-12.9
        mRect.set( (float)paddingLeft, y-rh*mDotY[mMode][3], (float)(paddingLeft+contentWidth), y-rh*mDotY[mMode][2]);
        mBack.setColor(Color.argb(75, 255, 255,0));
        canvas.drawRect(mRect, mBack);
        // ５１～７０/0.12-0.239/13.0-14.9
        mRect.set( (float)paddingLeft, y-rh*mDotY[mMode][4], (float)(paddingLeft+contentWidth), y-rh*mDotY[mMode][3]);
        mBack.setColor(Color.argb(75, 255, 128,0));
        canvas.drawRect(mRect, mBack);
        // 70-100/0.24-0.34/15.0-25.0
        mRect.set( (float)paddingLeft, y-rh*mDotY[mMode][5], (float)(paddingLeft+contentWidth), y-rh*mDotY[mMode][4]);
        mBack.setColor(Color.argb(75, 255, 0,0));
        canvas.drawRect(mRect, mBack);

        // グラフ枠
        mLine.setStrokeWidth(4);
        canvas.drawLine(paddingLeft, paddingTop + contentHeight, paddingLeft + contentWidth, paddingTop + contentHeight, mLine);
        canvas.drawLine( paddingLeft, paddingTop, paddingLeft, contentHeight+paddingTop, mLine );
        y = (float)(paddingTop+contentHeight);
        mLine.setStrokeWidth(1);
        for(int i=0; i<5; i++){
            y -= (float)contentHeight/5;
            canvas.drawLine(paddingLeft, y, paddingLeft + contentWidth, y, mLine);
            switch(mMode){
                case 0:
                    canvas.drawText(String.format("%d", i*20+20), 0, y + mTextHeight/2, mTextPaint);
                    break;
                case 1:
                    canvas.drawText(String.format("%.2f", i*mDotY[mMode][5]/5.0f+mDotY[mMode][5]/5.0f), 0, y + mTextHeight/2, mTextPaint);
                    break;
                case 2:
                    canvas.drawText(String.format("%.1f", i*mDotY[mMode][5]/5.0f+mDotY[mMode][5]/5.0f), 0, y + mTextHeight/2, mTextPaint);
                    break;
            }
        }

        // グラフ
        if(mSoramame.getSize() > 0){
            ArrayList<Soramame.SoramameData> list = mSoramame.getData();
            float x=paddingLeft+contentWidth;
            float gap = (float)contentWidth/list.size();

            y = (float)(paddingTop + contentHeight);

            int nCount=0;
            float doty = 0f;
            float fradius = 3.0f;
            float fOXY[] = { 0.0f, 0.0f  };
            for( Soramame.SoramameData data : list){
                fradius = 3.0f;
                switch(mMode){
                    case 0:
                        doty = y - (data.getPM25() * (float)contentHeight / mDotY[mMode][5]);
                        break;
                    case 1:
                        doty = y-(data.getOX() * (float)contentHeight /mDotY[mMode][5] );
                        break;
                    case 2:
                        doty = y - (data.getWS() * (float)contentHeight / mDotY[mMode][5] );
                        break;
                }

                if( (mMode == 0 && data.getPM25() > 0) ||
                        (mMode == 1 && data.getOX() > 0.0) ||
                        (mMode == 2 && data.getWS() > 0.0 )) {
                    if( nCount == mIndex) {
                        fradius = 12.0f;
                    }
                    canvas.drawCircle(x, doty, fradius, mDot);
                }
                // 時間軸描画
                if( data.getDate().get(Calendar.HOUR_OF_DAY) == 0 ){
                    canvas.drawLine(x, paddingTop, x, contentHeight + paddingTop, mLine);
                }
                if( data.getDate().get(Calendar.HOUR_OF_DAY) == 1 ){
                    // 日付描画
                    canvas.drawText(String.format("%d日", data.getDate().get(Calendar.DAY_OF_MONTH)),
                            x, paddingTop + contentHeight + mTextHeight, mTextPaint);
                }
                // リストにてクリックしたインデックスデータに描画<-ここを画像に切替える
//                if( nCount == mIndex){
//                    mVert[0]=x;
//                    mVert[1] = doty;
//                    mVert[2]=x+gap*2;
//                    mVert[3]=doty-30;
//                    mVert[4]=x-gap*2;
//                    mVert[5]=mVert[3];
//
//                    canvas.drawVertices(Canvas.VertexMode.TRIANGLES, 6, mVert, 0, null, 0,null,0,null,0,0, mDot);
//                }
                nCount += 1;
                x -= gap;
                // グラフ用ポリライン描画
                fOXY[1] = fOXY[0];
                fOXY[0] = doty;
                if( nCount > 1){
                    canvas.drawLine(x+gap, fOXY[0], x+gap+gap, fOXY[1], mOX);
                }
            }
            switch (mMode){
                case 0:
                    mExampleString = String.format("最高値:%.0f μg/m3", mMax[mMode]);
                    break;
                case 1:
                    mExampleString = String.format("最高値:%.2f ppm", mMax[mMode]);
                    break;
                case 2:
                    mExampleString = String.format("最高値:%.1f m/s", mMax[mMode]);
                    break;
            }
        }

        mTextPaint.setTextSize(60.0f);
        // Draw the text.
        canvas.drawText(mExampleString,
                paddingLeft + (contentWidth - mTextWidth) / 2,
//                paddingTop + (contentHeight + mTextHeight) / 2,
                paddingTop + (float)contentHeight/5 + mTextHeight,
                mTextPaint);

        // Draw the example drawable on top of the text.
//        if (mExampleDrawable != null) {
//            mExampleDrawable.setBounds(paddingLeft, paddingTop,
//                    paddingLeft + contentWidth, paddingTop + contentHeight);
//            mExampleDrawable.draw(canvas);
//        }
    }

    /**
     * Gets the example string attribute value.
     *
     * @return The example string attribute value.
     */
    public String getExampleString() {
        return mExampleString;
    }

    /**
     * Sets the view's example string attribute value. In the example view, this string
     * is the text to draw.
     *
     * @param exampleString The example string attribute value to use.
     */
    public void setExampleString(String exampleString) {
        mExampleString = exampleString;
        invalidateTextPaintAndMeasurements();
    }

    /**
     * Gets the example color attribute value.
     *
     * @return The example color attribute value.
     */
    public int getExampleColor() {
        return mExampleColor;
    }

    /**
     * Sets the view's example color attribute value. In the example view, this color
     * is the font color.
     *
     * @param exampleColor The example color attribute value to use.
     */
    public void setExampleColor(int exampleColor) {
        mExampleColor = exampleColor;
        invalidateTextPaintAndMeasurements();
    }

    /**
     * Gets the example dimension attribute value.
     *
     * @return The example dimension attribute value.
     */
    public float getExampleDimension() {
        return mExampleDimension;
    }

    /**
     * Sets the view's example dimension attribute value. In the example view, this dimension
     * is the font size.
     *
     * @param exampleDimension The example dimension attribute value to use.
     */
    public void setExampleDimension(float exampleDimension) {
        mExampleDimension = exampleDimension;
        invalidateTextPaintAndMeasurements();
    }

    /**
     * Gets the example drawable attribute value.
     *
     * @return The example drawable attribute value.
     */
    public Drawable getExampleDrawable() {
        return mExampleDrawable;
    }

    /**
     * Sets the view's example drawable attribute value. In the example view, this drawable is
     * drawn above the text.
     *
     * @param exampleDrawable The example drawable attribute value to use.
     */
    public void setExampleDrawable(Drawable exampleDrawable) {
        mExampleDrawable = exampleDrawable;
    }
}
