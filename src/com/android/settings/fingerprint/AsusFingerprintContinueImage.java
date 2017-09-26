package com.android.settings.fingerprint;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

import com.android.settings.R;

import java.util.ArrayList;

/**
 * Created by yueting-wong on 2016/7/19.
 */
public class AsusFingerprintContinueImage extends ImageView {

    private Context mContext;

    private final float BTN_BASE_RATION = 1f;
    private final float BTN_OUTSIDE_RATION = 1.15f;
    private final float BTN_INSIDE_RATION = 1.45f;

    private final float BTN_OUTSIDE_ALPHA_1 = 0.7f;
    private final float BTN_OUTSIDE_ALPHA_2 = 0.1f;
    private final float BTN_INSIDE_ALPHA_1 = 1f;
    private final float BTN_INSIDE_ALPHA_2 = 0f;

    private final int BTN_OUTSIDE_DURATION_1 = 500;
    private final int BTN_OUTSIDE_DURATION_2 = 700;
    private final int BTN_INSIDE_DURATION_1 = 1100;

    private int mCycleOutsideRadius;
    private int mCycleInsideRadius;
    private int mCycleW;
    private int mCycleH;
    private int mImageW;
    private int mImageH;
    private int mScaled = -1;

    private ArrayList<Cycle> mCycleList = new ArrayList<Cycle>();
    private Drawable mOutsideDrawable;
    private Drawable mInsideDdrawable;
    private Drawable mCentralDrawable;
    private Bitmap mCentralBitmap;

    private Paint mPaint;
    private int mFramesPerSecond = 120;

    public AsusFingerprintContinueImage(Context context) {
        super(context);
        init(context);
    }

