package com.android.settings.flipfont;

/*
 * Class containing typeface filenames
 */
public class TypefaceFile {

    // actual ttf filename
    private String fileName = null;

    // droidname that this file should replace
    private String droidName = null;

    public TypefaceFile() {

    }

    public TypefaceFile(String fileName, String droidName) {
        this.fileName = fileName;
        this.droidName = droidName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getDroidName() {
        return droidName;
    }

    public void setDroidName(String droidName) {
        this.droidName = droidName;
    }

    public String toString() {
        return "Filename = " + this.fileName + "\nDroidname = " + this.droidName;
    }
}
