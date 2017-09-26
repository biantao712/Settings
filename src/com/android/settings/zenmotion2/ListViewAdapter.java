package com.android.settings.zenmotion2;

import java.util.List;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.settings.zenmotion2.AppList;
import com.android.settings.R;
import android.util.Log;

/**
 * Created by mark_guo on 2016/8/31.
 */
public class ListViewAdapter extends BaseAdapter {
	private static final String TAG = "ZenMotion2_ListViewAdapter";
	private String NotEnabled="\uD83D\uDEAB";//special unicode char: no entry
    private Context context;
    private List<AppData> list;
    private List<Drawable> isChecked;
    private List<CharSequence> pkgActivityName;
    private ViewHolder viewHolder;

    public ListViewAdapter(Context context, List<AppData> list) {
        this.context = context;
        this.list = list;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean isEnabled(int position) {
        // TODO Auto-generated method stub
        if (list.get(position).getName().length() == 1)// 如果是字母索引
            return false;// 表示不能点击
        return super.isEnabled(position);
    }
    public void SetChecked(){

    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
    	//Log.i(TAG,"getView+++");
        AppData appName=list.get(position);
        String item = appName.getName();
        //boolean isChecked=appName.getEnabled();
        boolean isAssigned=appName.getAssigned();
		Log.i(TAG,"position="+String.valueOf(position)+" appName="+item+" isAssigned="+String.valueOf(isAssigned));
		//Log.i(TAG,"position="+String.valueOf(position)+" appName="+item+" isChecked="+String.valueOf(isChecked));
        viewHolder = new ViewHolder();
        if (item.length() == 1) {//index header
			String index=list.get(position).getName();
			if(index.equals("!")){
				convertView = LayoutInflater.from(context).inflate(R.layout.zenmotion2_applist_listindex_not_enabled,
						null);
			}
			else{
				convertView = LayoutInflater.from(context).inflate(R.layout.zenmotion2_applist_listindex,
						null);
			}
            viewHolder.indexTv = (TextView) convertView
                    .findViewById(R.id.indexTv);
        } else {//item
	        Drawable icon=list.get(position).getIcon();
			if(icon==null){//check NotEnable item
				//convertView=LayoutInflater.from(context).inflate(R.layout.zenmotion2_applist_listitem_not_enable,
				convertView=LayoutInflater.from(context).inflate(R.layout.zenmotion2_applist_listitem,
				null);
				//Log.i(TAG,"change not enable layout:zenmotion2_applist_listitem_not_enable: "+list.get(position).getName());
			}
			else{
	            convertView = LayoutInflater.from(context).inflate(R.layout.zenmotion2_applist_listitem,
                    null);
			}
            viewHolder.itemTv = (TextView) convertView.findViewById(R.id.itemTv);
            viewHolder.appicon= (ImageView) convertView.findViewById(R.id.app_icon);
            viewHolder.isChecked= (ImageView) convertView.findViewById(R.id.isChecked);
        }
        if (item.length() == 1) {
			String index=list.get(position).getName();
			if(index.equals("!")){
				index=NotEnabled;
				//viewHolder.indexTv.setText("");//UI spec: don't show not enable icon
			}
			else
				viewHolder.indexTv.setText(index);
        } else {
            viewHolder.itemTv.setText(list.get(position).getName());
            // add app icon
            if(list!= null) {
                viewHolder.appicon.setImageDrawable(list.get(position).getIcon());//get the app icon from the position
                //appName.setEnabled(isChecked);
                appName.setEnabled(isAssigned);
                //if(isChecked) {
                if(isAssigned) {
                    //viewHolder.isChecked.setImageResource(android.R.drawable.checkbox_on_background);
                    viewHolder.isChecked.setVisibility(View.VISIBLE);
                }
                else{
                    viewHolder.isChecked.setVisibility(View.GONE);
                }
            }
        }
		//Log.i(TAG,"getView---");
        return convertView;
    }

    private class ViewHolder {
        private ImageView appicon;
        private ImageView isChecked;
        private TextView indexTv;
        private TextView itemTv;
    }
}
