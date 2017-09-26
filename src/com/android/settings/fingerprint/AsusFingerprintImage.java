package com.android.settings.fingerprint;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

import com.android.settings.R;

public class AsusFingerprintImage extends ImageView{

    private Context mContext;

    private int mFinishMask;
    private int mRatio;
    private int mNextRatio;
    private int mLastRatio;
    private int mNextProgressAlpha;

    private int mImageW;
    private int mImageH;
    private int mContentW;
    private int mContentH;

    private boolean mDrawFinishMask = false;
    private boolean mDrawInvalidProgress = false;
    private boolean mShowSecondPhaseImage = false;

    private Bitmap mBitmap;
    private Bitmap mBackgound;

    private int[] mFingerprintStepId = {
            R.drawable.asus_fingerint_enrollment_step_0,
            R.drawable.asus_fingerint_enrollment_step_1,
            R.drawable.asus_fingerint_enrollment_step_2,
            R.drawable.asus_fingerint_enrollment_step_3,
            R.drawable.asus_fingerint_enrollment_step_4,
            R.drawable.asus_fingerint_enrollment_step_5,
            R.drawable.asus_fingerint_enrollment_step_6,
            R.drawable.asus_fingerint_enrollment_step_7,
            R.drawable.asus_fingerint_enrollment_step_8,
            R.drawable.asus_fingerint_enrollment_step_9,
            R.drawable.asus_fingerint_enrollment_step_10,
            R.drawable.asus_fingerint_enrollment_step_11,
            R.drawable.asus_fingerint_enrollment_step_12,
            R.drawable.asus_fingerint_enrollment_step_13,
            R.drawable.asus_fingerint_enrollment_step_14,
            R.drawable.asus_fingerint_enrollment_step_15,
            R.drawable.asus_fingerint_enrollment_step_16,
            R.drawable.asus_fingerint_enrollment_step_17,
            R.drawable.asus_fingerint_enrollment_step_18,
            R.drawable.asus_fingerint_enrollment_step_19,
            R.drawable.asus_fingerint_enrollment_step_20,
            R.drawable.asus_fingerint_enrollment_step_21,
            R.drawable.asus_fingerint_enrollment_step_22,
            R.drawable.asus_fingerint_enrollment_step_23,
            R.drawable.asus_fingerint_enrollment_step_23, // Continue image show on this index, no need show step 24.
            R.drawable.asus_fingerint_enrollment_step_25, // Continue image disappear on this index
            R.drawable.asus_fingerint_enrollment_step_26,
            R.drawable.asus_fingerint_enrollment_step_27,
            R.drawable.asus_fingerint_enrollment_step_28,
            R.drawable.asus_fingerint_enrollment_step_29,
            R.drawable.asus_fingerint_enrollment_step_30,
            R.drawable.asus_fingerint_enrollment_step_31,
            R.drawable.asus_fingerint_enrollment_step_32,
            R.drawable.asus_fingerint_enrollment_step_33,
            R.drawable.asus_fingerint_enrollment_step_34,
            R.drawable.asus_fingerint_enrollment_step_35};

    public static final int INTERRUPT_STEP = 26;
    public static final double INTERRUPT_RATIO = 25f / 36f;

    public AsusFingerprintImage(Context context) {
        super(context);
        init(context);
    }

