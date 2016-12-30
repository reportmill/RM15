/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;
import java.util.List;
import java.util.*;
import snap.gfx.*;
import snap.util.*;

/******************************************************************************
    RMPath - A graphics path (moveTo, lineTo, etc.). It provides the following functionality:
        - Allows for easy initialization with the path constructions methods (moveToPoint:, lineToPoint:, etc.).
        - Provides simple methods for path operations (stroke, fill, clip, etc.).
        
    Iterate over path like this:
    
        Point points[] = new Point[3];
        for(int i=0, iMax=path.getElmtCount(); i<iMax; i++) { int element = path.getElmt(i, points);
            switch(element) {
                case RMPath.MOVE_TO:
                case RMPath.LINE_TO:
                case RMPath.QUAD_TO:
                case RMPath.CURVE_TO:
                case RMPath.CLOSE:
            }
        }

******************************************************************************/
public class RMPath extends Shape implements Cloneable, XMLArchiver.Archivable {
    
    // Array of operators for path
    byte          _elmts[] = new byte[4];
    
    // Actual number of operators (can be less than _elements array length)
    int           _ecount;
    
    // Array of points for path
    List <Point>  _points = new Vector(8);
    
    // Rule describing how inner path perimeters are displayed when filled
    byte          _windingRule = WIND_NON_ZERO;
    
    // The rect that just contains the path
    Rect          _bounds;

    // Constants describing how inner path perimeters are filled and clipped
    public static final byte WIND_NON_ZERO = 0; // Inner perimeters drawn in same dir as outer pmtr filled
    public static final byte WIND_EVEN_ODD = 1; // Inner perimeters are alternately not covered
    
