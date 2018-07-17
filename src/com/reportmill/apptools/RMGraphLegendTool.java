/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.apptools;
import com.reportmill.shape.*;
import snap.view.ViewEvent;

/**
 * Provides UI editing for RMGraphLegend.
 */
public class RMGraphLegendTool <T extends RMGraphLegend> extends RMParentShapeTool <T> {

/**
 * Override to configure UI.
 */
protected void initUI()  { enableEvents("LegendText", DragDrop); }

/**
 * Reset UI.
 */
public void resetUI()
{
    // Get selected legend
    RMGraphLegend leg = getSelectedShape();
    
    // Update LegendText
    setViewText("LegendText", leg.getLegendText());
}

/**
 * Respond UI.
 */
protected void respondUI(ViewEvent anEvent)
{
    // Get selected legend
    RMGraphLegend leg = getSelectedShape();
    
    // Handle LegendText (Action and DragDrop)
    if(anEvent.equals("LegendText"))
        leg.setLegendText(anEvent.getStringValue());
}

/**
 * Returns the name of the graph inspector.
 */
public String getWindowTitle()  { return "Graph Legend Inspector"; }

/**
 * Override to make RMGraphLegend not super-selectable. 
 */
public boolean isSuperSelectable(RMShape aShape)  { return false; }

}