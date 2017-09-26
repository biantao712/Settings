package com.android.settings.deviceinfo.utils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.IPackageDataObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.os.storage.VolumeRecord;
import android.provider.DocumentsContract;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceGroup;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.applications.ManageApplications;
import com.android.settings.deviceinfo.PrivateVolumeFormat;
import com.android.settings.deviceinfo.PrivateVolumeUnmount;
import com.android.settings.deviceinfo.StorageItemPreference;
import com.android.settings.deviceinfo.StorageSettings;
import com.android.settings.deviceinfo.StorageSummaryPreference;
import com.android.settings.deviceinfo.StorageVolumePreference;
import com.android.settings.deviceinfo.StorageWizardMigrateConfirm;
import com.android.settingslib.deviceinfo.StorageMeasurement;
import com.google.android.collect.Lists;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by robert on 2016/10/31.
 */
public class FlattenStorageLayout implements StorageMeasureCompleted.OnMeasureCompletedListener, StorageVolumePreference.OnManageStorageClickListener{

    static final String TAG = "FlattenStorageSettings";

    private boolean mIsFlatten = false;
    private Context mContext;
    private StorageSettings mFrag;
    private PreferenceCategory mInternalCategory;
    private PreferenceCategory mExternalCategory;
    private UserManager mUserManager;
    private UserInfo mCurrentUser;
    private int mItemPoolIndex;
    private HashMap<String, List<StorageItemPreference>> mItemPreferencePool = new HashMap<String, List<StorageItemPreference>>();
    private HashMap<String, Boolean> isExpanded = new HashMap<String, Boolean>();
    private List<StorageVolumePreference> mInternalVolumePool = Lists.newArrayList();
    private HashMap<String, StorageMeasureCompleted> listeners = new HashMap<String, StorageMeasureCompleted>();

    private static final String AUTHORITY_MEDIA = "com.android.providers.media.documents";
    private static final String USER_CATEGORY = "USER_CATEGORY";
    private static final int[] ITEMS_NO_SHOW_SHARED = new int[] {
            R.string.storage_detail_apps,
    };
    private final int[] ITEMS_SHOW_SHARED = new int[] {
            R.string.storage_detail_apps,
            R.string.storage_detail_images,
            R.string.storage_detail_videos,
            R.string.storage_detail_audio,
            R.string.storage_detail_other
    };
    private Map<String, Integer> mStorageItemsTitleResMap = new HashMap<>();

    public FlattenStorageLayout(StorageSettings fragment, PreferenceCategory internalCategory, PreferenceCategory externalCategory) {
        mContext = fragment.getActivity();
        mIsFlatten = isFlatten();
        mFrag = fragment;
        mInternalCategory = internalCategory;
        mExternalCategory = externalCategory;
        mUserManager = mContext.getSystemService(UserManager.class);
        mCurrentUser = mUserManager.getUserInfo(UserHandle.myUserId());
        //Maping table to title resource mapping
        for (int res:ITEMS_SHOW_SHARED) {
            mStorageItemsTitleResMap.put(mContext.getString(res), res);
        }
        mStorageItemsTitleResMap.put(mContext.getString(R.string.storage_detail_cached), R.string.storage_detail_cached);
        //mStorageItemsTitleResMap.put(mContext.getString(R.string.storage_menu_explore), R.string.storage_menu_explore);
        Log.i(TAG, "Flatten Storage Settings Layout: "+(mIsFlatten?"enabled!":"disabled!"));
        //Verizon Request: Portable Storage -> MicroSD
        if (mIsFlatten) {
            mExternalCategory.setTitle(R.string.flatten_storage_external_title);
        }
    }

    ///refresh()
    public boolean clearInternalVolumePoolIfNeed() {
        if (mIsFlatten) {
            mInternalVolumePool.clear();
        }
        return mIsFlatten;
    }

