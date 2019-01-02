/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import snap.gfx.*;
import snap.util.*;

/**
 * This class represents an individual label inside an RMLabels template.
 */
public class RMLabel extends RMParentShape {

/**
 * Editor method - indicates that individual label accepts children.
 */
public boolean acceptsChildren()  { return true; }

/**
 * Paints label.
 */
protected void paintShape(Painter aPntr)
{
    // Do normal paint shape
    super.paintShape(aPntr);
    
    // Table bands should draw a red band around thier perimeter when it is selected
    RMShapePaintProps props = RMShapePaintProps.get(aPntr);
    if(props.isSelected(this) || props.isSuperSelected(this)) {
        Rect bounds = getBoundsInside(); bounds.inset(2, 2);
        aPntr.setColor(Color.RED); aPntr.setStroke(Stroke.Stroke1);
        aPntr.draw(bounds);
    }
}

/**
 * XML archival.
 */
protected XMLElement toXMLShape(XMLArchiver anArchiver)
{
    // Archive basic shape attributes and reset element name
    XMLElement e = super.toXMLShape(anArchiver); e.setName("label");
    return e;
}

}

