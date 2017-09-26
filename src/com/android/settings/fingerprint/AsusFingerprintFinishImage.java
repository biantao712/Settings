package com.android.settings.fingerprint;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

import com.android.settings.R;


public class AsusFingerprintFinishImage extends ImageView{

    private Context mContext;
    private int mImageW;
    private int mImageH;

    public AsusFingerprintFinishImage(Context context) {
        super(context);
        init(context);
    }

    public AsusFingerprintFinishImage(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public AsusFingerprintFinishImage(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public AsusFingerprintFinishImage(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context){
        mContext = context;

        mImageW = mContext.getResources()
                .getDimensionPixelOffset(R.dimen.asus_fingerprint_finish_content_w);
        mImageH = mContext.getResources()
                .getDimensionPixelOffset(R.dimen.asus_fingerprint_finish_content_h);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int specW = MeasureSpec.getSize(widthMeasureSpec);
        int specH = MeasureSpec.getSize(heightMeasureSpec);

        if(specW >= mImageW && specH >= mImageH){
            int width = measureDimension(mImageW, widthMeasureSpec);
            int height = measureDimension(mImageH, heightMeasureSpec);
            setMeasuredDimension(width, height);
            return;
        }

        if(specW == 0 || specH == 0){
            //I don't know why spec width/height is zero
            setMeasuredDimension(mImageW, mImageH);
            return;
        }

        // ++ Scalling for multi window size
        float scaleWidth = (float) specW/ (float) mImageW;
        float scaleHeight = (float) specH / (float) mImageH;
        float scale = Math.min(scaleHeight, scaleWidth);

        int scaledW = (int)(scale * mImageW);
        int scaledH = (int)(scale * mImageH);
        // -- Scalling for multi window size

        int width = measureDimension(scaledW, widthMeasureSpec);
        int height = measureDimension(scaledH, heightMeasureSpec);

        setMeasuredDimension(width, height);
    }


    protected int measureDimension( int defaultSize, int measureSpec ) {

        int result = defaultSize;

        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        }
        else if (specMode == MeasureSpec.AT_MOST) {
            result = Math.min(defaultSize, specSize);
        }
        else {
            result = defaultSize;
        }
        return result;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();


        Bitmap fpImg = drawble2Bitmap(w, h
                , mContext.getDrawable(R.drawable.asus_fingerint_enrollment_finish_icon));
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        canvas.drawBitmap(fpImg, 0, 0, paint);
    }

    private Bitmap drawble2Bitmap(int w, int h, Drawable drawable){
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, w, h);
        drawable.draw(canvas);
        return bitmap;
    }
}
