/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import com.reportmill.base.RMGroup;
import com.reportmill.graphics.*;
import java.util.*;
import snap.gfx.Rect;
import snap.util.*;

/**
 * An inner class for Legend.
 */
public class RMGraphLegend extends RMParentShape {

    // The legend text
    String          _legendText;
    
    // The font
    RMFont          _font;
    
/**
 * Returns the legend text.
 */
public String getLegendText()  { return _legendText; }

/**
 * Sets the legend text.
 */
public void setLegendText(String aString)  { _legendText = aString; relayout(); }

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
 * Override to configure sample legend if no children.
 */
public void setParent(RMParentShape aPar)
{
    // Do normal version and return if already has children
    super.setParent(aPar);
    if(getChildCount()>0) return;
    
    // Get graph rpg
    RMGraphRPG graphRPG = getGraphRPG(aPar);
    configureRPG(graphRPG, false);
}

/**
 * Override to .
 */
public RMShape rpgAll(ReportOwner anRptOwner, RMShape aParent)
{
    // Do normal clone (rpgShape())
    RMGraphLegend clone = (RMGraphLegend)clone();
    
    // Get Graph RPG and configure
    RMGraphRPG graphRPG = getGraphRPG((RMParentShape)aParent);
    clone.configureRPG(graphRPG, true);
    
    // Do bindings and return
    rpgBindings(anRptOwner, clone);
    return clone;
}

/**
 * Override to layout legend.
 */
protected void configureRPG(RMGraphRPG graphRPG, boolean doRPG)
{
    // Make sure there is a graphRPG
    if(graphRPG==null) { System.err.println("RMGraphLegend.confiureRPG: Graph RPG not found"); return; }
    
    // Get Graph
    RMGraph graph = getGraph(); if(graph==null) graph = graphRPG._graph;
    
    // Get legend text whether to do per item
    String legendText = getLegendText(); if(legendText==null) legendText = "";
    boolean doTextRPG = doRPG && legendText.contains("@");
    boolean doPerItem = graph.getType()==RMGraph.Type.Pie || graph.getKeyCount()<2 && legendText.contains("@");
    
    // Get strings and groups
    List <String> strings = new ArrayList();
    List <RMGroup> groups = new ArrayList();
    
    // If doPerItem, add for each item
    if(doPerItem) { RMGraphSeries series = graphRPG.getSeries(0); String ltext = StringUtils.min(getLegendText());
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
        if(doTextRPG) label.getXString().rpgClone(graphRPG._rptOwner, group, null, false);
        label.setBounds(x,y,getWidth()-x-2,8); label.setBestHeight();
        addChild(box); addChild(label); x = 2; y += label.getHeight() + 2;
    }
    
    // Resize
    Rect bounds = getBoundsOfChildren();
    if(bounds.getMaxX()+2>getWidth()) setWidth(bounds.getMaxX()+2);
    if(bounds.getMaxY()+2>getHeight()) setHeight(bounds.getMaxY()+2);
}

/**
 * Returns the graph that owns this legend.
 */
private RMGraph getGraph()  { return getParent()!=null? getParent().getChildWithClass(RMGraph.class) : null; }

/**
 * Returns RMGraphRPG, if parent has RMGraphRPG.GraphShape child.
 */
private RMGraphRPG getGraphRPG(RMParentShape aParent)
{
    RMGraphRPG.GraphShape gshp = aParent!=null? aParent.getChildWithClass(RMGraphRPG.GraphShape.class) : null;
    return gshp!=null? gshp.getGraphRPG() : null;
}

/**
 * XML archival.
 */
public XMLElement toXMLShape(XMLArchiver anArchiver)
{
    // Archive basic shape attributes
    XMLElement e = super.toXMLShape(anArchiver); e.setName("graph-legend");
    
    // Archive LegendText
    if(getLegendText()!=null && getLegendText().length()>0) e.add("text", getLegendText());
    return e;
}

/**
 * XML unarchival.
 */
public void fromXMLShape(XMLArchiver anArchiver, XMLElement anElement)
{
    // Unarchive basic shape attributes
    super.fromXMLShape(anArchiver, anElement);
    
    // Unarchive LegendText
    setLegendText(anElement.getAttributeValue("text"));
}

/** Override to suppress child archival. */
protected void toXMLChildren(XMLArchiver anArchiver, XMLElement anElement)  { }

}