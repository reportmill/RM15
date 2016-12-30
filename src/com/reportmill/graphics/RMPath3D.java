/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;
import snap.gfx.Point;
import snap.util.ListUtils;
import java.util.*;

/**
 * This class represents a path in 3D space.
 */
public class RMPath3D implements Cloneable {
    
    // The list of elements in this path
    List        _elements = new ArrayList();
    
    // The list of point3Ds in this path
    List        _points = new ArrayList();
    
    // The path center point
    RMPoint3D   _center;
    
    // The path normal vector
    RMVector3D  _normal;
    
    // The path bounding box
    RMPoint3D   _bbox[];
    
    // Cached pointers for iterating efficiently over the path
    int         _nextElementIndex = -100;
    int         _nextPointIndex = -100;
    
    // Constants for path construction element types
    public static final byte MOVE_TO = RMPath.MOVE_TO;
    public static final byte LINE_TO = RMPath.LINE_TO;
    public static final byte QUAD_TO = RMPath.QUAD_TO;
    public static final byte CURVE_TO = RMPath.CURVE_TO;
    public static final byte CLOSE = RMPath.CLOSE;

/**
 * Creates a new empty path.
 */
public RMPath3D() { }

/**
 * Creates a 3D path from a 2D path with a depth.
 */
public RMPath3D(RMPath aPath, double aDepth)
{
    // Add given 2D path at given depth
    addPath(aPath, aDepth);
}

/**
 * Returns the number of elements in the path3d.
 */
public int getElementCount()  { return _elements.size(); }

/**
 * Returns the element type at the given index. 
 */
public byte getElement(int anIndex)  { return ((Number)_elements.get(anIndex)).byteValue(); }

/**
 * Returns the number of points in the path3d.
 */
public int getPointCount()  { return _points.size(); }

/**
 * Returns the point3d at the given index.
 */
public RMPoint3D getPoint(int anIndex)  { return (RMPoint3D)_points.get(anIndex); }

/**
 * Returns the element at the given index.
 */
public byte getElement(int anIndex, RMPoint3D points[])
{
    // Get element type
    byte type = getElement(anIndex);
    
    // If null points, just return type
    if(points==null)
        return type;
    
    // If given index isn't equal to "next index" optimizer, reset next index ivar
    if(anIndex != _nextElementIndex) {
        _nextPointIndex = 0;
        for(int i=0; i<anIndex; i++) {
            byte t = ((Number)_elements.get(i)).byteValue();
            _nextPointIndex += t==MOVE_TO || t==LINE_TO? 1 : t==QUAD_TO? 2 : t==CURVE_TO? 3 : 0;
        }
    }
        
    // Handle element types
    switch(type) {
        case MOVE_TO:
        case LINE_TO: points[0] = getPoint(_nextPointIndex++); break;
        case QUAD_TO: points[0] = getPoint(_nextPointIndex++); points[1] = getPoint(_nextPointIndex++); break;
        case CURVE_TO: points[0] = getPoint(_nextPointIndex++); points[1] = getPoint(_nextPointIndex++); points[2] = getPoint(_nextPointIndex++); break;
        case CLOSE: break;
    }
        
    // Update next element pointer
    _nextElementIndex = anIndex+1;

    // Return type
    return type;
}

/**
 * Adds a moveto to the path3d with the given 3D coords.
 */
public void moveTo(double x, double y, double z)
{
    _elements.add(new Byte(MOVE_TO));
    _points.add(new RMPoint3D(x, y, z));
}

/**
 * Adds a line to the path3d with the given 3D coords.
 */
public void lineTo(double x, double y, double z)
{
    _elements.add(new Byte(LINE_TO));
    _points.add(new RMPoint3D(x, y, z));
}

/**
 * Adds a quad to to the path3d with the given 3D control point and coords.
 */
public void quadTo(double cpx, double cpy, double cpz, double x, double y, double z)
{
    _elements.add(new Byte(QUAD_TO));
    _points.add(new RMPoint3D(cpx, cpy, cpz));
    _points.add(new RMPoint3D(x, y, z));
}

/**
 * Adds a curve-to to the path3d with the given 3d coords.
 */
public void curveTo(double cp1x,double cp1y,double cp1z,double cp2x,double cp2y,double cp2z,double x,double y,double z)
{
    _elements.add(new Byte(CURVE_TO));
    _points.add(new RMPoint3D(cp1x, cp1y, cp1z));
    _points.add(new RMPoint3D(cp2x, cp2y, cp2z));
    _points.add(new RMPoint3D(x, y, z));
}

/**
 * Adds a close element to the path3d.
 */
public void close()  { _elements.add(new Byte(CLOSE)); }

/**
 * Adds a 2D path to the path3D at the given depth.
 */
public void addPath(RMPath aPath, double aDepth)
{
    // Iterate over elements in given path
    RMPath.RMPathIter piter = aPath.getPathIter(null); Point pts[] = new Point[3];
    for(int i=0; piter.hasNext(); i++) switch(piter.getNext(pts)) {
        case MoveTo: if(i+1<aPath.getElmtCount() && aPath.getElmt(i+1)!=MOVE_TO)
                moveTo(pts[0].x, pts[0].y, aDepth); break;
        case LineTo: lineTo(pts[0].x, pts[0].y, aDepth); break;
        case QuadTo: quadTo(pts[0].x, pts[0].y, aDepth, pts[1].x, pts[1].y, aDepth); break;
        case CubicTo: curveTo(pts[0].x, pts[0].y, aDepth, pts[1].x, pts[1].y, aDepth,
                    pts[2].x, pts[2].y, aDepth); break;
        case Close: close(); break;
    }
}

/**
 * Returns the center point of the path.
 */
public RMPoint3D getCenter()
{
    // If center point hasn't been cached, calculate and cache it
    if(_center==null) {
        RMPoint3D bbox[] = getBBox();
        _center = new RMPoint3D(bbox[0].x + (bbox[1].x-bbox[0].x)/2, bbox[0].y + (bbox[1].y-bbox[0].y)/2,
                                bbox[0].z + (bbox[1].z-bbox[0].z)/2);
    }
    
    // Return center point
    return _center;
}

/**
 * Sets the center point of the path.
 */
public void setCenter(RMPoint3D aPoint)  { _center = aPoint; }

/**
 * Returns the normal of the path3d. Right hand rule for clockwise/counter-clockwise defined polygons.
 */
public RMVector3D getNormal()
{
    // If normal hasn't been calculated
    if(_normal==null) {
        
        // Create a new normal vector
        _normal = new RMVector3D(0, 0, 0);
        
        // Calculate least-square-fit normal.
        // This works for either convex or concave polygons.
        // Reference is 
        //   Newell's Method for Computing the Plane Equation of a Polygon.
        //   Graphics Gems III, David Kirk (Ed.), AP Professional, 1992.

        for(int pc=getPointCount(), i=0; i<pc; i++) {
            RMPoint3D cur = getPoint(i), next = getPoint((i+1)%pc);
            _normal.x += (cur.y - next.y) * (cur.z + next.z);
            _normal.y += (cur.z - next.z) * (cur.x + next.x);
            _normal.z += (cur.x - next.x) * (cur.y + next.y);
        }
 
        // Normalize the result
        _normal.normalize();
        // swap the sign of the normal so it matches the right hand rule
        _normal.negate();
    }

    // Return normal
    return _normal;
}

/**
 * Returns the distance from a point to the plane of this polygon.
 */
public double getDistance(RMPoint3D aPoint)
{
    RMVector3D normal = getNormal();
    RMPoint3D point = getPoint(0);
    double d = -normal.x*point.x - normal.y*point.y - normal.z*point.z;
    double dist = normal.x*aPoint.x + normal.y*aPoint.y + normal.z*aPoint.z + d;
    return Math.abs(dist)<.01? 0 : dist;
}

/**
 * Reverses the path3d.
 */
public void reverse()  { reverse(0, null, null); }

/**
 * Reverse method worker method.
 */
private void reverse(int element, RMPoint3D lastPoint, RMPoint3D lastMoveTo)
{
    // Simply return if element is beyond bounds
    if(element==getElementCount()) {
        _elements.clear();
        _points.clear();
        _normal = null;
        return;
    }
    
    // Get info for this element
    RMPoint3D points[] = new RMPoint3D[3], lp = null, lmt = lastMoveTo;
    int type = getElement(element, points);
    switch(type) {
        case MOVE_TO: lmt = points[0];
        case LINE_TO: lp = points[0]; break;
        case QUAD_TO: lp = points[1]; break;
        case CURVE_TO: lp = points[2]; break;
        case CLOSE: lp = lastMoveTo;
    }

    // Recursively add following elements before this one
    byte nextType = element+1<getElementCount()? getElement(element+1,null) : -1;
    reverse(element+1, lp, lmt);
    
    // Add reverse element to path for current element
    switch(type) {
        case MOVE_TO:
            if(nextType!=MOVE_TO)
                close();
            break;
        case LINE_TO:
            if(!lastPoint.equals(lastMoveTo))
                lineTo(lastPoint.x, lastPoint.y, lastPoint.z);
            break;
        case QUAD_TO:
            quadTo(points[0].x, points[0].y, points[0].z, lastPoint.x, lastPoint.y, lastPoint.z);
            break;
        case CURVE_TO:
            curveTo(points[1].x, points[1].y, points[1].z, points[0].x, points[0].y, points[0].z,
                    lastPoint.x, lastPoint.y, lastPoint.z);
            break;
        case CLOSE:
            moveTo(lastMoveTo.x, lastMoveTo.y, lastMoveTo.z);
            lineTo(lastPoint.x, lastPoint.y, lastPoint.z);
            break;
    }
}

/**
 * Transforms the path by the given transform3d.
 */
public void transform(RMTransform3D xform)
{
    // Add center point to _points list
    _points.add(getCenter());
    
    // Transform points
    for(int i=0, iMax=getPointCount(); i<iMax; i++)
        getPoint(i).transform(xform);
    
    // Remove center point from _points list and reset normal
    _points.remove(_points.size()-1);
    _normal = null;
    _bbox=null;
}

/**
 * Transforms the path so the normal is aligned with the given vector.
 */
public void align(RMVector3D aVector) 
{
    // The dot product of vector and path's normal gives the angle in the rotation plane by which to rotate the path
    RMVector3D norm = getNormal();
    
    // Get angle between normal and given vector
    double angle = norm.getAngleBetween(aVector);
    
    // If angle, transform path
    if(angle != 0) {
        
        // The axis about which to rotate the path is given by the cross product of the two vectors
        RMVector3D rotAxis = norm.getCrossProduct(aVector);
        RMTransform3D xform = new RMTransform3D();
        RMTransform3D rotMatrix = new RMTransform3D();

        // create the rotation matrix
        rotMatrix.rotate(rotAxis, angle);
        
        // The point of rotation is located at the shape's center
        RMPoint3D rotOrigin = getCenter();
        
        xform.translate(-rotOrigin.x, -rotOrigin.y, -rotOrigin.z);
        xform.multiply(rotMatrix);
        xform.translate(rotOrigin.x, rotOrigin.y, rotOrigin.z);
        
        transform(xform);
    }
}

/**
 * Returns a path for the path3d.
 */
public RMPath getPath()
{
    // Create new path
    RMPath path = new RMPath();
    
    // Create points array for iterating over this path3d
    RMPoint3D points[] = new RMPoint3D[3];
    
    // Iterate over this path3d
    for(int i=0, iMax=getElementCount(); i<iMax; i++) {
        
        // Get type
        int type = getElement(i, points);
        
        // Do 2d operation
        switch(type) {
            case MOVE_TO: path.moveTo(points[0].x, points[0].y); break;
            case LINE_TO: path.lineTo(points[0].x, points[0].y); break;
            case QUAD_TO: path.quadTo(points[0].x, points[0].y, points[1].x, points[1].y); break;
            case CURVE_TO:
                path.curveTo(points[0].x, points[0].y, points[1].x, points[1].y, points[2].x, points[2].y);
                break;
            case CLOSE: path.closePath();
        }
    }
    
    // Draw surface normals - handy for debugging
    //RMPoint3D c=getCenter();
    //RMVector3D norm=getNormal();
    //path.moveTo(c.x, c.y); path.lineTo(c.x+norm.x*20, c.y+norm.y*20);
    
    // Return path
    return path;
}

/**
 * UNUSED!!! Returns wether the given path is behind (ASCEND) or in front (DESCEND) of this path.
 */
/*public int compare(Object anObj)
{
    // Cast other object as a path3d
    RMPath3D path = (RMPath3D)anObj;
    
    // If receiver max z is less than other path min z, return ORDER_ASCEND
    if(getZMax()<=path.getZMin()) return RMSort.ORDER_ASCEND;
    
    // If receiver min z is greater than otehr path max z, return ORDER_DESCEND
    if(getZMin()>=path.getZMax()) return RMSort.ORDER_DESCEND;
    
    // If receiver is in front or back of aPath's plane, return that
    int comp = comparePlane(path); if(comp!=RMSort.ORDER_SAME) return comp;
    
    // If aPath is in front or back of receiver, return that
    comp = path.comparePlane(this); if(comp!=RMSort.ORDER_SAME) return comp;
    return RMSort.ORDER_SAME;
}*/

/**
 * Returns whether receiver is in front (ORDER_ASCEND) or aPath in front (ORDER_DESCEND).
 * Returns (ORDER_SAME) if the two paths are coplanar, or (ORDER_INDETERMINATE) if they intersect.
 */
public int comparePlane(RMPath3D aPath)
{
    double d1 = 0;
    for(int i=0, iMax=getPointCount(); i<iMax; i++) {
        double d2 = aPath.getDistance(getPoint(i));
        
        // If d1 is uninitialized, initialize it
        if(d1==0)
            d1 = d2;
        
        // If d1 is initialized and d2 is nonzero and distances are opposite, return INDETERMINATE 
        else if(d2!=0 && d1*d2<0)
            return 2; //RMSort.ORDER_INDETERMINATE;
    }
    
    // If all of receiver's points are on aPath's plane, return SAME
    if(d1==0)
        return 0;//RMSort.ORDER_SAME;
    
    // If all points are above aPath's plane, return ORDER_ASCEND (receiver in front), otherwise ORDER_DESCEND
    return d1>0? -1 : 1; //RMSort.ORDER_ASCEND : RMSort.ORDER_DESCEND;
}

/**
 * Returns the bounding box for the path as {min,max}.
 */
public RMPoint3D[] getBBox()
{
    if (_bbox==null) {
        _bbox = new RMPoint3D[2];
        _bbox[0] = new RMPoint3D(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
        _bbox[1] = new RMPoint3D(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);
        
        for(int i=0, iMax=getPointCount(); i<iMax; i++) {
            RMPoint3D pt = getPoint(i);
            _bbox[0].x = Math.min(_bbox[0].x, pt.x);
            _bbox[0].y = Math.min(_bbox[0].y, pt.y);
            _bbox[0].z = Math.min(_bbox[0].z, pt.z);
            _bbox[1].x = Math.max(_bbox[1].x, pt.x);
            _bbox[1].y = Math.max(_bbox[1].y, pt.y);
            _bbox[1].z = Math.max(_bbox[1].z, pt.z);
        }
    }
    return _bbox;
}

/**
 * Returns the max X for the path.
 */
public double getXMin()  { return getBBox()[0].x; }

/**
 * Returns the max X for the path.
 */
public double getXMax()  { return getBBox()[1].x; }

/**
 * Returns the max Y for the path.
 */
public double getYMin()  { return getBBox()[0].y; }

/**
 * Returns the max Y for the path.
 */
public double getYMax()  { return getBBox()[1].y; }

/**
 * Returns the max Z for the path.
 */
public double getZMin()  { return getBBox()[0].z; }

/**
 * Returns the max Z for the path.
 */
public double getZMax()  { return getBBox()[1].z; }

/**
 * Standard clone implementation.
 */
public Object clone()
{
    RMPath3D clone = new RMPath3D();  // Create new path 3d
    clone._elements = ListUtils.clone(_elements);  // Clone elements (really unnecessary, since Bytes are immutable)
    clone._points = ListUtils.cloneDeep(_points);  // Clone points deep
    return clone;
}

}