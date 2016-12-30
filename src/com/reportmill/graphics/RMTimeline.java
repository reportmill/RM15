/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;
import java.util.*;
import snap.util.*;

/**
 * An animation object that holds a list of key frames.
 */
public class RMTimeline extends SnapObject implements Cloneable, PropChangeListener {

    // The owner of the timeline
    Object                 _owner;

    // The key frames
    List <RMKeyFrame>      _keyFrames = new ArrayList();
     
    // The list of key value lists
    List <RMKeyValueList>  _keyValueLists = new ArrayList();

    // The current time (in milliseconds)
    int                    _time;
     
/**
 * Creates a new timeline.
 */
public RMTimeline()  { }

/**
 * Creates a new timeline with owner.
 */
public RMTimeline(Object anOwner)  { _owner = anOwner; }

/**
 * Returns the timeline owner.
 */
public Object getOwner()  { return _owner; }

/**
 * Sets the timeline owner.
 */
public void setOwner(Object anOwner)  { _owner = anOwner; }

/**
 * Returns whether timeline is empty.
 */
public boolean isEmpty()
{
    for(RMKeyValueList kvlist : _keyValueLists)
        if(kvlist.getKeyValueCount()>1)
            return false;
    return true;
}

/**
 * Returns the number of key frames.
 */
public int getKeyFrameCount()  { return _keyFrames.size(); }

/**
 * Returns an individual key frame at given index.
 */
public RMKeyFrame getKeyFrame(int anIndex)  { return _keyFrames.get(anIndex); }

/**
 * Returns the list of key frames.
 */
public List <RMKeyFrame> getKeyFrames()  { return _keyFrames; }

/**
 * Adds a given key frame.
 */
public void addKeyFrame(RMKeyFrame aKeyFrame)
{
    int index = ListUtils.binarySearch(_keyFrames, aKeyFrame);
    addKeyFrame(aKeyFrame, index);
}

/**
 * Adds a given key frame at given index. This should be protected - but undo system current needs it public.
 */
public void addKeyFrame(RMKeyFrame aKeyFrame, int anIndex)
{
    // Add key frame and set its Timeline property
    _keyFrames.add(anIndex, aKeyFrame);
    aKeyFrame.setTimeline(this);
    
    // Start listening to changes on key frame
    aKeyFrame.addPropChangeListener(this);
    
    // Add key values for existing key frame values
    for(RMKeyValue kval : aKeyFrame.getKeyValues())
        addKeyFrameKeyValue(aKeyFrame, kval);
    
    // Fire property change
    firePropChange("KeyFrame", null, aKeyFrame, anIndex);
}

/**
 * Removes a keyframe at given index.
 */
public RMKeyFrame removeKeyFrame(int anIndex)
{
    // Remove key frame and reset its Timeline property
    RMKeyFrame kframe = _keyFrames.remove(anIndex);
    kframe.setTimeline(null);
    
    // Stop listening to changes on key frame
    kframe.removePropChangeListener(this);
    
    // Remove key frame time from key value lists
    for(RMKeyValue kval : kframe.getKeyValues())
        removeKeyFrameKeyValue(kframe, kval);
    
    // Fire property change
    firePropChange("KeyFrame", kframe, null, anIndex);

    // Return key frame
    return kframe;
}

/**
 * Removes the given key frame.
 */
public int removeKeyFrame(RMKeyFrame aKeyFrame)
{
    int index = getIndex(aKeyFrame);
    if(index>=0) removeKeyFrame(index);
    return index;
}

/**
 * Return index for given key frame.
 */
public int getIndex(RMKeyFrame aKeyFrame)  { return Collections.binarySearch(_keyFrames, aKeyFrame); }

/**
 * Returns the key frame at the given time.
 */
public RMKeyFrame getKeyFrameAt(int aTime)  { return getKeyFrameAt(aTime, false); }

/**
 * Returns the key frame at the given time, with an option to create it if missing.
 */
public RMKeyFrame getKeyFrameAt(int aTime, boolean doCreate)
{
    // Iterate over key frames and return if key frame for time is found
    for(int i=0, iMax=getKeyFrameCount(); i<iMax; i++) { RMKeyFrame kframe = getKeyFrame(i);
        if(kframe.getTime()==aTime)
            return kframe;
        if(kframe.getTime()>aTime)
            break;
    }
    
    // If create is requested, create and insert key frame
    if(doCreate) {
        RMKeyFrame kframe = new RMKeyFrame(aTime);
        addKeyFrame(kframe);
        return kframe;
    }
    
    // Return null since not found
    return null;
}

/**
 * Returns a key value list for a given target and value.
 */
public RMKeyValueList getKeyValueList(Object aTarget, String aKey)  { return getKeyValueList(aTarget, aKey, false); }

/**
 * Returns a key value list for a given target and value, with an option to create if missing.
 */
public RMKeyValueList getKeyValueList(Object aTarget, String aKey, boolean doCreate)
{
    // Iterate over key value lists and return if target/key found
    for(RMKeyValueList klist : _keyValueLists)
        if(klist.getTarget()==aTarget && klist.getKey().equals(aKey))
            return klist;
    
    // If create requested, create, and and return new list 
    RMKeyValueList klist = null;
    if(doCreate)
        _keyValueLists.add(klist = new RMKeyValueList(aTarget, aKey));
    return klist;
}

/**
 * Returns the current time of the timeline (in milliseconds).
 */
public int getTime()  { return _time; }

/**
 * Sets the current time of the timeline (in milliseconds).
 */
public void setTime(int aTime)
{
    // Iterate over key value lists and set time
    for(RMKeyValueList kvlist : _keyValueLists)
        kvlist.setTime(aTime, _time);

    // Set time
    _time = aTime;
}

/**
 * Adds a key frame key value, so that a key value list entry is created.
 */
protected void addKeyFrameKeyValue(RMKeyFrame aKeyFrame, RMKeyValue aKeyValue)
{
    RMKeyValueList kvlist = getKeyValueList(aKeyValue.getTarget(), aKeyValue.getKey(), true);
    kvlist.addKeyValue(aKeyValue, aKeyFrame.getTime());
}

/**
 * Removes a key frame key value, so that a key value list entry is removed.
 */
protected void removeKeyFrameKeyValue(RMKeyFrame aKeyFrame, RMKeyValue aKeyValue)
{
    // Remove key value
    RMKeyValueList kvlist = getKeyValueList(aKeyValue.getTarget(), aKeyValue.getKey()); if(kvlist==null) return;
    int index = kvlist.removeKeyValue(aKeyValue); if(index<0) return;
    
    // If time was earliest time, add it back at zero (maybe should be previous time?)
    if(kvlist.getKeyValueCount()>0 && kvlist.getTime(0)>aKeyFrame.getTime())
        getKeyFrameAt(0, true).addKeyValue(aKeyValue);
}

/**
 * Adds a new key frame key value for 
 */
public RMKeyValue addKeyFrameKeyValue(Object aTarget, String aKey, Object aValue, int aTime)
{
    return addKeyFrameKeyValue(aTarget, aKey, aValue, aTime, null, 0);
}

/**
 * Adds a new key frame key value for 
 */
public RMKeyValue addKeyFrameKeyValue(Object aTarget, String aKey, Object aValue, int aTime,
    Object anOldValue, int anOldTime)
{
    // If at time zero and no other key/values for target/key, just return
    if(aTime==0 && getKeyValueList(aTarget, aKey)==null)
        return null;
    
    // See if we need to add a previous record
    if(aTime!=0 && anOldValue!=null) {
        
        // If scope key frame not represented, add previous value
        if(!getKeyValueList(aTarget, aKey, true).containsTime(anOldTime))
            getKeyFrameAt(anOldTime, true).addKeyValue(aTarget, aKey, anOldValue);

        // Remove any other other key frame key values between
        removeKeyFrameKeyValues(aTarget, aKey, anOldTime, aTime, false);
    }
        
    // Otherwise 
    RMKeyFrame kframe = getKeyFrameAt(aTime, true);
    return kframe.addKeyValue(aTarget, aKey, aValue);
}

/**
 * Removes key frame key values for target and key between two times (with option to include those times).
 */
public void removeKeyFrameKeyValues(Object aTarget, String aKey, int aStartTime, int anEndTime, boolean isInclusive)
{
    // Iterate over key frames and remove key values between times that match target/key
    for(RMKeyFrame kframe : getKeyFrames()) { int time = kframe.getTime();
        if(time>aStartTime && time<anEndTime || isInclusive && (time==aStartTime || time==anEndTime))
            if(kframe.getKeyValue(aTarget, aKey)!=null)
                kframe.removeKeyValue(kframe.getKeyValue(aTarget, aKey));
    }
}

/**
 * This method shifts key frame values at or beyond a given time by the given amount.
 */
public void shiftRecords(int fromTime, int byTime)
{
    // Remove keyframes at or beyond from time
    List <RMKeyFrame> removedKeyFrames = new ArrayList();
    for(RMKeyFrame kframe : (List <RMKeyFrame>)new ArrayList(getKeyFrames()))
        if(kframe.getTime()>=fromTime) {
            if(kframe.getTime()!=0) removeKeyFrame(kframe); removedKeyFrames.add(kframe); }
    
    // Add new key frames for removed frames
    for(RMKeyFrame kframe : removedKeyFrames) {
        kframe = kframe.clone(); kframe._time += byTime;
        addKeyFrame(kframe);
    }
}

/**
 * This method scales all records from a given start time through a length to a new length.
 */
public void scaleRecords(int aStartTime, float aFactor)
{
    // Remove key frames at or beyond from time
    List <RMKeyFrame> removedKeyFrames = new ArrayList();
    for(RMKeyFrame kframe : (List <RMKeyFrame>)new ArrayList(getKeyFrames()))
        if(kframe.getTime()>aStartTime) {
            removeKeyFrame(kframe); removedKeyFrames.add(kframe); }

    // Add new key frames for removed frames
    for(RMKeyFrame kframe : removedKeyFrames) {
        kframe = kframe.clone(); kframe._time = Math.round(kframe._time*aFactor);
        addKeyFrame(kframe);
    }
}

/**
 * Implement to catch key value changes and forward to this key frame's property change listeners.
 */
public void propertyChange(PropChange anEvent)  { firePropChange(anEvent); }

/**
 * Standard clone implementation.
 */
public RMTimeline clone()  { return clone(null); }

/**
 * Standard clone implementation.
 */
public RMTimeline clone(Object aTarget)
{
    // Get normal clone
    RMTimeline clone = (RMTimeline)super.clone();
    
    // Create new key frame and key value lists
    clone._keyFrames = new ArrayList();
    clone._keyValueLists = new ArrayList();
    
    // Add key frame clones
    for(RMKeyFrame keyFrame : getKeyFrames())
        clone.addKeyFrame(keyFrame.clone());
    
    // If target provided, reset all key value targets
    if(aTarget!=null) {
        for(RMKeyFrame kframe : clone.getKeyFrames())
            for(RMKeyValue kval : kframe.getKeyValues()) kval._target = aTarget;
        for(RMKeyValueList kvlist : clone._keyValueLists) kvlist._target = aTarget;
    }
    
    // Return clone
    return clone;
}

/**
 * XML archival (for shape).
 */
public void toXML(XMLArchiver anArchiver, XMLElement anElement)
{
    // Iterate over key frames
    for(RMKeyFrame kframe : getKeyFrames()) {
        
        // Create and configure xml element for key frame
        XMLElement kframeXML = new XMLElement("KeyFrame");
        kframeXML.add("time", kframe.getTime());
        
        // Iterate over key values
        for(RMKeyValue kval : kframe.getKeyValues()) {
            
            // Create key value element and add key
            XMLElement kvalXML = new XMLElement("KeyValue");
            kvalXML.add("key", kval.getKey());

            // Add value to element as color, boolean or float
            Object value = kval.getValue();
            if(value instanceof RMColor) kvalXML.add("value", "#" + ((RMColor)value).toHexString());
            else if (value instanceof Boolean) kvalXML.add("value", value);
            else kvalXML.add("value", SnapUtils.floatValue(value));
            
            // Get interpolator and add
            Interpolator interp = kval.getInterpolator();
            if(interp!=null) kvalXML.add("interpolator", interp.getName());
            
            // Add key value xml element to key frame xml element
            kframeXML.add(kvalXML);
        }
        
        // Add key frame xml element to given element
        anElement.add(kframeXML);
    }
    
    // Iterate over record lists and records
    /*for(int i=0, iMax=getRecordListCount(); i<iMax; i++) { RMShapeAnimRecordList recordList = getRecordList(i);
        for(int j=0, jMax=recordList.getRecordCount(); j<jMax; j++) { RMShapeAnimRecord record =recordList.getRecord(j);
            RXElement animXML = new RXElement("anim");
            animXML.add("attr", recordList.getAttribute());
            animXML.add("time", record.getTime());
            Object value = record.getValue();
            if(value instanceof RMColor) animXML.add("value", "#" + ((RMColor)value).toHexString());
            else if (value instanceof Boolean) animXML.add("value", value);
            else animXML.add("value", RMUtils.floatValue(value));
            RMInterpolator interp = record.getInterpolation();
            if(interp!=null) {
                if(interp.isShared()) animXML.add("interpolator", interp.getName());
                else animXML.add(anArchiver.toXML(interp)); }
            anElement.add(animXML);
        }
    }*/
}

/**
 * XML unarchival for anim and shape.
 */
public void fromXML(Object aTarget, XMLArchiver anArchiver, XMLElement anElement)
{
    // Iterate over anim elements
    for(int i=anElement.indexOf("anim"); i>=0; i=anElement.indexOf("anim", i+1)) {
        
        // Get anim record xml element
        XMLElement animXML = anElement.get(i);
        
        // Get attribute name (do legacy conversion)
        String attr = animXML.getAttributeValue("attr");
        if(attr.equals("LineWidth")) attr = "StrokeWidth";
        
        // Get record time
        int time = Math.round(animXML.getAttributeFloatValue("time")*1000);
        
        // Get value and decode
        Object val = animXML.getAttributeValue("value");
        if(val.toString().startsWith("#")) val = new RMColor(val.toString());
        else if(val.toString().equalsIgnoreCase("true")) val = Boolean.TRUE;
        else if(val.toString().equalsIgnoreCase("false")) val = Boolean.FALSE;
        else val = SnapUtils.floatValue(val);
        
        // Add new key frame value
        getKeyFrameAt(time, true).addKeyValue(aTarget, attr, val);
    }
    
    // Iterate over key frame elements
    for(int i=anElement.indexOf("KeyFrame"); i>=0; i=anElement.indexOf("KeyFrame", i+1)) {
        
        // Get key frame xml element and time
        XMLElement kframeXML = anElement.get(i);
        int time = kframeXML.getAttributeIntValue("time");
        
        // Iterate over key value elements
        for(int j=kframeXML.indexOf("KeyValue"); j>=0; j=kframeXML.indexOf("KeyValue", j+1)) {
            
            // Get key value xml element and key name
            XMLElement kvalXML = kframeXML.getElement(j);
            String key = kvalXML.getAttributeValue("key");
        
            // Get value and decode
            Object value = kvalXML.getAttributeValue("value");
            if(value.toString().startsWith("#")) value = new RMColor(value.toString());
            else if(value.toString().equalsIgnoreCase("true")) value = true;
            else if(value.toString().equalsIgnoreCase("false")) value = false;
            else value = SnapUtils.floatValue(value);
        
            // Add new key frame value
            getKeyFrameAt(time, true).addKeyValue(aTarget, key, value);
        }
    }
}

/**
 * Standard toString implementation.
 */
public String toString()
{
    StringBuffer sb = new StringBuffer(getClass().getSimpleName()).append(" { times:\"");
    for(RMKeyFrame kframe : getKeyFrames()) sb.append(kframe.getTime()).append(',');
    if(getKeyFrameCount()>0) sb.delete(sb.length()-1,sb.length());
    sb.append("\" }");
    return sb.toString();
}

}