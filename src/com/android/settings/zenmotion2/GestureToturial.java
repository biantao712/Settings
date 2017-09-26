package com.android.settings.zenmotion2;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import com.android.settings.R;
import com.android.settings.zenmotion.GifView;

public class GestureToturial extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.zenmotion2_touchgesture_toturial);
        //button
        ImageButton imgbtn = (ImageButton) findViewById(R.id.close);
        imgbtn.setOnClickListener(this);
        //end
        GifView gifView = (GifView)findViewById(R.id.tutorial_gif);
        gifView.setGifResource(this,R.drawable.asus_w_gesture);

    }

    @Override
    public void onClick(View v) {
        finish();
    }
}
