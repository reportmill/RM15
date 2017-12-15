/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import com.reportmill.graphics.*;
import snap.gfx.*;
import snap.gfx.PathIter.Seg;
import snap.util.*;

/**
 * This class is an RMShape subclass that encapsulates an arbitrary path. 
 */
public class RMPolygonShape extends RMParentShape {
    
    // The explicit path associated with this shape
    RMPath        _path;
    
    // The path point handle hit by current mouse down
    static public int  _selectedPointIndex = 0;

/**
 * Creates a new empty polygon shape.
 */
public RMPolygonShape() { }

/**
 * Creates a new polygon shape for the given path.
 */
public RMPolygonShape(Shape aShape)  { this(); _path = new RMPath(aShape); }

/**
 * Returns the path for this polygon shape.
 */
public RMPath getPath()  { return _path.getPathInRect(getBoundsInside()); }

/**
 * Sets the path for this polygon shape.
 */
public void setPath(RMPath aPath)  { _path = aPath; }

/**
 * Replace the polygon's current path with a new path, adjusting the shape's bounds to match the new path.
 */
public void resetPath(RMPath newPath)
{
    // Get the transform to parent shape coords
    Transform toParentXF = getTransform();  

    // Set the new path and new size
    setPath(newPath);
    Rect bounds = newPath.getBounds();
    setSize(bounds.getWidth(), bounds.getHeight());
        
    // Transform to parent for new x & y
    Rect boundsInParent = bounds.clone(); toParentXF.transform(boundsInParent);
    setFrameXY(boundsInParent.getXY());
}

/**
 * Editor method - indicates that this shape can be super selected.
 */
public boolean superSelectable() { return true; }

/**
 * Handles painting a polygon shape.
 */
public void paintShape(Painter aPntr)
{
    // Do normal shape painting (if not super-selected, just return)
    super.paintShape(aPntr); if(!RMShapePaintProps.isSuperSelected(aPntr, this)) return;
    
   // Get plygon path
    RMPath path = getPath();
    
    // Declare some path iteration variables
    Seg lastElement = null;
    int currentPointIndex = 0;
    Point pnts[] = new Point[3];
    float HW = 6, HHW= HW/2;

    // Iterate over path segements
    for(int i=0; i<path.getSegCount(); i++) { int pointIndex = path.getSegPointIndex(i);
        
        // Get points
        pnts[0] = pointIndex<path.getPointCount()? path.getPoint(pointIndex++) : null;
        pnts[1] = pointIndex<path.getPointCount()? path.getPoint(pointIndex++) : null;
        pnts[2] = pointIndex<path.getPointCount()? path.getPoint(pointIndex++) : null;
        
        // Get segment type and next segment type
        Seg element = path.getSeg(i);
        Seg nextElement = i+1<path.getSegCount()? path.getSeg(i+1) : null;

        // Set color black for control lines and so alpha is correct for buttons
        aPntr.setColor(Color.BLACK);

        // Draw buttons for all segment endPoints
        switch(element) {

            // Handle MoveTo & LineTo: just draw button
            case MoveTo:
            case LineTo: {
                Rect hrect = new Rect(pnts[0].x-HHW, pnts[0].y-HHW, HW, HW);
                aPntr.drawButton(hrect, false);
                currentPointIndex++;
                break;
            }

            // Handle CURVE_TO: If selectedPointIndex is CurveTo, draw line to nearest endPoint and button
            case CubicTo: {
                
                // If controlPoint1's point index is the selectedPointIndex or last end point was selectedPointIndex
                // or lastElement was a CurveTo and it's controlPoint2's pointIndex was the selectedPointIndex
                //   then draw control line from controlPoint1 to last end point and draw handle for control point 1
                if(currentPointIndex==_selectedPointIndex || currentPointIndex-1==_selectedPointIndex ||
                   (lastElement==Seg.CubicTo && currentPointIndex-2==_selectedPointIndex)) {
                    Point lastPoint = path.getPoint(currentPointIndex-1);
                    aPntr.setStroke(Stroke.Stroke1);
                    aPntr.drawLine(pnts[0].getX(), pnts[0].getY(), lastPoint.getX(), lastPoint.getY());
                    aPntr.drawButton(pnts[0].x-HHW, pnts[0].y-HHW, HW, HW, false); // control pnt handle rect
                    aPntr.drawButton(lastPoint.x-HHW, lastPoint.y-HHW, HW, HW, false); // last pnt handle rect
                }

                // If controlPoint2's point index is selectedPointIndex or if end point's index is
                // selectedPointIndex or if next element is CurveTo and it's cp1 point index is
                // selectedPointIndex then draw control line from cp2 to end point and draw handle for cp2
                else if(currentPointIndex+1==_selectedPointIndex || currentPointIndex+2==_selectedPointIndex ||
                    (nextElement==Seg.CubicTo && currentPointIndex+3==_selectedPointIndex)) {
                    aPntr.setStroke(Stroke.Stroke1);
                    aPntr.drawLine(pnts[1].getX(), pnts[1].getY(), pnts[2].getX(), pnts[2].getY());
                    aPntr.drawButton(pnts[1].x-HHW, pnts[1].y-HHW, HW, HW, false);
                }

                // Draw button
                Rect hrect = new Rect(pnts[2].x-HHW, pnts[2].y-HHW, HW, HW);
                aPntr.drawButton(hrect, false);
                currentPointIndex += 3;
                break;
            }

            // Break
            default: break;
        }

        // Remember last element
        lastElement = element;
    }
}

/**
 * XML archival.
 */
protected XMLElement toXMLShape(XMLArchiver anArchiver)
{
    XMLElement e = super.toXMLShape(anArchiver); e.setName("polygon"); // Archive basic shape attributes and reset name
    e.add(_path.toXML(anArchiver));                                    // Archive path
    return e;                                                          // Return xml element
}

/**
 * XML unarchival.
 */
protected void fromXMLShape(XMLArchiver anArchiver, XMLElement anElement)
{
    super.fromXMLShape(anArchiver, anElement);                         // Unarchive basic shape attributes
    XMLElement pathXML = anElement.get("path");                        // Unarchive path
    _path = anArchiver.fromXML(pathXML, RMPath.class, this);
}

/**
 * Standard clone implementation.
 */
public RMPolygonShape clone()
{
    RMPolygonShape clone = (RMPolygonShape)super.clone();
    if(_path!=null) clone._path = _path.clone(); return clone;
}

}