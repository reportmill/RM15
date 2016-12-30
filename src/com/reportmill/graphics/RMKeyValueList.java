/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;
import java.util.*;
import snap.util.ListUtils;

/**
 * An animation object that keeps track of Key-Values at Key Frame times for a target/key pair. 
 */
public class RMKeyValueList {

    // The target
    Object             _target;
    
    // The key
    String             _key;
    
    // The list of key values
    List <RMKeyValue>  _keyValues = new ArrayList();
    
    // The list of times
    List <Integer>     _times = new ArrayList();
    
/**
 * Creates a new key value list for given target and key.
 */
public RMKeyValueList(Object aTarget, String aKey)  { _target = aTarget; _key = aKey; }

/**
 * Returns the list target.
 */
public Object getTarget()  { return _target; }

/**
 * Returns the list key.
 */
public String getKey()  { return _key; }

/**
 * Returns the number of key values.
 */
public int getKeyValueCount()  { return _keyValues.size(); }

/**
 * Returns the individual key value at given index.
 */
public RMKeyValue getKeyValue(int anIndex)  { return _keyValues.get(anIndex); }

/**
 * Returns the individual time at given index.
 */
public int getTime(int anIndex)  { return _times.get(anIndex); }

/**
 * Adds a key value and time to key values and times list.
 */
public void addKeyValue(RMKeyValue aKeyValue, int aTime)
{
    // Get index of given time
    int index = Collections.binarySearch(_times, aTime);
    
    // If index found, just set new key value
    if(index>=0)
        _keyValues.set(index, aKeyValue);
    
    // Otherwise fix index and add key value and time
    else {
        index = -index - 1;
        _keyValues.add(index, aKeyValue);
        _times.add(index, aTime);
    }
}

/**
 * Removes the key value and time at given index.
 */
public RMKeyValue removeKeyValue(int anIndex)
{
    _times.remove(anIndex);
    return _keyValues.remove(anIndex);
}

/**
 * Removes a given key value and its corresponding time.
 */
public int removeKeyValue(RMKeyValue aKeyValue)
{
    int index = ListUtils.indexOfId(_keyValues, aKeyValue);
    if(index>=0) removeKeyValue(index);
    return index;
}

/**
 * Removes a given time and its corresponding key value.
 */
public int removeTime(int aTime)
{
    int index = Collections.binarySearch(_times, aTime);
    if(index>=0) removeKeyValue(index);
    return index;
}

/**
 * Returns whether key value list contains the given time.
 */
public boolean containsTime(int aTime)  { return Collections.binarySearch(_times, aTime)>=0; }

/**
 * Sets the time based on key-value-time entries.
 */
public void setTime(int aTime, int theLastTime)
{
    RMInvocation inv = getInvocation(theLastTime, aTime);
    if(inv!=null)
        inv.invoke();
}

/**
 * Returns an invocation for the given time suitable to set appropriate interpolated value in shape.
 */
private RMInvocation getInvocation(int oldTime, int newTime)
{
    // If no records, just return
    if(getKeyValueCount()==0) return null;
    
    // Get index of record for given time
    int index = Collections.binarySearch(_times, newTime);
    
    // If time found, get key value and return invocation
    if(index>=0) {
        RMKeyValue keyValue = getKeyValue(index);
        return new RMInvocation(getTarget(), getKey(), keyValue.getValue());
    }
    
    // Otherwise get indexes of surrounding key values
    int index1 = -index - 2;
    int index2 = index1 + 1;
    
    // If either index invalid, return either null if last time was invalid, or invocation for other index
    if(index1<0 || index2>=getKeyValueCount())
        return index==Collections.binarySearch(_times, oldTime)? null :
            getInvocation(getKeyValue(index1<0? 0 : getKeyValueCount()-1).getValue());

    // Get records before and after time
    RMKeyValue kval1 = getKeyValue(index1);
    RMKeyValue kval2 = getKeyValue(index2);
        
    // Get invocations before and after time
    RMInvocation inv1 = getInvocation(kval1.getValue());
    RMInvocation inv2 = getInvocation(kval2.getValue());

    // Calculate timeFactor (delta of given time over delta of datapoint times)
    int time1 = getTime(index1);
    int time2 = getTime(index2);
    double timeFactor = (newTime - time1)/(double)(time2 - time1);
    if(kval2.getInterpolator()!=null) // If interpolator is present, adjust ratio
        timeFactor = kval2.getInterpolator().getValue(timeFactor, 0, 1);

    // Get previous invocation, next invocation and blended invocation - then invoke
    return inv1.getInterpolation(timeFactor, inv2);
}

/**
 * Returns an invocation for value and interpolator.
 */
private RMInvocation getInvocation(Object aValue)  { return new RMInvocation(getTarget(), getKey(), aValue); }

}