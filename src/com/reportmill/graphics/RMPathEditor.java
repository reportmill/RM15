/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;
import snap.geom.*;
import snap.gfx.Color;
import snap.gfx.Painter;
import snap.gfx.Stroke;

/**
 * This class edits a RMPath shape.
 */
public class RMPathEditor {

    // Constants
    private static float HANDLE_WIDTH = 6;
    private static float HALF_HANDLE_WIDTH = HANDLE_WIDTH / 2;
    private static Point ZERO_POINT = new Point();

    /**
     * Handles painting a polygon shape.
     */
    public static void paintHandles(Path2D aPath, Painter aPntr, int selectedPointIndex)
    {
        // Declare loop vars
        Seg lastSeg = null;
        int currentPointIndex = 0;
        Point[] points = new Point[3];
        int segCount = aPath.getSegCount();
        int pointCount = aPath.getPointCount();

        // Iterate over path segments
        for (int i = 0; i < segCount; i++) {

            // Get seg and next seg
            Seg seg = aPath.getSeg(i);
            Seg nextSeg = i + 1 < segCount ? aPath.getSeg(i + 1) : null;

            // Get points
            int pointIndex = aPath.getSegPointIndex(i);
            points[0] = pointIndex < pointCount ? aPath.getPoint(pointIndex) : ZERO_POINT;
            points[1] = pointIndex + 1 < pointCount ? aPath.getPoint(pointIndex + 1) : ZERO_POINT;
            points[2] = pointIndex + 2 < pointCount ? aPath.getPoint(pointIndex + 2) : ZERO_POINT;

            // Set color black for control lines and so alpha is correct for buttons
            aPntr.setColor(Color.BLACK);

            // Draw buttons for all segment endPoints
            switch (seg) {

                // Handle MoveTo & LineTo: just draw button
                case MoveTo:
                case LineTo: {
                    paintHandleAtXY(aPntr, points[0].x, points[0].y);
                    currentPointIndex++;
                    break;
                }

                // Handle CURVE_TO: If selectedPointIndex is CurveTo, draw line to nearest endPoint and button
                case CubicTo: {

                    // If controlPoint1's point index is the selectedPointIndex or last end point was selectedPointIndex
                    // or lastElement was a CurveTo and it's controlPoint2's pointIndex was the selectedPointIndex
                    //   then draw control line from controlPoint1 to last end point and draw handle for control point 1
                    if (currentPointIndex == selectedPointIndex || currentPointIndex - 1 == selectedPointIndex ||
                            (lastSeg == Seg.CubicTo && currentPointIndex - 2 == selectedPointIndex)) {
                        Point lastPoint = aPath.getPoint(currentPointIndex - 1);
                        aPntr.setStroke(Stroke.Stroke1);
                        aPntr.drawLine(points[0].x, points[0].getY(), lastPoint.getX(), lastPoint.getY());
                        paintHandleAtXY(aPntr, points[0].x, points[0].y);
                        paintHandleAtXY(aPntr, lastPoint.x, lastPoint.y);
                    }

                    // If controlPoint2's point index is selectedPointIndex or if end point's index is
                    // selectedPointIndex or if next element is CurveTo and it's cp1 point index is
                    // selectedPointIndex then draw control line from cp2 to end point and draw handle for cp2
                    else if (currentPointIndex + 1 == selectedPointIndex || currentPointIndex + 2 == selectedPointIndex ||
                            (nextSeg == Seg.CubicTo && currentPointIndex + 3 == selectedPointIndex)) {
                        aPntr.setStroke(Stroke.Stroke1);
                        aPntr.drawLine(points[1].x, points[1].y, points[2].x, points[2].y);
                        paintHandleAtXY(aPntr, points[1].x, points[1].y);
                    }

                    // Draw button
                    paintHandleAtXY(aPntr, points[2].x, points[2].y);
                    currentPointIndex += 3;
                    break;
                }

                // Break
                default: break;
            }

            // Remember last element
            lastSeg = seg;
        }
    }

