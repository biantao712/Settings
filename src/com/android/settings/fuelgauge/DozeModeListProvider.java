package com.android.settings.fuelgauge;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by steve on 2016/3/28.
 */
public class DozeModeListProvider extends ContentProvider {
    private static final String AUTHORITY = "com.android.settings.fuelgauge.DozeModeListProvider";
    private static final String PATH = "AppList";
    private static final int CODE = 1;

    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + PATH);

    private static final UriMatcher sUriMatcher;
    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, PATH, CODE);
    }

    public static final String WHITELIST = "whiteList";
    public static final String BLACKLIST = "blackList";
    public static final String QA = "qa";
    private static final String DOZEMODELIST = "DozeModeList";

    private static final String USERWHITELIST = "userWhiteList";
    private static final String USERBLACKLIST = "userBlackList";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {

        SharedPreferences list = getContext().getSharedPreferences(DOZEMODELIST,
                getContext().MODE_PRIVATE);
        Set<String> white = list.getStringSet("whiteList", new HashSet<String>());
        Set<String> black = list.getStringSet("blackList", new HashSet<String>());
        ArrayList<String> whiteList = new ArrayList<String>();
        ArrayList<String> blackList = new ArrayList<String>();
        for(String w : white) {
            whiteList.add(w);
        }
        for(String g : black) {
            blackList.add(g);
        }

        //+++ tim
        Set<String> userWhite = list.getStringSet(USERWHITELIST, new HashSet<String>());
        Set<String> userBlack = list.getStringSet(USERBLACKLIST, new HashSet<String>());
        ArrayList<String> userWhiteList = new ArrayList<String>();
        ArrayList<String> userBlackList = new ArrayList<String>();
        for(String w : userWhite) {
            userWhiteList.add(w);
        }
        for(String g : userBlack) {
            userBlackList.add(g);
        }
        //---
        Boolean qa = list.getBoolean("qa", false);
        Bundle bundle = new Bundle();
        bundle.putSerializable("whiteList", whiteList);
        bundle.putSerializable("blackList", blackList);
        bundle.putBoolean("qa", qa);
        bundle.putSerializable(USERWHITELIST, userWhiteList);
        bundle.putSerializable(USERBLACKLIST, userBlackList);

        MatrixCursor cursor = null;

        switch (sUriMatcher.match(uri)) {
            case CODE: {
                cursor = new MatrixCursor(new String[] {
                        WHITELIST, BLACKLIST, QA, USERWHITELIST, USERBLACKLIST
                });
//                MatrixCursor.RowBuilder b = cursor.newRow();
//                b.add(WHITELIST, whiteList);
//                b.add(BLACKLIST, blackList);
//                b.add(QA, qa);
                cursor.setExtras(bundle);

                break;
            }
        }

        if (cursor != null) {
            // make cursor aware of the change
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }

        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        int val = 0;
        switch (sUriMatcher.match(uri)) {
            case CODE: {
                if ("whiteList".equals(selection) || "userWhiteList".equals(selection)) {

                    SharedPreferences list = getContext().getSharedPreferences(DOZEMODELIST,
                            getContext().MODE_PRIVATE);
                    Set<String> white = list.getStringSet("userWhiteList", new HashSet<String>());
                    for (int i = 0; i<selectionArgs.length; i++) {
                        white.add(selectionArgs[i]);
                    }
                    list.edit().putStringSet("userWhiteList", white).commit();

                    Intent intent = new Intent();
                    intent.setAction("com.android.settings.ADD_WHITELIST");
                    getContext().sendBroadcast(intent);

                } else if ("userBlackList".equals(selection)) {
                    SharedPreferences list = getContext().getSharedPreferences(DOZEMODELIST,
                            getContext().MODE_PRIVATE);
                    Set<String> set = new HashSet<String>();
                    for (int i = 0; i<selectionArgs.length; i++) {
                        set.add(selectionArgs[i]);
                    }
                    list.edit().putStringSet("userBlackList", set).commit();
                }

                val = 1;
                break;
            }
        }

        if (val > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return val;
    }

}

