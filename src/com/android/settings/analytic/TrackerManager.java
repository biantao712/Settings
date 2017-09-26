package com.android.settings.analytic;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import com.android.settings.Utils;

import java.util.HashMap;

public class TrackerManager {

    private static final String TAG = "TrackerManager";

    public enum TrackerName {
        TRACKER_WIFI(false, 100.0D, GATracker.TRACKER_ID_WIFI),
        TRACKER_ZENMOTION(false, 10.0D, GATracker.TRACKER_ID_ZENMOTION),
        LockScreenSettings(false, 10.0D, GATracker.PROPERTY_ID),
        TRACKER_MAIN_ENTRIES(false, 0.1D, GATracker.TRACKER_ID_MAIN_ENTRIES),
        TRACKER_FONTLOCALELANGUAGE(false, 10.0D, GATracker.TRACKER_ID_FONTLOCALELANGUAGE);
        private boolean mAutoTrack;
        private double mSampleRate;
        private String mId;
        TrackerName(boolean isAutoTrack, double sampleRate, String id) {
            mAutoTrack = isAutoTrack;
            mSampleRate = sampleRate;
            mId = id;
        }

        public boolean isAutoTrack() {
            return mAutoTrack;
        }

        public double getSampleRate() {
            return mSampleRate;
        }

        public String getTrackerID() {
            return mId;
        }
    }

    public static final int FLAG_HAS_INITIALIZED = 1 << 0;
    public static final int FLAG_HAS_ANALYTICS_KEY = 1 << 1;
    public static final int FLAG_SETTINGS_ANALYTICS_ENABLED = 1 << 2;
    public static final int FLAG_IS_DEBUGGABLE = 1 << 3;
    public static final int FLAG_IS_MONKEY = 1 << 4;
    public static final int FLAG_ALLOW_ANALYTICS_FOR_SKU = 1 << 5;

    public static final int MASK_PERSISTENT_FLAGS = FLAG_HAS_INITIALIZED | FLAG_HAS_ANALYTICS_KEY
            | FLAG_IS_DEBUGGABLE | FLAG_ALLOW_ANALYTICS_FOR_SKU;

    //we can force enable GA by set DEBUG_FORCE_ENABLE_TRACKERS = true
    public static final boolean DEBUG_FORCE_ENABLE_TRACKERS = false;

    public static final String ANALYTICS_KEY = "asus_analytics";
    public static final long DEFAULT_VALUE = 0L;
    public static final String DEFAULT_LABEL = "UNKNOWN";

    private static int[] sAllTypeTaskCount = null;
    private static TrackerManager sInstance;

    // sTotalTaskCount Index
    private static final int LOCAL_TASK = 0;
    private static final int GOOGLE_TASK = 1;
    private static final int EXCHANGE_TASK = 2;

    private static final String DEFAULT_SNOOZE_TIME = "5";


    HashMap<TrackerName, AnalyticTracker> mTrackers = new HashMap<>();

    private boolean mEnableTracker = false;
    private int mFlags = 0;

    private TrackerManager(Context context) {
        Log.e(TAG,"NEW TrackerManager");
    }

    private AnalyticTracker getTracker(Context context, TrackerName trackerName) {
        AnalyticTracker tracker = mTrackers.get(trackerName);
        if (null == tracker) {
            tracker = new GATracker(context, trackerName);
            Log.e(TAG,"mTrackers.add");
            mTrackers.put(trackerName, tracker);
        }
        return tracker;
    }

    public void setEnableStatus(Context context) {
        updateFlags(context);
        if (DEBUG_FORCE_ENABLE_TRACKERS) {
            mEnableTracker = true;
            return;
        }
        if ((mFlags & FLAG_ALLOW_ANALYTICS_FOR_SKU) == 0 || (mFlags & FLAG_IS_MONKEY) != 0) {
            mEnableTracker = false;
            return;
        }
        // Currently we will block Trackers for all devices without the key
        mEnableTracker = (mFlags & FLAG_HAS_ANALYTICS_KEY) != 0
                && (mFlags & FLAG_SETTINGS_ANALYTICS_ENABLED) != 0;
    }

