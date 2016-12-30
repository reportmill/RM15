/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;
import com.reportmill.shape.*;
import java.util.*;
import snap.util.*;
import snap.view.ViewTimer;

/**
 * This class represents a timer for animation, holding things like a current time and max time.
 */
public class RMAnimator extends SnapObject implements Cloneable, XMLArchiver.Archivable {
    
    // The shape this animator works for
    RMParentShape    _owner;
    
    // The current animator time in milliseconds
    int              _time;
    
    // The max time of the animator in milliseconds
    int              _maxTime = 5000;
    
    // The animator update interval in milliseconds
    int              _interval = 40;
    
    // The action to be taken at end of animation (eg., Loop, Loop:5, Loop:5,3, Page:Next, Page:Last)
    boolean          _loops = true;
    
    // Whether to reset animator time when animator stopped
    boolean          _resetTimeOnStop;
    
    // The time animator was at prior to the last animator start
    int              _playStartTime;

    // The timer used to run animator
    ViewTimer        _timer;
    
    // The time the animator was started at
    long             _startTime;
    
    // Whether animator is enabled (so extra anim events don't get entered)
    boolean          _enabled = true;
    
    // The list of explicity requested key frames (have no records)
    List <Integer>   _freezeFrames = new ArrayList();
    
    // The key-frame time that new anim records should be made relative to
    Integer          _scopeTime;
    
    //  Array of shapes that were just added to stifle record entry
    List             _newBorns = new ArrayList();

