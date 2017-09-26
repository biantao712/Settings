package com.android.settings;

import android.app.PendingIntent;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import java.lang.reflect.Method;
import java.util.WeakHashMap;

import static com.android.settings.AsusSuggestionsAdapter.getColumnString;

public class AsusSearchView extends LinearLayout {
    private static final boolean DBG = false;
    private static final String LOG_TAG = "AsusSearchView";

    private final SearchAutoComplete mSearchSrcTextView;
    private final View mSearchPlate;
    private final Button mCancelBtn;
    private final ImageView mCleanBtn;
    private final CharSequence mDefaultQueryHint;
    private final View mDropDownAnchor;

    private UpdatableTouchDelegate mTouchDelegate;
    private Rect mSearchSrcTextViewBounds = new Rect();
    private Rect mSearchSrtTextViewBoundsExpanded = new Rect();
    private int[] mTemp = new int[2];
    private int[] mTemp2 = new int[2];

    // Resources used by SuggestionsAdapter to display suggestions.
    private final int mSuggestionRowLayout;
    private final int mSuggestionCommitIconResId;

    private OnQueryTextListener mOnQueryChangeListener;
    private OnCloseListener mOnCloseListener;
    private OnFocusChangeListener mOnQueryTextFocusChangeListener;
    private OnSuggestionListener mOnSuggestionListener;
    private OnClickListener mOnSearchClickListener;

    private CursorAdapter mSuggestionsAdapter;
    private boolean mSubmitButtonEnabled;
    private CharSequence mQueryHint;
    private boolean mQueryRefinement;
    private boolean mClearingFocus;
    private int mMaxWidth;
    private CharSequence mOldQueryText;
    private CharSequence mUserQuery;
    private int mCollapsedImeOptions;

    private SearchableInfo mSearchable;
    private Bundle mAppSearchData;

    /*
     * SearchView can be set expanded before the IME is ready to be shown during
     * initial UI setup. The show operation is asynchronous to account for this.
     */
    private Runnable mShowImeRunnable = new Runnable() {
        public void run() {
            InputMethodManager imm = getContext().getSystemService(InputMethodManager.class);

            if (imm != null) {
                imm.showSoftInput(AsusSearchView.this, 0, null);
            }
        }
    };

    private Runnable mUpdateDrawableStateRunnable = new Runnable() {
        public void run() {
            updateFocusedState();
        }
    };

    private Runnable mReleaseCursorRunnable = new Runnable() {
        public void run() {
            if (mSuggestionsAdapter != null && mSuggestionsAdapter instanceof AsusSuggestionsAdapter) {
                mSuggestionsAdapter.changeCursor(null);
            }
        }
    };

    // A weak map of drawables we've gotten from other packages, so we don't load them
    // more than once.
    private final WeakHashMap<String, Drawable.ConstantState> mOutsideDrawablesCache =
            new WeakHashMap<String, Drawable.ConstantState>();

    /**
     * Callbacks for changes to the query text.
     */
    public interface OnQueryTextListener {

        /**
         * Called when the user submits the query. This could be due to a key press on the
         * keyboard or due to pressing a submit button.
         * The listener can override the standard behavior by returning true
         * to indicate that it has handled the submit request. Otherwise return false to
         * let the SearchView handle the submission by launching any associated intent.
         *
         * @param query the query text that is to be submitted
         *
         * @return true if the query has been handled by the listener, false to let the
         * SearchView perform the default action.
         */
        boolean onQueryTextSubmit(String query);

        /**
         * Called when the query text is changed by the user.
         *
         * @param newText the new content of the query text field.
         *
         * @return false if the SearchView should perform the default action of showing any
         * suggestions if available, true if the action was handled by the listener.
         */
        boolean onQueryTextChange(String newText);
    }

    public interface OnCloseListener {

        /**
         * The user is attempting to close the SearchView.
         *
         * @return true if the listener wants to override the default behavior of clearing the
         * text field and dismissing it, false otherwise.
         */
        boolean onClose();
    }

    /**
     * Callback interface for selection events on suggestions. These callbacks
     * are only relevant when a SearchableInfo has been specified by {@link #setSearchableInfo}.
     */
    public interface OnSuggestionListener {

        /**
         * Called when a suggestion was selected by navigating to it.
         * @param position the absolute position in the list of suggestions.
         *
         * @return true if the listener handles the event and wants to override the default
         * behavior of possibly rewriting the query based on the selected item, false otherwise.
         */
        boolean onSuggestionSelect(int position);

        /**
         * Called when a suggestion was clicked.
         * @param position the absolute position of the clicked item in the list of suggestions.
         *
         * @return true if the listener handles the event and wants to override the default
         * behavior of launching any intent or submitting a search query specified on that item.
         * Return false otherwise.
         */
        boolean onSuggestionClick(int position);
    }

    public AsusSearchView(Context context) {
        this(context, null);
    }

