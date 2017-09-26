package com.android.settings;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.String;

import android.content.Context;
import android.content.res.Resources;
import android.net.TrafficStats;
import android.os.SystemProperties;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.text.TextUtils;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.ims.ImsConfig;
import com.android.ims.ImsException;
import com.android.ims.ImsManager;

import static android.net.TrafficStats.KB_IN_BYTES;
import static android.net.TrafficStats.MB_IN_BYTES;
import static android.net.TrafficStats.GB_IN_BYTES;
import static android.net.TrafficStats.TB_IN_BYTES;
import static android.net.TrafficStats.PB_IN_BYTES;

/**
 * @author Lide_Yang
 *
 */
public class AsusTelephonyUtils {

    private static final String TAG = "AsusTelephonyUtils";

    public static final String TAB_TEXT_SIM_1 = "SIM 1";
    public static final String TAB_TEXT_SIM_2 = "SIM 2";

    // +++ Mark_Huang@20151119: Verizon VZ_REQ_LTEDATA_6742
    public static final String VERIZON_CLASS_3_APN_NAME = "Verizon Internet";
    public static final String VERIZON_VZWAPP_APN_NAME = "Verizon APP";
    public static final String VERIZON_VZWAdmin_APN_NAME = "Verizon Admin";
    public static final String VERIZON_VZWIMS_APN_NAME = "Verizon IMS";
    // --- Mark_Huang@20151119: Verizon VZ_REQ_LTEDATA_6742

    public static int getSubscriptionId(int slotId) {
        Log.v(TAG, "getSubscriptionId(), slotId == " + slotId);

        int[] subIds = SubscriptionManager.getSubId(slotId);
        if (subIds != null) {
            Log.v(TAG, "getSubscriptionId(), subIds != null");

            int subId = subIds[0];
            Log.v(TAG, "getSubscriptionId(), subId == " + subId);
            return subId;
        } else {
            Log.v(TAG, "getSubscriptionId(), subIds == null");

            int subId = SubscriptionManager.getDefaultSubscriptionId();
            Log.v(TAG, "getSubscriptionId(), subId == " + subId);
            return subId;
        }
    }

    public static int getPhoneId(int subscriptionId) {
        return SubscriptionManager.getPhoneId(subscriptionId);
    }

    public static Phone getPhone(int phoneId) {
        return PhoneFactory.getPhone(phoneId);
    }

    public static boolean hasAnyIccCard(Context context) {
        Log.v(TAG, "hasAnyIccCard()");

        TelephonyManager telephonyManager = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        String hasIccCardMethodName = "hasIccCard";
        Method hasIccCardBySubMethod = null;
        Method hasIccCardBySlotMethod = null;

        try {
            hasIccCardBySubMethod = telephonyManager.getClass().getMethod(
                    hasIccCardMethodName, long.class);
        } catch (NoSuchMethodException e) {
            Log.v(TAG,
                    "hasAnyIccCard(), NoSuchMethodException, no TelephonyManager.hasIccCard(long)",
                    e);
            hasIccCardBySubMethod = null;
        }
        Log.v(TAG, "hasAnyIccCard(), hasIccCardBySubMethod == "
                + hasIccCardBySubMethod);

        if (hasIccCardBySubMethod == null) {
            try {
                hasIccCardBySlotMethod = telephonyManager.getClass().getMethod(
                        hasIccCardMethodName, int.class);
            } catch (NoSuchMethodException e) {
                Log.v(TAG,
                        "hasAnyIccCard(), NoSuchMethodException, no TelephonyManager.hasIccCard(int)",
                        e);
                hasIccCardBySlotMethod = null;
            }
        }
        Log.v(TAG, "hasAnyIccCard(), hasIccCardBySlotMethod == "
                + hasIccCardBySlotMethod);

        if ((hasIccCardBySubMethod == null) && (hasIccCardBySlotMethod == null)) {
            return false;
        }

        int simCount = telephonyManager.getSimCount();
        Log.v(TAG, "hasAnyIccCard(), simCount == " + simCount);
        for (int i = 0; i < simCount; i++) {
            if (hasIccCardBySubMethod != null) {

                long subId = AsusTelephonyUtils.getSubscriptionId(i);
                try {
                    boolean hasIccCard = (Boolean) hasIccCardBySubMethod
                            .invoke(telephonyManager, subId);
                    if (hasIccCard) {
                        Log.v(TAG, "hasAnyIccCard(), slot ID == " + i
                                + ", hasIccCardBySubMethod returns true");
                        return true;
                    }
                } catch (IllegalAccessException e) {
                    Log.v(TAG,
                            "hasAnyIccCard(), hasIccCardBySubMethod throws IllegalAccessException",
                            e);
                    return false;
                } catch (IllegalArgumentException e) {
                    Log.v(TAG,
                            "hasAnyIccCard(), hasIccCardBySubMethod throws IllegalArgumentException",
                            e);
                    return false;
                } catch (InvocationTargetException e) {
                    Log.v(TAG,
                            "hasAnyIccCard(), hasIccCardBySubMethod throws InvocationTargetException",
                            e);
                    return false;
                }

            } else if (hasIccCardBySlotMethod != null) {

                try {
                    boolean hasIccCard = (Boolean) hasIccCardBySlotMethod
                            .invoke(telephonyManager, i);
                    if (hasIccCard) {
                        Log.v(TAG, "hasAnyIccCard(), slot ID == " + i
                                + ", hasIccCardBySlotMethod returns true");
                        return true;
                    }
                } catch (IllegalAccessException e) {
                    Log.v(TAG,
                            "hasAnyIccCard(), hasIccCardBySlotMethod throws IllegalAccessException",
                            e);
                    return false;
                } catch (IllegalArgumentException e) {
                    Log.v(TAG,
                            "hasAnyIccCard(), hasIccCardBySlotMethod throws IllegalArgumentException",
                            e);
                    return false;
                } catch (InvocationTargetException e) {
                    Log.v(TAG,
                            "hasAnyIccCard(), hasIccCardBySlotMethod throws InvocationTargetException",
                            e);
                    return false;
                }

            } else {
                return false;
            }
        }
        Log.v(TAG, "hasAnyIccCard(), no card");
        return false;
    }

