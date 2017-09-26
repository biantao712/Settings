package com.android.settings.notification.ringtone;

import android.app.Fragment;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import com.android.settings.R;

import java.io.File;


/**
 * Created by leaon_wang on 2017/5/16.
 */

public class CNRingtoneBaseFragment extends Fragment implements SimpleCursorAdapter.ViewBinder, AudioManager.OnAudioFocusChangeListener {
    private static final String TAG = "CNRingtoneBaseFragment";
    private static final String RINGTONE_TYPE = "ringtone_type";
    public static final String RINGTONE_SILENT = "silent";
    private static final String DEFAULT_RINGTONE_PROPERTY_PREFIX = "ro.config.";

    protected String mQuery;
    private CNRingtonePickerActivity mActivity;
    protected Resources mResources;
    private ListView mListView;
    private SimpleCursorAdapter mAdapter;

    private  int mRingtoneType;
    protected String mRingtoneTypeSelecion;
    private Uri mDefaultRingtoneUri;
    private Uri mRingtoneUri;
    private int mStreamType;
    private String mDefaultFileName;

    private boolean mIsExternal;
    String[] paths;
    String[] musicTitles;
    String[] ids;

    public static Uri newUri;
    private boolean mPlaying = false;
    private MediaPlayer mMediaPlayer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = (CNRingtonePickerActivity) getActivity();
        mResources = mActivity.getResources();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.cn_ringtone_base_fragment,null);
        initLayout(rootView);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mRingtoneType = mActivity.getRingtoneType();
        Log.i(TAG,"mRingtoneType = "+mRingtoneType);
        switch (mRingtoneType){
            case RingtoneManager.TYPE_ALARM:
                mDefaultFileName = android.os.SystemProperties.get(DEFAULT_RINGTONE_PROPERTY_PREFIX + Settings.System.ALARM_ALERT);
                mDefaultRingtoneUri = Settings.System.DEFAULT_ALARM_ALERT_URI;
                mRingtoneUri = RingtoneManager.getActualDefaultRingtoneUri(mActivity,RingtoneManager.TYPE_ALARM);
                mRingtoneTypeSelecion = MediaStore.Audio.Media.IS_ALARM;
                mStreamType = AudioManager.STREAM_ALARM;
                break;
            case RingtoneManager.TYPE_NOTIFICATION:
                mDefaultFileName = android.os.SystemProperties.get(DEFAULT_RINGTONE_PROPERTY_PREFIX + Settings.System.NOTIFICATION_SOUND);
                mDefaultRingtoneUri = Settings.System.DEFAULT_NOTIFICATION_URI;
                mRingtoneUri = RingtoneManager.getActualDefaultRingtoneUri(mActivity,RingtoneManager.TYPE_NOTIFICATION);
                mRingtoneTypeSelecion = MediaStore.Audio.Media.IS_NOTIFICATION;
                mStreamType = AudioManager.STREAM_NOTIFICATION;
                break;
            case RingtoneManager.TYPE_RINGTONE:
                mDefaultFileName = android.os.SystemProperties.get(DEFAULT_RINGTONE_PROPERTY_PREFIX + Settings.System.RINGTONE);
                mDefaultRingtoneUri = Settings.System.DEFAULT_RINGTONE_URI;
                mRingtoneUri = RingtoneManager.getActualDefaultRingtoneUri(mActivity,RingtoneManager.TYPE_RINGTONE);
                mRingtoneTypeSelecion = MediaStore.Audio.Media.IS_RINGTONE;
                mStreamType = AudioManager.STREAM_RING;
                break;
            case RingtoneManager.TYPE_RINGTONE_2:
                mDefaultFileName = android.os.SystemProperties.get(DEFAULT_RINGTONE_PROPERTY_PREFIX + Settings.System.RINGTONE);
                mDefaultRingtoneUri = Settings.System.DEFAULT_RINGTONE_URI_2;
                mRingtoneUri = RingtoneManager.getActualDefaultRingtoneUri(mActivity,RingtoneManager.TYPE_RINGTONE_2);
                mRingtoneTypeSelecion = MediaStore.Audio.Media.IS_RINGTONE;
                mStreamType = AudioManager.STREAM_RING;
                break;
            default:
                mDefaultRingtoneUri = Settings.System.DEFAULT_RINGTONE_URI;
                mRingtoneUri = RingtoneManager.getActualDefaultRingtoneUri(mActivity,RingtoneManager.TYPE_RINGTONE);
                mRingtoneTypeSelecion = MediaStore.Audio.Media.IS_RINGTONE;
                mStreamType = AudioManager.STREAM_RING;
                break;
        }
        Log.i(TAG,"mDefaultRingtoneUri = "+mDefaultRingtoneUri);
        Log.i(TAG,"mRingtoneUri = "+mRingtoneUri);
        Log.i(TAG,"Default file name = "+mDefaultFileName);
    }

    @Override
    public void onPause() {
        super.onPause();
        stop();
    }

    private void initLayout(View rootView) {
        mListView = (ListView) rootView.findViewById(R.id.ringtone_list);
        String title = MediaStore.Audio.Media.TITLE;
        String[] from = {title};
        int[] to = {R.id.textview};
        mAdapter = new SimpleCursorAdapter(getActivity(), R.layout.cn_ringtone_picker_list_item, null, from, to, 0);
        TextView emptyView = (TextView) rootView.findViewById(R.id.no_result_found);
        mListView.setEmptyView(emptyView);
        mListView.setAdapter(mAdapter);
        mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                stop();
                final Uri playUri = getUri(position);
                CNRingtonePickerActivity.ringtoneUri = playUri;
                if (playUri != null && !RINGTONE_SILENT.equals(playUri.toString())) {
                    new Thread() {
                        public void run() {
                            play(playUri);
                        }
                    }.start();
                }
                mListView.setItemChecked(position, true);
            }
        });
        mAdapter.setViewBinder(this);
    }

    private Uri getUri(int which){
        if (ids[which].equals("-1")) {
            newUri = Uri.parse(RINGTONE_SILENT);
            return newUri;
        }//else if (ids[which].equals("-2")) {
//            return mDefaultRingtoneUri;
//        }
        File sdFile = new File(paths[which]);
        Uri uri = MediaStore.Audio.Media.getContentUriForPath(sdFile.getAbsolutePath());
        Log.i(TAG,"getUri-->before uri="+uri);
        if (mIsExternal) {
            uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            Log.i(TAG,"getUri-->external uri="+uri);
        }
        if (uri == null) {
            return newUri;
        }
        String newUriString = uri.toString() + "/" + ids[which];
        newUri = Uri.parse(newUriString);
        Log.i(TAG,"getUri-->after uri="+newUri);
        return newUri;
    }

    public void refreshData(Cursor data, boolean isExternal) {
        mIsExternal = isExternal;
        mListView.clearChoices();
        if (!mIsExternal) {
            data = insertSilentAndDefault(data);
        }
        mAdapter.swapCursor(data);
        String ringtoneId = getRingtoneId();

        if (data != null) {
            paths = new String[data.getCount()];
            musicTitles = new String[data.getCount()];
            ids = new String[data.getCount()];
            int selectedPos = -1;
            data.moveToFirst();
            for (int i = 0; i < data.getCount(); i++) {
                paths[i] = data.getString(5).substring(0);
                musicTitles[i] = data.getString(0).substring(0);
                ids[i] = data.getString(3).substring(0);
                if (ids[i].equals(ringtoneId)) {
                    selectedPos = i;
                }
                if (CNRingtonePickerActivity.ringtoneUri != null && CNRingtonePickerActivity.ringtoneUri.toString().startsWith("file://")) {
                    String ringPath = CNRingtonePickerActivity.ringtoneUri.getPath();
                    if (paths[i].equals(ringPath)) {
                        selectedPos = i;
                    }
                }
                data.moveToNext();
            }
            if (selectedPos != -1) {
                mListView.setSelection(selectedPos);
                mListView.setItemChecked(selectedPos, true);
            }
        }
    }

    /**
     * 切换页面时，重置指针
     */
    public void reset() {
        if (ids == null)
            return;
        stop();
//        mListView.clearChoices();
        int position = mListView.getCheckedItemPosition();
        if (position != ListView.INVALID_POSITION) {
            CNRingtonePickerActivity.ringtoneUri = getUri(position);
        }
        String ringId = getRingtoneId();
        for (int i = 0; i < ids.length; i++) {
            if (ids[i].equals(ringId)) {
                mListView.setSelection(i);
                mListView.setItemChecked(i, true);
                getUri(i);
            }
        }
    }

    /**
     * 获取ringtone的id
     * @return
     */
    private String getRingtoneId() {
        String uriString = CNRingtonePickerActivity.ringtoneUri == null ? null : CNRingtonePickerActivity.ringtoneUri.toString();
        if(uriString == null || RINGTONE_SILENT.equals(uriString)){
            return "-1";
        }
//        if(uriString.equals(mDefaultRingtoneUri.toString())){
//            return "-2";
//        }
        String ret = "";
        if (uriString != null && ((mIsExternal && uriString.contains("external"))
                || !mIsExternal && !uriString.contains("external"))) {
            String[] sp = uriString.split("/");
            ret = sp[sp.length - 1];
        }
        return ret;
    }

    /**
     * 添加静音选项和默认铃声选项
     * @param cursor
     * @return
     */
    private Cursor insertSilentAndDefault(Cursor cursor) {
        if (cursor != null && cursor.getCount() == 0) {
            Log.i(TAG,"insertSilentAndDefault.cursor is null");
            return cursor;
        }
        MatrixCursor extras = new MatrixCursor(new String[]{MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.ALBUM_ID});
        String silent = getResources().getString(R.string.ringtone_none);
        String[] silentArray = new String[]{silent, silent, silent, "-1", silent, RINGTONE_SILENT, null};
        extras.addRow(silentArray);
//        String defaultRing = mResources.getString(R.string.ringtone_default);
//        String[] defaultArray = new String[]{defaultRing, defaultRing, defaultRing, "-2",defaultRing, mDefaultRingtoneUri.getPath(), null};
//        extras.addRow(defaultArray);
        Cursor[] cursors = {extras, cursor};
        Cursor extendedCursor = new MergeCursor(cursors);
        return extendedCursor;
    }

    /**
     * 设置显示界面
     * @param view
     * @param cursor
     * @param columnIndex
     * @return
     */
    @Override
    public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        CheckedTextView tv = (CheckedTextView) view;
        String textString = cursor.getString(columnIndex);
        if (textString != null ) {
            if (!mIsExternal && mDefaultFileName != null && mDefaultFileName.startsWith(textString)){
                textString += "(" + mResources.getString(R.string.ringtone_default) + ")";
            }
            tv.setText(textString);
            return true;
        }
        return false;
    }

    private synchronized void play(Uri uri){
        if (mPlaying) {
            return;
        }
        // TODO: Reuse mMediaPlayer instead of creating a new one and/or use RingtoneManager.
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                mp.stop();
                mp.release();
                mMediaPlayer = null;
                return true;
            }
        });

        try {
            mMediaPlayer.setDataSource(getActivity(), uri);
            startAlarm(mMediaPlayer);
        } catch (Exception ex) {
            // The alert may be on the sd card which could be busy right now. Use the fallback ringtone.
            android.util.Log.e(TAG, "play, use the default ringtone to replace, " + ex.getMessage());
            try {
                // Must reset the media player to clear the error state.
                mMediaPlayer.reset();
                //setDataSourceFromResource(getResources(), mMediaPlayer, R.raw.fallbackring);
                //startAlarm(mMediaPlayer);
            } catch (Exception ex2) {
                // At this point we just don't play anything.
            }
        }
        mPlaying = true;
    }

    private void setDataSourceFromResource(Resources resources, MediaPlayer player, int res) throws java.io.IOException {
        AssetFileDescriptor afd = resources.openRawResourceFd(res);
        if (afd != null) {
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
        }
    }

    private void startAlarm(MediaPlayer player)
            throws java.io.IOException, IllegalArgumentException, IllegalStateException {
        final AudioManager audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        // do not play alarms if stream volume is 0 (typically because ringer mode is silent).
        audioManager.requestAudioFocus(this, mStreamType,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        if (audioManager.getStreamVolume(mStreamType) != 0) {
            player.setAudioStreamType(mStreamType);
            //player.setLooping(true);
            player.prepare();
            player.start();
        }
    }

    /**
     * 停止播放
     */
    private synchronized void stop(){
        if (mPlaying)
            mPlaying = false;
        if(mMediaPlayer != null){
            mMediaPlayer.stop();
            if(getActivity() != null){
                final AudioManager audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
                audioManager.abandonAudioFocus(this);
            }
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        Log.i(TAG,"onAudioFocusChange.focusChange = "+focusChange);
        if(focusChange == -1)
            return;
        stop();
    }

    private void printCursor(Cursor cursor){
        if(cursor == null){
            Log.i(TAG,"cursor is null");
            return;
        }
        if(!cursor.moveToFirst()){
            Log.i(TAG,"cursor moveToFirst error");
            return;
        }
        do{
            Long id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
            String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
            String uri = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
            Long alumb_id =  cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
            Log.i(TAG,"[id: "+id+"\t title: "+title +"\t uri:"+uri+"\t album: "+alumb_id+"]");
        }while(cursor.moveToNext());
    }
}
