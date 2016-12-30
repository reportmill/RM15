/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import com.reportmill.graphics.*;

/**
 * This class represents a shape for a document inside a page of another document.
 */
public class RMNestedDoc extends RMParentShape implements RMAnimator.Listener {
    
    // Actual document
    RMDocument      _doc;
    
    // Selected page index
    int             _pageIndex = 0;
    
    // The delay
    float           _delay;
    
    // Scale factor
    float           _scaleFactor = 1;
    
    // The time between loops for any animation of nested doc
    float           _gapDelay;
    
    // Child animator
    RMAnimator      _childAnimator;

/**
 * Returns the nested document.
 */
public RMDocument getNestedDoc()  { return _doc; }

/**
 * Sets the nested document.
 */
public void setNestedDoc(RMDocument aDoc)
{
    _doc = aDoc;
    copyShape(aDoc.getPage(0));
    setSelectedPageIndex(0);
}

/**
 * Returns the selected page index.
 */
public int getSelectedPageIndex()  { return _pageIndex; }

/**
 * Sets the selected page index for the nested doc.
 */
public void setSelectedPageIndex(int anIndex)
{
    // Set page index
    _pageIndex = Math.min(anIndex, getNestedDoc().getPageCount() - 1);
    
    // Get selected page
    RMPage page = getSelectedPage();
    
    // Remove children and add children from nested document
    removeChildren();
    for(int i=page.getChildCount()-1; i>=0; i--)
        addChild(page.getChild(i), 0);
}

/**
 * Returns the selected page.
 */
public RMPage getSelectedPage()  { return getNestedDoc().getPage(getSelectedPageIndex()); }

/**
 * Returns the number of seconds the nested doc animation time lags the master document.
 */
public float getDelay()  { return _delay; }

/**
 * Sets the number of seconds the nested doc animation time lags the master document.
 */
public void setDelay(float aValue)  { _delay = aValue; }

/**
 * Returns the relative speed of the nested docs animation relative to the master document.
 */
public float getScaleFactor()  { return _scaleFactor; }

/**
 * Sets the relative speed of the nested docs animation relative to the master document.
 */
public void setScaleFactor(float aValue)  { _scaleFactor = aValue; }

/**
 * Returns the number of seconds the nested doc animation will wait between loops.
 */
public float getGapDelay()  { return _gapDelay; }

/**
 * Sets the number of seconds the nested doc animation will wait between loops.
 */
public void setGapDelay(float aValue)  { _gapDelay = aValue; }

/**
 * Overrides shape method to register this with animator listener, so nested doc can update child timeline.
 */
public void setParent(RMParentShape aParent)
{
    // Remove this shape as listener of current parent animator
    if(aParent==null && getAnimator()!=null)
        getAnimator().removeAnimatorListener(this);
    
    // Do normal setParent
    super.setParent(aParent);

    // Add this shape as listener of new parent animator
    if(aParent!=null) {
        getAnimator(true).addAnimatorListener(this);
        animatorUpdated(getAnimator());
        if(getSelectedPage().getChildAnimator()!=null)
            setChildAnimator(getSelectedPage().getChildAnimator().clone());
    }
}

/**
 * Returns the child animator associated with this page, creating one if it doesn't exist (if requested).
 */
public RMAnimator getChildAnimator(boolean doCreate)
{
    // If child animator hasn't been created and create was requested, create and set child animator
    if(_childAnimator==null && doCreate)
        setChildAnimator(new RMAnimator());
    
    // Return child animator
    return _childAnimator;
}

/**
 * Sets the child animator associated with this page.
 */
protected void setChildAnimator(RMAnimator anAnimator)
{
    // Set new child animator
    if(_childAnimator!=null) _childAnimator.removePropChangeListener(this);
    _childAnimator = anAnimator;
    if(_childAnimator!=null) _childAnimator.addPropChangeListener(this);

    // If new animator, set owner to this page
    if(anAnimator!=null)
        anAnimator.setOwner(this);    
}

/** Animator Listener method. */
public void animatorStarted(RMAnimator anAnimator) { }

/** Animator listener method. */
public void animatorStopped(RMAnimator anAnimator) { }

/**
 * Animator Listener method.
 */
public void animatorUpdated(RMAnimator anAnimator)
{
    RMAnimator animator = getChildAnimator(); if(animator==null) return;
    float realTime = anAnimator.isRunning()? anAnimator.getSecondsElapsed() : anAnimator.getTimeSeconds();
    float time = (realTime - _delay)*_scaleFactor;
    float playTime = animator.getMaxTime();
    float gapDelay = _gapDelay*_scaleFactor;
    float loopTime = playTime + gapDelay;
    int currentLoop = (int)(time/loopTime);
    int loopCount = getAnimator().getLoops()? 999 : 1;

    // If currentLoop is past loopCount, just set time to maxTime
    if(currentLoop >= loopCount)
        animator.setTime(animator.getMaxTime());
    
    // If time is within zero and loopCount loops, set time corrected for loop gapDelays
    else if(time>0 && time<(currentLoop*loopTime + playTime))
        animator.setTimeSeconds(time - currentLoop*gapDelay);

    // If time is less than zero, set time to zero
    else animator.setTime(0);
}

/**
 * Editor method - indicates that nested doc is super-selectable.
 */
public boolean superSelectable() { return true; }

}