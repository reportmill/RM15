/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.apptools;
import com.reportmill.shape.*;
import snap.util.ClassUtils;
import snap.view.ViewEvent;

/**
 * Provides UI inspection for GraphPartBars.
 */
public class RMGraphPartBarsTool extends RMTool {

/**
 * Resets UI panel controls.
 */
public void resetUI()
{
    // Get the selected value axis
    RMGraphPartBars bars = getSelectedShape(); if(bars==null) return;
    
    // Update BarGapSpinner, SetGapSpinner, BarCountSpinner
    setViewValue("BarGapSpinner", bars.getBarGap());
    setViewValue("SetGapSpinner", bars.getSetGap());  
    setViewValue("BarCountSpinner", bars.getBarCount());
}

/**
 * Responds to UI panel controls.
 */
public void respondUI(ViewEvent anEvent)
{
    // Get the selected value axis
    RMGraphPartBars bars = getSelectedShape(); if(bars==null) return;
    
    // Handle BarGapSpinner, SetGapSpinner, BarCountSpinner
    if(anEvent.equals("BarGapSpinner")) bars.setBarGap(anEvent.getFloatValue());
    if(anEvent.equals("SetGapSpinner")) bars.setSetGap(anEvent.getFloatValue());
    if(anEvent.equals("BarCountSpinner")) bars.setBarCount(anEvent.getIntValue());
}

/**
 * Returns the currently selected RMGraphPartBars.
 */
public RMGraphPartBars getSelectedShape()
{
    RMGraph graph = getSelectedGraph();
    return graph!=null? graph.getBars() : null;
}

/**
 * Returns the currently selected graph area shape.
 */
public RMGraph getSelectedGraph()
{
    return ClassUtils.getInstance(super.getSelectedShape(), RMGraph.class);
}

/**
 * Returns the name of the graph inspector.
 */
public String getWindowTitle()  { return "Graph Bars Inspector"; }

}