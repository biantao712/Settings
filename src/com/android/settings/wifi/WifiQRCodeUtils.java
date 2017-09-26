package com.android.settings.wifi;


import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.wifi.WifiConfiguration;
import android.util.Log;
import com.android.settingslib.wifi.AccessPoint;
import java.util.EnumMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.Method;

import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.Result;
import com.google.zxing.client.result.ResultParser;
import com.google.zxing.client.result.WifiParsedResult;
import android.net.wifi.WifiConfiguration.KeyMgmt;

enum NetworkType {
    WEP,
    WPA,
    NO_PASSWORD;

    static NetworkType forIntentValue(String networkTypeString) {
        if (networkTypeString == null) {
            return NO_PASSWORD;
        }
        if ("WPA".equals(networkTypeString)) {
            return WPA;
        }
        if ("WEP".equals(networkTypeString)) {
            return WEP;
        }
        if ("nopass".equals(networkTypeString)) {
            return NO_PASSWORD;
        }
        throw new IllegalArgumentException(networkTypeString);
    }
}

public class WifiQRCodeUtils {

    public static int DeviceH = 250;
    public static int DeviceW = 250;

    public static WifiParsedResult getResult(String result) {
        String rawText = result;
        if (!rawText.startsWith("WIFI:")) {
            return null;
        }
        String ssid = matchSinglePrefixedField("S:", rawText, ';', false);
        if (ssid == null || ssid.isEmpty()) {
            return null;
        }
        String pass = matchSinglePrefixedField("P:", rawText, ';', false);
        String type = matchSinglePrefixedField("T:", rawText, ';', false);
        if (type == null) {
            type = "nopass";
        }
        boolean hidden = Boolean.parseBoolean(matchSinglePrefixedField("H:", rawText, ';', false));
        return new WifiParsedResult(type, ssid, pass, hidden);
    }

    static String matchSinglePrefixedField(String prefix, String rawText, char endChar, boolean trim) {
        String[] matches = matchPrefixedField(prefix, rawText, endChar, trim);
        return matches == null ? null : matches[0];
    }

    static String[] matchPrefixedField(String prefix, String rawText, char endChar, boolean trim) {
        List<String> matches = null;
        int i = 0;
        int max = rawText.length();
        while (i < max) {
            i = rawText.indexOf(prefix, i);
            if (i < 0) {
                break;
            }
            i += prefix.length(); // Skip past this prefix we found to start
            int start = i; // Found the start of a match here
            boolean more = true;
            while (more) {
                i = rawText.indexOf(endChar, i);
                if (i < 0) {
                    // No terminating end character? uh, done. Set i such that loop terminates and break
                    i = rawText.length();
                    more = false;
                } else if (countPrecedingBackslashes(rawText, i) % 2 != 0) {
                    // semicolon was escaped (odd count of preceding backslashes) so continue
                    i++;
                } else {
                    // found a match
                    if (matches == null) {
                        matches = new ArrayList<>(3); // lazy init
                    }
                    String element = unescapeBackslash(rawText.substring(start, i));
                    if (trim) {
                        element = element.trim();
                    }
                    if (!element.isEmpty()) {
                        matches.add(element);
                    }
                    i++;
                    more = false;
                }
            }
        }
        if (matches == null || matches.isEmpty()) {
            return null;
        }
        return matches.toArray(new String[matches.size()]);
    }

    static private int countPrecedingBackslashes(CharSequence s, int pos) {
        int count = 0;
        for (int i = pos - 1; i >= 0; i--) {
            if (s.charAt(i) == '\\') {
                count++;
            } else {
                break;
            }
        }
        return count;
    }

    static private String unescapeBackslash(String escaped) {
        int backslash = escaped.indexOf('\\');
        if (backslash < 0) {
            return escaped;
        }
        int max = escaped.length();
        StringBuilder unescaped = new StringBuilder(max - 1);
        unescaped.append(escaped.toCharArray(), 0, backslash);
        boolean nextIsEscaped = false;
        for (int i = backslash; i < max; i++) {
            char c = escaped.charAt(i);
            if (nextIsEscaped || c != '\\') {
                unescaped.append(c);
                nextIsEscaped = false;
            } else {
                nextIsEscaped = true;
            }
        }
        return unescaped.toString();
    }

