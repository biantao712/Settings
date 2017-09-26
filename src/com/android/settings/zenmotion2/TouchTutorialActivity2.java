package com.android.settings.zenmotion2;

import android.app.ActionBar;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.android.settings.zenmotion.GifView;
import com.android.settings.R;
import android.view.MenuItem;


import java.util.ArrayList;
import java.util.List;

public class TouchTutorialActivity2 extends FragmentActivity implements OnPageChangeListener{

    private static final String TAG = "TouchTutorialActivity2";
    private static final String TUTORIAL_PAGE = "tutorial_page";
    private static final int FIRST_PAGE = 0;

    private static List<DisplayPagination> mDisplayPaginations ;
    private TextView mTextViewDone;
    private List<View> mPaginationIndicators ;
    private TextView mStepTitle;
    private TextView mStepDescription;

    public static class DisplayPagination {
        public int titleTextId;
        public int descriptionTextId;
        public int tutorialImageId;
    }

    static enum Pagination {
        FIRST(R.string.w_launch_title,
                R.string.tutorial_w_launch_summary,
                R.drawable.asus_w_gesture,
                R.id.indicator_page1),
        SECOND(R.string.s_launch_title,
                R.string.tutorial_s_launch_summary,
                R.drawable.asus_s_gesture,
                R.id.indicator_page2),
        THIRD(R.string.e_launch_title,
                R.string.tutorial_e_launch_summary,
                R.drawable.asus_e_gesture,
                R.id.indicator_page3),
        FOURTH(R.string.c_launch_title,
                R.string.tutorial_c_launch_summary,
                R.drawable.asus_c_gesture,
                R.id.indicator_page4),
        FIFTH(R.string.z_launch_title,
                R.string.tutorial_z_launch_summary,
                R.drawable.asus_z_gesture,
                R.id.indicator_page5),
        SIXTH(R.string.v_launch_title,
                R.string.tutorial_v_launch_summary,
                R.drawable.asus_v_gesture,
                R.id.indicator_page6),
        SEVENTH(R.string.doubletap_on_setting_title,
                R.string.doubletap_on_setting_summary,
                R.drawable.asus_wakeup,
                R.id.indicator_page7),
        EIGHTH(R.string.doubletap_off_setting_title,
                R.string.doubletap_off_setting_summary,
                R.drawable.asus_suspend,
                R.id.indicator_page8),
        NINTH(R.string.tutorial_swipe_up_title,
                R.string.tutorial_swipe_up_summary,
                R.drawable.asus_swipe_up,
                R.id.indicator_page9);

        private final int titleTextId;
        private final int descriptionTextId;
        private final int tutorialImageId;
        private final int viewId;

        private Pagination(int titleTextId, int descriptionTextId, int tutorialImageId, int viewId) {
            this.titleTextId = titleTextId;
            this.descriptionTextId = descriptionTextId;
            this.tutorialImageId = tutorialImageId;
            this.viewId = viewId;
        }
    }

    public  static class TutorialFragment2 extends Fragment {

		// Public no-args constructor needed for fragment re-instantiation, multi-window issue
	    public TutorialFragment2() {super();}
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            DisplayPagination page = mDisplayPaginations.get(getArguments().getInt(TUTORIAL_PAGE, FIRST_PAGE));
            View root = inflater.inflate(R.layout.tutorial_gif, container, false);
            ((GifView) root.findViewById(R.id.tutorial_gif))
                    .setGifResource(getActivity(),page.tutorialImageId);
            return root;
        }
    }

    private class TutorialPagerAdapter extends FragmentPagerAdapter {
        private TutorialPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            TutorialFragment2 fragment = new TutorialFragment2();
            Bundle bundle = new Bundle();
            bundle.putInt(TUTORIAL_PAGE, position);
            fragment.setArguments(bundle);
            return fragment;
        }
        @Override
        public int getCount() {
            return mDisplayPaginations.size();
        }
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
            case 16908322:
                Log.i(TAG,"intent back to Setting,setResult ");
				super.onBackPressed();
				finishAffinity();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Theme_SubSettings, Theme_Settings_NoActionBar
//        setTheme(R.style.Theme_Settings_NoActionBar);
        //setTheme(R.style.Theme_SubSettings);
		setTheme(R.style.Theme_CNAsusRes);
        setContentView(R.layout.zenmotion2_touch_tutorial_layout);
