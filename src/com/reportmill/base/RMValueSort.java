/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.base;
import java.util.*;

/**
 * This is a sort subclass to support sorting objects by their relative position in an array of values.
 */
public class RMValueSort extends RMSort {

    // The list of values
    List       _values;
    
/**
 * Creates a new value sort.
 */
public RMValueSort(String aKey, List aValuesList)
{
    _key = aKey;
    _values = aValuesList;
}

/**
 * Compare objects with given RMSort.
 */
public int compare(Object obj1, Object obj2)
{
    // Get values for objects and sort key
    Object val1 = RMKeyChain.getValue(obj1, getKey());
    Object val2 = RMKeyChain.getValue(obj2, getKey());

    // Get indexes of values in values list
    int index1 = _values.indexOf(val1);
    int index2 = _values.indexOf(val2);

    // If val1 and val2 have same index, return same
    if(index1==index2)
        return RMSort.ORDER_SAME;
    
    // If val1 not found, return ordered same if val2 not found, otherwise descending
    if(index1<0)
        return RMSort.ORDER_DESCEND;

    // If val2 not found, return ordered ascending
    if(index2<0)
        return RMSort.ORDER_ASCEND;

    // If val1 appears before val2, return ascending
    if(index1<index2)
        return RMSort.ORDER_ASCEND;

    // Return descending, since it's the only remaining option
    return RMSort.ORDER_DESCEND;
}

}
