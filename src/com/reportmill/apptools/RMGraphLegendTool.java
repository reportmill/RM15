/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.apptools;
import com.reportmill.shape.*;

/**
 * Provides UI editing for RMGraphLegend.
 */
public class RMGraphLegendTool <T extends RMGraphLegend> extends RMParentShapeTool <T> {

/**
 * Override to suppress superclass.
 */
protected void initUI()  { enableEvents("LegendText", DragDrop); enableEvents("ColumnCountText", DragDrop); }

/**
 * Override to suppress superclass.
 */
public void resetUI()  { }

/**
 * Returns the name of the graph inspector.
 */
public String getWindowTitle()  { return "Graph Legend Inspector"; }

/**
 * Override to make RMGraphLegend not super-selectable. 
 */
public boolean isSuperSelectable(RMShape aShape)  { return false; }

}