    public boolean updateInternalVolumePoolIfNeed(StorageVolumePreference systemVolume) {
        if (mIsFlatten) {
            mInternalVolumePool.add(systemVolume);
        }
        return mIsFlatten;
    }

    public void setManageStorageListener(StorageVolumePreference storageVolumePreference) {
        storageVolumePreference.setManageStorageListener(this);
    }

    //VolumeInfo sharedVolume = mStorageManager.findEmulatedForPrivate(vol);
    public boolean showInternalStorageDetailIfNeed(VolumeInfo vol, VolumeInfo sharedVolume) {
        if (!mIsFlatten) {
            return false;
        }
        //+++ [Verizon] show volume's detail information
        isExpanded.put(vol.getId(), true);
        mItemPoolIndex = 0;
        mItemPreferencePool.put(vol.getId(), Lists.<StorageItemPreference>newArrayList());
        StorageMeasurement mMeasure = new StorageMeasurement(mContext, vol, sharedVolume);
        if(listeners.get(vol.getId())!=null){
            mMeasure.setReceiver(listeners.get(vol.getId()));
        } else {
            StorageMeasureCompleted receiver = new StorageMeasureCompleted(vol.getId());
            receiver.setOnMeasureCompletedListener(this);
            listeners.put(vol.getId(), receiver);
            mMeasure.setReceiver(receiver);
        }
        List<UserInfo> allUsers = mUserManager.getUsers();
        final int userCount = allUsers.size();
        final boolean showHeaders = userCount > 1;
        final boolean showShared = (sharedVolume != null) && sharedVolume.isMountedReadable();

        int addedUserCount = 0;
        // Add current user and its profiles first
        for (int userIndex = 0; userIndex < userCount; ++userIndex) {
            final UserInfo userInfo = allUsers.get(userIndex);
            if (isProfileOf(mCurrentUser, userInfo)) {
                if(showHeaders){
                    StorageItemPreference header = new StorageItemPreference(mContext);
                    header.setTitle(userInfo.name);
                    header.setKey(USER_CATEGORY);
                    mItemPreferencePool.get(vol.getId()).add(header);
                    addPreference(mInternalCategory, header);
                    ++mItemPoolIndex;
                }
                addDetailItems(mInternalCategory, showShared, userInfo.id, vol);
                ++addedUserCount;
            }
        }

        // Add rest of users
        if (userCount - addedUserCount > 0) {
            StorageItemPreference header = new StorageItemPreference(mContext);
            header.setTitle(mContext.getText(R.string.storage_other_users));
            header.setKey(USER_CATEGORY);
            addPreference(mInternalCategory, header);
            mItemPreferencePool.get(vol.getId()).add(header);
            ++mItemPoolIndex;
            for (int userIndex = 0; userIndex < userCount; ++userIndex) {
                final UserInfo userInfo = allUsers.get(userIndex);
                if (!isProfileOf(mCurrentUser, userInfo)) {
                    addItem(mInternalCategory, /* titleRes */ 0, userInfo.name, userInfo.id, vol);
                }
            }
        }
        addItem(mInternalCategory, R.string.storage_detail_cached, null, UserHandle.USER_NULL, vol);
        mMeasure.forceMeasure();

        /* ASUS: remove 'explore' entry
        if (showShared) {
            StorageItemPreference explore = new StorageItemPreference(mContext);
            explore.setLayoutResource(R.layout.storage_item);
            explore.setTitle(R.string.storage_menu_explore);
            explore.volumeID = vol.getId();
            mItemPreferencePool.get(vol.getId()).add(explore);
            addPreference(mInternalCategory, explore);
        }
        */
        //--- [Verizon] show volume's detail information
        return true;
    }