    public AsusSearchView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.searchViewStyle);
    }

    public AsusSearchView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public AsusSearchView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        final LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.cn_search_view, this, true);

        mCancelBtn = (Button) findViewById(R.id.search_cancel);
        mCancelBtn.setOnClickListener(mOnClickListener);
        mCleanBtn = (ImageView) findViewById(R.id.search_clean);
        mCleanBtn.setOnClickListener(mOnClickListener);

        mSearchSrcTextView = (SearchAutoComplete) findViewById(R.id.search_text);
        mSearchSrcTextView.setSearchView(this);

        mSearchPlate = findViewById(R.id.search_edit);

        // Extract dropdown layout resource IDs for later use.
        mSuggestionRowLayout = R.layout.cnasusres_search_dropdown_item_icons_2line;
        mSuggestionCommitIconResId = 0;

        mSearchSrcTextView.setOnClickListener(mOnClickListener);

        mSearchSrcTextView.addTextChangedListener(mTextWatcher);
        mSearchSrcTextView.setOnEditorActionListener(mOnEditorActionListener);
        mSearchSrcTextView.setOnItemClickListener(mOnItemClickListener);
        mSearchSrcTextView.setOnItemSelectedListener(mOnItemSelectedListener);
        mSearchSrcTextView.setOnKeyListener(mTextKeyListener);

        // Inform any listener of focus changes
        mSearchSrcTextView.setOnFocusChangeListener(new OnFocusChangeListener() {

            public void onFocusChange(View v, boolean hasFocus) {
                if (mOnQueryTextFocusChangeListener != null) {
                    mOnQueryTextFocusChangeListener.onFocusChange(AsusSearchView.this, hasFocus);
                }
            }
        });

        mDefaultQueryHint = "";
        setFocusable(true);

        mDropDownAnchor = findViewById(mSearchSrcTextView.getDropDownAnchor());
        if (mDropDownAnchor != null) {
            mDropDownAnchor.addOnLayoutChangeListener(new OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                           int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    adjustDropDownSizeAndPosition();
                }
            });
        }
    }

    int getSuggestionRowLayout() {
        return mSuggestionRowLayout;
    }

    int getSuggestionCommitIconResId() {
        return mSuggestionCommitIconResId;
    }

    /**
     * Sets the SearchableInfo for this SearchView. Properties in the SearchableInfo are used
     * to display labels, hints, suggestions, create intents for launching search results screens
     * and controlling other affordances such as a voice button.
     *
     * @param searchable a SearchableInfo can be retrieved from the SearchManager, for a specific
     * activity or a global search provider.
     */
    public void setSearchableInfo(SearchableInfo searchable) {
        mSearchable = searchable;
        if (mSearchable != null) {
            updateSearchAutoComplete();
            updateQueryHint();
        }
    }

    /**
     * Sets the APP_DATA for legacy SearchDialog use.
     * @param appSearchData bundle provided by the app when launching the search dialog
     * @hide
     */
    public void setAppSearchData(Bundle appSearchData) {
        mAppSearchData = appSearchData;
    }

    /**
     * Sets the IME options on the query text field.
     *
     * @see TextView#setImeOptions(int)
     * @param imeOptions the options to set on the query text field
     *
     * @attr ref android.R.styleable#SearchView_imeOptions
     */
    public void setImeOptions(int imeOptions) {
        mSearchSrcTextView.setImeOptions(imeOptions);
    }

    /**
     * Returns the IME options set on the query text field.
     * @return the ime options
     * @see TextView#setImeOptions(int)
     *
     * @attr ref android.R.styleable#SearchView_imeOptions
     */
    public int getImeOptions() {
        return mSearchSrcTextView.getImeOptions();
    }

    /**
     * Sets the input type on the query text field.
     *
     * @see TextView#setInputType(int)
     * @param inputType the input type to set on the query text field
     *
     * @attr ref android.R.styleable#SearchView_inputType
     */
    public void setInputType(int inputType) {
        mSearchSrcTextView.setInputType(inputType);
    }

    /**
     * Returns the input type set on the query text field.
     * @return the input type
     *
     * @attr ref android.R.styleable#SearchView_inputType
     */
    public int getInputType() {
        return mSearchSrcTextView.getInputType();
    }

    /** @hide */
    @Override
    public boolean requestFocus(int direction, Rect previouslyFocusedRect) {
        // Don't accept focus if in the middle of clearing focus
        if (mClearingFocus) return false;
        // Check if SearchView is focusable.
        if (!isFocusable()) return false;
        // If it is not iconified, then give the focus to the text field
        return mSearchSrcTextView.requestFocus(direction, previouslyFocusedRect);
    }

    /** @hide */
    @Override
    public void clearFocus() {
        mClearingFocus = true;
        setImeVisibility(false);
        super.clearFocus();
        mSearchSrcTextView.clearFocus();
        mClearingFocus = false;
    }

    /**
     * Sets a listener for user actions within the SearchView.
     *
     * @param listener the listener object that receives callbacks when the user performs
     * actions in the SearchView such as clicking on buttons or typing a query.
     */
    public void setOnQueryTextListener(OnQueryTextListener listener) {
        mOnQueryChangeListener = listener;
    }

    /**
     * Sets a listener to inform when the user closes the SearchView.
     *
     * @param listener the listener to call when the user closes the SearchView.
     */
    public void setOnCloseListener(OnCloseListener listener) {
        mOnCloseListener = listener;
    }

    /**
     * Sets a listener to inform when the focus of the query text field changes.
     *
     * @param listener the listener to inform of focus changes.
     */
    public void setOnQueryTextFocusChangeListener(OnFocusChangeListener listener) {
        mOnQueryTextFocusChangeListener = listener;
    }

    /**
     * Sets a listener to inform when a suggestion is focused or clicked.
     *
     * @param listener the listener to inform of suggestion selection events.
     */
    public void setOnSuggestionListener(OnSuggestionListener listener) {
        mOnSuggestionListener = listener;
    }

    /**
     * Sets a listener to inform when the search button is pressed.
     * @param listener the listener to inform when the search button is clicked or
     * the text field is programmatically de-iconified.
     */
    public void setOnSearchClickListener(OnClickListener listener) {
        mOnSearchClickListener = listener;
    }

    /**
     * Returns the query string currently in the text field.
     *
     * @return the query string
     */
    public CharSequence getQuery() {
        return mSearchSrcTextView.getText();
    }

    /**
     * Sets a query string in the text field and optionally submits the query as well.
     *
     * @param query the query string. This replaces any query text already present in the
     * text field.
     * @param submit whether to submit the query right now or only update the contents of
     * text field.
     */
    public void setQuery(CharSequence query, boolean submit) {
        mSearchSrcTextView.setText(query);
        if (query != null) {
            mSearchSrcTextView.setSelection(mSearchSrcTextView.length());
            mUserQuery = query;
        }

        // If the query is not empty and submit is requested, submit the query
        if (submit && !TextUtils.isEmpty(query)) {
            onSubmitQuery();
        }
    }

    /**
     * Sets the hint text to display in the query text field. This overrides
     * any hint specified in the {@link SearchableInfo}.
     * <p>
     * This value may be specified as an empty string to prevent any query hint
     * from being displayed.
     *
     * @param hint the hint text to display or {@code null} to clear
     * @attr ref android.R.styleable#SearchView_queryHint
     */
    public void setQueryHint(CharSequence hint) {
        mQueryHint = hint;
        updateQueryHint();
    }

    public CharSequence getQueryHint() {
        final CharSequence hint;
        if (mQueryHint != null) {
            hint = mQueryHint;
        } else if (mSearchable != null && mSearchable.getHintId() != 0) {
            hint = getContext().getString(mSearchable.getHintId());
        } else {
            hint = mDefaultQueryHint;
        }
        return hint;
    }

    /**
     * Specifies if a query refinement button should be displayed alongside each suggestion
     * or if it should depend on the flags set in the individual items retrieved from the
     * suggestions provider. Clicking on the query refinement button will replace the text
     * in the query text field with the text from the suggestion. This flag only takes effect
     * if a SearchableInfo has been specified with {@link #setSearchableInfo(SearchableInfo)}
     * and not when using a custom adapter.
     *
     * @param enable true if all items should have a query refinement button, false if only
     * those items that have a query refinement flag set should have the button.
     *
     * @see SearchManager#SUGGEST_COLUMN_FLAGS
     * @see SearchManager#FLAG_QUERY_REFINEMENT
     */
    public void setQueryRefinementEnabled(boolean enable) {
        mQueryRefinement = enable;
        if (mSuggestionsAdapter instanceof AsusSuggestionsAdapter) {
            ((AsusSuggestionsAdapter) mSuggestionsAdapter).setQueryRefinement(
                    enable ? AsusSuggestionsAdapter.REFINE_ALL : AsusSuggestionsAdapter.REFINE_BY_ENTRY);
        }
    }

    /**
     * Returns whether query refinement is enabled for all items or only specific ones.
     * @return true if enabled for all items, false otherwise.
     */
    public boolean isQueryRefinementEnabled() {
        return mQueryRefinement;
    }

    /**
     * You can set a custom adapter if you wish. Otherwise the default adapter is used to
     * display the suggestions from the suggestions provider associated with the SearchableInfo.
     *
     * @see #setSearchableInfo(SearchableInfo)
     */
    public void setSuggestionsAdapter(CursorAdapter adapter) {
        mSuggestionsAdapter = adapter;

        mSearchSrcTextView.setAdapter(mSuggestionsAdapter);
    }

    /**
     * Returns the adapter used for suggestions, if any.
     * @return the suggestions adapter
     */
    public CursorAdapter getSuggestionsAdapter() {
        return mSuggestionsAdapter;
    }

    /**
     * Makes the view at most this many pixels wide
     *
     * @attr ref android.R.styleable#SearchView_maxWidth
     */
    public void setMaxWidth(int maxpixels) {
        mMaxWidth = maxpixels;

        requestLayout();
    }

    /**
     * Gets the specified maximum width in pixels, if set. Returns zero if
     * no maximum width was specified.
     * @return the maximum width of the view
     *
     * @attr ref android.R.styleable#SearchView_maxWidth
     */
    public int getMaxWidth() {
        return mMaxWidth;
    }

