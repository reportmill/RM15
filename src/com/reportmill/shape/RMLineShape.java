/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import com.reportmill.graphics.*;
import snap.gfx.*;
import snap.util.*;

/**
 * This class represents a simple line shape, drawing a line from a start point to an end point.
 */
public class RMLineShape extends RMParentShape {
    
/**
 * Creates a basic line (a point actually at 0,0).
 */
public RMLineShape()  { setStroke(new RMStroke()); }

/**
 * Creates a basic black line from the given x1, y1 to the given x2, y2.
 */
public RMLineShape(double x1, double y1, double x2, double y2)  { this(); setFrame(x1, y1, x2-x1, y2-y1); }

/**
 * Returns the line path.
 */
public Shape getPath()
{
    double x1 = _width<0? -_width : 0, y1 = _height<0? -_height : 0;
    double x2 = _width>0? _width : 0, y2 = _height>0? _height : 0;
    return new Line(x1, y1, x2, y2);
}

/**
 * Returns the line segment arrow head.
 */
public ArrowHead getArrowHead()  { return getChildCount()==0? null : (ArrowHead)getChild(0); }

/**
 * Sets the line segment arrow head.
 */
public void setArrowHead(ArrowHead anArrowHead)
{
    repaint(); // Register for repaint
    removeChildren(); // Remove children then add arrow head
    if(anArrowHead!=null) addChild(anArrowHead);
}

/**
 * Override to prevent arrow heads from selecting.
 */
public boolean childrenSuperSelectImmediately()  { return false; }

/**
 * Override to handle arrow heads special.
 */
public void setStroke(RMStroke aStroke)
{
    // Sets stroke
    super.setStroke(aStroke);
    
    // Get arrow head (just return if null)
    ArrowHead arrowHead = getArrowHead(); if(arrowHead==null) return;
    
    // If stroke, set arrow head to stroke color and scale to line width
    if(aStroke!=null) {
        arrowHead.setColor(aStroke.getColor());
        arrowHead.setScaleXY(aStroke.getWidth(), aStroke.getWidth());
    }
    
    // Otherwise, just clear arrow head fill
    else arrowHead.setFill(null);
}

/**
 * Override to handle arrow heads special.
 */
public void setStrokeColor(RMColor aColor)
{
    super.setStrokeColor(aColor);
    if(getArrowHead()!=null) getArrowHead().setColor(aColor);
}

/**
 * Override to handle arrow heads special.
 */
public void setStrokeWidth(float aValue)
{
    super.setStrokeWidth(aValue);
    if(getArrowHead()!=null) getArrowHead().setScaleXY(aValue, aValue);
}

/**
 * Called to layout line arrow heads.
 */
protected void layoutChildren()
{
    // Get line and arrow head (just return if no arrow head)
    ArrowHead head = getArrowHead(); if(head==null) return;
    
    // Use width() instead of getWidth(), because we need the sign
    double w = width(), h = height();
    double angle = Math.atan2(h, w)*180/Math.PI;
    double x = w<0 ? 0 : w;
    double y = h<0 ? 0 : h;
    
    // Reset head location
    head.setRoll(angle);
    head.setXY(0,0);
    Point originInParent = head.convertedPointToShape(head.getOrigin(), this);
    head.setXY(x - originInParent.getX(), y - originInParent.getY());
}

/**
 * Override to prevent width from going to zero.
 */
public void setWidth(double aWidth)
{
    if(getArrowHead()!=null && Math.abs(aWidth)<=0.15) super.setWidth(0);
    else super.setWidth(aWidth);
}

/**
 * Override to prevent width from going to zero.
 */
public void setHeight(double aHeight)
{
    if(getArrowHead()!=null && Math.abs(aHeight)<=0.15) super.setHeight(0);
    else super.setHeight(aHeight);
}

/**
 * XML archival.
 */
protected XMLElement toXMLShape(XMLArchiver anArchiver)
{
    XMLElement e = super.toXMLShape(anArchiver); e.setName("line"); return e;
}

/**
 * A line segment arrow head.
 */
public static class ArrowHead extends RMPolygonShape {

    // Point, in path coords, that gets attached to end (for arrowheads) or start (for arrow tails) of the line 
    Point     _origin = new Point();

    /** Creates a new arrow head. */
    public ArrowHead() { }
    
    /** Creates a new arrow head of type. */
    public ArrowHead(int aType) 
    {
        RMPath p = new RMPath();
        p.moveTo(0,0); p.lineTo(-2.828, 2.828); p.lineTo(-2.121, 3.535);
        p.lineTo(1.4142, 0); p.lineTo(-2.121, -3.535); p.lineTo(-2.828, -2.828); p.close();
        Rect r = p.getBounds();
        setSize(r.getWidth(), r.getHeight());
        setPath(p);
    }
    
    /** Returns the point, in the shape's coordinate system, of the origin (attachment point). */
    public Point getOrigin()  { return _origin; }
    
    /** Overridden to indicate arrow head is always funky. */
    public boolean notRSS()  { return false; }
    
    /** Overridden from RMShape to change the center of rotation to the arrowhead origin. */
    public RMTransform getTransform()
    {
        // Get location, size, point of rotation, rotation, scale, skew
        double x = getX(), y = getY(), prx = _origin.x, pry = _origin.y;
        double roll = getRoll(), sx = getScaleX(), sy = getScaleY();
        
        // Transform about point of rotation and return
        RMTransform t = new RMTransform(); t.translate(-prx, -pry);
        if(sx!=1 || sy!=1) t.scale(sx, sy);
        if(roll!=0) t.rotate(roll);
        t.translate(prx + x, pry + y); return t;
    }
    
    /** XML archival. */
    public XMLElement toXMLShape(XMLArchiver anArchiver)
    {
        XMLElement e = super.toXMLShape(anArchiver); e.setName("arrow-head");
        if(_origin.getX() != 0) e.add("xorigin", _origin.getX());
        if(_origin.getY() != 0) e.add("yorigin", _origin.getY());
        return e;
    }
    
    /** XML unarchival. */
    protected void fromXMLShape(XMLArchiver anArchiver, XMLElement anElement)
    {
        super.fromXMLShape(anArchiver, anElement);
        float x = anElement.getAttributeFloatValue("xorigin", 0);
        float y = anElement.getAttributeFloatValue("yorigin", 0);
        _origin = new Point(x,y);
        
        // Origin is relative to path bounds instead of shape bounds - need to fix now that path always in shape bounds
        Rect prect = _path.getBounds(), srect = getBoundsInside();
        double sx = srect.getWidth()/prect.getWidth(), sy = srect.getHeight()/prect.getHeight();
        double tx = -prect.getX()*sx, ty = -prect.getY()*sy;
        new RMTransform(sx,0,0,sy,tx,ty).transform(_origin);
    }
}

}