package com.android.settings.notification.ringtone;

import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
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

public class InternalRingtoneFragment extends CNRingtoneBaseFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "InternalRingtone";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        return super.onCreateView(inflater,container,savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(0,null,this);
    }

    public CursorLoader getCursorLoader(){
        Log.i(TAG,"getCursorLoader");
        String selection = mRingtoneTypeSelecion + "=1" + " AND " + MediaStore.Audio.Media.DATA + " is not null";
        String[] media_music_info = new String[]{MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.ALBUM_ID};
        return new CursorLoader(getActivity(),MediaStore.Audio.Media.INTERNAL_CONTENT_URI,
                media_music_info, selection, null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
    }

    public Cursor getCursor(String query) {
        Log.i(TAG,"getCursor");
        mQuery = query;
        String selection = mRingtoneTypeSelecion + "=1" + " AND " + MediaStore.Audio.Media.DATA + " is not null";
        if (query != null && !query.trim().equals("")) {
            selection += " AND " + MediaStore.Audio.Media.TITLE + " LIKE '%" + query.trim() + "%'";
        }
        String[] media_music_info = new String[]{MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.ALBUM_ID};
        return getActivity().getContentResolver().query(MediaStore.Audio.Media.INTERNAL_CONTENT_URI,
                media_music_info, selection, null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.i(TAG,"onCreateLoader()");
        return getCursorLoader();
    }


    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.i(TAG,"onLoadFinished");
        if (TextUtils.isEmpty(mQuery)) {
            refreshData(data, false);
        } else {
            refreshData(getCursor(mQuery), false);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.i(TAG,"onLoaderReset");
        refreshData(null,false);
    }
}
