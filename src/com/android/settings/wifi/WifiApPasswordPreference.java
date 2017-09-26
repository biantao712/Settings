package com.android.settings.wifi;

import com.android.settings.CustomEditTextPreference;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.android.settings.R;

public class WifiApPasswordPreference extends CustomEditTextPreference {
    private static final String TAG = "WifiApPasswordPreference";
    
    private EditText editText;

    public WifiApPasswordPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    @Override
    public void onAttached() {
        super.onAttached();
    }
    
    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        editText = (EditText) view.findViewById(android.R.id.edit);
        if (editText != null) {
        	editText.setHint(R.string.wifi_password_hint);
//            editText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        	editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            editText.removeTextChangedListener(m_watcher);
            editText.addTextChangedListener(m_watcher);
            onEditTextChanged();
        }
    }
    
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
        	
        }
    }
    
    protected void onEditTextChanged()
    {
        boolean enable = false;
//        if(editText != null && editText.getText() != null && editText.getText().toString().length() >= 8)
        	enable = true;
        	
        Dialog dlg = getDialog();
        if(dlg instanceof AlertDialog)
        {
            AlertDialog alertDlg = (AlertDialog)dlg;
            Button btn = alertDlg.getButton(AlertDialog.BUTTON_POSITIVE);
            btn.setEnabled(enable);                
        }
    }
    
    TextWatcher m_watcher = new TextWatcher(){
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
