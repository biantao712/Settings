/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings;

import com.android.settings.util.ResCustomizeConfig;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.LruCache;
import android.view.View;
import android.widget.ImageView;

/**
 * Rotate the device when using AlertDialog.Builder & AlertDialog:
 * Drawable.createFromPath & BitmapFactory.decodeFile will produce OOM and crash
 * Even using LruCache, memory still leaks =>
 * Process will be killed when low memory (without crashing)
 *
 * AlertDialog.Builder builder = new AlertDialog.Builder(this)
 *         .setTitle(R.string.regulatory_information)
 *         .setOnDismissListener(this);
 *
 * RegulatoryInfoDisplayActivity is modified to remove AlertDialog.Builder & AlertDialog
 * Code blocks about text resource are removed since
 * R.string.regulatory_info_text => empty string
 *
 * https://developer.android.com/training/displaying-bitmaps/cache-bitmap.html#memory-cache
 */


/**
 * {@link Activity} that displays regulatory information for the "Regulatory information"
 * preference item, and when "*#07#" is dialed on the Phone keypad. To enable this feature,
 * set the "config_show_regulatory_info" boolean to true in a device overlay resource, and in the
 * same overlay, either add a drawable named "regulatory_info.png" containing a graphical version
 * of the required regulatory info (If ro.bootloader.hardware.sku property is set use
 * "regulatory_info_<sku>.png where sku is ro.bootloader.hardware.sku property value in lowercase"),
 * or add a string resource named "regulatory_info_text" with an HTML version of the required
 * information (text will be centered in the dialog).
 */
public class RegulatoryInfoDisplayActivity extends Activity {
    private final String REGULATORY_INFO_RESOURCE = "regulatory_info";
    // 50MB should be enough, if the size of file is too large, simply use decodeFile
    private final int MAX_CACHE_SIZE = 1024 * 1024 * 50;
    private final int BITMAP_RESOURCE_ID = 0;        // only one file, we can use a fixed ID
    private final int OPTIONS_IN_SAMPLE_SIZE = 1;    // 1: do not resize
    private LruCache<String, Bitmap> mMemoryCache;
    private ImageView mImageView = null;
    private boolean mCacheEnabled = false;

    /**
     * Display the regulatory info graphic in a dialog window.
     * => Use normal ImageView instead
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // No regulatory info to display for this device
        if (!ResCustomizeConfig.isShowRegulatory()) finish();

        boolean regulatoryInfoDrawableExists = false;
        int cacheSizeNeeded = 0;
        mCacheEnabled = false;
        if (0 != getResourceId()) {
            try {
                // Remove Drawable.createFromPath
                // Use BitmapFactory.decodeFile with BitmapFactory.Options
                BitmapFactory.Options options = new BitmapFactory.Options();
                // Do not actually decode the file
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(ResCustomizeConfig.getRegulatoryPath(),
                        options);

                regulatoryInfoDrawableExists = (options.outWidth > 2
                        && options.outHeight > 2);
                // for bitmap, it will use ARGB8888 => 4 bytes
                cacheSizeNeeded = options.outWidth * options.outHeight * 4;
                mCacheEnabled = cacheSizeNeeded <= MAX_CACHE_SIZE;
            } catch (Resources.NotFoundException ignored) {
                regulatoryInfoDrawableExists = false;
            }
        }
        // BitmapFactory.decodeFile fails or image size too small
        if (!regulatoryInfoDrawableExists) finish();

        RetainFragment retainFragment =
                RetainFragment.findOrCreateRetainFragment(getFragmentManager());
        mMemoryCache = retainFragment.mRetainedCache;
        if (null == mMemoryCache && mCacheEnabled) {
            mMemoryCache = new LruCache<String, Bitmap>(cacheSizeNeeded) {
                @Override
                protected int sizeOf(String key, Bitmap bitmap) {
                    // The cache size will be measured in bytes rather than number of items
                    return bitmap.getByteCount();
                }
            };
            retainFragment.mRetainedCache = mMemoryCache;
        }

        View view = getLayoutInflater().inflate(R.layout.regulatory_info, null);
        mImageView = (ImageView) view.findViewById(R.id.regulatoryInfo);
        loadBitmap(BITMAP_RESOURCE_ID);
        setContentView(view);
    }

    private void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (null == getBitmapFromMemCache(key)) mMemoryCache.put(key, bitmap);
    }

    private Bitmap getBitmapFromMemCache(String key) {
        return mMemoryCache.get(key);
    }

    private void loadBitmap(final int resId) {
        final String imageKey = String.valueOf(resId);

        // Try to get bitmap from memory cache
        // This should imply that mMemoryCache != null
        if (mCacheEnabled) {
            final Bitmap bitmap = getBitmapFromMemCache(imageKey);
            if (null != bitmap) {
                mImageView.setImageBitmap(bitmap);
                return;
            }
        }

        // Load bitmap using BitmapFactory.decodeFile and save it to the memory cache
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = OPTIONS_IN_SAMPLE_SIZE;
        Bitmap bmImg = BitmapFactory.decodeFile(ResCustomizeConfig.getRegulatoryPath(), options);
        if (mCacheEnabled) addBitmapToMemoryCache(String.valueOf(resId), bmImg);
        mImageView.setImageBitmap(bmImg);
    }

    private int getResourceId() {
        // Use regulatory_info by default.
        int resId = getResources().getIdentifier(
                REGULATORY_INFO_RESOURCE, "drawable", getPackageName());

        // When hardware sku property exists, use regulatory_info_<sku> resource if valid.
        String sku = SystemProperties.get("ro.boot.hardware.sku", "");
        if (!TextUtils.isEmpty(sku)) {
            String regulatory_info_res = REGULATORY_INFO_RESOURCE + "_" + sku.toLowerCase();
            int id = getResources().getIdentifier(
                    regulatory_info_res, "drawable", getPackageName());
            if (id != 0) {
                resId = id;
            }
        }
        return resId;
    }
}

/**
 * https://developer.android.com/training/displaying-bitmaps/cache-bitmap.html#config-changes
 */
class RetainFragment extends Fragment {
    private static final String TAG = "RetainFragment";
    public LruCache<String, Bitmap> mRetainedCache;

    public RetainFragment() {}

    public static RetainFragment findOrCreateRetainFragment(FragmentManager fm) {
        RetainFragment fragment = (RetainFragment) fm.findFragmentByTag(TAG);
        if (fragment == null) {
            fragment = new RetainFragment();
            fm.beginTransaction().add(fragment, TAG).commit();
        }
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }
}
