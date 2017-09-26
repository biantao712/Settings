package com.android.settings.search;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MergeCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

/**
 * Created by Blenda_Fu on 2017/4/11.
 */

public class SearchContentProvider extends ContentProvider {
    private static final String AUTHORITY = "com.android.settings.search.SearchContentProvider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY );
    private IndexDatabaseHelper dbHelp;
    private static String TAG = "SearchContentProvider";

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public String getType(Uri uri) {
        // TODO: Implement this to handle requests for the MIME type of the data
        // at the given URI.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
    private ContentResolver contentResolver;
    @Override
    public boolean onCreate() {
        Context context = getContext();
        contentResolver = context.getContentResolver();
        dbHelp = IndexDatabaseHelper.getInstance(context);
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteDatabase database = dbHelp.getReadableDatabase();
        Cursor[] cursors = new Cursor[2];

        String primarySql = Index.buildSearchSQL(selection, Index.MATCH_COLUMNS_PRIMARY, true);
        Log.d(TAG, "Search primary query: " + primarySql);
        cursors[0] = database.rawQuery(primarySql, null);

        // We need to use an EXCEPT operator as negate MATCH queries do not work.
        StringBuilder sql = new StringBuilder(
                Index.buildSearchSQL(selection, Index.MATCH_COLUMNS_SECONDARY, false));
        sql.append(" EXCEPT ");
        sql.append(primarySql);

        String secondarySql = sql.toString();
        Log.d(TAG, "Search secondary query: " + secondarySql);
        cursors[1] = database.rawQuery(secondarySql, null);

        return new MergeCursor(cursors);
    }
    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        // TODO: Implement this to handle requests to update one or more rows.
        throw new UnsupportedOperationException("Not yet implemented");
    }


}
