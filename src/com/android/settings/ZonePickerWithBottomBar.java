package com.android.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Toast;

import com.asus.cncommonres.AsusButtonBar;


public class ZonePickerWithBottomBar extends Fragment {

    private AsusButtonBar buttonBar;
    private ZonePicker zonePickerFrament;
    private boolean mSortByTimeZone = true;
    final static String SORT_BY_TIME_ZONE = "SortByTimeZone";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null){
            mSortByTimeZone = savedInstanceState.getBoolean(SORT_BY_TIME_ZONE);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        zonePickerFrament = (ZonePicker)getChildFragmentManager().findFragmentById(R.id.zone_picker);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.zone_picker_with_bottom_bar, container, false);
        buttonBar = (AsusButtonBar) view.findViewById(R.id.button_bar);
        if(buttonBar != null) {
            buttonBar.setVisibility(View.VISIBLE);
            buttonBar.addButton(1, R.drawable.asus_icon_sort, getString(R.string.order_text));
            buttonBar.getButton(1).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int selectedItem = mSortByTimeZone?0:1;
                    AlertDialog.Builder alertDialog_single_list_item = new AlertDialog.Builder(getContext())
                            .setSingleChoiceItems(new String[] { getString(R.string.zone_list_menu_sort_by_timezone), getString(R.string.zone_list_menu_sort_alphabetically)},
                                    selectedItem, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // TODO Auto-generated method stub

                                    switch (which){
                                        case 0:
                                            zonePickerFrament.setSorting(true);
                                            mSortByTimeZone = true;
                                            dialog.dismiss();
                                            break;
                                        case 1:
                                            zonePickerFrament.setSorting(false);
                                            mSortByTimeZone = false;
                                            dialog.dismiss();
                                            break;
                                    }
                                }
                            })
                            .setTitle(getString(R.string.order_text))
                            .setNegativeButton(getString(android.R.string.cancel), null);
                    AlertDialog dialog = alertDialog_single_list_item.create();
                    Window window = dialog.getWindow();
                    window.setGravity(Gravity.BOTTOM);
                    dialog.show();
                }
            });
        }
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        zonePickerFrament.setSorting(mSortByTimeZone);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // TODO Auto-generated method stub
        outState.putBoolean(SORT_BY_TIME_ZONE, mSortByTimeZone);
        super.onSaveInstanceState(outState);
    }

}