//        ActionBar actionBar = getActionBar();
//        if (actionBar != null) {
//            actionBar.setDisplayHomeAsUpEnabled(false);
//            actionBar.setHomeButtonEnabled(false);
//        }
		getWindow().getDecorView().setSystemUiVisibility(
				  View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
		getWindow().setStatusBarColor(Color.TRANSPARENT);

        initViews();

        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        pager.setAdapter(new TutorialPagerAdapter(getSupportFragmentManager()));
        pager.setOnPageChangeListener(this);
        switchToPage(FIRST_PAGE);
    }

    private void initViews() {
		/*
        final boolean isSupportDoubleTap = getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_ASUS_TOUCHGESTURE_DOUBLE_TAP);

        final boolean isSupportGestureLunchApp = getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_ASUS_TOUCHGESTURE_LAUNCH_APP);

        final boolean isSupportSwipeUp = getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_ASUS_TOUCHGESTURE_SWIPE_UP);
        */
		final boolean isSupportDoubleTap = false;		
		final boolean isSupportSwipeUp = false;
		final boolean isSupportGestureLunchApp = true;

        //test
//         isSupportGestureLunchApp = false;
        mDisplayPaginations = new ArrayList<>();
        mPaginationIndicators = new ArrayList<>();
        if(isSupportDoubleTap){
            for(int i = 6 ; i < 8 ; i++){
                DisplayPagination displayPagination = new DisplayPagination();
                displayPagination.titleTextId = Pagination.values()[i].titleTextId;
                displayPagination.descriptionTextId = Pagination.values()[i].descriptionTextId;
                displayPagination.tutorialImageId = Pagination.values()[i].tutorialImageId;
                mDisplayPaginations.add(displayPagination);
                View paginationView = (View)findViewById( Pagination.values()[i].viewId);
                paginationView.setVisibility(View.VISIBLE);
                mPaginationIndicators.add(paginationView);
            }
        }

        if (isSupportSwipeUp) {
            DisplayPagination displayPagination = new DisplayPagination();
            displayPagination.titleTextId = Pagination.NINTH.titleTextId;
            displayPagination.descriptionTextId = Pagination.NINTH.descriptionTextId;
            displayPagination.tutorialImageId = Pagination.NINTH.tutorialImageId;
            mDisplayPaginations.add(displayPagination);
            View paginationView = (View) findViewById(Pagination.NINTH.viewId);
            paginationView.setVisibility(View.VISIBLE);
            mPaginationIndicators.add(paginationView);
        }

        if(isSupportGestureLunchApp){
            for(int i = 0 ; i < 6 ; i++){
                DisplayPagination displayPagination = new DisplayPagination();
                displayPagination.titleTextId = Pagination.values()[i].titleTextId;
                displayPagination.descriptionTextId = Pagination.values()[i].descriptionTextId;
                displayPagination.tutorialImageId = Pagination.values()[i].tutorialImageId;
                mDisplayPaginations.add(displayPagination);
                View paginationView = (View)findViewById( Pagination.values()[i].viewId);
                paginationView.setVisibility(View.VISIBLE);
                mPaginationIndicators.add(paginationView);
            }
        }

        mStepTitle = (TextView) findViewById(R.id.tutorial_title);
        mStepDescription = (TextView) findViewById(R.id.tutorial_message);
        mStepDescription.setMovementMethod(new ScrollingMovementMethod());
		//UI spec requirement, no done button.
		/*
        mTextViewDone = (TextView) findViewById(R.id.text_done);
        mTextViewDone.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        */
    }

    private void switchToPage(int page) {
        DisplayPagination currentPage = mDisplayPaginations.get(page);

        for (int pageOrdinal = FIRST_PAGE; pageOrdinal < mPaginationIndicators.size(); pageOrdinal++) {
            mPaginationIndicators.get(pageOrdinal).setEnabled(pageOrdinal == page);
        }

        mStepTitle.setText(currentPage.titleTextId);
        mStepDescription.setText(getString(currentPage.descriptionTextId));
        mStepDescription.scrollTo(0, 0);

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        finishAffinity();
    }
    @Override
    public void onPageScrollStateChanged(int arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onPageScrolled(int arg0, float arg1, int arg2) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onPageSelected(int arg0) {
        // TODO Auto-generated method stub
        switchToPage(arg0);
    }

}
