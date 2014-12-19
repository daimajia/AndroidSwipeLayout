package com.daimajia.swipe.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.daimajia.swipe.SwipeLayout;
import com.daimajia.swipe.implments.SwipeItemRecyclerMangerImpl;
import com.daimajia.swipe.interfaces.SwipeAdapterInterface;
import com.daimajia.swipe.interfaces.SwipeItemMangerInterface;
import com.daimajia.swipe.util.Attributes;

import java.util.ArrayList;
import java.util.List;

public abstract class RecyclerSwipeAdapter extends RecyclerView.Adapter<RecyclerSwipeAdapter.ViewHolder> implements SwipeItemMangerInterface,SwipeAdapterInterface {

    private SwipeItemRecyclerMangerImpl mItemManger = new SwipeItemRecyclerMangerImpl(this);

    /**
     * This must be over-ridden
     * Containes the view of the swiped item in the recycler.view
     */
    public static abstract class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View itemView) {
            super(itemView);
        }
    };

    private Context mContext;
    private ArrayList mDataset;

    public RecyclerSwipeAdapter(Context context, ArrayList objects) {
        this.mContext = context;
        this.mDataset = objects;
    }

    /**
     * @param parent
     * @param viewType
     * @return View
     */
    public abstract ViewHolder createRecyclerViewHolder(ViewGroup parent, int viewType);

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return createRecyclerViewHolder(parent, viewType);
    }

    /**
     * @param viewHolder
     * @param position
     */
    public abstract void bindRecyclerViewHolder(ViewHolder viewHolder, int position);

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, final int position) {
        bindRecyclerViewHolder(viewHolder, position);
    }

    @Override
    public void openItem(int position) {
        mItemManger.openItem(position);
    }

    @Override
    public void closeItem(int position) {
        mItemManger.closeItem(position);
    }

    @Override
    public void closeAllExcept(SwipeLayout layout) {
        mItemManger.closeAllExcept(layout);
    }

    @Override
    public List<Integer> getOpenItems() {
        return mItemManger.getOpenItems();
    }

    @Override
    public List<SwipeLayout> getOpenLayouts() {
        return mItemManger.getOpenLayouts();
    }

    @Override
    public void removeShownLayouts(SwipeLayout layout) {
        mItemManger.removeShownLayouts(layout);
    }

    @Override
    public boolean isOpen(int position) {
        return mItemManger.isOpen(position);
    }

    @Override
    public Attributes.Mode getMode() {
        return mItemManger.getMode();
    }

    @Override
    public void setMode(Attributes.Mode mode) {
        mItemManger.setMode(mode);
    }
}
