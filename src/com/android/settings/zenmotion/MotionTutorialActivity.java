
package com.android.settings.zenmotion;

import android.app.ActionBar;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
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
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.settings.zenmotion.GifView;
import com.android.settings.R;

import java.util.ArrayList;
import java.util.List;

public class MotionTutorialActivity extends FragmentActivity implements OnPageChangeListener {

    private static final String TAG = "MotionTutorialActivity";
    private static final String TUTORIAL_PAGE = "tutorial_page";
    private static final int FIRST_PAGE = 0;

    private static List<DisplayPagination> mDisplayPaginations;
    private TextView mTextViewDone;
    private List<View> mPaginationIndicators;
    private TextView mStepTitle;
    private TextView mStepDescription;

    public static class DisplayPagination {
        public int titleTextId;
        public int descriptionTextId;
        public int tutorialImageId;
    }

    static enum Pagination {
        FLIP(R.string.tutorial_flip_down_title,
                R.string.tutorial_flip_down_summary,
                R.drawable.asus_flip_down,
                R.id.indicator_page1),
        HAND_UP(R.string.motion_hand_up_title,
                R.string.tutorial_hands_up_summary,
                R.drawable.asus_hands_up,
                R.id.indicator_page2);

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

    public static class TutorialFragment extends Fragment {
        public TutorialFragment(){}
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            DisplayPagination page = mDisplayPaginations.get(getArguments().getInt(TUTORIAL_PAGE,
                    FIRST_PAGE));
            View root = inflater.inflate(R.layout.tutorial_gif, container, false);
            ((GifView) root.findViewById(R.id.tutorial_gif))
                    .setGifResource(getActivity(), page.tutorialImageId);
            return root;
        }
    }

    private class TutorialPagerAdapter extends FragmentPagerAdapter {
        private TutorialPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            TutorialFragment fragment = new TutorialFragment();
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Theme_SubSettings, Theme_Settings_NoActionBar
//        setTheme(R.style.Theme_Settings_NoActionBar);
        setTheme(R.style.Theme_SubSettings);
        setContentView(R.layout.zenmotion_touch_tutorial_layout);
//        ActionBar actionBar = getActionBar();
//        if (actionBar != null) {
//            actionBar.setDisplayHomeAsUpEnabled(false);
//            actionBar.setHomeButtonEnabled(false);
//        }

        initViews();

        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        pager.setAdapter(new TutorialPagerAdapter(getSupportFragmentManager()));
        pager.setOnPageChangeListener(this);
        switchToPage(FIRST_PAGE);
    }

    private void initViews() {
        final boolean isSupportFlip = getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_ASUS_SENSOR_SERVICE_TERMINAL);

        final boolean isSupportHandsUp = getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_ASUS_SENSOR_SERVICE_EARTOUCH);

        mDisplayPaginations = new ArrayList<>();
        mPaginationIndicators = new ArrayList<>();

        if (isSupportFlip) {
            DisplayPagination displayPagination = new DisplayPagination();
            displayPagination.titleTextId = Pagination.FLIP.titleTextId;
            displayPagination.descriptionTextId = Pagination.FLIP.descriptionTextId;
            displayPagination.tutorialImageId = Pagination.FLIP.tutorialImageId;
            mDisplayPaginations.add(displayPagination);
            View paginationView = (View) findViewById(Pagination.FLIP.viewId);
            paginationView.setVisibility(View.VISIBLE);
            mPaginationIndicators.add(paginationView);
        }

        if (isSupportHandsUp) {
            DisplayPagination displayPagination = new DisplayPagination();
            displayPagination.titleTextId = Pagination.HAND_UP.titleTextId;
            displayPagination.descriptionTextId = Pagination.HAND_UP.descriptionTextId;
            displayPagination.tutorialImageId = Pagination.HAND_UP.tutorialImageId;
            mDisplayPaginations.add(displayPagination);
            View paginationView = (View) findViewById(Pagination.HAND_UP.viewId);
            paginationView.setVisibility(View.VISIBLE);
            mPaginationIndicators.add(paginationView);
        }

        mStepTitle = (TextView) findViewById(R.id.tutorial_title);
        mStepDescription = (TextView) findViewById(R.id.tutorial_message);
        mStepDescription.setMovementMethod(new ScrollingMovementMethod());
        mTextViewDone = (TextView) findViewById(R.id.text_done);
        mTextViewDone.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
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
