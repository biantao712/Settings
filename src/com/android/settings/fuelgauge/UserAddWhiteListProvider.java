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
 * Created by steve on 2016/4/11.
 */
public class UserAddWhiteListProvider extends ContentProvider {
    private static final String AUTHORITY = "com.android.settings.fuelgauge.UserAddWhiteListProvider";
    private static final String PATH = "AppList";
    private static final int CODE = 1;

    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + PATH);

    private static final UriMatcher sUriMatcher;
    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, PATH, CODE);
    }

    public static final String USERADDWHITELIST = "user_add_doze_list";
    private static final String DOZEMODELIST = "DozeModeList";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {

        SharedPreferences list = getContext().getSharedPreferences(DOZEMODELIST,
                getContext().MODE_PRIVATE);
        Set<String> userWhite = list.getStringSet(USERADDWHITELIST, new HashSet<String>());
        ArrayList<String> whiteList = new ArrayList<String>();
        for(String w : userWhite) {
            whiteList.add(w);
        }
        Bundle bundle = new Bundle();
        bundle.putSerializable(USERADDWHITELIST, whiteList);

        MatrixCursor cursor = null;

        switch (sUriMatcher.match(uri)) {
            case CODE: {
                cursor = new MatrixCursor(new String[] {
                        USERADDWHITELIST
                });

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
                //+++ tim
                //selection = "user_add_doze_list"
                if (USERADDWHITELIST.equals(selection)) {
                    SharedPreferences list = getContext().getSharedPreferences(DOZEMODELIST,
                            getContext().MODE_PRIVATE);
                    Set<String> white = list.getStringSet(USERADDWHITELIST, new HashSet<String>());
                    PowerWhitelistBackend backend = PowerWhitelistBackend.getInstance();
                    backend.createDozeModeBackend(getContext().getApplicationContext());
                    for (int i = 0; i<selectionArgs.length; i++) {
                        if (!white.contains(selectionArgs[i])) {
                            backend.addApp(selectionArgs[i]);
                        }
                    }
                }else {
                //---
                //selection = "userSelectOptimizeList"
                SharedPreferences list = getContext().getSharedPreferences(DOZEMODELIST,
                        getContext().MODE_PRIVATE);
                Set<String> userWhite = list.getStringSet(USERADDWHITELIST, new HashSet<String>());

                Set<String> white = new HashSet<String>();
                for (int i = 0; i<selectionArgs.length; i++) {
                    if (userWhite.contains(selectionArgs[i])) {
                        white.add(selectionArgs[i]);
                    }
                }
                list.edit().putStringSet("userSelectOptimizeList", white).commit();

                Intent intent = new Intent();
                intent.setAction("com.android.settings.OPTIMIZED_ADD_USER_WHITELIST");
                getContext().sendBroadcast(intent);
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