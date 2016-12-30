/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import snap.gfx.*;
import snap.util.*;

/**
 * This class represents a simple oval, with a setable start angle and sweep angle.
 */
public class RMOvalShape extends RMShape {
    
    // The oval start angle
    float         _start = 0;
    
    // The oval sweep angle
    float         _sweep = 360;

/**
 * Returns the start angle for the oval.
 */
public float getStartAngle()  { return _start; }

/**
 * Sets the start angle for the oval.
 */
public void setStartAngle(float aValue)
{
    if(getStartAngle()==aValue) return; repaint();
    firePropChange("StartAngle", _start, _start = aValue);
}

/**
 * Returns the sweep angle for the oval.
 */
public float getSweepAngle()  { return _sweep; }

/**
 * Sets the sweep angle for the oval.
 */
public void setSweepAngle(float aValue)
{
    if(getSweepAngle()==aValue) return; repaint();
    firePropChange("SweepAngle", _sweep, _sweep = aValue);
}

/**
 * Returns the (oval) path for this shape.
 */
public Shape getPath()  { return new Arc(0, 0, getWidth(), getHeight(), _start, _sweep); }

/**
 * XML archival.
 */
public XMLElement toXML(XMLArchiver anArchiver)
{
    XMLElement e = super.toXML(anArchiver); e.setName("oval");  // Archive basic shape attributes and reset name
    if(_start!=0) e.add("start", _start);                       // Archive StartAngle, Sweep
    if(_sweep!=360) e.add("sweep", _sweep);
    return e;
}

/**
 * XML unarchival.
 */
public Object fromXML(XMLArchiver anArchiver, XMLElement anElement)
{
    super.fromXML(anArchiver, anElement);                           // Unarchive basic shape attributes
    setStartAngle(anElement.getAttributeFloatValue("start"));       // Unarchive StartAngle, Sweep
    setSweepAngle(anElement.getAttributeFloatValue("sweep", 360));
    return this;
}

}