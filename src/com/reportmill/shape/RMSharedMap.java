/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import java.util.Hashtable;

/**
 * An inner class to hold less-common attributes. Tracks owner shape to facilitate sharing.
 */
public class RMSharedMap extends Hashtable {
    
    // Whether this map is being shared
    boolean _shared = false;
    
    // A shared default map (cloned to turn on shared flag)
    static RMSharedMap _sharedMap = new RMSharedMap().clone();
    
/**
 * Creates a new map.
 */
public RMSharedMap() { }

/**
 * Returns an empty shared map.
 */
public static RMSharedMap getShared()  { return _sharedMap; }

/**
 * Returns whether map is already being shared.
 */
public boolean isShared()  { return _shared; }

/**
 * Overrides hashtable method to just mark hashtable shared and return it.
 */
public RMSharedMap clone()
{
    // Set shared to true since somone asked for a clone
    _shared = true;
    
    // Return this map
    return this;
}

/**
 * Provides real clone implementation.
 */
public RMSharedMap cloneX()
{
    // Do normal map clone
    RMSharedMap clone = (RMSharedMap)super.clone();
    
    // Reset shared flag to false
    clone._shared = false;
    
    // Return clone
    return clone;
}

}