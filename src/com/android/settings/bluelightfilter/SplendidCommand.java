package com.android.settings.bluelightfilter;

import java.io.File;
import java.io.IOException;

import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.android.settings.bluelightfilter.Constants;
import com.asus.splendidcommandagent.ISplendidCommandAgentService;

public class SplendidCommand {

    protected static final String TAG = "SplendidCommand";
    protected static final String COMMAND_DIR_NAME = "/system/bin/";

    public static final String COMMAND_NAME_LIST[] = {
        "GammaSetting",
        "HSVSetting",
        "DisplayColorSetting",
        "GamutSetting"
    };
    public static final int MODE_CT = 0;
    public static final int MODE_HSV = 1;
    public static final int MODE_DISPLAY_COLOR_SETTING = 2;
    public static final int MODE_GAMUT_MAPPING = 3;

    public static boolean isCommandExists(int mode) {
        if (mode == MODE_CT) return isGammaExists();
        else if (mode == MODE_HSV) return isHSVExists();
        else return isDisplayColorSettingExists();
    }

    public static boolean isGammaExists(){
        File cmdFile = new File(COMMAND_DIR_NAME + COMMAND_NAME_LIST[MODE_CT]);
        return cmdFile.exists();
    }

    public static boolean isHSVExists(){
        File cmdFile = new File(COMMAND_DIR_NAME + COMMAND_NAME_LIST[MODE_HSV]);
        return cmdFile.exists();
    }

    public static boolean isDisplayColorSettingExists(){
        File cmdFile = new File(COMMAND_DIR_NAME + COMMAND_NAME_LIST[MODE_DISPLAY_COLOR_SETTING]);
        return cmdFile.exists();
    }

    public static boolean isGamutSettingExists(){
        File cmdFile = new File(COMMAND_DIR_NAME + COMMAND_NAME_LIST[MODE_GAMUT_MAPPING]);
        return cmdFile.exists();
    }