    // Constants describing path element types (MoveToPoint, LineToPoint, CurveToPoint, Close)
    public static final byte MOVE_TO = 1;
    public static final byte LINE_TO = 3;
    public static final byte QUAD_TO = 20;
    public static final byte CURVE_TO = 5;
    public static final byte CLOSE = 10;

/**
 * Creates an empty path.
 */
public RMPath()  { }

/**
 * Creates a path for the given shape.
 */
public RMPath(Shape aShape)  { appendShape(aShape); }

/**
 * Creates a path for the given path iterator.
 */
public RMPath(PathIter aPI)  { appendPathIter(aPI); }

/**
 * Adds a MoveTo element to the path for the given point.
 */
public void moveTo(Point p) { moveTo(p.getX(), p.getY()); }

/**
 * Adds a MoveTo element to the path for the given point.
 */
public void moveTo(double px, double py)  { addElmt(MOVE_TO); addPoint(px, py); }
  
/**
 * Adds a LineTo element to the path for the given point.
 */
public void lineTo(Point p) { lineTo(p.getX(), p.getY()); }

/**
 * Adds a LineTo element to the path for the given point.
 */
public void lineTo(double px, double py)  { addElmt(LINE_TO); addPoint(px, py); }

/**
 * Adds a QuadTo element to the path for the given point and control point.
 */
public void quadTo(Point cp, Point p) { quadTo(cp.getX(), cp.getY(), p.getX(), p.getY()); }

/**
 * Adds a QuadTo element to the path for the given point and control point.
 */
public void quadTo(double cpx, double cpy, double px, double py)
{
    addElmt(QUAD_TO); addPoint(cpx, cpy); addPoint(px, py);
}

/**
 * Adds a CurveTo element to the path for the given point and control points.
 */
public void curveTo(Point cp1, Point cp2, Point p)
{
    curveTo(cp1.getX(), cp1.getY(), cp2.getX(), cp2.getY(), p.getX(), p.getY());
}

/**
 * Adds a CurveTo element to the path for the given point and control points.
 */
public void curveTo(double cp1x, double cp1y, double cp2x, double cp2y, double px, double py)
{
    addElmt(CURVE_TO); addPoint(cp1x, cp1y); addPoint(cp2x, cp2y); addPoint(px, py);
}

/**
 * Adds a Close element to the given path.
 */
public void closePath()  { addElmt(CLOSE); }

/** Adds an element to the elements array. */
private void addElmt(byte anElmt)
{
    if(_ecount==_elmts.length) _elmts = Arrays.copyOf(_elmts, _elmts.length*2);
    _elmts[_ecount++] = anElmt; _bounds = null;
}

/** Adds a point to the points list. */
private void addPoint(double px, double py)  { _points.add(Point.get(px,py)); }

/**
 * Resets the current path with no elements or points.
 */
public void reset()  { _ecount = 0; _points.clear(); _bounds = null; }

/**
 * Returns a new path from a shape.
 */
public void appendShape(Shape aShape)  { appendPathIter(aShape.getPathIter(null)); }

/**
 * Returns a new path from a PathIter.
 */
public void appendPathIter(PathIter aPI)
{
    double pts[] = new double[6];
    while(aPI.hasNext()) {
        switch(aPI.getNext(pts)) {
            case MoveTo: moveTo(pts[0], pts[1]); break;
            case LineTo: lineTo(pts[0], pts[1]); break;
            case QuadTo: quadTo(pts[0], pts[1], pts[2], pts[3]); break;
            case CubicTo: curveTo(pts[0], pts[1], pts[2], pts[3], pts[4], pts[5]); break;
            case Close: closePath();
        }
    }
}

/**
 * Returns the winding rule which describes how inner path perimeters are filled and clipped.
 */
public byte getWindingRule()  { return _windingRule; }

/**
 * Sets the winding rule which describes how inner path perimeters are filled and clipped.
 */
public void setWindingRule(byte windingRule)  { _windingRule = windingRule; }

/**
 * Returns the number of elements in this path.
 */
public int getElmtCount()  { return _ecount; }

/**
 * Returns the element type at the given index.
 */
public byte getElmt(int anIndex)  { return _elmts[anIndex]; }

/**
 * Returns the last element.
 */
public byte getElmtLast()  { return _ecount>0? _elmts[_ecount-1] : 0; }

/**
 * Returns the number of points in the path.
 */
public int getPointCount()  { return _points.size(); }

/**
 * Returns the point at the given index.
 */
public Point getPoint(int anIndex)  { return _points.get(anIndex); }

/**
 * Returns the last point in the path.
 */
public Point getPointLast()  { return getPointCount()>0? getPoint(getPointCount()-1) : new Point(); }

/**
 * Returns the point count for given element type.
 */
protected int getPointCount(byte t)  { return t==RMPath.CURVE_TO? 3 : t==RMPath.QUAD_TO? 2 : t==RMPath.CLOSE? 0 : 1; }

/**
 * Returns the bounds for the path.
 */
public Rect getBounds()  { return _bounds!=null? _bounds : (_bounds=super.getBounds()); }

/**
 * Returns a PathIter for RMPath.
 */
public RMPathIter getPathIter(Transform aTrans)  { return new RMPathIter(this, aTrans); }

/**
 * Returns the point index for a given element.
 */
public int getElmtPointIndex(int anIndex)
{
    // Iterate over segments and increment point index
    int pointIndex = 0;
    for(int i=0; i<anIndex; i++)
        switch(getElmt(i)) {
            case MOVE_TO:
            case LINE_TO: pointIndex++; break;
            case QUAD_TO: pointIndex += 2; break;
            case CURVE_TO: pointIndex += 3; break;
            default: break;
        }
    
    // Return calculated point index
    return pointIndex;
}

/**
 * Returns the element index for the given point index.
 */
public int getElmtIndexForPointIndex(int index)
{
    // Iterate over segments and increment element index
    int elementIndex = 0;
    for(int pointIndex=0; pointIndex<=index; elementIndex++)
        switch(getElmt(elementIndex)) {
            case MOVE_TO:
            case LINE_TO: pointIndex++; break;
            case QUAD_TO: pointIndex += 2; break;
            case CURVE_TO: pointIndex += 3; break;
            default: break;
        }
    
    // Return calculated element index
    return elementIndex - 1;
}

/**
 * Returns the total number of points associated with a given type of path element.
 */
public int pointCountForElmt(int element)
{ 
    switch(element) {
        case MOVE_TO: 
        case LINE_TO: return 1;
        case QUAD_TO: return 2;
        case CURVE_TO: return 3;
        default: return 0;
    }
}

/**
 * Returns true of the point at pointIndex is on the path, and false if it is on the convex hull.
 */ 
public boolean pointOnPath(int pointIndex)
{
    int elIndex = getElmtIndexForPointIndex(pointIndex);
    int indexInElement = pointIndex - getElmtPointIndex(elIndex);
    
    // Only the last point is actually on the path
    int elType = getElmt(elIndex);
    int numPts = pointCountForElmt(elType);
    return indexInElement == numPts-1;
}
    
/**
 * Returns whether path has any open subpaths.
 */
public boolean isClosed()
{
    // Iterate over path
    PathIter piter = getPathIter(null);
    double pnts[] = new double[6], m0x = 0, m0y = 0, m1x = 0, m1y = 0; boolean inPath = false;
    while(piter.hasNext()) switch(piter.getNext(pnts)) {
        
        // Handle MoveTo: If we were in a path, and last move-to isn't equal, return false
        case MoveTo: if(inPath && !(MathUtils.equals(m1x,m0x) && MathUtils.equals(m1y,m0y)))
                return false;
            m0x = pnts[0]; m0y = pnts[1]; inPath = false; break; // Set last move-to point, set not in path and break
            
        // Handle LineTo
        case LineTo: m1x = pnts[0]; m1y = pnts[1]; inPath = true; break;
        case QuadTo: m1x = pnts[2]; m1y = pnts[3]; inPath = true; break;
        case CubicTo: m1x = pnts[4]; m1y = pnts[5]; inPath = true; break;
            
        // Handle Close
        case Close: inPath = false; break;
    }
    
    // Return false if we're still in path
    return !inPath;
}

/**
 * Returns a copy of the path scaled to exactly fit in the given rect.
 */
public RMPath getPathInRect(Rect aRect)
{
    // Get bounds (just return path if equal to rect)
    Rect bounds = getBounds(); if(bounds.equals(aRect)) return this;
    
    // Get scale and translation from current bounds to new bounds
    double sx = aRect.getWidth()/bounds.getWidth(); if(Double.isNaN(sx)) sx = 0;
    double sy = aRect.getHeight()/bounds.getHeight(); if(Double.isNaN(sy)) sy = 0;
    double tx = aRect.getX() - bounds.getX()*sx;
    double ty = aRect.getY() - bounds.getY()*sy;
    
    // Get transform from current bounds to new bounds and return transformed path
    Transform trans = Transform.get(sx, 0, 0, sy, tx, ty);
    RMPath clone = clone(); clone.transformBy(trans); return clone;
}

/**
 * Returns whether the given xy coordinate is inside the path.
 */
public boolean contains(double x, double y)  { return isClosed() && super.contains(x,y); }

/**
 * Returns whether path is hit by a line.
 */
public boolean intersects(RMLine aLine)  { return getHitInfo(aLine, false)!=null; }

/**
 * Returns whether path drawn with given linewidth is hit by given path.
 */
public boolean intersects(RMPath aPath, float lineWidth)
{
    // If path bounds contains rect, just return true (seems bogus!)
    if(aPath.getBounds().contains(getBounds()))
        return true;
    
    // If path bounds don't even intersect, return false
    //if(!aPath.getBounds().intersectsRect(getBounds().insetRect(-lineWidth/2))) return false;
    
    // Iterate over path segments
    PathIter piter = getPathIter(null);
    double pnts[] = new double[6], lastX = 0, lastY = 0, lastMoveX = 0, lastMoveY = 0;
    while(piter.hasNext()) switch(piter.getNext(pnts)) {

        // Handle MoveTo: Just update last point & last move-to point and break
        case MoveTo: lastX = lastMoveX = pnts[0]; lastY = lastMoveY = pnts[1]; break;

        // Handle Close: If last point is last move-to point, just break
        case Close: if(MathUtils.equals(lastX,lastMoveX) && MathUtils.equals(lastY,lastMoveY))
                break;
            
            // Otherwise, set current segment point to last move-to point and fall through to LINE_TO
            pnts[0] = lastMoveX; pnts[1] = lastMoveY;

        // Handle LineTo
        case LineTo:
            
            // If last point is same as last move-to, just see if point hits path
            if(MathUtils.equals(lastX,lastMoveX) && MathUtils.equals(lastY,lastMoveY))
                if(intersects(lastX, lastY, lineWidth))
                    return true;
            
            // If current segment point hits path, return true
            if(intersects(pnts[0], pnts[1], lineWidth))
                return true;
            
            // Create line for current path segment - If path is hit by line, return true
            RMLine line = new RMLine(lastX, lastY, pnts[0], pnts[1]);
            if(intersects(line))
                return true;
            
            // Update last point and break
            lastX = pnts[0]; lastY = pnts[1]; break;

        // Complain if anyone is using this for path with curves
        default: System.err.println("Hit by Path: Element type not implemented yet"); break;
    }

    // Return false if no path segments hit given path
    return false;
}

/**
 * Returns the hit info for the given line against this path.
 */
public RMHitInfo getHitInfo(RMLine aLine, boolean findFirstHit)
{
    // Iterate over path segments
    RMPathIter piter = getPathIter(null); RMHitInfo hitInfo = null;
    Point pts[] = new Point[3], lastPoint = new Point(), lastMoveToPoint = new Point();
    for(int i=0; piter.hasNext(); i++) switch(piter.getNext(pts)) {

        // Handle MoveTo: Update last point & last move-to point and break
        case MoveTo: lastPoint = lastMoveToPoint = pts[0]; break;

        // Handle Close:  If last point is same as last move-to point, just break
        case Close: if(lastPoint.equals(lastMoveToPoint))
               break;
            
            // Otherwise, update current segment point and fall through to LINE_TO
            pts[0] = lastMoveToPoint;

        // Handle LineTo
        case LineTo: {
            
            // Get RMLine for last point and current point and do RMLine hit detection
            RMLine line = new RMLine(lastPoint, pts[0]);
            RMHitInfo newHitInfo = aLine.getHitInfo(line);

            // If hit, see if we need to findFirstHit or just return hitInfo
            if(newHitInfo!=null) {

                // If findFirstHit, see if newHitInfo hit is closer in than current hitInfo
                if(findFirstHit) {
                    if(hitInfo==null || newHitInfo._r<hitInfo._r) {
                        hitInfo = newHitInfo; hitInfo._index = i; }
                }

                // If not findFirstHit, just return newHitInfo
                else return newHitInfo;
            }

            // Cache last point and break
            lastPoint = pts[0];
            break;
        }
            
        // If QuadTo, calculate control points for equivalent cubic and fall through
        case QuadTo: 
            pts[2] = pts[1]; 
            pts[1] = Point.get((2*pts[0].x+pts[1].x)/3, (2*pts[0].y+pts[1].y)/3);
            pts[0] = Point.get((2*pts[0].x+lastPoint.x)/3, (2*pts[0].y+lastPoint.y)/3); // fall through

        // If CubicTo, get simple RMBezier and do line/bezier hit detection
        case CubicTo: {
            
            // Get simple RMBezier for current segment and do line-bezier hit detection
            RMBezier bezier = new RMBezier(lastPoint, pts[0], pts[1], pts[2]);
            RMHitInfo newHitInfo = aLine.getHitInfo(bezier);

            // If hit, see if we need to findFirstHit or just return hitInfo
            if(newHitInfo!=null) {

                // If findFirstHit, see if newHitInfo hit is closer in than current hitInfo
                if(findFirstHit) {
                    if(hitInfo==null || newHitInfo._r<hitInfo._r) {
                        hitInfo = newHitInfo; hitInfo._index = i; }
                }

                // If not findFirstHit, just return newHitInfo
                else return newHitInfo;
            }

            // Cache last point and break
            lastPoint = pts[2]; break;
        }
    }

    // Return hit info
    return hitInfo;
}

/**
 * Converts a path into a list of RMLine/RMQuadratic/RMBezier.
 */
public List <? extends RMLine> getSegments() 
{
    // Iterate over elements
    RMPathIter piter = getPathIter(null); List <RMLine> segments = new ArrayList();
    Point pts[] = new Point[3], lastPoint = new Point(), lastMoveToPoint = lastPoint;
    while(piter.hasNext()) switch(piter.getNext(pts)) {
        
        // Handle MoveTo
        case MoveTo: lastPoint = lastMoveToPoint = pts[0]; break;

        // Handle Close: set points to last MoveTo and fall through to LineTo
        case Close: pts[0] = lastMoveToPoint;

        // Handle LineTo
        case LineTo:
            if(!lastPoint.equals(pts[0]))
                segments.add(new RMLine(lastPoint, lastPoint = pts[0]));
            break;
            
        // Handle QuadTo
        case QuadTo: segments.add(new RMQuadratic(lastPoint, pts[0], lastPoint = pts[1])); break;
        
        // Handle CurveTo
        case CubicTo: segments.add(new RMBezier(lastPoint, pts[0], pts[1], lastPoint = pts[2])); break;
    }
    
    // Return paths
    return segments;
}

/**
 * Converts a path into a list subpath lists of RMLine/RMQuadratic/RMBezier.
 */
public List <List <? extends RMLine>> getSubpathsSegments() 
{
    // Iterate over elements
    RMPathIter piter = getPathIter(null); List subpaths = new ArrayList(), segments = new ArrayList();
    Point pts[] = new Point[3], lastPoint = new Point(), lastMoveToPoint = lastPoint;
    while(piter.hasNext()) switch(piter.getNext(pts)) {
        
        // Handle MoveTo
        case MoveTo: lastPoint = lastMoveToPoint = pts[0];
            if(!segments.isEmpty()) {
                subpaths.add(segments);
                segments = new ArrayList();
            }
            break;

        // Handle Close: set points to last MoveTo and fall through to LineTo
        case Close: pts[0] = lastMoveToPoint;

        // Handle LineTo
        case LineTo: if(!lastPoint.equals(pts[0]))
                segments.add(new RMLine(lastPoint, lastPoint = pts[0])); break;
            
        // Handle QuadTo
        case QuadTo: segments.add(new RMQuadratic(lastPoint, pts[0], lastPoint = pts[1])); break;
        
        // Handle CubicTo
        case CubicTo: segments.add(new RMBezier(lastPoint, pts[0], pts[1], lastPoint = pts[2])); break;
    }
    
    // Add the last subpath
    if(!segments.isEmpty())
        subpaths.add(segments);
    
    // Return the subpaths
    return subpaths;
}

/**
 * Adds the list of segments to the path, starting with a moveto.
 */
public void addSegments(List <? extends RMLine> theSegments)
{
    // Just return if empty
    if(theSegments.size()==0) return;
    
    // Get first segment start point and do MoveTo
    Point startPoint = theSegments.get(0).getSP();
    moveTo(startPoint);
        
    // Iterate over segments
    for(int i=0, iMax=theSegments.size(); i<iMax; i++) { RMLine segment = theSegments.get(i);
        if(segment.getClass()==RMLine.class && segment.getEP().equals(startPoint))
            closePath();
        else addSegment(segment);
    }
}

/**
 * Adds the list of segments to the path, starting with a moveto.
 */
public void addSegment(RMLine aSegment)
{
    // Handle Bezier
    if(aSegment instanceof RMBezier) { RMBezier b = (RMBezier)aSegment;
        curveTo(b.getCP1(), b.getCP2(), b.getEP()); }
   
    // Handle Quadratic
    else if(aSegment instanceof RMQuadratic) { RMQuadratic q = (RMQuadratic)aSegment;
        quadTo(q.getCP1(), q.getEP()); }
   
    // Handle basic Line
    else lineTo(aSegment.getEP());
}

/**
 * Returns the hit info for the given bezier curve against this path.
 */
public RMHitInfo getHitInfo(RMBezier aBezier, boolean findFirstHit)
{
    // Iterate over path segments
    RMPathIter piter = getPathIter(null); RMHitInfo hitInfo = null;
    Point pts[] = new Point[3], lastPoint = new Point(), lastMoveToPoint = lastPoint;
    for(int i=0; piter.hasNext(); i++) switch(piter.getNext(pts)) {

        // Handle MoveTo: Just update last point & last move-to point and break
        case MoveTo: lastPoint = lastMoveToPoint = pts[0]; break;

        // Handle Close: If last point is last move-to point, just break
        case Close: if(lastPoint.equals(lastMoveToPoint))
               break;
            
            // Otherwise set current segment point to last move-to point and fall through to LineTo
            pts[0] = lastMoveToPoint;

        // Handle LineTo
        case LineTo: {
            
            // Create line for current path segment and get hit info for given beizer and current path segment
            RMLine line = new RMLine(lastPoint, pts[0]);
            RMHitInfo newHitInfo = aBezier.getHitInfo(line);

            // If hit, see if we need to findFirstHit or just return hitInfo
            if(newHitInfo!=null) {

                // If findFirstHit, see if newHitInfo hit is closer in than current hitInfo
                if(findFirstHit) {
                    if(hitInfo==null || newHitInfo._r<hitInfo._r) {
                        hitInfo = newHitInfo; hitInfo._index = i; }
                }

                // If not findFirstHit, just return newHitInfo
                else return newHitInfo;
            }

            // Update last point and break
            lastPoint = pts[0]; break;
        }

        // Handle QuadTo
        case QuadTo:
            
            // Convert quad-to to curve-to and fall through to CubicTo
            pts[2] = pts[1]; pts[1] = pts[0];

        // CubicTo
        case CubicTo: {
            
            // Create bezier for current path segment and get hit info for given bezier and current path segment
            RMBezier bezier = new RMBezier(lastPoint, pts[0], pts[1], pts[2]);
            RMHitInfo newHitInfo = aBezier.getHitInfo(bezier);

            // If hit, see if we need to findFirstHit or just return hitInfo
            if(newHitInfo!=null) {

                // If findFirstHit, see if newHitInfo hit is closer in than current hitInfo
                if(findFirstHit) {
                    if(hitInfo==null || newHitInfo._r<hitInfo._r) {
                        hitInfo = newHitInfo; hitInfo._index = i; }
                }

                // If not findFirstHit, just return newHitInfo
                else return newHitInfo;
            }

            // Update last point and break
            lastPoint = pts[2]; break;
        }
    }

    // Return hit info
    return hitInfo;
}

/**
 * Returns the handle index for a given point against this path scaled to the given rect.
 * Only returns points that are on the path, except for the control points of
 * selectedPoint (if not -1)
 */
public int handleAtPointForBounds(Point aPoint, Rect aRect, int selectedPoint, RMSize handleSize)
{
    // convert point from shape coords to path coords
    Point point = pointInPathCoordsFromPoint(aPoint, aRect);

    // Check against off-path control points of selected path first, otherwise you might never be able to select one
    if(selectedPoint != -1) {
        int offPathPoints[]=new int[2];
        int noffPathPoints=0;
        int ecount = getElmtCount();
        int eindex = getElmtIndexForPointIndex(selectedPoint);
        int elmt = getElmt(eindex);
        
        // If the selected point is one of the on path points, figure out the indices of the others
        if (pointOnPath(selectedPoint)) {
            
            // If the selected element is a curveto or quadto, the second to the last control point will be active
            if(elmt==CURVE_TO || elmt==QUAD_TO)
                offPathPoints[noffPathPoints++] = selectedPoint-1;

            // If the element following the selected element is a curveto, it's first control point will be active
            if (eindex<ecount-1 && getElmt(eindex+1)==CURVE_TO)
                offPathPoints[noffPathPoints++] = selectedPoint+1;
        }
        
        // If selected point is off-path, add it to list to check and then figure out what other point might be active
        else {
            offPathPoints[noffPathPoints++] = selectedPoint;
            
            // if selected point is first control point, check previous segment, otherwise check next segment
            if (selectedPoint == getElmtPointIndex(eindex)) {
                if((eindex>0) && (getElmt(eindex-1)==CURVE_TO))
                    offPathPoints[noffPathPoints++] = selectedPoint-2;
            }
            else {
                if((eindex<ecount-1) && (getElmt(eindex+1)==CURVE_TO)) 
                    offPathPoints[noffPathPoints++] = selectedPoint+2;
            }
        }
        
        // hit test any selected off-path handles
        for(int i=0; i<noffPathPoints; ++i)
            if(hitHandle(point, offPathPoints[i], handleSize))
                return offPathPoints[i];
    }
    
    // Check the rest of the points, but only ones that are actually on the path
    for(int i=0, iMax=getPointCount(); i<iMax; i++)
        if(hitHandle(point, i, handleSize) && pointOnPath(i))
            return i;

    // nothing hit
    return -1;
}
        
/**
 * Hit test the point (in path coords) against a given path point.
 */
private boolean hitHandle(Point aPoint, int ptIndex, RMSize handleSize)
{
    Point p = _points.get(ptIndex);
    Rect br = new Rect(p.x-handleSize.width/2, p.y-handleSize.height/2, handleSize.width, handleSize.height);
    return br.contains(aPoint.getX(), aPoint.getY());
}

/**
 * Returns the given point converted to path coords for given path bounds.
 */
public Point pointInPathCoordsFromPoint(Point aPoint, Rect aRect)
{
    Rect bounds = getBounds();
    double sx = bounds.getWidth()/aRect.getWidth();
    double sy = bounds.getHeight()/aRect.getHeight();
    double x = (aPoint.getX()-aRect.getMidX())*sx + bounds.getMidX();
    double y = (aPoint.getY()-aRect.getMidY())*sy + bounds.getMidY();
    return Point.get(x,y);
}

/**
 * Removes the last element from the path.
 */
public void removeLastElmt()
{
    // Handle specific element type
    switch(getElmt(_ecount-1)) {
        case CURVE_TO: ListUtils.removeLast(_points);
        case QUAD_TO: ListUtils.removeLast(_points);
        case MOVE_TO: case LINE_TO: ListUtils.removeLast(_points); break;
        default: break;
    }
    
    // Decrement the element count and invalidate bounds
    _ecount--; _bounds = null;
}

/**
 * Removes an element, reconnecting the elements on either side of the deleted element.
 */
public void removeElmt(int elmtIndex) 
{
    // range check
    if((elmtIndex<0) || (elmtIndex>=_ecount))
        throw new IndexOutOfBoundsException("element index " + elmtIndex + " out of bounds");
    
    // If this is the last element, nuke it
    if(elmtIndex==_ecount-1) {
        removeLastElmt();
        if(_ecount>0 && getElmt(_ecount-1)==MOVE_TO) // but don't leave stray moveto sitting around
            removeLastElmt();
        return;
    }
        
    // Get some info
    int pindex = getElmtPointIndex(elmtIndex);  // get the index to the first point for this element
    int etype = getElmt(elmtIndex);            // the type of element (MOVETO,LINETO,etc)
    int nPts = pointCountForElmt(etype);      // and how many points are associated with this element
    int nDeletedPts = nPts;                  // how many points to delete from the points array
    int nDeletedElmts = 1;                  // how many elements to delete (usually 1)
    int deletedElmntIndex = elmtIndex;     // index to delete from element array (usually same as original index)
    
    // delete all poins but the last of the next segment
    if(etype==MOVE_TO) {
        nDeletedPts = pointCountForElmt(getElmt(elmtIndex+1));
        ++deletedElmntIndex;  // delete the next element and preserve the MOVETO
    }
    
    else {
        // If next element is a curveTo, we are merging 2 curves into one, so delete points such that slopes
        // at endpoints of new curve match the starting and ending slopes of the originals.
        if(getElmt(elmtIndex+1)==CURVE_TO)
            pindex++;
        
        // Deleting the only curve or a line in a subpath can leave a stray moveto. If that happens, delete it, too
        else if ((getElmt(elmtIndex-1) == MOVE_TO) && (getElmt(elmtIndex+1)==MOVE_TO)){
          ++nDeletedElmts;
          --deletedElmntIndex;
          ++nDeletedPts;
          --pindex;
        }
    }
    
    // Remove the element
    System.arraycopy(_elmts, deletedElmntIndex+nDeletedElmts, _elmts, deletedElmntIndex,
        _ecount-deletedElmntIndex-nDeletedElmts);
    _ecount -= nDeletedElmts;
    
    // Remove the points and invalidate bounds
    ListUtils.remove(_points, pindex, pindex+nDeletedPts); _bounds = null;
}
    
/**
 * Sets the path point at the given index to the given point.
 */
public void setPoint(int index, double px, double py)  { _points.set(index, Point.get(px,py)); _bounds = null; }

/**
 * Resets the point at the given index to the given point, while preserving something.
 */
public void setPointStructured(int index, Point point)
{
    int elmtIndex = getElmtIndexForPointIndex(index);
    byte elmt = getElmt(elmtIndex);

    // If point at index is part of a curveto, perform structured set
    if(elmt == RMPath.CURVE_TO) {
        int pointIndexForElementIndex = getElmtPointIndex(elmtIndex);

        // If point index is control point 1, and previous element is a curveto, bring control point 2 of previous curveto in line
        if(index - pointIndexForElementIndex == 0) {
            if((elmtIndex-1 > 0) && (getElmt(elmtIndex-1) == RMPath.CURVE_TO)) {
                Point endPoint = getPoint(index-1), cntrlPnt2 = getPoint(index-2);
                // endpoint==point winds up putting a NaN in the path 
                if (!endPoint.equals(point)) {
                    RMSize size = new RMSize(point.getX() - endPoint.getX(), point.getY() - endPoint.getY());
                    size.normalize(); size.negate();
                    RMSize size2 = new RMSize(cntrlPnt2.getX() - endPoint.getX(), cntrlPnt2.getY() - endPoint.getY());
                    double mag = size2.getMagnitude();
                    setPoint(index-2, endPoint.getX() + size.getWidth()*mag, endPoint.getY() + size.getHeight()*mag);
                }
                else {
                    // Illustrator pops the otherControlPoint here to what it was at the 
                    // start of the drag loop.  Not sure that's much better...
                }
            }
        }

        // If point index is control point 2, and next element is a curveto, bring control point 1 of next curveto in line
        else if(index - pointIndexForElementIndex == 1) {
            if((elmtIndex+1<_ecount) && (getElmt(elmtIndex+1) == RMPath.CURVE_TO)) {
                Point endPoint = getPoint(index+1), otherControlPoint = getPoint(index+2);
                // don't normalize a point
                if (!endPoint.equals(point)) {
                    RMSize size = new RMSize(point.getX() - endPoint.x, point.getY() - endPoint.y);
                    size.normalize(); size.negate();
                    RMSize size2 = new RMSize(otherControlPoint.x - endPoint.x, otherControlPoint.y - endPoint.y);
                    double mag = size2.getMagnitude();
                    setPoint(index+2, endPoint.x+size.width*mag, endPoint.y + size.height*mag);
                }
                else { }
            }
        }

        // If point index is curve end point, move the second control point by the same amount as main point move
        else if(index - pointIndexForElementIndex == 2) {
            Point p1 = Point.get(point); p1.subtract(getPoint(index));
            Point p2 = Point.get(getPoint(index-1)); p2.add(p1);
            setPoint(index-1, p2.getX(), p2.getY());
            if((elmtIndex+1 < _ecount) && (getElmt(elmtIndex+1) == RMPath.CURVE_TO)) {
                p1 = new Point(point); p1.subtract(getPoint(index));
                p2 = new Point(getPoint(index+1)); p2.add(p1);
                setPoint(index+1, p2.getX(), p2.getY());
            }
        }
    }

    // If there is a next element and it is a curveto, move its first control point by the same amount as main point move
    else if((elmtIndex+1 < _ecount) && (getElmt(elmtIndex+1) == RMPath.CURVE_TO)) {
        Point p1 = Point.get(point); p1.subtract(getPoint(index));
        Point p2 = Point.get(getPoint(index+1)); p2.add(p1);
        setPoint(index+1, p2.getX(), p2.getY());
    }

    // Set point at index to requested point
    setPoint(index, point.getX(), point.getY());
}

/**
 * Returns a path with only moveto, lineto.
 */
public RMPath getPathFlattened()
{
    // Get a new path and point-array for path segment iteration and iterate over path segments
    PathIter piter = getPathIter(null);
    RMPath path = new RMPath(); double pnts[] = new double[6], lastx = 0, lasty = 0;
    while(piter.hasNext()) switch(piter.getNext(pnts)) {
        case MoveTo: path.moveTo(pnts[0], pnts[1]); lastx = pnts[0]; lasty = pnts[1]; break;
        case LineTo: path.lineTo(pnts[0], pnts[1]); lastx = pnts[0]; lasty = pnts[1]; break;
        case Close: path.closePath(); break;
        case QuadTo: pnts[4] = pnts[0]; pnts[5] = pnts[1]; pnts[2] = (2*pnts[0]+pnts[2])/3;
            pnts[3] = (2*pnts[1]+pnts[3])/3; pnts[0] = (2*pnts[0]+lastx)/3; pnts[1] = (2*pnts[1]+lasty)/3;
        case CubicTo:
            path.addCubicFlat(new RMBezier(lastx, lasty, pnts[0], pnts[1], pnts[2], pnts[3], pnts[4], pnts[5]));
            lastx = pnts[4]; lasty = pnts[5]; break;
    }
    
    // Return new path
    return path;
}

/**
 * Adds a bezier to the path as a series of approximated line segments.
 */
private void addCubicFlat(RMBezier aBezier)
{
    // Get simple line between bezier start/end points and if control points almost on line, return hit info for line
    RMLine bezierLine = new RMLine(aBezier.getSP(), aBezier.getEP());
    double dist1 = bezierLine.getDistanceLine(aBezier.getCP1x(), aBezier.getCP1y());
    double dist2 = bezierLine.getDistanceLine(aBezier.getCP2x(), aBezier.getCP2y());
    if(dist1<.25 && dist2<.25) {
        lineTo(bezierLine.getEP()); return; }
    
    // Subdivide bezier and add pieces
    RMBezier b1 = new RMBezier(), b2 = new RMBezier();
    aBezier.subdivide(b1, b2, .5);
    addCubicFlat(b1); addCubicFlat(b2);
}

/**
 * Transforms the points in the path by the given transform.
 */
public void transformBy(Transform aTrans)
{
    for(int i=0, iMax=_points.size(); i<iMax; i++)
        getPoint(i).transformBy(aTrans);
    _bounds = null;
}

/**
 * Standard equals implementation.
 */
public boolean equals(Object anObj)
{
    // Check identity & class and get other path
    if(anObj==this) return true;
    if(!(anObj instanceof RMPath)) return false;
    RMPath path = (RMPath)anObj;
    
    // Check ElementCount, WindingRule, Elements and Points
    if(path._ecount!=_ecount) return false;
    if(path._windingRule!=_windingRule) return false;
    if(!ArrayUtils.equals(path._elmts, _elmts)) return false;
    if(!SnapUtils.equals(path._points, _points)) return false;
    return true; // Return true since all checks passed
}

/**
 * Standard clone implementation.
 */
public RMPath clone()
{
    // Do normal Object clone, copy elements list and points list
    RMPath clone = null; try { clone = (RMPath)super.clone(); }
    catch(Exception e) { System.err.println(e); return null; }
    clone._elmts = _elmts.clone(); clone._points = new Vector(getPointCount());
    for(int i=0, iMax=getPointCount(); i<iMax; i++) clone._points.add(Point.get(getPoint(i)));
    return clone;
}

/**
 * XML archival.
 */
public XMLElement toXML(XMLArchiver anArchiver)
{
    // Get new element named path
    XMLElement e = new XMLElement("path");
    
    // Archive winding rule
    if(_windingRule!=WIND_NON_ZERO)
        e.add("wind", "even-odd");

    // Archive individual elements/points
    RMPathIter piter = getPathIter(null); Point pts[] = new Point[3];
    while(piter.hasNext()) switch(piter.getNext(pts)) {
        
        // Handle MoveTo
        case MoveTo: XMLElement move = new XMLElement("mv");
            move.add("x", pts[0].x); move.add("y", pts[0].y); e.add(move); break;
        
        // Handle LineTo
        case LineTo: XMLElement line = new XMLElement("ln");
            line.add("x", pts[0].x); line.add("y", pts[0].y); e.add(line); break;
            
        // Handle QuadTo
        case QuadTo: XMLElement quad = new XMLElement("qd");
            quad.add("cx", pts[0].x); quad.add("cy", pts[0].y);
            quad.add("x", pts[1].x); quad.add("y", pts[1].y);
            e.add(quad); break;

        // Handle CubicTo
        case CubicTo: XMLElement curve = new XMLElement("cv");
            curve.add("cp1x", pts[0].x); curve.add("cp1y", pts[0].y);
            curve.add("cp2x", pts[1].x); curve.add("cp2y", pts[1].y);
            curve.add("x", pts[2].x); curve.add("y", pts[2].y);
            e.add(curve); break;

        // Handle Close
        case Close: XMLElement close = new XMLElement("cl"); e.add(close); break;
    }
    
    return e;
}

/**
 * XML unarchival.
 */
public Object fromXML(XMLArchiver anArchiver, XMLElement anElement)
{
    // Unarchive winding rule
    if(anElement.getAttributeValue("wind", "non-zero").equals("even-odd"))
        setWindingRule(WIND_EVEN_ODD);

    // Unarchive individual elements/points
    for(int i=0, iMax=anElement.size(); i<iMax; i++) { XMLElement e = anElement.get(i);
        if(e.getName().equals("mv"))
            moveTo(e.getAttributeFloatValue("x"), e.getAttributeFloatValue("y"));
        else if(e.getName().equals("ln"))
            lineTo(e.getAttributeFloatValue("x"), e.getAttributeFloatValue("y"));
        else if(e.getName().equals("qd"))
            quadTo(e.getAttributeFloatValue("cx"), e.getAttributeFloatValue("cy"),
                e.getAttributeFloatValue("x"), e.getAttributeFloatValue("y"));
        else if(e.getName().equals("cv"))
            curveTo(e.getAttributeFloatValue("cp1x"), e.getAttributeFloatValue("cp1y"),
                e.getAttributeFloatValue("cp2x"), e.getAttributeFloatValue("cp2y"),
                e.getAttributeFloatValue("x"), e.getAttributeFloatValue("y"));
        else if(e.getName().equals("cl"))
            closePath();
    }
    
    // Return this path
    return this;
}

/**
 * A PathIter for Path.
 */
public static class RMPathIter extends PathIter {
    