    private void updateFlags(Context context) {
        ContentResolver cr = context.getApplicationContext().getContentResolver();

        if ((mFlags & FLAG_HAS_INITIALIZED) == 0) {
            Log.d(TAG, "Initializing flags...");
            mFlags = 0;

            // FLAG_HAS_ANALYTICS_KEY
            try {
                // Modified by Mingszu Liang, 2017.01.25. - BEGIN.
                //
                //   - from N beginning, we phase out and don't porting framework part.
                //   - Use Secure table instead of System table.
                //
                Settings.Secure.getInt(cr, ANALYTICS_KEY);
                //
                // Modified by Mingszu Liang, 2017.01.25. - END.

                mFlags |= FLAG_HAS_ANALYTICS_KEY;
            }
            catch (Settings.SettingNotFoundException e) {
                // Modified by Mingszu Liang, 2017.01.25. - BEGIN.
                //
                //   - from N beginning, we phase out and don't porting framework part.
                //   - Use Secure table instead of System table.
                //   - If secure "asus_analytics" isn't found,
                //     we will check system "asus_analytics", and set 0 to system "asus_analytics".
                //
                int dbValue = 0;

                try {
                    dbValue = Settings.System.getInt(cr, ANALYTICS_KEY);
                }
                catch (Settings.SettingNotFoundException e2) {
                    Log.d(TAG, "Cannot find Analytics preference in Settings");
                }
                finally {
                    try {
                        Settings.System.putInt(cr, ANALYTICS_KEY, 0);
                    }
                    catch (Exception e3) {
                        // ignore this part.
                    }

                    try {
                        Settings.Secure.putInt(cr, ANALYTICS_KEY, dbValue);
                        mFlags |= FLAG_HAS_ANALYTICS_KEY;
                    }
                    catch (Exception e4) {
                        // ignore this part.
                    }
                }
                //
                // Modified by Mingszu Liang, 2017.01.25. - END.
            }

            // FLAG_IS_DEBUGGABLE
//            if (GeneralUtils.DEBUG) {
//                Log.d(TAG, "ro.debuggable is true");
//                mFlags |= FLAG_IS_DEBUGGABLE;
//            }

//            // FLAG_ALLOW_ANALYTICS_FOR_SKU
             mFlags |= FLAG_ALLOW_ANALYTICS_FOR_SKU;
           if (Utils.isCNSKU()) {
               Log.d(TAG, "Analytics not allowed in CN SKU");
               mFlags &= ~FLAG_ALLOW_ANALYTICS_FOR_SKU;
           }

            mFlags |= FLAG_HAS_INITIALIZED;
        } else {
            Log.d(TAG, "Updating flags...");
            mFlags &= MASK_PERSISTENT_FLAGS;
        }

        // FLAG_SETTINGS_ANALYTICS_ENABLED
        if ((mFlags & FLAG_HAS_ANALYTICS_KEY) != 0) {
            try {
                // Modified by Mingszu Liang, 2016.08.23. - BEGIN.
                //
                //   - from N beginning, we phase out and don't porting framework part.
                //   - Use Secure table instead of System table.
                //
                if (Settings.Secure.getInt(cr, ANALYTICS_KEY) == 1) {
                    mFlags |= FLAG_SETTINGS_ANALYTICS_ENABLED;
                }
                //
                // Modified by Mingszu Liang, 2016.08.23. - END.
            } catch (Settings.SettingNotFoundException e) {
                // This should not happen, as we had already checked Settings
                throw new IllegalStateException("Settings found during initialization,"
                        + "but gone during update");
            }
        }

        // FLAG_IS_MONKEY
//        if (GeneralUtils.DEBUG_MONKEY) {
//            mFlags |= FLAG_IS_MONKEY;
//        }
    }

    public boolean getEnableStatus() {
        if ((mFlags & FLAG_HAS_INITIALIZED) == 0) {
            Log.w(TAG, "Flags not initialized before calling getters.");
            return false;
        }
        return mEnableTracker;
    }

    private boolean isDebug() {
        if (DEBUG_FORCE_ENABLE_TRACKERS) {
            return true;
        }
        if ((mFlags & FLAG_HAS_INITIALIZED) == 0) {
            Log.w(TAG, "Flags not initialized before calling getters.");
            return false;
        }
        return (mFlags & FLAG_IS_DEBUGGABLE) != 0;
    }

    public static TrackerManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new TrackerManager(context);
        }
        sInstance.setEnableStatus(context);
        return sInstance;
    }