   public static void run(ISplendidCommandAgentService service, int mode, String data) throws IOException, InterruptedException{
        if (Constants.PRIVATE_DEBUG) Log.d(TAG, "run ISplendidCommandAgentService");
        String cmd;
        cmd = COMMAND_NAME_LIST[mode] + " " + data;

        if (Constants.DEBUG) Log.i(TAG, "command: " + cmd);
        try {
            service.doCommand(cmd);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
   }

    public static String getCommandH(SplendidColor color, boolean isOnPhoneMode, SplendidColor defaultColor) throws IOException, InterruptedException{
        String cmdString = "";
        if(isOnPhoneMode){
            cmdString = convertColorH(color, defaultColor);
        }else{
            cmdString = convertPadColorH(color, defaultColor);
        }
        return cmdString;
    }

    public static String getCommandS(SplendidColor color, boolean isOnPhoneMode, SplendidColor defaultColor) throws IOException, InterruptedException{
        String cmdString = "";
        if(isOnPhoneMode){
            cmdString = convertColorS(color, defaultColor);
        }else{
            cmdString = convertPadColorS(color, defaultColor);
        }
        return cmdString;
    }

    public static String getCommandV(SplendidColor color, boolean isOnPhoneMode) throws IOException, InterruptedException {
        String cmdString = "";
        if(isOnPhoneMode){
            cmdString = convertColorV(color.v);
        }else{
            cmdString = convertPadColorV(color.v);
        }
        return cmdString;
    }

    // H_PHONE : convert "0 ~ 359" to "80 ~ ff, 00, 01 ~ 7F" (-15 ~ 0 ~ +15)
    private static String convertColorH(SplendidColor color, SplendidColor defaultColor) {
        if (color.equalsH(defaultColor)) {
            return "-h 0";
        }

        if(color.h > 359.0f){
            return "-h 7F";
        }

        float fh = (float) (color.h % 60.0f);
        int nh = Math.min((int) ((fh - 30.0) * 128.0 / 30.0), 127);

        String hString = Integer.toHexString(nh);
        if (hString.length() > 2) {
            hString = hString.substring(hString.length() - 2);
        } else if (hString.length() == 1) {
            hString = "0" + hString;
        }

        return "-h " + hString;
    }

    // H_PAD : convert "0 ~ 359" to "f ~ 1, 00, f1 ~ ff" (-15 ~ 0 ~ +15)
    private static String convertPadColorH(SplendidColor color, SplendidColor defaultColor) {
        if (color.equalsH(defaultColor)) {
            return "-h 0";
        }

        if (color.h > 359.0f) {
            return "-h ff";
        }

        float fh = (float) Math.min((color.h % 60) / 59.0f, 1.0f);
        int nh = (int) (fh * 30.0f - 15.0f);
        String hString = "";
        if (nh <= 0) {
            hString = Integer.toHexString(-nh);
        } else {
            hString = "f" + Integer.toHexString(nh);
        }

        return "-h " + hString;
    }

    // S_PHONE : convert "0 ~ 1" to "0 ~ ff",  default 80
    private static String convertColorS(SplendidColor color, SplendidColor defaultColor) {
        if (color.equalsS(defaultColor)) {
            return "-s 80:80:80:80:80:80";
        }

        String cmdString = "";
        String sString = Integer.toHexString((int) (color.s * 255));
        if (sString.length() > 2) {
            sString = sString.substring(sString.length() - 2);
        } else if (sString.length() == 1) {
            sString = "0" + sString;
        }
        if (color.h <= 60.0) {
            cmdString = sString + ":80:80:80:80:80";
        } else if (60.0 < color.h && color.h <= 120.0) {
            cmdString = "80:" + sString + ":80:80:80:80";
        } else if (120.0 < color.h && color.h <= 180.0) {
            cmdString = "80:80:" + sString + ":80:80:80";
        } else if (180.0 < color.h && color.h <= 240.0) {
            cmdString = "80:80:80:" + sString + ":80:80";
        } else if (240.0 < color.h && color.h <= 300.0) {
            cmdString = "80:80:80:80:" + sString + ":80";
        } else if (300.0 < color.h && color.h <= 360.0) {
            cmdString = "80:80:80:80:80:" + sString;
        }

        return "-s " + cmdString;
    }

    // S_PAD : convert "0 ~ 1" to "0 ~ ff",  default 80
    private static String convertPadColorS(SplendidColor color, SplendidColor defaultColor){
        if(color.equalsS(defaultColor)){
            return "-s 80:80:80:80:80:80";
        }

        String cmdString = "";
        String sString = Integer.toHexString((int)(color.s * 255));
        if(sString.length() > 2){
            sString = sString.substring(sString.length() - 2);
        }else if(sString.length() == 1){
            sString = "0" + sString;
        }
        if (color.h <= 60.0) {
            cmdString = sString + ":80:80:80:80:80";
        } else if (60.0 < color.h && color.h <= 120.0) {
            cmdString = "80:" + sString + ":80:80:80:80";
        } else if (120.0 < color.h && color.h <= 180.0) {
            cmdString = "80:80:" + sString + ":80:80:80";
        } else if (180.0 < color.h && color.h <= 240.0) {
            cmdString = "80:80:80:" + sString + ":80:80";
        } else if (240.0 < color.h && color.h <= 300.0) {
            cmdString = "80:80:80:80:" + sString + ":80";
        } else if (300.0 < color.h && color.h <= 360.0) {
            cmdString = "80:80:80:80:80:" + sString;
        }

        return "-s " + cmdString;
    }

    // V_PHONE covert "0 ~ 1" to "-128 ~ 0 ~ 128" to "180 ~ 1ff, 00 , 01 ~ 80"
    private static String convertColorV(double v) {
        int nV = (int)(v * 256.0 - 128.0);
        String cmdString = "";
        if(nV == 0){
            cmdString = "00";
        }else if(nV > 0){
            cmdString = Integer.toHexString(nV-1); // limit to 127
        }else{
            String vString = Integer.toHexString( nV + 256); // limit to -127
            if(vString.length() == 1){
                cmdString = "10" + vString;
            }else if(vString.length() == 2){
                cmdString = "1" + vString;
            }
        }

        return "-i " + cmdString;
    }

    // V_PAD covert "0 ~ 1" to "-129 ~ 0 ~ 127" to "20 ~ 80 ~ ff"
    private static String convertPadColorV(double v) {
        int nV = (int)(v * 256.0 - 129.0);
        String cmdString = "";
        if(nV == 0){
            cmdString = "00";
        }else if(nV > 0){
            cmdString = Integer.toHexString(nV + 128); // limit to 128
        }else{
            cmdString = Integer.toHexString( (int)( ( (nV+129.0) * 96.0 )/ 128.0 + 32.0 )); // limit to -127
        }

        return "-i " + cmdString;
    }
}
