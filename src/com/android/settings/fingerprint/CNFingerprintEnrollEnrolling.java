/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.settings.fingerprint;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Animatable2;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.ChooseLockSettingsHelper;
import com.android.settings.R;

/**
 * Activity which handles the actual enrolling for fingerprint.
 */
public class CNFingerprintEnrollEnrolling extends CNFingerprintEnrollBase
        implements FingerprintEnrollSidecar.Listener {

    public static final String TAG_SIDECAR = "sidecar";

    public static final int PROGRESS_BAR_MAX = 10000;

    private static final int FLASH_DURATION = 250;
    private static final int FINISH_DURATION = 500;
    private static final int FINISH_DELAY = 250;

    /**
     * If we don't see progress during this time, we show an error message to remind the user that
     * he needs to lift the finger and touch again.
     */
    private static final int HINT_TIMEOUT_DURATION = 2500;

    /**
     * How long the user needs to touch the icon until we show the dialog.
     */
    private static final long ICON_TOUCH_DURATION_UNTIL_DIALOG_SHOWN = 500;

    /**
     * How many times the user needs to touch the icon until we show the dialog that this is not the
     * fingerprint sensor.
     */
    private static final int ICON_TOUCH_COUNT_SHOW_UNTIL_DIALOG_SHOWN = 3;

    private ProgressBar mProgressBar;
    private AsusFingerprintImage mFingerprintAnimator;
    private ObjectAnimator mProgressAnim;
    private TextView mContent;
    private TextView mErrorText;
    private Interpolator mFastOutSlowInInterpolator;
    private Interpolator mLinearOutSlowInInterpolator;
    private Interpolator mFastOutLinearInInterpolator;
    private int mIconTouchCount;
    private FingerprintEnrollSidecar mSidecar;
    private boolean mAnimationCancelled;
    private AnimatedVectorDrawable mIconAnimationDrawable;
    private int mIndicatorBackgroundRestingColor;
    private int mIndicatorBackgroundActivatedColor;
    private boolean mRestoring;

    //++ Asus feature
    private static final double DESCRIPTION_UPDATE_RATIO1 = 0.1;
    private static final double DESCRIPTION_UPDATE_RATIO2 = AsusFingerprintImage.INTERRUPT_RATIO;

    private ViewGroup mEnrollingContinue;
    //-- Asus feature

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cn_fingerprint_enroll_enrolling);
        mContent = (TextView) findViewById(R.id.content);
        mErrorText = (TextView) findViewById(R.id.error_text);
        //++ Asus feature
//        mProgressText = (TextView) findViewById(R.id.progress_text);
        mFingerprintAnimator = (AsusFingerprintImage) findViewById(R.id.fingerprint_animator);
        //-- Asus feature
        mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(
                this, android.R.interpolator.fast_out_slow_in);
        mLinearOutSlowInInterpolator = AnimationUtils.loadInterpolator(
                this, android.R.interpolator.linear_out_slow_in);
        mFastOutLinearInInterpolator = AnimationUtils.loadInterpolator(
                this, android.R.interpolator.fast_out_linear_in);
        mFingerprintAnimator.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    mIconTouchCount++;
                    if (mIconTouchCount == ICON_TOUCH_COUNT_SHOW_UNTIL_DIALOG_SHOWN) {
                        showIconTouchDialog();
                    } else {
                        mFingerprintAnimator.postDelayed(mShowDialogRunnable,
                                ICON_TOUCH_DURATION_UNTIL_DIALOG_SHOWN);
                    }
                } else if (event.getActionMasked() == MotionEvent.ACTION_CANCEL
                        || event.getActionMasked() == MotionEvent.ACTION_UP) {
                    mFingerprintAnimator.removeCallbacks(mShowDialogRunnable);
                }
                return true;
            }
        });
        //mIndicatorBackgroundRestingColor
        //        = getColor(R.color.fingerprint_indicator_background_resting);
        //mIndicatorBackgroundActivatedColor
        //        = getColor(R.color.asus_fingerprint_indicator_background_activated);
        mRestoring = savedInstanceState != null;

        //++ Asus feature
        if (mRestoring) {
            mShow = savedInstanceState.getInt(SHOW_NOTIFICATION);
            mLastProgress = savedInstanceState.getInt(LAST_PROGRESS);
        }
