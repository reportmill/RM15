/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import snap.gfx.*;
import snap.util.*;

/**
 * This class is an RMShape subclass that encapsulates an arbitrary path. 
 */
public class RMPolygonShape extends RMParentShape {
    
    // The explicit path associated with this shape
    Path        _path;
    
/**
 * Creates a new empty polygon shape.
 */
public RMPolygonShape() { }

/**
 * Creates a new polygon shape for the given path.
 */
public RMPolygonShape(Shape aShape)  { this(); _path = new Path(aShape); }

/**
 * Returns the path for this polygon shape.
 */
public Path getPath()  { return _path.copyFor(getBoundsInside()); }

/**
 * Sets the path for this polygon shape.
 */
public void setPath(Path aPath)  { _path = aPath; repaint(); }

/**
 * Replace the polygon's current path with a new path, adjusting the shape's bounds to match the new path.
 */
public void resetPath(Path newPath)
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
    _path = anArchiver.fromXML(pathXML, Path.class, this);
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