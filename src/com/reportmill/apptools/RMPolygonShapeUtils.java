package com.reportmill.apptools;
import snap.gfx.*;
import snap.gfx.PathIter.Seg;

/**
 * Utility methods for Path and PathView to assist PathViewTool.
 */
public class RMPolygonShapeUtils {

/**
 * Resets the point at the given index to the given point, while preserving something.
 */
public static void setPointStructured(Path aPath, int index, Point point)
{
    int elmtIndex = aPath.getSegIndexForPointIndex(index);
    Seg elmt = aPath.getSeg(elmtIndex);

    // If point at index is part of a curveto, perform structured set
    if(elmt==Seg.CubicTo) {
        int pointIndexForElementIndex = aPath.getSegPointIndex(elmtIndex);

        // If point index is control point 1, and previous element is a curveto, bring control point 2 of previous curveto in line
        if(index - pointIndexForElementIndex == 0) {
            if(elmtIndex-1>0 && aPath.getSeg(elmtIndex-1)==Seg.CubicTo) {
                Point endPoint = aPath.getPoint(index-1), cntrlPnt2 = aPath.getPoint(index-2);
                // endpoint==point winds up putting a NaN in the path 
                if (!endPoint.equals(point)) {
                    Size size = new Size(point.getX() - endPoint.getX(), point.getY() - endPoint.getY());
                    size.normalize(); size.negate();
                    Size size2 = new Size(cntrlPnt2.getX() - endPoint.getX(), cntrlPnt2.getY() - endPoint.getY());
                    double mag = size2.getMagnitude();
                    aPath.setPoint(index-2, endPoint.getX() + size.getWidth()*mag, endPoint.getY() + size.getHeight()*mag);
                }
                else {
                    // Illustrator pops the otherControlPoint here to what it was at the 
                    // start of the drag loop.  Not sure that's much better...
                }
            }
        }

        // If point index is control point 2, and next element is a curveto, bring control point 1 of next curveto in line
        else if(index - pointIndexForElementIndex == 1) {
            if(elmtIndex+1<aPath.getSegCount() && aPath.getSeg(elmtIndex+1)==Seg.CubicTo) {
                Point endPoint = aPath.getPoint(index+1), otherControlPoint = aPath.getPoint(index+2);
                // don't normalize a point
                if (!endPoint.equals(point)) {
                    Size size = new Size(point.getX() - endPoint.x, point.getY() - endPoint.y);
                    size.normalize(); size.negate();
                    Size size2 = new Size(otherControlPoint.x - endPoint.x, otherControlPoint.y - endPoint.y);
                    double mag = size2.getMagnitude();
                    aPath.setPoint(index+2, endPoint.x+size.width*mag, endPoint.y + size.height*mag);
                }
                else { }
            }
        }

        // If point index is curve end point, move the second control point by the same amount as main point move
        else if(index - pointIndexForElementIndex == 2) {
            Point p1 = new Point(point); p1.subtract(aPath.getPoint(index));
            Point p2 = new Point(aPath.getPoint(index-1)); p2.add(p1);
            aPath.setPoint(index-1, p2.getX(), p2.getY());
            if(elmtIndex+1<aPath.getSegCount() && aPath.getSeg(elmtIndex+1)==Seg.CubicTo) {
                p1 = new Point(point); p1.subtract(aPath.getPoint(index));
                p2 = new Point(aPath.getPoint(index+1)); p2.add(p1);
                aPath.setPoint(index+1, p2.getX(), p2.getY());
            }
        }
    }

    // If there is a next element and it is a curveto, move its first control point by the same amount as main point move
    else if(elmtIndex+1<aPath.getSegCount() && aPath.getSeg(elmtIndex+1)==Seg.CubicTo) {
        Point p1 = new Point(point); p1.subtract(aPath.getPoint(index));
        Point p2 = new Point(aPath.getPoint(index+1)); p2.add(p1);
        aPath.setPoint(index+1, p2.getX(), p2.getY());
    }

    // Set point at index to requested point
    aPath.setPoint(index, point.getX(), point.getY());
}

/**
 * Returns the handle index for a given point against this path scaled to the given rect.
 * Only returns points that are on the path, except for the control points of
 * selectedPoint (if not -1)
 */
public static int handleAtPointForBounds(Path aPath, Point aPoint, Rect aRect, int selectedPoint, Size handleSize)
{
    // convert point from shape coords to path coords
    Point point = pointInPathCoordsFromPoint(aPath, aPoint, aRect);

    // Check against off-path control points of selected path first, otherwise you might never be able to select one
    if(selectedPoint != -1) {
        int offPathPoints[]=new int[2];
        int noffPathPoints=0;
        int ecount = aPath.getSegCount();
        int eindex = getSegIndexForPointIndex(aPath, selectedPoint);
        Seg elmt = aPath.getSeg(eindex);
        
        // If the selected point is one of the on path points, figure out the indices of the others
        if(pointOnPath(aPath, selectedPoint)) {
            
            // If the selected element is a curveto or quadto, the second to the last control point will be active
            if(elmt==Seg.CubicTo || elmt==Seg.QuadTo)
                offPathPoints[noffPathPoints++] = selectedPoint-1;

            // If the element following the selected element is a curveto, it's first control point will be active
            if (eindex<ecount-1 && aPath.getSeg(eindex+1)==Seg.CubicTo)
                offPathPoints[noffPathPoints++] = selectedPoint+1;
        }
        
        // If selected point is off-path, add it to list to check and then figure out what other point might be active
        else {
            offPathPoints[noffPathPoints++] = selectedPoint;
            
            // if selected point is first control point, check previous segment, otherwise check next segment
            if (selectedPoint == aPath.getSegPointIndex(eindex)) {
                if(eindex>0 && aPath.getSeg(eindex-1)==Seg.CubicTo)
                    offPathPoints[noffPathPoints++] = selectedPoint-2;
            }
            else {
                if(eindex<ecount-1 && aPath.getSeg(eindex+1)==Seg.CubicTo) 
                    offPathPoints[noffPathPoints++] = selectedPoint+2;
            }
        }
        
        // hit test any selected off-path handles
        for(int i=0; i<noffPathPoints; ++i)
            if(hitHandle(aPath, point, offPathPoints[i], handleSize))
                return offPathPoints[i];
    }
    
    // Check the rest of the points, but only ones that are actually on the path
    for(int i=0, iMax=aPath.getPointCount(); i<iMax; i++)
        if(hitHandle(aPath, point, i, handleSize) && pointOnPath(aPath, i))
            return i;

    // nothing hit
    return -1;
}
        
/**
 * Hit test the point (in path coords) against a given path point.
 */
private static boolean hitHandle(Path aPath, Point aPoint, int ptIndex, Size handleSize)
{
    Point p = aPath.getPoint(ptIndex);
    Rect br = new Rect(p.x-handleSize.width/2, p.y-handleSize.height/2, handleSize.width, handleSize.height);
    return br.contains(aPoint.getX(), aPoint.getY());
}

/**
 * Returns the given point converted to path coords for given path bounds.
 */
public static Point pointInPathCoordsFromPoint(Path aPath, Point aPoint, Rect aRect)
{
    Rect bounds = aPath.getBounds();
    double sx = bounds.getWidth()/aRect.getWidth();
    double sy = bounds.getHeight()/aRect.getHeight();
    double x = (aPoint.getX()-aRect.getMidX())*sx + bounds.getMidX();
    double y = (aPoint.getY()-aRect.getMidY())*sy + bounds.getMidY();
    return new Point(x,y);
}

/**
 * Returns the element index for the given point index.
 */
public static int getSegIndexForPointIndex(Path aPath, int index)
{
    // Iterate over segments and increment element index
    int elementIndex = 0;
    for(int pointIndex=0; pointIndex<=index; elementIndex++)
        switch(aPath.getSeg(elementIndex)) {
            case MoveTo:
            case LineTo: pointIndex++; break;
            case QuadTo: pointIndex += 2; break;
            case CubicTo: pointIndex += 3; break;
            default: break;
        }
    
    // Return calculated element index
    return elementIndex - 1;
}

/**
 * Returns true of the point at pointIndex is on the path, and false if it is on the convex hull.
 */ 
public static boolean pointOnPath(Path aPath, int pointIndex)
{
    int sindex = getSegIndexForPointIndex(aPath, pointIndex);
    int indexInElement = pointIndex - aPath.getSegPointIndex(sindex);
    
    // Only the last point is actually on the path
    Seg seg = aPath.getSeg(sindex);
    int numPts = seg.getCount();
    return indexInElement==numPts-1;
}

}