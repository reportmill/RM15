/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;
import java.util.*;
import snap.util.*;

/**
 * An animation object that holds an object key/value for a given time.
 */
public class RMKeyFrame extends SnapObject implements Cloneable, PropChangeListener, Comparable<RMKeyFrame> {

    // The timeline that owns this key frame
    RMTimeline           _timeline;
    
    // The time (in milliseconds)
    int                  _time;
    
    // The list of key-values
    List <RMKeyValue>    _keyValues = new ArrayList();
    
/**
 * Creates a new key frame.
 */
public RMKeyFrame(int aTime)  { _time = aTime; }

/**
 * Returns the timeline that owns this key frame (if present).
 */
public RMTimeline getTimeline()  { return _timeline; }

/**
 * Sets the timeline that owns this key frame (if present).
 */
protected void setTimeline(RMTimeline aTimeline)  { _timeline = aTimeline; }

/**
 * Returns the time.
 */
public int getTime()  { return _time; }

/**
 * Returns the number of key values.
 */
public int getKeyValueCount()  { return _keyValues.size(); }

/**
 * Returns the individual key value at index.
 */
public RMKeyValue getKeyValue(int anIndex)  { return _keyValues.get(anIndex); }

/**
 * Returns the list of key values.
 */
public List <RMKeyValue> getKeyValues()  { return _keyValues; }

/**
 * Adds a new key value.
 */
public void addKeyValue(RMKeyValue aKeyValue)  { addKeyValue(aKeyValue, getKeyValueCount()); }

/**
 * Adds a new key value at given index.
 */
public void addKeyValue(RMKeyValue aKeyValue, int anIndex)
{
    // Add key value
    _keyValues.add(getKeyValueCount(), aKeyValue);
    
    // Start listening to record list changes
    aKeyValue.addPropChangeListener(this);
    
    // If Timeline, update Timeline KeyValueList for new KeyValue
    if(getTimeline()!=null)
        getTimeline().addKeyFrameKeyValue(this, aKeyValue);
    
    // Fire property change
    firePropChange("KeyValue", null, aKeyValue, anIndex);
}

/**
 * Removes a key value at given index.
 */
public RMKeyValue removeKeyValue(int anIndex)
{
    // Remove key value and return it
    RMKeyValue kval = _keyValues.remove(anIndex);
    
    // Stop listening to record list changes
    kval.removePropChangeListener(this);
    
    // If Timeline, update Timeline KeyValueList for departing KeyValue
    if(getTimeline()!=null)
        getTimeline().removeKeyFrameKeyValue(this, kval);
    
    // Fire property change
    firePropChange("KeyValue", kval, null, anIndex);

    // Remove key value and return it
    return kval;
}

/**
 * Removes a key value.
 */
public int removeKeyValue(RMKeyValue aKVal)
{
    int index = ListUtils.indexOfId(_keyValues, aKVal);
    if(index>=0) removeKeyValue(index);
    return index;
}

/**
 * Returns the key value for given object and key, if present.
 */
public RMKeyValue getKeyValue(Object aTarget, String aKey)
{
    for(int i=0, iMax=getKeyValueCount(); i<iMax; i++) { RMKeyValue kval = getKeyValue(i);
        if(aTarget==kval.getTarget() && aKey.equals(kval.getKey()))
            return kval; }
    return null;
}

/**
 * Adds a new key value for given target, key and value.
 */
public RMKeyValue addKeyValue(Object aTarget, String aKey, Object aValue)
{
    RMKeyValue kval = getKeyValue(aTarget, aKey);
    if(kval!=null) removeKeyValue(kval);
    kval = new RMKeyValue(aTarget, aKey, aValue);
    addKeyValue(kval);
    return kval;
}

/**
 * Implement to catch key value changes and forward to this key frame's property change listeners.
 */
public void propertyChange(PropChange anEvent)  { firePropChange(anEvent); }

/**
 * Standard compare implementation.
 */
public int compareTo(RMKeyFrame aKeyFrame)
{
    if(getTime()<aKeyFrame.getTime()) return -1;
    if(getTime()>aKeyFrame.getTime()) return 1;
    return 0;
}

/**
 * Standard clone method.
 */
public RMKeyFrame clone()
{
    // Do normal clone
    RMKeyFrame clone = (RMKeyFrame)super.clone();
    
    // Clear Timeline
    clone._timeline = null;
    
    // Create new key values list and clone key values
    clone._keyValues = new ArrayList();
    for(RMKeyValue kval : getKeyValues())
        clone.addKeyValue(kval.clone());
    
    // Return clone
    return clone;
}

/**
 * Standard to string implementation.
 */
public String toString()
{
    StringBuffer sb = new StringBuffer(getClass().getSimpleName()).append(" { time:").append(getTime()).append(" }\n");
    for(RMKeyValue kval : getKeyValues())
        sb.append("  ").append(kval).append("\n");
    if(sb.charAt(sb.length()-1)=='\n') sb.delete(sb.length()-1, sb.length());
    return sb.toString();
}

}