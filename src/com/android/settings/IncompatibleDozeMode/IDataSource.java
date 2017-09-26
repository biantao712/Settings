package com.android.settings.IncompatibleDozeMode;

import android.content.pm.PackageInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by steve on 2016/3/9.
 */
public interface IDataSource {
    public void queryAppInfo(List<PackageInfo> apps, ArrayList<String> names);
    public void destroy();
}
