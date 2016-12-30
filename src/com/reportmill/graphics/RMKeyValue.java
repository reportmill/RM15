/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;
import snap.util.*;

/**
 * An animation class that represents a key/value pair in a key frame.
 */
public class RMKeyValue extends SnapObject implements Cloneable {

    // The target
    Object             _target;
    
    // The key
    String             _key;
    
    // The value
    Object             _value;
    
    // An interpolator
    Interpolator       _interp;

/**
 * Creates a new key frame.
 */
public RMKeyValue(Object anObj, String aKey, Object aValue)  { _target = anObj; _key = aKey; _value = aValue; }

/**
 * Returns the target.
 */
public Object getTarget()  { return _target; }

/**
 * Returns the key.
 */
public String getKey()  { return _key; }

/**
 * returns the value.
 */
public Object getValue()  { return _value; }

/**
 * Returns the interpolator.
 */
public Interpolator getInterpolator()  { return _interp; }

/**
 * Sets the interpolator.
 */
public void setInterpolator(Interpolator anInterp)
{
    firePropChange("Interpolator", _interp, _interp=anInterp);
}

/**
 * Standard clone method.
 */
public RMKeyValue clone()  { return (RMKeyValue)super.clone(); }

/**
 * Standard toString method.
 */
public String toString()
{
    return String.format("RMKeyValue { key:%s, value:%s }", getKey(), getValue());
}

}