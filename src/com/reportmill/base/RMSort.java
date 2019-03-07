/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.base;
import java.util.*;
import snap.util.*;

/**
 * This class provides a basic description for comparison and sorting with a simple key and order (ascending or
 * descending). You can create a new order like this:
 * <p><blockquote><pre>
 *   RMSort titleFirst = new RMSort("getTitle");
 *   RMSort bestRevenueFirst = new RMSort("getRevenue", ORDER_DESCEND);
 * </pre></blockquote><p>
 * This class also provides useful static methods for comparison and sorting:
 * <p><blockquote><pre>
 *   List mySortList = new ArrayList();
 *   mySortList.add(bestRevenueFirst);
 *   mySortList.add(titleFirst);
 *   RMSort.sort(myList, mySortList); // Sort myList by revenue and title
 * </blockquote></pre>
 */
public class RMSort implements Comparator, Cloneable, XMLArchiver.Archivable {

    // The key that is evaluated on objects to be sorted
    String   _key;
    
    // The sort order: ascending or descending
    int      _order = getOrderDefault();

    // Constants for sort order
    public static final byte ORDER_SAME = 0;
    public static final byte ORDER_ASCEND = -1;
    public static final byte ORDER_DESCEND = 1;
    public static final byte ORDER_INDETERMINATE = 2;
    
    // A default Comparator
    public static Comparator COMPARATOR = new Comparator() {
        public int compare(Object obj1, Object obj2) { return RMSort.Compare(obj1, obj2); }};

/**
 * Creates a plain sort with no key. Used for unarchival, shouldn't be called directly.
 */
public RMSort() { }

/**
 * Creates a sort with the given key and ORDER_ASCEND.
 */
public RMSort(String aKey)  { _key = aKey; }

/**
 * Creates a sort with the given key and order.
 */
public RMSort(String aKey, int anOrder)  { _key = aKey; _order = anOrder; }

/**
 * Returns the key for this sort.
 */
public String getKey()  { return _key; }

/**
 * Sets the key for this sort.
 */
public void setKey(String aKey)  { _key = aKey; }

/**
 * Returns the order for this sort.
 */
public int getOrder()  { return _order; }

/**
 * Sets the order for this sort.
 */
public void setOrder(int anOrder)  { _order = anOrder; }

/**
 * Returns the order default.
 */
public int getOrderDefault()  { return ORDER_ASCEND; }

/**
 * Returns the order as a string.
 */
public String getOrderString()  { return _order==ORDER_ASCEND? "ascend" : _order==ORDER_DESCEND? "descend" : "same"; }

/**
 * Sets the order as a string.
 */
public void setOrderString(String aString)
{
    if(aString.equals("ascend")) _order = ORDER_ASCEND;
    else if(aString.equals("descend")) _order = ORDER_DESCEND;
    else if(aString.equals("same")) _order = ORDER_SAME;
}

/**
 * Toggles the order for this sort between ORDER_ASCEND<->ORDER_DESCEND.
 */
public void toggleOrder()
{
    _order = _order==ORDER_ASCEND? ORDER_DESCEND : _order==ORDER_DESCEND? ORDER_ASCEND : ORDER_SAME;
}

/**
 * Returns how the two given objects compare with this sort.
 */
public int compare(Object obj1, Object obj2)
{
    // Get values for objects and sort key
    Object val1 = RMKeyChain.getValue(obj1, getKey());
    Object val2 = RMKeyChain.getValue(obj2, getKey());
    
    // Get standard compare result
    int compare = RMSort.Compare(val1, val2);
    
    // If order is descending, flip result
    if(getOrder()==RMSort.ORDER_DESCEND)
        compare = -compare;
    
    // Return compare result
    return compare;
}

/**
 * Compare two value objects (assumed to be String, Number, Boolean, Date, Comparable, etc.).
 */
public static int Compare(Object anObj1, Object anObj2)
{
    // Convert keychains to strings
    if(anObj1 instanceof RMKeyChain) anObj1 = anObj1.toString();
    if(anObj2 instanceof RMKeyChain) anObj2 = anObj2.toString();
    
    // Handle identity
    if(anObj1==anObj2) return ORDER_SAME;
    
    // Handle either null: make any existing object come first (opposite if other is String)
    if(anObj1==null) return anObj2 instanceof String? ORDER_DESCEND : ORDER_ASCEND;
    if(anObj2==null) return anObj1 instanceof String? ORDER_ASCEND : ORDER_DESCEND;
    
    // Handle String (ignore case)
    if(anObj1 instanceof String && anObj2 instanceof String) {
        int order = ((String)anObj1).compareToIgnoreCase((String)anObj2);
        return order<0? ORDER_ASCEND : order>0? ORDER_DESCEND : ORDER_SAME;
    }
    
    // Handle Number
    else if(anObj1 instanceof Number && anObj2 instanceof Number) {
        double value1 = ((Number)anObj1).doubleValue();
        double value2 = ((Number)anObj2).doubleValue();
        if(value1 < value2) return ORDER_ASCEND;
        if(value1 > value2) return ORDER_DESCEND;
    }

    // Handle Date
    else if(anObj1 instanceof Date && anObj2 instanceof Date) {
        if(((Date)anObj1).before((Date)anObj2)) return ORDER_ASCEND;
        if(((Date)anObj1).after((Date)anObj2)) return ORDER_DESCEND;
    }
    
    // Handle Boolean
    else if(anObj1 instanceof Boolean && anObj2 instanceof Boolean) {
        if(!(((Boolean)anObj1).booleanValue()) && (((Boolean)anObj2).booleanValue())) return ORDER_ASCEND;
        if((((Boolean)anObj1).booleanValue()) && !(((Boolean)anObj2).booleanValue())) return ORDER_DESCEND;
    }
    
    // Handle Comparable objects
    else if(anObj1 instanceof Comparable)
        return ((Comparable)anObj1).compareTo(anObj2);
    
    // Handle everything else
    else if(!anObj1.equals(anObj2))
        return ORDER_ASCEND;

    // If nothing above applies, just return order same
    return ORDER_SAME;
}

/**
 * Returns the given list sorted by the given list of sorts.
 */
public static void sort(List aList, List <RMSort> aSortList)
{
    Collections.sort(aList, new RMSortsComparator(aSortList));
}

/**
 * A comparator that compares with a given list of sorts.
 */
private static class RMSortsComparator implements Comparator {

