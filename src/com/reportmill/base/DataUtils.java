/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.base;
import java.util.*;
import snap.util.SnapUtils;
import java.text.*;
import java.util.Date;

/**
 * Utility methods for RMProperty.
 */
public class DataUtils {

    // Decimal sysmbols for current local
    static String _groupingSeparator = "" + new DecimalFormat().getDecimalFormatSymbols().getGroupingSeparator();

    // DateFormat for toString(date)
    static SimpleDateFormat _dfmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * Converts a given value to a given type.
     */
    public static Object convertValue(Object aValue, Property.Type aType)
    {
        return convertValue(aValue, aType, Property.NumberType.Double);
    }

    /**
     * Converts a given value to a given type.
     */
    public static Object convertValue(Object aValue, Property.Type aType, Property.NumberType aNumberType)
    {
        // Handle String conversions
        if (aType == Property.Type.String)
            return SnapUtils.stringValue(aValue);

        // Handle Number conversions
        if (aType == Property.Type.Number) {

            // Remove grouping (thousands) separator
            //if(aValue instanceof String) aValue = RMStringUtils.delete((String)aValue, _groupingSeparator);

            // Do normal conversion
            switch (aNumberType) {
                case Byte:
                case Short:
                case Integer:
                    return SnapUtils.getInteger(aValue);
                case Long:
                    return SnapUtils.longValue(aValue);
                case Float:
                    return SnapUtils.getFloat(aValue);
                case Double:
                    return SnapUtils.getDouble(aValue);
                case Decimal:
                    return SnapUtils.getBigDecimal(aValue);
                default:
                    return SnapUtils.numberValue(aValue);
            }
        }

        // Handle Boolean conversions
        if (aType == Property.Type.Boolean)
            return SnapUtils.booleanValue(aValue);

        // Handle Date conversions
        if (aType == Property.Type.Date)
            return SnapUtils.getDate(aValue);

        // Handle Binary conversion
        if (aType == Property.Type.Binary)
            return SnapUtils.getBytes(aValue);

        // Just return value
        return aValue;
    }

    /**
     * Returns a string for a number according to given NumberType.
     */
    public static String toString(Number aNumber)
    {
        return SnapUtils.stringValue(aNumber);
    }

    /**
     * Returns a string for a date according to given DateType.
     */
    public static String toString(Date aDate)
    {
        return _dfmt.format(aDate);
    }

    /**
     * Returns a filtered list (copy with given key chain string.
     */
    public static List getFilteredList(List aList, String aKeyChain)
    {
        // If key is null or zero length, just return copy
        if (aKeyChain == null || aKeyChain.length() == 0) return new ArrayList(aList);

        // Get key chain and new list
        RMKeyChain keyChain = RMKeyChain.getKeyChain(aKeyChain);
        List list = new ArrayList(aList.size());

        // Iterate over list objects and add to new list if they satisfy key chain
        for (Object object : aList)
            if (RMKeyChain.getBoolValue(object, keyChain))
                list.add(object);

        // Return list
        return list;
    }

    /**
     * Returns a property type for a given object/class.
     */
    public static Property.Type getPropertyType(Object anObj)
    {
        // If null, return null
        if (anObj == null) return null;

        // If Property, return type (kind of hokey, but useful for building condition expressions)
        if (anObj instanceof Property) return ((Property) anObj).getType();

        // Get class
        Class objClass = anObj instanceof Class ? (Class) anObj : anObj.getClass();

        // Handle String
        if (String.class.isAssignableFrom(objClass)) return Property.Type.String;

        // Handle Number
        if (Number.class.isAssignableFrom(objClass)) return Property.Type.Number;
        if (int.class.isAssignableFrom(objClass)) return Property.Type.Number;
        if (byte.class.isAssignableFrom(objClass)) return Property.Type.Number;
        if (short.class.isAssignableFrom(objClass)) return Property.Type.Number;
        if (long.class.isAssignableFrom(objClass)) return Property.Type.Number;
        if (float.class.isAssignableFrom(objClass)) return Property.Type.Number;
        if (double.class.isAssignableFrom(objClass)) return Property.Type.Number;

        // Handle Boolean
        if (Boolean.class.isAssignableFrom(objClass)) return Property.Type.Boolean;
        if (boolean.class.isAssignableFrom(objClass)) return Property.Type.Boolean;

        // Handle Date
        if (Date.class.isAssignableFrom(objClass)) return Property.Type.Date;

        // Return TYPE_OTHER
        return Property.Type.Other;
    }

}