package com.daimajia.swipe.interfaces;

import com.daimajia.swipe.SwipeLayout;
import com.daimajia.swipe.implments.SwipeItemMangerImpl;
import com.daimajia.swipe.util.Attributes;

import java.util.List;

public interface SwipeItemMangerInterface {

    public void openItem(int position);

    public void closeItem(int position);

    public void closeAllExcept(SwipeLayout layout);
    
    public void closeAllItems();

    public List<Integer> getOpenItems();

    public List<SwipeLayout> getOpenLayouts();

    public void removeShownLayouts(SwipeLayout layout);

    public boolean isOpen(int position);

    public Attributes.Mode getMode();

    public void setMode(Attributes.Mode mode);
}