    /**
     * An interface for classes that want to receive notification of certain animator events.
     */
    public interface Listener {
        public void animatorStarted(RMAnimator anAnimator);
        public void animatorUpdated(RMAnimator anAnimator);
        public void animatorStopped(RMAnimator anAnimator);
    }
    
/**
 * Returns the owner of the animator.
 */
public RMParentShape getOwner()  { return _owner; }

/**
 * Sets the owner of the animator.
 */
public void setOwner(RMParentShape anObj)  { _owner = anObj; }

/**
 * Returns the current time of the animator.
 */
public int getTime()  { return _time; }

/**
 * Sets the current time of the animator (clamped to maxTime), tells owner to setTime as well.
 */
public void setTime(int aTime)
{
    // If we loop, mod the time, otherwise, clamp time
    if(getLoops() && aTime>getMaxTime())
        aTime = aTime%getMaxTime();
    else aTime = MathUtils.clamp(aTime, 0, getMaxTime());
    
    // If already at given time, just return
    if(_time==aTime) return;

    // Set new value and fire property change
    firePropChange("Time", _time, _time = aTime);
    
    // Reset time of owner children
    for(int i=0, iMax=getOwner().getChildCount(); i<iMax; i++)
        getOwner().getChild(i).setTime(_time);
    
    // Send did update notification
    for(int i=0, iMax=getListenerCount(Listener.class); i<iMax; i++)
        getListener(Listener.class, i).animatorUpdated(this);
    
    // Clear new born list
    _newBorns.clear();
}

/**
 * Returns the max time for this animator.
 */
public int getMaxTime()  { return _maxTime; }

/**
 * Sets the max time for this animator. May also call setTime() if animator's current time is beyond new max.
 */
public void setMaxTime(int aMaxTime)
{
    // Set max time
    _maxTime = aMaxTime;
    
    // If time is greater than max-time, reset time to max time
    if(getTime()>_maxTime)
        setTime(_maxTime);
}

/**
 * Returns the animator time in seconds.
 */
public float getTimeSeconds()  { return getTime()/1000f; }

/**
 * Sets the time in seconds.
 */
public void setTimeSeconds(float aTime)  { setTime(Math.round(aTime*1000)); }

/**
 * Returns the animator max time in seconds.
 */
public float getMaxTimeSeconds()  { return getMaxTime()/1000f; }

/**
 * Sets the max time in seconds.
 */
public void setMaxTimeSeconds(float aTime)  { setMaxTime(Math.round(aTime*1000)); }

/**
 * Returns the update interval of the animator in milliseconds.
 */
public int getInterval()  { return _interval; }

/**
 * Sets the update interval of the animator in milliseconds.
 */
public void setInterval(int anInterval)
{
    // Set interval
    _interval = anInterval;
    
    // If timer present, change interval
    if(_timer!=null)
        _timer.setPeriod(_interval);
}

/**
 * Returns the frame rate for this animator.
 */
public float getFrameRate()  { return 1000f/_interval; }

/**
 * Sets the frame rate for this animator.
 */
public void setFrameRate(float aValue)  { setInterval(Math.round(1000/aValue)); }

/**
 * Returns whether this animator loops when time hits max time.
 */
public boolean getLoops()  { return _loops; }

/**
 * Sets whether this animator loops when time hits max time.
 */
public void setLoops(boolean aValue)  { _loops = aValue; }

/**
 * Returns whether the time is reset to start value when animator stops.
 */
public boolean getResetTimeOnStop()  { return _resetTimeOnStop; }

/**
 * Sets whether the time is reset to start value when animator stops.
 */
public void setResetTimeOnStop(boolean aFlag)  { _resetTimeOnStop = aFlag; }

/**
 * Returns the list of key-frames present in animator owner's children.
 */
public List <Integer> getKeyFrameTimes()  { return getKeyFrameTimes(null, true); }

/**
 * Returns the list of key-frames present in the given list of shapes (assumed to be animator owner's descendants).
 */
public List <Integer> getKeyFrameTimes(List <RMShape> theShapes, boolean includeImplied)
{
    // If a shapeList wasn't provided, assume it should be animator owner's children.
    if(theShapes==null)
        theShapes = _owner.getChildren();

    // Create new list for key frames
    List <Integer> keyFrames = new Vector();
    
    // Add implied key frames: time zero, current time and freeze frames
    if(includeImplied) {
        keyFrames.add(0);
        if(getTime()!=0) keyFrames.add(getTime());
        for(Integer freezeFrame : _freezeFrames) {
            int index = Collections.binarySearch(keyFrames, freezeFrame);
            if(index<0)
                keyFrames.add(-index - 1, freezeFrame);
        }
    }
    
    // Add explicit key frames by iterating over owner's children (on down)
    for(int i=0, iMax=ListUtils.size(theShapes); i<iMax; i++)
        getKeyFrames(theShapes.get(i), keyFrames);

    // Update Scoped keyframe
    if(!keyFrames.contains(_scopeTime))
        _scopeTime = null;

    // Return key frames
    return keyFrames;
}

/**
 * Loads the key-frames from the given shape into the given list, with an option to recurse into children.
 */
private void getKeyFrames(RMShape aShape, List <Integer> aList)
{
    // Get shape timeline
    RMTimeline timeline = aShape.getTimeline();
    
    // If available, load key frames
    if(timeline!=null) for(RMKeyFrame kframe : timeline.getKeyFrames()) {
        int index = Collections.binarySearch(aList, kframe.getTime());
        if(index<0)
            aList.add(-index - 1, kframe.getTime());
    }
    
    // Iterate over shape children
    for(int i=0, iMax=aShape.getChildCount(); i<iMax; i++)
        getKeyFrames(aShape.getChild(i), aList);
}

/**
 * Returns whether the current time is not currently a key-frame.
 */
public boolean canFreezeFrame()
{
    // Get current time
    int time = getTime();
    
    // If frame zero or last frame, return false
    if(time==0 || time==getMaxTime())
        return false;
    
    // If frame already in freeze frames, return false
    if(Collections.binarySearch(_freezeFrames, time)>=0)
        return false;
    
    // If frame is already in key frames, return false
    if(Collections.binarySearch(getKeyFrameTimes(null, false), time)>=0)
        return false;
    
    // Return true
    return true;
}

/**
 * Adds a freeze frame for the animator's current time to the freeze-frame list.
 */
public void addFreezeFrame()
{
    int index = Collections.binarySearch(_freezeFrames, getTime());
    if(index<0)
        _freezeFrames.add(-index - 1, getTime());
}

/**
 * Returns the key-frame time that new anim records should be made relative to.
 */
public int getScopeTime()
{
    // If scope time not explicitly set, get time prior to given time
    if(_scopeTime==null) {
        List <Integer> keyFrames = getKeyFrameTimes();
        int index = Collections.binarySearch(keyFrames, getTime());
        if(index<0) index = -index - 2; else index--;
        return index>=0? keyFrames.get(index) : 0;
    }
    
    // Return scope time
    return _scopeTime;
}

/**
 * Sets the key-frame time that new anim records should be made from.
 */
public void setScopeTime(Integer aValue)  { _scopeTime = aValue; }

/**
 * Initiates a timer to start automatically incrementing the animator's time (by frame rate increments).
 */
public void play()
{
    // If timer is null, create it
    if(_timer==null)
        _timer = new ViewTimer(getInterval(), this :: update);

    // If animator not running, start timer
    if(!isRunning()) {
        
        // Record time animator was at and time started
        _playStartTime = getTime();
        _startTime = System.currentTimeMillis();
        
        // Start timer
        _timer.start();
        
        // Send animatorStarted notification
        for(Listener listener : getListeners(Listener.class)) listener.animatorStarted(this);
    }
}
      
/**
 * Disables the timer to stop automatically incrementing the animator's time.
 */
public void stop()
{
    // If animator is running, stop it
    if(isRunning()) {
        
        // Stop timer
        _timer.stop();
        
        // Reset time to play start time
        if(getPlayStartTime()>=0 && getResetTimeOnStop())
            setTime(getPlayStartTime());
        else setTime(getMaxTime());
        
        // Send animatorStopped notification
        for(Listener listener : getListeners(Listener.class)) listener.animatorStopped(this);
    }
}

/**
 * This method tells the animator to move one frame forward (based on the frame rate).
 */
public void frameForward()
{
    stop();
    setTime(getTime() + getInterval());
}

/**
 * This method tells the animator to move one frame backward (based on the frame rate).
 */
public void frameBackward()
{
    stop();
    setTime(getTime() - getInterval());
}

/**
 * Returns whether the animator is currently in the act of playing.
 */
public boolean isRunning()  { return _timer!=null && _timer.isRunning(); }

/**
 * Returns the number of seconds the animator has been playing for.
 */
public float getSecondsElapsed()  { return _startTime==0? 0 : (System.currentTimeMillis() - _startTime)/1000f; }

/**
 * Returns the play start time.
 */
public int getPlayStartTime()  { return _playStartTime; }

/**
 * This method is used by the timer to actually set the new time during play.
 */
private void update(ViewTimer aTimer)
{
    // Get seconds elapsed
    int elapsedTime = (int)(System.currentTimeMillis() - _startTime);
    
    // Get play start time
    int playStartTime = getPlayStartTime()>=0? getPlayStartTime() : 0;

    // Increment time by time elapsed
    setTime(playStartTime + elapsedTime);
    
    // If not looping and time is beyond max, stop animator
    if(!getLoops() && (getPlayStartTime() + elapsedTime >= getMaxTime()))
        stop();
}

/**
 * Adds an animator listener.
 */
public void addAnimatorListener(Listener aListener)  { addListener(Listener.class, aListener); }

/**
 * Removes an animator listener.
 */
public void removeAnimatorListener(Listener aListener)  { removeListener(Listener.class, aListener); }

/**
 * Shifts records of animator owner's children from the given from-time on, by the given by-time.
 */
public void shiftFrames(int fromTime, int aShiftTime)
{
    // Scale frames for shapes
    for(int i=0, iMax=getOwner().getChildCount(); i<iMax; i++)
        shiftFrames(getOwner().getChild(i), fromTime, aShiftTime);
    
    // If new time beyond max, reset max time
    setTime(fromTime + aShiftTime);
    setMaxTime(getMaxTime() + aShiftTime);
}

/**
 * Shifts records of a given shape from the given from-time on, by the given by-time.
 */
private void shiftFrames(RMShape aShape, int fromTime, int aShiftTime)
{
    // If shape timeline available, have it shift records
    if(aShape.getTimeline()!=null)
        aShape.getTimeline().shiftRecords(fromTime, aShiftTime);
    
    // Forward shift frame on to shape children
    for(int i=0, iMax=aShape.getChildCount(); i<iMax; i++)
        shiftFrames(aShape.getChild(i), fromTime, aShiftTime);
}

/**
 * Scales records of the animator owner's children in the range from start to start + length to a given new length.
 */
public void scaleFrames(int aStartTime, int newMaxTime)
{
    // Get scale factor
    float factor = newMaxTime/(float)getMaxTime();
    
    // Scale frames for shapes
    for(int i=0, iMax=getOwner().getChildCount(); i<iMax; i++)
        scaleFrames(getOwner().getChild(i), aStartTime, factor);
    
    // Set new max time
    setMaxTime(newMaxTime);
}

/**
 * Scales records of a given shape in the range from start to start + length to a given new length.
 */
private void scaleFrames(RMShape aShape, int aStartTime, float aFactor)
{
    // If shape timeline available, have it scale records
    if(aShape.getTimeline()!=null) {
        
        // Scale timeline records
        aShape.getTimeline().scaleRecords(aStartTime, aFactor);
        
        // Reset time
        resetShapeTimeline(aShape);
    }
    
    // Forward scale frames on to shape children
    for(int i=0, iMax=aShape.getChildCount(); i<iMax; i++)
        scaleFrames(aShape.getChild(i), aStartTime, aFactor);
}

/**
 * Returns whether animator owner's children are devoid of any animation records.
 */
public boolean isEmpty()
{
    // If any attributes are not default return false
    if(getMaxTime()!=5 || getFrameRate()!=25)
        return false;
    
    // Iterate over owner children and if any are not empty, return false
    for(int i=0, iMax=getOwner().getChildCount(); i<iMax; i++)
        if(!isEmpty(getOwner().getChild(i)))
            return false;
    
    // Return true since every child was empty
    return true;
}

/**
 * Returns whether a given shape and it's children are devoid of any animation records.
 */
private boolean isEmpty(RMShape aShape)
{
    // If shape timeline is non-null and not-empty, return false
    if(aShape.getTimeline()!=null && !aShape.getTimeline().isEmpty())
        return false;
    
    // Iterate over children and return false if any are not empty
    for(int i=0, iMax=aShape.getChildCount(); i<iMax; i++)
        if(!isEmpty(aShape.getChild(i)))
            return false;
    
    // Return true since shape anim and children are all empty
    return true;
}

/**
 * Removes records of the animator owner's children at a given time.
 */
public void removeFramesAtTime(int aTime)
{
    // Iterate over owner children and remove frame at time
    for(int i=0, iMax=getOwner().getChildCount(); i<iMax; i++)
        removeFramesBetweenTimes(getOwner().getChild(i), aTime, aTime, true);
    
    // Remove frame from freeze frames
    int index = Collections.binarySearch(_freezeFrames, aTime);
    if(index>=0)
        _freezeFrames.remove(index);
}

/**
 * Removes records of the animator owner's children in the given time range.
 */
public void removeFramesBetweenTimes(int startTime, int endTime, boolean inclusive)
{
    // Iterate over owner children and remove frames between times
    for(int i=0, iMax=getOwner().getChildCount(); i<iMax; i++)
        removeFramesBetweenTimes(getOwner().getChild(i), startTime, endTime, inclusive);
    
    // Remove freeze frames between times
    for(int i=_freezeFrames.size()-1; i>=0; i--)
        if(_freezeFrames.get(i)>startTime && _freezeFrames.get(i)<endTime)
            _freezeFrames.remove(i);
}

/**
 * Removes records of a given shape in the given time range.
 */
private void removeFramesBetweenTimes(RMShape aShape, int startTime, int endTime, boolean isInclusive)
{
    // Get shape timeline
    RMTimeline timeline = aShape.getTimeline();
    
    // If available, have it remove records between times
    if(timeline!=null) {
        for(int i=timeline.getKeyFrameCount()-1; i>=0; i--) { RMKeyFrame kframe = timeline.getKeyFrame(i);
            int time = kframe.getTime();
            if(time>startTime && time<endTime || isInclusive && (time==startTime || time==endTime))
                timeline.removeKeyFrame(i);
        }
        
        // Reset timeline
        resetShapeTimeline(aShape);
    }
    
    // Iterate over shape children and remove frames between times
    for(int i=0, iMax=aShape.getChildCount(); i<iMax; i++)
        removeFramesBetweenTimes(aShape.getChild(i), startTime, endTime, isInclusive);
}

/**
 * Resets a shape's timeline.
 */
protected void resetShapeTimeline(RMShape aShape)
{
    RMTimeline timeline = aShape.getTimeline(); if(timeline==null) return;
    aShape.undoerDisable();
    int time = timeline.getTime(); timeline.setTime(0); timeline.setTime(time);
    aShape.undoerEnable();
}

/**
 * Returns whether the animator owner's children should accept changes.
 */
public boolean isEnabled()  { return _enabled; }
 
/**
 * Sets whether the animator owner's children should accept changes. This can be used strategically to disable the
 * Animator so that it does not receive certain events (like each of the discrete moves in a mouse drag loop).
 * Should be followed by enable (they can be nested).
 */
public void setEnabled(boolean aFlag)  { _enabled = aFlag; }
 
/**
 * Returns whether the given objects was created while at the current time.
 */
public boolean isNewborn(Object anObj)  { return ListUtils.indexOfId(_newBorns, anObj)>=0; }

/**
 * Declare newBorn shapes so that if they are added at non-zero time, we won't get bogus initial values
 *  (after an explicit time change, they will start to accept changes).
 */
public void addNewborn(Object anObj)  { _newBorns.add(anObj); }

/**
 * Standard clone implementation.
 */
public RMAnimator clone()
{
    RMAnimator clone = (RMAnimator)super.clone(); // Get normal clone
    clone._timer = null; clone._startTime = 0; // Clear timer and start date
    return clone;
}

/**
 * XML archival.
 */
public XMLElement toXML(XMLArchiver anArchiver)
{
    // Get new element named animator
    XMLElement e = new XMLElement("animator");
    
    // Archive Time, MaxTime, Interval, EndAction, Loop
    if(_time!=0) e.add("time", _time);
    if(_maxTime!=5) e.add("max-time", _maxTime);
    if(_interval!=40) e.add("interval", _interval);
    if(getLoops()) e.add("loop", true);

    // Return xml element
    return e;
}

/**
 * XML unarchival.
 */
public Object fromXML(XMLArchiver anArchiver, XMLElement anElement)
{
    // Unarchive Time, MaxTime, Interval, Loop, EndAction
    _time = anElement.getAttributeIntValue("time"); if(_time!=0 && _time<10) _time *= 1000;
    float mt = anElement.getAttributeFloatValue("max-time", 5000); if(mt<50) mt *= 1000; setMaxTime((int)mt);
    if(anElement.hasAttribute("interval")) setInterval(anElement.getAttributeIntValue("interval"));
    if(anElement.hasAttribute("loop")) setLoops(anElement.getAttributeBoolValue("loop"));

    // Return this animator
    return this;
}

/**
 * Returns a string description of the animator.
 */
public String toString()  { return "RMAnimator Time=" + getTime() + ", MaxTime=" + getMaxTime(); }

}
