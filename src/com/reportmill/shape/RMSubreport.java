/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import com.reportmill.base.RMKeyChain;
import com.reportmill.graphics.*;
import snap.gfx.*;
import snap.util.*;

/**
 * A shape to reference another template.
 */
public class RMSubreport extends RMRectShape {

    // The name of the subreport
    String             _subreportName;
    
    // The subreport shape
    RMParentShape      _subreportShape;
    
    // The fill to use on top of subreport shape when painting in editor
    GradientPaint      _coverFill;
    
    // The stroke to use on top of subreport shape when painting in editor
    Stroke             _coverStroke;
    
/**
 * Returns the subreport name.
 */
public String getSubreportName()  { return _subreportName; }

/**
 * Sets the subreport name.
 */
public void setSubreportName(String aName)
{
    if(SnapUtils.equals(aName, getSubreportName())) return;
    firePropChange("SubreportName", _subreportName, _subreportName = aName);
    repaint(); _subreportShape = null;
}

/**
 * Returns the subreport shape.
 */
public RMShape getSubreportShape()
{
    // If not set, load and set shape
    if(_subreportShape==null && getSubreportName()!=null) {
        
        // Get subreport document from document
        RMDocument document = getDocument().getSubreport(getSubreportName());
        RMPage page = document!=null? document.getPage(0) : null;
        
        // If one page shape, get it (and move to upper left)
        if(page!=null && page.getChildCount()==1) {
            _subreportShape = (RMParentShape)page.getChild(0);
            _subreportShape.setXY(0,0);
        }
        
        // Otherwise if multiple page shapes, get shapes inside wrapper shape (not page)
        else if(page!=null && page.getChildCount()>1) {
            _subreportShape = new RMParentShape();
            _subreportShape.setSize(page.getSize());
            while(page.getChildCount()>0)
                _subreportShape.addChild(page.getChild(0));
        }
    }
    
    // Return subreport shape
    return _subreportShape;
}

/**
 * Override to paint subreport.
 */
public void paintShape(Painter aPntr)
{
    // If not editing, just paint subreport shape and return (shouldn't happen)
    if(!RMShapePaintProps.isEditing(aPntr)) {
        if(getSubreportShape()!=null) getSubreportShape().paint(aPntr); return; }
    
    // If subreport shape, paint it
    if(getSubreportShape()!=null) {
        double opacity = aPntr.getOpacity(); aPntr.setOpacity(.5);
        getSubreportShape().paint(aPntr);
        aPntr.setOpacity(opacity);
    }
    
    // Paint rect on top
    Rect bnds = getBoundsInside();
    aPntr.setPaint(getCoverFill().copyFor(bnds)); aPntr.fill(getPath());
    aPntr.setPaint(Color.DARKGRAY); aPntr.draw(bnds);

    // Draw TableGroup button
    aPntr.drawButton(getWidth() - 101, getHeight() - 20, 100, 20, false);
    aPntr.setColor(Color.WHITE); aPntr.setFont(Font.Arial12.getBold());
    aPntr.drawString("Subreport", getWidth() - 100 + 24, getHeight() - 5);
}

/**
 * Override to make rounding a constant of 3.
 */
public float getRadius()  { return 3; }

/**
 * Returns clip shape for shape.
 */
public Shape getClipShape()  { return getPath(); }

/**
 * Returns the fill to use on top of subreport shape when painting in editor.
 */
private GradientPaint getCoverFill()
{
    if(_coverFill!=null) return _coverFill;
    RMColor c1 = new RMColor("#F7F8FC55"), c2 = new RMColor("#C5D3FF77"), c3 = new RMColor("#DEE9FF55");
    GradientPaint.Stop stops[] = GradientPaint.getStops(0, c1, .67, c2, 1, c3);
    return _coverFill = new GradientPaint(60, stops);
}

/**
 * Override to setReportMill in subreport shape and install.
 */
public RMShape rpgAll(ReportOwner anRptOwner, RMShape aParent)
{
    // See if shape is visible
    boolean visible = isVisible() && getSubreportShape()!=null;
    if(visible && getBinding("Visible")!=null) { String key = getBinding("Visible").getKey();
        visible = RMKeyChain.getBoolValue(anRptOwner, key); }

    // Generate report for SubreportShape
    RMShape rpg = visible? getSubreportShape().rpgAll(anRptOwner, aParent) : new RMShape();
    
    // Reposition to location of Subreport template and return
    if(rpg instanceof ReportOwner.ShapeList)
        for(RMShape child : rpg.getChildren()) child.setXY(getX(), getY());
    else rpg.setXY(getX(), getY());
    return rpg;
}

/**
 * XML archival.
 */
public XMLElement toXML(XMLArchiver anArchiver)
{
    // Do normal version and reset name
    XMLElement e = super.toXML(anArchiver); e.setName("subreport");
    
    // Archive subreport name
    if(getSubreportName()!=null && getSubreportName().length()>0)
        e.add("subreport-name", getSubreportName());
    
    // Return xml element
    return e;
}

/**
 * XML unarchival.
 */
public RMSubreport fromXML(XMLArchiver anArchiver, XMLElement anElement)
{
    // Do normal version
    super.fromXML(anArchiver, anElement);
    
    // Unarchive subreport name
    if(anElement.hasAttribute("subreport-name"))
        setSubreportName(anElement.getAttributeValue("subreport-name"));
    
    // Return this subreport
    return this;
}

}