    /** Creates a new PathPathIter for Path. */
    RMPathIter(RMPath aPath, Transform aTrans)  { _path = aPath; _trans = aTrans; }
    RMPath _path; Transform _trans; int _sindex, _pindex;
    
    /** Returns whether PathIter has another segement. */
    public boolean hasNext()  { return _sindex<_path._ecount; }
    
    /** Returns the next segment. */
    public Seg getNext(double coords[])
    {
        byte etype = _path._elmts[_sindex++]; Seg seg = seg(etype); int count = seg.getCount();
        for(int i=0;i<count;i++) { Point p = _path.getPoint(_pindex++); coords[i*2] = p.x; coords[i*2+1] = p.y; }
        if(_trans!=null) _trans.transform(coords, count);
        return seg;
    }
    
    /** Returns the next segment (Point coords). Not very efficient. */
    public Seg getNext(Point coords[])
    {
        double dcoords[] = new double[6]; Seg seg = getNext(dcoords); int count = seg.getCount();
        for(int i=0;i<count;i++) coords[i] = Point.get(dcoords[i*2],dcoords[i*2+1]);
        return seg;
    }

    /** Returns the PathIter.Seg type for given type. */
    Seg seg(byte t) { switch(t) { case CURVE_TO: return Seg.CubicTo; case QUAD_TO: return Seg.QuadTo;
        case MOVE_TO: return Seg.MoveTo; case LINE_TO: return Seg.LineTo; default: return Seg.Close; } }
}

}