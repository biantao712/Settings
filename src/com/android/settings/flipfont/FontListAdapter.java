package com.android.settings.flipfont;

import com.android.settings.R;

import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.view.Display;
import android.view.WindowManager;
import android.util.DisplayMetrics;
import java.io.File;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import android.content.ContentResolver;
import android.net.Uri;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;

/*
 * FontListAdapter provides the installed fonts to list components.
 */

public class FontListAdapter extends BaseAdapter {

    // Parent context
    Context context = null;

    // Inflater used to create the font list appearance.
    LayoutInflater mInflater = null;

    // Context package manager
    public PackageManager mPackageManager = null;

    // Used to load the fonts from other application apk
    public AssetManager mFontAssetManager = null;

    // Typeface finder and typeface array handler
    public TypefaceFinder mTypefaceFinder = new TypefaceFinder();

    // Contains the font package names that are shown on the list
    public Vector mFontPackageNames = new Vector();

    // Contains the font names that are shown on the list
    public Vector mFontNames = new Vector();

    // Contains xml-filenames that specify the font
    public Vector mTypefaceFiles = new Vector();

    // Contains the typefaces loaded from assets
    private Vector mTypefaces = new Vector();

    // List of installed applications, used for getting installed fontst
    public List<ApplicationInfo> mInstalledApplications;

    // Font package
    static final String FONT_PACKAGE = "com.monotype.android.font.";
    // font directory inside assets
    static final String FONT_DIRECTORY = "fonts/";

    // Android default system font
    private Typeface droidSansFont = null;

    static final String FONT_DROID_SANS = "DroidSans.ttf";

    // OEM font names
    public static final String OEM_FONT1 = "OEM Font1";
    public static final String OEM_FONT2 = "OEM Font2";
    public static final String OEM_FONT3 = "OEM Font3";

    /*
     * FontListAdapter constructor. Tries to load installed fonts.
     */
    FontListAdapter(Context context) {
        super();
        this.context = context;
        mTypefaceFinder.context = context;
        mInflater = (LayoutInflater)context.getSystemService(
                  Context.LAYOUT_INFLATER_SERVICE);

        mPackageManager = context.getPackageManager();

        try {
            this.mInstalledApplications = mPackageManager.getInstalledApplications(PackageManager.GET_META_DATA);

            // Find all installed fonts using the package names
            String fontPackageName = null;
            for (int i = 0; i < this.mInstalledApplications.size(); i++) {
                fontPackageName = this.mInstalledApplications.get(i).packageName;
                if (fontPackageName.startsWith(FONT_PACKAGE)) {
                        try{
                            ApplicationInfo appInfo = mPackageManager.getApplicationInfo(fontPackageName, PackageManager.GET_META_DATA);
                            appInfo.publicSourceDir = appInfo.sourceDir;
                            Resources res = mPackageManager.getResourcesForApplication(appInfo);

                            this.mFontAssetManager = res.getAssets();
                            mTypefaceFinder.findTypefaces(mFontAssetManager,fontPackageName);
                        }
                        catch (Exception e) {
                        }
               }
            }

            mTypefaceFinder.getSansEntries(mPackageManager, mFontNames, mTypefaceFiles, mFontPackageNames);
        }
        catch (Exception e) {
            // font package not found, just use default fonts
        }

        // This is the default android font
        File f = new File("/system/fonts/UIFont.ttf");
        if(f.exists()) {
            this.droidSansFont = Typeface.createFromFile("/system/fonts/UIFont.ttf");
        }
        else {
            this.droidSansFont = Typeface.createFromFile("/system/fonts/" + FONT_DROID_SANS);
        }
    }

    /*
     * Returns the number of available fonts.
     */
    public int getCount() {
        return mFontNames.size();
    }

    /*
     * (non-Javadoc)
     * @see android.widget.Adapter#getItem(int)
     */
    public Object getItem(int position) {
        return mTypefaceFiles.elementAt(position);
    }

    /*
     * (non-Javadoc)
     * @see android.widget.Adapter#getItemId(int)
     */
    public long getItemId(int position) {
        return position;
    }

    /*
     * Returns the actual user readable font name from given position.
     */
    public String getFontName(int position) {
        // Check the name to see if it is an OEM font.
        // If it is use the name in the resource
        String tmpString = (String)mFontNames.elementAt(position);

        if (tmpString.equals(OEM_FONT1))
            tmpString = (String) context.getResources().getText(R.string.monotype_dialog_font_oem_font1);
        if (tmpString.equals(OEM_FONT2))
            tmpString = (String) context.getResources().getText(R.string.monotype_dialog_font_oem_font2);
        if (tmpString.equals(OEM_FONT3))
            tmpString = (String) context.getResources().getText(R.string.monotype_dialog_font_oem_font3);

        return tmpString;
    }

