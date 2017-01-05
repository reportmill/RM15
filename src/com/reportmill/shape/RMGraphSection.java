/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import com.reportmill.base.RMGroup;
import java.util.*;
import snap.gfx.Rect;
import snap.util.ListUtils;

/**
 * Represents the data in the sections of a merged or separated graph. 
 */
public class RMGraphSection {

    // The RMGraphRPG this section is in
    RMGraphRPG       _graphRPG;

    // The graph this section works for
    RMGraph          _graph;
    
    // The index of this section
    int              _index;

    // The list of section items
    List <Item>      _items = new ArrayList();

/**
 * Creates a new graph section.
 */
public RMGraphSection(RMGraphRPG aGraphRPG, int anIndex)
{
    _graphRPG = aGraphRPG; _graph = _graphRPG.getGraph(); _index = anIndex;
}

/**
 * Returns the number of section items
 */
public int getItemCount()  { return _items.size(); }

/**
 * Returns the specific item at the given index.
 */
public RMGraphSection.Item getItem(int anIndex)  { return _items.get(anIndex); }

/**
 * Adds an item for the given series and series item.
 */
public void addItem(int aSeriesIndex, int anItemIndex)
{
    _items.add(new RMGraphSection.Item(aSeriesIndex, anItemIndex));
}

/**
 * Returns section minimum value.
 */
public float getMinValue()
{
    // Iterate over series values
    float min = 0;
    for(int i=0, iMax=getItemCount(); i<iMax; i++) {
        
        // Get series value and float value
        RMGraphSeries.Item item = getItem(i).getSeriesItem();
        float value = item.getFloatValue();
        
        // If stacked or first item, just add value, otherwise get min of this value and previous value(s)
        if(_graph.isStacked() || i==0) min = min + value;
        else min = Math.min(value, min);
    }
    
    // Return min value
    return min;
}

/**
 * Returns section maximum value.
 */
public float getMaxValue()
{
    // Iterate over series values
    float max = 0;
    for(int i=0, iMax=getItemCount(); i<iMax; i++) {
        
        // Get series value and float value
        RMGraphSeries.Item item = getItem(i).getSeriesItem();
        float value = item.getFloatValue();
        
        // If stacked or first item, just add value, otherwise get max of this value and previous value(s)
        if(_graph.isStacked() || i==0) max = max + value;
        else max = Math.max(value, max);
    }
    
    // Return max value
    return max;
}

/**
 * Returns the total for all values in series.
 */
public float getSectionTotal()
{
    float total = 0;
    for(int i=0, iMax=getItemCount(); i<iMax; i++) total += getItem(i).getSeriesItem().getFloatValue();
    return total;
}

/**
 * Returns the bounds of the section.
 */
public Rect getBounds()
{
    // Declare section x, y, width, height and initialize width/height to graph area width/height
    double x = 0, y = 0, w = _graph.getWidth(), h = _graph.getHeight();
    
    // If vertical, divide width by section count and shift x by section-index*section width
    // If horizontal, divide height by section count and shift y by section-index*section height
    if(_graph.isVertical()) { w /= _graphRPG.getSectionCount(); x += _index*w; }
    else { h /= _graphRPG.getSectionCount(); y += _index*h; }
    return new Rect(x, y, w, h); // Return bounds
}

/**
 * Returns the list of sections for the graph.
 */
public static List getSections(RMGraphRPG aGraphRPG)
{
    // Create new list for sections (just return if no series)
    List sections = new ArrayList(); if(aGraphRPG.getSeriesCount()==0) return sections;
    
    // Handle Meshed
    if(aGraphRPG.isMeshed()) {
        
        // Get first series
        RMGraphSeries series = aGraphRPG.getSeries(0);
        
        // Iterate over series items: Create new section, iterate over series and add series+item, add section
        for(int i=0, iMax=series.getItemCount(); i<iMax; i++) {
            RMGraphSection section = new RMGraphSection(aGraphRPG, i);
            for(int j=0, jMax=aGraphRPG.getSeriesCount(); j<jMax; j++) section.addItem(j, i);
            sections.add(section);
        }
    }
    
    // Handle Separated
    else {
        
        // Iterate over series: Create new section, iterate over items and add series+item, add section
        for(int i=0, iMax=aGraphRPG.getSeriesCount(); i<iMax; i++) { RMGraphSeries series = aGraphRPG.getSeries(i);
            RMGraphSection section = new RMGraphSection(aGraphRPG, i);
            for(int j=0, jMax=series.getItemCount(); j<jMax; j++) section.addItem(i, j);
            sections.add(section);
        }
    }
    
    // Return sections
    return sections;
}

/**
 * An inner class for a section item.
 */
public class Item {

    // The section item series index and item index
    int   _seriesIndex, _itemIndex;
    
    /** Creates a new section item. */
    public Item(int aSeriesIndex, int anItemIndex)  { _seriesIndex = aSeriesIndex; _itemIndex = anItemIndex; }
    
    /** Returns the series index. */
    public int getSeriesIndex()  { return _seriesIndex; }
    
    /** Returns the item index. */
    public int getItemIndex()  { return _itemIndex; }
    
    /** Returns the section item series. */
    public RMGraphSeries getSeries()  { return _graphRPG.getSeries(_seriesIndex); }
    
    /** Returns the section item series item. */
    public RMGraphSeries.Item getSeriesItem()  { return getSeries().getItem(_itemIndex); }
    
    /** Returns the item float value */
    public float getFloatValue()  { return getSeriesItem().getFloatValue(); }
    
    /** Returns the item group. */
    public RMGroup getGroup()  { return getSeriesItem().getGroup(); }
    
    /** Returns the item bar. */
    public RMShape getBar()  { return getSeriesItem().getBar(); }
    
    /** Sets the bar created for this series value. */
    public void setBar(RMShape aBar)  { getSeriesItem().setBar(aBar); }
    
    /** Returns the bounds for this section item. */
    public Rect getBounds()
    {
        // Get bounds of section, graph bars, effective bar count and index of this section item
        Rect bounds = RMGraphSection.this.getBounds();
        RMGraphPartBars bars = _graph.getBars();
        float barCount = getItemCount() + (getItemCount()-1)*bars.getBarGap() + bars.getSetGap();
        int index = ListUtils.indexOfId(_items, this); // Get index of this section item
        
        // If vertical, divide width by item count and shift x by section-item-index*section-item-width
        if(_graph.isVertical()) {
            bounds.width /= barCount;
            bounds.x += bars.getSetGap()/2*bounds.width + index*(1+bars.getBarGap())*bounds.width;
        }
        
        // If horizontal, divide height by item count and shift y by section-item-index*section-item-height
        else {
            bounds.height /= barCount;
            bounds.y += bars.getSetGap()/2*bounds.height + index*(1+bars.getBarGap())*bounds.height;
        }
        
        // Return bounds
        return bounds;
    }
}

}