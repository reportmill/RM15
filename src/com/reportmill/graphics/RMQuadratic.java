/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;
import snap.gfx.Point;
import snap.gfx.Rect;

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
public RMQuadratic(Point startPoint, Point cp, Point endPoint)
{
    super(startPoint, endPoint); _cp1x = cp.getX(); _cp1y = cp.getY();
}

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
public Point getCP1()  { return Point.get(_cp1x,_cp1y); }

/**
 * Sets the curve values.
 */
public void setCurve(double x1, double y1, double x2, double y2, double x3, double y3)
{
    _spx = x1; _spy = y1; _cp1x = x2; _cp1y = y2; _epx = x3; _epy = y3;
}

/**
 * Sets the curve values.
 */
public void setCurve(Point p1, Point p2, Point p3)  { setCurve(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y); }

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
 * Returns the minimum distance from the given point to this segment.
 */
public double getDistance(double aX, double aY)  { return getDistanceQuadratic(aX, aY); }

/**
 * Returns the minimum distance from the given point to the curve.
 */
public double getDistanceQuadratic(double aX, double aY)
{
    // If control points almost on end ponts line, return distance to line
    double dist = getDistanceLine(_cp1x, _cp1y);
    if(dist<.255)
        return getDistanceLine(aX, aY);

    // Split the curve and recurse
    RMQuadratic c1 = new RMQuadratic();
    RMQuadratic c2 = new RMQuadratic();
    subdivide(c1, c2);
    double dist1 = c1.getDistanceQuadratic(aX, aY);
    double dist2 = c2.getDistanceQuadratic(aX, aY);
    return Math.min(dist1, dist2);
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
 * Subdivides this curve into the given left and right curves.
 */
public void subdivide(RMQuadratic left, RMQuadratic right)
{
    // Calculate new control points
    double x1 = _spx, y1 = _spy;
    double x2 = _epx, y2 = _epy;
    double ctrlx1 = (_spx + _cp1x) / 2f;
    double ctrly1 = (_spy + _cp1y) / 2f;
    double ctrlx2 = (_epx + _cp1x) / 2f;
    double ctrly2 = (_epy + _cp1y) / 2f;
    double midpx = (ctrlx1 + ctrlx2) / 2f;
    double midpy = (ctrly1 + ctrly2) / 2f;
    
    // Set new curve values if curves are present
    if(left!=null)
        left.setCurve(x1, y1, ctrlx1, ctrly1, midpx, midpy);
    if(right!=null)
        right.setCurve(midpx, midpy, ctrlx2, ctrly2, x2, y2);
}

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

/**
 * Returns the bounds.
 */
public void getBounds(Rect aRect)  { getBounds(_spx, _spy, _cp1x, _cp1y, _epx, _epy, aRect); }

/**
 * Returns the bounds of the bezier.
 */
public static void getBounds(double x0, double y0, double x1, double y1, double x2, double y2, Rect aRect)
{
    // Declare coords for min/max points
    double p1x = x0;
    double p1y = y0;
    double p2x = x0;
    double p2y = y0;

    // For quadratic, slope at point t is just linear interpolation of slopes at the endpoints.
    // Find solution to LERP(slope0,slope1,t) == 0
    double d = x0 - 2*x1 + x2;
    double t = d==0 ? 0 : (x0 - x1) / d;

    // If there's a valid solution, get actual x point at t and add it to the rect
    if(t>0 && t<1) {
        double turningpoint = x0*(1-t)*(1-t) + 2*x1*(1-t)*t + x2*t*t;
        p1x = Math.min(p1x, turningpoint);
        p2x = Math.max(p2x, turningpoint);
    }
    
    // Do the same for y
    d = y0 - 2*y1 + y2;
    t = d==0? 0 : (y0 - y1)/d;
    if(t>0 && t<1) {
        double turningpoint = y0*(1-t)*(1-t) + 2*y1*(1-t)*t + y2*t*t;
        p1y = Math.min(p1y, turningpoint);
        p2y = Math.max(p2y, turningpoint);
    }
    
    // Include endpoint
    p1x = Math.min(p1x, x2);
    p2x = Math.max(p2x, x2);
    p1y = Math.min(p1y, y2);
    p2y = Math.max(p2y, y2);    
    
    // Set rect
    aRect.setRect(p1x, p1y, p2x - p1x, p2y - p1y);
}

}