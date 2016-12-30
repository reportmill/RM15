/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;

/**
 * Handles code for laying out shape children with springs and struts.
 */
public class RMShapeLayout implements Cloneable {

    // The parent this layout works for
    RMParentShape  _parent;
    
/**
 * Returns the parent for this layout.
 */
public RMParentShape getParent()  { return _parent; }

/**
 * Sets the parent for this layout.
 */
public void setParent(RMParentShape aParent)  { _parent = aParent; }

/**
 * Performs layout.
 */
protected void layoutChildren()  { }

/**
 * Returns the preferred with of the parent.
 */
protected double computePrefWidth(double aHeight)  { return getParent().getWidth(); }

/**
 * Returns the preferred with of the parent.
 */
protected double computePrefHeight(double aWidth)  { return getParent().getHeight(); }

/**
 * Called when a child is added to parent shape.
 */
protected void addLayoutChild(RMShape aChild)  { }

/**
 * Called when a child is removed from parent shape.
 */
protected void removeLayoutChild(RMShape aChild)  { }

/**
 * Standard clone implementation.
 */
public RMShapeLayout clone()
{
    try { RMShapeLayout clone = (RMShapeLayout)super.clone(); clone._parent = null; return clone; }
    catch(CloneNotSupportedException e) { throw new RuntimeException(e); }
}

}