    //onPreferenceTreeClick()
    public boolean onStorageItemPreferenceTreeClick(SettingsPreferenceFragment parent, StorageManager sm, Preference pref) {
        if (!mIsFlatten) return false;
        final String key = pref.getKey();
        if (key != null && key.equals(USER_CATEGORY)) {
            return true;
        }
        final int userId = ((StorageItemPreference)pref).userHandle;
        final String title = (pref.getTitle()).toString();
        final int titleId = mStorageItemsTitleResMap.containsKey(title)?
                mStorageItemsTitleResMap.get(title) : 0;
        Intent intent = null;
        switch(titleId) {
            case R.string.storage_detail_apps: {
                Bundle args = new Bundle();
                args.putString(ManageApplications.EXTRA_CLASSNAME,
                        Settings.StorageUseActivity.class.getName());
                args.putString(ManageApplications.EXTRA_VOLUME_UUID, ((StorageItemPreference) pref).fsUuid);
                args.putString(ManageApplications.EXTRA_VOLUME_NAME, ((StorageItemPreference) pref).description);
                intent = Utils.onBuildStartFragmentIntent(mContext,
                        ManageApplications.class.getName(), args, null, R.string.apps_storage, null,
                        false);
                break;
            }
            case R.string.storage_detail_images: {
                intent = new Intent(DocumentsContract.ACTION_BROWSE);
                intent.setData(DocumentsContract.buildRootUri(AUTHORITY_MEDIA, "images_root"));
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                break;
            }
            case R.string.storage_detail_videos: {
                intent = new Intent(DocumentsContract.ACTION_BROWSE);
                intent.setData(DocumentsContract.buildRootUri(AUTHORITY_MEDIA, "videos_root"));
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                break;
            }
            case R.string.storage_detail_audio: {
                intent = new Intent(DocumentsContract.ACTION_BROWSE);
                intent.setData(DocumentsContract.buildRootUri(AUTHORITY_MEDIA, "audio_root"));
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                break;
            }
            case R.string.storage_detail_other: {
                final VolumeInfo mVolume = sm.findVolumeById(((StorageItemPreference) pref).volumeID);
                final VolumeInfo mSharedVolume = sm.findEmulatedForPrivate(mVolume);
                OtherInfoFragment.show(parent, sm.getBestVolumeDescription(mVolume),
                        mSharedVolume);
                break;
            }
            case R.string.storage_detail_cached: {
                ConfirmClearCacheFragment.show(parent);
                break;
            }
            case R.string.storage_menu_explore: {
                final VolumeInfo mVolume = sm.findVolumeById(((StorageItemPreference) pref).volumeID);
                final VolumeInfo mSharedVolume = sm.findEmulatedForPrivate(mVolume);
                intent = mSharedVolume.buildBrowseIntent();
                break;
            }
            case 0: {
                UserInfoFragment.show(parent, pref.getTitle(), pref.getSummary());
                break;
            }
        }
        if (intent != null) {
            try {
                if (userId == -1) {
                    parent.startActivity(intent);
                } else {
                    parent.getActivity().startActivityAsUser(intent, new UserHandle(userId));
                }
            } catch (ActivityNotFoundException e) {
                Log.w(TAG, "No activity found for " + intent);
            }
        }
        return true;
    }

    public boolean onInternalStorageVolumePreferenceTreeClick(StorageSummaryPreference summary, VolumeInfo vol) {
        if (!mIsFlatten) {
            return false;
        }
        //+++ [Verizon] show volume's detail information
        mInternalCategory.removeAll();
        mInternalCategory.addPreference(summary);
        for (StorageVolumePreference internalVolume : mInternalVolumePool) {
            addPreference(mInternalCategory, internalVolume);
            if (internalVolume.getKey().equals(StorageSettings.ID_SYSTEM_RESERVED)) {
                continue;
            }
            boolean expanded = isExpanded.get(internalVolume.getKey());
            if(vol.getId().equals(internalVolume.getKey())) {
                isExpanded.put(internalVolume.getKey(), !expanded);
                expanded = !expanded;
            }
            if (expanded) {
                for (StorageItemPreference item : mItemPreferencePool.get(internalVolume.getKey())) {
                    addPreference(mInternalCategory, item);
                }
            }
        }
        //--- [Verizon] show volume's detail information
        return true;
    }

