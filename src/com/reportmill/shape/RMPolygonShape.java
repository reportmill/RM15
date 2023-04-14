/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import snap.geom.Path;
import snap.geom.Rect;
import snap.geom.Shape;
import snap.geom.Transform;
import snap.util.*;

/**
 * This class is an RMShape subclass that encapsulates an arbitrary path.
 */
public class RMPolygonShape extends RMParentShape {

    // The explicit path associated with this shape
    protected Path _path;

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
        _path = new Path(aShape);
    }

    /**
     * Returns the path for this polygon shape.
     */
    public Path getPath()
    {
        Rect boundsInside = getBoundsInside();
        return _path.copyFor(boundsInside);
    }

    /**
     * Sets the path for this polygon shape.
     */
    public void setPath(Shape aPath)
    {
        Path newPath = aPath instanceof Path || aPath == null ? (Path) aPath : new Path(aPath);
        _path = newPath;
        repaint();
    }

    /**
     * Replace the polygon's current path with a new path, adjusting the shape's bounds to match the new path.
     */
    public void setPathAndBounds(Shape newShape)
    {
        // Get shape as path
        Path newPath = new Path(newShape);

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
        e.add(_path.toXML(anArchiver));

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
        _path = anArchiver.fromXML(pathXML, Path.class, this);
    }
}