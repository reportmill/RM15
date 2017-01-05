/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;
import snap.gfx.*;

/**
 * This class represents a simple geometric rectangle. Legacy, use snap.gfx.Rect.
 */
public class RMRect extends Rect {

    // DivdeRect constants
    public static final byte MinXEdge = 1;
    public static final byte MinYEdge = 1<<1;
    public static final byte MaxXEdge = 1<<2;
    public static final byte MaxYEdge = 1<<3;

/** Creates an empty rect. */
public RMRect() { }

/** Creates a rect with the given x, y, width and height (doubles). */
public RMRect(double x, double y, double w, double h)  { super(x, y, w, h); }

/**
 * Slices rect by given amount (from given edge) - returns remainder.
 */
public static Rect divideRect(Rect aRect, double anAmt, byte anEdge, Rect aRmndr)
{
    if(aRmndr!=null)
        aRmndr.setRect(aRect);
    
    switch(anEdge) {
        case MinXEdge: aRect.width = anAmt; if(aRmndr!=null) { aRmndr.x += anAmt; aRmndr.width -= anAmt; } break;
        case MinYEdge: aRect.height = anAmt; if(aRmndr!=null) { aRmndr.y += anAmt; aRmndr.height -= anAmt; } break;
        case MaxXEdge:
            aRect.x = aRect.getMaxX() - anAmt; aRect.width = anAmt; if(aRmndr!=null) aRmndr.width -= anAmt; break;
        case MaxYEdge:
            aRect.y = aRect.getMaxY() - anAmt; aRect.height = anAmt; if(aRmndr!=null) aRmndr.height -= anAmt; break;
    }
    
    return aRmndr==null? aRect : aRmndr;
}

}