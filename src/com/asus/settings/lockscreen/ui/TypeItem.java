package com.asus.settings.lockscreen.ui;

/**
 * Created by Wesley_Sun on 2017/2/17.
 */
//package com.asus.screenlock.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.settings.R;
import android.util.Log;

public class TypeItem extends LinearLayout implements View.OnClickListener {
    public static final String TAG = "TypeItem_NewFeature";

    private ImageView mTypeImage;
    private TextView mTypeTitle;
    private TextView mCheckBtn;
    //private TextView mUnCheckBtn;
    //private LinearLayout mStyleCheckFlag;
    //private LinearLayout mStyleCheckRect;

//    private boolean mCheckFlag;

    public TypeItem(Context context) {
        super(context);
        init();
    }

    public TypeItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void setTypeName(CharSequence typeName) {
        Log.d(TAG,"Set type name to :" + typeName);
        mTypeTitle.setText(typeName);
    }

    public void setTypeImage(int id) {
        Log.d(TAG,"Set Image to ID: " + id);
        mTypeImage.setImageResource(id);
    }

    /**
     * 设置选中
     * @param checked FLAG
     */
    public void setChecked(boolean checked) {
        Log.d(TAG,"Enter setChecked :" + checked);
        if (checked) {
            Log.d(TAG,"set visible");;
            mCheckBtn.setVisibility(VISIBLE);
            mTypeTitle.setTextColor(0xFFFFFFFF);
            mCheckBtn.setTextColor(0xFFFFFFFF);
            //mStyleCheckFlag.setBackgroundColor(0xFFFFFFFF);
            //mStyleCheckRect.setVisibility(VISIBLE);
        } else {
            Log.d(TAG,"set invisible");
            //mStyleCheckFlag.setBackgroundColor(0xFF000000);
            //mStyleCheckRect.setVisibility(GONE);
            mCheckBtn.setVisibility(INVISIBLE);
            mTypeTitle.setTextColor(0xFF505050);
            mCheckBtn.setTextColor(0xFF505050);
        }
    }

    @Override
    public void onClick(View v) {
        Log.d(TAG,"enter functio onclcik: TypeItem>>" + v);
        if (mTypeItemListener != null) {
            Log.d(TAG,"listener is not null, call listener");
            mTypeItemListener.onCheckChanged(TypeItem.this,true);
        }
        else
        {
            Log.d(TAG,"listener is null, handle it myself");
        }
    }

    private TypeItemListener mTypeItemListener = null;

    public void setTypeItemListener(TypeItemListener listener) {
        Log.d(TAG,"Set listener");
        mTypeItemListener = listener;
    }

    public interface TypeItemListener {
        void onCheckChanged(View view,boolean check);
    }

    private void init() {
        Log.d(TAG,"enter init");
        inflate(getContext(), R.layout.item_setting_style, this);
    }

    /**
     * 控件采用ViewStub方式，只有显示时，才会真正绘制
     * @return 是否需要加载
     */
    public boolean showReal() {
        Log.d(TAG,"Enter show real");
        if (mTypeImage == null) {
            Log.d(TAG,"mTypeImage is null");
            initReal();
            return true;
        }
        else
        {
            Log.d(TAG,"mTypeImage is not null");
        }

        return false;
    }

    private void initReal() {
        try {
            Log.d(TAG, "Enter initReal");
            ViewStub viewStub = (ViewStub) findViewById(R.id.StyleViewStub);
            viewStub.inflate();
            mTypeImage = (ImageView) findViewById(R.id.StyleShowLay);
            mTypeTitle = (TextView) findViewById(R.id.StyleTitle);
            mCheckBtn = (TextView) findViewById(R.id.StyleCheckBtn);
            //mUnCheckBtn = (TextView) findViewById(R.id.StyleUnCheckBtn);
            //mStyleCheckFlag = (LinearLayout) findViewById(R.id.StyleCheckFlag);
            //mStyleCheckRect = (LinearLayout) findViewById(R.id.StyleCheckRect);
            setOnClickListener(this);

            mTypeImage.setVisibility(View.VISIBLE);
            mTypeTitle.setVisibility(View.VISIBLE);
            mCheckBtn.setVisibility(View.VISIBLE);
            //mUnCheckBtn.setVisibility(View.GONE);
            //mStyleCheckFlag.setVisibility(View.VISIBLE);
            mCheckBtn.setVisibility(View.VISIBLE);

            if(mTypeImage == null ||
                    mTypeTitle == null ||
                    mCheckBtn == null)
                    // ||mStyleCheckFlag == null
            {
                Log.d(TAG,"error variable is null");
            }
            else
            {
                Log.d(TAG,"variables is valid");
            }
//        mCheckFlag = false;
        }
        catch(Exception ee)
        {
            Log.d(TAG,"Error : " + ee);
        }
    }

    /**
     * 重新导入字符串
     */
    public void reloadString() {
        Log.d(TAG,"enter reloadString()");
        //if (mUnCheckBtn != null) {
        if (mCheckBtn != null) {
            //mUnCheckBtn.setText(R.string.au_style_un_use);
            mCheckBtn.setText(R.string.au_style_used);
        }
    }
}