//    public static void putBoolItem(Context context, String key, boolean bool) {
//        TrackerManager tm = getInstance(context);
//        if (tm.getEnableStatus()) {
//            PreferenceUtils.putBoolean(context, key, bool);
//        }
//    }
//
//    public static void putIntItem(Context context, String key, int value) {
//        TrackerManager tm = getInstance(context);
//        if (tm.getEnableStatus()) {
//            PreferenceUtils.putInt(context, key, value);
//        }
//    }
//
//    public static int getIntItem(Context context, String key) {
//        TrackerManager tm = getInstance(context);
//        if (!tm.getEnableStatus()) return 0;
//        // get 0 while item not exist
//        return PreferenceUtils.getInt(context, key, 0);
//    }
//
//    public static boolean getBoolItem(Context context, String key) {
//        TrackerManager tm = getInstance(context);
//        if (!tm.getEnableStatus()) return false;
//        // get false while item not exist
//        return PreferenceUtils.getBoolean(context, key, false);
//    }

    public static void activityStart(Activity activity, TrackerName trackerName) {
        TrackerManager tm = getInstance(activity);
        if (tm.getEnableStatus()) {
            tm.getTracker(activity, trackerName).activityStart(activity);
        }
    }

    public static void activityStop(Activity activity, TrackerName trackerName) {
        TrackerManager tm = getInstance(activity);
        if (tm.getEnableStatus()) {
            tm.getTracker(activity, trackerName).activityStop(activity);
        }
    }

    public static void sendEvents(Context context, TrackerName trackerName, String category,
            String action, String label, Long value) {
        TrackerManager tm = getInstance(context);
        if (tm.getEnableStatus()) {
            tm.getTracker(context, trackerName).sendEvents(
                    context,
                    category,
                    action,
                    label,
                    value);
        }
    }

    public static void sendTiming(Context context, TrackerName trackerName, String category,
            long intervalInMillis, String name, String label) {
        TrackerManager tm = getInstance(context);
        if (tm.getEnableStatus()) {
            tm.getTracker(context, trackerName).sendTiming(
                    context,
                    category,
                    intervalInMillis,
                    name,
                    label);
        }
    }

    public static void sendException(Context context, TrackerName trackerName, String description,
            Throwable ex, boolean fatal) {
        TrackerManager tm = getInstance(context);
        if (tm.getEnableStatus()) {
            tm.getTracker(context, trackerName).sendException(context, description, ex, fatal);
        }
    }

    public static void sendView(Context context, TrackerName trackerName, String viewName) {
        TrackerManager tm = getInstance(context);
        if (tm.getEnableStatus()) {
            tm.getTracker(context, trackerName).sendView(context, viewName);
        }
    }