    public AsusFingerprintContinueImage(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public AsusFingerprintContinueImage(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public AsusFingerprintContinueImage(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        mContext = context;

        mCycleInsideRadius = mContext.getResources()
                .getDimensionPixelOffset(R.dimen.asus_fingerprint_enrolling_continue_btn_inside_radius);
        mCycleOutsideRadius = mContext.getResources()
                .getDimensionPixelOffset(R.dimen.asus_fingerprint_enrolling_continue_btn_outside_radius);

        mImageW = (int)(mContext.getResources()
                .getDimensionPixelOffset(R.dimen.asus_fingerprint_enrolling_continue_image_w));
        mImageH = (int)(mContext.getResources()
                .getDimensionPixelOffset(R.dimen.asus_fingerprint_enrolling_continue_image_h));

        mCycleW = mContext.getResources()
                .getDimensionPixelOffset(R.dimen.asus_fingerprint_enrolling_continue_btn_w);
        mCycleH = mContext.getResources()
                .getDimensionPixelOffset(R.dimen.asus_fingerprint_enrolling_continue_btn_h);

        mInsideDdrawable = mContext.getDrawable(R.drawable.asus_fingerprint_motion_inside);
        mOutsideDrawable = mContext.getDrawable(R.drawable.asus_fingerprint_motion_outside);
        mCentralDrawable = mContext.getDrawable(R.drawable.asus_fingerprint_motion_central);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
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

        // ++ Scalling for multi window size
        float scaleWidth = (float) specW/ (float) mImageW;
        float scaleHeight = (float) specH / (float) mImageH;
        float scale = Math.min(scaleHeight, scaleWidth);

        int scaledW = (int)(scale * mImageW);
        int scaledH = (int)(scale * mImageH);
        // -- Scalling for multi window size

        int width = measureDimension(mImageW, widthMeasureSpec);
        int height = measureDimension(mImageH, heightMeasureSpec);
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
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Cycle c1 = new Cycle(Cycle.TYPE_OUTSIDE);
        Cycle c2 = new Cycle(Cycle.TYPE_INSIDE);
        mCycleList.add(c1);
        mCycleList.add(c2);

    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mCycleList.size() > 0) {
            for (int i = 0; i < mCycleList.size(); i++) {
                mCycleList.get(i).cancel();
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();

        float scaleWidth = (float) w / (float) mImageW;
        float scaleHeight = (float) h / (float) mImageH;
        float scale = Math.min(scaleHeight, scaleWidth);

        int l = canvas.saveLayer(0, 0, w, h, null, Canvas.ALL_SAVE_FLAG);
        for (int i = 0; i < mCycleList.size(); i++) {
            Cycle c = mCycleList.get(i);

            int cycleR = (int) c.getRadius();
            Drawable drawable = c.getType() == Cycle.TYPE_INSIDE ?
                    mInsideDdrawable :
                    mOutsideDrawable;
            mPaint.setAlpha((int) (255 * c.getAlpha()));

            int cycleRadiusScaled = (int) (cycleR * scale * 2);
            Bitmap cycleImg = drawble2Bitmap(cycleRadiusScaled, cycleRadiusScaled, drawable);
            canvas.drawBitmap(cycleImg, w / 2 - cycleImg.getWidth() / 2, h / 2 - cycleImg.getHeight() / 2, mPaint);
        }

        Bitmap centralBitmap = drawble2Bitmap((int) (mCycleW * scale), (int) (mCycleH* scale), mCentralDrawable);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        canvas.drawBitmap(centralBitmap, w / 2 - centralBitmap.getWidth() / 2, h / 2 - centralBitmap.getHeight() / 2, paint);
        canvas.restoreToCount(l);
        postInvalidateDelayed(1000 / mFramesPerSecond);
    }

    private Bitmap drawble2Bitmap(int w, int h, Drawable drawable){
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, w, h);
        drawable.draw(canvas);
        return bitmap;
    }

    private class Cycle {
        private AnimatorSet mSet;
        public static final int TYPE_INSIDE = 1;
        public static final int TYPE_OUTSIDE = 2;
        private int mType;
        private float mRadius;
        private float mAlpha;

        public Cycle(int type) {
            mType = type;
            startAnimation();
        }

        public int getType(){
            return mType;
        }

        public float getRadius() {
            return mRadius;
        }

        public float getAlpha() {
            return mAlpha;
        }

        public void cancel(){
            removeCallbacks(mStartAnimationRunnable);
            if(mSet != null) {
                mSet.cancel();
            }
        }

        private final Runnable mStartAnimationRunnable = new Runnable() {
            @Override
            public void run() {
                startAnimation();
            }
        };

        private void startAnimation(){
            AnimatorSet myAnimatorSet = null;
            if(mType == TYPE_INSIDE) {
                myAnimatorSet = createInsideAnimaion(BTN_INSIDE_ALPHA_1, BTN_INSIDE_ALPHA_2,
                        BTN_BASE_RATION * mCycleInsideRadius, BTN_INSIDE_RATION * mCycleInsideRadius,
                        BTN_INSIDE_DURATION_1);

            }else{
                myAnimatorSet = new AnimatorSet();
                AnimatorSet set1 = createInsideAnimaion(BTN_OUTSIDE_ALPHA_1, BTN_OUTSIDE_ALPHA_2,
                        BTN_BASE_RATION * mCycleOutsideRadius, BTN_OUTSIDE_RATION * mCycleOutsideRadius,
                        BTN_OUTSIDE_DURATION_1);
                AnimatorSet set2 = createInsideAnimaion(BTN_OUTSIDE_ALPHA_2, BTN_OUTSIDE_ALPHA_1,
                        BTN_OUTSIDE_RATION* mCycleOutsideRadius, BTN_BASE_RATION  * mCycleOutsideRadius,
                        BTN_OUTSIDE_DURATION_2);
                myAnimatorSet.play(set1).after(set2);
            }
            myAnimatorSet.addListener(new AnimatorListenerAdapter() {
                private boolean mCanceled;
                @Override
                public void onAnimationStart(Animator animation) {
                    mCanceled = false;
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    mCanceled = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mSet = null;
                    if (!mCanceled) {
                        post(mStartAnimationRunnable);
                    }
                }

            });
            myAnimatorSet.start();
            mSet = myAnimatorSet;
        }

        private AnimatorSet createInsideAnimaion(float alpha1, float alpha2, float scale1, float scale2, int duration) {
            ValueAnimator alpha_anim = ValueAnimator.ofFloat(alpha1, alpha2);
            final ValueAnimator.AnimatorUpdateListener listener1 =
                    new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            mAlpha = (Float) animation.getAnimatedValue();
                        }
                    };
            alpha_anim.addUpdateListener(listener1);
            alpha_anim.setDuration(duration);

            ValueAnimator scale_anim = ValueAnimator.ofFloat(scale1, scale2);
            final ValueAnimator.AnimatorUpdateListener listener2 =
                    new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            mRadius = (Float) animation.getAnimatedValue();
                        }
                    };
            scale_anim.addUpdateListener(listener2);
            scale_anim.setDuration(duration);

            AnimatorSet set = new AnimatorSet();
            set.playTogether(alpha_anim, scale_anim);
            return set;
        }
    }

    public void recycle(){
        if(mCentralBitmap != null){
            mCentralBitmap.recycle();
            mCentralBitmap = null;
        }
    }
}
