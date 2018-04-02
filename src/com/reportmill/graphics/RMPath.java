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
    Transform trans = new Transform(sx, 0, 0, sy, tx, ty);
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
    PathIter piter = getPathIter(null); RMHitInfo hitInfo = null;
    double pts[] = new double[6], lastX = 0, lastY = 0, lastMoveToX = 0, lastMoveToY = 0;
    for(int i=0; piter.hasNext(); i++) switch(piter.getNext(pts)) {

        // Handle MoveTo: Update last point & last move-to point and break
        case MoveTo: lastX = lastMoveToX = pts[0]; lastY = lastMoveToY = pts[1]; break;

        // Handle Close:  If last point is same as last move-to point, just break
        case Close: if(Point.equals(lastX, lastY, lastMoveToX, lastMoveToY))
               break;
            
            // Otherwise, update current segment point and fall through to LINE_TO
            pts[0] = lastMoveToX; pts[1] = lastMoveToY;

        // Handle LineTo
        case LineTo: {
            
            // Get RMLine for last point and current point and do RMLine hit detection
            RMLine line = new RMLine(lastX, lastY, pts[0], pts[1]);
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
            lastX = pts[0]; lastY = pts[1]; break;
        }
            
        // If QuadTo, calculate control points for equivalent cubic and fall through
        case QuadTo: 
            pts[4] = pts[2]; pts[5] = pts[3];
            pts[2] = (2*pts[0]+pts[2])/3; pts[3] = (2*pts[1]+pts[3])/3;
            pts[0] = (2*pts[0]+lastX)/3; pts[1] = (2*pts[1]+lastY)/3; // fall through

        // If CubicTo, get simple RMBezier and do line/bezier hit detection
        case CubicTo: {
            
            // Get simple RMBezier for current segment and do line-bezier hit detection
            RMBezier bezier = new RMBezier(lastX, lastY, pts[0], pts[1], pts[2], pts[3], pts[4], pts[5]);
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
            lastX = pts[4]; lastY = pts[5]; break;
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
    PathIter piter = getPathIter(null); List subpaths = new ArrayList(), segments = new ArrayList();
    double pts[] = new double[6], lastX = 0, lastY = 0, lastMoveToX = 0, lastMoveToY = 0;
    while(piter.hasNext()) switch(piter.getNext(pts)) {
        
        // Handle MoveTo
        case MoveTo: lastX = lastMoveToX = pts[0]; lastY = lastMoveToY = pts[1];
            if(!segments.isEmpty()) {
                subpaths.add(segments);
                segments = new ArrayList();
            }
            break;

        // Handle Close: set points to last MoveTo and fall through to LineTo
        case Close: pts[0] = lastMoveToX; pts[1] = lastMoveToY;

        // Handle LineTo
        case LineTo: if(!Point.equals(lastX, pts[0], lastY, pts[1]))
                segments.add(new RMLine(lastX, lastY, lastX = pts[0], lastY = pts[1])); break;
            
        // Handle QuadTo
        case QuadTo: segments.add(new RMQuadratic(lastX, lastY, pts[0], pts[1], lastX = pts[2], lastY = pts[3])); break;
        
        // Handle CubicTo
        case CubicTo: segments.add(new RMBezier(lastX, lastY, pts[0], pts[1], pts[2], pts[3],
            lastX=pts[4], lastY=pts[5])); break;
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
    PathIter piter = getPathIter(null); double pts[] = new double[6];
    while(piter.hasNext()) switch(piter.getNext(pts)) {
        
        // Handle MoveTo
        case MoveTo: XMLElement move = new XMLElement("mv");
            move.add("x", pts[0]); move.add("y", pts[1]); e.add(move); break;
        
        // Handle LineTo
        case LineTo: XMLElement line = new XMLElement("ln");
            line.add("x", pts[0]); line.add("y", pts[1]); e.add(line); break;
            
        // Handle QuadTo
        case QuadTo: XMLElement quad = new XMLElement("qd");
            quad.add("cx", pts[0]); quad.add("cy", pts[1]);
            quad.add("x", pts[2]); quad.add("y", pts[3]);
            e.add(quad); break;

        // Handle CubicTo
        case CubicTo: XMLElement curve = new XMLElement("cv");
            curve.add("cp1x", pts[0]); curve.add("cp1y", pts[1]);
            curve.add("cp2x", pts[2]); curve.add("cp2y", pts[3]);
            curve.add("x", pts[4]); curve.add("y", pts[5]);
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

}