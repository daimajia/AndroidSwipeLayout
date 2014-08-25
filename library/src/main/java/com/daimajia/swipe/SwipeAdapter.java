package com.daimajia.swipe;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.HashSet;
import java.util.Set;

public abstract class SwipeAdapter extends BaseAdapter {

    private Set<Integer> mOpenPositions = new HashSet<Integer>();

    /**
     * return the {@link com.daimajia.swipe.SwipeLayout} resource id, int the view item.
     * @param position
     * @return
     */
    public abstract int getSwipeLayoutResourceId(int position);

    /**
     * generate a new view item, you don't need to fill any value to this view, you have a chance
     * to fill it in {@code fillValues} method.
     * @param position
     * @param parent
     * @return
     */
    public abstract View generateView(int position, ViewGroup parent);

    /**
     * fill values to the view.
     * @param position
     * @param convertView
     */
    public abstract void fillValues(int position, View convertView);


    @Override
    public final View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        SwipeLayout swipeLayout;
        int swipeResourceId = getSwipeLayoutResourceId(position);
        if(v == null){
            v = generateView(position, parent);
            swipeLayout = (SwipeLayout)v.findViewById(swipeResourceId);
            if(swipeLayout != null){
                OnLayoutListener onLayoutListener = new OnLayoutListener(position);
                SwipeMemory swipeMemory = new SwipeMemory(position);
                swipeLayout.addSwipeListener(swipeMemory);
                swipeLayout.addOnLayoutListener(onLayoutListener);
                swipeLayout.setTag(swipeResourceId, new ValueBox(position, swipeMemory, onLayoutListener));
            }
        }else{
            swipeLayout = (SwipeLayout)v.findViewById(swipeResourceId);
            if(swipeLayout != null){
                ValueBox valueBox = (ValueBox)swipeLayout.getTag(swipeResourceId);
                valueBox.swipeMemory.setPosition(position);
                valueBox.onLayoutListener.setPosition(position);
                valueBox.position = position;
            }
        }
        fillValues(position, v);
        return v;
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
            if(mOpenPositions.contains(position))
                v.open(false);
            else{
                v.close(false);
            }
        }
    }

    class SwipeMemory implements SwipeLayout.SwipeListener {

        private int position;

        SwipeMemory(int position) {
            this.position = position;
        }

        @Override
        public void onClose(SwipeLayout layout) {
            mOpenPositions.remove(position);
        }


        @Override
        public void onUpdate(SwipeLayout layout, int leftOffset, int topOffset) {

        }

        @Override
        public void onOpen(SwipeLayout layout) {
            mOpenPositions.add(position);
        }

        @Override
        public void onHandRelease(SwipeLayout layout, float xvel, float yvel) {

        }

        public void setPosition(int position){
            this.position = position;
        }
    }

}
