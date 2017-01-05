/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;
import snap.gfx.Point;

/**
 * This class represents a geometric point. Legacy, use snap.gfx.Point.
 */
public class RMPoint extends Point {
    
/** Creates a point initialized to 0,0. */
public RMPoint() { }

/** Creates a point initialized to the given x and y. */
public RMPoint(double x1, double y1)  { x = x1; y = y1; }

}