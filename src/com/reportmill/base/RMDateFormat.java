/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.base;
import com.reportmill.shape.RMDocument;
import java.util.*;
import java.text.*;
import snap.gfx.TextStyle;
import snap.util.*;

/**
 * This is just a SimpleDateFormat subclass to support RM archiving and legacy formats.
 */
public class RMDateFormat implements RMFormat, Cloneable {
    
    // The format
    SimpleDateFormat  _fmt = new SimpleDateFormat();
    
    // The string to be substituted if asked to format null
    String            _nullString = "<N/A>";
    
    // The local of the format
    Locale            _locale;
    
    // Shared common formats
    public static RMDateFormat BASIC = new RMDateFormat("MM/dd/yyyy");
    public static RMDateFormat DEFAULT = new RMDateFormat("MMM dd, yyyy");
    public static RMDateFormat defaultFormat = DEFAULT;

/**
 * Creates a plain format.
 */
public RMDateFormat()  { }

/**
 * Creates a format from the given string format.
 */
public RMDateFormat(String aFormat)  { setPattern(aFormat); }

/**
 * Returns the String that is substituted when this format is asked to provide stringForObjectValue(null).
 */
public String getNullString()  { return _nullString; }

/**
 * Sets the String that is substituted when this format is asked to provide stringForObjectValue(null).
 */
public void setNullString(String aString)  { _nullString = aString; }

/**
 * Returns the date format string.
 */
public String getPattern()  { return _fmt.toPattern(); }

/**
 * Sets the date format string. Has support for legacy RM formats and Java style.
 */
public void setPattern(String aFormat)  { _fmt.applyPattern(aFormat); }

/**
 * Formats the given object.
 */
public String format(Object anObj) 
{
    // If locale hasn't been set, get it from RMDocument locale
    if(_locale != RMDocument._locale) {
        _locale = RMDocument._locale;
        _fmt.setDateFormatSymbols(new DateFormatSymbols(RMDocument._locale));
    }

    // If object is date, return date format
    if(anObj instanceof Date)
        return _fmt.format(anObj);
    
    // If object isn't date, just return null string
    return _nullString;
}

/**
 * Returns the format style.
 */
public TextStyle formatStyle(Object anObj)  { return null; }

/**
 * Returns the time zone.
 */
public TimeZone getTimeZone()  { return _fmt.getTimeZone(); }

/**
 * Sets the time zone.
 */
public void setTimeZone(TimeZone aTZ)  { _fmt.setTimeZone(aTZ); }

/**
 * Tries to parse a number from given string using this format.
 */
public Date parse(String aStr)
{
    try { return _fmt.parse(aStr); }
    catch(Exception e) { return null; }
}

/**
 * Standard equals implementation.
 */
public boolean equals(Object anObj)
{
    // Return true if given object is this format
    if(anObj==this) return true;
    
    // Return false if given object isn't a format
    if(!getClass().isInstance(anObj)) return false;
    
    // Get other date format
    RMDateFormat other = (RMDateFormat)anObj;
    
    // Return false if other format null string isn't equal to this null string
    if(!SnapUtils.equals(other._nullString, _nullString)) return false;
    
    // Return result of super equals
    return super.equals(anObj);
}

/**
 * Standard clone implementation.
 */
public RMDateFormat clone()
{
    RMDateFormat clone = null; try { clone = (RMDateFormat)super.clone(); }
    catch(CloneNotSupportedException e) { throw new RuntimeException(e); }
    clone._fmt = (SimpleDateFormat)_fmt.clone();
    return clone;
}

/** XML archival. */
public XMLElement toXML(XMLArchiver anArchiver)
{
    // Get new element named format
    XMLElement e = new XMLElement("format");
    
    // Set type to date
    e.add("type", "date");
    
    // Archive pattern
    e.add("pattern", _fmt.toPattern());
    
    // Archive _nullString
    if(_nullString!=null && _nullString.length()>0)
        e.add("null-string", _nullString);
    
    // Return element
    return e;
}

/** XML unarchival. */
public Object fromXML(XMLArchiver anArchiver, XMLElement anElement)
{
    // Unarchive Pattern
    _fmt.applyPattern(anElement.getAttributeValue("pattern"));
    
    // Unarchive _nullString
    _nullString = anElement.getAttributeValue("null-string");
    
    // Return this format
    return this;
}

/**
 * Returns string representation of this format.
 */
public String toString()  { return getPattern(); }

}