package com.asus.suw.lockscreen;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Created by yueting-wong on 2016/9/7.
 */
public class AsusSuwUtilisClient {

    private static final String TAG ="AsusSuwUtilisClient";
    public static String TARGET_PACKAGE_NAME = "com.asus.setupwizard";
    public static String TARGET_CLASS_NAME = "com.asus.setupwizard.settings.AsusSetupWizardLayoutUtils";
    private Context mContext;


    private Class mTargetClass = null;
    private Object mTargetObject = null;

    private Button mNextBtn;
    private Button mBackBtn;
    private ViewGroup mSuwLayout;
    private ViewGroup mButtonBar;

    public AsusSuwUtilisClient(Context context){
        mContext = context;
    }

    private Context getTargetContext() {
        return SuwClassLoader.getInstance(mContext).getTargetContext();
    }

    private Class getTargetClass() {
        return SuwClassLoader.getInstance(mContext).getTargetClass();
    }

    private Object getTargetObject() {
        if (mTargetObject == null) {
            try {
                Context context = getTargetContext();
                Constructor constructor = SuwClassLoader.getInstance(mContext).getTargetConstructor();
                mTargetObject = constructor.newInstance(context);
            } catch(Exception e) {
                e.printStackTrace();
                Log.d(TAG, "getTargetObject fail");
            }
        }
        return mTargetObject;
    }



    public int getResIdFromSetupWizard(String name, String type){
        Context context = getTargetContext();
        Resources targetRes = context.getResources();
        return targetRes.getIdentifier(name, type, TARGET_PACKAGE_NAME);
    }

    public ViewGroup getSetupWizardLayout_Short(){
        if(mSuwLayout == null) {
            try {
                Class clazz = getTargetClass();
                Object obj = getTargetObject();
                if(clazz == null) return null;

                Method method = clazz.getMethod("findShortTemplateLayout");
                ViewGroup suwLayout = (ViewGroup) method.invoke(obj);
                initLayout(suwLayout);
                mSuwLayout = addRootView(suwLayout);
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(TAG, "getSetupWizardLayout_Short fail");
            }
        }
        return mSuwLayout;
    }

    public ViewGroup getSetupWizardLayout(){
        if(mSuwLayout == null) {
            try {
                Class clazz = getTargetClass();
                Object obj = getTargetObject();
                if(clazz == null) return null;

                Method method = clazz.getMethod("findTemplateLayout");
                ViewGroup suwLayout = (ViewGroup) method.invoke(obj);
                initLayout(suwLayout);

                mSuwLayout = addRootView(suwLayout);
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(TAG, "getSetupWizardLayout fail");
            }
        }
        return mSuwLayout;
    }

    private ViewGroup addRootView(ViewGroup view){
        SuwRootView root = new SuwRootView(mContext);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        root.addView(view, params);
        return root;
    }

    public ViewGroup getSetupWizardLayout_Short(int resid){
        if(mSuwLayout == null) {
            try {
                Context context = getTargetContext();
                Class clazz = getTargetClass();
                Object obj = getTargetObject();
                if(clazz == null) return null;

                Method method = clazz.getMethod("findTempletLayout", new Class[]{int.class});
                ViewGroup suwLayout = (ViewGroup) method.invoke(obj, resid);
                mSuwLayout = addRootView(suwLayout);
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(TAG, "getSetupWizardLayout_Short fail");
            }
        }
        return mSuwLayout;
    }

    private void initLayout(ViewGroup viewGroup){
        try {
            ViewGroup linearLayout = (LinearLayout) viewGroup.findViewById(getResIdFromSetupWizard("suw_layout_decor_inner", "id"));
            if (linearLayout != null) {
                linearLayout.setPadding(0, 0, 0, 0);
            }
        }catch(Exception ex) {
            ex.printStackTrace();
            Log.d(TAG, "initLayout fail");
        }
    }

    public View getNavigationBar() {
        if(mButtonBar == null) {
            try {
                Class clazz = getTargetClass();
                Object obj = getTargetObject();

                Method method = clazz.getMethod("getNavigationBar");
                mButtonBar = (ViewGroup) method.invoke(obj);
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(TAG, "getNavigationBar fail");
            }
        }
        return mButtonBar;
    }

    public Button getNextButton(){
        if(mNextBtn == null) {
            try {
                Class clazz = getTargetClass();
                Object obj = getTargetObject();

                Method method = clazz.getMethod("getNextButton");
                mNextBtn = (Button) method.invoke(obj);
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(TAG, "getNextButton fail");
            }
        }
        return mNextBtn;
    }

    public Button getBackButton(){
        if(mBackBtn == null) {
            try {
                Class clazz = getTargetClass();
                Object obj = getTargetObject();

                Method method = clazz.getMethod("getBackButton");
                mBackBtn = (Button) method.invoke(obj);
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(TAG, "getBackButton fail");
            }
        }
        return mBackBtn;
    }

