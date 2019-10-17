/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import snap.util.*;

/**
 * This shape is used by graph area to hold attributes of bars.
 */
public class RMGraphPartBars extends RMShape {

    // The width of the gap between bars in a set as a fraction of bar width
    float         _barGap = .3f;

    // The width of the gap between sets as a fraction of chart area
    float         _setGap = 1;
    
    // The minimum number of bars for the graph to provide space for in a section
    int           _barCount = 0;

/**
 * Returns the width of the gap between bars in a set as a fraction of bar width.
 */
public float getBarGap()  { return _barGap; }

/**
 * Sets width of the gap between bars in a set as a fraction of bar width.
 */
public void setBarGap(float aValue)
{
    _barGap = aValue;
    relayoutParent();
}

/**
 * Returns the width of the gap between sets as a fraction of chart area.
 */
public float getSetGap()  { return _setGap; }

/**
 * Sets the width of the gap between sets as a fraction of chart area.
 */
public void setSetGap(float aValue)
{
    _setGap = aValue;
    relayoutParent();
}

/**
 * Returns the minimum number of bars for the graph to provide space for in a section.
 */
public int getBarCount()  { return _barCount; }

/**
 * Sets the minimum number of bars for the graph to provide space for in a section.
 */
public void setBarCount(int aValue)
{
    _barCount = aValue;
    relayoutParent();
}

/**
 * XML archival.
 */
public XMLElement toXML(XMLArchiver anArchiver)
{
    // Archive basic shape attributes and reset element name
    XMLElement e = super.toXML(anArchiver); e.setName("bars");
    
    // Archive BarGap, SetGap, BarCount
    if(_barGap!=.3f) e.add("bar-gap", _barGap);
    if(_setGap!=1) e.add("set-gap", _setGap);
    if(_barCount>0) e.add("bar-count", _barCount);
    
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

    // Unarchive BarGap, SetGap, BarCount
    setBarGap(anElement.getAttributeFloatValue("bar-gap", .25f));
    setSetGap(anElement.getAttributeFloatValue("set-gap", 1f));
    setBarCount(anElement.getAttributeIntValue("bar-count"));
    
    // Return this graph
    return this;
}

}