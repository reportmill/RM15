/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;

/**
 * This class encapsulates simple hit information of a given curve or line against another curve or line.
 */
public class RMHitInfo {
    
    // Number of intersection points
    public int        _hitCount;

    // Parametric location (from 0-1) of hit on receiving geometry
    public double     _r;
    
    // Parametric location (from 0-1) of hit on secondary geometry
    public double     _s;
    
    // Index of hit segment (for paths)
    public int        _index;
   
/** Creates a hit info record with given attributes. */
public RMHitInfo(int hitCount, double r, double s, int index)
{
    _hitCount = hitCount; _r = r; _s = s; _index = index;
}

/**
 * Returns the numer of intersections associated with the given geometries.
 */
public int getHitCount()  { return _hitCount; }

/**
 * Returns the parametric location (from 0-1) of hit on receiving geometry.
 */
public double getR()  { return _r; }

/**
 * Returns the parametric location (from 0-1) of hit on secondary geometry.
 */
public double getS()  { return _s; }

/**
 * Returns the index of hit segment (for paths).
 */
public int getIndex()  { return _index; }

}