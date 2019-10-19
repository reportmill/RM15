/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.base;
import java.util.*;
import snap.util.*;

/**
 * This class simply manages a list of groupings and has some nice convenience methods.
 */
public class RMGrouper implements Cloneable, XMLArchiver.Archivable {
    
    // The list of groupings
    List <RMGrouping>   _groupings = new ArrayList();

    // Selected group index (editing only)
    int                 _selectedGroupingIndex = 0;
    
    // The PropChangeSupport
    PropChangeSupport   _pcs = PropChangeSupport.EMPTY;
    
    // A listener to catch RMGrouping PropChange
    PropChangeListener  _groupingLsnr = pc -> groupingDidPropChange(pc);
    
/**
 * Creates an empty grouper.
 */
public RMGrouper() { }

/**
 * Returns the number of groupings in this grouper.
 */
public int getGroupingCount()  { return _groupings.size(); }

/**
 * Returns the grouping at the given index.
 */
public RMGrouping getGrouping(int anIndex)  { return _groupings.get(anIndex); }

/**
 * Returns the list of groupings
 */
public List <RMGrouping> getGroupings()  { return _groupings; }

/**
 * Adds a given grouping to grouper's list of groupings.
 */
public void addGrouping(RMGrouping aGrouping)  { addGrouping(aGrouping, getGroupingCount()); }

/**
 * Adds a given grouping to grouper's list of groupings.
 */
public void addGrouping(RMGrouping aGrouping, int anIndex)
{
    // Add grouping to list
    _groupings.add(anIndex, aGrouping);
    
    // Start listening to property change events
    aGrouping.addPropChangeListener(_groupingLsnr);
    
    // Set selected grouping index to new grouping
    _selectedGroupingIndex = anIndex;
    
    // Fire property change
    firePropChange("Grouping", null, aGrouping, anIndex);
}

/**
 * Removes the grouping at the given index.
 */
public RMGrouping removeGrouping(int anIndex)
{
    // Remove grouping at index
    RMGrouping grouping = _groupings.remove(anIndex);
    
    // Stop listening to property change events
    grouping.removePropChangeListener(_groupingLsnr);
    
    // Adjust selected grouping index if needed
    _selectedGroupingIndex = Math.min(_selectedGroupingIndex, getGroupingCount() - 1);
    
    // Fire property change
    firePropChange("Grouping", grouping, null, anIndex);
    
    // Return removed object
    return grouping;
}

/**
 * Returns the grouping with the given key.
 */
public RMGrouping getGrouping(String aKey)
{
    // Get index for grouping key (just return null if not found)
    int index = indexOf(aKey);
    if(index<0)
        return null;
    
    // Return grouping for index
    return getGrouping(index);
}

/**
 * Returns the last grouping.
 */
public RMGrouping getGroupingLast()  { return getGrouping(getGroupingCount()-1); }

/**
 * Return the key for the grouping at the given index.
 */
public String getGroupingKey(int anIndex)  { return getGrouping(anIndex)._key; }

/**
 * Returns the index for the grouping with the given key.
 */
public int indexOf(String aKey)
{
    // Iterate over groupings and return index of grouping with given key
    for(int i=0, iMax=getGroupingCount(); i<iMax; i++)
        if(getGrouping(i).getKey().equals(aKey))
            return i;
    
    // Return -1 if grouping with key not found
    return -1;
}

/**
 * Adds a given list of groupings to grouper's list of groupings.
 */
public void addGroupings(List<RMGrouping> aList)
{
    for(int i=0, iMax=aList!=null? aList.size() : 0; i<iMax; i++)
        addGrouping(aList.get(i), getGroupingCount());
}

/**
 * Adds a new grouping with the given key.
 */
public RMGrouping addGroupingForKey(String aKey)  { return addGroupingForKey(aKey, getGroupingCount()); }

/**
 * Adds a new grouping with the given key at the given index.
 */
public RMGrouping addGroupingForKey(String aKey, int anIndex)
{
    // Create grouping for key
    RMGrouping grouping = new RMGrouping(aKey);
    
    // Add grouping at given index
    addGrouping(grouping, anIndex);
    
    // Return grouping
    return grouping;
}

/**
 * Removes the given grouping.
 */
public boolean removeGrouping(RMGrouping aGrouping)
{
    return removeGrouping(ListUtils.indexOfId(_groupings, aGrouping))!=null;
}

/**
 * Moves a grouping from given fromIndex to given toIndex.
 */
public void moveGrouping(int fromIndex, int toIndex)
{
    // If from or to index is invalid, just return, otherwise remove grouping from given index and add to given index
    if(fromIndex>=getGroupingCount()-1 || toIndex>=getGroupingCount()-1) return;
    RMGrouping grouping = removeGrouping(fromIndex);
    addGrouping(grouping, toIndex);
}

/**
 * Returns the currently selected grouping's index (for editing, mostly).
 */
public int getSelectedGroupingIndex()  { return _selectedGroupingIndex; }

/**
 * Sets the currently selected grouping by index (for editing, mostly).
 */
public void setSelectedGroupingIndex(int anIndex)  { _selectedGroupingIndex = anIndex; }

/**
 * Returns the currently selected grouping (while editing only).
 */
public RMGrouping getSelectedGrouping()  { return getGrouping(_selectedGroupingIndex); }

/**
 * Separates given objects into RMGroups defined by groupings.
 */
public RMGroup groupObjects(List aList)
{
    // Create new group for given list
    RMGroup group = new RMGroup(aList);
    
    // Group by this grouper
    group.groupBy(this, 0);
    
    // Return group
    return group;
}

/**
 * Listen for property changes and forward to grouper's property change listeners.
 */
protected void groupingDidPropChange(PropChange anEvent)  { firePropChange(anEvent); }

/**
 * Add listener.
 */
public void addPropChangeListener(PropChangeListener aLsnr)
{
    if(_pcs==PropChangeSupport.EMPTY) _pcs = new PropChangeSupport(this);
    _pcs.addPropChangeListener(aLsnr);
}

/**
 * Remove listener.
 */
public void removePropChangeListener(PropChangeListener aLsnr)  { _pcs.removePropChangeListener(aLsnr); }

/**
 * Fires a property change for given property name, old value, new value and index.
 */
protected void firePropChange(String aProp, Object oldVal, Object newVal)
{
    if(!_pcs.hasListener(aProp)) return;
    PropChange pc = new PropChange(this, aProp, oldVal, newVal);
    firePropChange(pc);
}

/**
 * Fires a property change for given property name, old value, new value and index.
 */
protected void firePropChange(String aProp, Object oldVal, Object newVal, int anIndex)
{
    if(!_pcs.hasListener(aProp)) return;
    PropChange pc = new PropChange(this, aProp, oldVal, newVal, anIndex);
    firePropChange(pc);
}

/**
 * Fires a given property change.
 */
protected void firePropChange(PropChange aPC)
{
    _pcs.firePropChange(aPC);
}

/**
 * Standard clone implementation.
 */
public RMGrouper clone()
{
    // Do normal clone
    RMGrouper clone = null; try { clone = (RMGrouper)super.clone(); }
    catch(CloneNotSupportedException e) { throw new RuntimeException(e); }
    
    // Clear PropChangeSupport
    clone._pcs = PropChangeSupport.EMPTY;
    
    // Clone deep grouping
    clone._groupings = new ArrayList();
    for(RMGrouping grp : _groupings) {
        RMGrouping grp2 = grp.clone();
        clone.addGrouping(grp2);
    }
    
    // Return clone
    return clone;
}

/**
 * Standard equals implementation.
 */
public boolean equals(Object anObj)
{
    // Check identity and get other grouper
    if(anObj==this) return true;
    RMGrouper other = anObj instanceof RMGrouper? (RMGrouper)anObj : null; if(other==null) return false;
    
    // Check groupings
    if(!other._groupings.equals(_groupings)) return false;
    
    // Return true if all checks passed
    return true;
}

/**
 * Standard toString implementation.
 */
public String toString()
{
    StringBuffer sb = new StringBuffer("RMGrouper { Keys=");
    for(RMGrouping grp : getGroupings())
        sb.append(grp.getKey()).append(", ");
    if(getGroupingCount()>0)
        sb.delete(sb.length()-2, sb.length());
    return sb.append(" }").toString();
}

/**
 * XML archival.
 */
public XMLElement toXML(XMLArchiver anArchiver)
{
    // Get new element named grouper
    XMLElement e = new XMLElement("grouper");
    
    // Archive child groupings
    for(int i=0, iMax=getGroupingCount(); i<iMax; i++)
        e.add(getGrouping(i).toXML(anArchiver));

    // Return xml element
    return e;
}

/**
 * XML unarchival.
 */
public Object fromXML(XMLArchiver anArchiver, XMLElement anElement)
{
    // Unarchive grouping List
    _groupings = anArchiver.fromXMLList(anElement, "grouping", null, this);
    
    // Return this grouper
    return this;
}

}