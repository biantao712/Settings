package com.android.settings.notification;

import com.android.settings.R;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.app.ActionBar;
import android.widget.TextView;
import android.widget.CheckBox;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.graphics.Color;
import android.content.Intent;
import android.database.Cursor;
import android.database.StaleDataException;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.content.Context;
import android.view.ViewGroup;
import android.provider.MediaStore;
import android.widget.AdapterView;
import android.os.Handler;
import android.widget.Toolbar;
import android.widget.ImageView;

public class CNSoundRingtonePickerSettingsActivity extends Activity implements
        AdapterView.OnItemClickListener ,Runnable{
	
	private static final String TAG = "CNSoundRingtonePickerSettingsActivity";
	
	private static final int DELAY_MS_SELECTION_PLAYED = 300;
	
	private static final int POS_UNKNOWN = -1;
	
	private static final String SAVE_CLICKED_POS = "clicked_pos";
	
	private ActionBar mActionBar;
	
	/** The position in the list of the 'Silent' item. */
    private int mSilentPos = POS_UNKNOWN;

    /** The position in the list of the 'Default' item. */
    private int mDefaultRingtonePos = POS_UNKNOWN;

    /** The position in the list of the last clicked item. */
    private int mClickedPos = POS_UNKNOWN;

    /** The position in the list of the ringtone to sample. */
    private int mSampleRingtonePos = POS_UNKNOWN;
	
	/** Whether this list has the 'Default' item. */
    private boolean mHasDefaultItem;

    /** The Uri to play when the 'Default' item is clicked. */
    private Uri mUriForDefaultItem;
	
	 /** Whether this list has the 'Silent' item. */
    private boolean mHasSilentItem;
	
	/** The number of static items in the list. */
    private int mStaticItemCount = 0;
	
	/** The Uri to place a checkmark next to. */
    private Uri mExistingUri;
	
	private int mAttributesFlags;
	
	/**
     * A Ringtone for the default ringtone. In most cases, the RingtoneManager
     * will stop the previous ringtone. However, the RingtoneManager doesn't
     * manage the default ringtone for us, so we should stop this one manually.
     */
    private Ringtone mDefaultRingtone;

    /**
     * The ringtone that's currently playing, unless the currently playing one is the default
     * ringtone.
     */
    private Ringtone mCurrentRingtone;
	
	/**
     * Keep the currently playing ringtone around when changing orientation, so that it
     * can be stopped later, after the activity is recreated.
     */
    private static Ringtone sPlayingRingtone;
	
	private RingtoneManager mRingtoneManager;
	private int mType;
	private Cursor mCursor;
	
	private String mTitle = "";
	
	private ListView mListView;
	
	private Handler mHandler;
	
	private CheckBox mSilentCB;
	
	 @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		setContentView(R.layout.cn_sound_ringtone_picker_layout);
		
		Intent intent = getIntent();
		
		mHandler = new Handler();
		
		if (savedInstanceState != null) {
            mClickedPos = savedInstanceState.getInt(SAVE_CLICKED_POS, POS_UNKNOWN);
        }
		
		mHasDefaultItem = intent.getBooleanExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
		Log.i(TAG,"mHasDefaultItem = " + mHasDefaultItem);
        mUriForDefaultItem = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI);
		Log.i(TAG,"mUriForDefaultItem = " + mUriForDefaultItem);
        if (mUriForDefaultItem == null) {
            mUriForDefaultItem = Settings.System.DEFAULT_RINGTONE_URI;
        }
		Log.i(TAG,"mUriForDefaultItem = " + mUriForDefaultItem);
		
		// Get whether to show the 'Silent' item
        mHasSilentItem = intent.getBooleanExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
		Log.i(TAG,"mHasSilentItem = " + mHasSilentItem);
        // AudioAttributes flags
        mAttributesFlags |= intent.getIntExtra(
                RingtoneManager.EXTRA_RINGTONE_AUDIO_ATTRIBUTES_FLAGS,
                0 /*defaultValue == no flags*/);
		Log.i(TAG,"mAttributesFlags = " + mAttributesFlags);
		
		// Get the URI whose list item should have a checkmark
        mExistingUri = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI);
		Log.i(TAG,"mExistingUri = " + mExistingUri);
		
		mRingtoneManager = new RingtoneManager(this);

        // Get the types of ringtones to show
        mType = intent.getIntExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, -1);
		Log.i(TAG,"mType = " + mType);
        if (mType != -1) {
            mRingtoneManager.setType(mType);
        }
		
		mTitle = intent.getStringExtra(RingtoneManager.EXTRA_RINGTONE_TITLE);
        if (mTitle == null) {
            mTitle = getString(com.android.internal.R.string.ringtone_picker_title);
        }

        mCursor = mRingtoneManager.getCursor();
		
		// The volume keys will control the stream that we are choosing a ringtone for
        setVolumeControlStream(mRingtoneManager.inferStreamType());
		
		setCustomActionBar();
		setStatusBarColor();
		initView();
	}
	
	 @Override
    protected void onStop() {
        super.onStop();
        mHandler.removeCallbacks(this);
        //mCursor.deactivate();

        if (!isChangingConfigurations()) {
            stopAnyPlayingRingtone();
        } else {
            saveAnyPlayingRingtone();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!isChangingConfigurations()) {
            stopAnyPlayingRingtone();
        }
    }
	
	@Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SAVE_CLICKED_POS, mClickedPos);
    }
	
	private void saveAnyPlayingRingtone() {
        if (mDefaultRingtone != null && mDefaultRingtone.isPlaying()) {
            sPlayingRingtone = mDefaultRingtone;
        } else if (mCurrentRingtone != null && mCurrentRingtone.isPlaying()) {
            sPlayingRingtone = mCurrentRingtone;
        }
    }
	
	CursorAdapter mAdapter;
	private void initView(){
		mListView = (ListView)findViewById(R.id.content_list);
		mAdapter = new CursorAdapter(this,mCursor,false) {
		private final int mLabelIndex;
            {
                final Cursor cursor = getCursor();
                mLabelIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
            }
            @Override
            public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
                return LayoutInflater.from(CNSoundRingtonePickerSettingsActivity.this).inflate(R.layout.cn_sound_ringtone_list_item,viewGroup,false);
            }

            @Override
            public void bindView(View view, Context context, Cursor cursor) {
				CheckBox cb = (CheckBox)view.findViewById(R.id.checkbox);
				cb.setText(cursor.getString(mLabelIndex));
            }
			
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View view = super.getView(position, convertView, parent);
				CheckBox cb = (CheckBox)view.findViewById(R.id.checkbox);
				if(mClickedPos == getListPosition(position)){
					cb.setChecked(true);
				}else{
					cb.setChecked(false);
				}
				return view;
			}
        };
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(this);
		onPrepareListView(mListView);
	}
	
	/**
	 * add DefaultItem && SilentItem
	**/
	private void onPrepareListView(ListView listView){
	
		//cancel DefaultItem
		
        if (mHasSilentItem) {
            mSilentPos = addSilentItem(listView);

            // The 'Silent' item should use a null Uri
            if (mClickedPos == POS_UNKNOWN && mExistingUri == null) {
                mClickedPos = mSilentPos;
            }
        }

        if (mClickedPos == POS_UNKNOWN) {
            mClickedPos = getListPosition(mRingtoneManager.getRingtonePosition(mExistingUri));
        }
		Log.i(TAG,"onPrepareListView.mClickedPos = " + mClickedPos);
		listView.setItemChecked(mClickedPos, true);
		listView.setSelection(mClickedPos);
	}
	
	/**
     * Adds a static item to the top of the list. A static item is one that is not from the
     * RingtoneManager.
     *
     * @param listView The ListView to add to.
     * @param textResId The resource ID of the text for the item.
     * @return The position of the inserted item.
     */
    private int addStaticItem(ListView listView, int textResId) {
        View view =  getLayoutInflater().inflate(
                R.layout.cn_sound_ringtone_list_item, listView, false);
		mSilentCB = (CheckBox)view.findViewById(R.id.checkbox);
        mSilentCB.setText(textResId);
        listView.addHeaderView(view);
        mStaticItemCount++;
        return listView.getHeaderViewsCount() - 1;
    }

    private int addDefaultRingtoneItem(ListView listView) {
        if (mType == RingtoneManager.TYPE_NOTIFICATION) {
            //return addStaticItem(listView, R.string.notification_sound_default);
			return 0;
        } else if (mType == RingtoneManager.TYPE_ALARM) {
            //return addStaticItem(listView, R.string.alarm_sound_default);
			return 0;
        }

        //return addStaticItem(listView, R.string.ringtone_default);
		return 0;
    }

    private int addSilentItem(ListView listView) {
        return addStaticItem(listView, R.string.cn_sound_settings_ringtone_silent);
    }
	
	private void setCustomActionBar(){
		Toolbar mToolbar = (Toolbar) findViewById(R.id.action_bar);
        setActionBar(mToolbar);
        getActionBar().setDisplayHomeAsUpEnabled(false);
        getActionBar().setHomeButtonEnabled(false);
        getActionBar().setDisplayShowTitleEnabled(false);
        mToolbar.setContentInsetsRelative(0,0);
        mToolbar.setContentInsetsAbsolute(0,0);

        ImageView back = (ImageView)findViewById(R.id.action_bar_back);
		back.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onBackPressed();
                    }
                });
				
		TextView titleTV = (TextView)findViewById(R.id.toolbar_title);
		titleTV.setText(mTitle);

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().setStatusBarColor(getColor(R.color.action_bar_background));
	}
	
	@Override
    public void onBackPressed() {
		Intent resultIntent = new Intent();
        Uri uri = null;

        if (mClickedPos == mDefaultRingtonePos) {
                // Set it to the default Uri that they originally gave us
			Log.i(TAG,"onBackPressed  mDefaultRingtonePos");
            uri = mUriForDefaultItem;
        } else if (mClickedPos == mSilentPos) {
            // A null Uri is for the 'Silent' item
            uri = null;
			Log.i(TAG,"onBackPressed  mSilentPos");
        } else {
			Log.i(TAG,"onBackPressed getUri");
            uri = mRingtoneManager.getRingtoneUri(getRingtoneManagerPosition(mClickedPos));
        }
		
		Log.i(TAG,"onBackPressed.uri = " + uri);

        resultIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, uri);
        setResult(RESULT_OK, resultIntent);
		super.onBackPressed();
    }
	
	private void setStatusBarColor(){
		Window window = getWindow();
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(getColor(R.color.action_bar_background));
	}
	/*
	public void onItemSelected(AdapterView parent, View view, int position, long id) {
		Log.i(TAG,"onItemSelected.position = " + position);
        playRingtone(position, DELAY_MS_SELECTION_PLAYED);
    }
	
	public void onNothingSelected(AdapterView parent) {}
	*/
	
	public void onItemClick(AdapterView parent, View view,int position, long id) {
		Log.i(TAG,"onItemClick.position = " + position);
		playRingtone(position, DELAY_MS_SELECTION_PLAYED);
		mClickedPos = position;
		mAdapter.notifyDataSetChanged();
		if(mSilentCB != null){
			if(mClickedPos == mSilentPos){
				mSilentCB.setChecked(true);
			}else{
				mSilentCB.setChecked(false);
			}
		}
	}
	
	private void playRingtone(int position, int delayMs) {
        mHandler.removeCallbacks(this);
        mSampleRingtonePos = position;
        mHandler.postDelayed(this, delayMs);
    }
	
	public void run() {
		Log.i(TAG,"rintone.run()");
        stopAnyPlayingRingtone();
        if (mSampleRingtonePos == mSilentPos) {
            return;
        }

        Ringtone ringtone;
        if (mSampleRingtonePos == mDefaultRingtonePos) {
            if (mDefaultRingtone == null) {
                mDefaultRingtone = RingtoneManager.getRingtone(this, mUriForDefaultItem);
            }
           /*
            * Stream type of mDefaultRingtone is not set explicitly here.
            * It should be set in accordance with mRingtoneManager of this Activity.
            */
            if (mDefaultRingtone != null) {
                mDefaultRingtone.setStreamType(mRingtoneManager.inferStreamType());
            }
            ringtone = mDefaultRingtone;
            mCurrentRingtone = null;
        } else {
            try {
               ringtone =mRingtoneManager.getRingtone(
                          getRingtoneManagerPosition(mSampleRingtonePos));
            } catch (StaleDataException staleDataException) {
               ringtone = null;
            } catch (IllegalStateException illegalStateException) {
               ringtone = null;
            }
            mCurrentRingtone = ringtone;
        }

        if (ringtone != null) {
            if (mAttributesFlags != 0) {
                ringtone.setAudioAttributes(
                        new AudioAttributes.Builder(ringtone.getAudioAttributes())
                                .setFlags(mAttributesFlags)
                                .build());
            }
			Log.i(TAG,"rintone.play()");
            ringtone.play();
        }
    }
	
	private void stopAnyPlayingRingtone() {
        if (sPlayingRingtone != null && sPlayingRingtone.isPlaying()) {
            sPlayingRingtone.stop();
        }
        sPlayingRingtone = null;

        if (mDefaultRingtone != null && mDefaultRingtone.isPlaying()) {
            mDefaultRingtone.stop();
        }

        if (mRingtoneManager != null) {
            mRingtoneManager.stopPreviousRingtone();
        }
    }
	
	private int getRingtoneManagerPosition(int listPos) {
        return listPos - mStaticItemCount;
    }
	
	private int getListPosition(int ringtoneManagerPos) {
		// If the manager position is -1 (for not found), return that
        if (ringtoneManagerPos < 0) return ringtoneManagerPos;

        return ringtoneManagerPos + mStaticItemCount;
    }
	
}