    //Factory Methods
    public static boolean isFlatten() {
        final String SKU = SystemProperties.get("ro.build.asus.sku", "WW").toUpperCase();
        return "VZW".equals(SKU);
    }

    //Implementation of StorageMeasureCompleted.OnMeasureCompletedListener
    @Override
    public void onMeasureCompleted(StorageMeasurement.MeasurementDetails details, String volumeID) {
        Log.i(TAG, "onMeasureCompleted:" + volumeID);
        updateDetails(details, volumeID);
    }

    private void updateDetails(StorageMeasurement.MeasurementDetails details, String volumeID) {
        final int length = mItemPreferencePool.get(volumeID).size();
        for (int i = 0; i < length; ++i) {
            StorageItemPreference item = mItemPreferencePool.get(volumeID).get(i);
            final String key = item.getKey();
            if(key!=null && key.equals(USER_CATEGORY)){
                //header don't need update.
                continue;
            }
            final int userId = item.userHandle;
            final String title = item.getTitle().toString();
            final int titleId = mStorageItemsTitleResMap.containsKey(title)?
                    mStorageItemsTitleResMap.get(title):0;
            switch (titleId) {
                case R.string.storage_detail_apps:
                    updatePreference(item, details.appsSize.get(userId));
                    break;
                case R.string.storage_detail_images:
                    final long imagesSize = totalValues(details, userId,
                            Environment.DIRECTORY_DCIM, Environment.DIRECTORY_MOVIES,
                            Environment.DIRECTORY_PICTURES);
                    updatePreference(item, imagesSize);
                    break;
                case R.string.storage_detail_videos:
                    final long videosSize = totalValues(details, userId,
                            Environment.DIRECTORY_MOVIES);
                    updatePreference(item, videosSize);
                    break;
                case R.string.storage_detail_audio:
                    final long audioSize = totalValues(details, userId,
                            Environment.DIRECTORY_MUSIC,
                            Environment.DIRECTORY_ALARMS, Environment.DIRECTORY_NOTIFICATIONS,
                            Environment.DIRECTORY_RINGTONES, Environment.DIRECTORY_PODCASTS);
                    updatePreference(item, audioSize);
                    break;
                case R.string.storage_detail_other:
                    updatePreference(item, details.miscSize.get(userId));
                    break;
                case R.string.storage_detail_cached:
                    updatePreference(item, details.cacheSize);
                    break;
                case 0:
                    final long userSize = details.usersSize.get(userId);
                    updatePreference(item, userSize);
                    break;
            }
        }
    }

    private void updatePreference(StorageItemPreference pref, long size) {
        pref.setSummary(Formatter.formatFileSize(mContext, size));
    }

    private boolean isProfileOf(UserInfo user, UserInfo profile) {
        return user.id == profile.id ||
                (user.profileGroupId != UserInfo.NO_PROFILE_GROUP_ID
                        && user.profileGroupId == profile.profileGroupId);
    }

    private static long totalValues(StorageMeasurement.MeasurementDetails details, int userId, String... keys) {
        long total = 0;
        HashMap<String, Long> map = details.mediaSize.get(userId);
        if (map != null) {
            for (String key : keys) {
                if (map.containsKey(key)) {
                    total += map.get(key);
                }
            }
        } else {
            Log.w(TAG, "MeasurementDetails mediaSize array does not have key for user " + userId);
        }
        return total;
    }

    private void addDetailItems(PreferenceGroup category, boolean showShared, int userId, VolumeInfo volume) {
        final int[] itemsToAdd = (showShared ? ITEMS_SHOW_SHARED : ITEMS_NO_SHOW_SHARED);
        for (int i = 0; i < itemsToAdd.length; ++i) {
            addItem(category, itemsToAdd[i], null, userId, volume);
        }
    }

    private void addPreference(PreferenceGroup group, Preference pref) {
        pref.setOrder(Preference.DEFAULT_ORDER);
        group.addPreference(pref);
    }

