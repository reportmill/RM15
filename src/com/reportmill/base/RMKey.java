/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.base;
import snap.util.Key;

/**
 * This class provides an optimized convenience for getting named values from arbitrary objects.
 */
public class RMKey extends snap.util.Key {

/**
 * Returns a value for given object and key.
 */
public static Object getValue(Object anObj, String aKey)
{
    if(anObj instanceof Get) return ((Get)anObj).getKeyValue(aKey);
    return getValueImpl(anObj, aKey);
}

/**
 * Returns a value for given object and key.
 */
public static Object getValueImpl(Object anObj, String aKey)
{
    Object obj = ReportMill.convertFromAppServerType(anObj); if(obj==null) return null;
    Object value = Key.getValueImpl(obj, aKey);
    return ReportMill.convertFromAppServerType(value);
}

/**
 * Returns whether given object has an accessor for given key.
 */
public static boolean hasKey(Object anObj, String aKey)
{
    Object obj = ReportMill.convertFromAppServerType(anObj);
    return Key.hasKey(obj, aKey);
}

}