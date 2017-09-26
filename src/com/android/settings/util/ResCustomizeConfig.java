package com.android.settings.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.os.SystemProperties;
import android.os.Build;
import android.util.Log;
import android.util.Xml;

public class ResCustomizeConfig {
    private static final boolean DEBUG = false;
    private static final String TAG = "ResCustomizeConfig";
    private static final String REGULATORY_FILE_NAME = "regulatory_info.png";
    private static final String CONFIG_FILE_NAME = "config.xml";
    private static final String RES_CUSTOMIZE_ETC_ROOT_PATH = "/system/etc/SettingsRes";
    private static final String MODEL_NUMBER = Build.MODEL.toLowerCase();
    private static final String CID = SystemProperties.get("ro.config.CID").toLowerCase();
    private static final String COUNTRY_CODE = Build.COUNTRYCODE.toLowerCase();
    private static final String SKU = Build.ASUSSKU.toLowerCase();
    private static final String ELABEL_FILE_NAME = COUNTRY_CODE + "_" + CID + "_" + MODEL_NUMBER +
            "_" + REGULATORY_FILE_NAME;
    private static final String WWSKU_DEFAULT_ELABEL_FILE_NAME = "ww_asus_" + MODEL_NUMBER + "_" +
            REGULATORY_FILE_NAME;
    private static final String ASUS_CID = "asus";
    private static final String WW_SKU = "ww";
    private static final Map<String,String> sProperties = new HashMap<String,String>();

    /**
     * get customize configure value from /system/etc/SettingsRes/config.xml
     * @param key
     * @param def default value
     * @return configure string
     */
    public static String getProperty(String key, String def) {
        if (key == null || !sProperties.containsKey(key))
            return def;
        if (DEBUG) Log.d(TAG, "getProperty: " + key);
        return sProperties.get(key);
    }

    public static String getSettingsResFolderPath() {
        return RES_CUSTOMIZE_ETC_ROOT_PATH;
    }

    public static String getRegulatoryImage() {
        //in WW-sku, when CID=ASUS, show the default e-label with CountryCode=ww
        if (SKU.equals(WW_SKU) && CID.equals(ASUS_CID)) {
            return WWSKU_DEFAULT_ELABEL_FILE_NAME;
        }
        return ELABEL_FILE_NAME;
    }

    public static boolean isDirectory(String path) {
        return new File(path).isDirectory();
    }

    public static boolean isDirectory(File file) {
        return file.isDirectory();
    }

    public static boolean isFile(String path) {
        return new File(path).isFile();
    }

    public static boolean isFile(File file) {
        return file.isFile();
    }

    private static File[] getFileList(String path) {
        File f = new File(path);
        return f.listFiles();
    }

    public static boolean isFileExist(String filename) {
        File f = new File(RES_CUSTOMIZE_ETC_ROOT_PATH+"/"+filename);
        if (DEBUG) Log.d(TAG, "is " +filename + " exist: " + String.valueOf(f.exists()));
        return f.exists();
    }

    public static boolean isShowRegulatory(){
        //in WW-sku, when CID=ASUS, show the default e-label with CountryCode=ww
        if (SKU.equals(WW_SKU) && CID.equals(ASUS_CID)) {
            return isFileExist(WWSKU_DEFAULT_ELABEL_FILE_NAME);
        }
        return isFileExist(ELABEL_FILE_NAME);
    }

    public static boolean hasConfigFile(){
        return isFileExist(CONFIG_FILE_NAME);
    }

    /**
     * use to get settings configure file
     *
     * @param fileName
     * @return xml file if found, else return null
     */
   public static File getXMLFile(String fileName) {
       File[] fileList = getFileList(RES_CUSTOMIZE_ETC_ROOT_PATH);
       if (fileList == null) {
           return null;
       }
       for (File file : fileList) {
           if (file.getName().equals(fileName)) {
               if (DEBUG) Log.d(TAG,"getXMLFile: "+file.getName()+", URI= "+file.toURI());
               return file;
           }
       }
       return null;
   }

    /**
     * use to get settings customized resource
     *
     * @param imageName
     * @return resource file if found, else return null
     */
    public static String getImagePath(String imageName) {
        File[] fileList = getFileList(RES_CUSTOMIZE_ETC_ROOT_PATH);
        if(fileList != null) {
            for (File file : fileList) {
                if (file.getName().equals(imageName)) {
                    if (DEBUG) Log.d(TAG, "getImageFile: " + file.getName() + ", URI= " + file.toURI());
                    return file.toString();
                }
            }
        }
        return null;
    }

    public static String getRegulatoryPath(){
        return isShowRegulatory() ? ResCustomizeConfig.getImagePath(getRegulatoryImage()) : null;
    }

    public static void parsingConfig() {
        final int configName = 0;
        final String startTag = "config";
        File file = getXMLFile(CONFIG_FILE_NAME);
        FileInputStream fileIn = null;
        InputStream in = null;
        try {
            fileIn = new FileInputStream(file);
            in = new BufferedInputStream(fileIn);
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(in, null);
            if (DEBUG) Log.d(TAG, "parsing file");
            while (parser.next() != XmlPullParser.START_TAG) {
                // Do nothing
            }
            parser.next();
            while (parser.getEventType() != XmlPullParser.END_TAG) {
                while (parser.getEventType() != XmlPullParser.START_TAG) {
                    if (parser.getEventType() == XmlPullParser.END_DOCUMENT) {
                        return;
                    }
                    parser.next();
                }
                if (parser.getName().equals(startTag)) {
                    String key = parser.getAttributeValue(configName);
                    String value = parser.nextText();
                    if (DEBUG) Log.d(TAG, "key: " + key + ", value: " + value);
                    sProperties.put(key, value);
                }
                while (parser.getEventType() != XmlPullParser.END_TAG) {
                    parser.next();
                }
                parser.next();
            }
        } catch (XmlPullParserException e) {
            // TODO Auto-generated catch block
            Log.e(TAG,
                    "Got XmlPullParserException while parsing customized config.",
                    e);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            Log.e(TAG,
                    "Got FileNotFoundException while parsing customized config.",
                    e);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            Log.e(TAG, "Got IOException while parsing customized config.", e);
        } finally {
            if (null != fileIn) {
                try {
                    fileIn.close();
                } catch (IOException e) {
                    Log.e(TAG, "Got IOException while closing FileInputStream.", e);
                }
            }
            if (null != in) {
                try {
                    in.close();
                } catch (IOException e) {
                    Log.e(TAG, "Got IOException while closing InputStream.", e);
                }
            }
        }
    }

    public static Integer getIntConfig (String key, int def) {
        return Integer.valueOf(getProperty(key, String.valueOf(def)));
    }

    public static Boolean getBooleanConfig (String key, boolean def) {
        return Boolean.valueOf(getProperty(key, String.valueOf(def)));
    }

    public static int getIdentifier(Context context, String defType, String name) {
        return context.getResources().getIdentifier(name, defType, "android");
    }
}
