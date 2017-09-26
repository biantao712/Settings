package com.android.settings.flipfont;

import com.android.settings.R;

import android.util.AttributeSet;
import java.io.File;
import java.io.InputStream;
import java.util.Locale;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.preference.ListPreference;
import android.widget.ListView;
import android.widget.TextView;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.pm.ApplicationInfo;
import android.app.ActivityManager;
import android.util.Log;
import java.util.List;
import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.content.res.Configuration;
import android.os.RemoteException;
import android.app.ProgressDialog;
import android.os.AsyncTask;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import android.graphics.Typeface;

import java.security.MessageDigest;
import android.util.Base64;

import android.content.ContentResolver;
import android.net.Uri;

//GA {
import com.android.settings.analytic.AnalyticUtils.Category;
import com.android.settings.analytic.AnalyticUtils.Action;
import com.android.settings.analytic.TrackerManager;
import com.android.settings.analytic.TrackerManager.TrackerName;
//GA }

import java.lang.reflect.Field;

/*
 * FontListPreference contains a preference with a list that shows installed fonts.
 * Each font is represented with its own typeface.
 * @hide
 */
public class FontListPreference extends ListPreference {
    /*
     * Change the following booleens to get different UI behaviors
     */
    // If true use rebooting
    public static boolean REBOOT = false;

    // If true don't put up warning dialog
    public static boolean NO_WARNING_DIALOG = true;

    // If true no "Cancel" button in the main dialog
    public static boolean NO_CANCEL = true;

    public static boolean DEBUG = false;

    // Parent context
    private Context context = null;

    // Font preferences name
    public static final String KEY_PREFERENCE = "MONOTYPE";

    // Custom list rendering adapter
    private FontListAdapter mFontListAdapter = null;

    // Contains the previous font selection i.e. the font index what is was when the dialog was shown.
    private int mPreviousFont = -1;

    // Index of the clicked font item
    private int mClickedItem = -1;

    // Needed to separate cancel button press from back-button.
    private boolean mBuyButtonClicked = false;

    // Font sizes in alert dialogs
    private int mQuestionDialogFontSize = 20;

/* no more used
    // Padding used in alert dialogs
    private int mDialogLeftPadding = 20;
    private int mDialogTopPadding = 10;
    private int mDialogRightPadding = 20;
    private int mDialogBottomPadding = 10;
*/
    // used to save state
    public static final String PRIVATE_PREFERENCES = "prefs";
    private boolean mSelectDialogIsActive = false;
    private int  mSavedClickedItem = 0;

    // the signature key for monotype. This key will be same for all monotype apks
    private static final String SIGNATURE = "T84drf8v3ZMOLvt2SFG/K7ODXgI=";

    private AlertDialog mDialog;

    AlertDialog SetFontToAlert;
    private boolean mRebootDialogIsActive = false;
    private boolean mListDialogIsActive = false;

    // Is file copying
    private boolean mFileCopying = false;

    Bundle mState = null;

    private String getFontName_OEM(Context context, String fontName) {
        // Check the name to see if it is a OEM fonts.
        // If it is use the name in the resource
        if (fontName.equals(FontListAdapter.OEM_FONT1))
            fontName = (String) context.getResources().getText(R.string.monotype_dialog_font_oem_font1);
        else if (fontName.equals(FontListAdapter.OEM_FONT2))
            fontName = (String) context.getResources().getText(R.string.monotype_dialog_font_oem_font2);
        else if (fontName.equals(FontListAdapter.OEM_FONT3))
            fontName = (String) context.getResources().getText(R.string.monotype_dialog_font_oem_font3);

        return fontName;
    }

    /*
     * FontListPreference Constructor
     */
    public FontListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        String sName;
        String sPath;

        if(DEBUG)
            Log.v("FlipFont", "FontListPreference(Context context, AttributeSet attrs)");
        this.context = context;

        if(!isFlipfontSupport()) return;

        setKey(KEY_PREFERENCE);

        setTitle(R.string.monotype_preference_title);

        sPath = Typeface.getFontPathFlipFont(context, 1);

