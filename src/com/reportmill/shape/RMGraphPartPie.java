/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import snap.util.*;

/**
 * This shape is used by graph area to hold attributes of pies.
 */
public class RMGraphPartPie extends RMShape {

    // Pie Chart: Whether to draw lines from wedge to labels
    boolean       _drawWedgeLabelLines = false;

    // Pie Chart: The key used to determine which wedge is extruded
    String        _extrusionKey = EXTRUDE_NONE;

    // Constants for Pie Wedge Exusion type
    public static final String EXTRUDE_NONE = "None";
    public static final String EXTRUDE_FIRST = "First";
    public static final String EXTRUDE_LAST = "Last";
    public static final String EXTRUDE_ALL = "All";
    public static final String EXTRUDE_CUSTOM = "Custom...";
    public static final String[] EXTRUSIONS = { EXTRUDE_NONE, EXTRUDE_FIRST, EXTRUDE_LAST,
        EXTRUDE_ALL, EXTRUDE_CUSTOM };

/**
 * Returns whether a pie graph draws lines from the wedges to wedge labels.
 */
public boolean getDrawWedgeLabelLines()  { return _drawWedgeLabelLines; }

/**
 * Sets whether a pie graph draws lines from the wedges to wedge labels.
 */
public void setDrawWedgeLabelLines(boolean aFlag)  { _drawWedgeLabelLines = aFlag; }

/**
 * Returns the key used to determine which pie wedges get extruded.
 */
public String getExtrusionKey()  { return _extrusionKey; }

/**
 * Sets the key used to determine which pie wedges get extruded.
 */
public void setExtrusionKey(String aKey)  { _extrusionKey = aKey; }

/**
 * XML archival.
 */
public XMLElement toXML(XMLArchiver anArchiver)
{
    // Archive basic shape attributes and reset element name
    XMLElement e = super.toXML(anArchiver); e.setName("pie");
    
    // Archive DrawWedgeLines, ExtrudeKey
    if(_drawWedgeLabelLines) e.add("draw-wedge-lines", true);
    if(_extrusionKey!=null && !_extrusionKey.equals(EXTRUDE_NONE)) e.add("extrude-key", _extrusionKey);

    // Return xml element
    return e;
}

/**
 * XML unarchival.
 */
public Object fromXML(XMLArchiver anArchiver, XMLElement anElement)
{
    // Unarchive basic shape attributes
    super.fromXML(anArchiver, anElement);

    // Unarchive DrawWedgeLines, ExtrudeKey
    setDrawWedgeLabelLines(anElement.getAttributeBoolValue("draw-wedge-lines"));
    setExtrusionKey(anElement.getAttributeValue("extrude-key", EXTRUDE_NONE));

    // Return this graph
    return this;
}

}