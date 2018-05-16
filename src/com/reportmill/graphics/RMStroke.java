/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;
import snap.gfx.*;
import snap.util.*;

/**
 * An RMFill subclass specifically designed to describe strokes.
 */
public class RMStroke implements Cloneable, XMLArchiver.Archivable {
    
    // The color
    RMColor      _color = RMColor.black;
    
    // The stroke width
    float        _width = 1;
    
    // The dash array
    float        _dashArray[];
    
    // The dash phase
    float        _dashPhase = 0;

/**
 * Creates a plain, black stroke.
 */
public RMStroke()  { }

/**
 * Creates a stroke with the given color and line width.
 */
public RMStroke(Color aColor, double aStrokeWidth)  { _color = RMColor.get(aColor); _width = (float)aStrokeWidth; }

/**
 * Returns the color associated with this fill.
 */
public RMColor getColor()  { return _color; }

/**
 * Returns the line width of this stroke.
 */
public float getWidth()  { return _width; }

/**
 * Returns the dash array for this stroke.
 */
public float[] getDashArray()  { return _dashArray; }

/**
 * Returns the dash array for this stroke as a string.
 */
public String getDashArrayString()  { return getDashArrayString(getDashArray(), ", "); }

/**
 * Returns a dash array for given dash array string and delimeter.
 */
public static float[] getDashArray(String aString, String aDelimeter)
{
    // Just return null if empty
    if(aString==null || aString.length()==0) return null;
    
    String dashStrings[] = aString.split(",");
    float dashArray[] = new float[dashStrings.length];
    for(int i=0; i<dashStrings.length; i++) dashArray[i] = SnapUtils.floatValue(dashStrings[i]);
    return dashArray;
}

/**
 * Returns the dash array for this stroke as a string.
 */
public static String getDashArrayString(float dashArray[], String aDelimiter)
{
    if(dashArray==null || dashArray.length==0) return null;
    String dstring = SnapUtils.stringValue(dashArray[0]);
    for(int i=1; i<dashArray.length; i++) dstring += aDelimiter + SnapUtils.stringValue(dashArray[i]);
    return dstring;
}

/**
 * Returns the dash phase.
 */
public float getDashPhase()  { return _dashPhase; }

/**
 * Returns the path to be stroked, transformed from the input path.
 */
public Shape getStrokePath(Shape aShape)  { return aShape; }

/**
 * Returns the name of the fill.
 */
public String getName()
{
    if(getClass()==RMStroke.class) return "Stroke";
    String cname = getClass().getSimpleName(); return cname.substring(2,cname.length()-6);
}

/**
 * Returns a duplicate stroke with new color.
 */
public RMStroke deriveColor(RMColor aColor)  { RMStroke s = clone(); s._color = aColor; return s; }

/**
 * Returns a duplicate stroke with new stroke width.
 */
public RMStroke deriveWidth(float aWidth)  { RMStroke s = clone(); s._width = aWidth; return s; }

/**
 * Returns a duplicate stroke with new dash array.
 */
public RMStroke deriveDashArray(float ... aDA)  { RMStroke s = clone(); s._dashArray = aDA; return s; }

/**
 * Returns a duplicate stroke with new dash phase.
 */
public RMStroke deriveDashPhase(float aDP)  { RMStroke s = clone(); s._dashPhase = aDP; return s; }

/**
 * Returns the snap version of this fill.
 */
public Stroke snap()  { return new Stroke(getWidth(), getDashArray(), getDashPhase()); }

/**
 * Standard equals implementation.
 */
public boolean equals(Object anObj)
{
    // Check identity, superclass and get other
    if(anObj==this) return true;
    if(!super.equals(anObj)) return false;
    RMStroke other = (RMStroke)anObj;
    
    // Check Width, DashArray, DashPhase
    if(!MathUtils.equals(other._width, _width)) return false;
    if(!ArrayUtils.equals(other._dashArray, _dashArray)) return false;
    if(other._dashPhase!=_dashPhase) return false;
    return true; // Return true since all checks passed
}

/**
 * Standard clone implementation.
 */
public RMStroke clone()
{
    try { return (RMStroke)super.clone(); }
    catch(CloneNotSupportedException e) { throw new RuntimeException(e); }
}

/**
 * XML archival.
 */
public XMLElement toXML(XMLArchiver anArchiver)
{
    // Create element
    XMLElement e = new XMLElement("stroke");
    
    // Archive Color, Width, DashArray, DashPhase
    if(!getColor().equals(RMColor.black)) e.add("color", "#" + getColor().toHexString());
    if(_width!=1) e.add("width", _width);
    if(getDashArrayString()!=null && getDashArrayString().length()>0) e.add("dash-array", getDashArrayString());
    if(getDashPhase()!=0) e.add("dash-phase", getDashPhase());
    return e;
}

/**
 * XML unarchival.
 */
public Object fromXML(XMLArchiver anArchiver, XMLElement anElement)
{
    // Unarchive Color
    String color = anElement.getAttributeValue("color");
    if(color!=null) _color = new RMColor(color);
    
    // Unarchive Width, DashArray, DashPhase
    if(anElement.hasAttribute("width"))  _width = anElement.getAttributeFloatValue("width", 1);
    else if(anElement.hasAttribute("linewidth")) _width = anElement.getAttributeFloatValue("linewidth", 1);
    if(anElement.hasAttribute("dash-array")) _dashArray = getDashArray(anElement.getAttributeValue("dash-array"), ",");
    if(anElement.hasAttribute("dash-phase")) _dashPhase = anElement.getAttributeFloatValue("dash-phase");
    return this;
}

}