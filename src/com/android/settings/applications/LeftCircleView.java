package com.android.settings.applications;

import android.content.Context;  
import android.graphics.Canvas;  
import android.graphics.Paint;  
import android.util.AttributeSet;  
import android.view.View;  
import com.android.settings.R;
  
public class LeftCircleView extends View {  
  
    private final  Paint paint;  
    private final Context context; 
      
    public LeftCircleView(Context context) {  
          
        // TODO Auto-generated constructor stub  
        this(context, null);  
    }  
  
    public LeftCircleView(Context context, AttributeSet attrs) {  
        super(context, attrs);  
        // TODO Auto-generated constructor stub  
        this.context = context;  
        this.paint = new Paint();  
        this.paint.setAntiAlias(true); //消除锯齿  
        this.paint.setColor(context.getColor(R.color.running_processes_system_ram));
    }  
     
    @Override  
    protected void onDraw(Canvas canvas) {  
        // TODO Auto-generated method stub  
        int center = getWidth()/2;  
          
        canvas.drawCircle(center,center, center, this.paint);  
          
        super.onDraw(canvas);  
    }  
      
}  