    static public String getPreSharedKey(List<WifiConfiguration> configs, String ssid)
    {
        String key = "*";
        for(WifiConfiguration config : configs)
        {
            if(config.SSID.equals(ssid)) {
                if (config.allowedKeyManagement.get(KeyMgmt.WPA_PSK)) {
                    if(config.preSharedKey == null)
                        continue;

                    return config.preSharedKey;
                } else {
                    if(config.wepKeys[config.wepTxKeyIndex] == null)
                        continue;

                    return config.wepKeys[config.wepTxKeyIndex];
                }
            }
        }

        return key;
    }

    static public String getApQRCodeString(AccessPoint mSelectedAccessPoint,WifiManager mWifiManager)
    {
        String mQRCODE_Content = "";
        if(mSelectedAccessPoint == null) //for add new network, return ""
            return "";
        if(mSelectedAccessPoint.getSecurityString(true).equals("802.1x") || mSelectedAccessPoint.getSecurityString(true).equals(""))
            return mQRCODE_Content;
        try{
            Method method = mWifiManager.getClass().getMethod("getPrivilegedConfiguredNetworks");
            List<WifiConfiguration> configs = (List<WifiConfiguration>)method.invoke(mWifiManager);
            String type = WifiConfiguration.KeyMgmt.strings[mSelectedAccessPoint.getConfig().getAuthType()];
            type = type.substring(0,3);
            String presharekey = WifiQRCodeUtils.getPreSharedKey(configs,mSelectedAccessPoint.getConfig().SSID);
            String ssid = mSelectedAccessPoint.getConfig().SSID;
            ssid = ssid.substring(1,ssid.length()-1);
            mQRCODE_Content += "WIFI:T:" + type + ";P:" + presharekey + ";S:" + ssid + ";";
//            Log.d("WifiQRCodeUtils"," Generate WiFi QRCode =" + mQRCODE_Content);
        } catch(Exception e){
            Log.d("WifiQRCodeUtils"," Generate WiFi QRCODE Exception = " + e.getMessage());
        }
        return mQRCODE_Content;
    }

    static public String getApQRCodeString(WifiConfiguration mWifiConfig)
    {
        String mQRCODE_Content = "";
        String type = WifiConfiguration.KeyMgmt.strings[mWifiConfig.getAuthType()];
        type = type.substring(0,3);
        String presharekey = mWifiConfig.preSharedKey;
        String ssid = mWifiConfig.SSID;
        mQRCODE_Content += "WIFI:T:" + type + ";P:" + presharekey + ";S:" + ssid + ";";
        return mQRCODE_Content;
    }

    static public Bitmap getQRCode(String content)
    {
        Bitmap bitmap = null;
        int QRCodeWidth = (int)(DeviceW * 0.37);
        int QRCodeHeight = (int)(DeviceH * 0.208);

        Map<EncodeHintType, Object> hints = new EnumMap<EncodeHintType, Object>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

        MultiFormatWriter writer = new MultiFormatWriter();
        try {
            BitMatrix result= writer.encode(content, BarcodeFormat.QR_CODE, QRCodeWidth, QRCodeHeight, hints);
            bitmap = Bitmap.createBitmap(QRCodeWidth, QRCodeHeight, Bitmap.Config.ARGB_8888);
            for (int y = 0; y < QRCodeHeight; y++) {
                for (int x = 0; x < QRCodeWidth; x++) {
                    bitmap.setPixel(x, y, result.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
        } catch (WriterException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return bitmap;

    }

    static public Bitmap getQRCode(String content, int width, int height)
    {
        Bitmap bitmap = null;
        int QRCodeWidth = width;
        int QRCodeHeight = height;

        Map<EncodeHintType, Object> hints = new EnumMap<EncodeHintType, Object>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);

        MultiFormatWriter writer = new MultiFormatWriter();
        try {
            BitMatrix result= writer.encode(content, BarcodeFormat.QR_CODE, QRCodeWidth, QRCodeHeight, hints);
            bitmap = Bitmap.createBitmap(QRCodeWidth, QRCodeHeight, Bitmap.Config.ARGB_8888);
            for (int y = 0; y < QRCodeHeight; y++) {
                for (int x = 0; x < QRCodeWidth; x++) {
                    bitmap.setPixel(x, y, result.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
        } catch (WriterException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return bitmap;

    }
}
