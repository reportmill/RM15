/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import com.reportmill.base.*;
import java.util.*;

/**
 * A class to represent a graph series, contains a list of series items.
 */
public class RMGraphSeries {

    // The title for this series
    String         _title;
    
    // The key for this series
    String         _key;
    
    // The group for this series (if swapped)
    RMGroup        _group;

    // A list of items
    List <Item>    _items = new ArrayList();
    
/**
 * Creates a new series.
 */
public RMGraphSeries(String aTitle, String aKey, RMGroup aGroupList)
{
    // Set title, key, group
    _title = aTitle; _key = aKey; _group = aGroupList;
    
    // Iterate over inner series and add value for each root object
    for(int j=0, jMax=aGroupList.size(); j<jMax; j++)
        addItem((RMGroup)aGroupList.get(j));
}

/**
 * Returns the title for this series.
 */
public String getTitle()  { return _title; }

/**
 * Returns the key for this series.
 */
public String getKey()  { return _key; }

/**
 * Add item for given key/group.
 */
public void addItem(RMGroup aGroup)
{
    Item value = new Item(_key, aGroup); // Create new series value
    _items.add(value); // Add value
}

/**
 * Returns the number of items in this series.
 */
public int getItemCount()  { return _items.size(); }

/**
 * Returns the individual series item at the given index.
 */
public Item getItem(int anIndex)  { return _items.get(anIndex); }

/**
 * Returns the total for all values in series.
 */
public float getSeriesTotal()
{
    float total = 0; for(int i=0, iMax=getItemCount(); i<iMax; i++) total += getItem(i).getFloatValue();
    return total;
}

/**
 * Returns the graph series for a given graph.
 */
public static List <RMGraphSeries> getSeries(RMGraphRPG aGraphRPG)
{
    // Create new list for series
    List <RMGraphSeries> series = new ArrayList();
    
    // If each object in objects is a group, make series for each group
    RMGraph graph = aGraphRPG.getGraph();
    RMGroup graphObjects = aGraphRPG.getObjects();
    
    // Iterate over keys and create real graph series
    for(int i=0, iMax=graph.getKeyCount(); i<iMax; i++) {
        String title = graph.getSeries(i).getTitle(); // Get series title
        RMGraphSeries subseries = new RMGraphSeries(title, graph.getKey(i), graphObjects); // Create graph series
        series.add(subseries); // Add series to series list
    }
    
    // Return series
    return series;
}

/**
 * A class to represent an individual item inside a graph series, including its key, group, value and bar/wedge shape.
 */
public static class Item {

    // The key for this value
    String       _key;
    
    // The group for this value
    RMGroup      _group;
    
    // The numerical value for this series value
    Number       _value;
    
    // The bar created for this series value
    RMShape      _bar;
    
    /** Creates a new series value. */
    public Item(String aKey, RMGroup aGroup)  { _key = aKey; _group = aGroup; }
    
    /** Returns the key. */
    public String getKey()  { return _key; }
    
    /** Returns the group. */
    public RMGroup getGroup()  { return _group; }
    
    /** Returns the value for this series value. */
    public Number getValue()  { return _value!=null? _value : (_value=getValueImpl()); }

    /** Returns the value for this series value. */
    private Number getValueImpl()
    {
        Number value = RMKeyChain.getNumberValue(_group, _key);
        return value!=null? value : 0;
    }
    
    /** Returns the float value for this series value. */
    public float getFloatValue()  { return getValue().floatValue(); }
    
    /** Returns the bar created for this series value. */
    public RMShape getBar()  { return _bar; }
    
    /** Sets the bar created for this series value. */
    public void setBar(RMShape aBar)  { _bar = aBar; }
}

}