    public void setHeaderText(int resId){
        try {
            String str = mContext.getString(resId);
            setHeaderText(str);
        } catch(Exception e) {
            e.printStackTrace();
            Log.d(TAG, "setHeaderText fail");
        }
    }

    public void setHeaderText(String str){
        try {
            Class clazz = getTargetClass();
            Object obj = getTargetObject();

            Method method = clazz.getMethod("setHeaderText", new Class[]{String.class});
            method.invoke(obj, str);
        } catch(Exception e) {
            e.printStackTrace();
            Log.d(TAG, "setHeaderText fail");
        }
    }

    public void setSubContentView(int viewId){
        setSubContentView(viewId, false);
    }

    public void setSubContentView(int viewId, boolean padding){
        try {
            LayoutInflater layoutInflater = LayoutInflater.from(mContext);
            View myView = layoutInflater.inflate(viewId, null);
            setSubContentView(myView, padding);
        } catch(Exception e) {
            e.printStackTrace();
            Log.d(TAG, "setContent fail");
        }
    }

    public void setSubContentView(View view){
        setSubContentView(view, false);
    }

    public void setSubContentView(View view, boolean padding){
        try {
            Class clazz = getTargetClass();
            Object obj = getTargetObject();

            Method method = clazz.getMethod("setSubContentView", new Class[]{View.class, boolean.class});
            method.invoke(obj, view, padding);
        } catch(Exception e) {
            e.printStackTrace();
            Log.d(TAG, "setContent fail");
        }
    }

    public void setIllustration(int illustration) {
        try {
            Object obj = getTargetObject();
            Class clazz = getTargetClass();
            Drawable drawable  = mContext.getResources().getDrawable(illustration);

            Method method = clazz.getMethod("setIllustration", new Class[]{Drawable.class});
            method.invoke(obj, drawable);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void setIllustration(Drawable illustration) {
        try {
            Object obj = getTargetObject();
            Class clazz = getTargetClass();
            Method method = clazz.getMethod("setIllustration", new Class[]{Drawable.class});
            method.invoke(obj, illustration);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void initActivity(Activity activity) {
        try {
            Object obj = getTargetObject();
            Class clazz = getTargetClass();
            Method method = clazz.getMethod("initActivity", new Class[]{Activity.class});
            method.invoke(obj, activity);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("InlinedApi")
    private static final int DIALOG_IMMERSIVE_FLAGS =
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;


    /**
     * Hide the navigation bar for a dialog.
     *
     * This will only take effect in versions Lollipop or above. Otherwise this is a no-op.
     */
    public static void hideSystemBars(final Dialog dialog) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final Window window = dialog.getWindow();
            temporarilyDisableDialogFocus(window);
            addImmersiveFlagsToWindow(window, DIALOG_IMMERSIVE_FLAGS);
            addImmersiveFlagsToDecorView(window, new Handler(), DIALOG_IMMERSIVE_FLAGS);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static void addImmersiveFlagsToWindow(final Window window, final int vis) {
        WindowManager.LayoutParams attrs = window.getAttributes();
        attrs.systemUiVisibility |= vis;
        window.setAttributes(attrs);

        // Also set the navigation bar and status bar to transparent color. Note that this doesn't
        // work on some devices.
        window.setNavigationBarColor(0);
        window.setStatusBarColor(0);
    }

    /**
     * View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN only takes effect when it is added a view instead of
     * the window.
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static void addImmersiveFlagsToDecorView(final Window window, final Handler handler,
                                                     final int vis) {
        // Use peekDecorView instead of getDecorView so that clients can still set window features
        // after calling this method.
        final View decorView = window.peekDecorView();
        if (decorView != null) {
            addVisibilityFlag(decorView, vis);
        } else {
            // If the decor view is not installed yet, try again in the next loop.
            handler.post(new Runnable() {
                @Override
                public void run() {
                    addImmersiveFlagsToDecorView(window, handler, vis);
                }
            });
        }
    }

    /**
     * Convenience method to add a visibility flag in addition to the existing ones.
     */
    public static void addVisibilityFlag(final View view, final int flag) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            final int vis = view.getSystemUiVisibility();
            view.setSystemUiVisibility(vis | flag);
        }
    }

    /**
     * Apply a hack to temporarily set the window to not focusable, so that the navigation bar
     * will not show up during the transition.
     */
    private static void temporarilyDisableDialogFocus(final Window window) {
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        // Add the SOFT_INPUT_IS_FORWARD_NAVIGATION_FLAG. This is normally done by the system when
        // FLAG_NOT_FOCUSABLE is not set. Setting this flag allows IME to be shown automatically
        // if the dialog has editable text fields.
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_IS_FORWARD_NAVIGATION);
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            }
        });
    }
}
