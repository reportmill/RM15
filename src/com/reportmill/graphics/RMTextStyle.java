/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.graphics;
import com.reportmill.base.RMFormat;
import snap.gfx.*;

/**
 * A class to hold style attributes for a text run.
 */
public class RMTextStyle implements Cloneable {

    // The style
    TextStyle          _style;
    
    // Constants for style attribute keys
    public static final String FONT_KEY = TextStyle.FONT_KEY;
    public static final String COLOR_KEY = TextStyle.COLOR_KEY;
    public static final String FORMAT_KEY = TextStyle.FORMAT_KEY;
    public static final String UNDERLINE_KEY = TextStyle.UNDERLINE_KEY;
    public static final String BORDER_KEY = TextStyle.BORDER_KEY;
    public static final String OUTLINE_KEY = TextStyle.BORDER_KEY;
    public static final String SCRIPTING_KEY = TextStyle.SCRIPTING_KEY;
    public static final String CHAR_SPACING_KEY = TextStyle.CHAR_SPACING_KEY;
    
/**
 * Creates a new RMTextStyle.
 */
public RMTextStyle(TextStyle aStyle)  { _style = aStyle; }

/**
 * Returns the font for this run.
 */
public RMFont getFont()  { return RMFont.get(_style.getFont()); }

/**
 * Returns the color for this run.
 */
public RMColor getColor()  { return RMColor.get(_style.getColor()); }

/**
 * Returns whether this run is underlined.
 */
public boolean isUnderlined()  { return _style.isUnderlined(); }

/**
 * Returns the scripting for this run (1=SuperScripting, -1=Subscripting, 0=none).
 */
public int getScripting()  { return _style.getScripting(); }

/**
 * Returns the char spacing.
 */
public double getCharSpacing()  { return _style.getCharSpacing(); }

/**
 * Returns the format.
 */
public RMFormat getFormat()  { return (RMFormat)_style.getFormat(); }

/**
 * Returns the text border.
 */
public Border getBorder()  { return _style.getBorder(); }

/**
 * Clone for Style value.
 */
public RMTextStyle copyFor(Object ... theVals)
{
    RMTextStyle cln = clone(); cln._style = _style.copyFor(theVals); return cln;
}

/**
 * Clone with key/value.
 */
public RMTextStyle copyFor(String aKey, Object aValue)
{
    RMTextStyle clone = clone(); //if(aKey.equals(OUTLINE_KEY)) clone._outline = (Outline)aValue;
    clone._style = _style.copyFor(aKey, aValue);
    return clone;
}

/**
 * Standard clone implementation.
 */
public RMTextStyle clone()
{
    try { return (RMTextStyle)super.clone(); }
    catch(CloneNotSupportedException e) { throw new RuntimeException(e); }
}

/**
 * Standard equals implementation.
 */
public boolean equals(Object anObj)
{
    if(anObj==this) return true;
    RMTextStyle other = anObj instanceof RMTextStyle? (RMTextStyle)anObj : null; if(other==null) return false;
    return other._style.equals(_style);
}

/**
 * Standard toString implementation.
 */
public String toString()  { return _style.toString(); }

/**
 * Returns the most likely key for a given style attribute.
 */
public static String getStyleKey(Object anAttr)  { return TextStyle.getStyleKey(anAttr); }

}