    /*
     * (non-Javadoc)
     * @see android.widget.Adapter#getView(int, android.view.View, android.view.ViewGroup)
     */
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView v;

        if (convertView == null) {
            v = (TextView) mInflater.inflate(android.R.layout.simple_list_item_single_choice, null);
        }
        else {
            v = (TextView) convertView;
        }

        v.setText(getFontName(position));
         setFont(position, v);

         // Get density dpi from window manager
         WindowManager wm = (WindowManager)this.context.getSystemService(Context.WINDOW_SERVICE);
         DisplayMetrics metrics = new DisplayMetrics();
         wm.getDefaultDisplay().getMetrics(metrics);
         int density = metrics.densityDpi;

         // The set height of the TextView to 40 dips
         int pixelHeight = (int)(65 * (float)((float)density / 160));
         v.setHeight(pixelHeight);

         return v;
    }

    /*
     * Set font to given TextView
     */
    private void setFont(int position, TextView textView) {
        Typeface fontTypeface = (Typeface) this.mTypefaces.elementAt(position);
        if (fontTypeface != null) {
            textView.setTypeface(fontTypeface);
        }
    }

    /*
     * Loads the typefaces from asset
     */
    public void loadTypefaces() {
        String fontfile = null;
        String fontPackageName = null;
        Typeface newTypeface = null;

        mTypefaces.add(this.droidSansFont);

        int i = 1;
        while (i < mTypefaceFiles.size()) {
            fontfile = mTypefaceFiles.elementAt(i).toString();
            fontPackageName = mFontPackageNames.elementAt(i).toString();
            newTypeface = getFont(fontfile, fontPackageName);

            // if the typeface didn't load - remove it from the lists (instead of pre-checking in getSansEntries)
            if (newTypeface == null)
            {
                // remove the entry from all vector lists
                mFontPackageNames.remove(i);
                mFontNames.remove(i);
                mTypefaceFiles.remove(i);

                continue;
            }

            mTypefaces.add(newTypeface);
            i++;
        }
    }

    /*
     *  Loads a new typeface using the ContentResolver
     */
    private Typeface getFontfromCR(String sFont, String sPackageName)
    {
        // find the content resolver - return if none
        ContentResolver cr = null;
        try {
            cr = context.getContentResolver();
        } catch (Exception e) {
            return null;
        }

        // set up the font URI to open
        Uri uriFont = Uri.parse("content://" + sPackageName + "/fonts/" + sFont);
        InputStream isFont = null;
        try {
            isFont = cr.openInputStream(uriFont);
        } catch (Exception e) {
            return null;
        }

        // create a temp local font file
        File fTemp = null;
        try {
            fTemp = File.createTempFile("font", null);
        } catch (IOException e) {
            try {
                isFont.close();
            } catch (Exception e1) {
            }
            return null;
        }

        // copy the font data over from the ContentResolver stream to the temp file
        try {
            FileOutputStream fOut = new FileOutputStream(fTemp);
            BufferedOutputStream os = new BufferedOutputStream(fOut);

            byte[] b = new byte[1024];

            int read = 0;
            while ((read = isFont.read(b)) > 0) {
                os.write(b, 0, read);
            }
            os.flush();
            fOut.flush();
            os.close();
            fOut.close();
            isFont.close();
        } catch (FileNotFoundException e) {
            return null;
        } catch (IOException e) {
            return null;
        }

        // create the typeface and delete the temp file created above
        Typeface tf = Typeface.createFromFile(fTemp);
        fTemp.delete();

        return tf;
    }

    /*
     *  Loads a new typeface using the xmlfilename.
     *  Replace the ".xml" with ".ttf" to get the font filename
     */
    private Typeface getFont(String typeface, String fontPackageName) {
        // replace ".xml" in filename with ".ttf"
        String fontName = typeface.replace(".xml", ".ttf");

        try {
            ApplicationInfo appInfo = mPackageManager.getApplicationInfo(fontPackageName, PackageManager.GET_META_DATA);
            appInfo.publicSourceDir = appInfo.sourceDir;
            Resources res = mPackageManager.getResourcesForApplication(appInfo);

            this.mFontAssetManager = res.getAssets();
        }
        catch (Exception e) {
            // font package not found - return null and the font will be removed
            return null;
        }

        // try to open/close the font file - if you can't, try using a ContentResolver
        try {
            InputStream is = this.mFontAssetManager.open(FONT_DIRECTORY + fontName);
            is.close();
        } catch (IOException e) {
            return getFontfromCR(fontName, fontPackageName);
        }

        return Typeface.createFromAsset(this.mFontAssetManager, FONT_DIRECTORY + fontName);
    }
}

