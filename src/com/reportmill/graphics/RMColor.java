/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;
import snap.gfx.Color;

/**
 * This class represents an RGBA color.
 */
public class RMColor extends Color {
    
    // Common Colors
    public static RMColor black = new RMColor(0d);
    public static RMColor blue = new RMColor(0f, 0f, 1f);
    public static RMColor cyan = new RMColor(1f, 0f, 0f, 0f, 1f);
    public static RMColor darkGray = new RMColor(.333f);
    public static RMColor gray = new RMColor(.5f);
    public static RMColor green = new RMColor(0f, 1f, 0f);
    public static RMColor lightGray = new RMColor(.667f);
    public static RMColor magenta = new RMColor(0f, 1f, 0f, 0f, 1f);
    public static RMColor orange = new RMColor(1f, 200/255f, 0f);
    public static RMColor pink = new RMColor(1f, 175/255f, 175/255f);
    public static RMColor red = new RMColor(1f, 0f, 0f);
    public static RMColor white = new RMColor(1d);
    public static RMColor yellow = new RMColor(0f, 0f, 1f, 0f, 1f);
    public static RMColor clear = new RMColor(0f, 0f, 0f, 0f);
    public static RMColor lightBlue = new RMColor(.333f, .333f, 1f);
    public static RMColor clearWhite = new RMColor(1f, 1f, 1f, 0f);

/**
 * Creates a plain black opaque color.
 */
public RMColor() { }

/**
 * Creates a color with the given gray value (0-1).
 */
public RMColor(double g)  { super(g); }

/**
 * Creates a color with the given gray and alpha values (0-1).
 */
public RMColor(double g, double a)  { super(g,a); }

/**
 * Creates a color with the given red, green blue values (0-1).
 */
public RMColor(double r, double g, double b)  { super(r,g,b); }

/**
 * Creates a color with the given red, green blue values (0-1).
 */
public RMColor(int r, int g, int b)  { super(r,g,b); }

/**
 * Creates a color with the given red, green blue values (0-1).
 */
public RMColor(int r, int g, int b, int a)  { super(r,g,b,a); }

/**
 * Creates a color with the given red, green, blue values (0-1).
 */
public RMColor(double r, double g, double b, double a)  { super(r,g,b,a); }

/**
 * Creates a color with the given cyan, magenta, yellow, black and alpha values (0-1). Bogus right now.
 */
public RMColor(double c, double m, double y, double k, double a)  { super(c,m,y,k,a); }

/**
 * Creates an RMColor from the given AWT color.
 */
public RMColor(int anRGB)  { super(anRGB); }

/**
 * Creates a new color from the given hex string.
 */
public RMColor(String aHexString)  { super(aHexString); }

/**
 * Returns a color brighter than this color (blended with white).
 */
public RMColor brighter()  { return blend(white, .25); }

/**
 * Returns a color darker than this color (blended with black).
 */
public RMColor darker()  { return blend(black, .25); }

/**
 * Returns a color darker than this color (by this given fraction).
 */
public RMColor blend(RMColor aColor, double aFraction)  { return get(super.blend(aColor, aFraction)); }

/**
 * Returns a random color.
 */
public static RMColor getRandom()  { return get(Color.getRandom()); }
    
/**
 * Returns a color value for a given object.
 */
public static RMColor get(Object anObj)  { return get(Color.get(anObj)); }

/**
 * Returns a random color.
 */
public static RMColor get(Color c)
{
    if(c==null || c instanceof RMColor) return (RMColor)c;
    return new RMColor(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
}
    
}