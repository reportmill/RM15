/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import snap.gfx.*;
import snap.util.*;

/**
 * A parent shape that does child layout with RMSpringLayout.
 */
public class RMSpringShape extends RMParentShape {

    // Whether to GrowHeight
    boolean       _growHeight = true;

/**
 * Creates a new RMSpringShape.
 */
public RMSpringShape()  { setLayout(new RMShapeLayout()); }

/**
 * Returns the shape preferred height.
 */
protected double getPrefHeightImpl(double aWidth)
{
    if(!_growHeight) return getHeight();
    return super.getPrefHeightImpl(aWidth);
}

/**
 * XML unarchival.
 */
protected void fromXMLShape(XMLArchiver anArchiver, XMLElement anElement)
{
    super.fromXMLShape(anArchiver,anElement);
    if(anElement.hasAttribute("GrowHeight")) _growHeight = anElement.getAttributeBoolValue("GrowHeight");
    if(anArchiver.getVersion()<=12)
        _growHeight = false;
}

/**
 * XML Archival.
 */
public XMLElement toXMLShape(XMLArchiver anArchiver)
{
    XMLElement e = super.toXMLShape(anArchiver); e.setName("spring-shape");
    e.add("GrowHeight", false);
    return e;
}
    
/**
 * Override to paint dashed box around bounds.
 */
public void paintShape(Painter aPntr)
{
    // Do normal version
    super.paintShape(aPntr);
    
    // Paint dashed box around bounds
    RMShapePaintProps props = RMShapePaintProps.get(aPntr);
    if(props.isEditing() && getStroke()==null && getFill()==null && getEffect()==null &&
        (props.isSelected(this) || props.isSuperSelected(this))) {
        aPntr.setColor(Color.LIGHTGRAY); aPntr.setStroke(Stroke.Stroke1.copyForDashes(3,2));
        aPntr.setAntialiasing(false); aPntr.draw(getBoundsInside()); aPntr.setAntialiasing(true);
    }
}

}