//    public static void sendDailyUseReport(Context context) {
//        Log.d(TAG,"sendDailyReport");
//        int day = getIntItem(context, PreferenceUtils.KEY_REPORT_DAY);
//        int currentDay = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
//        if (day != currentDay) {
//            Log.d(TAG, "sendEvent");
//            sendEvents(context, Category.CATEGORY_DAILY_REPORT, Action.ACTION_DAILY_REPORT,
//                    Label.UNKNOWN, DEFAULT_VALUE);
//            sendTotalTaskCount(context);
//            sendDoItLaterTaskCount(context);
//            sendTypeOfTask(context);
//            sendWidgetStatistics(context);
//            putTaskCompletedType(context);
//            putIsSettingChange(context);
//            sendFolderCount(context);
//            putIntItem(context, PreferenceUtils.KEY_REPORT_DAY, currentDay);
//        }
//    }
//
//    private static void sendTotalTaskCount(Context context) {
//        sAllTypeTaskCount = TaskManager.getAllTypeTaskCountArray(context);
//        if (sAllTypeTaskCount == null) return;
//
//        int sum = 0;
//        for (int typeTaskCount : sAllTypeTaskCount) {
//            sum += typeTaskCount;
//        }
//        sendEvents(context, Category.CATEGORY_TASK_STATISTICS, Action.ACTION_TOTAL_TASK_COUNT,
//                String.valueOf(sum), DEFAULT_VALUE);
//    }
//
//    private static void sendDoItLaterTaskCount(Context context) {
//        sendEvents(context, Category.CATEGORY_TASK_STATISTICS,
//                Action.ACTION_TOTAL_DOITLATER_COUNT,
//                String.valueOf(TaskManager.getAllDoItLaterTaskCount(context)), DEFAULT_VALUE);
//    }
//
//    private static void sendTypeOfTask(Context context) {
//        if (sAllTypeTaskCount == null) return;
//        if (0 != sAllTypeTaskCount[LOCAL_TASK]) {
//            sendEvents(context, Category.CATEGORY_TASK_STATISTICS, Action.ACTION_LOCAL_TASK_COUNT,
//                    String.valueOf(sAllTypeTaskCount[LOCAL_TASK]), DEFAULT_VALUE);
//        }
//        if (0 != sAllTypeTaskCount[EXCHANGE_TASK]) {
//            sendEvents(context, Category.CATEGORY_TASK_STATISTICS,
//                    Action.ACTION_EXCHANGE_TASK_COUNT,
//                    String.valueOf(sAllTypeTaskCount[EXCHANGE_TASK]), DEFAULT_VALUE);
//        }
//        if (0 != sAllTypeTaskCount[GOOGLE_TASK]) {
//            sendEvents(context, Category.CATEGORY_TASK_STATISTICS, Action.ACTION_GOOGLE_TASK_COUNT,
//                    String.valueOf(sAllTypeTaskCount[GOOGLE_TASK]), DEFAULT_VALUE);
//        }
//    }
//
//    private static void sendWidgetStatistics(Context context) {
//        boolean isUsingWidget = TrackerManager.getBoolItem(context,
//                Action.ACTION_KEY_IS_USING_WIDGET);
//        if (isUsingWidget) {
//            sendEvents(context, Category.CATEGORY_WIDGET, Action.ACTION_IS_WIDGET_CHANGED,
//                    TrackerManager.getBoolItem(context, Action.ACTION_KEY_IS_SIZE_CHANGED) ?
//                            Label.True : Label.False, DEFAULT_VALUE);
//        }
//        sendEvents(context, Category.CATEGORY_WIDGET, Action.ACTION_IS_USING_WIDGET,
//                Boolean.toString(isUsingWidget), DEFAULT_VALUE);
//    }
//
//    private static void putTaskCompletedType(Context context) {
//        String completedType = TaskManager.getTaskCompletedType(context);
//        if (completedType == null) return;
//        sendEvents(context, Category.CATEGORY_TASK_STATISTICS,
//                Action.ACTION_DELETE_COMPlETED_TASK, completedType, DEFAULT_VALUE);
//    }
//
//    private static void putIsSettingChange(Context context) {
//        sendEvents(context, Category.CATEGORY_SETTINGS, Action.ACTION_ALERT_CHANGED,
//                PreferenceManager.getDefaultSharedPreferences(context)
//                        .getBoolean(GeneralPreference.KEY_ALERTS, true) ?
//                        Label.False : Label.True, DEFAULT_VALUE);
//        sendEvents(context, Category.CATEGORY_SETTINGS, Action.ACTION_SNOOZE_TIME_CHANGED,
//                PreferenceManager.getDefaultSharedPreferences(context).getString(
//                        GeneralPreference.KEY_DEFAULT_SNOOZE_TIME, DEFAULT_SNOOZE_TIME),
//                DEFAULT_VALUE);
//    }
//
//    private static void sendFolderCount(Context context) {
//        Cursor folderCursor = TaskManager.getFolderCountCursor(context);
//        if (folderCursor == null) return;
//        try {
//            while (folderCursor.moveToNext()) {
//                String mailboxType = folderCursor.getString(TaskManager.MAILBOX_ACCOUNT_TYPE);
//                sendEvents(context, Category.CATEGORY_ACCOUNT_FOLDERS, mailboxType,
//                        String.valueOf(folderCursor.getInt(TaskManager.COUNT_ACCOUNT_ID)),
//                        DEFAULT_VALUE);
//            }
//        } finally {
//            folderCursor.close();
//        }
//    }
//
//    public static void sendDoItLaterEventClicked(Context context, String packageName, long period) {
//        float days = (float) period / DateUtils.DAY_IN_MILLIS;
//        sendEvents(context, Category.CATEGORY_LATER_CLICK_EVENT, packageName,
//                String.format("%.2f", days), DEFAULT_VALUE);
//    }
//
//    public static void sendNewEvent(Context context, long duringTime, long descriptionLength,
//            long titleLength) {
//        float minutes = (float) duringTime / DateUtils.MINUTE_IN_MILLIS;
//        sendTiming(context, Category.CATEGORY_EDIT_ACTIVITY, duringTime,
//                String.format("%.2f", minutes), Label.UNKNOWN);
//        sendEvents(context, Category.CATEGORY_EDIT_ACTIVITY, Action.ACTION_DESCRIPTION_LENGTH,
//                String.valueOf(descriptionLength), descriptionLength);
//        sendEvents(context, Category.CATEGORY_EDIT_ACTIVITY, Action.ACTION_TITLE_LENGTH,
//                String.valueOf(titleLength), titleLength);
//    }
}
