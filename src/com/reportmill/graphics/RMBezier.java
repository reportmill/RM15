/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;
import snap.gfx.Point;
import snap.gfx.Rect;

/**
 * This class models a simple bezier curve, providing methods for extracting points, distance calculation, bisection,
 * hit detection and such.
 */
public class RMBezier extends RMQuadratic {
    
    // Bezier control point (inherits start & end from line and _cp1 from quadratic)
    public double _cp2x, _cp2y;
    
/**
 * Creates a new bezier.
 */
public RMBezier()  { }

/**
 * Creates a new bezier from the given start point, control points and end point.
 */
public RMBezier(Point startPoint, Point cp1, Point cp2, Point endPoint)
{
    super(startPoint, cp1, endPoint);
    _cp2x = cp2.getX(); _cp2y = cp2.getY();
}

/**
 * Creates a new bezier from the given start point, control points and end point.
 */
public RMBezier(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4)
{
    super(x1, y1, x2, y2, x4, y4);
    _cp2x = x3; _cp2y = y3;
}

/**
 * Sets the values of the curve.
 */
public void setCurve(Point p1, Point p2, Point p3, Point p4)
{
    _spx = p1.x; _spy = p1.y; _cp1x = p2.getX(); _cp1y = p2.getY();
    _cp2x = p3.getX(); _cp2y = p3.getY(); _epx = p4.x; _epy = p4.y;
}

/**
 * Returns control point 2 x.
 */
public double getCP2x()  { return _cp2x; }

/**
 * Returns control point 2 y.
 */
public double getCP2y()  { return _cp2y; }

/**
 * Returns control point 2 as point.
 */
public Point getCP2()  { return Point.get(_cp2x,_cp2y); }

/**
 * Returns a specific point on the curve at the given parametric value (from 0-1).
 */
public Point getPoint(double t, Point aPoint)
{
    // p' = (1-t)^3*p0 + 3*t*(1-t)^2*p1 + 3*t^2*(1-t)*p2 + t^3*p3
    double s = 1 - t, s2 = s*s, s3 = s2*s, t2 = t*t, t3 = t2*t;
    double x = s3*_spx + 3*t*s2*_cp1x + 3*t2*s*_cp2x + t3*_epx;
    double y = s3*_spy + 3*t*s2*_cp1y + 3*t2*s*_cp2y + t3*_epy;
    if(aPoint==null) return new Point(x, y);
    aPoint.setXY(x, y); return aPoint;
}

/**
 * Returns the point count of segment.
 */
public int getPointCount()  { return 4; }

/**
 * Returns the x of point at given index.
 */
public double getPointX(int anIndex)  { return anIndex==0? _spx : anIndex==1? _cp1x : anIndex==2? _cp2x : _epx; }

/**
 * Returns the y of point at given index.
 */
public double getPointY(int anIndex)  { return anIndex==0? _spy : anIndex==1? _cp1y : anIndex==2? _cp2y : _epy; }

/**
 * Returns the minimum distance from the given point to this segment.
 */
public double getDistance(double aX, double aY)  { return getDistanceBezier(aX, aY); }

/**
 * Returns the minimum distance from the given point to the curve.
 */
public double getDistanceBezier(double aX, double aY)
{
    // If control points almost on end points line, return distance to line
    double dist1 = getDistanceLine(_cp1x, _cp1y);
    double dist2 = getDistanceLine(_cp2x, _cp2y);
    if(dist1<.25 && dist2<.25)
        return getDistanceLine(aX, aY);

    // Split the curve and recurse
    RMBezier c1 = new RMBezier();
    RMBezier c2 = new RMBezier();
    subdivide(c1, c2, .5f);
    dist1 = c1.getDistanceBezier(aX, aY);
    dist2 = c2.getDistanceBezier(aX, aY);
    return Math.min(dist1, dist2);
}

/**
 * Returns the min x point of this bezier.
 */
public double getMinX()  { return Math.min(super.getMinX(), _cp2x); }

/**
 * Returns the min y point of this bezier.
 */
public double getMinY()  { return Math.min(super.getMinY(), _cp2y); }

/**
 * Returns the max x point of this bezier.
 */
public double getMaxX()  { return Math.max(super.getMaxX(), _cp2x); }

/**
 * Returns the max y point of this bezier.
 */
public double getMaxY()  { return Math.max(super.getMaxY(), _cp2y); }

/**
 * Returns the bounds.
 */
public void getBounds(Rect aRect)  { getBounds(_spx, _spy, _cp1x, _cp1y, _cp2x, _cp2y, _epx, _epy, aRect); }

/**
 * Returns the bounds of the bezier.
 */
public static void getBounds(double x0, double y0, double x1, double y1, double x2, double y2, double x3, double y3, Rect aRect)
{
    // Declare coords for min/max points
    double p1x = x0;
    double p1y = y0;
    double p2x = x0;
    double p2y = y0;

    // Get coeficients of b-curve parametric equations (1-t)^3*x0 + 3*t*(1-t)^2*x1 + 3*t^2*(1-t)*x2 + t^3*x3
    // Take derivative of above function and solve for t where derivative equation = 0 (I used Mathematica).
    //   Since derivative of bezier cubic is quadradic, solution is of form (-b +- sqrt(b^2-4ac))/2a.
    // Get the part in the sqrt for x & y.
    double aX = -x0 + 3*x1 - 3*x2 + x3;
    double aY = -y0 + 3*y1 - 3*y2 + y3;
    double bX = 2*x0 - 4*x1 + 2*x2;
    double bY = 2*y0 - 4*y1 + 2*y2;
    double cX = -x0 + x1;
    double cY = -y0 + y1;
    double bSquaredMinus4acForX = bX*bX - 4*aX*cX;
    double bSquaredMinus4acForY = bY*bY - 4*aY*cY;
        
    // If square root part x is at least zero, there is a local max & min on bezier curve for x.
    if(bSquaredMinus4acForX >= 0) {
        
        // Declare variables for the two solutions
        double t1 = -1, t2 = -1;
        
        // If A is zero, the eqn reduces to a simple linear equation (Using the quadratic here would give NaNs)
        if(aX==0)
            t1 = bX==0? 0 : -cX/bX;
        
        // Otherwise, solve for tMax(-b + sqrt(b^2-4ac)/2a) and tMin(-b - sqrt(b^2-4ac)/2a)        
        else {
            t1 = (-bX - Math.sqrt(bSquaredMinus4acForX))/(2*aX);
            t2 = (-bX + Math.sqrt(bSquaredMinus4acForX))/(2*aX);
        }

        // If t1 is in valid range (0 to 1), solve for x value and use it to expand bounds
        if(t1>=0 && t1<=1) {
            double x = Math.pow(1-t1, 3)*x0 + 3*t1*Math.pow(1-t1, 2)*x1 + 3*Math.pow(t1, 2)*(1-t1)*x2 + Math.pow(t1,3)*x3;
            p1x = Math.min(p1x, x);
            p2x = Math.max(p2x, x);
        }

        // If t2 is in valid range (0 to 1), solve for x value and use it to expand bounds
        if(t2>=0 && t2<=1) {
            double x = Math.pow(1-t2, 3)*x0 + 3*t2*Math.pow(1-t2, 2)*x1 + 3*Math.pow(t2, 2)*(1-t2)*x2 + Math.pow(t2,3)*x3;
            p1x = Math.min(p1x, x);
            p2x = Math.max(p2x, x);
        }
    }

    // If square root part y is at least zero, there is a local max & min on bezier curve for y.
    if(bSquaredMinus4acForY >= 0) {
        
        // Declare variables for the two solutions
        double t1 = -1, t2 = -1;
        
        // If A is zero, the eqn reduces to a linear. (or possibly a point if B is zero)
        if(aY==0)
            t1 = (bY==0) ? 0 : -cY/bY;
        
        // Otherwise, solve for tMax(-b + sqrt(b^2-4ac)/2a) and tMin(-b - sqrt(b^2-4ac)/2a)        
        else {
            t1 = (-bY - Math.sqrt(bSquaredMinus4acForY))/(2*aY);
            t2 = (-bY + Math.sqrt(bSquaredMinus4acForY))/(2*aY);
        }
        
        // If tMin is in valid range (0 to 1), solve for x value and use it to expand bounds
        if((t1 >=0) && (t1 <= 1)) {
            double y = Math.pow(1-t1, 3)*y0 + 3*t1*Math.pow(1-t1, 2)*y1 + 3*Math.pow(t1, 2)*(1-t1)*y2 +
                Math.pow(t1,3)*y3;
            p1y = Math.min(p1y, y);
            p2y = Math.max(p2y, y);
        }

        // If tMax is in valid range (0 to 1), solve for x value and use it to expand bounds
        if((t2 >=0) && (t2 <= 1)) {
            double y = Math.pow(1-t2, 3)*y0 + 3*t2*Math.pow(1-t2, 2)*y1 + 3*Math.pow(t2, 2)*(1-t2)*y2 +
                Math.pow(t2,3)*y3;
            p1y = Math.min(p1y, y);
            p2y = Math.max(p2y, y);
        }
    }

    // Evaluate bounds expansion for curve endpoint
    p1x = Math.min(p1x, x3);
    p1y = Math.min(p1y, y3);
    p2x = Math.max(p2x, x3);
    p2y = Math.max(p2y, y3);
    
    // Set rect
    aRect.setRect(p1x, p1y, p2x - p1x, p2y - p1y);
}

/**
 * Returns the hit info for this bezier and a given line.
 */
public RMHitInfo getHitInfo(RMLine aLine)  { return RMBezierLineHit.getHitInfo(this, aLine, 0); }

/**
 * Returns a hit info for this bezier and given bezier.
 */
public RMHitInfo getHitInfo(RMBezier aBezier)
{
    // If control points almost on end ponts line, return hit info for line
    double dist1 = getDistanceLine(_cp1x, _cp1y);
    double dist2 = getDistanceLine(_cp2x, _cp2y);
    if(dist1<.25 && dist2<.25)
        return super.getHitInfo(aBezier);

    // Get bezier for head and tail
    RMBezier headBezier = new RMBezier();
    RMBezier tailBezier = new RMBezier();
    subdivide(headBezier, tailBezier, .5);
    
    // If head hit, adjusting s and returning
    RMHitInfo hitInfo = headBezier.getHitInfo(aBezier);
    if(hitInfo!=null) {
        hitInfo._r = hitInfo._r*.5;
        return hitInfo;
    }

    // Get bezier for tail and see if that hit (adjusting s if it did)
    hitInfo = tailBezier.getHitInfo(aBezier);
    if(hitInfo!=null)
        hitInfo._r = .5f + hitInfo._r*.5;

    // Return hit info
    return hitInfo;
}

/**
 * Returns a new line from this line's start point to given parametric location t (defined from 0-1) on this line.
 */
public RMLine getHead(double t)  { RMBezier b = new RMBezier(); subdivide(b, null, t); return b; }

/**
 * Returns a new line from given parametric location t (defined from 0-1) on this line to this line's end point.
 */
public RMLine getTail(double t)  { RMBezier b = new RMBezier(); subdivide(null, b, t); return b; }

/**
 * Returns a bezier curve from this curve's start point to the given parametric location (0-1).
 */
public void subdivide(RMBezier left, RMBezier right, double t)
{
    // Recalc control points from weighted average for parametric location t
    Point sp = getSP();
    Point ep = getEP();
    Point p01 = average(_spx, _spy, _cp1x, _cp1y, t);
    Point p12 = average(_cp1x, _cp1y, _cp2x, _cp2y, t);
    Point p23 = average(_cp2x, _cp2y, _epx, _epy, t);
    Point p012 = average(p01.x, p01.y, p12.x, p12.y, t);
    Point p123 = average(p12.x, p12.y, p23.x, p23.y, t);
    Point p0123 = average(p012.x, p012.y, p123.x, p123.y, t);
    
    // Construct either new start or end
    if(left!=null) left.setCurve(sp, p01, p012, p0123);
    if(right!=null) right.setCurve(p0123, p123, p23, ep);
}

/**
 * Reset this curve's end point to the given parametric location (0-1).
 */
public void setEnd(double t)  { subdivide(this, null, t); }

/**
 * Reset this curve's start point to the given parametric location (0-1).
 */
public void setStart(double t)  { subdivide(null, this, t); }

/**
 * Returns the weighted average of this point with another point.
 */
private static Point average(double p1x, double p1y, double p2x, double p2y, double t)
{
    double ax = (1-t)*p1x + t*p2x;
    double ay = (1-t)*p1y + t*p2y;
    return new Point(ax, ay);
}

/**
 * Returns a string representation of this bezier.
 */
public String toString()
{
    String fmt = "Bezier: %.2g %.2g %.2g %.2g %.2g %.2g %.2g %.2g";
    return String.format(fmt, _spx, _spy, _cp1x, _cp1y, _cp2x, _cp2y, _epx, _epy);
}

}