//        mProgressText.setText("0%");
        mEnrollingContinue = (ViewGroup)findViewById(R.id.enroll_notification_layout);

        //++ Modify string when device doesn't have vibrator.
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && !vibrator.hasVibrator()) {
//            mStartMessage.setText(R.string.asus_security_settings_fingerprint_enroll_start_message_2);
        }
        //-- Modify string when device doesn't have vibrator.
        //-- Asus feature
    }

    @Override
    protected void onStart() {
        super.onStart();
        mSidecar = (FingerprintEnrollSidecar) getFragmentManager().findFragmentByTag(TAG_SIDECAR);
        if (mSidecar == null) {
            mSidecar = new FingerprintEnrollSidecar();
            getFragmentManager().beginTransaction().add(mSidecar, TAG_SIDECAR).commit();
        }
        mSidecar.setListener(this);
        int progress = updateProgress(false /* animate */);
        updateDescription(progress);
        // ++ Asus feature
        //updateNotification(progress);
        setImageProgress(progress);
        // -- Asus feature
        if (mRestoring) {
            startIconAnimation();
            //++ Fix 100% rotation issue that is finish animation doesn't play
            if(progress == PROGRESS_BAR_MAX && !mFinish){
                playFinishAnimaion();
            }
            //++ Fix 100% rotation issue that is finish animation doesn't play
        }
    }

    @Override
    public void onEnterAnimationComplete() {
        super.onEnterAnimationComplete();
        mAnimationCancelled = false;
        startIconAnimation();
    }

    private void startIconAnimation() {
        //mIconAnimationDrawable.start();
    }

    private void stopIconAnimation() {
        mAnimationCancelled = true;
        //mIconAnimationDrawable.stop();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // ++ recycle fingerprint image
        if(mFingerprintAnimator != null){
            mFingerprintAnimator.recycle();
        }
        // -- recycle fingerprint image
        if (mSidecar != null) {
            mSidecar.setListener(null);
        }
        stopIconAnimation();
        if (!isChangingConfigurations()) {
            if (mSidecar != null) {
                mSidecar.cancelEnrollment();
                getFragmentManager().beginTransaction().remove(mSidecar).commitAllowingStateLoss();
            }
            setResult(RESULT_STOP);
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        if (mSidecar != null) {
            mSidecar.setListener(null);
            mSidecar.cancelEnrollment();
            getFragmentManager().beginTransaction().remove(mSidecar).commitAllowingStateLoss();
            mSidecar = null;
        }
        super.onBackPressed();
    }

    private void animateProgress(int progress) {
        if (mProgressAnim != null) {
            mProgressAnim.cancel();
        }
        ObjectAnimator anim = ObjectAnimator.ofInt(mProgressBar, "progress",
                mProgressBar.getProgress(), progress);
        anim.addListener(mProgressAnimationListener);
        anim.setInterpolator(mFastOutSlowInInterpolator);
        anim.setDuration(250);
        anim.start();
        mProgressAnim = anim;
    }

    private void setImageProgress(int progress){
        mFingerprintAnimator.setProgress(progress);
    }

    //++ Modify api for Asus feature
    private void animateFlash(final int start,final int end) {
        //ValueAnimator anim = ValueAnimator.ofArgb(mIndicatorBackgroundRestingColor,
        //        mIndicatorBackgroundActivatedColor);
        ValueAnimator anim = ValueAnimator.ofInt(start, end);
        final ValueAnimator.AnimatorUpdateListener listener =
                new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                //mFingerprintAnimator.setBackgroundTintList(ColorStateList.valueOf(
                //        (Integer) animation.getAnimatedValue()));
                int value = (Integer) animation.getAnimatedValue();
                mFingerprintAnimator.setProgress(value);
            }
        };
        anim.addUpdateListener(listener);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                //ValueAnimator anim = ValueAnimator.ofArgb(mIndicatorBackgroundActivatedColor,
                //        mIndicatorBackgroundRestingColor);
                //anim.addUpdateListener(listener);
                //anim.setDuration(300);
                //anim.setInterpolator(mLinearOutSlowInInterpolator);
                //anim.start();
                //++ Asus feature
                if(end >= PROGRESS_BAR_MAX) {
                    playFinishAnimaion();
                }
                //-- Asus feature
            }
        });
        //anim.setInterpolator(mFastOutSlowInInterpolator);
        anim.setDuration(FLASH_DURATION);
        anim.start();
    }

    private void playFinishAnimaion(){
        int h = mFingerprintAnimator.getHeight();
        ValueAnimator anim = ValueAnimator.ofInt(h, 0);
        anim.setDuration(FINISH_DURATION);
        anim.addUpdateListener(mFinishUpdateAnimationListener);
        anim.addListener(mFinishAnimationListener);
        anim.start();
        //mProgressText.setText(R.string.finish_button_label);
        //mProgressText.setTextColor(getResources().getColor(R.color.asus_fingerprint_progress_finish));
    }

    private void animateInvalidFlash(final int start,final int end, final boolean recursive) {
        //mFingerprintAnimator.playInvalidFingerprintAnimaion(end, FLASH_DURATION * 2);
        ValueAnimator anim = ValueAnimator.ofInt(start, end);
        final ValueAnimator.AnimatorUpdateListener listener =
                new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        int value = (Integer) animation.getAnimatedValue();
                        setImageProgress(value);
                    }
                };
        anim.addUpdateListener(listener);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if(recursive) {
                    animateInvalidFlash(end, start, false);
                }
            }
        });
        //anim.setInterpolator(mFastOutSlowInInterpolator);
        anim.setDuration(FLASH_DURATION);
        anim.start();
    }
    //-- Modify api for Asus feature

    private void launchFinish(byte[] token) {
        Intent intent = getFinishIntent();
        intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, token);
        if (mUserId != UserHandle.USER_NULL) {
            intent.putExtra(Intent.EXTRA_USER_ID, mUserId);
        }
        startActivity(intent);
        finish();
    }

    protected Intent getFinishIntent() {
        return new Intent(this, CNFingerprintEnrollFinish.class);
    }

    private void updateDescription(int progress) {
        if (mSidecar.getEnrollmentSteps() == -1 || progress <= PROGRESS_BAR_MAX * DESCRIPTION_UPDATE_RATIO1) {
            mContent.setText(R.string.cn_fingerprint_enroll_start_message);
        } else if(progress <= PROGRESS_BAR_MAX * DESCRIPTION_UPDATE_RATIO2) {
            mContent.setText(R.string.cn_fingerprint_enroll_more_message);
        } else  {
            mContent.setText(R.string.cn_fingerprint_enroll_more_message);

        }
    }


    @Override
    public void onEnrollmentHelp(CharSequence helpString) {
        showError(helpString);

        //++ Asus: show invalid fingerprint animaion
        /*if(!mSidecar.isDone()){
            int nextProgress = getProgress(
                mSidecar.getEnrollmentSteps(), mSidecar.getEnrollmentRemaining() - 1);
            if( mShow != NOTIFICATION_EXIT 
                && nextProgress >= PROGRESS_BAR_MAX * DESCRIPTION_UPDATE_RATIO2){
                nextProgress = (int)(PROGRESS_BAR_MAX * DESCRIPTION_UPDATE_RATIO2);
            }
            if(nextProgress == 0){
               nextProgress = (int) (PROGRESS_BAR_MAX * 0.05); // show next progress is 5%.
            }
            animateInvalidFlash(mLastProgress, nextProgress, true);
        }*/
        //--
    }

    @Override
    public void onEnrollmentError(int errMsgId, CharSequence errString) {
        int msgId;
        switch (errMsgId) {
            case FingerprintManager.FINGERPRINT_ERROR_TIMEOUT:
                // This message happens when the underlying crypto layer decides to revoke the
                // enrollment auth token.
                msgId = R.string.security_settings_fingerprint_enroll_error_timeout_dialog_message;
                break;
            default:
                // There's nothing specific to tell the user about. Ask them to try again.
                msgId = R.string.security_settings_fingerprint_enroll_error_generic_dialog_message;
                break;
        }
        showErrorDialog(getText(msgId), errMsgId);
        stopIconAnimation();
        mErrorText.removeCallbacks(mTouchAgainRunnable);
    }

    @Override
    public void onEnrollmentProgressChange(int steps, int remaining) {
        int progress = updateProgress(true /* animate */);
        updateDescription(progress);
        //updateNotification(progress);
        clearError();
        animateFlash(mLastProgress, progress);
        mLastProgress = progress;
        mErrorText.removeCallbacks(mTouchAgainRunnable);
        //Don't show error message when continue button is shown
        if(mShow != NOTIFICATION_SHOW) {
            mErrorText.postDelayed(mTouchAgainRunnable, HINT_TIMEOUT_DURATION);
        }
    }

    private int updateProgress(boolean animate) {
        int progress = getProgress(
                mSidecar.getEnrollmentSteps(), mSidecar.getEnrollmentRemaining());
        //if (animate) {
        //    animateProgress(progress);
        //} else {
        //    mProgressBar.setProgress(progress);
        //}

        int num = (int)(((double) progress / (double) PROGRESS_BAR_MAX) * 100.0);
        String progressText = String.format("%d", num) + "%";
//        mProgressText.setText(progressText);
        return progress;
    }

    private int getProgress(int steps, int remaining) {
        if (steps == -1) {
            return 0;
        }
        int progress = Math.max(0, steps + 1 - remaining);
        return PROGRESS_BAR_MAX * progress / (steps + 1);
    }

    private void showErrorDialog(CharSequence msg, int msgId) {
        ErrorDialog dlg = ErrorDialog.newInstance(msg, msgId);
        dlg.show(getFragmentManager(), ErrorDialog.class.getName());
    }

    private void showIconTouchDialog() {
        mIconTouchCount = 0;
        new IconTouchDialog().show(getFragmentManager(), null /* tag */);
    }

    private void showError(CharSequence error) {
        if(mShow == NOTIFICATION_SHOW){
            return;
        }

        mErrorText.setText(error);
        if (mErrorText.getVisibility() == View.INVISIBLE) {
            mErrorText.setVisibility(View.VISIBLE);
            mErrorText.setTranslationY(getResources().getDimensionPixelSize(
                    R.dimen.fingerprint_error_text_appear_distance));
            mErrorText.setAlpha(0f);
            mErrorText.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(200)
                    .setInterpolator(mLinearOutSlowInInterpolator)
                    .start();
        } else {
            mErrorText.animate().cancel();
            mErrorText.setAlpha(1f);
            mErrorText.setTranslationY(0f);
        }
    }

    private void clearError() {
        if (mErrorText.getVisibility() == View.VISIBLE) {
            mErrorText.animate()
                    .alpha(0f)
                    .translationY(getResources().getDimensionPixelSize(
                            R.dimen.fingerprint_error_text_disappear_distance))
                    .setDuration(100)
                    .setInterpolator(mFastOutLinearInInterpolator)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            mErrorText.setVisibility(View.INVISIBLE);
                        }
                    })
                    .start();
        }

    }


    private final Animator.AnimatorListener mProgressAnimationListener
            = new Animator.AnimatorListener() {

        @Override
        public void onAnimationStart(Animator animation) { }

        @Override
        public void onAnimationRepeat(Animator animation) { }

        @Override
        public void onAnimationEnd(Animator animation) {
            //if (mProgressBar.getProgress() >= PROGRESS_BAR_MAX) {
            //    mProgressBar.postDelayed(mDelayedFinishRunnable, FINISH_DELAY);
            //}
        }

        @Override
        public void onAnimationCancel(Animator animation) { }
    };

    //++ Asus fingerprint progress animation
    private ValueAnimator.AnimatorUpdateListener mFinishUpdateAnimationListener
            = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            int value = (Integer) animation.getAnimatedValue();
            mFingerprintAnimator.setFinishProgress(value);
        }

    };

    private boolean mFinish = false;
    private final Animator.AnimatorListener mFinishAnimationListener
            = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) { }

        @Override
        public void onAnimationRepeat(Animator animation) { }

        @Override
        public void onAnimationEnd(Animator animation) {
            if (mFingerprintAnimator != null) {
                mFingerprintAnimator.postDelayed(mDelayedFinishRunnable, FINISH_DELAY);
                mFinish = true;
            }
        }

        @Override
        public void onAnimationCancel(Animator animation) { }
    };
    //++ Asus fingerprint progress animation

    // Give the user a chance to see progress completed before jumping to the next stage.
    private final Runnable mDelayedFinishRunnable = new Runnable() {
        @Override
        public void run() {
            launchFinish(mToken);
        }
    };

    private final Animatable2.AnimationCallback mIconAnimationCallback =
            new Animatable2.AnimationCallback() {
        @Override
        public void onAnimationEnd(Drawable d) {
            if (mAnimationCancelled) {
                return;
            }

            // Start animation after it has ended.
            mFingerprintAnimator.post(new Runnable() {
                @Override
                public void run() {
                    startIconAnimation();
                }
            });
        }
    };

    private final Runnable mShowDialogRunnable = new Runnable() {
        @Override
        public void run() {
            showIconTouchDialog();
        }
    };

    private final Runnable mTouchAgainRunnable = new Runnable() {
        @Override
        public void run() {
            showError(getString(R.string.asus_security_settings_fingerprint_enroll_lift_touch_again));
        }
    };

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.FINGERPRINT_ENROLLING;
    }

    public static class IconTouchDialog extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.security_settings_fingerprint_enroll_touch_dialog_title)
                    .setMessage(R.string.security_settings_fingerprint_enroll_touch_dialog_message)
                    .setPositiveButton(R.string.security_settings_fingerprint_enroll_dialog_ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
            return builder.create();
        }
    }

    public static class ErrorDialog extends DialogFragment {

        /**
         * Create a new instance of ErrorDialog.
         *
         * @param msg the string to show for message text
         * @param msgId the FingerprintManager error id so we know the cause
         * @return a new ErrorDialog
         */
        static ErrorDialog newInstance(CharSequence msg, int msgId) {
            ErrorDialog dlg = new ErrorDialog();
            Bundle args = new Bundle();
            args.putCharSequence("error_msg", msg);
            args.putInt("error_id", msgId);
            dlg.setArguments(args);
            return dlg;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            CharSequence errorString = getArguments().getCharSequence("error_msg");
            final int errMsgId = getArguments().getInt("error_id");
            builder.setTitle(R.string.security_settings_fingerprint_enroll_error_dialog_title)
                    .setMessage(errorString)
                    .setCancelable(false)
                    .setPositiveButton(R.string.security_settings_fingerprint_enroll_dialog_ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    boolean wasTimeout =
                                        errMsgId == FingerprintManager.FINGERPRINT_ERROR_TIMEOUT;
                                    Activity activity = getActivity();
                                    activity.setResult(wasTimeout ?
                                            RESULT_TIMEOUT : RESULT_FINISHED);
                                    activity.finish();
                                }
                            });
            AlertDialog dialog = builder.create();
            dialog.setCanceledOnTouchOutside(false);
            return dialog;
        }
    }

    // ++ Asus feature
    private static final String SHOW_NOTIFICATION = "show_notification";
    private static final String LAST_PROGRESS = "last_progress";
    private static final int NOTIFICATION_NONE = 0;
    private static final int NOTIFICATION_SHOW = 1;
    private static final int NOTIFICATION_EXIT = 2;
    private int mShow = NOTIFICATION_NONE;
    private int mLastProgress = 0;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SHOW_NOTIFICATION, mShow);
        outState.putInt(LAST_PROGRESS, mLastProgress);
    }

    private void updateNotification(int progress){
        if (progress >= PROGRESS_BAR_MAX * DESCRIPTION_UPDATE_RATIO2) {
            if (mShow == NOTIFICATION_NONE) {
                updateContintueImage(true);
                mShow = NOTIFICATION_SHOW;
            } else if (mShow == NOTIFICATION_SHOW && progress != mLastProgress) {
                updateContintueImage(false);
                mShow = NOTIFICATION_EXIT;
            } else if (mShow == NOTIFICATION_SHOW) {
                updateContintueImage(true);
            }
        }
    }

    private void updateContintueImage(boolean show){
        if(show){
            mEnrollingContinue.setVisibility(View.VISIBLE);
            mFingerprintAnimator.setVisibility(View.INVISIBLE);
            mFingerprintAnimator.showSecondPhaseImage(true, true /*immediately*/);

            clearError();
        } else {
            mEnrollingContinue.setVisibility(View.INVISIBLE);
            mFingerprintAnimator.setVisibility(View.VISIBLE);
        }
    }
    // -- Asus feature

}