    private void addItem(PreferenceGroup group, int titleRes, CharSequence title, int userId, VolumeInfo volume) {
        final String volumeID = volume.getId();
        StorageItemPreference item;
        if (mItemPoolIndex < mItemPreferencePool.get(volumeID).size()) {
            item = mItemPreferencePool.get(volumeID).get(mItemPoolIndex);
        } else {
            item = buildItem();
            mItemPreferencePool.get(volumeID).add(item);
        }
        item.setLayoutResource(R.layout.storage_item);
        if (title != null) {
            item.setTitle(title);
        } else {
            item.setTitle(titleRes);
        }
        item.setSummary(R.string.memory_calculating_size);
        item.userHandle = userId;
        item.fsUuid = volume.getFsUuid();
        item.description = volume.getDescription();
        item.volumeID = volume.getId();
        addPreference(group, item);
        ++mItemPoolIndex;
    }

    private StorageItemPreference buildItem() {
        final StorageItemPreference item = new StorageItemPreference(mContext);
        return item;
    }

    public static class OtherInfoFragment extends DialogFragment {
        private static final String TAG_OTHER_INFO = "otherInfo";

        public static void show(Fragment parent, String title, VolumeInfo sharedVol) {
            if (!parent.isAdded()) return;

            final OtherInfoFragment dialog = new OtherInfoFragment();
            dialog.setTargetFragment(parent, 0);
            final Bundle args = new Bundle();
            args.putString(Intent.EXTRA_TITLE, title);
            args.putParcelable(Intent.EXTRA_INTENT, sharedVol.buildBrowseIntent());
            dialog.setArguments(args);
            dialog.show(parent.getFragmentManager(), TAG_OTHER_INFO);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = this.getActivity();

            final String title = getArguments().getString(Intent.EXTRA_TITLE);
            final Intent intent = getArguments().getParcelable(Intent.EXTRA_INTENT);

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(
                    TextUtils.expandTemplate(getText(R.string.storage_detail_dialog_other), title));

            builder.setPositiveButton(R.string.storage_menu_explore,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivity(intent);
                        }
                    });
            builder.setNegativeButton(android.R.string.cancel, null);

