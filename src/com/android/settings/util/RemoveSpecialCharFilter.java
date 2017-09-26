package com.android.settings.util;

import android.text.InputFilter;
import android.text.Spanned;

public class RemoveSpecialCharFilter implements InputFilter {

    private boolean mAllowAsciiChars = false;

    public RemoveSpecialCharFilter (boolean allowAsciiChars) {
        mAllowAsciiChars = allowAsciiChars;
    }

    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        int firstNonSpaceIndex = start;
        boolean hasInvalidToken = false;
        StringBuilder out = new StringBuilder();
        for (int i = start; i < end; i++) {
            char cur = source.charAt(i);
            // strip leading spaces
            if ((dstart == 0) && (i == firstNonSpaceIndex) && (cur == ' ')) {
                firstNonSpaceIndex++;
                hasInvalidToken = true;
            } else if (Character.isDigit(cur) || Character.isLetter(cur)) {
                out.append(cur);
            } else if (!mAllowAsciiChars && (cur == ' ' || cur == '-' || cur == '_')) {
                out.append(cur);
            } else if (mAllowAsciiChars && (cur >= 0x20 && cur <= 0x7E)) {
                out.append(cur);
            } else {
                hasInvalidToken = true;
            }
        }
        if (hasInvalidToken) {
            return out.toString();
        }
        return null;
    }
}
