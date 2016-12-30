/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;
import snap.gfx.*;

/**
 * This class extends Rectanlge2D to add some convenient rect methods.
 */
public class RMRect extends Rect {

    // A shared zero rect
    public static final RMRect zeroRect = new RMRect(0,0,0,0);
    
    // A shared unit rect
    public static final RMRect unitRect = new RMRect(0,0,1,1);

    // DivdeRect constants
    public static final byte MinXEdge = 1;
    public static final byte MinYEdge = 1<<1;
    public static final byte MaxXEdge = 1<<2;
    public static final byte MaxYEdge = 1<<3;

/**
 * Creates an empty rect.
 */
public RMRect() { }

/**
 * Creates a rect with the given x, y, width and height (doubles).
 */
public RMRect(double x, double y, double w, double h)  { super(x, y, w, h); }

/**
 * Creates a rect from an awt rect.
 */
public RMRect(Rect aRect)  { this(aRect.getX(), aRect.getY(), aRect.getWidth(), aRect.getHeight()); }

/**
 * Creates a rect encompassing the receiver and the given rect.
 */
public RMRect getUnionRect(Rect r2)  { RMRect copy = new RMRect(r2); copy.unionEvenIfEmpty(r2); return copy; }

/**
 * Slices rect by given amount (from given edge) - returns remainder.
 */
public RMRect divideRect(double anAmount, byte anEdge)  { return divideRect(anAmount, anEdge, new RMRect()); }

/**
 * Slices rect by given amount (from given edge) - returns remainder.
 */
public RMRect divideRect(double anAmount, byte anEdge, RMRect aRmndr)
{
    if(aRmndr!=null)
        aRmndr.setRect(this);
    
    switch(anEdge) {
        case MinXEdge: width = anAmount; if(aRmndr!=null) { aRmndr.x += anAmount; aRmndr.width -= anAmount; } break;
        case MinYEdge: height = anAmount; if(aRmndr!=null) { aRmndr.y += anAmount; aRmndr.height -= anAmount; } break;
        case MaxXEdge: x = getMaxX() - anAmount; width = anAmount; if(aRmndr!=null) aRmndr.width -= anAmount; break;
        case MaxYEdge: y = getMaxY() - anAmount; height = anAmount; if(aRmndr!=null) aRmndr.height -= anAmount; break;
    }
    
    return aRmndr==null? this : aRmndr;
}

/**
 * Returns the mask of edges hit by the given point.
 */
public static int getHitEdges(Rect aRect, Point aPoint, double aRadius)
{
    // Check MinXEdge, MaxXEdge, MinYEdge, MaxYEdge
    int hitEdges = 0;
    if(Math.abs(aPoint.getX()-aRect.getX()) < aRadius) hitEdges |= MinXEdge;
    else if(Math.abs(aPoint.getX()-aRect.getMaxX()) < aRadius) hitEdges |= MaxXEdge;
    if(Math.abs(aPoint.getY()-aRect.getY()) < aRadius) hitEdges |= MinYEdge;
    else if(Math.abs(aPoint.getY()-aRect.getMaxY()) < aRadius) hitEdges |= MaxYEdge;
    return hitEdges;
}

/**
 * Resets the edges of a rect, given a mask of edges and a new point.
 */
public static void setHitEdges(Rect aRect, Point aPoint, int anEdgeMask)
{
    // Handle MinXEdge drag
    if((anEdgeMask & MinXEdge) > 0) {
        double newX = Math.min(aPoint.getX(), aRect.getMaxX()-1);
        aRect.setWidth(aRect.getMaxX() - newX); aRect.setX(newX); }
    
    // Handle MaxXEdge drag
    else if((anEdgeMask & MaxXEdge) > 0)
        aRect.setWidth(Math.max(1, aPoint.getX() - aRect.getX()));
    
    // Handle MinYEdge drag
    if((anEdgeMask & MinYEdge) > 0) {
        double newY = Math.min(aPoint.getY(), aRect.getMaxY()-1);
        aRect.setHeight(aRect.getMaxY() - newY); aRect.setY(newY); }
    
    // Handle MaxYEdge drag
    else if((anEdgeMask & MaxYEdge) > 0)
        aRect.setHeight(Math.max(1, aPoint.getY() - aRect.getY()));
}

}