    /**
     * Resets the point at the given index to the given point, while preserving something.
     */
    public static Path2D setPointStructured(Shape aPath, int index, Point point)
    {
        Path2D newPath = new Path2D(aPath);
        int segIndex = newPath.getSegIndexForPointIndex(index);
        Seg seg = newPath.getSeg(segIndex);

        // If point at index is part of a curveto, perform structured set
        if (seg == Seg.CubicTo) {
            int pointIndexForElementIndex = newPath.getSegPointIndex(segIndex);

            // If point index is control point 1, and previous element is a curveto, bring control point 2 of previous curveto in line
            if (index - pointIndexForElementIndex == 0) {
                if (segIndex - 1 > 0 && newPath.getSeg(segIndex - 1) == Seg.CubicTo) {
                    Point endPoint = newPath.getPoint(index - 1), cntrlPnt2 = newPath.getPoint(index - 2);
                    // endpoint==point winds up putting a NaN in the path
                    if (!endPoint.equals(point)) {
                        Size size = new Size(point.x - endPoint.x, point.y - endPoint.y);
                        size.normalize();
                        size.negate();
                        Size size2 = new Size(cntrlPnt2.x - endPoint.x, cntrlPnt2.y - endPoint.y);
                        double mag = size2.getMagnitude();
                        newPath.setPoint(index - 2, endPoint.x + size.getWidth() * mag, endPoint.y + size.getHeight() * mag);
                    }
                }
            }

            // If point index is control point 2, and next element is a curveto, bring control point 1 of next curveto in line
            else if (index - pointIndexForElementIndex == 1) {
                if (segIndex + 1 < newPath.getSegCount() && newPath.getSeg(segIndex + 1) == Seg.CubicTo) {
                    Point endPoint = newPath.getPoint(index + 1), otherControlPoint = newPath.getPoint(index + 2);
                    // don't normalize a point
                    if (!endPoint.equals(point)) {
                        Size size = new Size(point.x - endPoint.x, point.y - endPoint.y);
                        size.normalize();
                        size.negate();
                        Size size2 = new Size(otherControlPoint.x - endPoint.x, otherControlPoint.y - endPoint.y);
                        double mag = size2.getMagnitude();
                        newPath.setPoint(index + 2, endPoint.x + size.width * mag, endPoint.y + size.height * mag);
                    }
                }
            }

            // If point index is curve end point, move the second control point by the same amount as main point move
            else if (index - pointIndexForElementIndex == 2) {
                Point p1 = new Point(point);
                p1.subtract(newPath.getPoint(index));
                Point p2 = new Point(newPath.getPoint(index - 1));
                p2.add(p1);
                newPath.setPoint(index - 1, p2.x, p2.y);
                if (segIndex + 1 < newPath.getSegCount() && newPath.getSeg(segIndex + 1) == Seg.CubicTo) {
                    p1 = new Point(point);
                    p1.subtract(newPath.getPoint(index));
                    p2 = new Point(newPath.getPoint(index + 1));
                    p2.add(p1);
                    newPath.setPoint(index + 1, p2.x, p2.y);
                }
            }
        }

        // If there is a next element and it is a curveto, move its first control point by the same amount as main point move
        else if (segIndex + 1 < newPath.getSegCount() && newPath.getSeg(segIndex + 1) == Seg.CubicTo) {
            Point p1 = new Point(point);
            p1.subtract(newPath.getPoint(index));
            Point p2 = new Point(newPath.getPoint(index + 1));
            p2.add(p1);
            newPath.setPoint(index + 1, p2.x, p2.y);
        }

        // Set point at index to requested point
        newPath.setPoint(index, point.x, point.y);

        // Return
        return newPath;
    }

