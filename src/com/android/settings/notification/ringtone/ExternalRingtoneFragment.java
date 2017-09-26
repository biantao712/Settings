package com.android.settings.notification.ringtone;

import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by leaon_wang on 2017/5/16.
 */

public class ExternalRingtoneFragment extends CNRingtoneBaseFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "ExternalRingtone";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LoaderManager loaderManager = getLoaderManager();
        Loader loader = loaderManager.getLoader(0);
        if (loader == null) {
            loaderManager.destroyLoader(0);
        }
        loaderManager.initLoader(0,null,this);
        return super.onCreateView(inflater,container,savedInstanceState);
    }

    private CursorLoader getCursorLoader() {
        Log.i(TAG,"getCursorLoader");
        //if (!PermissionCheckHelper.isPermissionGrant(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE))
        //   return null;
        String filterNull = MediaStore.Audio.Media.DATA + " is not null AND " + MediaStore.Audio.Media.DISPLAY_NAME + " is not null" +
                " ) " + " group by " + " ( " + MediaStore.Audio.Media.TITLE;
        String[] media_music_info = new String[]{MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.ALBUM_ID};
        return new CursorLoader(getActivity(), MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                media_music_info, filterNull, null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
    }

    public Cursor getCursor(String query) {
        Log.i(TAG,"getCursor");
        mQuery = query;
        Cursor cursor = getCustomCursor();
        if (query == null) {
            return cursor;
        }
        MatrixCursor extras = new MatrixCursor(new String[]{MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.ALBUM_ID});
        if (cursor != null) {
            cursor.moveToFirst();
            for (int i = 0; i < cursor.getCount(); i++) {
                String nameNoSuffix = customName(cursor.getString(4));
                if (nameNoSuffix != null && nameNoSuffix.contains(query.trim())
                        || nameNoSuffix.toLowerCase().contains(query.trim().toLowerCase())
                        || nameNoSuffix.toUpperCase().contains(query.trim().toUpperCase())) {
                    String[] silentArray = new String[]{cursor.getString(0), cursor.getString(1), cursor.getString(2),
                            cursor.getString(3), cursor.getString(4), cursor.getString(5), cursor.getString(6)};
                    extras.addRow(silentArray);
                }
                cursor.moveToNext();
            }
            cursor.close();
        }
        return extras;
    }

    private String customName(String name) {
        int end = name.lastIndexOf(".");
        if (end > -1 && end < name.length()) {
            name = name.substring(0, end);
        }
        return name;
    }

    private Cursor getCustomCursor() {
        //if (!PermissionCheckHelper.isPermissionGrant(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE))
        //    return null;
        String filterNull = MediaStore.Audio.Media.DATA + " is not null AND " + MediaStore.Audio.Media.DISPLAY_NAME + " is not null" +
                " ) " + " group by " + " ( " + MediaStore.Audio.Media.TITLE;
        String[] media_music_info = new String[]{MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.ALBUM_ID};
        return getActivity().getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                media_music_info, filterNull, null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.i(TAG,"onCreateLoader");
        return getCursorLoader();
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.i(TAG,"onLoadFinished");
        if (TextUtils.isEmpty(mQuery)) {
            refreshData(data, true);
        } else {
            refreshData(getCursor(mQuery), true);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.i(TAG,"onLoaderReset");
    }
}
