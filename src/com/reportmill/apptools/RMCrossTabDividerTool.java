/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.apptools;
import com.reportmill.shape.RMCrossTabDivider;
import snap.view.*;

/**
 * Provides ReportMill UI editing for CellDivider shape.
 */
public class RMCrossTabDividerTool <T extends RMCrossTabDivider> extends RMTool <T> {

/**
 * Override to return empty panel.
 */
protected View createUI()  { return new Label("CrossTab Divider"); }

/**
 * Overrides tool method to indicate that cell dividers have no handles.
 */
public int getHandleCount(T aShape)  { return 0; }

}
