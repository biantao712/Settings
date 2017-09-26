package com.android.settings;

import android.content.ComponentName;

interface IDeviceAdmin {
    boolean setAdmin(in ComponentName component);
}
