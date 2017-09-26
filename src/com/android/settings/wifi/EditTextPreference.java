package com.android.settings.wifi;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.android.settings.R;

public class EditTextPreference extends Preference {
    private static final String TAG = "WifiEditTextPreference";
    
    private Context mContext;
    private EditText mEditText;
    private CheckBox mCheckBox;
    private OnCheckedChangeListener mListener;
    private TextWatcher m_watcher;
    
    private boolean mInit = false;
    private int mInputType = InputType.TYPE_CLASS_TEXT;
    private boolean mIsPassword = false;
    private String mText;
    private String mHint;
    private boolean mTextSet;
    private boolean mFocused = false;
    private InputFilter[] mFilters;

    public EditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        
        m_watcher = new TextWatcher(){
        	@Override
            public void onTextChanged(CharSequence s, int start, int before, int count){}

            @Override
            public void beforeTextChanged(CharSequence s, int start, int before, int count){}

            @Override
            public void afterTextChanged(Editable s)
            {
                onEditTextChanged();
            }
        };
    }
    
    public void init(int inputType, String hint, boolean noDivider){
    	mInit = true;
    	mInputType = inputType;
    	mHint = hint;
    	
    	if(noDivider)
    		setLayoutResource(R.layout.asusres_preference_wifi_edit_text_nodivider);
    	else
    		setLayoutResource(R.layout.asusres_preference_wifi_edit_text);
    	
    	if((inputType == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) 
    			|| (inputType == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD))){
    		mIsPassword = true;
	    	mListener = new OnCheckedChangeListener(){
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					// TODO Auto-generated method stub
					mInputType = isChecked ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
							: (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
					
					if (mEditText != null) {
						mEditText.removeTextChangedListener(m_watcher);
			        	mEditText.setInputType(mInputType);
			        	mEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
			        	mEditText.addTextChangedListener(m_watcher);
			        	
//			        	mEditText.requestFocus();
			        	mEditText.setSelection(mEditText.length());
			        }
				}
	    	};
    	}
    }
    
    public void init(int inputType, int hintResid, boolean noDivider){
    	init(inputType, mContext.getString(hintResid), noDivider);
    }

    public void init(int inputType, int hintResid, boolean noDivider, InputFilter[] filters){
        mFilters = filters;
        init(inputType, hintResid, noDivider);
    }

    public void init(int inputType, String hint){
    	init(inputType, hint, false);
    }
    
    public void init(int inputType, int hintResid){
    	init(inputType, mContext.getString(hintResid));
    }
    
    public void init(int inputType){
    	init(inputType, null);
    }
    
    public void init(String hint, boolean noDivider){
    	init(InputType.TYPE_CLASS_TEXT, hint, noDivider);
    }
    
    public void init(int hintResid, boolean noDivider){
    	init(mContext.getString(hintResid), noDivider);
    }
    
    public void init(boolean noDivider){
    	init(InputType.TYPE_CLASS_TEXT, null, noDivider);
    }
    
    public void init(){
    	init(false);
    }
    
    @Override
    public void onAttached() {
        super.onAttached();
        if(!mInit)
        	init();
    }
    
    @Override
    protected void onClick() {
        if(mIsPassword){
            if(mCheckBox != null)
                mCheckBox.setChecked(!mCheckBox.isChecked());
        }
    }

    public void setFocus(boolean focused){
    	mFocused = focused;
    }
    
    public void setHint(String text) {
    	mHint = text;
    	if(mEditText != null){
    		mEditText.setHint(mHint);
    		mEditText.invalidate();
    	}
    }
    
    public void setHint(int resid) {
    	setHint(mContext.getString(resid));
    }
    
//    @Override
//    public void setSummary(int resid) {
//    	setHint(resid);
//    }

    public void setText(String text) {
        // Always persist/notify the first time.
        final boolean changed = !TextUtils.equals(mText, text);
        if (changed || !mTextSet) {
            mText = text;
            mTextSet = true;
//            persistString(text);
            if(changed) {
//                notifyDependencyChange(shouldDisableDependents());
                notifyChanged();
            }
        }
    }
    
    public String getText() {
//    	if(mEditText == null)
//    		return null;
//        return mEditText.getText().toString();
    	return mText;
    }
    
    @Override
    public void onBindViewHolder(final PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        Log.d("timhu", "onBindViewHolder mHint = " + mHint);
        
        LinearLayout editContainer = (LinearLayout) view.findViewById(R.id.edit_container);
        if (editContainer != null) {
        	if(mEditText != null)
        		mEditText.removeTextChangedListener(m_watcher);
        	
        	mEditText = obtainEditText();
        	editContainer.removeAllViews();
        	editContainer.addView(mEditText);
        	
        	mEditText.setInputType(mInputType);
            if(mFilters != null)
                mEditText.setFilters(mFilters);
        	if(mText != null){
        		mEditText.setText(mText);
        	}
        	if(mHint != null)
        		mEditText.setHint(mHint);
        	
//        	if(mIsPassword)
//    			mEditText.requestFocus();
    		mEditText.setSelection(mEditText.length());
    		mEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        	
        	mEditText.addTextChangedListener(m_watcher);
        	
        	if(mFocused){
        		mEditText.requestFocus();
        	} else{
        		mEditText.clearFocus();
        	}
        }
        
        mCheckBox = (CheckBox) view.findViewById(android.R.id.checkbox);
        if (mCheckBox != null) {
        	mCheckBox.setVisibility(mIsPassword ? View.VISIBLE : View.GONE);
        	mCheckBox.setOnCheckedChangeListener(mListener);
        }
    }
    
    protected void onEditTextChanged()
    {
    	if(mEditText != null){
    		mText = mEditText.getText().toString();
    		callChangeListener(mText);
    	}
    }

    private EditText obtainEditText(){
    	EditText editText = new EditText(mContext);
    	editText.setBackgroundColor(Color.TRANSPARENT);
    	editText.setSingleLine();
    	editText.setTextAppearance(android.R.attr.textAppearanceListItem);
    	editText.setTextColor(Color.parseColor("#0c0c0c"));
    	editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
    	editText.setHintTextColor(0xff9e9e9e);
    	editText.setPadding(0, 0, 0, 0);
    	
    	ViewGroup.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT
    			, ViewGroup.LayoutParams.MATCH_PARENT);
    	editText.setLayoutParams(params);
    	editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
    	return editText;
    }
}
