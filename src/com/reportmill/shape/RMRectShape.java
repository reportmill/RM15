/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import snap.gfx.*;
import snap.util.*;

/**
 * This class represents a simple rectangle shape with a rounding radius.
 */
public class RMRectShape extends RMShape {
    
    // Rounding radius
    float      _radius = 0;

/**
 * Returns the rounding radius for the rectangle.
 */
public float getRadius()  { return _radius; }

/**
 * Sets the rounding radius for the rectangle.
 */
public void setRadius(float aValue)
{
    if(getRadius()==aValue) return;
    repaint();
    firePropChange("Radius", _radius, _radius = aValue);
}

/**
 * Returns the path for the rectangle (building path with rounded corners if needed).
 */
public Shape getPath()
{
    if(getRadius()<0.0001) return super.getPath();
    return new RoundRect(0, 0, getWidth(), getHeight(), getRadius());
}

/**
 * XML archival.
 */
public XMLElement toXML(XMLArchiver anArchiver)
{
    XMLElement e = super.toXML(anArchiver); e.setName("rect"); // Archive basic shape attributes and reset name
    if(_radius!=0) e.add("radius", _radius);                   // Archive Radius
    return e;
}

/**
 * XML unarchival.
 */
public Object fromXML(XMLArchiver anArchiver, XMLElement anElement)
{
    super.fromXML(anArchiver, anElement);                                          // Unarchive basic shape attributes
    if(anElement.hasAttribute("radius")) setRadius(anElement.getAttributeFloatValue("radius")); // Unarchive Radius
    return this;
}

}