//    @Override
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
//        int width = MeasureSpec.getSize(widthMeasureSpec);
//
//        switch (widthMode) {
//            case MeasureSpec.AT_MOST:
//                // If there is an upper limit, don't exceed maximum width (explicit or implicit)
//                if (mMaxWidth > 0) {
//                    width = Math.min(mMaxWidth, width);
//                } else {
//                    width = Math.min(getPreferredWidth(), width);
//                }
//                break;
//            case MeasureSpec.EXACTLY:
//                // If an exact width is specified, still don't exceed any specified maximum width
//                if (mMaxWidth > 0) {
//                    width = Math.min(mMaxWidth, width);
//                }
//                break;
//            case MeasureSpec.UNSPECIFIED:
//                // Use maximum width, if specified, else preferred width
//                width = mMaxWidth > 0 ? mMaxWidth : getPreferredWidth();
//                break;
//        }
//        widthMode = MeasureSpec.EXACTLY;
//
//        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
//        int height = MeasureSpec.getSize(heightMeasureSpec);
//
//        switch (heightMode) {
//            case MeasureSpec.AT_MOST:
//            case MeasureSpec.UNSPECIFIED:
//                height = Math.min(getPreferredHeight(), height);
//                break;
//        }
//        heightMode = MeasureSpec.EXACTLY;
//
//        super.onMeasure(MeasureSpec.makeMeasureSpec(width, widthMode),
//                MeasureSpec.makeMeasureSpec(height, heightMode));
//    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (changed) {
            // Expand mSearchSrcTextView touch target to be the height of the parent in order to
            // allow it to be up to 48dp.
            getChildBoundsWithinSearchView(mSearchSrcTextView, mSearchSrcTextViewBounds);
            mSearchSrtTextViewBoundsExpanded.set(
                    mSearchSrcTextViewBounds.left, 0, mSearchSrcTextViewBounds.right, bottom - top);
            if (mTouchDelegate == null) {
                mTouchDelegate = new UpdatableTouchDelegate(mSearchSrtTextViewBoundsExpanded,
                        mSearchSrcTextViewBounds, mSearchSrcTextView);
                setTouchDelegate(mTouchDelegate);
            } else {
                mTouchDelegate.setBounds(mSearchSrtTextViewBoundsExpanded, mSearchSrcTextViewBounds);
            }
        }
    }

    private void getChildBoundsWithinSearchView(View view, Rect rect) {
        view.getLocationInWindow(mTemp);
        getLocationInWindow(mTemp2);
        final int top = mTemp[1] - mTemp2[1];
        final int left = mTemp[0] - mTemp2[0];
        rect.set(left , top, left + view.getWidth(), top + view.getHeight());
    }

    private int getPreferredWidth() {
        return getContext().getResources()
                .getDimensionPixelSize(R.dimen.search_view_preferred_width);
    }

    private int getPreferredHeight() {
        return getContext().getResources()
                .getDimensionPixelSize(R.dimen.search_view_preferred_height);
    }

    private void postUpdateFocusedState() {
        post(mUpdateDrawableStateRunnable);
    }

    private void updateFocusedState() {
        final boolean focused = mSearchSrcTextView.hasFocus();
        final int[] stateSet = focused ? FOCUSED_STATE_SET : EMPTY_STATE_SET;
        final Drawable searchPlateBg = mSearchPlate.getBackground();
        if (searchPlateBg != null) {
            searchPlateBg.setState(stateSet);
        }
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(mUpdateDrawableStateRunnable);
        post(mReleaseCursorRunnable);
        super.onDetachedFromWindow();
    }

    private void setImeVisibility(final boolean visible) {
        if (visible) {
            post(mShowImeRunnable);
        } else {
            removeCallbacks(mShowImeRunnable);
            InputMethodManager imm = getContext().getSystemService(InputMethodManager.class);

            if (imm != null) {
                imm.hideSoftInputFromWindow(getWindowToken(), 0);
            }
        }
    }

    /**
     * Called by the SuggestionsAdapter
     * @hide
     */
    /* package */void onQueryRefine(CharSequence queryText) {
        setQuery(queryText);
    }

    private final OnClickListener mOnClickListener = new OnClickListener() {

        public void onClick(View v) {
            if (v == mCancelBtn) {
                onCloseClicked();
            } else if (v == mCleanBtn) {
                onCleanClicked();
            } else if (v == mSearchSrcTextView) {
                forceSuggestionQuery();
            }
        }
    };

    /**
     * Handles the key down event for dealing with action keys.
     *
     * @param keyCode This is the keycode of the typed key, and is the same value as
     *        found in the KeyEvent parameter.
     * @param event The complete event record for the typed key
     *
     * @return true if the event was handled here, or false if not.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mSearchable == null) {
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * React to the user typing "enter" or other hardwired keys while typing in
     * the search box. This handles these special keys while the edit box has
     * focus.
     */
    View.OnKeyListener mTextKeyListener = new View.OnKeyListener() {
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            // guard against possible race conditions
            if (mSearchable == null) {
                return false;
            }

            if (DBG) {
                Log.d(LOG_TAG, "mTextListener.onKey(" + keyCode + "," + event + "), selection: "
                        + mSearchSrcTextView.getListSelection());
            }

            // If a suggestion is selected, handle enter, search key, and action keys
            // as presses on the selected suggestion
            if (mSearchSrcTextView.isPopupShowing()
                    && mSearchSrcTextView.getListSelection() != ListView.INVALID_POSITION) {
                return onSuggestionsKey(v, keyCode, event);
            }

            // If there is text in the query box, handle enter, and action keys
            // The search key is handled by the dialog's onKeyDown().
            if (!mSearchSrcTextView.isEmpty() && event.hasNoModifiers()) {
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    if (keyCode == KeyEvent.KEYCODE_ENTER) {
                        v.cancelLongPress();

                        // Launch as a regular search.
                        launchQuerySearch(KeyEvent.KEYCODE_UNKNOWN, null, mSearchSrcTextView.getText()
                                .toString());
                        return true;
                    }
                }
            }
            return false;
        }
    };

    /**
     * React to the user typing while in the suggestions list. First, check for
     * action keys. If not handled, try refocusing regular characters into the
     * EditText.
     */
    private boolean onSuggestionsKey(View v, int keyCode, KeyEvent event) {
        // guard against possible race conditions (late arrival after dismiss)
        if (mSearchable == null) {
            return false;
        }
        if (mSuggestionsAdapter == null) {
            return false;
        }
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.hasNoModifiers()) {
            // First, check for enter or search (both of which we'll treat as a
            // "click")
            if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_SEARCH
                    || keyCode == KeyEvent.KEYCODE_TAB) {
                int position = mSearchSrcTextView.getListSelection();
                return onItemClicked(position, KeyEvent.KEYCODE_UNKNOWN, null);
            }

            // Next, check for left/right moves, which we use to "return" the
            // user to the edit view
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                // give "focus" to text editor, with cursor at the beginning if
                // left key, at end if right key
                // TODO: Reverse left/right for right-to-left languages, e.g.
                // Arabic
                int selPoint = (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) ? 0 : mSearchSrcTextView
                        .length();
                mSearchSrcTextView.setSelection(selPoint);
                mSearchSrcTextView.setListSelection(0);
                mSearchSrcTextView.clearListSelection();
                return true;
            }

            // Next, check for an "up and out" move
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP && 0 == mSearchSrcTextView.getListSelection()) {
                // TODO: restoreUserQuery();
                // let ACTV complete the move
                return false;
            }
        }
        return false;
    }

    private void updateQueryHint() {
        final CharSequence hint = getQueryHint();
        mSearchSrcTextView.setHint(hint);
    }

    /**
     * Updates the auto-complete text view.
     */
    private void updateSearchAutoComplete() {
        mSearchSrcTextView.setThreshold(mSearchable.getSuggestThreshold());
        mSearchSrcTextView.setImeOptions(mSearchable.getImeOptions());
        int inputType = mSearchable.getInputType();
        // We only touch this if the input type is set up for text (which it almost certainly
        // should be, in the case of search!)
        if ((inputType & InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_TEXT) {
            // The existence of a suggestions authority is the proxy for "suggestions
            // are available here"
            inputType &= ~InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE;
            if (mSearchable.getSuggestAuthority() != null) {
                inputType |= InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE;
                // TYPE_TEXT_FLAG_AUTO_COMPLETE means that the text editor is performing
                // auto-completion based on its own semantics, which it will present to the user
                // as they type. This generally means that the input method should not show its
                // own candidates, and the spell checker should not be in action. The text editor
                // supplies its candidates by calling InputMethodManager.displayCompletions(),
                // which in turn will call InputMethodSession.displayCompletions().
                inputType |= InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
            }
        }
        mSearchSrcTextView.setInputType(inputType);
        if (mSuggestionsAdapter != null) {
            mSuggestionsAdapter.changeCursor(null);
        }
        // attach the suggestions adapter, if suggestions are available
        // The existence of a suggestions authority is the proxy for "suggestions available here"
        if (mSearchable.getSuggestAuthority() != null) {
            mSuggestionsAdapter = new AsusSuggestionsAdapter(getContext(),
                    this, mSearchable, mOutsideDrawablesCache);
            mSearchSrcTextView.setAdapter(mSuggestionsAdapter);
            ((AsusSuggestionsAdapter) mSuggestionsAdapter).setQueryRefinement(
                    mQueryRefinement ? AsusSuggestionsAdapter.REFINE_ALL
                            : AsusSuggestionsAdapter.REFINE_BY_ENTRY);
        }
    }

    private final OnEditorActionListener mOnEditorActionListener = new OnEditorActionListener() {

        /**
         * Called when the input method default action key is pressed.
         */
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            onSubmitQuery();
            return true;
        }
    };

    private void onTextChanged(CharSequence newText) {
        CharSequence text = mSearchSrcTextView.getText();
        if (TextUtils.isEmpty(text))
            mCleanBtn.setVisibility(GONE);
        else
            mCleanBtn.setVisibility(VISIBLE);
        mUserQuery = text;
        if (mOnQueryChangeListener != null && !TextUtils.equals(newText, mOldQueryText)) {
            mOnQueryChangeListener.onQueryTextChange(newText.toString());
        }
        mOldQueryText = newText.toString();
    }

    private void onSubmitQuery() {
        CharSequence query = mSearchSrcTextView.getText();
        if (query != null && TextUtils.getTrimmedLength(query) > 0) {
            if (mOnQueryChangeListener == null
                    || !mOnQueryChangeListener.onQueryTextSubmit(query.toString())) {
                if (mSearchable != null) {
                    launchQuerySearch(KeyEvent.KEYCODE_UNKNOWN, null, query.toString());
                }
                setImeVisibility(false);
                dismissSuggestions();
            }
        }
    }

    private void dismissSuggestions() {
        mSearchSrcTextView.dismissDropDown();
    }

    private void onCleanClicked() {
        mSearchSrcTextView.setText("");
        mSearchSrcTextView.requestFocus();
        setImeVisibility(true);
    }

    private void onCloseClicked() {
        setImeVisibility(false);
        mSearchSrcTextView.setText(null);
        // If the app doesn't override the close behavior
        if (mOnCloseListener == null || !mOnCloseListener.onClose()) {
            // hide the keyboard and remove focus
            clearFocus();
        }
    }

    public void clearSearchText(){

        if (mSearchSrcTextView!=null)
            mSearchSrcTextView.setText(null);
    }
    private void onSearchClicked() {
        mSearchSrcTextView.requestFocus();
        setImeVisibility(true);
        if (mOnSearchClickListener != null) {
            mOnSearchClickListener.onClick(this);
        }
    }

    void onTextFocusChanged() {
        // Delayed update to make sure that the focus has settled down and window focus changes
        // don't affect it. A synchronous update was not working.
        postUpdateFocusedState();
        if (mSearchSrcTextView.hasFocus()) {
            forceSuggestionQuery();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);

        postUpdateFocusedState();
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return AsusSearchView.class.getName();
    }

    private void adjustDropDownSizeAndPosition() {
        if (mDropDownAnchor.getWidth() > 1) {
            Resources res = getContext().getResources();
            int anchorPadding = mSearchPlate.getPaddingLeft();
            Rect dropDownPadding = new Rect();

            int iconOffset = res.getDimensionPixelSize(R.dimen.dropdownitem_icon_width)
                    + res.getDimensionPixelSize(R.dimen.dropdownitem_text_padding_left);
            mSearchSrcTextView.getDropDownBackground().getPadding(dropDownPadding);
            int offset = anchorPadding - (dropDownPadding.left + iconOffset);
            mSearchSrcTextView.setDropDownHorizontalOffset(offset);
            final int width = mDropDownAnchor.getWidth() + dropDownPadding.left
                    + dropDownPadding.right + iconOffset - anchorPadding;
            mSearchSrcTextView.setDropDownWidth(width);
        }
    }

    private boolean onItemClicked(int position, int actionKey, String actionMsg) {
        if (mOnSuggestionListener == null
                || !mOnSuggestionListener.onSuggestionClick(position)) {
            launchSuggestion(position, KeyEvent.KEYCODE_UNKNOWN, null);
            setImeVisibility(false);
            dismissSuggestions();
            return true;
        }
        return false;
    }

    private boolean onItemSelected(int position) {
        if (mOnSuggestionListener == null
                || !mOnSuggestionListener.onSuggestionSelect(position)) {
            rewriteQueryFromSuggestion(position);
            return true;
        }
        return false;
    }

    private final OnItemClickListener mOnItemClickListener = new OnItemClickListener() {

        /**
         * Implements OnItemClickListener
         */
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (DBG) Log.d(LOG_TAG, "onItemClick() position " + position);
            onItemClicked(position, KeyEvent.KEYCODE_UNKNOWN, null);
        }
    };

    private final OnItemSelectedListener mOnItemSelectedListener = new OnItemSelectedListener() {

        /**
         * Implements OnItemSelectedListener
         */
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (DBG) Log.d(LOG_TAG, "onItemSelected() position " + position);
            AsusSearchView.this.onItemSelected(position);
        }

        /**
         * Implements OnItemSelectedListener
         */
        public void onNothingSelected(AdapterView<?> parent) {
            if (DBG)
                Log.d(LOG_TAG, "onNothingSelected()");
        }
    };

    /**
     * Query rewriting.
     */
    private void rewriteQueryFromSuggestion(int position) {
        CharSequence oldQuery = mSearchSrcTextView.getText();
        Cursor c = mSuggestionsAdapter.getCursor();
        if (c == null) {
            return;
        }
        if (c.moveToPosition(position)) {
            // Get the new query from the suggestion.
            CharSequence newQuery = mSuggestionsAdapter.convertToString(c);
            if (newQuery != null) {
                // The suggestion rewrites the query.
                // Update the text field, without getting new suggestions.
                setQuery(newQuery);
            } else {
                // The suggestion does not rewrite the query, restore the user's query.
                setQuery(oldQuery);
            }
        } else {
            // We got a bad position, restore the user's query.
            setQuery(oldQuery);
        }
    }

    /**
     * Launches an intent based on a suggestion.
     *
     * @param position The index of the suggestion to create the intent from.
     * @param actionKey The key code of the action key that was pressed,
     *        or {@link KeyEvent#KEYCODE_UNKNOWN} if none.
     * @param actionMsg The message for the action key that was pressed,
     *        or <code>null</code> if none.
     * @return true if a successful launch, false if could not (e.g. bad position).
     */
    private boolean launchSuggestion(int position, int actionKey, String actionMsg) {
        Cursor c = mSuggestionsAdapter.getCursor();
        if ((c != null) && c.moveToPosition(position)) {

            Intent intent = createIntentFromSuggestion(c, actionKey, actionMsg);

            // launch the intent
            launchIntent(intent);

            return true;
        }
        return false;
    }

    /**
     * Launches an intent, including any special intent handling.
     */
    private void launchIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        try {
            // If the intent was created from a suggestion, it will always have an explicit
            // component here.
            getContext().startActivity(intent);
        } catch (RuntimeException ex) {
            Log.e(LOG_TAG, "Failed launch activity: " + intent, ex);
        }
    }

    /**
     * Sets the text in the query box, without updating the suggestions.
     */
    private void setQuery(CharSequence query) {
        mSearchSrcTextView.setText(query, true);
        // Move the cursor to the end
        mSearchSrcTextView.setSelection(TextUtils.isEmpty(query) ? 0 : query.length());
    }

    private void launchQuerySearch(int actionKey, String actionMsg, String query) {
        String action = Intent.ACTION_SEARCH;
        Intent intent = createIntent(action, null, null, query, actionKey, actionMsg);
        getContext().startActivity(intent);
    }

    private Intent createIntent(String action, Uri data, String extraData, String query,
                                int actionKey, String actionMsg) {
        // Now build the Intent
        Intent intent = new Intent(action);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // We need CLEAR_TOP to avoid reusing an old task that has other activities
        // on top of the one we want. We don't want to do this in in-app search though,
        // as it can be destructive to the activity stack.
        if (data != null) {
            intent.setData(data);
        }
        intent.putExtra(SearchManager.USER_QUERY, mUserQuery);
        if (query != null) {
            intent.putExtra(SearchManager.QUERY, query);
        }
        if (extraData != null) {
            intent.putExtra(SearchManager.EXTRA_DATA_KEY, extraData);
        }
        if (mAppSearchData != null) {
            intent.putExtra(SearchManager.APP_DATA, mAppSearchData);
        }
        if (actionKey != KeyEvent.KEYCODE_UNKNOWN) {
            intent.putExtra(SearchManager.ACTION_KEY, actionKey);
            intent.putExtra(SearchManager.ACTION_MSG, actionMsg);
        }
        intent.setComponent(mSearchable.getSearchActivity());
        return intent;
    }

    /**
     * When a particular suggestion has been selected, perform the various lookups required
     * to use the suggestion.  This includes checking the cursor for suggestion-specific data,
     * and/or falling back to the XML for defaults;  It also creates REST style Uri data when
     * the suggestion includes a data id.
     *
     * @param c The suggestions cursor, moved to the row of the user's selection
     * @param actionKey The key code of the action key that was pressed,
     *        or {@link KeyEvent#KEYCODE_UNKNOWN} if none.
     * @param actionMsg The message for the action key that was pressed,
     *        or <code>null</code> if none.
     * @return An intent for the suggestion at the cursor's position.
     */
    private Intent createIntentFromSuggestion(Cursor c, int actionKey, String actionMsg) {
        try {
            // use specific action if supplied, or default action if supplied, or fixed default
            String action = getColumnString(c, SearchManager.SUGGEST_COLUMN_INTENT_ACTION);

            if (action == null) {
                action = mSearchable.getSuggestIntentAction();
            }
            if (action == null) {
                action = Intent.ACTION_SEARCH;
            }

            // use specific data if supplied, or default data if supplied
            String data = getColumnString(c, SearchManager.SUGGEST_COLUMN_INTENT_DATA);
            if (data == null) {
                data = mSearchable.getSuggestIntentData();
            }
            // then, if an ID was provided, append it.
            if (data != null) {
                String id = getColumnString(c, SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID);
                if (id != null) {
                    data = data + "/" + Uri.encode(id);
                }
            }
            Uri dataUri = (data == null) ? null : Uri.parse(data);

            String query = getColumnString(c, SearchManager.SUGGEST_COLUMN_QUERY);
            String extraData = getColumnString(c, SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA);

            return createIntent(action, dataUri, extraData, query, actionKey, actionMsg);
        } catch (RuntimeException e ) {
            int rowNum;
            try {                       // be really paranoid now
                rowNum = c.getPosition();
            } catch (RuntimeException e2 ) {
                rowNum = -1;
            }
            Log.w(LOG_TAG, "Search suggestions cursor at row " + rowNum +
                    " returned exception.", e);
            return null;
        }
    }

    private void forceSuggestionQuery() {
        try {
            Method doBeforeTextChanged = AutoCompleteTextView.class.getDeclaredMethod("doBeforeTextChanged");
            doBeforeTextChanged.setAccessible(true);
            doBeforeTextChanged.invoke(mSearchSrcTextView);

            Method doAfterTextChanged = AutoCompleteTextView.class.getDeclaredMethod("doAfterTextChanged");
            doAfterTextChanged.setAccessible(true);
            doAfterTextChanged.invoke(mSearchSrcTextView);
        }catch (Exception ex) {
            Log.e(LOG_TAG,ex.toString());
        }
    }

    static boolean isLandscapeMode(Context context) {
        return context.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
    }

    /**
     * Callback to watch the text field for empty/non-empty
     */
    private TextWatcher mTextWatcher = new TextWatcher() {

        public void beforeTextChanged(CharSequence s, int start, int before, int after) { }

        public void onTextChanged(CharSequence s, int start,
                                  int before, int after) {
            AsusSearchView.this.onTextChanged(s);
        }

        public void afterTextChanged(Editable s) {
        }
    };

    private static class UpdatableTouchDelegate extends TouchDelegate {
        /**
         * View that should receive forwarded touch events
         */
        private final View mDelegateView;

        /**
         * Bounds in local coordinates of the containing view that should be mapped to the delegate
         * view. This rect is used for initial hit testing.
         */
        private final Rect mTargetBounds;

        /**
         * Bounds in local coordinates of the containing view that are actual bounds of the delegate
         * view. This rect is used for event coordinate mapping.
         */
        private final Rect mActualBounds;

        /**
         * mTargetBounds inflated to include some slop. This rect is to track whether the motion events
         * should be considered to be be within the delegate view.
         */
        private final Rect mSlopBounds;

        private final int mSlop;

        /**
         * True if the delegate had been targeted on a down event (intersected mTargetBounds).
         */
        private boolean mDelegateTargeted;

        public UpdatableTouchDelegate(Rect targetBounds, Rect actualBounds, View delegateView) {
            super(targetBounds, delegateView);
            mSlop = ViewConfiguration.get(delegateView.getContext()).getScaledTouchSlop();
            mTargetBounds = new Rect();
            mSlopBounds = new Rect();
            mActualBounds = new Rect();
            setBounds(targetBounds, actualBounds);
            mDelegateView = delegateView;
        }

        public void setBounds(Rect desiredBounds, Rect actualBounds) {
            mTargetBounds.set(desiredBounds);
            mSlopBounds.set(desiredBounds);
            mSlopBounds.inset(-mSlop, -mSlop);
            mActualBounds.set(actualBounds);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            final int x = (int) event.getX();
            final int y = (int) event.getY();
            boolean sendToDelegate = false;
            boolean hit = true;
            boolean handled = false;

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (mTargetBounds.contains(x, y)) {
                        mDelegateTargeted = true;
                        sendToDelegate = true;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_MOVE:
                    sendToDelegate = mDelegateTargeted;
                    if (sendToDelegate) {
                        if (!mSlopBounds.contains(x, y)) {
                            hit = false;
                        }
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                    sendToDelegate = mDelegateTargeted;
                    mDelegateTargeted = false;
                    break;
            }
            if (sendToDelegate) {
                if (hit && !mActualBounds.contains(x, y)) {
                    // Offset event coordinates to be in the center of the target view since we
                    // are within the targetBounds, but not inside the actual bounds of
                    // mDelegateView
                    event.setLocation(mDelegateView.getWidth() / 2,
                            mDelegateView.getHeight() / 2);
                } else {
                    // Offset event coordinates to the target view coordinates.
                    event.setLocation(x - mActualBounds.left, y - mActualBounds.top);
                }

                handled = mDelegateView.dispatchTouchEvent(event);
            }
            return handled;
        }
    }

    /**
     * Local subclass for AutoCompleteTextView.
     * @hide
     */
    public static class SearchAutoComplete extends AutoCompleteTextView {

        private int mThreshold;
        private AsusSearchView mSearchView;

        public SearchAutoComplete(Context context) {
            super(context);
            mThreshold = getThreshold();
        }

        public SearchAutoComplete(Context context, AttributeSet attrs) {
            super(context, attrs);
            mThreshold = getThreshold();
        }

        public SearchAutoComplete(Context context, AttributeSet attrs, int defStyleAttrs) {
            super(context, attrs, defStyleAttrs);
            mThreshold = getThreshold();
        }

        public SearchAutoComplete(
                Context context, AttributeSet attrs, int defStyleAttrs, int defStyleRes) {
            super(context, attrs, defStyleAttrs, defStyleRes);
            mThreshold = getThreshold();
        }

        @Override
        protected void onFinishInflate() {
            super.onFinishInflate();
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            setMinWidth((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    getSearchViewTextMinWidthDp(), metrics));
        }

        void setSearchView(AsusSearchView searchView) {
            mSearchView = searchView;
        }

        @Override
        public void setThreshold(int threshold) {
            super.setThreshold(threshold);
            mThreshold = threshold;
        }

        /**
         * Returns true if the text field is empty, or contains only whitespace.
         */
        private boolean isEmpty() {
            return TextUtils.getTrimmedLength(getText()) == 0;
        }

        /**
         * We override this method to avoid replacing the query box text when a
         * suggestion is clicked.
         */
        @Override
        protected void replaceText(CharSequence text) {
        }

        /**
         * We override this method to avoid an extra onItemClick being called on
         * the drop-down's OnItemClickListener by
         * {@link AutoCompleteTextView#onKeyUp(int, KeyEvent)} when an item is
         * clicked with the trackball.
         */
        @Override
        public void performCompletion() {
        }

        /**
         * We override this method to be sure and show the soft keyboard if
         * appropriate when the TextView has focus.
         */
        @Override
        public void onWindowFocusChanged(boolean hasWindowFocus) {
            super.onWindowFocusChanged(hasWindowFocus);

            if (hasWindowFocus && mSearchView.hasFocus() && getVisibility() == VISIBLE) {
                InputMethodManager inputManager =
                        getContext().getSystemService(InputMethodManager.class);
                inputManager.showSoftInput(this, 0);
            }
        }

        @Override
        protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
            super.onFocusChanged(focused, direction, previouslyFocusedRect);
            mSearchView.onTextFocusChanged();
        }

        /**
         * We override this method so that we can allow a threshold of zero,
         * which ACTV does not.
         */
        @Override
        public boolean enoughToFilter() {
            return mThreshold <= 0 || super.enoughToFilter();
        }

        @Override
        public boolean onKeyPreIme(int keyCode, KeyEvent event) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                // special case for the back key, we do not even try to send it
                // to the drop down list but instead, consume it immediately
                if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                    KeyEvent.DispatcherState state = getKeyDispatcherState();
                    if (state != null) {
                        state.startTracking(event, this);
                    }
                    return true;
                } else if (event.getAction() == KeyEvent.ACTION_UP) {
                    KeyEvent.DispatcherState state = getKeyDispatcherState();
                    if (state != null) {
                        state.handleUpEvent(event);
                    }
                    if (event.isTracking() && !event.isCanceled()) {
                        mSearchView.clearFocus();
                        mSearchView.setImeVisibility(false);
                        return true;
                    }
                }
            }
            return super.onKeyPreIme(keyCode, event);
        }

        /**
         * Get minimum width of the search view text entry area.
         */
        private int getSearchViewTextMinWidthDp() {
            final Configuration configuration = getResources().getConfiguration();
            final int width = configuration.screenWidthDp;
            final int height = configuration.screenHeightDp;
            final int orientation = configuration.orientation;
            if (width >= 960 && height >= 720
                    && orientation == Configuration.ORIENTATION_LANDSCAPE) {
                return 256;
            } else if (width >= 600 || (width >= 640 && height >= 480)) {
                return 192;
            };
            return 160;
        }
    }
}
