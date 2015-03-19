package com.daimajia.swipe;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Build;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.FrameLayout;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
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
    private boolean mClickToClose = true;

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
        setClickToClose(a.getBoolean(R.styleable.SwipeLayout_clickToClose, mClickToClose));

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
            boolean result = child == getSurfaceView() || getBottomViews().contains(child);
            if(result){
                isCloseBeforeDrag = getOpenStatus() == Status.Close;
            }
            return result;
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            return mDragDistance;
        }

        @Override
        public int getViewVerticalDragRange(View child) {
            return mDragDistance;
        }

        boolean isCloseBeforeDrag = true;
        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            super.onViewReleased(releasedChild, xvel, yvel);
            for (SwipeListener l : mSwipeListeners)
                l.onHandRelease(SwipeLayout.this, xvel, yvel);
            if (releasedChild == getSurfaceView()) {
                processSurfaceRelease(xvel, yvel, isCloseBeforeDrag);
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
    public void addBottomView(View child, DragEdge dragEdge){
        addBottomView(child, null, dragEdge);
    }
    public void addBottomView(View child, ViewGroup.LayoutParams params, DragEdge dragEdge){
        if(params==null){
            params = generateDefaultLayoutParams();
        }
        if(!checkLayoutParams(params)){
            params = generateLayoutParams(params);
        }
        int gravity = -1;
        switch (dragEdge){
            case Left:gravity = Gravity.LEFT;break;
            case Right:gravity = Gravity.RIGHT;break;
            case Top:gravity = Gravity.TOP;break;
            case Bottom:gravity = Gravity.BOTTOM;break;
        }
        if(params instanceof FrameLayout.LayoutParams){
            ((LayoutParams) params).gravity = gravity;
        }
        super.addView(child, 0, params);
    }
    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        //the child should be viewGroup, convert child here
        if(!(child instanceof  ViewGroup)){
            WrapGroup childContain = new WrapGroup(getContext());
            childContain.addView(child);
            child = childContain;
        }

        int gravity = Gravity.NO_GRAVITY;
        try {
            gravity = (Integer) params.getClass().getField("gravity").get(params);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(gravity>0){
            //declared the layout_gravity, set the child's drag edge
            if(child.getId()==View.NO_ID){
                if(Build.VERSION.SDK_INT<17){
                    child.setId(child.hashCode());
                }else{
                    child.setId(View.generateViewId());
                }
            }
            gravity = GravityCompat.getAbsoluteGravity(gravity, ViewCompat.getLayoutDirection(this));

            if(gravity == Gravity.LEFT){
                mBottomViewIdsSet = true;
                if(!mDragEdges.contains(DragEdge.Left)){
                    mDragEdges.add(DragEdge.Left);
                }
                mBottomViewIdMap.put(DragEdge.Left, child.getId());
            }
            if(gravity == Gravity.RIGHT){
                mBottomViewIdsSet = true;
                if(!mDragEdges.contains(DragEdge.Right)){
                    mDragEdges.add(DragEdge.Right);
                }
                mBottomViewIdMap.put(DragEdge.Right, child.getId());
            }
            if(gravity == Gravity.TOP){
                mBottomViewIdsSet = true;
                if(!mDragEdges.contains(DragEdge.Top)){
                    mDragEdges.add(DragEdge.Top);
                }
                mBottomViewIdMap.put(DragEdge.Top, child.getId());
            }
            if(gravity == Gravity.BOTTOM){
                mBottomViewIdsSet = true;
                if(!mDragEdges.contains(DragEdge.Bottom)){
                    mDragEdges.add(DragEdge.Bottom);
                }
                mBottomViewIdMap.put(DragEdge.Bottom, child.getId());
            }
            populateIndexes();
        }
        super.addView(child, index, params);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int childCount = getChildCount();
        if (childCount != 1 + mDragEdges.size()) {
            throw new IllegalStateException("You need to have one surface view plus one view for each of your drag edges." +
                    " ChildCount:" + childCount +
                    ", mDragEdges.size():"+ mDragEdges.size());
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

    private boolean mIsBeingDragged;
    private void checkCanDrag(MotionEvent ev){
        if(mIsBeingDragged) return;
        if(getOpenStatus()==Status.Middle){
            mIsBeingDragged = true;
            return;
        }
        Status status = getOpenStatus();
        float distanceX = ev.getRawX() - sX;
        float distanceY = ev.getRawY() - sY;
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
        if (!shouldAllowSwipe()) return;

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
        mIsBeingDragged = !doNothing;
    }
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!isSwipeEnabled()) {
            return false;
        }
        if(mClickToClose && getOpenStatus() == Status.Open && getSurfaceView()!=null){
            Rect rect = new Rect();
            getSurfaceView().getHitRect(rect);
            if(rect.contains((int)ev.getX(), (int)ev.getY())){
                return true;
            }
        }
        for (SwipeDenier denier : mSwipeDeniers) {
            if (denier != null && denier.shouldDenySwipe(ev)) {
                return false;
            }
        }

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDragHelper.processTouchEvent(ev);
                mIsBeingDragged = false;
                sX = ev.getRawX();
                sY = ev.getRawY();
                //if the swipe is in middle state(scrolling), should intercept the touch
                if(getOpenStatus() == Status.Middle){
                    mIsBeingDragged = true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                boolean beforeCheck = mIsBeingDragged;
                checkCanDrag(ev);
                if (mIsBeingDragged) {
                    ViewParent parent = getParent();
                    if(parent!=null){
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                }
                if(!beforeCheck && mIsBeingDragged){
                    //let children has one chance to catch the touch, and request the swipe not intercept
                    //useful when swipeLayout wrap a swipeLayout or other gestural layout
                    return false;
                }
                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mIsBeingDragged = false;
                mDragHelper.processTouchEvent(ev);
                break;
            default://handle other action, such as ACTION_POINTER_DOWN/UP
                mDragHelper.processTouchEvent(ev);
        }
        return mIsBeingDragged;
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
        if (!isSwipeEnabled()) return super.onTouchEvent(event);

        int action = event.getActionMasked();
        gestureDetector.onTouchEvent(event);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mDragHelper.processTouchEvent(event);
                sX = event.getRawX();
                sY = event.getRawY();


            case MotionEvent.ACTION_MOVE: {
                //the drag state and the direction are already judged at onInterceptTouchEvent
                checkCanDrag(event);
                if(mIsBeingDragged){
                    getParent().requestDisallowInterceptTouchEvent(true);
                    mDragHelper.processTouchEvent(event);
                }
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsBeingDragged = false;
                mDragHelper.processTouchEvent(event);
                break;

            default://handle other action, such as ACTION_POINTER_DOWN/UP
                mDragHelper.processTouchEvent(event);
        }

        return super.onTouchEvent(event) || mIsBeingDragged || action == MotionEvent.ACTION_DOWN;
    }
    public boolean isClickToClose() {
        return mClickToClose;
    }

    public void setClickToClose(boolean mClickToClose) {
        this.mClickToClose = mClickToClose;
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
        if (t instanceof AdapterView) {
            return (AdapterView) t;
        }
        return null;
    }

    private void performAdapterViewItemClick() {
        if(getOpenStatus()!= Status.Close) return;
        ViewParent t = getParent();
        if (t instanceof AdapterView) {
            AdapterView view = (AdapterView) t;
            int p = view.getPositionForView(SwipeLayout.this);
            if (p != AdapterView.INVALID_POSITION){
                view.performItemClick(view.getChildAt(p - view.getFirstVisiblePosition()), p, view
                        .getAdapter().getItemId(p));
            }
        }
    }
    private boolean performAdapterViewItemLongClick() {
        if(getOpenStatus()!= Status.Close) return false;
        ViewParent t = getParent();
        if (t instanceof AdapterView) {
            AdapterView view = (AdapterView) t;
            int p = view.getPositionForView(SwipeLayout.this);
            if (p == AdapterView.INVALID_POSITION) return false;
            long vId = view.getItemIdAtPosition(p);
            boolean handled = false;
            try {
                Method m = AbsListView.class.getDeclaredMethod("performLongPress", View.class, int.class, long.class);
                m.setAccessible(true);
                handled = (boolean) m.invoke(view, SwipeLayout.this, p, vId);

            } catch (Exception e) {
                e.printStackTrace();

                if (view.getOnItemLongClickListener() != null) {
                    handled = view.getOnItemLongClickListener().onItemLongClick(view, SwipeLayout.this, p, vId);
                }
                if (handled) {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                }
            }
            return handled;
        }
        return false;
    }
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if(insideAdapterView()){
            if(clickListener==null){
                setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        performAdapterViewItemClick();
                    }
                });
            }
            if(longClickListener==null){
                setOnLongClickListener(new OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        performAdapterViewItemLongClick();
                        return true;
                    }
                });
            }
        }
    }
    OnClickListener clickListener;
    @Override
    public void setOnClickListener(OnClickListener l) {
        super.setOnClickListener(l);
        clickListener = l;
    }
    OnLongClickListener longClickListener;
    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        super.setOnLongClickListener(l);
        longClickListener = l;
    }


    private GestureDetector gestureDetector = new GestureDetector(getContext(), new SwipeDetector());

    class SwipeDetector extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if(mClickToClose){
                close();
            }
            return super.onSingleTapUp(e);
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

    /**
     * @return all bottomViews.
     */
    public List<ViewGroup> getBottomViews() {
        List<ViewGroup> lvg = new ArrayList<ViewGroup>();
        // If the user has provided a map for views to
        if (mBottomViewIdsSet) {
            lvg.addAll(Arrays.asList(new ViewGroup[mDragEdges.size()]));

            if (mDragEdges.contains(DragEdge.Left)) {
                lvg.set(mLeftIndex, ((ViewGroup) findViewById(mBottomViewIdMap.get(DragEdge.Left))));
            }
            if (mDragEdges.contains(DragEdge.Top)) {
                lvg.set(mTopIndex, ((ViewGroup) findViewById(mBottomViewIdMap.get(DragEdge.Top))));
            }
            if (mDragEdges.contains(DragEdge.Right)) {
                lvg.set(mRightIndex, ((ViewGroup) findViewById(mBottomViewIdMap.get(DragEdge.Right))));
            }
            if (mDragEdges.contains(DragEdge.Bottom)) {
                lvg.set(mBottomIndex, ((ViewGroup) findViewById(mBottomViewIdMap.get(DragEdge.Bottom))));
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
     * @param xvel xVelocity
     * @param yvel yVelocity
     * @param isCloseBeforeDragged the open state before drag
     */
    private void processSurfaceRelease(float xvel, float yvel, boolean isCloseBeforeDragged) {
        float minVelocity = mDragHelper.getMinVelocity();
        View surfaceView = getSurfaceView();
        DragEdge currentDragEdge = null;
        try {
            currentDragEdge = mDragEdges.get(mCurrentDirectionIndex);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(currentDragEdge == null || surfaceView == null){
            return;
        }
        float willOpenPercent = (isCloseBeforeDragged ? .3f : .7f);
        if(currentDragEdge == DragEdge.Left){
            if(xvel > minVelocity) open();
            else if(xvel < -minVelocity) close();
            else{
                float openPercent = 1f * getSurfaceView().getLeft() / mDragDistance;
                if(openPercent > willOpenPercent ) open();
                else close();
            }
        }else if(currentDragEdge == DragEdge.Right){
            if(xvel > minVelocity) close();
            else if(xvel < -minVelocity) open();
            else{
                float openPercent = 1f * (-getSurfaceView().getLeft()) / mDragDistance;
                if(openPercent > willOpenPercent ) open();
                else close();
            }
        }else if(currentDragEdge == DragEdge.Top){
            if(yvel > minVelocity) open();
            else if(yvel < -minVelocity) close();
            else{
                float openPercent = 1f * getSurfaceView().getTop() / mDragDistance;
                if(openPercent > willOpenPercent ) open();
                else close();
            }
        }else if(currentDragEdge == DragEdge.Bottom){
            if(yvel > minVelocity) close();
            else if(yvel < -minVelocity) open();
            else{
                float openPercent = 1f * (-getSurfaceView().getTop()) / mDragDistance;
                if(openPercent > willOpenPercent ) open();
                else close();
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

    //if child is not viewGroup, this group will wrap it
    public class WrapGroup extends FrameLayout{
        public WrapGroup(Context context) {
            super(context);
        }
    }
}
