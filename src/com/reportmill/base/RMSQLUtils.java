/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.base;
import java.sql.*;
import java.util.*;
import snap.util.MapUtils;

/**
 * Utility methods for ResultSet.
 */
public class RMSQLUtils {

/**
 * Returns a list of maps for a given ResultSet.
 * @param aResultSet the result set to be converted to a list of maps.
 * @param aLimit the fetch limit for the list of maps. Use -1, 0 or Integer.MAX_VALUE for unlimited.
 */
public static List <Map> getMaps(ResultSet aResultSet, int aLimit)
{
    // Adjust limit
    if(aLimit<=0)
        aLimit = Integer.MAX_VALUE;
    
    // Create result set list
    List <Map> list = new ArrayList();
    
    // If result set is null, just return list
    if(aResultSet==null)
        return list;
    
    // Catch exceptions
    try {
        
        // Get result set meta data
        ResultSetMetaData metaData = aResultSet.getMetaData();
        
        // Get column count
        int columnCount = metaData.getColumnCount();
        
        // Iterate over result set and load each record into map
        while(aResultSet.next() && list.size()<aLimit) {
            
            // Create new map for record
            Map map = new HashMap();
    
            // Iterate over columns
            for(int i=1; i<=columnCount; i++) {
                
                // Get column key
                Object key = null;
                try { 
                    key = metaData.getColumnLabel(i); 
                    if (key==null)
                        key = metaData.getColumnName(i);
                  }
                catch(Exception e) { }
                
                // Get column value
                Object val = null;
                val = aResultSet.getObject(i);
                
                // If value is blob, get bytes
                if(val instanceof Blob)
                    val = ((Blob)val).getBytes(1, (int)((Blob)val).length());
    
                // If key and value are non-null, add to map
                if(key!=null && val!=null)
                    map.put(key, val);
            }
            
            // Add record map to list
            list.add(map);
        }
        
        // Close the result set
        aResultSet.close();
    
    // Catch exceptions
    } catch (Exception e) { e.printStackTrace(); }
    
    // Return result set list
    return list;
}

/**
 * Returns map where any ResultSets have been converted to Lists. Copies the original Map if ResultSets exist.
 * @param aMap the map to be stripped of result sets.
 * @param aDepth the depth to traverse any nested maps or lists (suggested value: 2).
 */
public static Map getMapsDeep(Map aMap, int aDepth)
{
    // If depth is reached, just return map
    if(aDepth==0)
        return aMap;
    
    // Declare local variable for map
    Map map = aMap;
    
    // Iterate over map entries to see if ResultSets are present
    for(Map.Entry entry : (Set<Map.Entry>)aMap.entrySet()) { Object value = entry.getValue();
        
        // If value is ResultSet, convert to List
        if(value instanceof ResultSet)
            value = getMaps((ResultSet)value, 0);
        
        // If value is Map, convert to Map without ResultSet
        else if(value instanceof Map)
            value = getMapsDeep((Map)value, aDepth-1);
            
        // If value changed, put new value in map (clone it first)
        if(value!=entry.getValue()) {
            
            // If map is still original, clone it
            if(map==aMap)
                map = MapUtils.clone(aMap);
            
            // Put replace old value with new value
            map.put(entry.getKey(), value);
        }
    }
    
    // Return map (potentially new)
    return map;
}

}