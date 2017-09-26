package com.android.settings.bluelightfilter;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Switch;
import com.android.settings.R;

public class SeekBarDialog implements SeekBar.OnSeekBarChangeListener{

    private static final String TAG = "SeekBarDialog";
    private SeekBar mSeekBar;
    private int mReadingProgress = 0; //get from seek bar
    private int preLevel = 0; //before seekbar changes
    private Switch mSwitch;
    private Context mContext;
    private AlertDialog mAlertDialog;
    private AlertDialog.Builder mBuilder;
    private View contentView;

    public SeekBarDialog(Context context, Activity activity){
        mContext = context;
        mBuilder = new AlertDialog.Builder(activity)
        .setTitle(R.string.bluelightfilter_mode_title)
        .setCancelable(true)
        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        })
        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                revertToPreLevel();
                dialog.dismiss();
            }
        });

        contentView = onCreateDialogView();
        if (contentView != null) {
            mBuilder.setView(contentView);
        }
        mAlertDialog = mBuilder.create();
        setSeekBar();
    }

    public void show(){
        mAlertDialog.show();
    }

    public View onCreateDialogView() {
        preLevel = Settings.System.getInt(mContext.getContentResolver(),
        Constants.BLUELIGHT_FILTER_LEVEL, Constants.BLUELIGHT_FILTER_LEVEL_RDWEAK);
        LayoutInflater inflater = LayoutInflater.from(mBuilder.getContext());
        return inflater.inflate(R.layout.preference_dialog_bluelight_filter, null);
    }

    public void setSeekBar(){
        mSeekBar = (SeekBar)contentView.findViewById(R.id.seekbar_splendid);
        mSeekBar.setMax(Constants.BLUELIGHT_FILTER_MODE_SEEKBAR_MAX);
        int option = getBluelightFilterLevel();
        mSeekBar.setOnClickListener(null);
        mSeekBar.setProgress(option);
        mSeekBar.setOnSeekBarChangeListener(this);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,
            boolean fromUser) {
        // TODO Auto-generated method stub
        mReadingProgress = seekBar.getProgress();
        Settings.System.putInt(mContext.getContentResolver(),
                Constants.BLUELIGHT_FILTER_LEVEL, Constants.READING_MODE_SEEKBAR_PROGRESS2OPTION[mReadingProgress]);
        Log.d(TAG, "value of progress: "+Integer.toString(mReadingProgress));
        int option = getBluelightFilterLevel();
        Log.d(TAG, "value of option: "+Integer.toString(option));
        //Intent service_intent = new Intent(mContext, TaskWatcherService5Level.class);
        //service_intent.putExtra(EXTRA_QUICKSETTING_READER_MODE_ON_OFF, mEnable ? 1 : 0);
        //startService(service_intent);
        doCommand();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // TODO Auto-generated method stub
    }

public Switch getSwitch(){
        return mSwitch;
    }

    public void doCommand(){
        int option = getBluelightFilterLevel();
        String reading_mode = Constants.READING_MODE_OPTION2LUT[option];
        int cmdCTMode = -1;
        int cmdHSVMode = -1;
        cmdCTMode = SplendidCommand.MODE_CT;
        cmdHSVMode = SplendidCommand.MODE_HSV;
        if(SplendidCommand.isCommandExists(cmdCTMode)){
            try {
                execute(SplendidCommand.COMMAND_NAME_LIST[cmdCTMode] + " -d " + reading_mode);
            } catch (Exception e) {
                Log.w(TAG, "run command error!" + e);
            }
        } else {
            Log.d(TAG, SplendidCommand.COMMAND_NAME_LIST[cmdCTMode] + " not exist!");
        }

        if(SplendidCommand.isCommandExists(cmdHSVMode)){
            try {
                execute(SplendidCommand.COMMAND_NAME_LIST[cmdHSVMode] + " -r " + reading_mode);
            } catch (Exception e) {
                Log.w(TAG, "run command error!" + e);
            }
        } else {
            Log.d(TAG, SplendidCommand.COMMAND_NAME_LIST[cmdHSVMode] + " not exist!");
        }
    }

    public void execute(String cmd){
        if (!TextUtils.isEmpty(cmd)) {
            if (cmd.startsWith("HSVSetting") || cmd.startsWith("GammaSetting") || cmd.startsWith("DisplayColorSetting")) {
                Runtime run = Runtime.getRuntime();
                Process p = null;
                try {
                    p = run.exec(cmd);
                    BufferedInputStream in = new BufferedInputStream(p.getInputStream());
                    BufferedReader inBr = new BufferedReader(new InputStreamReader(in));
                    StringBuilder stringBuilder = new StringBuilder();
                    String lineStr = "";
                    while ((lineStr = inBr.readLine()) != null) {
                        stringBuilder.append(lineStr);
                    }
                    Log.i(TAG, "result :" + stringBuilder.toString());

                    if (p.waitFor() != 0 && p.exitValue() == 1) {
                        Log.w(TAG, "abnormally exit!");
                    }
                    inBr.close();
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    if (p != null) p.destroy();
                }
            } else {
                Log.d(TAG, "not supported command:" + cmd);
            }
        }
    }

    public int getBluelightFilterLevel(){
        int level = Settings.System.getInt(mContext.getContentResolver(),
                Constants.BLUELIGHT_FILTER_LEVEL, Constants.BLUELIGHT_FILTER_LEVEL_RDWEAK);
        if (level < 0 || level > 4) level = 0;
        return level;
    }

    public void revertToPreLevel(){
        Settings.System.putInt(mContext.getContentResolver(),
             Constants.BLUELIGHT_FILTER_LEVEL, preLevel);
        doCommand();
    }
}
