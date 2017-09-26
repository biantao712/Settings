package com.android.settings.flipfont;

import java.util.List;
import java.util.ArrayList;

/*
 * Typeface class
 */
public class Typeface {

    // Name of the typeface
    private String mName = null;

    // Name of the Font package
    private String mFontPackageName = null;

    // Xml filename, unique identification of typeface
    private String mTypefaceFilename = null;

    // List containing sans files
    public final List<TypefaceFile> mSansFonts = new ArrayList<TypefaceFile>();
    // List containing serif files
    public final List<TypefaceFile> mSerifFonts = new ArrayList<TypefaceFile>();
    // List containing monospace files
    public final List<TypefaceFile> mMonospaceFonts = new ArrayList<TypefaceFile>();

    public String getFontPackageName() {
        return mFontPackageName;
    }

    public void setFontPackageName(String name) {
        mFontPackageName = name;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getTypefaceFilename() {
        return mTypefaceFilename;
    }

    public void setTypefaceFilename(String typefaceFilename) {
        mTypefaceFilename = typefaceFilename;
    }

    /*
     * Method to get typeface name
     * @return typeface name if this font is a sans font, otherwise returns null
     */
    public String getSansName() {

        if (mSansFonts.isEmpty()) {
            return null;
        }
        return mName;
    }

    /*
     * Method to get typeface name
     * @return typeface name if this font is a serif font, otherwise returns null
     */
    public String getSerifName() {

        if (mSerifFonts.isEmpty()) {
            return null;
        }
        return mName;
    }

    /*
     * Method to get typeface name
     * @return typeface name if this font is a monospace font, otherwise returns null
     */
    public String getMonospaceName() {

        if (mMonospaceFonts.isEmpty()) {
            return null;
        }
        return mName;
    }
}
