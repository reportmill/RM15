/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import snap.gfx.*;

/**
 * A parent shape that does child layout with RMSpringLayout.
 */
public class RMSpringShape extends RMParentShape {
    
    // Whether springs resizing is disabled
    boolean          _springsDisabled;
    
    // A class to do real layout work
    RMShapeLayout    _layout = null;
    
/**
 * Creates a new RMSpringShape.
 */
public RMSpringShape()  { _layout = new RMShapeLayout(); _layout._parent = this; }

/**
 * Return whether springs have been disabled.
 */
public boolean isSpringsDisabled()  { return _springsDisabled; }

/**
 * Return whether springs have been disabled.
 */
public void setSpringsDisabled(boolean aValue)  { _springsDisabled = aValue; }

/**
 * Override to notify layout.
 */
public void addChild(RMShape aChild, int anIndex)
{
    super.addChild(aChild, anIndex);
    if(!_springsDisabled) _layout.addChild(aChild);
}

/**
 * Override to notify layout.
 */
public RMShape removeChild(int anIndex)
{
    RMShape child = super.removeChild(anIndex);
    if(!_springsDisabled) _layout.removeChild(child);
    return child;
}

/**
 * Override to paint dashed box around bounds.
 */
public void paintShape(Painter aPntr)
{
    // Do normal version
    super.paintShape(aPntr); if(getClass()!=RMSpringShape.class) return;
    
    // Paint dashed box around bounds
    RMShapePaintProps props = RMShapePaintProps.get(aPntr);
    if(props.isEditing() && getStroke()==null && getFill()==null && getEffect()==null &&
        (props.isSelected(this) || props.isSuperSelected(this))) {
        aPntr.setColor(Color.LIGHTGRAY); aPntr.setStroke(Stroke.Stroke1.copyForDashes(3,2));
        aPntr.setAntialiasing(false); aPntr.draw(getBoundsInside()); aPntr.setAntialiasing(true);
    }
}

/**
 * Called to reposition/resize children.
 */
protected void layoutImpl()  { _layout.layout(); }

/**
 * Override to get from layout, if set.
 */
protected double getPrefHeightImpl(double aWidth)  { return _layout.getPrefHeight(aWidth); }

/**
 * Resets layout.
 */
protected void resetLayout()  { _layout.reset(); }

/**
 * Override to reset layout.
 */
public RMShape divideShapeFromTop(double anAmount)
{
    RMShape btmShape = super.divideShapeFromTop(anAmount);
    resetLayout();
    return btmShape;
}

/**
 * Standard clone implementation.
 */
public RMSpringShape clone()
{
    RMSpringShape clone = (RMSpringShape)super.clone();
    clone._layout = new RMShapeLayout(); clone._layout._parent = clone;
    return clone;
}

}