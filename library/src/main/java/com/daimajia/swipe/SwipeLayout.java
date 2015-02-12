package com.daimajia.swipe;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ListAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SwipeLayout extends FrameLayout {

    public static final int EMPTY_LAYOUT = -1;

    private static final int DRAG_LEFT = 1;
    private static final int DRAG_RIGHT = 2;
    private static final int DRAG_TOP = 4;
    private static final int DRAG_BOTTOM = 8;

    private int mTouchSlop;

    private int mLeftIndex;
    private int mRightIndex;
    private int mTopIndex;
    private int mBottomIndex;

    private int mCurrentDirectionIndex = 0;
    private ViewDragHelper mDragHelper;

    private int mDragDistance = 0;
    private List<DragEdge> mDragEdges;
    private ShowMode mShowMode;

    private float mLeftEdgeSwipeOffset;
    private float mRightEdgeSwipeOffset;
    private float mTopEdgeSwipeOffset;
    private float mBottomEdgeSwipeOffset;

    private Map<DragEdge, Integer> mBottomViewIdMap = new HashMap<DragEdge, Integer>();
    private boolean mBottomViewIdsSet = false;

    private List<SwipeListener> mSwipeListeners = new ArrayList<SwipeListener>();
    private List<SwipeDenier> mSwipeDeniers = new ArrayList<SwipeDenier>();
    private Map<View, ArrayList<OnRevealListener>> mRevealListeners = new HashMap<View, ArrayList<OnRevealListener>>();
    private Map<View, Boolean> mShowEntirely = new HashMap<View, Boolean>();

    private DoubleClickListener mDoubleClickListener;

    private boolean mSwipeEnabled = true;
    private boolean mLeftSwipeEnabled = true;
    private boolean mRightSwipeEnabled = true;
    private boolean mTopSwipeEnabled = true;
    private boolean mBottomSwipeEnabled = true;

    public static enum DragEdge {
        Left,
        Right,
        Top,
        Bottom;
    }

    ;

    public static enum ShowMode {
        LayDown,
        PullOut
    }

    public SwipeLayout(Context context) {
        this(context, null);
    }

    public SwipeLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SwipeLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mDragHelper = ViewDragHelper.create(this, mDragHelperCallback);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SwipeLayout);
        int dragEdgeChoices = a.getInt(R.styleable.SwipeLayout_drag_edge, DRAG_RIGHT);
        mLeftEdgeSwipeOffset = a.getDimension(R.styleable.SwipeLayout_leftEdgeSwipeOffset, 0);
        mRightEdgeSwipeOffset = a.getDimension(R.styleable.SwipeLayout_rightEdgeSwipeOffset, 0);
        mTopEdgeSwipeOffset = a.getDimension(R.styleable.SwipeLayout_topEdgeSwipeOffset, 0);
        mBottomEdgeSwipeOffset = a.getDimension(R.styleable.SwipeLayout_bottomEdgeSwipeOffset, 0);

        mDragEdges = new ArrayList<DragEdge>();
        if ((dragEdgeChoices & DRAG_LEFT) == DRAG_LEFT) {
            mDragEdges.add(DragEdge.Left);
        }
        if ((dragEdgeChoices & DRAG_RIGHT) == DRAG_RIGHT) {
            mDragEdges.add(DragEdge.Right);
        }
        if ((dragEdgeChoices & DRAG_TOP) == DRAG_TOP) {
            mDragEdges.add(DragEdge.Top);
        }
        if ((dragEdgeChoices & DRAG_BOTTOM) == DRAG_BOTTOM) {
            mDragEdges.add(DragEdge.Bottom);
        }
        populateIndexes();
        int ordinal = a.getInt(R.styleable.SwipeLayout_show_mode, ShowMode.PullOut.ordinal());
        mShowMode = ShowMode.values()[ordinal];
        a.recycle();
    }

    public interface SwipeListener {
        public void onStartOpen(SwipeLayout layout);

        public void onOpen(SwipeLayout layout);

        public void onStartClose(SwipeLayout layout);

        public void onClose(SwipeLayout layout);

        public void onUpdate(SwipeLayout layout, int leftOffset, int topOffset);

        public void onHandRelease(SwipeLayout layout, float xvel, float yvel);
    }

    public void addSwipeListener(SwipeListener l) {
        mSwipeListeners.add(l);
    }

    public void removeSwipeListener(SwipeListener l) {
        mSwipeListeners.remove(l);
    }

    public static interface SwipeDenier {
        /*
         * Called in onInterceptTouchEvent Determines if this swipe event should
         * be denied Implement this interface if you are using views with swipe
         * gestures As a child of SwipeLayout
         * 
         * @return true deny false allow
         */
        public boolean shouldDenySwipe(MotionEvent ev);
    }

    public void addSwipeDenier(SwipeDenier denier) {
        mSwipeDeniers.add(denier);
    }

    public void removeSwipeDenier(SwipeDenier denier) {
        mSwipeDeniers.remove(denier);
    }

    public void removeAllSwipeDeniers() {
        mSwipeDeniers.clear();
    }

    public interface OnRevealListener {
        public void onReveal(View child, DragEdge edge, float fraction, int distance);
    }

    /**
     * bind a view with a specific
     * {@link com.daimajia.swipe.SwipeLayout.OnRevealListener}
     *
     * @param childId the view id.
     * @param l       the target
     *                {@link com.daimajia.swipe.SwipeLayout.OnRevealListener}
     */
    public void addRevealListener(int childId, OnRevealListener l) {
        View child = findViewById(childId);
        if (child == null) {
            throw new IllegalArgumentException("Child does not belong to SwipeListener.");
        }

        if (!mShowEntirely.containsKey(child)) {
            mShowEntirely.put(child, false);
        }
        if (mRevealListeners.get(child) == null)
            mRevealListeners.put(child, new ArrayList<OnRevealListener>());

        mRevealListeners.get(child).add(l);
    }

    /**
     * bind multiple views with an
     * {@link com.daimajia.swipe.SwipeLayout.OnRevealListener}.
     *
     * @param childIds the view id.
     * @param l        the {@link com.daimajia.swipe.SwipeLayout.OnRevealListener}
     */
    public void addRevealListener(int[] childIds, OnRevealListener l) {
        for (int i : childIds)
            addRevealListener(i, l);
    }

    public void removeRevealListener(int childId, OnRevealListener l) {
        View child = findViewById(childId);

        if (child == null) return;

        mShowEntirely.remove(child);
        if (mRevealListeners.containsKey(child)) mRevealListeners.get(child).remove(l);
    }

    public void removeAllRevealListeners(int childId) {
        View child = findViewById(childId);
        if (child != null) {
            mRevealListeners.remove(child);
            mShowEntirely.remove(child);
        }
    }

    private ViewDragHelper.Callback mDragHelperCallback = new ViewDragHelper.Callback() {

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            if (child == getSurfaceView()) {
                switch (mDragEdges.get(mCurrentDirectionIndex)) {
                    case Top:
                    case Bottom:
                        return getPaddingLeft();
                    case Left:
                        if (left < getPaddingLeft()) return getPaddingLeft();
                        if (left > getPaddingLeft() + mDragDistance)
                            return getPaddingLeft() + mDragDistance;
                        break;
                    case Right:
                        if (left > getPaddingLeft()) return getPaddingLeft();
                        if (left < getPaddingLeft() - mDragDistance)
                            return getPaddingLeft() - mDragDistance;
                        break;
                }
            } else if (getBottomViews().get(mCurrentDirectionIndex) == child) {

                switch (mDragEdges.get(mCurrentDirectionIndex)) {
                    case Top:
                    case Bottom:
                        return getPaddingLeft();
                    case Left:
                        if (mShowMode == ShowMode.PullOut) {
                            if (left > getPaddingLeft()) return getPaddingLeft();
                        }
                        break;
                    case Right:
                        if (mShowMode == ShowMode.PullOut) {
                            if (left < getMeasuredWidth() - mDragDistance) {
                                return getMeasuredWidth() - mDragDistance;
                            }
                        }
                        break;
                }
            }
            return left;
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            if (child == getSurfaceView()) {
                switch (mDragEdges.get(mCurrentDirectionIndex)) {
                    case Left:
                    case Right:
                        return getPaddingTop();
                    case Top:
                        if (top < getPaddingTop()) return getPaddingTop();
                        if (top > getPaddingTop() + mDragDistance)
                            return getPaddingTop() + mDragDistance;
                        break;
                    case Bottom:
                        if (top < getPaddingTop() - mDragDistance) {
                            return getPaddingTop() - mDragDistance;
                        }
                        if (top > getPaddingTop()) {
                            return getPaddingTop();
                        }
                }
            } else {
                switch (mDragEdges.get(mCurrentDirectionIndex)) {
                    case Left:
                    case Right:
                        return getPaddingTop();
                    case Top:
                        if (mShowMode == ShowMode.PullOut) {
                            if (top > getPaddingTop()) return getPaddingTop();
                        } else {
                            if (getSurfaceView().getTop() + dy < getPaddingTop())
                                return getPaddingTop();
                            if (getSurfaceView().getTop() + dy > getPaddingTop() + mDragDistance)
                                return getPaddingTop() + mDragDistance;
                        }
                        break;
                    case Bottom:
                        if (mShowMode == ShowMode.PullOut) {
                            if (top < getMeasuredHeight() - mDragDistance)
                                return getMeasuredHeight() - mDragDistance;
                        } else {
                            if (getSurfaceView().getTop() + dy >= getPaddingTop())
                                return getPaddingTop();
                            if (getSurfaceView().getTop() + dy <= getPaddingTop() - mDragDistance)
                                return getPaddingTop() - mDragDistance;
                        }
                }
            }
            return top;
        }

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return child == getSurfaceView() || getBottomViews().contains(child);
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            return mDragDistance;
        }

        @Override
        public int getViewVerticalDragRange(View child) {
            return mDragDistance;
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            super.onViewReleased(releasedChild, xvel, yvel);
            for (SwipeListener l : mSwipeListeners)
                l.onHandRelease(SwipeLayout.this, xvel, yvel);
            if (releasedChild == getSurfaceView()) {
                processSurfaceRelease(xvel, yvel);
            } else if (getBottomViews().contains(releasedChild)) {
                if (getShowMode() == ShowMode.PullOut) {
                    processBottomPullOutRelease(xvel, yvel);
                } else if (getShowMode() == ShowMode.LayDown) {
                    processBottomLayDownMode(xvel, yvel);
                }
            }

            invalidate();
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            int evLeft = getSurfaceView().getLeft(), evRight = getSurfaceView().getRight(), evTop = getSurfaceView()
                    .getTop(), evBottom = getSurfaceView().getBottom();
            if (changedView == getSurfaceView()) {

                if (mShowMode == ShowMode.PullOut) {
                    if (mDragEdges.get(mCurrentDirectionIndex) == DragEdge.Left
                            || mDragEdges.get(mCurrentDirectionIndex) == DragEdge.Right)
                        getBottomViews().get(mCurrentDirectionIndex).offsetLeftAndRight(dx);
                    else getBottomViews().get(mCurrentDirectionIndex).offsetTopAndBottom(dy);
                }

            } else if (getBottomViews().contains(changedView)) {

                if (mShowMode == ShowMode.PullOut) {
                    getSurfaceView().offsetLeftAndRight(dx);
                    getSurfaceView().offsetTopAndBottom(dy);
                } else {
                    Rect rect = computeBottomLayDown(mDragEdges.get(mCurrentDirectionIndex));
                    getBottomViews().get(mCurrentDirectionIndex).layout(rect.left, rect.top, rect.right, rect.bottom);

                    int newLeft = getSurfaceView().getLeft() + dx, newTop = getSurfaceView().getTop() + dy;

                    if (mDragEdges.get(mCurrentDirectionIndex) == DragEdge.Left && newLeft < getPaddingLeft())
                        newLeft = getPaddingLeft();
                    else if (mDragEdges.get(mCurrentDirectionIndex) == DragEdge.Right && newLeft > getPaddingLeft())
                        newLeft = getPaddingLeft();
                    else if (mDragEdges.get(mCurrentDirectionIndex) == DragEdge.Top && newTop < getPaddingTop())
                        newTop = getPaddingTop();
                    else if (mDragEdges.get(mCurrentDirectionIndex) == DragEdge.Bottom && newTop > getPaddingTop())
                        newTop = getPaddingTop();

                    getSurfaceView()
                            .layout(newLeft, newTop, newLeft + getMeasuredWidth(), newTop + getMeasuredHeight());
                }
            }

            dispatchRevealEvent(evLeft, evTop, evRight, evBottom);

            dispatchSwipeEvent(evLeft, evTop, dx, dy);

            invalidate();
        }
    };

    /**
     * the dispatchRevealEvent method may not always get accurate position, it
     * makes the view may not always get the event when the view is totally
     * show( fraction = 1), so , we need to calculate every time.
     *
     * @param child
     * @param relativePosition
     * @param edge
     * @param surfaceLeft
     * @param surfaceTop
     * @param surfaceRight
     * @param surfaceBottom
     * @return
     */
    protected boolean isViewTotallyFirstShowed(View child, Rect relativePosition, DragEdge edge, int surfaceLeft,
                                               int surfaceTop, int surfaceRight, int surfaceBottom) {
        if (mShowEntirely.get(child)) return false;
        int childLeft = relativePosition.left;
        int childRight = relativePosition.right;
        int childTop = relativePosition.top;
        int childBottom = relativePosition.bottom;
        boolean r = false;
        if (getShowMode() == ShowMode.LayDown) {
            if ((edge == DragEdge.Right && surfaceRight <= childLeft)
                    || (edge == DragEdge.Left && surfaceLeft >= childRight)
                    || (edge == DragEdge.Top && surfaceTop >= childBottom)
                    || (edge == DragEdge.Bottom && surfaceBottom <= childTop)) r = true;
        } else if (getShowMode() == ShowMode.PullOut) {
            if ((edge == DragEdge.Right && childRight <= getWidth())
                    || (edge == DragEdge.Left && childLeft >= getPaddingLeft())
                    || (edge == DragEdge.Top && childTop >= getPaddingTop())
                    || (edge == DragEdge.Bottom && childBottom <= getHeight())) r = true;
        }
        return r;
    }

    protected boolean isViewShowing(View child, Rect relativePosition, DragEdge availableEdge, int surfaceLeft,
                                    int surfaceTop, int surfaceRight, int surfaceBottom) {
        int childLeft = relativePosition.left;
        int childRight = relativePosition.right;
        int childTop = relativePosition.top;
        int childBottom = relativePosition.bottom;
        if (getShowMode() == ShowMode.LayDown) {
            switch (availableEdge) {
                case Right:
                    if (surfaceRight > childLeft && surfaceRight <= childRight) {
                        return true;
                    }
                    break;
                case Left:
                    if (surfaceLeft < childRight && surfaceLeft >= childLeft) {
                        return true;
                    }
                    break;
                case Top:
                    if (surfaceTop >= childTop && surfaceTop < childBottom) {
                        return true;
                    }
                    break;
                case Bottom:
                    if (surfaceBottom > childTop && surfaceBottom <= childBottom) {
                        return true;
                    }
                    break;
            }
        } else if (getShowMode() == ShowMode.PullOut) {
            switch (availableEdge) {
                case Right:
                    if (childLeft <= getWidth() && childRight > getWidth()) return true;
                    break;
                case Left:
                    if (childRight >= getPaddingLeft() && childLeft < getPaddingLeft()) return true;
                    break;
                case Top:
                    if (childTop < getPaddingTop() && childBottom >= getPaddingTop()) return true;
                    break;
                case Bottom:
                    if (childTop < getHeight() && childTop >= getPaddingTop()) return true;
                    break;
            }
        }
        return false;
    }

    protected Rect getRelativePosition(View child) {
        View t = child;
        Rect r = new Rect(t.getLeft(), t.getTop(), 0, 0);
        while (t.getParent() != null && t != getRootView()) {
            t = (View) t.getParent();
            if (t == this) break;
            r.left += t.getLeft();
            r.top += t.getTop();
        }
        r.right = r.left + child.getMeasuredWidth();
        r.bottom = r.top + child.getMeasuredHeight();
        return r;
    }

    private int mEventCounter = 0;

    protected void dispatchSwipeEvent(int surfaceLeft, int surfaceTop, int dx, int dy) {
        DragEdge edge = getDragEdge();
        boolean open = true;
        if (edge == DragEdge.Left) {
            if (dx < 0) open = false;
        } else if (edge == DragEdge.Right) {
            if (dx > 0) open = false;
        } else if (edge == DragEdge.Top) {
            if (dy < 0) open = false;
        } else if (edge == DragEdge.Bottom) {
            if (dy > 0) open = false;
        }

        dispatchSwipeEvent(surfaceLeft, surfaceTop, open);
    }

    protected void dispatchSwipeEvent(int surfaceLeft, int surfaceTop, boolean open) {
        safeBottomView();
        Status status = getOpenStatus();

        if (!mSwipeListeners.isEmpty()) {
            mEventCounter++;
            for (SwipeListener l : mSwipeListeners) {
                if (mEventCounter == 1) {
                    if (open) {
                        l.onStartOpen(this);
                    } else {
                        l.onStartClose(this);
                    }
                }
                l.onUpdate(SwipeLayout.this, surfaceLeft - getPaddingLeft(), surfaceTop - getPaddingTop());
            }

            if (status == Status.Close) {
                for (SwipeListener l : mSwipeListeners) {
                    l.onClose(SwipeLayout.this);
                }
                mEventCounter = 0;
            }

            if (status == Status.Open) {
                getBottomViews().get(mCurrentDirectionIndex).setEnabled(true);
                for (SwipeListener l : mSwipeListeners) {
                    l.onOpen(SwipeLayout.this);
                }
                mEventCounter = 0;
            }
        }
    }

    /**
     * prevent bottom view get any touch event. Especially in LayDown mode.
     */
    private void safeBottomView() {
        Status status = getOpenStatus();
        List<ViewGroup> bottoms = getBottomViews();

        if (status == Status.Close) {
            for (ViewGroup bottom : bottoms) {
                if (bottom.getVisibility() != INVISIBLE) bottom.setVisibility(INVISIBLE);
            }
        } else {
            if (bottoms.get(mCurrentDirectionIndex).getVisibility() != VISIBLE)
                bottoms.get(mCurrentDirectionIndex).setVisibility(VISIBLE);
        }
    }

    protected void dispatchRevealEvent(final int surfaceLeft, final int surfaceTop, final int surfaceRight,
                                       final int surfaceBottom) {
        if (mRevealListeners.isEmpty()) return;
        for (Map.Entry<View, ArrayList<OnRevealListener>> entry : mRevealListeners.entrySet()) {
            View child = entry.getKey();
            Rect rect = getRelativePosition(child);
            if (isViewShowing(child, rect, mDragEdges.get(mCurrentDirectionIndex), surfaceLeft, surfaceTop,
                    surfaceRight, surfaceBottom)) {
                mShowEntirely.put(child, false);
                int distance = 0;
                float fraction = 0f;
                if (getShowMode() == ShowMode.LayDown) {
                    switch (mDragEdges.get(mCurrentDirectionIndex)) {
                        case Left:
                            distance = rect.left - surfaceLeft;
                            fraction = distance / (float) child.getWidth();
                            break;
                        case Right:
                            distance = rect.right - surfaceRight;
                            fraction = distance / (float) child.getWidth();
                            break;
                        case Top:
                            distance = rect.top - surfaceTop;
                            fraction = distance / (float) child.getHeight();
                            break;
                        case Bottom:
                            distance = rect.bottom - surfaceBottom;
                            fraction = distance / (float) child.getHeight();
                            break;
                    }
                } else if (getShowMode() == ShowMode.PullOut) {
                    switch (mDragEdges.get(mCurrentDirectionIndex)) {
                        case Left:
                            distance = rect.right - getPaddingLeft();
                            fraction = distance / (float) child.getWidth();
                            break;
                        case Right:
                            distance = rect.left - getWidth();
                            fraction = distance / (float) child.getWidth();
                            break;
                        case Top:
                            distance = rect.bottom - getPaddingTop();
                            fraction = distance / (float) child.getHeight();
                            break;
                        case Bottom:
                            distance = rect.top - getHeight();
                            fraction = distance / (float) child.getHeight();
                            break;
                    }
                }

                for (OnRevealListener l : entry.getValue()) {
                    l.onReveal(child, mDragEdges.get(mCurrentDirectionIndex), Math.abs(fraction), distance);
                    if (Math.abs(fraction) == 1) {
                        mShowEntirely.put(child, true);
                    }
                }
            }

            if (isViewTotallyFirstShowed(child, rect, mDragEdges.get(mCurrentDirectionIndex), surfaceLeft, surfaceTop,
                    surfaceRight, surfaceBottom)) {
                mShowEntirely.put(child, true);
                for (OnRevealListener l : entry.getValue()) {
                    if (mDragEdges.get(mCurrentDirectionIndex) == DragEdge.Left
                            || mDragEdges.get(mCurrentDirectionIndex) == DragEdge.Right)
                        l.onReveal(child, mDragEdges.get(mCurrentDirectionIndex), 1, child.getWidth());
                    else
                        l.onReveal(child, mDragEdges.get(mCurrentDirectionIndex), 1, child.getHeight());
                }
            }

        }
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mDragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    /**
     * {@link android.view.View.OnLayoutChangeListener} added in API 11. I need
     * to support it from API 8.
     */
    public interface OnLayout {
        public void onLayout(SwipeLayout v);
    }

    private List<OnLayout> mOnLayoutListeners;

    public void addOnLayoutListener(OnLayout l) {
        if (mOnLayoutListeners == null) mOnLayoutListeners = new ArrayList<OnLayout>();
        mOnLayoutListeners.add(l);
    }

    public void removeOnLayoutListener(OnLayout l) {
        if (mOnLayoutListeners != null) mOnLayoutListeners.remove(l);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int childCount = getChildCount();
        if (childCount != 1 + mDragEdges.size()) {
            throw new IllegalStateException("You need to have one surface view plus one view for each of your drag edges");
        }
        for (int i = 0; i < childCount; i++) {
            if (!(getChildAt(i) instanceof ViewGroup)) {
                throw new IllegalArgumentException("All the children in SwipeLayout must be an instance of ViewGroup");
            }
        }

        if (mShowMode == ShowMode.PullOut)
            layoutPullOut();
        else if (mShowMode == ShowMode.LayDown) layoutLayDown();

        safeBottomView();

        if (mOnLayoutListeners != null) for (int i = 0; i < mOnLayoutListeners.size(); i++) {
            mOnLayoutListeners.get(i).onLayout(this);
        }

    }

    void layoutPullOut() {
        Rect rect = computeSurfaceLayoutArea(false);
        getSurfaceView().layout(rect.left, rect.top, rect.right, rect.bottom);
        rect = computeBottomLayoutAreaViaSurface(ShowMode.PullOut, rect);
        getBottomViews().get(mCurrentDirectionIndex).layout(rect.left, rect.top, rect.right, rect.bottom);
        bringChildToFront(getSurfaceView());
    }

    void layoutLayDown() {
        Rect rect = computeSurfaceLayoutArea(false);
        getSurfaceView().layout(rect.left, rect.top, rect.right, rect.bottom);
        rect = computeBottomLayoutAreaViaSurface(ShowMode.LayDown, rect);
        getBottomViews().get(mCurrentDirectionIndex).layout(rect.left, rect.top, rect.right, rect.bottom);
        bringChildToFront(getSurfaceView());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (mDragEdges.get(mCurrentDirectionIndex) == DragEdge.Left
                || mDragEdges.get(mCurrentDirectionIndex) == DragEdge.Right)
            mDragDistance = getBottomViews().get(mCurrentDirectionIndex).getMeasuredWidth()
                    - dp2px(getCurrentOffset());
        else mDragDistance = getBottomViews().get(mCurrentDirectionIndex).getMeasuredHeight()
                - dp2px(getCurrentOffset());
    }

    private boolean mTouchConsumedByChild = false;

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        if (!isEnabled() || !isEnabledInAdapterView()) {
            return true;
        }

        if (!isSwipeEnabled()) {
            return false;
        }

        for (SwipeDenier denier : mSwipeDeniers) {
            if (denier != null && denier.shouldDenySwipe(ev)) {
                return false;
            }
        }
        //
        // if a child wants to handle the touch event,
        // then let it do it.
        //
        int action = ev.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                Status status = getOpenStatus();
                if (status == Status.Close) {
                    mTouchConsumedByChild = childNeedHandleTouchEvent(getSurfaceView(), ev) != null;
                } else if (status == Status.Open) {
                    mTouchConsumedByChild = childNeedHandleTouchEvent(getBottomViews().get(mCurrentDirectionIndex), ev) != null;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mTouchConsumedByChild = false;
        }

        if (mTouchConsumedByChild) return false;
        return mDragHelper.shouldInterceptTouchEvent(ev);
    }

    /**
     * if the ViewGroup children want to handle this event.
     *
     * @param v
     * @param event
     * @return
     */
    private View childNeedHandleTouchEvent(ViewGroup v, MotionEvent event) {
        if (v == null) return null;
        if (v.onTouchEvent(event)) return v;

        int childCount = v.getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            View child = v.getChildAt(i);
            if (child instanceof ViewGroup) {
                View grandChild = childNeedHandleTouchEvent((ViewGroup) child, event);
                if (grandChild != null) return grandChild;
            } else {
                if (childNeedHandleTouchEvent(v.getChildAt(i), event)) return v.getChildAt(i);
            }
        }
        return null;
    }

    /**
     * if the view (v) wants to handle this event.
     *
     * @param v
     * @param event
     * @return
     */
    private boolean childNeedHandleTouchEvent(View v, MotionEvent event) {
        if (v == null) return false;

        int[] loc = new int[2];
        v.getLocationOnScreen(loc);
        int left = loc[0], top = loc[1];

        if (event.getRawX() > left && event.getRawX() < left + v.getWidth() && event.getRawY() > top
                && event.getRawY() < top + v.getHeight()) {
            return v.onTouchEvent(event);
        }

        return false;
    }

    private float sX = -1, sY = -1;

    private boolean shouldAllowSwipe() {
        if (mCurrentDirectionIndex == mLeftIndex && !mLeftSwipeEnabled) return false;
        if (mCurrentDirectionIndex == mRightIndex && !mRightSwipeEnabled) return false;
        if (mCurrentDirectionIndex == mTopIndex && !mTopSwipeEnabled) return false;
        if (mCurrentDirectionIndex == mBottomIndex && !mBottomSwipeEnabled) return false;
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabledInAdapterView() || !isEnabled()) return true;

        if (!isSwipeEnabled()) return super.onTouchEvent(event);

        int action = event.getActionMasked();
        ViewParent parent = getParent();

        gestureDetector.onTouchEvent(event);
        Status status = getOpenStatus();
        ViewGroup touching = null;
        if (status == Status.Close) {
            touching = getSurfaceView();
        } else if (status == Status.Open) {
            touching = getBottomViews().get(mCurrentDirectionIndex);
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mDragHelper.processTouchEvent(event);
                parent.requestDisallowInterceptTouchEvent(true);

                sX = event.getRawX();
                sY = event.getRawY();

                if (touching != null) touching.setPressed(true);
                return true;
            case MotionEvent.ACTION_MOVE: {
                float distanceX = event.getRawX() - sX;
                float distanceY = event.getRawY() - sY;
                float angle = Math.abs(distanceY / distanceX);
                angle = (float) Math.toDegrees(Math.atan(angle));
                if (getOpenStatus() == Status.Close) {
                    int lastCurrentDirectionIndex = mCurrentDirectionIndex;
                    if (angle < 45) {
                        if (mLeftIndex != -1 && distanceX > 0 && isLeftSwipeEnabled()) {
                            mCurrentDirectionIndex = mLeftIndex;
                        } else if (mRightIndex != -1 && distanceX < 0 && isRightSwipeEnabled()) {
                            mCurrentDirectionIndex = mRightIndex;
                        }
                    } else {
                        if (mTopIndex != -1 && distanceY > 0 && isTopSwipeEnabled()) {
                            mCurrentDirectionIndex = mTopIndex;
                        } else if (mBottomIndex != -1 && distanceY < 0 && isBottomSwipeEnabled()) {
                            mCurrentDirectionIndex = mBottomIndex;
                        }
                    }
                    if (lastCurrentDirectionIndex != mCurrentDirectionIndex) {
                        updateBottomViews();
                    }
                }
                if (!shouldAllowSwipe()) return super.onTouchEvent(event);

                boolean doNothing = false;
                if (mDragEdges.get(mCurrentDirectionIndex) == DragEdge.Right) {
                    boolean suitable = (status == Status.Open && distanceX > mTouchSlop)
                            || (status == Status.Close && distanceX < -mTouchSlop);
                    suitable = suitable || (status == Status.Middle);

                    if (angle > 30 || !suitable) {
                        doNothing = true;
                    }
                }

                if (mDragEdges.get(mCurrentDirectionIndex) == DragEdge.Left) {
                    boolean suitable = (status == Status.Open && distanceX < -mTouchSlop)
                            || (status == Status.Close && distanceX > mTouchSlop);
                    suitable = suitable || status == Status.Middle;

                    if (angle > 30 || !suitable) {
                        doNothing = true;
                    }
                }

                if (mDragEdges.get(mCurrentDirectionIndex) == DragEdge.Top) {
                    boolean suitable = (status == Status.Open && distanceY < -mTouchSlop)
                            || (status == Status.Close && distanceY > mTouchSlop);
                    suitable = suitable || status == Status.Middle;

                    if (angle < 60 || !suitable) {
                        doNothing = true;
                    }
                }

                if (mDragEdges.get(mCurrentDirectionIndex) == DragEdge.Bottom) {
                    boolean suitable = (status == Status.Open && distanceY > mTouchSlop)
                            || (status == Status.Close && distanceY < -mTouchSlop);
                    suitable = suitable || status == Status.Middle;

                    if (angle < 60 || !suitable) {
                        doNothing = true;
                    }
                }

                if (doNothing) {
                    parent.requestDisallowInterceptTouchEvent(false);
                    return false;
                } else {
                    if (touching != null) {
                        touching.setPressed(false);
                    }
                    parent.requestDisallowInterceptTouchEvent(true);
                    mDragHelper.processTouchEvent(event);
                }
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                sX = -1;
                sY = -1;
                if (touching != null) {
                    touching.setPressed(false);
                }
            }
            default:
                parent.requestDisallowInterceptTouchEvent(true);
                mDragHelper.processTouchEvent(event);
        }

        return true;
    }

    /**
     * if working in {@link android.widget.AdapterView}, we should response
     * {@link android.widget.Adapter} isEnable(int position).
     *
     * @return true when item is enabled, else disabled.
     */
    private boolean isEnabledInAdapterView() {
        AdapterView adapterView = getAdapterView();
        boolean enable = true;
        if (adapterView != null) {
            Adapter adapter = adapterView.getAdapter();
            if (adapter != null) {
                int p = adapterView.getPositionForView(SwipeLayout.this);
                if (adapter instanceof BaseAdapter) {
                    enable = ((BaseAdapter) adapter).isEnabled(p);
                } else if (adapter instanceof ListAdapter) {
                    enable = ((ListAdapter) adapter).isEnabled(p);
                }
            }
        }
        return enable;
    }

    public void setSwipeEnabled(boolean enabled) {
        mSwipeEnabled = enabled;
    }

    public boolean isSwipeEnabled() {
        return mSwipeEnabled;
    }

    public boolean isLeftSwipeEnabled() {
        return mLeftSwipeEnabled;
    }

    public void setLeftSwipeEnabled(boolean leftSwipeEnabled) {
        this.mLeftSwipeEnabled = leftSwipeEnabled;
    }

    public boolean isRightSwipeEnabled() {
        return mRightSwipeEnabled;
    }

    public void setRightSwipeEnabled(boolean rightSwipeEnabled) {
        this.mRightSwipeEnabled = rightSwipeEnabled;
    }

    public boolean isTopSwipeEnabled() {
        return mTopSwipeEnabled;
    }

    public void setTopSwipeEnabled(boolean topSwipeEnabled) {
        this.mTopSwipeEnabled = topSwipeEnabled;
    }

    public boolean isBottomSwipeEnabled() {
        return mBottomSwipeEnabled;
    }

    public void setBottomSwipeEnabled(boolean bottomSwipeEnabled) {
        this.mBottomSwipeEnabled = bottomSwipeEnabled;
    }

    private boolean insideAdapterView() {
        return getAdapterView() != null;
    }

    private AdapterView getAdapterView() {
        ViewParent t = getParent();
        while (t != null) {
            if (t instanceof AdapterView) {
                return (AdapterView) t;
            }
            t = t.getParent();
        }
        return null;
    }

    private void performAdapterViewItemClick(MotionEvent e) {
        ViewParent t = getParent();
        while (t != null) {
            if (t instanceof AdapterView) {
                AdapterView view = (AdapterView) t;
                int p = view.getPositionForView(SwipeLayout.this);
                if (p != AdapterView.INVALID_POSITION
                        && view.performItemClick(view.getChildAt(p - view.getFirstVisiblePosition()), p, view
                        .getAdapter().getItemId(p))) return;
            } else {
                if (t instanceof View && ((View) t).performClick()) return;
            }
            t = t.getParent();
        }
    }

    private GestureDetector gestureDetector = new GestureDetector(getContext(), new SwipeDetector());

    class SwipeDetector extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        /**
         * Simulate the touch event lifecycle. If you use SwipeLayout in
         * {@link android.widget.AdapterView} ({@link android.widget.ListView},
         * {@link android.widget.GridView} etc.) It will manually call
         * {@link android.widget.AdapterView}.performItemClick,
         * performItemLongClick.
         *
         * @param e
         * @return
         */
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if (mDoubleClickListener == null) {
                performAdapterViewItemClick(e);
            }
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (mDoubleClickListener != null) {
                performAdapterViewItemClick(e);
            }
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            performLongClick();
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (mDoubleClickListener != null) {
                View target;
                ViewGroup bottom = getBottomViews().get(mCurrentDirectionIndex);
                ViewGroup surface = getSurfaceView();
                if (e.getX() > bottom.getLeft() && e.getX() < bottom.getRight() && e.getY() > bottom.getTop()
                        && e.getY() < bottom.getBottom()) {
                    target = bottom;
                } else {
                    target = surface;
                }
                mDoubleClickListener.onDoubleClick(SwipeLayout.this, target == surface);
            }
            return true;
        }
    }

    public void setDragEdge(DragEdge dragEdge) {
        mDragEdges = new ArrayList<DragEdge>();
        mDragEdges.add(dragEdge);
        mCurrentDirectionIndex = 0;
        populateIndexes();
        requestLayout();
        updateBottomViews();
    }

    /**
     * set the drag distance, it will force set the bottom view's width or
     * height via this value.
     *
     * @param max
     */
    public void setDragDistance(int max) {
        if (max < 0) throw new IllegalArgumentException("Drag distance can not be < 0");
        mDragDistance = dp2px(max);
        requestLayout();
    }

    /**
     * There are 2 diffirent show mode.
     * {@link com.daimajia.swipe.SwipeLayout.ShowMode}.PullOut and
     * {@link com.daimajia.swipe.SwipeLayout.ShowMode}.LayDown.
     *
     * @param mode
     */
    public void setShowMode(ShowMode mode) {
        mShowMode = mode;
        requestLayout();
    }

    public DragEdge getDragEdge() {
        return mDragEdges.get(mCurrentDirectionIndex);
    }

    public int getDragDistance() {
        return mDragDistance;
    }

    public ShowMode getShowMode() {
        return mShowMode;
    }

    public ViewGroup getSurfaceView() {
        return (ViewGroup) getChildAt(getChildCount() - 1);
    }

    public List<ViewGroup> getBottomViews() {
        List<ViewGroup> lvg = new ArrayList<ViewGroup>();
        // If the user has provided a map for views to
        if (mBottomViewIdsSet) {
            if (mDragEdges.contains(DragEdge.Left)) {
                lvg.add(mLeftIndex, ((ViewGroup) findViewById(mBottomViewIdMap.get(DragEdge.Left))));
            }
            if (mDragEdges.contains(DragEdge.Right)) {
                lvg.add(mRightIndex, ((ViewGroup) findViewById(mBottomViewIdMap.get(DragEdge.Right))));
            }
            if (mDragEdges.contains(DragEdge.Top)) {
                lvg.add(mTopIndex, ((ViewGroup) findViewById(mBottomViewIdMap.get(DragEdge.Top))));
            }
            if (mDragEdges.contains(DragEdge.Bottom)) {
                lvg.add(mBottomIndex, ((ViewGroup) findViewById(mBottomViewIdMap.get(DragEdge.Bottom))));
            }
        }
        // Default behaviour is to simply use the first n-1 children in the order they're listed in the layout
        // and return them in
        else {
            for (int i = 0; i < (getChildCount() - 1); i++) {
                lvg.add((ViewGroup) getChildAt(i));
            }
        }
        return lvg;
    }

    // Pass the id of the view if set, otherwise pass -1
    public void setBottomViewIds(int left, int right, int top, int bottom) {
        if (mDragEdges.contains(DragEdge.Left)) {
            if (left == EMPTY_LAYOUT) {
                mBottomViewIdsSet = false;
            } else {
                mBottomViewIdMap.put(DragEdge.Left, left);
                mBottomViewIdsSet = true;
            }
        }
        if (mDragEdges.contains(DragEdge.Right)) {
            if (right == EMPTY_LAYOUT) {
                mBottomViewIdsSet = false;
            } else {
                mBottomViewIdMap.put(DragEdge.Right, right);
                mBottomViewIdsSet = true;
            }
        }
        if (mDragEdges.contains(DragEdge.Top)) {
            if (top == EMPTY_LAYOUT) {
                mBottomViewIdsSet = false;
            } else {
                mBottomViewIdMap.put(DragEdge.Top, top);
                mBottomViewIdsSet = true;
            }
        }
        if (mDragEdges.contains(DragEdge.Bottom)) {
            if (bottom == EMPTY_LAYOUT) {
                mBottomViewIdsSet = false;
            } else {
                mBottomViewIdMap.put(DragEdge.Bottom, bottom);
                mBottomViewIdsSet = true;
            }
        }
    }

    public enum Status {
        Middle,
        Open,
        Close
    }

    /**
     * get the open status.
     *
     * @return {@link com.daimajia.swipe.SwipeLayout.Status} Open , Close or
     * Middle.
     */
    public Status getOpenStatus() {
        int surfaceLeft = getSurfaceView().getLeft();
        int surfaceTop = getSurfaceView().getTop();
        if (surfaceLeft == getPaddingLeft() && surfaceTop == getPaddingTop()) return Status.Close;

        if (surfaceLeft == (getPaddingLeft() - mDragDistance) || surfaceLeft == (getPaddingLeft() + mDragDistance)
                || surfaceTop == (getPaddingTop() - mDragDistance) || surfaceTop == (getPaddingTop() + mDragDistance))
            return Status.Open;

        return Status.Middle;
    }

    /**
     * Process the surface release event.
     *
     * @param xvel
     * @param yvel
     */
    private void processSurfaceRelease(float xvel, float yvel) {
        if (xvel == 0 && getOpenStatus() == Status.Middle) close();

        if (mDragEdges.get(mCurrentDirectionIndex) == DragEdge.Left
                || mDragEdges.get(mCurrentDirectionIndex) == DragEdge.Right) {
            if (xvel > 0) {
                if (mDragEdges.get(mCurrentDirectionIndex) == DragEdge.Left)
                    open();
                else close();
            }
            if (xvel < 0) {
                if (mDragEdges.get(mCurrentDirectionIndex) == DragEdge.Left)
                    close();
                else open();
            }
        } else {
            if (yvel > 0) {
                if (mDragEdges.get(mCurrentDirectionIndex) == DragEdge.Top)
                    open();
                else close();
            }
            if (yvel < 0) {
                if (mDragEdges.get(mCurrentDirectionIndex) == DragEdge.Top)
                    close();
                else open();
            }
        }
    }

    /**
     * process bottom (PullOut mode) hand release event.
     *
     * @param xvel
     * @param yvel
     */
    private void processBottomPullOutRelease(float xvel, float yvel) {

        if (xvel == 0 && getOpenStatus() == Status.Middle) close();

        if (mDragEdges.get(mCurrentDirectionIndex) == DragEdge.Left
                || mDragEdges.get(mCurrentDirectionIndex) == DragEdge.Right) {
            if (xvel > 0) {
                if (mDragEdges.get(mCurrentDirectionIndex) == DragEdge.Left)
                    open();
                else close();
            }
            if (xvel < 0) {
                if (mDragEdges.get(mCurrentDirectionIndex) == DragEdge.Left)
                    close();
                else open();
            }
        } else {
            if (yvel > 0) {
                if (mDragEdges.get(mCurrentDirectionIndex) == DragEdge.Top)
                    open();
                else close();
            }

            if (yvel < 0) {
                if (mDragEdges.get(mCurrentDirectionIndex) == DragEdge.Top)
                    close();
                else open();
            }
        }
    }

    /**
     * process bottom (LayDown mode) hand release event.
     *
     * @param xvel
     * @param yvel
     */
    private void processBottomLayDownMode(float xvel, float yvel) {

        if (xvel == 0 && getOpenStatus() == Status.Middle) close();

        int l = getPaddingLeft(), t = getPaddingTop();

        if (xvel < 0 && mDragEdges.get(mCurrentDirectionIndex) == DragEdge.Right)
            l -= mDragDistance;
        if (xvel > 0 && mDragEdges.get(mCurrentDirectionIndex) == DragEdge.Left) l += mDragDistance;

        if (yvel > 0 && mDragEdges.get(mCurrentDirectionIndex) == DragEdge.Top) t += mDragDistance;
        if (yvel < 0 && mDragEdges.get(mCurrentDirectionIndex) == DragEdge.Bottom)
            t -= mDragDistance;

        mDragHelper.smoothSlideViewTo(getSurfaceView(), l, t);
        invalidate();
    }

    /**
     * smoothly open surface.
     */
    public void open() {
        open(true, true);
    }

    public void open(boolean smooth) {
        open(smooth, true);
    }

    public void open(boolean smooth, boolean notify) {
        ViewGroup surface = getSurfaceView(), bottom = getBottomViews().get(mCurrentDirectionIndex);
        int dx, dy;
        Rect rect = computeSurfaceLayoutArea(true);
        if (smooth) {
            mDragHelper.smoothSlideViewTo(getSurfaceView(), rect.left, rect.top);
        } else {
            dx = rect.left - surface.getLeft();
            dy = rect.top - surface.getTop();
            surface.layout(rect.left, rect.top, rect.right, rect.bottom);
            if (getShowMode() == ShowMode.PullOut) {
                Rect bRect = computeBottomLayoutAreaViaSurface(ShowMode.PullOut, rect);
                bottom.layout(bRect.left, bRect.top, bRect.right, bRect.bottom);
            }
            if (notify) {
                dispatchRevealEvent(rect.left, rect.top, rect.right, rect.bottom);
                dispatchSwipeEvent(rect.left, rect.top, dx, dy);
            } else {
                safeBottomView();
            }
        }
        invalidate();
    }

    public void open(DragEdge edge) {
        switch (edge) {
            case Left:
                mCurrentDirectionIndex = mLeftIndex;
            case Right:
                mCurrentDirectionIndex = mRightIndex;
            case Top:
                mCurrentDirectionIndex = mTopIndex;
            case Bottom:
                mCurrentDirectionIndex = mBottomIndex;
        }
        open(true, true);
    }

    public void open(boolean smooth, DragEdge edge) {
        switch (edge) {
            case Left:
                mCurrentDirectionIndex = mLeftIndex;
            case Right:
                mCurrentDirectionIndex = mRightIndex;
            case Top:
                mCurrentDirectionIndex = mTopIndex;
            case Bottom:
                mCurrentDirectionIndex = mBottomIndex;
        }
        open(smooth, true);
    }

    public void open(boolean smooth, boolean notify, DragEdge edge) {
        switch (edge) {
            case Left:
                mCurrentDirectionIndex = mLeftIndex;
            case Right:
                mCurrentDirectionIndex = mRightIndex;
            case Top:
                mCurrentDirectionIndex = mTopIndex;
            case Bottom:
                mCurrentDirectionIndex = mBottomIndex;
        }
        open(smooth, notify);
    }

    /**
     * smoothly close surface.
     */
    public void close() {
        close(true, true);
    }

    public void close(boolean smooth) {
        close(smooth, true);
    }

    /**
     * close surface
     *
     * @param smooth smoothly or not.
     * @param notify if notify all the listeners.
     */
    public void close(boolean smooth, boolean notify) {
        ViewGroup surface = getSurfaceView();
        int dx, dy;
        if (smooth)
            mDragHelper.smoothSlideViewTo(getSurfaceView(), getPaddingLeft(), getPaddingTop());
        else {
            Rect rect = computeSurfaceLayoutArea(false);
            dx = rect.left - surface.getLeft();
            dy = rect.top - surface.getTop();
            surface.layout(rect.left, rect.top, rect.right, rect.bottom);
            if (notify) {
                dispatchRevealEvent(rect.left, rect.top, rect.right, rect.bottom);
                dispatchSwipeEvent(rect.left, rect.top, dx, dy);
            } else {
                safeBottomView();
            }
        }
        invalidate();
    }

    public void toggle() {
        toggle(true);
    }

    public void toggle(boolean smooth) {
        if (getOpenStatus() == Status.Open)
            close(smooth);
        else if (getOpenStatus() == Status.Close) open(smooth);
    }

    /**
     * a helper function to compute the Rect area that surface will hold in.
     *
     * @param open open status or close status.
     * @return
     */
    private Rect computeSurfaceLayoutArea(boolean open) {
        int l = getPaddingLeft(), t = getPaddingTop();
        if (open) {
            if (mDragEdges.get(mCurrentDirectionIndex) == DragEdge.Left)
                l = getPaddingLeft() + mDragDistance;
            else if (mDragEdges.get(mCurrentDirectionIndex) == DragEdge.Right)
                l = getPaddingLeft() - mDragDistance;
            else if (mDragEdges.get(mCurrentDirectionIndex) == DragEdge.Top)
                t = getPaddingTop() + mDragDistance;
            else t = getPaddingTop() - mDragDistance;
        }
        return new Rect(l, t, l + getMeasuredWidth(), t + getMeasuredHeight());
    }

    private Rect computeBottomLayoutAreaViaSurface(ShowMode mode, Rect surfaceArea) {
        Rect rect = surfaceArea;

        int bl = rect.left, bt = rect.top, br = rect.right, bb = rect.bottom;
        if (mode == ShowMode.PullOut) {
            if (mDragEdges.get(mCurrentDirectionIndex) == DragEdge.Left)
                bl = rect.left - mDragDistance;
            else if (mDragEdges.get(mCurrentDirectionIndex) == DragEdge.Right)
                bl = rect.right;
            else if (mDragEdges.get(mCurrentDirectionIndex) == DragEdge.Top)
                bt = rect.top - mDragDistance;
            else bt = rect.bottom;

            if (mDragEdges.get(mCurrentDirectionIndex) == DragEdge.Left || mDragEdges.get(mCurrentDirectionIndex) == DragEdge.Right) {
                bb = rect.bottom;
                br = bl + getBottomViews().get(mCurrentDirectionIndex).getMeasuredWidth();
            } else {
                bb = bt + getBottomViews().get(mCurrentDirectionIndex).getMeasuredHeight();
                br = rect.right;
            }
        } else if (mode == ShowMode.LayDown) {
            if (mDragEdges.get(mCurrentDirectionIndex) == DragEdge.Left)
                br = bl + mDragDistance;
            else if (mDragEdges.get(mCurrentDirectionIndex) == DragEdge.Right)
                bl = br - mDragDistance;
            else if (mDragEdges.get(mCurrentDirectionIndex) == DragEdge.Top)
                bb = bt + mDragDistance;
            else bt = bb - mDragDistance;

        }
        return new Rect(bl, bt, br, bb);

    }

    private Rect computeBottomLayDown(DragEdge dragEdge) {
        int bl = getPaddingLeft(), bt = getPaddingTop();
        int br, bb;
        if (dragEdge == DragEdge.Right) {
            bl = getMeasuredWidth() - mDragDistance;
        } else if (dragEdge == DragEdge.Bottom) {
            bt = getMeasuredHeight() - mDragDistance;
        }
        if (dragEdge == DragEdge.Left || dragEdge == DragEdge.Right) {
            br = bl + mDragDistance;
            bb = bt + getMeasuredHeight();
        } else {
            br = bl + getMeasuredWidth();
            bb = bt + mDragDistance;
        }
        return new Rect(bl, bt, br, bb);
    }

    public void setOnDoubleClickListener(DoubleClickListener doubleClickListener) {
        mDoubleClickListener = doubleClickListener;
    }

    public interface DoubleClickListener {
        public void onDoubleClick(SwipeLayout layout, boolean surface);
    }

    private int dp2px(float dp) {
        return (int) (dp * getContext().getResources().getDisplayMetrics().density + 0.5f);
    }

    public List<DragEdge> getDragEdges() {
        return mDragEdges;
    }

    public void setDragEdges(List<DragEdge> mDragEdges) {
        this.mDragEdges = mDragEdges;
        mCurrentDirectionIndex = 0;
        populateIndexes();
        updateBottomViews();
    }

    public void setDragEdges(DragEdge... mDragEdges) {
        this.mDragEdges = new ArrayList<DragEdge>();
        for (DragEdge e : mDragEdges) {
            this.mDragEdges.add(e);
        }
        mCurrentDirectionIndex = 0;
        populateIndexes();
        updateBottomViews();
    }

    private void populateIndexes() {
        mLeftIndex = this.mDragEdges.indexOf(DragEdge.Left);
        mRightIndex = this.mDragEdges.indexOf(DragEdge.Right);
        mTopIndex = this.mDragEdges.indexOf(DragEdge.Top);
        mBottomIndex = this.mDragEdges.indexOf(DragEdge.Bottom);
    }

    private float getCurrentOffset() {
        if (mDragEdges.get(mCurrentDirectionIndex) == DragEdge.Left) return mLeftEdgeSwipeOffset;
        else if (mDragEdges.get(mCurrentDirectionIndex) == DragEdge.Right)
            return mRightEdgeSwipeOffset;
        else if (mDragEdges.get(mCurrentDirectionIndex) == DragEdge.Top) return mTopEdgeSwipeOffset;
        else return mBottomEdgeSwipeOffset;
    }

    private void updateBottomViews() {
//        removeAllViews();
//        addView(getBottomViews().get(mCurrentDirectionIndex));
//        addView(getSurfaceView());
//        getBottomViews().get(mCurrentDirectionIndex).bringToFront();
//        getSurfaceView().bringToFront();
        if (mShowMode == ShowMode.PullOut)
            layoutPullOut();
        else if (mShowMode == ShowMode.LayDown) layoutLayDown();

        safeBottomView();

        if (mOnLayoutListeners != null) for (int i = 0; i < mOnLayoutListeners.size(); i++) {
            mOnLayoutListeners.get(i).onLayout(this);
        }
    }
}
