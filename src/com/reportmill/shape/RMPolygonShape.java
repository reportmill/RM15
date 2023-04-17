/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import snap.geom.*;
import snap.util.*;

/**
 * This class is an RMShape subclass that encapsulates an arbitrary path.
 */
public class RMPolygonShape extends RMParentShape {

    // The explicit path associated with this shape
    protected Path2D _path;

    /**
     * Constructor.
     */
    public RMPolygonShape()
    {
        super();
    }

    /**
     * Constructor for given path.
     */
    public RMPolygonShape(Shape aShape)
    {
        this();
        _path = new Path2D(aShape);
    }

    /**
     * Returns the path for this polygon shape.
     */
    public Path2D getPath()
    {
        Rect boundsInside = getBoundsInside();
        return _path.copyFor(boundsInside);
    }

    /**
     * Sets the path for this polygon shape.
     */
    public void setPath(Shape aPath)
    {
        Path2D newPath = aPath instanceof Path2D || aPath == null ? (Path2D) aPath : new Path2D(aPath);
        _path = newPath;
        repaint();
    }

    /**
     * Replace the polygon's current path with a new path, adjusting the shape's bounds to match the new path.
     */
    public void setPathAndBounds(Shape newShape)
    {
        // Get shape as path
        Path2D newPath = new Path2D(newShape);

        // Get the transform to parent shape coords
        Transform localToParent = getTransform();

        // Set the new path and new size
        setPath(newPath);
        Rect newPathBounds = newPath.getBounds();
        setSize(newPathBounds.width, newPathBounds.height);

        // Transform to parent for new x & y
        Rect boundsInParent = newPathBounds.clone();
        localToParent.transformRect(boundsInParent);
        setFrameXY(boundsInParent.getXY());
    }

    /**
     * Standard clone implementation.
     */
    public RMPolygonShape clone()
    {
        RMPolygonShape clone = (RMPolygonShape) super.clone();
        if (_path != null)
            clone._path = _path.clone();
        return clone;
    }

    /**
     * XML archival.
     */
    protected XMLElement toXMLShape(XMLArchiver anArchiver)
    {
        // Archive basic shape attributes and reset name
        XMLElement e = super.toXMLShape(anArchiver); e.setName("polygon");

        // Archive path
        XMLElement pathXML = getXmlForPath(_path);
        e.add(pathXML);

        // Return
        return e;
    }

    /**
     * XML unarchival.
     */
    protected void fromXMLShape(XMLArchiver anArchiver, XMLElement anElement)
    {
        // Unarchive basic shape attributes
        super.fromXMLShape(anArchiver, anElement);

        // Unarchive path
        XMLElement pathXML = anElement.get("path");
        _path = getPathFromXML(pathXML);
    }

    /**
     * XML archival for path.
     */
    private XMLElement getXmlForPath(Path2D aPath)
    {
        // Get new element named path
        XMLElement e = new XMLElement("path");

        // Archive individual elements/points
        PathIter pathIter = aPath.getPathIter(null);
        double[] points = new double[6];
        while (pathIter.hasNext()) switch (pathIter.getNext(points)) {

            // Handle MoveTo
            case MoveTo:
                XMLElement move = new XMLElement("mv");
                move.add("x", points[0]);
                move.add("y", points[1]);
                e.add(move);
                break;

            // Handle LineTo
            case LineTo:
                XMLElement line = new XMLElement("ln");
                line.add("x", points[0]);
                line.add("y", points[1]);
                e.add(line);
                break;

            // Handle QuadTo
            case QuadTo:
                XMLElement quad = new XMLElement("qd");
                quad.add("cx", points[0]);
                quad.add("cy", points[1]);
                quad.add("x", points[2]);
                quad.add("y", points[3]);
                e.add(quad);
                break;

            // Handle CubicTo
            case CubicTo:
                XMLElement curve = new XMLElement("cv");
                curve.add("cp1x", points[0]);
                curve.add("cp1y", points[1]);
                curve.add("cp2x", points[2]);
                curve.add("cp2y", points[3]);
                curve.add("x", points[4]);
                curve.add("y", points[5]);
                e.add(curve);
                break;

            // Handle Close
            case Close:
                XMLElement close = new XMLElement("cl");
                e.add(close);
                break;
        }

        return e;
    }

    /**
     * XML unarchival for path.
     */
    public Path2D getPathFromXML(XMLElement anElement)
    {
        Path2D path = new Path2D();

        // Unarchive individual elements/points
        for (int i = 0, iMax = anElement.size(); i < iMax; i++) {
            XMLElement segXML = anElement.get(i);
            String segName = segXML.getName();
            double endX = segXML.getAttributeFloatValue("x");
            double endY = segXML.getAttributeFloatValue("y");

            switch (segName) {
                case "mv": path.moveTo(endX, endY); break;
                case "ln":path.lineTo(endX, endY); break;
                case "qd": {
                    double cpx = segXML.getAttributeFloatValue("cx");
                    double cpy = segXML.getAttributeFloatValue("cy");
                    path.quadTo(cpx, cpy, endX, endY);
                    break;
                }
                case "cv": {
                    double cp1x = segXML.getAttributeFloatValue("cp1x");
                    double cp1y = segXML.getAttributeFloatValue("cp1y");
                    double cp2x = segXML.getAttributeFloatValue("cp2x");
                    double cp2y = segXML.getAttributeFloatValue("cp2y");
                    path.curveTo(cp1x, cp1y, cp2x, cp2y, endX, endY);
                    break;
                }
                case "cl":
                    path.close();
                    break;
            }
        }

        // Return
        return path;
    }
}