    // The list of sorts
    List <RMSort> _sorts;

    // Creates a new RMSortsComparator
    public RMSortsComparator(List <RMSort> aSortList)  { _sorts = aSortList; }

    // Compares two objects with sorts list
    public int compare(Object obj1, Object obj2)
    {
        // Iterate over sorts: Compare and if not equal, return result
        for(int i=0, iMax=_sorts.size(); i<iMax; i++) { RMSort sort = _sorts.get(i);
            int compare = sort.compare(obj1, obj2);
            if(compare!=0)
                return compare;
        }
        
        // If no more sorts, return 0
        return 0;
    }
}

/**
 * Standard equals implementation.
 */
public boolean equals(Object anObj)
{
    // Check identity, and class and get other
    if(anObj==this) return true;
    RMSort other = anObj instanceof RMSort? (RMSort)anObj : null; if(other==null) return false;
    
    // Check Key, Order
    if(!SnapUtils.equals(other._key, _key)) return false;
    if(other._order!=_order) return false;
    return true; // Return true since all checks passed
}

/**
 * Standard clone implementation.
 */
public RMSort clone()
{
    try { return (RMSort)super.clone(); }
    catch(CloneNotSupportedException e) { return null; }
}

/**
 * XML archival.
 */
public XMLElement toXML(XMLArchiver anArchiver)
{
    XMLElement e = new XMLElement("sort");
    e.add("key", _key);
    if(_order==RMSort.ORDER_DESCEND) e.add("order", "descend");
    return e;
}

/**
 * XML unarchival.
 */
public Object fromXML(XMLArchiver anArchiver, XMLElement anElement)
{
    _key = anElement.getAttributeValue("key");
    if(anElement.getAttributeValue("order", "ascend").equals("descend")) _order = RMSort.ORDER_DESCEND;
    return this;
}

/**
 * Returns a string representation of sort (just the sort key).
 */
public String toString()  { return _key; }

}