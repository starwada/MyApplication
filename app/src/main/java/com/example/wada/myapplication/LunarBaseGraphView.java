package com.example.wada.myapplication;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;


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
    private Paint mBack;
    private Paint mLine ;
    private Paint mDot ;
    private RectF mRect;
    private float[] mVert;

    private int mIndex;

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

            mBack = new Paint();
            mBack.setColor(Color.argb(75, 0, 0, 255));
            mLine = new Paint();
            mLine.setColor(Color.argb(125, 0, 0, 0));
            mLine.setStrokeWidth(3);
            mDot = new Paint();
            mDot.setColor(Color.argb(255, 255, 0, 0));
            mDot.setStrokeWidth(2);
            mRect = new RectF();
            mVert = new float[6];
            mIndex = 0;
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
        mTextHeight = fontMetrics.bottom;
    }

    public void setData(Soramame sora){
        if(mSoramame != null){ mSoramame = null; }
        if( sora.getSize() < 1 ){ return ; }

        mSoramame = new Soramame(sora.getMstCode(), sora.getMstName(), sora.getAddress());
        ArrayList<Soramame.SoramameData> list = sora.getData();
        for( Soramame.SoramameData data : list){
            mSoramame.setData(data);
        }
        // 再描画
        invalidate();
    }

    public void setPos(int position){
        mIndex = position;
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

        // グラフ描画
        // グラフ背景
        float y = (float)(paddingTop+contentHeight);
        float rh = (float)contentHeight/100;

        // ～１０
        mRect.set( (float)paddingLeft, y-rh*10, (float)(paddingLeft+contentWidth), y);
        mBack.setColor(Color.argb(75, 0, 0, 255));
        canvas.drawRect(mRect, mBack);
        // １１～１５
        mRect.set( (float)paddingLeft, y-rh*15, (float)(paddingLeft+contentWidth), y-rh*10);
        mBack.setColor(Color.argb(75, 0, 255,255));
        canvas.drawRect(mRect, mBack);
        // １６～３５
        mRect.set( (float)paddingLeft, y-rh*35, (float)(paddingLeft+contentWidth), y-rh*15);
        mBack.setColor(Color.argb(75, 0, 255,128));
        canvas.drawRect(mRect, mBack);
        // ３６～５０
        mRect.set( (float)paddingLeft, y-rh*50, (float)(paddingLeft+contentWidth), y-rh*35);
        mBack.setColor(Color.argb(75, 255, 255,0));
        canvas.drawRect(mRect, mBack);
        // ５１～７０
        mRect.set( (float)paddingLeft, y-rh*70, (float)(paddingLeft+contentWidth), y-rh*50);
        mBack.setColor(Color.argb(75, 255, 128,0));
        canvas.drawRect(mRect, mBack);
        // ７０～
        mRect.set( (float)paddingLeft, y-rh*100, (float)(paddingLeft+contentWidth), y-rh*70);
        mBack.setColor(Color.argb(75, 255, 0,0));
        canvas.drawRect(mRect, mBack);

        // グラフ枠
        mLine.setStrokeWidth(3);
        canvas.drawLine(paddingLeft, paddingTop + contentHeight, paddingLeft + contentWidth, paddingTop + contentHeight, mLine);
        canvas.drawLine( paddingLeft, paddingTop, paddingLeft, contentHeight+paddingTop, mLine );
        y = (float)(paddingTop+contentHeight);
        mLine.setStrokeWidth(1);
        for(int i=0; i<5; i++){
            y -= (float)contentHeight/5;
            canvas.drawLine( paddingLeft, y, paddingLeft+contentWidth, y, mLine );
        }

        // グラフ
        if(mSoramame.getSize() > 0){
            ArrayList<Soramame.SoramameData> list = mSoramame.getData();
            float x=paddingLeft+contentWidth;
            float gap = (float)contentWidth/list.size();

            y = (float)(paddingTop + contentHeight);

            int nCount=0;
            float doty = 0f;
            for( Soramame.SoramameData data : list){
                doty = y - (data.getPM25() * (float)contentHeight / 100);
                if( data.getPM25() > 0) {
                    canvas.drawCircle(x, doty, 3, mDot);
                }
                if( nCount == mIndex){
                    mVert[0]=x;
                    mVert[1] = doty;
                    mVert[2]=x+gap*2;
                    mVert[3]=doty-30;
                    mVert[4]=x-gap*2;
                    mVert[5]=mVert[3];

                    canvas.drawVertices(Canvas.VertexMode.TRIANGLES, 6, mVert, 0, null, 0,null,0,null,0,0, mDot);
                }
                nCount += 1;
                x -= gap;
            }
            mExampleString = String.format("height:%d gap:%.2f", contentHeight, rh);
        }

        // Draw the text.
        canvas.drawText(mExampleString,
                paddingLeft + (contentWidth - mTextWidth) / 2,
                paddingTop + (contentHeight + mTextHeight) / 2,
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
