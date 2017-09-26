package com.android.settings.widget;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

import com.android.settings.R;

public class SwitchBarWithFixedTitle extends SwitchBar {
	private TextView mTextView;//for compile error: is not visible

    public SwitchBarWithFixedTitle(Context context) {
        this(context, null);
    }

    public SwitchBarWithFixedTitle(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SwitchBarWithFixedTitle(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SwitchBarWithFixedTitle(
            Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mTextView = (TextView) findViewById(R.id.switch_text);
    }

    /**
     * The {@link SwitchBar#setTextViewLabel(boolean isChecked)} is overridden for
     * keeping the text of the switch bar
     */
    @Override
    public void setTextViewLabel(boolean isChecked) {
    }

    public void setTitle(String title) {
        mTextView.setText(title);
    }

    /**
     * Valid typeface:
     * {@link Typeface#NORMAL} {@link Typeface#BOLD}
     * {@link Typeface#ITALIC} {@link Typeface#BOLD_ITALIC}
     */
    public void setTitleTypeface(int typeface) {
        if (Typeface.NORMAL == typeface || Typeface.BOLD == typeface
                || Typeface.ITALIC == typeface || Typeface.BOLD_ITALIC == typeface)
            mTextView.setTypeface(null, typeface);
    }

    public int getTextViewPaddingStart() {
        return mTextView.getPaddingStart();
    }

    public int getTextViewPaddingTop() {
        return mTextView.getPaddingTop();
    }

    public int getTextViewPaddingEnd() {
        return mTextView.getPaddingEnd();
    }

    public int getTextViewPaddingBottom() {
        return mTextView.getPaddingBottom();
    }

    public void setTextViewPaddingRelative(int start, int top, int end, int bottom) {
        mTextView.setPaddingRelative(start, top, end, bottom);
    }
}
