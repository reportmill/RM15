/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;
import snap.gfx.*;
import snap.util.SnapUtils;
import java.util.*;

/**
 * This class represents a path in 3D space.
 */
public class Path3D implements Cloneable {
    
    // The list of elements in this path
    List <Seg>      _elements = new ArrayList();
    
    // The list of point3Ds in this path
    List <Point3D>  _points = new ArrayList();
    
    // The path center point
    Point3D         _center;
    
    // The path normal vector
    Vector3D        _normal;
    
    // The path bounding box
    Point3D         _bbox[];
    
    // Cached pointers for iterating efficiently over the path
    int             _nextElementIndex = -100;
    int             _nextPointIndex = -100;
    
    // Constants for path construction element types
    public static final Seg MOVE_TO = Path.MoveTo;
    public static final Seg LINE_TO = Path.LineTo;
    public static final Seg QUAD_TO = Path.QuadTo;
    public static final Seg CURVE_TO = Path.CubicTo;
    public static final Seg CLOSE = Path.Close;

/**
 * Creates a Path3D.
 */
public Path3D() { }

/**
 * Creates a Path3D from a 2D path with a depth.
 */
public Path3D(Path aPath, double aDepth)  { addPath(aPath, aDepth); }

/**
 * Returns the number of elements in the path3d.
 */
public int getElementCount()  { return _elements.size(); }

/**
 * Returns the element type at the given index. 
 */
public Seg getElement(int anIndex)  { return _elements.get(anIndex); }

/**
 * Returns the number of points in the path3d.
 */
public int getPointCount()  { return _points.size(); }

/**
 * Returns the point3d at the given index.
 */
public Point3D getPoint(int anIndex)  { return _points.get(anIndex); }

/**
 * Returns the element at the given index.
 */
public Seg getElement(int anIndex, Point3D pts[])
{
    // Get element type (if no points, just return type)
    Seg type = getElement(anIndex); if(pts==null) return type;
    
    // If given index isn't equal to "next index" optimizer, reset next index ivar
    if(anIndex != _nextElementIndex) {
        _nextPointIndex = 0;
        for(int i=0; i<anIndex; i++) {
            Seg t = _elements.get(i);
            _nextPointIndex += t==MOVE_TO || t==LINE_TO? 1 : t==QUAD_TO? 2 : t==CURVE_TO? 3 : 0;
        }
    }
        
    // Handle element types
    switch(type) {
        case MoveTo:
        case LineTo: pts[0] = getPoint(_nextPointIndex++); break;
        case QuadTo: pts[0] = getPoint(_nextPointIndex++); pts[1] = getPoint(_nextPointIndex++); break;
        case CubicTo: pts[0] = getPoint(_nextPointIndex++); pts[1] = getPoint(_nextPointIndex++);
            pts[2] = getPoint(_nextPointIndex++); break;
        case Close: break;
    }
        
    // Update next element pointer and return
    _nextElementIndex = anIndex+1;
    return type;
}

/**
 * Adds a moveto to the path3d with the given 3D coords.
 */
public void moveTo(double x, double y, double z)
{
    _elements.add(MOVE_TO);
    _points.add(new Point3D(x, y, z));
}

/**
 * Adds a line to the path3d with the given 3D coords.
 */
public void lineTo(double x, double y, double z)
{
    _elements.add(LINE_TO);
    _points.add(new Point3D(x, y, z));
}

/**
 * Adds a quad to to the path3d with the given 3D control point and coords.
 */
public void quadTo(double cpx, double cpy, double cpz, double x, double y, double z)
{
    _elements.add(QUAD_TO);
    _points.add(new Point3D(cpx, cpy, cpz));
    _points.add(new Point3D(x, y, z));
}

/**
 * Adds a curve-to to the path3d with the given 3d coords.
 */
public void curveTo(double cp1x,double cp1y,double cp1z,double cp2x,double cp2y,double cp2z,double x,double y,double z)
{
    _elements.add(CURVE_TO);
    _points.add(new Point3D(cp1x, cp1y, cp1z));
    _points.add(new Point3D(cp2x, cp2y, cp2z));
    _points.add(new Point3D(x, y, z));
}

/**
 * Adds a close element to the path3d.
 */
public void close()  { _elements.add(CLOSE); }

/**
 * Adds a 2D path to the path3D at the given depth.
 */
public void addPath(Path aPath, double aDepth)
{
    // Iterate over elements in given path
    PathIter piter = aPath.getPathIter(null); double pts[] = new double[6];
    for(int i=0; piter.hasNext(); i++) switch(piter.getNext(pts)) {
        case MoveTo: if(i+1<aPath.getSegCount() && aPath.getSeg(i+1)!=Seg.MoveTo)
                moveTo(pts[0], pts[1], aDepth); break;
        case LineTo: lineTo(pts[0], pts[1], aDepth); break;
        case QuadTo: quadTo(pts[0], pts[1], aDepth, pts[2], pts[3], aDepth); break;
        case CubicTo: curveTo(pts[0], pts[1], aDepth, pts[2], pts[3], aDepth, pts[4], pts[5], aDepth); break;
        case Close: close(); break;
    }
}

/**
 * Returns the center point of the path.
 */
public Point3D getCenter()
{
    // If center point hasn't been cached, calculate and cache it
    if(_center==null) {
        Point3D bbox[] = getBBox();
        _center = new Point3D(bbox[0].x + (bbox[1].x-bbox[0].x)/2, bbox[0].y + (bbox[1].y-bbox[0].y)/2,
                                bbox[0].z + (bbox[1].z-bbox[0].z)/2);
    }
    
    // Return center point
    return _center;
}

/**
 * Sets the center point of the path.
 */
public void setCenter(Point3D aPoint)  { _center = aPoint; }

/**
 * Returns the normal of the path3d. Right hand rule for clockwise/counter-clockwise defined polygons.
 */
public Vector3D getNormal()
{
    // If normal hasn't been calculated
    if(_normal==null) {
        
        // Create a new normal vector
        _normal = new Vector3D(0, 0, 0);
        
        // Calculate least-square-fit normal. Works for either convex or concave polygons.
        // Reference is Newell's Method for Computing the Plane Equation of a Polygon.
        //   Graphics Gems III, David Kirk (Ed.), AP Professional, 1992.
        for(int pc=getPointCount(), i=0; i<pc; i++) {
            Point3D cur = getPoint(i), next = getPoint((i+1)%pc);
            _normal.x += (cur.y - next.y) * (cur.z + next.z);
            _normal.y += (cur.z - next.z) * (cur.x + next.x);
            _normal.z += (cur.x - next.x) * (cur.y + next.y);
        }
 
        // Normalize the result
        _normal.normalize();
        _normal.negate(); // swap sign of normal so it matches right hand rule
    }

    // Return normal
    return _normal;
}

/**
 * Returns the distance from a point to the plane of this polygon.
 */
public double getDistance(Point3D aPoint)
{
    Vector3D normal = getNormal();
    Point3D point = getPoint(0);
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
private void reverse(int element, Point3D lastPoint, Point3D lastMoveTo)
{
    // Simply return if element is beyond bounds
    if(element==getElementCount()) {
        _elements.clear(); _points.clear(); _normal = null; return; }
    
    // Get info for this element
    Point3D pts[] = new Point3D[3], lp = null, lmt = lastMoveTo;
    Seg type = getElement(element, pts);
    switch(type) {
        case MoveTo: lmt = pts[0];
        case LineTo: lp = pts[0]; break;
        case QuadTo: lp = pts[1]; break;
        case CubicTo: lp = pts[2]; break;
        case Close: lp = lastMoveTo;
    }

    // Recursively add following elements before this one
    Seg nextType = element+1<getElementCount()? getElement(element+1,null) : null;
    reverse(element+1, lp, lmt);
    
    // Add reverse element to path for current element
    switch(type) {
        case MoveTo:
            if(nextType!=MOVE_TO)
                close();
            break;
        case LineTo:
            if(!lastPoint.equals(lastMoveTo))
                lineTo(lastPoint.x, lastPoint.y, lastPoint.z);
            break;
        case QuadTo: quadTo(pts[0].x, pts[0].y, pts[0].z, lastPoint.x, lastPoint.y, lastPoint.z); break;
        case CubicTo:
            curveTo(pts[1].x, pts[1].y, pts[1].z, pts[0].x, pts[0].y, pts[0].z, lastPoint.x, lastPoint.y, lastPoint.z);
            break;
        case Close:
            moveTo(lastMoveTo.x, lastMoveTo.y, lastMoveTo.z);
            lineTo(lastPoint.x, lastPoint.y, lastPoint.z);
            break;
    }
}

/**
 * Transforms the path by the given transform3d.
 */
public void transform(Transform3D xform)
{
    // Add center point to _points list
    _points.add(getCenter());
    
    // Transform points
    for(int i=0, iMax=getPointCount(); i<iMax; i++)
        getPoint(i).transform(xform);
    
    // Remove center point from _points list and reset normal
    _points.remove(_points.size()-1); _normal = null; _bbox = null;
}

/**
 * Transforms the path so the normal is aligned with the given vector.
 */
public void align(Vector3D aVector) 
{
    // The dot product of vector and path's normal gives the angle in the rotation plane by which to rotate the path
    Vector3D norm = getNormal();
    
    // Get angle between normal and given vector
    double angle = norm.getAngleBetween(aVector);
    
    // If angle, transform path
    if(angle != 0) {
        
        // The axis about which to rotate the path is given by the cross product of the two vectors
        Vector3D rotAxis = norm.getCrossProduct(aVector);
        Transform3D xform = new Transform3D();
        Transform3D rotMatrix = new Transform3D();

        // create the rotation matrix
        rotMatrix.rotate(rotAxis, angle);
        
        // The point of rotation is located at the shape's center
        Point3D rotOrigin = getCenter();
        
        xform.translate(-rotOrigin.x, -rotOrigin.y, -rotOrigin.z);
        xform.multiply(rotMatrix);
        xform.translate(rotOrigin.x, rotOrigin.y, rotOrigin.z);
        
        transform(xform);
    }
}

/**
 * Returns a path for the path3d.
 */
public Path getPath()
{
    // Create new path
    Path path = new Path();
    
    // Iterate over this path3d
    Point3D pts[] = new Point3D[3];
    for(int i=0, iMax=getElementCount(); i<iMax; i++) { Seg type = getElement(i, pts);
        
        // Do 2d operation
        switch(type) {
            case MoveTo: path.moveTo(pts[0].x, pts[0].y); break;
            case LineTo: path.lineTo(pts[0].x, pts[0].y); break;
            case QuadTo: path.quadTo(pts[0].x, pts[0].y, pts[1].x, pts[1].y); break;
            case CubicTo: path.curveTo(pts[0].x, pts[0].y, pts[1].x, pts[1].y, pts[2].x, pts[2].y); break;
            case Close: path.close();
        }
    }
    
    // Draw surface normals - handy for debugging
    //RMPoint3D c = getCenter(); RMVector3D n = getNormal(); path.moveTo(c.x,c.y); path.lineTo(c.x+n.x*20,c.y+.y*20);
    
    // Return path
    return path;
}

/**
 * UNUSED!!! Returns wether the given path is behind (ASCEND) or in front (DESCEND) of this path.
 */
/*public int compare(Object anObj)
{
    // Cast other object as a path3d
    Path3D path = (Path3D)anObj;
    
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
public int comparePlane(Path3D aPath)
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
public Point3D[] getBBox()
{
    if (_bbox==null) {
        _bbox = new Point3D[2];
        _bbox[0] = new Point3D(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
        _bbox[1] = new Point3D(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);
        
        for(int i=0, iMax=getPointCount(); i<iMax; i++) { Point3D pt = getPoint(i);
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
    Path3D clone = new Path3D();
    clone._elements = SnapUtils.clone(_elements);
    clone._points = SnapUtils.cloneDeep(_points);
    return clone;
}

/**
 * Creates and returns a list of paths in 3D for a given 2D path and extrusion. 
 * Also can take into account the width of a stroke applied to the side (extrusion) panels.
 */
public static List <Path3D> getPaths(Path aPath, double z1, double z2, double strokeWidth)
{
    // Create list to hold paths
    List <Path3D> paths = new ArrayList();

    // Declare local variable for back face
    Path3D back = null;
    
    // If path is closed, create path3d for front from aPath and z1
    if(aPath.isClosed()) {
        
        // Create path3d for front and back
        Path3D front = new Path3D(aPath, z1);
        back = new Path3D(aPath, z2);
        
        // Add front to paths list
        paths.add(front);
    
        // If front is pointing wrong way, reverse it
        if(front.getNormal().isAway(new Vector3D(0, 0, -1), true))
            front.reverse();
        
        // Otherwise, reverse back
        else {
            back.reverse();
            aPath = back.getPath();
        }
    }
    
    // Make room for path stroke
    z1 += strokeWidth;
    z2 -= strokeWidth;
    
    // Iterate over path elements
    PathIter piter = aPath.getPathIter(null);
    double pts[] = new double[6], lastX = 0, lastY = 0, lastMoveX = 0, lastMoveY = 0;
    while(piter.hasNext()) switch(piter.getNext(pts)) {

        // MoveTo
        case MoveTo: lastX = lastMoveX = pts[0]; lastY = lastMoveY = pts[1]; break;
        
        // LineTo
        case LineTo: {
            if(Point.equals(lastX,lastY,pts[0],pts[1])) continue;
            Path3D path = new Path3D(); path.moveTo(lastX, lastY, z1);
            path.lineTo(pts[0], pts[1], z1);
            path.lineTo(pts[0], pts[1], z2);
            path.lineTo(lastX, lastY, z2);
            path.close();
            double x = lastX + (pts[0] - lastX)/2;
            double y = lastY + (pts[1] - lastY)/2;
            path.setCenter(new Point3D(x, y, z2/2));
            paths.add(path);
            lastX = pts[0]; lastY = pts[1];
        } break;
            
        // QuadTo
        case QuadTo: {
            Path3D path = new Path3D(); path.moveTo(lastX, lastY, z1);
            path.quadTo(pts[0], pts[1], z1, pts[2], pts[3], z1);
            path.lineTo(pts[4], pts[5], z2);
            path.quadTo(pts[0], pts[1], z2, lastX, lastY, z2);
            path.close();
            double x = lastX + (pts[2] - lastX)/2;
            double y = lastY + (pts[3] - lastY)/2;
            path.setCenter(new Point3D(x, y, z2/2));
            paths.add(path);
            lastX = pts[2]; lastY = pts[3];
        } break;
            
        // CubicTo
        case CubicTo: {
            Path3D path = new Path3D(); path.moveTo(lastX, lastY, z1);
            path.curveTo(pts[0], pts[1], z1, pts[2], pts[3], z1, pts[4], pts[5], z1);
            path.lineTo(pts[4], pts[5], z2);
            path.curveTo(pts[2], pts[3], z2, pts[0], pts[1], z2, lastX, lastY, z2);
            path.close();
            double x = lastX + (pts[4] - lastX)/2;
            double y = lastY + (pts[5] - lastY)/2;
            path.setCenter(new Point3D(x, y, z2/2));
            paths.add(path);
            lastX = pts[4]; lastY = pts[5];
        } break;
        
        // Close
        case Close: {
            Path3D path = new Path3D(); path.moveTo(lastX, lastY, z1);
            path.lineTo(lastMoveX, lastMoveY, z1);
            path.lineTo(lastMoveX, lastMoveY, z2);
            path.lineTo(lastX, lastY, z2);
            path.close();
            double x = lastX + (lastMoveX - lastX)/2;
            double y = lastY + (lastMoveY - lastY)/2;
            path.setCenter(new Point3D(x, y, z2/2));
            paths.add(path);
        } break;
    }
    
    // Add back face to paths
    if(back != null)
        paths.add(back);
    
    // Return paths
    return paths;
}

}