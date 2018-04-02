/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;
import snap.gfx.*;

/**
 * This class models a simple quadratic curve, providing methods for extracting points, distance calculation, bisection,
 * hit detection and such.
 */
public class RMQuadratic extends RMLine {
    
    // Quadratic control point (inherits start & end from line)
    public double _cp1x, _cp1y;
  
/**
 * Creates a new quadratic curve.
 */
public RMQuadratic()  { }

/**
 * Creates a new quadratic curve for this gvein start point, control point and end point.
 */
public RMQuadratic(double x1, double y1, double x2, double y2, double x3, double y3)
{
    super(x1, y1, x3, y3); _cp1x = x2; _cp1y = y2;
}

/**
 * Returns control point 1 x.
 */
public double getCP1x()  { return _cp1x; }

/**
 * Returns control point 1 y.
 */
public double getCP1y()  { return _cp1y; }

/**
 * Returns control point 1 as point.
 */
public Point getCP1()  { return new Point(_cp1x,_cp1y); }

/**
 * Returns the point on this curve at the parametric location t (defined from 0-1).
 */
public Point getPoint(double t, Point aPoint)
{
    // p' = (1-t)^2*p0 + 2*t*(1-t)*p1 + t^2*p2
    double s = 1 - t, s2 = s*s, t2 = t*t;
    double x = s2*_spx + 2*t*s*_cp1x + t2*_epx;
    double y = s2*_spy + 2*t*s*_cp1y + t2*_epy;
    if(aPoint==null) return new Point(x, y);
    aPoint.setXY(x, y); return aPoint;
}

/**
 * Returns the point count of segment.
 */
public int getPointCount()  { return 3; }

/**
 * Returns the x of point at given index.
 */
public double getPointX(int anIndex)  { return anIndex==0? _spx : anIndex==1? _cp1x : _epx; }

/**
 * Returns the y of point at given index.
 */
public double getPointY(int anIndex)  { return anIndex==0? _spy : anIndex==1? _cp1y : _epy; }

/**
 * Returns the min x point of this bezier.
 */
public double getMinX()  { return Math.min(super.getMinX(), _cp1x); }

/**
 * Returns the min y point of this bezier.
 */
public double getMinY()  { return Math.min(super.getMinY(), _cp1y); }

/**
 * Returns the max x point of this bezier.
 */
public double getMaxX()  { return Math.max(super.getMaxX(), _cp1x); }

/**
 * Returns the max y point of this bezier.
 */
public double getMaxY()  { return Math.max(super.getMaxY(), _cp1y); }

}