    /**
     * Add a point to the curve by subdividing the path segment at the hit point.
     */
    public static Path2D addPathPointAtPoint(Shape aPath, Point aPoint)
    {
        // Get old path and new path
        Path2D newPath = new Path2D();

        // Create small horizontal and vertical lines around mouse point
        Line hor = new Line(aPoint.x - 3, aPoint.y, aPoint.x + 3, aPoint.y);
        Line vert = new Line(aPoint.x, aPoint.y - 3, aPoint.x, aPoint.y + 3);

        // Iterate over path and if segment is hit by mouse point, split segment
        PathIter pathIter = aPath.getPathIter(null);
        double[] points = new double[6];
        double moveX = 0, moveY = 0;
        double lineX = 0, lineY = 0;

        // Iterate over path
        while (pathIter.hasNext()) {
            switch (pathIter.getNext(points)) {

                // Handle MoveTo
                case MoveTo:
                    newPath.moveTo(moveX = lineX = points[0], moveY = lineY = points[1]);
                    break;

                // Handle LineTo
                case LineTo: {
                    Line seg = new Line(lineX, lineY, lineX = points[0], lineY = points[1]);
                    Line seg2 = null;
                    double ix = seg.getHitPoint(hor);
                    double iy = seg.getHitPoint(vert);
                    if (.1 < ix && ix < .9)
                        seg2 = seg.split(ix);
                    else if (.1 < iy && iy < .9)
                        seg2 = seg.split(iy);
                    newPath.appendSegment(seg);
                    if (seg2 != null)
                        newPath.appendSegment(seg2);
                }
                break;

                // Handle QuadTo
                case QuadTo: {
                    Quad seg = new Quad(lineX, lineY, points[0], points[1], lineX = points[2], lineY = points[3]);
                    Quad seg2 = null;
                    double ix = seg.getHitPoint(hor);
                    double iy = seg.getHitPoint(vert);
                    if (.1 < ix && ix < .9)
                        seg2 = seg.split(ix);
                    else if (.1 < iy && iy < .9)
                        seg2 = seg.split(iy);
                    newPath.appendSegment(seg);
                    if (seg2 != null)
                        newPath.appendSegment(seg2);
                }
                break;

                // Handle CubicTo
                case CubicTo: {
                    Cubic seg = new Cubic(lineX, lineY, points[0], points[1], points[2], points[3], lineX = points[4], lineY = points[5]);
                    Cubic seg2 = null;
                    double ix = seg.getHitPoint(hor);
                    double iy = seg.getHitPoint(vert);
                    if (.1 < ix && ix < .9)
                        seg2 = seg.split(ix);
                    else if (.1 < iy && iy < .9)
                        seg2 = seg.split(iy);
                    newPath.appendSegment(seg);
                    if (seg2 != null)
                        newPath.appendSegment(seg2);
                }
                break;

                // Handle Close
                case Close: {
                    Line seg = new Line(lineX, lineY, lineX = moveX, lineY = moveY);
                    Line seg2 = null;
                    double ix = seg.getHitPoint(hor);
                    double iy = seg.getHitPoint(vert);
                    if (.1 < ix && ix < .9)
                        seg2 = seg.split(ix);
                    else if (.1 < iy && iy < .9)
                        seg2 = seg.split(iy);
                    if (seg2 != null)
                        newPath.appendSegment(seg);
                    newPath.close();
                }
                break;
            }
        }

        // Return
        return newPath;
    }

    /**
     * Delete the selected control point and readjust shape bounds
     */
    public static Path2D deletePathPointAtPointIndex(Shape aPath, int pointIndex)
    {
        // Make changes to a clone of the path so deletions can be undone
        Path2D newPath = new Path2D(aPath);

        // get the index of the path segment corresponding to the selected control point
        int selPointIndex = newPath.getSegIndexForPointIndex(pointIndex);

        // Delete the point from path in parent coords
        Path2DUtils.removeSegAtIndexSmoothly(newPath, selPointIndex);

        // Return
        return newPath;
    }

    /**
     * Returns the bounds for all the control points.
     */
    public static Rect getControlPointBoundsWithSelectedPoint(Path2D aPath, int selectedPointIndex)
    {
        // Get segment index for selected control point handle
        int mouseDownIndex = aPath.getSegIndexForPointIndex(selectedPointIndex);
        if (mouseDownIndex >= 0 && aPath.getSeg(mouseDownIndex) == Seg.CubicTo &&
                (aPath.getSegPointIndex(mouseDownIndex) == selectedPointIndex))
            mouseDownIndex--;

        // Iterate over path elements
        Point p0 = aPath.getPointCount() > 0 ? new Point(aPath.getPoint(0)) : ZERO_POINT;
        double p1x = p0.x, p1y = p0.y;
        double p2x = p1x, p2y = p1y;
        PathIter pathIter = aPath.getPathIter(null);
        double[] points = new double[6];


        for (int i = 0; pathIter.hasNext(); i++)
            switch (pathIter.getNext(points)) {

                // Handle MoveTo
                case MoveTo:

                    // Handle LineTo
                case LineTo: {
                    p1x = Math.min(p1x, points[0]);
                    p1y = Math.min(p1y, points[1]);
                    p2x = Math.max(p2x, points[0]);
                    p2y = Math.max(p2y, points[1]);
                }
                break;

                // Handle CubicTo
                case CubicTo: {
                    if ((i - 1) == mouseDownIndex) {
                        p1x = Math.min(p1x, points[0]);
                        p1y = Math.min(p1y, points[1]);
                        p2x = Math.max(p2x, points[0]);
                        p2y = Math.max(p2y, points[1]);
                    }
                    if (i == mouseDownIndex) {
                        p1x = Math.min(p1x, points[2]);
                        p1y = Math.min(p1y, points[3]);
                        p2x = Math.max(p2x, points[2]);
                        p2y = Math.max(p2y, points[3]);
                    }
                    p1x = Math.min(p1x, points[4]);
                    p1y = Math.min(p1y, points[5]);
                    p2x = Math.max(p2x, points[4]);
                    p2y = Math.max(p2y, points[5]);
                }
                break;

                // Handle default
                default:
                    break;
            }

        // Create control point bounds rect, union with path bounds and return
        Rect controlPointBounds = new Rect(p1x, p1y, Math.max(1, p2x - p1x), Math.max(1, p2y - p1y));
        controlPointBounds.union(aPath.getBounds());

        // Return
        return controlPointBounds;
    }

