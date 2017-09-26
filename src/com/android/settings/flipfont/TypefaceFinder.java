package com.android.settings.flipfont;

import com.android.settings.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.pm.ApplicationInfo;
import android.util.Log;

import android.content.ContentResolver;
import android.net.Uri;

/*
 * Font finder that reads font xml files
 */
public class TypefaceFinder {

    // Subdirectory name under assets
    static final String FONT_ASSET_DIR = "xml";

    // font directory inside assets
    static final String FONT_DIRECTORY = "fonts/";

    // Default typeface definition
    public static final String DEFAULT_FONT_VALUE = "default";
    public Context context;

    // List of found typefaces
    public final List<Typeface> mTypefaces = new ArrayList<Typeface>();


    private boolean findTypefacesWithCR(String fontPackageName)
    {
        // find the content resolver - return if none
        ContentResolver cr = null;
        try {
            cr = context.getContentResolver();
        } catch (Exception e) {
            return false;
        }

        // setup the URI to look for xml files (but uses "fonts")
        Uri uriFonts = Uri.parse("content://" + fontPackageName + "/fonts");

        // find the list of XML files for the apk - return if none
        String[] xmlfiles = null;
        String xmlFilesString = "";
        try {
            xmlFilesString = cr.getType(uriFonts);
            if(xmlFilesString != null && !xmlFilesString.isEmpty())
            {
                xmlfiles = xmlFilesString.split("\n");
            }
        } catch (Exception e) {
            return false;
        }
        if (xmlfiles == null || xmlfiles.length == 0)
        {
            return false;
        }

        // for each xml file - add to list
        for (int ix = 0; ix < xmlfiles.length; ix++ ) {
            Uri uriXML = Uri.parse("content://" + fontPackageName + "/xml/" + xmlfiles[ix]);
            try {
                InputStream in = cr.openInputStream(uriXML);
                parseTypefaceXml(xmlfiles[ix], in, fontPackageName);
                in.close();
            } catch (Exception e) {
                // couldn't process that xml file
            }
        }
        return true;
    }

    /*
     * Method to find all installed typefaces.
     * @param assetManager Asset manager from font package.
     */
    public boolean findTypefaces(AssetManager assetManager, String fontPackageName) {

        String[] xmlfiles = null;
        try {
            xmlfiles = assetManager.list(FONT_ASSET_DIR);
        }
        catch (Exception ex) {
            return false;
        }

        // there should always be at least 1 - if none try ContentResolver
        if (xmlfiles.length == 0)
        {
            return findTypefacesWithCR(fontPackageName);
        }

        // Loop and parse each typeface xml file
        int i = 0;
        while (i < xmlfiles.length ) {
            try {
                InputStream in = assetManager.open(FONT_ASSET_DIR + "/" + xmlfiles[i]);
                parseTypefaceXml(xmlfiles[i], in, fontPackageName);
                in.close();
            }
            catch (Exception ex) {
                // not possible to open, continue to next file
            }
            i = i + 1;
        }
        return true;
    }

    /*
     * Parse typeface xml-file to typeface objects
     */
    public void parseTypefaceXml(String xmlFilename, InputStream inStream, String fontPackageName) {
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser sp = spf.newSAXParser();
            XMLReader xr = sp.getXMLReader();

            TypefaceParser fontParser = new TypefaceParser();
            xr.setContentHandler(fontParser);
            xr.parse(new InputSource(inStream));

            Typeface newTypeface = fontParser.getParsedData();

            newTypeface.setTypefaceFilename(xmlFilename);
            newTypeface.setFontPackageName(fontPackageName);
            mTypefaces.add(newTypeface);
        }
        catch (Exception e) {
            // file parsing is not possible, omit this typeface
        }

    }

    // Comparator class to sort Typeface by name
    public class TypefaceSortByName implements Comparator<Typeface>{

        public int compare(Typeface o1, Typeface o2) {
            return o1.getName().compareTo(o2.getName());
        }
    }

    /*
     * Returns all sans fonts for preference list view
     * @param entries Vector containing list entries
     * @param entryValues Vector containing corresponding filenames
     */
    public void getSansEntries(PackageManager packageManager, Vector entries, Vector entryValues, Vector fontPackageName) {

        entries.add((String) context.getResources().getText(R.string.monotype_default_font));
        entryValues.add(DEFAULT_FONT_VALUE);
        fontPackageName.add("");

        // Sort typeface by name
        Collections.sort(mTypefaces, new TypefaceSortByName());

        for (int i=0; i<this.mTypefaces.size(); i++)
        {
            String s = mTypefaces.get(i).getSansName();
            if (s != null)
            {
                // don't check font here - always add and we'll remove later if it fails... faster
                entries.add(s);
                entryValues.add(mTypefaces.get(i).getTypefaceFilename());
                fontPackageName.add(mTypefaces.get(i).getFontPackageName());
            }
        }
    }

    /*
     * Returns all serif fonts for preference list view
     * @param entries Vector containing list entries
     * @param entryValues Vector containing corresponding filenames
     */
    public void getSerifEntries(Vector entries, Vector entryValues) {

        entries.add((String) context.getResources().getText(R.string.monotype_default_font));
        entryValues.add(DEFAULT_FONT_VALUE);

        for (int i=0; i<this.mTypefaces.size(); i++)
        {
            String s = mTypefaces.get(i).getSerifName();
            if (s != null)
            {
                entries.add(s);
                entryValues.add(mTypefaces.get(i).getTypefaceFilename());
            }
        }
    }

    /*
     * Returns all monospace fonts for preference list view
     * @param entries Vector containing list entries
     * @param entryValues Vector containing corresponding filenames
     */
    public void getMonospaceEntries(Vector entries, Vector entryValues) {

        entries.add((String) context.getResources().getText(R.string.monotype_default_font));
        entryValues.add(DEFAULT_FONT_VALUE);

        for (int i=0; i<this.mTypefaces.size(); i++)
        {
            String s = mTypefaces.get(i).getMonospaceName();
            if (s != null)
            {
                entries.add(s);
                entryValues.add(mTypefaces.get(i).getTypefaceFilename());
            }
        }
    }

    /*
     * Find matching typeface
     * @param typefaceFilename String identifying a typeface (xml filename)
     * @return Found typeface or null if there are no such typeface
     */
    public Typeface findMatchingTypeface(String typefaceFilename){

        Typeface typeface = null;

        for (int i=0; i<this.mTypefaces.size(); i++)
        {
            typeface = this.mTypefaces.get(i);
            if (typeface.getTypefaceFilename().equals(typefaceFilename)) {
                break;
            }
        }
        return typeface;
    }

}
