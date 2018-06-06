package com.bw.ydb.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.widget.ProgressBar;
import com.bw.ydb.R;

public class TextProgressBar extends ProgressBar{

    private String mProgressText;

    private Paint mProgressPaint;

    public TextProgressBar(Context context) {
        super(context);
        initializeProgressBar();
    }

    public TextProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeProgressBar();
    }

    public TextProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initializeProgressBar();
    }

    public void initializeProgressBar() {
        mProgressText = "0%";
        mProgressPaint = new Paint();
        //Logger.e("DPI>>" + getResources().getDisplayMetrics().densityDpi);
        switch (getResources().getDisplayMetrics().densityDpi) {
            case DisplayMetrics.DENSITY_TV:
                mProgressPaint.setTextSize(14);
                break;
            case DisplayMetrics.DENSITY_HIGH:
                mProgressPaint.setTextSize(16);
                break;
            case DisplayMetrics.DENSITY_XHIGH:
                mProgressPaint.setTextSize(30);
                break;
            case DisplayMetrics.DENSITY_XXHIGH:
                mProgressPaint.setTextSize(40);
                break;
            case DisplayMetrics.DENSITY_XXXHIGH:
                mProgressPaint.setTextSize(50);
                break;
            default:
                mProgressPaint.setTextSize(50);
                break;
        }
        mProgressPaint.setColor(getResources().getColor(R.color.colorRed));
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Rect boundsProgress = new Rect();
        mProgressPaint.getTextBounds(mProgressText, 0, mProgressText.length(), boundsProgress);
        int xp;
        switch (getResources().getDisplayMetrics().densityDpi) {
            case DisplayMetrics.DENSITY_TV:
                xp = getWidth() - 70;
                break;
            case DisplayMetrics.DENSITY_HIGH:
                xp = getWidth() - 80;
                break;
            case DisplayMetrics.DENSITY_XHIGH:
                xp = getWidth() - 100;
                break;
            case DisplayMetrics.DENSITY_XXHIGH:
                xp = getWidth() - 120;
                break;
            case DisplayMetrics.DENSITY_XXXHIGH:
                xp = getWidth() - 140;
                break;
            default:
                xp = getWidth() - 140;
                break;
        }
        int yp = getHeight() / 2 - boundsProgress.centerY();
        canvas.drawText(mProgressText, xp, yp, mProgressPaint);
    }

    public synchronized void setProgressText(String text) {
        this.mProgressText = text;
        drawableStateChanged();
    }

}
