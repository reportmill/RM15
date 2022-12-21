/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.base;
import snap.util.Convert;
import snap.util.SnapUtils;
import java.text.*;
import java.util.Date;

/**
 * Utility methods for RMProperty.
 */
public class DataUtils {

    // DateFormat for toString(date)
    static SimpleDateFormat  _dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

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
            return Convert.stringValue(aValue);

        // Handle Number conversions
        if (aType == Property.Type.Number) {

            // Remove grouping (thousands) separator
            //if(aValue instanceof String) aValue = RMStringUtils.delete((String)aValue, _groupingSeparator);

            // Do normal conversion
            switch (aNumberType) {
                case Byte:
                case Short:
                case Integer: return Convert.getInteger(aValue);
                case Long: return Convert.longValue(aValue);
                case Float: return Convert.getFloat(aValue);
                case Double: return Convert.getDouble(aValue);
                case Decimal: return Convert.getBigDecimal(aValue);
                default: return Convert.numberValue(aValue);
            }
        }

        // Handle Boolean conversions
        if (aType == Property.Type.Boolean)
            return Convert.booleanValue(aValue);

        // Handle Date conversions
        if (aType == Property.Type.Date)
            return Convert.getDate(aValue);

        // Handle Binary conversion
        if (aType == Property.Type.Binary)
            return SnapUtils.getBytes(aValue);

        // Just return value
        return aValue;
    }

    /**
     * Returns a string for a date according to given DateType.
     */
    public static String toString(Date aDate)
    {
        return _dateFormat.format(aDate);
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
        Class<?> objClass = anObj instanceof Class ? (Class<?>) anObj : anObj.getClass();

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