    /**
     * Returns the handle index for a given point for given path. Only returns points that are on the path,
     * except for the control points of selectedPoint (if not -1)
     */
    public static int handleAtPoint(Path2D aPath, Point aPoint, int selectedPoint)
    {
        // Check against off-path control points of selected path first, otherwise you might never be able to select one
        if (selectedPoint != -1) {
            int[] offPathPoints = new int[2];
            int noffPathPoints = 0;
            int segCount = aPath.getSegCount();
            int segIndex = getSegIndexForPointIndex(aPath, selectedPoint);
            Seg seg = aPath.getSeg(segIndex);

            // If the selected point is one of the on path points, figure out the indices of the others
            if (pointOnPath(aPath, selectedPoint)) {

                // If the selected element is a curveto or quadto, the second to the last control point will be active
                if (seg == Seg.CubicTo || seg == Seg.QuadTo)
                    offPathPoints[noffPathPoints++] = selectedPoint - 1;

                // If the element following the selected element is a curveto, it's first control point will be active
                if (segIndex < segCount - 1 && aPath.getSeg(segIndex + 1) == Seg.CubicTo)
                    offPathPoints[noffPathPoints++] = selectedPoint + 1;
            }

            // If selected point is off-path, add it to list to check and then figure out what other point might be active
            else {
                offPathPoints[noffPathPoints++] = selectedPoint;

                // If selected point is first control point, check previous segment
                if (selectedPoint == aPath.getSegPointIndex(segIndex)) {
                    if (segIndex > 0 && aPath.getSeg(segIndex - 1) == Seg.CubicTo)
                        offPathPoints[noffPathPoints++] = selectedPoint - 2;
                }

                // Otherwise check next segment
                else {
                    if (segIndex < segCount - 1 && aPath.getSeg(segIndex + 1) == Seg.CubicTo)
                        offPathPoints[noffPathPoints++] = selectedPoint + 2;
                }
            }

            // Hit test any selected off-path handles
            for (int i = 0; i < noffPathPoints; ++i)
                if (hitHandle(aPath, aPoint, offPathPoints[i]))
                    return offPathPoints[i];
        }

        // Check the rest of the points, but only ones that are actually on the path
        for (int i = 0, iMax = aPath.getPointCount(); i < iMax; i++)
            if (hitHandle(aPath, aPoint, i) && pointOnPath(aPath, i))
                return i;

        // Return nothing hit
        return -1;
    }

    /**
     * Hit test the point (in path coords) against a given path point.
     */
    private static boolean hitHandle(Path2D aPath, Point aPoint, int pointIndex)
    {
        Point p = aPath.getPoint(pointIndex);
        double handleSize = 9;
        Rect br = new Rect(p.x - handleSize / 2, p.y - handleSize / 2, handleSize, handleSize);
        return br.contains(aPoint.x, aPoint.y);
    }

    /**
     * Returns the element index for the given point index.
     */
    private static int getSegIndexForPointIndex(Path2D aPath, int index)
    {
        // Iterate over segments and increment element index
        int elementIndex = 0;
        for (int pointIndex = 0; pointIndex <= index; elementIndex++) {
            switch (aPath.getSeg(elementIndex)) {
                case MoveTo: case LineTo: pointIndex++; break;
                case QuadTo: pointIndex += 2; break;
                case CubicTo: pointIndex += 3; break;
                default: break;
            }
        }

        // Return calculated element index
        return elementIndex - 1;
    }

    /**
     * Returns true of the point at pointIndex is on the path, and false if it is on the convex hull.
     */
    public static boolean pointOnPath(Path2D aPath, int pointIndex)
    {
        int segIndex = getSegIndexForPointIndex(aPath, pointIndex);
        int indexInSeg = pointIndex - aPath.getSegPointIndex(segIndex);

        // Only the last point is actually on the path
        Seg seg = aPath.getSeg(segIndex);
        int segPointCount = seg.getCount();
        return indexInSeg == segPointCount - 1;
    }

    /**
     * Paints a handle at given point.
     */
    private static void paintHandleAtXY(Painter aPntr, double aX, double aY)
    {
        aPntr.drawButton(aX - HALF_HANDLE_WIDTH, aY - HALF_HANDLE_WIDTH, HANDLE_WIDTH, HANDLE_WIDTH, false);
    }
}