    public static boolean isAnySimStateReady(Context context) {
        Log.v(TAG, "isAnySimStateReady()");

        TelephonyManager telephonyManager = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        int simCount = telephonyManager.getSimCount();

        Log.v(TAG, "isAnySimStateReady(), simCount == " + simCount);
        for (int i = 0; i < simCount; i++) {
            if (isSimStateReady(telephonyManager, i)) {
                Log.v(TAG, "isAnySimStateReady(), SIM ready, slot id == "
                        + i);
                return true;
            }
        }
        Log.v(TAG, "isAnySimStateReady(), no SIM ready");
        return false;
    }

    public static boolean isSimStateReady(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        int simState = telephonyManager.getSimState();
        if (simState == TelephonyManager.SIM_STATE_READY) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isSimStateReady(Context context, int slotId) {
        TelephonyManager telephonyManager = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        return isSimStateReady(telephonyManager, slotId);
    }

    private static boolean isSimStateReady(TelephonyManager telephonyManager,
            int slotId) {
        Log.v(TAG, "isSimStateReady(), slotId == " + slotId);

        int simState = telephonyManager.getSimState(slotId);

        Log.v(TAG, "isSimStateReady(), simState == " + simState);
        if (simState == TelephonyManager.SIM_STATE_READY) {
            return true;
        } else {
            return false;
        }
    }

    // +++ Mark_Huang@20151117: Add for Verizon
    public static boolean isVerizon() {
        boolean ret = false;
        if (SystemProperties.getInt("persist.dbg.is_verizon_device", 0) == 1){
            return true;
	}
        try {
            if (SystemProperties.getInt("ro.asus.is_verizon_device", 0) == 1) {
                ret = true;
            }
        } catch (Exception e) {
            Log.v(TAG, "isVerizon(): Failed to get property since " + e.toString());
        }
        Log.v(TAG, "isVerizon(): ret = " + ret);
        return ret;
    }

    public static boolean isVerizonSim() {
        boolean ret = false;
        try {
            if (SystemProperties.getInt("ril.is_verizon_sim", 0) == 1) {
                ret = true;
            }
        } catch (Exception e) {
            Log.v(TAG, "isVerizonSim(): Failed to get property since " + e.toString());
        }
        Log.v(TAG, "isVerizonSim(): ret = " + ret);
        return ret;
    }

    // +++ Mark_Huang@20151119: Verizon VZ_REQ_LTEDATA_6742
    public static boolean isVerizonClass3Apn(String apnName) {
        return VERIZON_CLASS_3_APN_NAME.equals(apnName);
    }
    // --- Mark_Huang@20151119: Verizon VZ_REQ_LTEDATA_6742

    // +++ Mark_Huang@20151117: Verizon VZ_REQ_UI_15715
    public enum SizeUnit {
        B(0), KB(1), MB(2), GB(3), TB(4), PB(5);

        private final long unitBytes;
        private final String displayString;
        private SizeUnit(int displayUnit) {
            switch (displayUnit) {
                case 0:
                    this.unitBytes = 1L; // Byte
                    this.displayString = Resources.getSystem().getString(
                            com.android.internal.R.string.byteShort);
                    break;
                case 1:
                    this.unitBytes = TrafficStats.KB_IN_BYTES; // KiloBytes
                    this.displayString = Resources.getSystem().getString(
                            com.android.internal.R.string.kilobyteShort);
                    break;
                case 2:
                    this.unitBytes = TrafficStats.MB_IN_BYTES; // MegaBytes
                    this.displayString = Resources.getSystem().getString(
                            com.android.internal.R.string.megabyteShort);
                    break;
                case 3:
                    this.unitBytes = TrafficStats.GB_IN_BYTES; // GigaBytes
                    this.displayString = Resources.getSystem().getString(
                            com.android.internal.R.string.gigabyteShort);
                    break;
                case 4:
                    this.unitBytes = TrafficStats.TB_IN_BYTES; // Terabytes
                    this.displayString = Resources.getSystem().getString(
                            com.android.internal.R.string.terabyteShort);
                    break;
                case 5:
                    this.unitBytes = TrafficStats.PB_IN_BYTES; // Petabytes
                    this.displayString = Resources.getSystem().getString(
                            com.android.internal.R.string.petabyteShort);
                    break;
                default:
                    this.unitBytes = 1L; // Byte
                    this.displayString = Resources.getSystem().getString(
                            com.android.internal.R.string.byteShort);
            }
        }

        public long getUnitbytes() {
            return this.unitBytes;
        }

        @Override
        public String toString() {
            return this.displayString;
        }
    }
    // --- Mark_Huang@20151117: Verizon VZ_REQ_UI_15715

    // +++ Millie_Chang@20151211 : VZ_REQ_UI_15765 - IMEI or ICCID shall be displayed in 3 or 4 digit chunks
    public static String formatIdentityDisplay (String Identity){
        String displayString;
        if(Identity == null || Identity.equals("")){
            return Identity;
        }
        if (isVerizon()){
            if (Identity.length()%3==0){
                displayString = TextUtils.join(" ", splitByNumber(Identity,3));
            } else if (Identity.length()%4==0){
                displayString = TextUtils.join(" ", splitByNumber(Identity,4));
            } else {
                displayString = Identity;
            }
        } else {
            displayString = Identity;
        }
        return displayString;
    }

    public static String[] splitByNumber(String number, int chunkSize){
        int chunkCount = (number.length() / chunkSize) + (number.length() % chunkSize == 0 ? 0 : 1);
        String[] chunkString = new String[chunkCount];
        for(int i=0;i<chunkCount;i++){
            chunkString[i] = number.substring(i*chunkSize, Math.min((i+1)*chunkSize, number.length()));
        }
        return chunkString;
    }
    // --- Millie_Chang@20151211 : VZ_REQ_UI_15765

    // +++ Mark_Huang@20151119: Verizon VZ_REQ_LTEDATA_6742
    public static boolean shouldDisplayAPN(String apnName) {
        boolean ret = true;
        if (VERIZON_VZWAPP_APN_NAME.equals(apnName) ||
                VERIZON_VZWAdmin_APN_NAME.equals(apnName) ||
                VERIZON_VZWIMS_APN_NAME.equals(apnName)) {
            ret = false;
        }
        return ret;
    }
    // --- Mark_Huang@20151119: Verizon VZ_REQ_LTEDATA_6742
    // --- Mark_Huang@20151117: Add for Verizon

    // +++ Millie_Chang : Verizon Advanced Calling Setting
    public static boolean shouldDisplayAdvanceCalling(Context context) {
        boolean ret = false;
        ImsManager mImsManager = ImsManager.getInstance(context,
                SubscriptionManager.getDefaultVoicePhoneId());
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (isVerizon() && mImsManager.isVolteEnabledByPlatform(context)
                && mImsManager.isVolteProvisionedOnDevice(context)
                && telephonyManager.isVoiceCapable()) {
            ret = true;
        }
        Log.d(TAG," shouldDisplayAdvanceCalling : " + ret);
        return ret;
    }
    // --- Millie_Chang : Verizon Advanced Calling Setting
}