    public AsusFingerprintImage(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public AsusFingerprintImage(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public AsusFingerprintImage(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context){
        mContext = context;
        mFinishMask = getHeight();

        mImageW = mContext.getResources()
                .getDimensionPixelOffset(R.dimen.asus_fingerprint_enrolling_continue_image_w);
        mImageH = mContext.getResources()
                .getDimensionPixelOffset(R.dimen.asus_fingerprint_enrolling_continue_image_h);

        mContentW = mContext.getResources()
                .getDimensionPixelOffset(R.dimen.asus_fingerprint_enrolling_content_w);
        mContentH = mContext.getResources()
                .getDimensionPixelOffset(R.dimen.asus_fingerprint_enrolling_content_h);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int specW = MeasureSpec.getSize(widthMeasureSpec);
        int specH = MeasureSpec.getSize(heightMeasureSpec);

        if( specW >= mContentW && specH >= mContentH){
            int width = measureDimension(mContentW, widthMeasureSpec);
            int height = measureDimension(mContentH, heightMeasureSpec);
            setMeasuredDimension(width, height);
            return;
        }

        if(specW == 0 || specH == 0){
            //I don't know why spec width/height is zero
            setMeasuredDimension(mContentW, mContentH);
            return;
        }

        // ++ Scalling for multi window size
        float scaleWidth = (float) specW/ (float) mContentW;
        float scaleHeight = (float) specH / (float) mContentH;
        float scale = Math.min(scaleHeight, scaleWidth);

        int scaledW = (int)(scale * mContentW);
        int scaledH = (int)(scale * mContentH);
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
        int l = canvas.saveLayer(0, 0, w, h, null, Canvas.ALL_SAVE_FLAG);

        //Create fingerprint
        drawFingerprintProgress(255 /*[0..255]*/, w, h, canvas);

        //draw Finish mask
        if(mDrawFinishMask) {
            Paint paint = new Paint();
            paint.setColor(mContext.getColor(R.color.asus_fingerprint_progress_finish));
            paint.setAlpha(200); //Range[0..255]
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            canvas.drawRect(0, mFinishMask, w, h, paint);
        }
        canvas.restoreToCount(l);
    }

    public double progressMapStep(double s){
        double a1 = 0;
        double a2 = FingerprintEnrollEnrolling.PROGRESS_BAR_MAX;
        double b1 = 0;
        double b2 = mFingerprintStepId.length -1;
        return b1 + ((s - a1)*(b2 - b1))/(a2 - a1);
    }

    private void createProgressBitmap(int w, int h){
        int step = ((int) progressMapStep(mRatio));
        if(step >= mFingerprintStepId.length){
            step = mFingerprintStepId.length - 1;
        }

        if(mBitmap == null || mRatio != mLastRatio){
            if(mShowSecondPhaseImage && step <= INTERRUPT_STEP){
                mBitmap = drawble2Bitmap(w, h, mContext.getDrawable(mFingerprintStepId[INTERRUPT_STEP]));
            }else{
                mBitmap = drawble2Bitmap(w, h, mContext.getDrawable(mFingerprintStepId[step]));
            }
           mLastRatio = mRatio;
        }

        int nextStep = ((int) progressMapStep(mNextRatio));
        if(nextStep >= mFingerprintStepId.length){
            nextStep = mFingerprintStepId.length - 1;
        }
    }

    private void drawFingerprintProgress(int alpha, int w, int h, Canvas canvas){
        createProgressBitmap(w, h);
        canvas.saveLayer(0, 0, w, h, null, Canvas.ALL_SAVE_FLAG);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setAlpha(alpha);
        canvas.drawBitmap(mBitmap, 0, 0, paint);
        canvas.restore();
    }

    private Bitmap drawble2Bitmap(int w, int h, Drawable drawable){
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, w, h);
        drawable.draw(canvas);
        return bitmap;
    }

    public void setProgress(int progress){
        if(mRatio != progress) {
            mRatio = progress;
            invalidate();
        }
    }

    public void setFinishProgress(int progress){
        mDrawFinishMask = true;
        mFinishMask = progress;
        invalidate();
    }

    public void playInvalidFingerprintAnimaion(int progress, int duration){
        mNextRatio = progress;
        mDrawInvalidProgress = true;
        ValueAnimator anim = ValueAnimator.ofInt(255, 0); //Alpha 255 -> 0
        final ValueAnimator.AnimatorUpdateListener listener =
                new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mNextProgressAlpha = (Integer) animation.getAnimatedValue();
                invalidate();
            }
        };
        anim.addUpdateListener(listener);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mDrawInvalidProgress = false;
                invalidate();
            }
        });
        anim.setDuration(duration);
        anim.start();
    }

    public void showSecondPhaseImage(boolean show, boolean immediately){
        mShowSecondPhaseImage = show;
        if(immediately) {
            invalidate();
        }
    }

    public void recycle(){
        if(mBitmap != null) {
            mBitmap.recycle();
            mBitmap = null;
        }
        if(mBackgound != null){
            mBackgound.recycle();
            mBackgound = null;
        }
    }
}
