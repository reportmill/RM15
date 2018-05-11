/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import com.reportmill.base.RMGroup;
import com.reportmill.graphics.*;
import java.util.*;
import snap.util.*;

/**
 * An inner class for Legend.
 */
public class RMGraphLegend extends RMParentShape {

    // The legend text
    String          _legendText;
    
    // The number of columns in legend
    int             _colCount = 1;
    
    // The font
    RMFont          _font;
    
/**
 * Returns the graph that owns this legend.
 */
public RMGraph getGraph()  { return getParent()!=null? getParent().getChildWithClass(RMGraph.class) : null; }

/**
 * Returns the legend text.
 */
public String getLegendText()  { return _legendText; }

/**
 * Sets the legend text.
 */
public void setLegendText(String aString)  { _legendText = aString; relayout(); }

/**
 * Returns the number of columns in legend.
 */
public int getColumnCount()  { return _colCount; }

/**
 * Sets the number of columns in legend.
 */
public void setColumnCount(int aValue)
{
    firePropChange("ColumnCount", _colCount, _colCount = aValue);
    relayout(); repaint();
}

/**
 * Returns whether font has been set.
 */
public boolean isFontSet()  { return _font!=null; }

/**
 * Return current font.
 */
public RMFont getFont()  { return _font!=null? _font : RMFont.Helvetica10; }

/**
 * Set current font.
 */
public void setFont(RMFont aFont)  { _font = aFont; }

/**
 * Override to layout legend.
 */
protected void layoutImpl()
{
    // Remove children
    removeChildren(); if(getParent()==null) return;
    
    // Get Graph/GraphRPG
    RMGraph graph = getParent().getChildWithClass(RMGraph.class);
    RMGraphRPG graphRPG = getGraphRPG(); if(graphRPG==null || graphRPG.getSeriesCount()==0) return;
    boolean isPreview = graph!=null; if(graph==null) graph = graphRPG._graph;
    boolean isPie = graph.getType()==RMGraph.Type.Pie;
    
    // Get strings and groups
    List <String> strings = new ArrayList();
    List <RMGroup> groups = new ArrayList();
    
    // If pie, add for each item
    if(isPie) { RMGraphSeries series = graphRPG.getSeries(0); String ltext = StringUtils.min(getLegendText());
        for(int i=0,iMax=series.getItemCount();i<iMax;i++) { RMGraphSeries.Item item = series.getItem(i);
            strings.add(ltext!=null? ltext : ("Item " + (i+1))); groups.add(item._group); }
    }

    // If more than one series, add item for each series
    else for(int i=0, iMax=graphRPG.getSeriesCount(); i<iMax; i++) { RMGraphSeries series = graphRPG.getSeries(i);
        strings.add(series.getTitle()); groups.add(series._group); }
    
    // Iterate over strings and add legend items
    double x = 2, y = 2;
    for(int i=0,iMax=strings.size();i<iMax; i++) {
        String text = strings.get(i); RMGroup group = groups.get(i);
        RMRectShape box = new RMRectShape(); box.setColor(graph.getColor(i)); box.setBounds(x,y,16,12); x += 18;
        RMTextShape label = new RMTextShape(); label.setText(text); label.setFont(getFont());
        if(!isPreview) label.getXString().rpgClone(graphRPG._rptOwner, group, null, false);
        //double pw = label.getPrefWidth(), ph = label.getPrefHeight(); label.setBounds(x,y,pw,ph);
        label.setBounds(x,y,getWidth()-x-2,8); label.setBestHeight(); double ph = label.getHeight();
        addChild(box); addChild(label); x = 2; y += ph+2;
    }
    
    // Resize
    //Rect bounds = getBoundsOfChildren();
    //if(bounds.getMaxX()>getWidth()) setWidth(bounds.getMaxX());
    //if(bounds.getMaxY()>getHeight()) setHeight(bounds.getMaxX());
}

/**
 * Returns RMGraphRPG, if parent has RMGraphRPG.GraphShape child.
 */
private RMGraphRPG getGraphRPG()
{
    RMGraphRPG.GraphShape gshp = getParent()!=null? getParent().getChildWithClass(RMGraphRPG.GraphShape.class) : null;
    return gshp!=null? gshp.getGraphRPG() : null;
}

/**
 * XML archival.
 */
public XMLElement toXMLShape(XMLArchiver anArchiver)
{
    // Archive basic shape attributes
    XMLElement e = super.toXMLShape(anArchiver); e.setName("graph-legend");
    
    // Archive LegendText, ColumnCount
    if(getLegendText()!=null && getLegendText().length()>0) e.add("text", getLegendText());
    if(getColumnCount()>1) e.add("ColumnCount", getColumnCount());
    return e;
}

/**
 * XML unarchival.
 */
public void fromXMLShape(XMLArchiver anArchiver, XMLElement anElement)
{
    // Unarchive basic shape attributes
    super.fromXMLShape(anArchiver, anElement);
    
    // Unarchive LegendText, ColumnCount
    setLegendText(anElement.getAttributeValue("text"));
    if(anElement.hasAttribute("ColumnCount")) setColumnCount(anElement.getAttributeIntValue("ColumnCount"));
}

/** Override to suppress child archival. */
protected void toXMLChildren(XMLArchiver anArchiver, XMLElement anElement)  { }

}