            return builder.create();
        }
    }

    /**
     * Dialog to request user confirmation before clearing all cache data.
     */
    public static class ConfirmClearCacheFragment extends DialogFragment {
        private static final String TAG_CONFIRM_CLEAR_CACHE = "confirmClearCache";

        public static void show(Fragment parent) {
            if (!parent.isAdded()) return;

            final ConfirmClearCacheFragment dialog = new ConfirmClearCacheFragment();
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), TAG_CONFIRM_CLEAR_CACHE);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = this.getActivity();

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.memory_clear_cache_title);
            builder.setMessage(getString(R.string.memory_clear_cache_message));

            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final StorageSettings target = (StorageSettings) getTargetFragment();
                    final PackageManager pm = context.getPackageManager();
                    final List<PackageInfo> infos = pm.getInstalledPackages(0);
                    final ClearCacheObserver observer = new ClearCacheObserver(
                            target, infos.size());
                    for (PackageInfo info : infos) {
                        pm.deleteApplicationCacheFiles(info.packageName, observer);
                    }
                }
            });
            builder.setNegativeButton(android.R.string.cancel, null);

            return builder.create();
        }
    }

    private static class ClearCacheObserver extends IPackageDataObserver.Stub {
        private final StorageSettings mTarget;
        private int mRemaining;

        public ClearCacheObserver(StorageSettings target, int remaining) {
            mTarget = target;
            mRemaining = remaining;
        }

        @Override
        public void onRemoveCompleted(final String packageName, final boolean succeeded) {
            synchronized (this) {
                if (--mRemaining == 0) {
                    mTarget.getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mTarget.refresh();
                        }
                    });
                }
            }
        }
    }

    public static class UserInfoFragment extends DialogFragment {
        private static final String TAG_USER_INFO = "userInfo";
        public static void show(Fragment parent, CharSequence userLabel, CharSequence userSize) {
            if (!parent.isAdded()) return;

            final UserInfoFragment dialog = new UserInfoFragment();
            dialog.setTargetFragment(parent, 0);
            final Bundle args = new Bundle();
            args.putCharSequence(Intent.EXTRA_TITLE, userLabel);
            args.putCharSequence(Intent.EXTRA_SUBJECT, userSize);
            dialog.setArguments(args);
            dialog.show(parent.getFragmentManager(), TAG_USER_INFO);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = this.getActivity();

            final CharSequence userLabel = getArguments().getCharSequence(Intent.EXTRA_TITLE);
            final CharSequence userSize = getArguments().getCharSequence(Intent.EXTRA_SUBJECT);

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(TextUtils.expandTemplate(
                    getText(R.string.storage_detail_dialog_user), userLabel, userSize));

            builder.setPositiveButton(android.R.string.ok, null);

            return builder.create();
        }
    }

    //+++ Asus Flatten Storage Settins Layout
    //StorageVolumePreference.OnManageStorageClickListener
    @Override
    public void onManageStorageClick(VolumeInfo volume, MenuItem item) {
        final Context context = mContext;
        final Bundle args = new Bundle();
        switch (item.getItemId()) {
            case R.id.storage_rename:
                RenameFragment.show((StorageSettings) mFrag, volume);
                break;
            case R.id.storage_mount:
                new StorageSettings.MountTask(context, volume).execute();
                break;
            case R.id.storage_unmount:
                args.putString(VolumeInfo.EXTRA_VOLUME_ID, volume.getId());
                mFrag.startFragment(mFrag, PrivateVolumeUnmount.class.getCanonicalName(),
                        R.string.storage_menu_unmount, 0, args);
                break;
            case R.id.storage_format:
                args.putString(VolumeInfo.EXTRA_VOLUME_ID, volume.getId());
                mFrag.startFragment(mFrag, PrivateVolumeFormat.class.getCanonicalName(),
                        R.string.storage_menu_format, 0, args);
                break;
            case R.id.storage_migrate:
                final Intent intent = new Intent(context, StorageWizardMigrateConfirm.class);
                intent.putExtra(VolumeInfo.EXTRA_VOLUME_ID, volume.getId());
                mFrag.startActivity(intent);
                break;
        }
    }

    public static class RenameFragment extends DialogFragment {
        private static final String TAG_RENAME = "rename";

        public static void show(StorageSettings parent, VolumeInfo vol) {
            if (!parent.isAdded()) return;

            final RenameFragment dialog = new RenameFragment();
            dialog.setTargetFragment(parent, 0);
            final Bundle args = new Bundle();
            args.putString(VolumeRecord.EXTRA_FS_UUID, vol.getFsUuid());
            dialog.setArguments(args);
            dialog.show(parent.getFragmentManager(), TAG_RENAME);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();
            final StorageManager storageManager = context.getSystemService(StorageManager.class);

            final String fsUuid = getArguments().getString(VolumeRecord.EXTRA_FS_UUID);
            final VolumeInfo vol = storageManager.findVolumeByUuid(fsUuid);
            final VolumeRecord rec = storageManager.findRecordByUuid(fsUuid);

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            final LayoutInflater dialogInflater = LayoutInflater.from(builder.getContext());

            final View view = dialogInflater.inflate(R.layout.dialog_edittext, null, false);
            final EditText nickname = (EditText) view.findViewById(R.id.edittext);
            nickname.setText(rec.getNickname());

            builder.setTitle(R.string.storage_rename_title);
            builder.setView(view);

            builder.setPositiveButton(R.string.save,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // TODO: move to background thread
                            storageManager.setVolumeNickname(fsUuid,
                                    nickname.getText().toString());
                        }
                    });
            builder.setNegativeButton(R.string.cancel, null);

            return builder.create();
        }
    }
    //---
}

