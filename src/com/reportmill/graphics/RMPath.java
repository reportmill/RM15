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
public class RMPath extends Path implements Cloneable, XMLArchiver.Archivable {
    
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
public RMPath(Shape aShape)  { super(aShape); }

/**
 * Creates a path for the given path iterator.
 */
public RMPath(PathIter aPI)  { super(aPI); }

/**
 * Adds a MoveTo element to the path for the given point.
 */
public void moveTo(Point p) { moveTo(p.getX(), p.getY()); }

/**
 * Adds a LineTo element to the path for the given point.
 */
public void lineTo(Point p) { lineTo(p.getX(), p.getY()); }

/**
 * Returns a PathIter for RMPath.
 */
public RMPathIter getPathIter(Transform aTrans)  { return new RMPathIter(super.getPathIter(aTrans)); }

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
            pts[1] = new Point((2*pts[0].x+pts[1].x)/3, (2*pts[0].y+pts[1].y)/3);
            pts[0] = new Point((2*pts[0].x+lastPoint.x)/3, (2*pts[0].y+lastPoint.y)/3); // fall through

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
            close();
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
        curveTo(b.getCP1x(), b.getCP1y(), b.getCP2x(), b.getCP2y(), b.getEPx(), b.getEPy()); }
   
    // Handle Quadratic
    else if(aSegment instanceof RMQuadratic) { RMQuadratic q = (RMQuadratic)aSegment;
        quadTo(q.getCP1x(), q.getCP1y(), q.getEPx(), q.getEPy()); }
   
    // Handle basic Line
    else lineTo(aSegment.getEP());
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
        case Close: path.close(); break;
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
    for(int i=0, iMax=getPointCount(); i<iMax; i++) { Point p = getPoint(i); aTrans.transform(p,p);
        setPoint(i, p.x, p.y); }
}

/**
 * Standard clone implementation.
 */
public RMPath clone()  { return (RMPath)super.clone(); }

/**
 * XML archival.
 */
public XMLElement toXML(XMLArchiver anArchiver)
{
    // Get new element named path
    XMLElement e = new XMLElement("path");
    
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
            close();
    }
    
    // Return this path
    return this;
}

/**
 * A PathIter for Path.
 */
public static class RMPathIter extends PathIter {
    
    /** Creates a new PathPathIter for Path. */
    RMPathIter(PathIter aPI)  { _piter = aPI; } PathIter _piter; double _pts[] = new double[6];
    
    /** Returns whether PathIter has another segement. */
    public boolean hasNext()  { return _piter.hasNext(); }
    
    /** Returns the next segment. */
    public Seg getNext(double coords[])  { return _piter.getNext(coords); }
    
    /** Returns the next segment (Point coords). Not very efficient. */
    public Seg getNext(Point pts[])
    {
        Seg seg = getNext(_pts);
        for(int i=0;i<seg.getCount();i++) pts[i] = new Point(_pts[i*2], _pts[i*2+1]);
        return seg;
    }
}

}