        if (sPath.endsWith("default"))
            setSummary(R.string.monotype_default_font);
        else
        {
            sName = Typeface.getFontNameFlipFont(context, 1);
            if (sName == null)
                setSummary(R.string.monotype_preference_summary);
            else
                setSummary(getFontName_OEM(context, sName));
        }

        /*the Flipfont Dialog matches the text in the settings list.*/
        //setDialogTitle(R.string.monotype_dialog_title);
        setDialogTitle(R.string.monotype_preference_title);

        if(NO_CANCEL)
        {
        }
        else
        {
            setNegativeButtonText(android.R.string.cancel);
        }
        setDialogIcon(R.drawable.flipfont_icon);
    }

    /*
     * FontListPreference Constructor
     */
    public FontListPreference(Context context) {
        this(context, null);
        if(DEBUG)
            Log.v("FlipFont", "FontListPreference(Context context)");
    }


    @Override
    protected void onClick() {
        Log.v("FlipFont", "onClick");
        //mState = state;
        new LoadListTask().execute();
    }


    protected void startFontList() {
        loadPreferences();

        // if first time flipfont is run, set to default
        String selectedFont = this.getPersistedString(KEY_PREFERENCE);
        if (selectedFont == KEY_PREFERENCE)
            selectedFont = "default";
        mPreviousFont = mFontListAdapter.mTypefaceFiles.indexOf(selectedFont);
        if (mPreviousFont < 0) {
            // if previous font not present select none
            mPreviousFont = -1;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this.context);
        builder.setTitle(R.string.monotype_preference_title);
        builder.setIcon(R.drawable.flipfont_icon);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setSingleChoiceItems(mFontListAdapter, mSavedClickedItem, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mClickedItem = which;
                    onOkButtonPressed();
                    dialog.dismiss();
                }
            });
        mDialog = builder.create();
        mDialog.show();
    }

    private class LoadListTask extends AsyncTask<Void, Void, Void> {
        private ProgressDialog mProgressDialog = null;
        private Bundle privState;

        @Override
        protected void onPreExecute() {
            //hold on to the state locally
            privState = mState;

            String sTitle = (String) context.getResources().getText(R.string.monotype_preference_title);
            String sMsg = (String) context.getResources().getText(R.string.monotype_loading_list);

            // show the "Loading font list..." dialog
            mProgressDialog = ProgressDialog.show(context, sTitle, sMsg, true, false);
        }

        @Override
        protected Void doInBackground(Void... params) {
            // load the font list - can take a few seconds
            mFontListAdapter = new FontListAdapter(context);
            mFontListAdapter.loadTypefaces();
            return null;
        }

       @Override
       protected void onPostExecute(Void unused) {
            // dismiss the "Loading font list..." dialog and show the menu list dialog
            try {
                mProgressDialog.dismiss();
                startFontList();
            }
            catch (Exception ex) {
                if(DEBUG)
                    Log.v("FlipFont", "dismiss/show ListLoad() - catch (Exception ex)");
            }
       }
    } //private class LoadListTask

    /*
     * (non-Javadoc)
     * @see android.preference.DialogPreference#showDialog(android.os.Bundle)
     */
    protected void showDialog(Bundle state) {
        if(DEBUG)
            Log.v("FlipFont", "showDialog  (AlertDialog.Builder builder)");

        if(mFontListAdapter == null) {
            mState = state;
            new LoadListTask().execute();
            return;
        }

        if (!mSelectDialogIsActive && !mRebootDialogIsActive) {
            setEntries((String[])mFontListAdapter.mFontNames.toArray(new String[mFontListAdapter.mFontNames.size()]));
            setEntryValues((String[])mFontListAdapter.mTypefaceFiles.toArray(new String[mFontListAdapter.mTypefaceFiles.size()]));
        }

        // do we have only the default font
        if (mFontListAdapter.getCount() < 2) {
            this.setDialogTitle(R.string.monotype_dialog_no_fonts_installed);
        }
        //super.showDialog(state);

        loadPreferences();
        if(mSelectDialogIsActive && (state != null)){
            mSelectDialogIsActive = false;
            savePreferences();
            selectDialog();
        }
        if(mRebootDialogIsActive && (state != null)){
            mRebootDialogIsActive = false;
            savePreferences();
            RebootDialog();
        }
    }

    /*
     * Reboot
     */
    public void restartNow() {
        // Reboot now
        Intent i = new Intent(Intent.ACTION_REBOOT);
        i.putExtra("nowait", 1);
        i.putExtra("interval", 1);
        i.putExtra("window", 0);

        // wait up  to 20 seconds for copying files to finish
        for(int index = 0; index<20; index++)
        {
            if (!mFileCopying)
                break;
            try {
                Thread.sleep(500);
            }
            catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        this.context.sendBroadcast(i);
    }

    /*
     * Closes the list dialog
     */
    public void restartLater() {
        dismissList();              // return to the Sound and Display dialog
        //System.exit(0);           // return to the Settings dialog.
    }

    /*
     * open sFileIn with ContentResolver and write it named sFileOut in directory sDir
     */
    private boolean copyFileWithCR(FontWriter fontWriter, File dir, String sPackageName, String sFileIn, String sFileOut)
    {
        // find the content resolver - return if none
        ContentResolver cr = null;
        boolean err_filecopy = false;
        try {
            cr = context.getContentResolver();
        } catch (Exception e) {
            return err_filecopy;
        }

        // set up the font URI to open - return if it can't be opened
        Uri uriFont = Uri.parse("content://" + sPackageName + "/fonts/" + sFileIn);
        InputStream isFont = null;
        try {
            isFont = cr.openInputStream(uriFont);
        } catch (Exception e) {
            /* this case is not file copy error*/
            return false;
        }

        // write the data out using the InputStream from the ContentResolver
        err_filecopy = fontWriter.copyFontFile(dir, isFont, sFileOut);

        try {
            isFont.close();
        } catch (Exception e1) {
        }

        return err_filecopy;
    }

    /*
     * User has selected to change the system font.
     */
    public boolean onOkButtonPressed() {

        boolean err_filecopy = false;
        // Get the selected typeface filename
        final String selectedFont;

        // Check if valid font
        String apkname = mFontListAdapter.mFontPackageNames.elementAt(mClickedItem).toString();
/*        if (checkFont(apkname)) {

            mClickedItem = mSavedClickedItem;
            Dialog d = this.getDialog();
            ListView v = (ListView) d.getCurrentFocus();
            v.setItemChecked(this.mPreviousFont, true);
            savePreferences();
            if(DEBUG)
                Log.v("FlipFont", "**onOkButtonPressed - bad font**");

            //Put up dialog for bad font
            errorDialog((String)context.getResources().getText(R.string.monotype_invalid_font));

            // exit back to the list
            return false;
        }
*/
        if (mClickedItem == -1)
            mClickedItem = mSavedClickedItem;

        selectedFont = (String) mFontListAdapter.mTypefaceFiles.elementAt(mClickedItem);
        //this.persistString(selectedFont);

        // Create a new fontfile writer
        final FontWriter fontWriter = new FontWriter();

        // if default font
        if ((selectedFont == null) || (selectedFont.equals(TypefaceFinder.DEFAULT_FONT_VALUE))) {
            this.persistString(selectedFont);
            fontWriter.deleteFontDirectory(context, " ");
            mSavedClickedItem = mClickedItem;
            savePreferences();
            // Set the previous font for going back
            mPreviousFont = mClickedItem;
            // Write default sans location
            fontWriter.writeLoc(context, TypefaceFinder.DEFAULT_FONT_VALUE, TypefaceFinder.DEFAULT_FONT_VALUE);
        }
        // anything else but default font
        else {
            // Get the selected typeface
            com.android.settings.flipfont.Typeface sansTypeface = mFontListAdapter.mTypefaceFinder.findMatchingTypeface(selectedFont);

            // Create a new directory for this font - stop at ".xml, i.e.: "tetra.xml" => "tetra"
            String sFontDir = selectedFont;
            int index = selectedFont.indexOf(".xml");
            if (index > 0)
                sFontDir = selectedFont.substring(0, index);

            File fontDir = fontWriter.createFontDirectory(context, sFontDir);

            // Loop through the files and copy ttf files to new directory.
            TypefaceFile tpf = null;

            if (sansTypeface != null) {
                for (int i=0; i<sansTypeface.mSansFonts.size(); i++) {
                    tpf = sansTypeface.mSansFonts.get(i);
                    try {
                        // get the package name for the font and get assetmanager
                        apkname = mFontListAdapter.mFontPackageNames.elementAt(mClickedItem).toString();

                        ApplicationInfo appInfo = mFontListAdapter.mPackageManager.getApplicationInfo(apkname, PackageManager.GET_META_DATA);
                        appInfo.publicSourceDir = appInfo.sourceDir;
                        Resources res = mFontListAdapter.mPackageManager.getResourcesForApplication(appInfo);

                        AssetManager assetManager = res.getAssets();

                        InputStream in = assetManager.open(FontListAdapter.FONT_DIRECTORY + tpf.getFileName());
                        err_filecopy = fontWriter.copyFontFile(fontDir, in, tpf.getDroidName());
                        in.close();
                    }
                    catch (Exception ex) {
                        // try using the ContentResolver
                        err_filecopy = copyFileWithCR(fontWriter, fontDir, apkname, tpf.getFileName(), tpf.getDroidName());
                    }
                }
            }

            if(err_filecopy==true)
            {
                mClickedItem = mSavedClickedItem;
//                Dialog d = this.getDialog();
//                ListView v = (ListView) d.getCurrentFocus();
//                v.setItemChecked(this.mPreviousFont, true);
                savePreferences();
                if(DEBUG)
                    Log.v("FlipFont", "**onOkButtonPressed - enospc error **");

                //Put up dialog for bad font
                errorDialog((String)context.getResources().getText(R.string.storage_low_title));

                // exit back to the list
                return false;
            }

            this.persistString(selectedFont);
            // deleate other font directories
            fontWriter.deleteFontDirectory(context, sFontDir);

            mSavedClickedItem = mClickedItem;
            savePreferences();
            // Set the previous font for going back
            mPreviousFont = mClickedItem;
            // Write file path to loc file
            fontWriter.writeLoc(context, fontDir.getAbsolutePath(), mFontListAdapter.mFontNames.elementAt(mClickedItem).toString());

        } // else

        IActivityManager am = ActivityManagerNative.getDefault();

        //GA {
        String gaFontNames = mFontListAdapter.mFontNames.elementAt(mClickedItem).toString().toLowerCase();
        String gaApkname = mFontListAdapter.mFontPackageNames.elementAt(mClickedItem).toString().toLowerCase();
        String gaEventlabel = (gaApkname.length() == 0) ? "DEFAULT FONT" :
                gaFontNames.concat("(").concat(gaApkname.concat(")"));
        if(DEBUG) Log.i("FlipFont", "[GA][Select]" + gaEventlabel);
        TrackerManager.sendEvents(context, TrackerName.TRACKER_FONTLOCALELANGUAGE, Category.FONT_FLIPFONT,
                Action.FONT_FLIPFONT_SELECT, gaEventlabel, TrackerManager.DEFAULT_VALUE);

        try {
            for (int i = 0; i < mFontListAdapter.mInstalledApplications.size(); i++) {
                gaApkname = mFontListAdapter.mInstalledApplications.get(i).packageName;
                if (gaApkname.startsWith(mFontListAdapter.FONT_PACKAGE)) {
                    try {
                        ApplicationInfo appInfo = mFontListAdapter.mPackageManager.getApplicationInfo(gaApkname, PackageManager.GET_META_DATA);
                        gaFontNames = appInfo.loadLabel(mFontListAdapter.mPackageManager).toString();
                        gaEventlabel = gaFontNames.concat("(").concat(gaApkname.concat(")"));
                        if (DEBUG) Log.i("FlipFont", "[GA][Install]" + gaEventlabel);
                        TrackerManager.sendEvents(context, TrackerName.TRACKER_FONTLOCALELANGUAGE, Category.FONT_FLIPFONT,
                                Action.FONT_FLIPFONT_INSTALL, gaEventlabel, TrackerManager.DEFAULT_VALUE);
                    } catch (Exception e) {
                    }
                }
            }
        } catch (Exception e) {
        }
        //GA }

        // If REBOOT false, restart apps
        if(!REBOOT) {
            // delay to make sure the font is done writing
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Change configuration for FWR
            try {
                Configuration config = am.getConfiguration();

                // value should be > 1
                config.FlipFont = Math.abs(selectedFont.hashCode()) + 1;

                am.updateConfiguration(config);
            }
            catch (RemoteException e) {
            }

            // Get activity manager
            ActivityManager activityManager = (ActivityManager)this.context.getSystemService(this.context.ACTIVITY_SERVICE);

            // Leave one of these next three lines uncommented, based on your desired functionality.
            activityManager.restartPackage("com.android.settings");   // return to the desktop
            //System.exit(0);                                         // return to the Settings dialog.
            //restartLater();                                         // return to the Sound and Display dialog
        }
        return true;
    }

    /*
     * User has selected to cancel, so we restore the previous selection.
     */
    public void onCancelButtonPressed() {
        if(DEBUG)
            Log.v("FlipFont", "onCancelButtonPressed() ");
//        Dialog d = this.getDialog();
//        ListView v = (ListView) d.getCurrentFocus();
//        v.setItemChecked(this.mPreviousFont, true);
        restartLater(); // The user is canceling the "set font as" (2nd) dialog. See the restartLater routine to find
                        // out to which dialog we return the user.
    }


    public void selectDialog() {
        // only allow one dialog up
        if(mSelectDialogIsActive)
            return;

        mSelectDialogIsActive = true;

        String question = null;
        String fontName = null;

        loadPreferences();
        mClickedItem = mSavedClickedItem;

        if (mClickedItem < 1) {
            String s = (String) context.getResources().getText(R.string.monotype_dialog_revert_question);
            question = s;
        }
        else {
            String s = (String) context.getResources().getText(R.string.monotype_dialog_set_font_question);
            fontName = mFontListAdapter.getFontName(mClickedItem);
            question = String.format(s, fontName);
        }

        if (NO_WARNING_DIALOG)
        {
            mSelectDialogIsActive = false;
            savePreferences();
            if (!onOkButtonPressed()) {
                return;
            }
            dismissApp();
        }

        {
            String setFontString;
            if(REBOOT) {
                setFontString = (String) context.getResources().getText(android.R.string.yes);
            }
            else {
                setFontString = (String) context.getResources().getText(R.string.monotype_restart_dialog_button_1);
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this.context);
            builder.setPositiveButton(setFontString, new DialogInterface.OnClickListener() {
                          public void onClick(DialogInterface dialog, int id) {
                                mSelectDialogIsActive = false;
                                savePreferences();
                                onOkButtonPressed();
                                dialog.cancel();
                                dismissApp();
                          }
                      })
                      .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                          public void onClick(DialogInterface dialog, int id) {
                              mSelectDialogIsActive = false;
                              savePreferences();
                              dialog.cancel();
                              dismissApp();
                          }
                      });
                      builder.setOnCancelListener(new DialogInterface.OnCancelListener(){
                          public void onCancel(DialogInterface dialog) {
                              mSelectDialogIsActive = false;
                              savePreferences();
                              dismissApp();
                          }
                      });
            hideList();

            builder.setTitle(question);
            builder.setIcon(0);

            savePreferences();
        }
    }

    protected void dismissApp() {
        if(DEBUG)
            Log.v("FlipFont", "dismissApp()");

        // reset flags
        mRebootDialogIsActive = false;
        mSelectDialogIsActive = false;
        savePreferences();
        dismissList();
    }


    /*
     * Convenience function for FlipFont application to be able to
     * open the dialog automatically when activity is started.
     */
    public void showFontListDialog() {
        this.onClick();
    }

    protected void savePreferences() {
        // Create or retrieve the shared preference object
        int mode = Activity.MODE_PRIVATE;
        SharedPreferences mySharedPreference = context.getSharedPreferences(PRIVATE_PREFERENCES, mode);

        // retrieve an editor to modify shared preferences
        SharedPreferences.Editor editor = mySharedPreference.edit();

        // Store new primitive types in the shared preferences object
        editor.putBoolean("SelectDialogIsActive", mSelectDialogIsActive);
        editor.putBoolean("RebootDialogIsActive", mRebootDialogIsActive);
        editor.putInt("SavedClickedItem", mSavedClickedItem);

        // Commit the changes
        editor.commit();
    }

    public void loadPreferences() {
        // Get the stored preferences
        int mode = Activity.MODE_PRIVATE;
        SharedPreferences mySharedPreference = context.getSharedPreferences(PRIVATE_PREFERENCES, mode);

        // Retrieve the saved values
        mSelectDialogIsActive = mySharedPreference.getBoolean("SelectDialogIsActive", false);
        mRebootDialogIsActive = mySharedPreference.getBoolean("RebootDialogIsActive", false);
        mSavedClickedItem = mySharedPreference.getInt("SavedClickedItem", 0);
    }

    protected void RebootDialog() {
        if(DEBUG)
            Log.v("FlipFont", "RebootDialog()");
        // only allow one dialog up
        if(mRebootDialogIsActive)
            return;

        // setup restart warning dialog
        // This should be removed when reboot is no longer required for flipping the font
        AlertDialog rebootDialog;
        mRebootDialogIsActive = true;
        savePreferences();

        hideList();

        AlertDialog.Builder builder = new AlertDialog.Builder(this.context);
        String s = (String) context.getResources().getText(R.string.monotype_restart_dialog_text_2);

        builder.setIcon(android.R.drawable.ic_dialog_alert)
             .setTitle(R.string.monotype_restart_dialog_text_1)
             .setMessage(s)
             .setPositiveButton(R.string.monotype_restart_dialog_button_1, new DialogInterface.OnClickListener() {
                      public void onClick(DialogInterface dialog, int id) {
                            onOkButtonPressed();
                            mRebootDialogIsActive = false;
                            savePreferences();
                            restartNow();
                            dismissList();
                      }
             })
             .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                       public void onClick(DialogInterface dialog, int id) {
                            mRebootDialogIsActive = false;
                            savePreferences();
                            dismissList();
                       }
              });
              builder.setOnCancelListener(new DialogInterface.OnCancelListener(){
                      public void onCancel(DialogInterface dialog) {
                          mRebootDialogIsActive = false;
                          savePreferences();
                          dismissList();
                      }
               });

        rebootDialog = builder.create();
        rebootDialog.show();
    }

    protected void dismissList() {
        if(DEBUG)
            Log.v("FlipFont", "dismissList()");
        try {
//            this.getDialog().dismiss();
        }
        catch (Exception ex) {
           if(DEBUG)
                Log.v("FlipFont", "dismissList() - catch (Exception ex)");
        }
    }

    protected void hideList() {
        try {
//            getDialog().hide();
        }
        catch (Exception ex) {
           if(DEBUG)
                Log.v("FlipFont", "hideList() - catch (Exception ex)");
        }
    }

    private static final String[] apkNameList = {
        ""
    };

    protected boolean checkFont(String apkname) {
        // check for a valid fontname
        if(DEBUG)
            Log.v("FlipFont", "checkFont - checking apkname: " + apkname);

        // Check if apk package name is in the list. These are built in and have debug certificates
        for (int i = 0; i < apkNameList.length; i++) {
            if (apkname.equals(apkNameList[i])) {
                if(DEBUG)
                    Log.v("FlipFont", "**Apk name matches list**");
                return false;
            }
        }

        // check for valid certificate
        if(DEBUG)
            Log.v("FlipFont", "checkFont - check if valid certificate");
        PackageInfo packageInfo = null;
        try {
            packageInfo = mFontListAdapter.mPackageManager.getPackageInfo(apkname, PackageManager.GET_SIGNATURES|PackageManager.GET_PERMISSIONS);
        }
        catch (Exception ex) {

        }

        if(packageInfo.requestedPermissions != null)
        {
             if(DEBUG)
                Log.v("FlipFont", "**Suspicious Permission detected**");
            return true;
        }

        android.content.pm.Signature[] signatures = packageInfo.signatures;

        // cert = DER encoded X.509 certificate:
        byte[] cert = signatures[0].toByteArray();
        try
        {
            MessageDigest md = MessageDigest.getInstance("SHA");
            md.update(signatures[0].toByteArray());
            String currentSignature = Base64.encodeToString(md.digest(), Base64.DEFAULT);
            currentSignature = currentSignature.trim();

            //compare signatures
            if (SIGNATURE.equals(currentSignature))
            {
                if(DEBUG)
                    Log.v("FlipFont", "**Signature is correct**");
                return false;
            }
            else
            {
                if(DEBUG)
                    Log.v("FlipFont", "**Signature is incorrect**");
                return true;
            }
        } catch (Exception e)
        {

        }
        InputStream input = new ByteArrayInputStream(cert);

        CertificateFactory cf = null;
        try {
            cf = CertificateFactory.getInstance("X509");
        } catch (CertificateException e) {
            // TODO some error checking
            e.printStackTrace();
        }
        X509Certificate c = null;
        try {
            c = (X509Certificate) cf.generateCertificate(input);
        } catch (CertificateException e) {
            // TODO some error checking
            e.printStackTrace();
        }
        if(DEBUG) {
            Log.v("Example", "APK name: " + apkname);
            Log.v("Example", "Certificate for: " + c.getSubjectDN());
            Log.v("Example", "Certificate issued by: " + c.getIssuerDN());
            Log.v("Example", "The certificate is valid from " + c.getNotBefore() + " to " + c.getNotAfter());
            Log.v("Example", "Certificate SN# " + c.getSerialNumber());
            Log.v("Example", "Generated with " + c.getSigAlgName());
        }

        // Check if certificate data is correct
        String certIssuedByString = "CN=Ed Platz, OU=Display Imaging, O=Monotype Imanging Inc., L=Woburn, ST=MA, C=US";
        String issuerDNString = c.getIssuerDN().toString();

        if(certIssuedByString.equals(issuerDNString)) {
            if(DEBUG)
                Log.v("FlipFont", "**Certificate data is correct**");
            return false;
        }
        return true;
    }

    protected void errorDialog(String s) {
        AlertDialog.Builder builder  = new AlertDialog.Builder(
                        this.context);
        if(s==null)
        return;

        // Setting Dialog Title
        builder.setTitle("");

        // Setting Dialog Message
        builder.setMessage(s);

        // Setting Icon to Dialog
        builder.setIcon(android.R.drawable.ic_dialog_alert);

        // Setting OK Button
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                }
        });

        // Showing Alert Message
        builder.show();
    }

    public boolean isFlipfontSupport() {
        boolean DEBUG = true;
        String TAG = "FontListPreference";
        try {
            Class<?> c = Class.forName("android.graphics.Typeface");
            Field m = c.getDeclaredField("isFlipFontUsed");
            if (DEBUG) {
                Log.v(TAG, "========ReflectionMethods.isFlipfontSupport = true ===============");
            }
            return true;
        } catch (ClassNotFoundException e) {
            if (DEBUG) {
                Log.v(TAG, "========ReflectionMethods.isFlipfontSupport = false ===============");
                Log.v(TAG, "ClassNotFoundException e=" + e);
            }
            return false;
        } catch (NoSuchFieldException e) {
            if (DEBUG) {
                Log.v(TAG, "========ReflectionMethods.isFlipfontSupport = false ===============");
                Log.v(TAG, "NoSuchFieldException e=" + e);
            }
            return false;
        }
    }
}
