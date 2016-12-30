/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.base;
import snap.util.*;

/**
 * This is a sort subclass to support sorting objects by their relative position in an array of values.
 */
public class RMTopNSort extends RMSort {

    // The count of items to keep
    int       _count;
    
    // Whether to include others
    boolean   _includeOthers;
    
    // Whether to pad to top n value
    boolean   _pad;
    
/**
 * Creates a new value sort.
 */
public RMTopNSort() { }

/**
 * Creates a new value sort.
 */
public RMTopNSort(String aKey, int anOrder, int aCount, boolean includeOthers)
{
    _key = aKey;
    _order = anOrder;
    _count = aCount;
    _includeOthers = includeOthers;
}

/**
 * Returns the count.
 */
public int getCount()  { return _count; }

/**
 * Sets the count.
 */
public void setCount(int aCount)  { _count = aCount; }

/**
 * Returns whether to include others.
 */
public boolean getIncludeOthers()  { return _includeOthers; }

/**
 * Sets whether to include others.
 */
public void setIncludeOthers(boolean includeOthers)  { _includeOthers = includeOthers; }

/**
 * Returns whether to pad to top N value.
 */
public boolean getPad()  { return _pad; }

/**
 * Sets whether to pad to top N value.
 */
public void setPad(boolean aValue)  { _pad = aValue; }

/**
 * Standard equals implementation.
 */
public boolean equals(Object anObj)
{
    // Check identity
    if(anObj==this) return true;
    
    // Check super
    if(!super.equals(anObj)) return false;
    
    // Check class
    if(!(anObj instanceof RMTopNSort)) return false;
    
    // Get other top n sort
    RMTopNSort other = (RMTopNSort)anObj;
    
    // Check count
    if(other._count!=_count) return false;
    
    // Check include others
    if(other._includeOthers!=_includeOthers) return false;
    
    // Check pad
    if(other._pad!=_pad) return false;
    
    // Return true if all checks passed
    return true;
}

/**
 * XML archival.
 */
public XMLElement toXML(XMLArchiver anArchiver)
{
    // Archive basic sort attributes
    XMLElement e = new XMLElement("sort");
    
    // Set type
    e.add("type", "top-n");
    
    // Archive count
    e.add("count", getCount());

    // Archive include others
    if(getIncludeOthers())
        e.add("include-others", true);
    
    // Archive pad
    if(getPad())
        e.add("pad", true);
    
    // Return element
    return e;
}

/**
 * XML unarchival.
 */
public Object fromXML(XMLArchiver anArchiver, XMLElement anElement)
{
    // Unarchive basic sort attributes
    super.fromXML(anArchiver, anElement);
    
    // Unarchive count
    setCount(anElement.getAttributeIntValue("count"));
    
    // Unarchive include others
    if(anElement.hasAttribute("include-others"))
        setIncludeOthers(anElement.getAttributeBoolValue("include-others"));

    // Unarchive pad
    if(anElement.hasAttribute("pad"))
        setPad(anElement.getAttributeBoolValue("pad"));

    // Return sort
    return this;
}

/**
 * Returns a string representation of top n sort.
 */
public String toString()
{
    return "TopNSort: { key=" + getKey() + ", count=" + getCount() + ", incd-othrs=" + getIncludeOthers() +
        ", pad=" + getPad() + " }";
}

}