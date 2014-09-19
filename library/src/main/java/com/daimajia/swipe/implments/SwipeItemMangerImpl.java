package com.daimajia.swipe.implments;

import android.view.View;
import android.widget.BaseAdapter;

import com.daimajia.swipe.SimpleSwipeListener;
import com.daimajia.swipe.SwipeLayout;
import com.daimajia.swipe.interfaces.SwipeAdapterInterface;
import com.daimajia.swipe.interfaces.SwipeItemMangerInterface;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * SwipeItemMangerImpl is a helper class to help all the adapters to maintain open status.
 */
public class SwipeItemMangerImpl implements SwipeItemMangerInterface {

    private Mode mode = Mode.Single;
    public final int INVALID_POSITION = -1;

    protected int mOpenPosition = INVALID_POSITION;

    protected Set<Integer> mOpenPositions = new HashSet<Integer>();
    protected Set<SwipeLayout> mShownLayouts = new HashSet<SwipeLayout>();

    protected BaseAdapter mAdapter;

    public SwipeItemMangerImpl(BaseAdapter adapter) {
        if(adapter == null)
            throw new IllegalArgumentException("Adapter can not be null");

        if(!(adapter instanceof SwipeItemMangerInterface))
            throw new IllegalArgumentException("adapter should implement the SwipeAdapterInterface");

        this.mAdapter = adapter;
    }

    public enum Mode{
        Single, Multiple
    };

    public Mode getMode(){
        return mode;
    }

    public void setMode(Mode mode){
        this.mode = mode;
        mOpenPositions.clear();
        mShownLayouts.clear();
        mOpenPosition = INVALID_POSITION;
    }

    public void initialize(View target, int position) {
        int resId = getSwipeLayoutId(position);

        OnLayoutListener onLayoutListener = new OnLayoutListener(position);
        SwipeLayout swipeLayout = (SwipeLayout)target.findViewById(resId);
        if(swipeLayout == null)
            throw new IllegalStateException("can not find SwipeLayout in target view");

        SwipeMemory swipeMemory = new SwipeMemory(position);
        swipeLayout.addSwipeListener(swipeMemory);
        swipeLayout.addOnLayoutListener(onLayoutListener);
        swipeLayout.setTag(resId, new ValueBox(position, swipeMemory, onLayoutListener));

        mShownLayouts.add(swipeLayout);
    }

    public void updateConvertView(View target, int position) {
        int resId = getSwipeLayoutId(position);

        SwipeLayout swipeLayout = (SwipeLayout)target.findViewById(resId);
        if(swipeLayout == null)
            throw new IllegalStateException("can not find SwipeLayout in target view");

        ValueBox valueBox = (ValueBox)swipeLayout.getTag(resId);
        valueBox.swipeMemory.setPosition(position);
        valueBox.onLayoutListener.setPosition(position);
        valueBox.position = position;
    }

    private int getSwipeLayoutId(int position){
        return ((SwipeAdapterInterface)(mAdapter)).getSwipeLayoutResourceId(position);
    }

    @Override
    public void openItem(int position) {
        if(mode == Mode.Multiple){
            if(!mOpenPositions.contains(position))
                mOpenPositions.add(position);
        }else{
            mOpenPosition = position;
        }
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void closeItem(int position) {
        if(mode == Mode.Multiple){
            mOpenPositions.remove(position);
        }else{
            if(mOpenPosition == position)
                mOpenPosition = INVALID_POSITION;
        }
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void closeAllExcept(SwipeLayout layout) {
        for(SwipeLayout s : mShownLayouts){
            if(s != layout)
                s.close();
        }
    }

    @Override
    public void removeShownLayouts(SwipeLayout layout) {
        mShownLayouts.remove(layout);
    }

    @Override
    public List<Integer> getOpenItems() {
        if(mode == Mode.Multiple){
            return new ArrayList<Integer>(mOpenPositions);
        }else{
            return Arrays.asList(mOpenPosition);
        }
    }

    @Override
    public List<SwipeLayout> getOpenLayouts() {
        return new ArrayList<SwipeLayout>(mShownLayouts);
    }

    @Override
    public boolean isOpen(int position) {
        if(mode == Mode.Multiple){
            return mOpenPositions.contains(position);
        }else{
            return mOpenPosition == position;
        }
    }

    class ValueBox {
        OnLayoutListener onLayoutListener;
        SwipeMemory swipeMemory;
        int position;

        ValueBox(int position, SwipeMemory swipeMemory, OnLayoutListener onLayoutListener) {
            this.swipeMemory = swipeMemory;
            this.onLayoutListener = onLayoutListener;
            this.position = position;
        }
    }

    class OnLayoutListener implements SwipeLayout.OnLayout{

        private int position;

        OnLayoutListener(int position) {
            this.position = position;
        }

        public void setPosition(int position){
            this.position = position;
        }

        @Override
        public void onLayout(SwipeLayout v) {
            if(isOpen(position)){
                v.open(false, false);
            }else{
                v.close(false, false);
            }
        }

    }

    class SwipeMemory extends SimpleSwipeListener {

        private int position;

        SwipeMemory(int position) {
            this.position = position;
        }

        @Override
        public void onClose(SwipeLayout layout) {
            if(mode == Mode.Multiple){
                mOpenPositions.remove(position);
            }else{
                mOpenPosition = INVALID_POSITION;
            }
        }

        @Override
        public void onStartOpen(SwipeLayout layout) {
            if(mode == Mode.Single) {
                closeAllExcept(layout);
            }
        }

        @Override
        public void onOpen(SwipeLayout layout) {
            if (mode == Mode.Multiple)
                mOpenPositions.add(position);
            else {
                closeAllExcept(layout);
                mOpenPosition = position;
            }
        }

        public void setPosition(int position){
            this.position = position;
        }
    }

}
