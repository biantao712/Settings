package com.android.settings.IncompatibleDozeMode;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.android.settings.IncompatibleDozeMode.IDataSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by steve on 2016/3/9.
 */
public class AppDataSource implements IDataSource{

    private PackageManager mPackageManager;

    public AppDataSource(PackageManager pm) {
        mPackageManager = pm;
    }

    @Override
    public void queryAppInfo(List<PackageInfo> apps, ArrayList<String> names) {
        apps.clear();

        for(String name: names) {
            try {
                PackageInfo pi = mPackageManager.getPackageInfo(name,0);
                apps.add(pi);
            } catch(PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void destroy() {
        mPackageManager = null;
    }
}
