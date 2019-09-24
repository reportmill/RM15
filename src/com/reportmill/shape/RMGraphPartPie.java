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
    
    // The ratio that describes the hole (0 = no hole, 1 = no wedge)
    double        _holeRatio;

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
public void setDrawWedgeLabelLines(boolean aFlag)
{
    _drawWedgeLabelLines = aFlag;
    relayoutParent();
}

/**
 * Returns the key used to determine which pie wedges get extruded.
 */
public String getExtrusionKey()  { return _extrusionKey; }

/**
 * Sets the key used to determine which pie wedges get extruded.
 */
public void setExtrusionKey(String aKey)
{
    _extrusionKey = aKey;
    relayoutParent();
}

/**
 * Returns the ratio that describes the hole (0 = no hole, 1 = no wedge).
 */
public double getHoleRatio()  { return _holeRatio; }

/**
 * Sets the ratio that describes the hole (0 = no hole, 1 = no wedge).
 */
public void setHoleRatio(double aValue)
{
    double value = MathUtils.clamp(aValue, 0, 1);
    if(value==getHoleRatio()) return;
    firePropChange("HoleRatio", _holeRatio, _holeRatio = value);
    relayoutParent();
}

/**
 * XML archival.
 */
public XMLElement toXML(XMLArchiver anArchiver)
{
    // Archive basic shape attributes and reset element name
    XMLElement e = super.toXML(anArchiver); e.setName("pie");
    
    // Archive DrawWedgeLines, ExtrudeKey, HoleRatio
    if(_drawWedgeLabelLines) e.add("draw-wedge-lines", true);
    if(_extrusionKey!=null && !_extrusionKey.equals(EXTRUDE_NONE)) e.add("extrude-key", _extrusionKey);
    if(_holeRatio!=0) e.add("HoleRatio", _holeRatio);

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
    setHoleRatio(anElement.getAttributeFloatValue("HoleRatio", 0));

    // Return this graph
    return this;
}

}