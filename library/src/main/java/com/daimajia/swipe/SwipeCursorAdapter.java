package com.daimajia.swipe;


import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;

import java.util.HashSet;
import java.util.Set;

public abstract class SwipeCursorAdapter extends CursorAdapter {


    public static enum Mode {
        Single, Multiple
    }

    ;

    private Mode mode = Mode.Single;
    public final int INVALID_POSITION = -1;

    private Set<Integer> mOpenPositions = new HashSet<Integer>();
    private int mOpenPosition = INVALID_POSITION;
    private Set<SwipeLayout> mShownLayouts = new HashSet<SwipeLayout>();

    private Context mContext;

    public SwipeCursorAdapter(Context context) {
        super(context, null, false);
        mContext = context;
    }

    /**
     * return the {@link com.daimajia.swipe.SwipeLayout} resource id, int the view item.
     *
     * @param position
     * @return
     */
    public abstract int getSwipeLayoutResourceId(int position);


    /**
     * generate a new view item.
     * Never bind SwipeListener or fill values here, every item has a chance to fill value or bind
     * listeners in fillValues.
     * to fill it in {@code fillValues} method.
     *
     * @param position
     * @param parent
     * @return
     */
    public abstract View generateView(int position, ViewGroup parent);


    /**
     * fill values or bind listeners to the view.
     *
     * @param position
     * @param cursor
     * @param convertView
     */
    public abstract void fillValues(int position, Cursor cursor, View convertView);


    @Override
    public final View newView(Context context, Cursor cursor, ViewGroup parent) {
        return generateView(cursor.getPosition(), parent);
    }

    @Override
    public final void bindView(View convertView, Context context, Cursor cursor) {
        View v = convertView;
        SwipeLayout swipeLayout;
        int position = cursor.getPosition();
        int swipeResourceId = getSwipeLayoutResourceId(position);
        swipeLayout = (SwipeLayout) v.findViewById(swipeResourceId);
        ValueBox valueBox = (ValueBox) swipeLayout.getTag(swipeResourceId);
        if (valueBox == null) {
            OnLayoutListener onLayoutListener = new OnLayoutListener(position);
            SwipeMemory swipeMemory = new SwipeMemory(position);
            swipeLayout.addSwipeListener(swipeMemory);
            swipeLayout.addOnLayoutListener(onLayoutListener);
            swipeLayout.setTag(swipeResourceId, new ValueBox(position, swipeMemory, onLayoutListener));
            mShownLayouts.add(swipeLayout);
        } else {
            valueBox.swipeMemory.setPosition(position);
            valueBox.onLayoutListener.setPosition(position);
            valueBox.position = position;
        }
        fillValues(position, cursor, v);
    }


    /**
     * set open mode
     *
     * @param mode
     */
    public void setMode(Mode mode) {
        // Clear currently set values
        mOpenPositions.clear();
        mOpenPosition = INVALID_POSITION;
        this.mode = mode;
        notifyDataSetChanged();
    }

    public Mode getMode() {
        return mode;
    }

    /**
     * Open and item in the list
     *
     * @param position Position of the item
     */
    public void openItem(int position) {
        if (mode == Mode.Multiple) {
            if (!mOpenPositions.contains(position))
                mOpenPositions.add(position);
        } else {
            mOpenPosition = position;
        }
        notifyDataSetChanged();
    }

    /**
     * Close an item in the list
     *
     * @param position Position of the item
     */
    public void closeItem(int position) {
        if (mode == Mode.Multiple) {
            mOpenPositions.remove(position);
        } else {
            if (mOpenPosition == position)
                mOpenPosition = INVALID_POSITION;
        }
        notifyDataSetChanged();
    }


    public void closeAllItems() {
        closeAllExcept(null);
    }

    public void closeAllExcept(SwipeLayout layout) {
        for (SwipeLayout s : mShownLayouts) {
            if (s != layout)
                s.close();
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

    class OnLayoutListener implements SwipeLayout.OnLayout {

        private int position;

        OnLayoutListener(int position) {
            this.position = position;
        }

        public void setPosition(int position) {
            this.position = position;
        }

        @Override
        public void onLayout(SwipeLayout v) {
            if (mode == Mode.Multiple) {
                if (mOpenPositions.contains(position))
                    v.open(false, false);
                else {
                    v.close(false, false);
                }
            } else {
                if (mOpenPosition == position) {
                    v.open(false, false);
                } else {
                    v.close(false, false);
                }
            }
        }

    }

    class SwipeMemory extends SimpleSwipeListener{

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
