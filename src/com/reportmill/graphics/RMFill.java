/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;
import snap.gfx.*;
import snap.util.*;

/**
 * This class represents a simple shape fill, drawing a given color in a provided path. Subclasses support things
 * like gradients, textures, etc.
 */
public class RMFill implements Cloneable, XMLArchiver.Archivable {

    // Fill color
    RMColor        _color = RMColor.black;

/**
 * Creates a plain, black fill.
 */
public RMFill()  { }

/**
 * Creates a plain fill with the given color.
 */
public RMFill(RMColor aColor)  { _color = aColor; }

/**
 * Returns the color associated with this fill.
 */
public RMColor getColor()  { return _color; }

/**
 * Returns the name of the fill.
 */
public String getName()
{
    if(getClass()==RMFill.class) return "Color Fill";
    String cname = getClass().getSimpleName(); return cname.substring(2,cname.length()-4);
}

/**
 * Returns the snap version of this fill.
 */
public Paint snap()  { return getColor(); }

/**
 * Derives an instance of this class from another fill.
 */
public RMFill copyForColor(Color aColor)
{
    RMFill clone = clone();
    clone._color = aColor!=null? RMColor.get(aColor) : _color;
    return clone;
}
  
/**
 * Derives an instance of this class from another fill.
 */
public RMFill deriveFill(RMFill aFill)
{
    RMFill clone = clone();
    if(aFill!=null) clone._color = aFill.getColor();
    return clone;
}
  
/**
 * Standard clone implementation.
 */
public RMFill clone()
{
    try { return (RMFill)super.clone(); }
    catch(CloneNotSupportedException e) { throw new RuntimeException(e); }
}

/**
 * Standard equals implementation.
 */
public boolean equals(Object anObj)
{
    // Check identity, class and get other
    if(anObj==this) return true;
    if(anObj==null || anObj.getClass()!=getClass()) return false;
    RMFill other = (RMFill)anObj;
    
    // Check Color
    if(!SnapUtils.equals(other._color, _color)) return false;
    return true; // Return true since all checks passed
}

/**
 * XML archival.
 */
public XMLElement toXML(XMLArchiver anArchiver)
{
    XMLElement e = new XMLElement("fill");
    if(!getColor().equals(RMColor.black)) e.add("color", "#" + getColor().toHexString());
    return e;
}

/**
 * XML unarchival.
 */
public Object fromXML(XMLArchiver anArchiver, XMLElement anElement)
{
    String color = anElement.getAttributeValue("color");
    if(color!=null) _color = new RMColor(color);
    return this;
}

/**
 * Returns a string representation.
 */
public String toString()
{
    return String.format("%s: { color:%s }", getClass().getSimpleName(), getColor().toHexString());
}

}