/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;
import java.util.*;
import snap.gfx.*;
import snap.gfx.GradientPaint.Stop;
import snap.util.*;

/**
 * This class represents a fill that draws a linear gradient between an arbitrary list of colors.
 */
public class RMGradientFill extends RMFill {
    
    // The snap gradient fill
    GradientPaint  _snap;
    
/**
 * Creates an uninitialized gradient fill.
 */
public RMGradientFill()  { _snap = new GradientPaint(); }

/**
 * Creates a gradient fill from the given start color to the given end color with the given roll.
 */
public RMGradientFill(RMColor aColor1, RMColor aColor2, float aRotation)
{
    _snap = new GradientPaint(aRotation, GradientPaint.getStops(0, aColor1, 1, aColor2));
}

/**
 * Creates a new gradient fill.
 */
public RMGradientFill(double aSX, double aSY, double aEX, double aEY, Stop theStops[])
{
    _snap = new GradientPaint(aSX, aSY, aEX, aEY, theStops);
}

/**
 * Returns the start x.
 */
public double getStartX()  { return _snap.getStartX(); }

/**
 * Returns the start y.
 */
public double getStartY()  { return _snap.getStartY(); }

/**
 * Returns the end x.
 */
public double getEndX()  { return _snap.getEndX(); }

/**
 * Returns the end y.
 */
public double getEndY()  { return _snap.getEndY(); }

/** 
 * Returns the number of color stops in the gradient
 */
public int getStopCount()  { return _snap.getStopCount(); }

/**
 * Returns the individual color stop at given index.
 */
public Stop getStop(int anIndex)  { return _snap.getStop(anIndex); }

/**
 * Returns the color of the stop at the given index.
 */
public RMColor getStopColor(int index)  { return RMColor.get(getStop(index).getColor()); }

/**
 * Returns the position (in the range {0-1}) for the given stop index.
 */
public double getStopOffset(int index)  { return getStop(index).getOffset(); }

/**
 * Returns the list of color stops.
 */
public Stop[] getStops()  { return _snap.getStops(); }

/**
 * Returns whether gradient is linear.
 */
public boolean isLinear()  { return _snap.isLinear(); }

/**
 * Returns whether gradient is radial.
 */
public boolean isRadial()  { return _snap.isRadial(); }

/**
 * Returns the gradient's rotation.
 */
public double getRoll()  { return _snap.getRoll(); }

/**
 * Returns the color associated with this fill.
 */
public RMColor getColor()  { return getStopColor(0); }

/**
 * Returns the snap version of this fill.
 */
public GradientPaint snap()  { return _snap; }

/**
 * Returns a new gradient which is a copy of this gradient with a different gradient axis.
 */
public RMGradientFill copyForPoints(Point begin, Point end)
{
    RMGradientFill clone = clone(); clone._snap = _snap.copyFor(begin.x,begin.y,end.x,end.y); return clone;
}

/**
 * Resets all the stops from the new list.
 */
public RMGradientFill copyForStops(Stop theStops[])
{
    RMGradientFill clone = clone(); clone._snap = _snap.copyForStops(theStops); return clone;
}

/**
 * Returns a new gradient which is a copy of this gradient but of a different type.
 */
public RMGradientFill copyForType(GradientPaint.Type aType)
{
    if(aType==_snap.getType()) return this;
    RMGradientFill clone = clone(); clone._snap = _snap.copyForType(aType); return clone;
}

/**
 * Derives an instance of this class from another fill.
 */
public RMGradientFill copyForColor(Color aColor)
{
    RMGradientFill clone = clone(); clone._color = aColor!=null? RMColor.get(aColor) : _color;
    GradientPaint.Stop stops[] = Arrays.copyOf(getStops(), getStopCount());
    stops[0] = new Stop(getStopOffset(0), aColor);
    clone._snap = _snap.copyForStops(stops); return clone;
}
  
/**
 * Returns a new gradient which is a copy of this gradient but with a different roll value.
 */
public RMGradientFill copyForRoll(double aRoll)
{
    RMGradientFill clone = clone(); clone._snap = _snap.copyForRoll(aRoll); return clone;
}

/**
 * Reverse the order of the color stops
 */
public RMGradientFill copyForReverseStops()
{
    int nstops = getStopCount(); Stop stops[] = new Stop[nstops];
    for(int i=0; i<nstops; i++) stops[nstops-i-1] = new Stop(1 - getStopOffset(i), getStopColor(i));
    return copyForStops(stops);
}

/**
 * Standard clone implementation.
 */
public RMGradientFill clone()
{
    RMGradientFill clone = (RMGradientFill)super.clone(); // Do normal clone
    clone._snap = _snap.clone();
    return clone;
}

/**
 * Standard equals implementation.
 */
public boolean equals(Object anObj)
{
    if(anObj==this) return true;
    RMGradientFill other = anObj instanceof RMGradientFill? (RMGradientFill)anObj : null; if(other==null) return false;
    return _snap.equals(other._snap);
}

/**
 * XML archival.
 */
public XMLElement toXML(XMLArchiver anArchiver)  { return _snap.toXML(anArchiver); }

/**
 * XML unarchival.
 */
public Object fromXML(XMLArchiver anArchiver, XMLElement anElement) { _snap.fromXML(anArchiver,anElement); return this;}

/**
 * Standard to string implementation.
 */
public String toString()  { return _snap.toString(); }

}