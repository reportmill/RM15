/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import java.util.*;
import snap.util.*;

/**
 * This class represents a page layer, a standard feature of page layout applications that lets you
 * manage the children of a page as separate groups, so that some can be made unselectable or invisible
 * to ease template editing.
 */
public class RMPageLayer implements Cloneable {
    
    // The parent page
    RMPage          _page;
    
    // The name of the layer
    String          _name;
    
    // Whether layer is visible
    boolean         _visible = true;
    
    // Whether layer is locked
    boolean         _locked = false;
    
    // The children in this layer
    List <RMShape>  _children = new ArrayList();
    
    // Constants defining the state of the layer
    public static final int StateVisible = 0;
    public static final int StateInvisible = 1;
    public static final int StateLocked = 2;

/**
 * Creates a plain layer.
 */
public RMPageLayer() { }

/**
 * Creates a page layer for a given page and name.
 */
public RMPageLayer(RMPage aPage, String aName)  { _page = aPage; _name = aName; }

/**
 * Returns the page associated with this layer.
 */
public RMPage getPage()  { return _page; }

/**
 * Returns the layer's name.
 */
public String getName()  { return _name; }

/**
 * Sets the layer's name.
 */
public void setName(String aName)  { _name = aName; }

/**
 * Returns whether this layer is to be drawn.
 */
public boolean isVisible()  { return _visible; }

/**
 * Sets whether this layer is to be drawn.
 */
public void setVisible(boolean aFlag)  { _visible = aFlag; }

/**
 * Returns whether this layer is editable.
 */
public boolean isLocked()  { return _locked; }

/**
 * Sets whether this layer is editable.
 */
public void setLocked(boolean aFlag)  { _locked = aFlag; }

/**
 * Sets whether this layer is selectable.
 */
public boolean isSelectable()  { return _visible && !_locked; }

/**
 * Returns the state of this layer (locked, visible, invisible).
 */
public int getLayerState()
{
    if(_locked) return StateLocked; // Handle StateLocked
    if(_visible) return StateVisible; // Handle Statevisible
    return StateInvisible; // Handle StateInvisible
}

/**
 * Returns the state of this layer (locked, visible, invisible).
 */
public void setLayerState(int aState)
{
    // Handle StateLocked
    if(aState==StateLocked) _locked = _visible = true;
    
    // Handle StateInvisible
    else if(aState==StateInvisible) _locked = _visible = false;
    
    // Handle StateVisible
    else if(aState==StateVisible) { _locked = false; _visible = true; }
}

/**
 * Returns the number of children in this layer.
 */
public int getChildCount()  { return _children.size(); }

/**
 * Returns the specific child of this layer at the given index.
 */ 
public RMShape getChild(int anIndex)  { return _children.get(anIndex); }

/**
 * Returns the list of children for this layer.
 */
public List <RMShape> getChildren()  { return _children; }

/**
 * Adds a child to this layer.
 */
public void addChild(RMShape aChild)  { _children.add(aChild); }

/**
 * Adds a child to this layer at the given index.
 */
public void addChild(RMShape aChild, int anIndex)  { _children.add(anIndex, aChild); }

/**
 * Removes a child from this layer.
 */
public Object removeChild(int anIndex)  { return _children.remove(anIndex); }

/**
 * Removes a child from this layer.
 */
public int removeChild(RMShape aChild)  { return ListUtils.removeId(_children, aChild); }

/**
 * Adds a list of children to this layer.
 */
public void addChildren(List <RMShape> theShapes)
{
    if(theShapes!=null)
        for(RMShape shape : theShapes)
            addChild(shape);
}

/**
 * Removes a list of children from this layer.
 */
public void removeChildren(List <RMShape> theShapes)
{
    for(RMShape shape : theShapes)
        removeChild(shape);
}

/**
 * Removes all children from this layer.
 */
public void removeChildren()  { _children.clear(); }

/**
 * Returns the index of this layer in its page.
 */
public int getIndex()
{
    // Iterate over page layers and return index of this layer
    for(int i=0, iMax=getPage().getLayerCount(); i<iMax; i++)
        if(getPage().getLayer(i)==this)
            return i;
    return -1; // Return -1 since layer not found
}

/**
 * Returns the index of a given child.
 */
public int getChildIndex(RMShape aChild)  { return ListUtils.indexOfId(getChildren(), aChild); }

/**
 * Returns the index of this layer's first child in the page.
 */
public int getPageChildIndex()
{
    // Get index (just return 0 if 0)
    int index = getIndex(); if(index==0) return 0;
    
    // Get previous layer
    RMPageLayer layer = getPage().getLayer(index-1);
    return layer.getPageChildIndex() + layer.getChildCount();
}

/**
 * Moves the shapes specified in the given list to the front of this layer's list of shapes.
 */
public void bringShapesToFront(List shapes)
{
    // Iterate over given shapes and move each to front of this layer
    for(int i=0, iMax=shapes.size(); i<iMax; i++) {
        RMShape child = (RMShape)shapes.get(i);
        if(ListUtils.removeId(_children, child)>=0)
            _children.add(child);
    }
}

/**
 * Moves the shapes specified in the given list to the back of this layer's list of shapes.
 */
public void sendShapesToBack(List shapes)
{
    // Iterate over given shapes and move each to back of this layer
    for(int i=0, iMax=shapes.size(); i<iMax; i++) {
        RMShape child = (RMShape)shapes.get(i);
        if(ListUtils.removeId(_children, child)>=0)
            _children.add(i, child);
    }
}

/**
 * Standard clone implementation.
 */
public Object clone()
{
    // Do normal clone
    RMPageLayer clone; try { clone = (RMPageLayer)super.clone(); }
    catch(CloneNotSupportedException e) { throw new RuntimeException(e); }
    
    // Clone children
    clone._children = new ArrayList(_children.size());
    for(RMShape shp : _children) {
        clone._children.add(shp);
    }
    
    // Return clone
    return clone;
}

}