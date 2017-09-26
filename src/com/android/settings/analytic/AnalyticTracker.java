package com.android.settings.analytic;

import com.google.android.gms.analytics.ExceptionParser;

import android.app.Activity;
import android.content.Context;

public abstract class AnalyticTracker {

    /**
     * A custom exception parser which records stack trace for tracker uploading
     * exceptions
     *
     * @author albertjs
     */
    public static class AnalyticsExceptionParser implements ExceptionParser {
        public String getDescription(String p_description, Throwable p_throwable) {
            return "Description: " + p_description + ", Exception: "
                    + getExceptionDescription(p_throwable);
        }

        private String getExceptionDescription(Throwable p_throwable) {

            final StringBuilder result = new StringBuilder();
            result.append(p_throwable.toString());
            result.append(",\n");
            String oneElement;

            for (StackTraceElement element : p_throwable.getStackTrace()) {
                oneElement = element.toString();
                result.append(oneElement);
                result.append(",\n");
            }

            return result.toString();
        }
    }

    public abstract void activityStart(Activity activity);

    public abstract void activityStop(Activity activity);

    public abstract void sendEvents(Context context, String category, String action, String label,
            Long value);

    public abstract void sendTiming(Context context, String category, long intervalInMillis,
            String name, String label);

    public abstract void sendException(Context context, String description, Throwable ex,
            boolean fatal);

    public abstract void sendView(Context context, String viewName);
}

