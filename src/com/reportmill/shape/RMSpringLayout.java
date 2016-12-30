/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import snap.gfx.Rect;
import snap.util.*;

/**
 * Handles code for laying out shape children with springs and struts.
 */
public class RMSpringLayout extends RMShapeLayout implements PropChangeListener {

    // The SpringSizer
    RMSpringSizer          _sizer;

    // Whether we are in layout
    boolean                _inLayout;
    
/**
 * Override to start listening to property changes.
 */
public void addLayoutChild(RMShape aChild)
{
    aChild.addPropChangeListener(this); // Start listening to shape property changes
    addSpringInfo(aChild); _sizer = null;
}

/**
 * Override to stop listening to property changes.
 */
public void removeLayoutChild(RMShape aChild)
{
    aChild.removePropChangeListener(this);
    removeSpringInfo(aChild); _sizer = null;
}

/**
 * Returns spring info for child.
 */
protected SpringInfo getSpringInfo(RMShape aChild)  { return (SpringInfo)aChild._layoutInfoX; }

/**
 * Adds spring info for child.
 */
protected void addSpringInfo(RMShape aChild)
{
    double x = aChild.getFrameX(), y = aChild.getFrameY(), w = aChild.getFrameWidth(), h = aChild.getFrameHeight();
    SpringInfo sinfo = new SpringInfo(x,y,w,h,_parent.getWidth(),_parent.getHeight());
    aChild._layoutInfoX = sinfo;
}

/**
 * Removes spring info for child.
 */
protected void removeSpringInfo(RMShape aChild)  { aChild._layoutInfoX = null; }

/**
 * Override to do springs stuff.
 */
protected void layoutChildren()
{
    _inLayout = true;
    
    // Get child bounds rects and set bounds of children for new width/height
    Rect rects[] = getChildrenBoundsRects();
    for(int i=0, iMax=_parent.getChildCount(); i<iMax; i++) {
        RMShape child = _parent.getChild(i); Rect rect = rects[i];
        child.setFrame(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
    }
    
    _inLayout = false;
}

/**
 * Returns the child rects for given parent height.
 */
private Rect[] getChildrenBoundsRects()
{
    // If Sizer.ChildBoxes is set, return them
    if(_sizer!=null && _sizer._cboxes!=null) return _sizer._cboxes;
    
    // Get original child rects
    int ccount = _parent.getChildCount();
    double newPW = _parent.getWidth(), newPH = _parent.getHeight();
    
    // Iterate over children and calculate new bounds rect for original child bounds and new parent width/height
    Rect rects[] = new Rect[ccount];
    for(int i=0; i<ccount; i++) { RMShape child = _parent.getChild(i);
        SpringInfo li = getSpringInfo(child);
        Rect rect = rects[i] = new Rect(li.x, li.y, li.width, li.height);
        double oldPW = li.pwidth, oldPH = li.pheight;
        String asize = child.getAutosizing();
        boolean lms = asize.charAt(0)=='~', ws = asize.charAt(1)=='~', rms = asize.charAt(2)=='~';
        boolean tms = asize.charAt(4)=='~', hs = asize.charAt(5)=='~', bms = asize.charAt(6)=='~';
        double x1 = rect.getX(), y1 = rect.getY(), w1 = rect.getWidth(), h1 = rect.getHeight();
        double sw = (lms? x1 : 0) + (ws? w1 : 0) + (rms? oldPW - (x1 + w1) : 0), dw = newPW - oldPW;
        double sh = (tms? y1 : 0) + (hs? h1 : 0) + (bms? oldPH - (y1 + h1) : 0), dh = newPH - oldPH;
        double x2 = (!lms || sw==0)? x1 : (x1 + dw*x1/sw);
        double y2 = (!tms || sh==0)? y1 : (y1 + dh*y1/sh);
        double w2 = (!ws || sw==0)? w1 : (w1 + dw*w1/sw);
        double h2 = (!hs || sh==0)? h1 : (h1 + dh*h1/sh);
        rect.setRect(x2, y2, w2, h2);
    }
    
    // Return rects
    return rects;
}

/**
 * Update LayoutInfo for all children. 
 */
public void reset()
{
    for(RMShape child : _parent.getChildren()) addSpringInfo(child);
    _parent.setNeedsLayout(false);
}

/**
 * Override to return parent width.
 */
protected double computePrefWidth(double aHeight)  { return _parent.getWidth(); }

/**
 * Override to get from Sizer.
 */
protected double computePrefHeight(double aWidth) { return getSizer().getPrefHeight(); }

/** Returns the sizer. */
private RMSpringSizer getSizer()  { return _sizer!=null? _sizer : (_sizer=new RMSpringSizer(_parent)); }

/** Returns child best height. */
public double getBestHeight(RMShape aChild)
{
    double minH = aChild.getMinHeight();
    double prefH = !(aChild instanceof RMImageShape)? aChild.getPrefHeight() : aChild.getHeight();
    return Math.max(prefH, minH);
}

/**
 * Called to revalidate when shape bounds change.
 */
public void propertyChange(PropChange anEvent)
{
    // If InLayout (we caused property change), just return
    if(_inLayout) return;

    // Get property name - if frame changer, do something
    String pname = anEvent.getPropertyName();
    if(pname=="X" || pname=="Y" || pname=="Width" || pname=="Height" || pname=="Roll" || pname=="Autosizing") {
        RMShape child = (RMShape)anEvent.getSource();
        addSpringInfo(child);
    }
}

/**
 * Standard clone implementation.
 */
public RMSpringLayout clone()  { RMSpringLayout cln = (RMSpringLayout)super.clone(); cln._sizer = null; return cln; }

/**
 * A class to hold info for a spring child.
 */
protected static class SpringInfo {
    
    // The bounds and original parent width/height
    double x, y, width, height, pwidth, pheight;
    
    /** Creates a SpringInfo. */
    public SpringInfo(double aX, double aY, double aW, double aH, double aPW, double aPH) {
        x = aX; y = aY; width = aW; height = aH; pwidth = aPW; pheight = aPH; }
}

}