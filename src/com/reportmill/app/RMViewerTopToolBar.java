/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import snap.view.*;

/**
 * UI controls for RMViewerPlus top.
 */
public class RMViewerTopToolBar extends ViewOwner {

    // The viewer associated with this tool bar
    RMViewerPane    _viewerPane;
    
/**
 * Creates a new top ui.
 */
public RMViewerTopToolBar(RMViewerPane aViewerPane)  { _viewerPane = aViewerPane; }

/**
 * Returns the viewer pane.
 */
public RMViewerPane getViewerPane()  { return _viewerPane; }

/**
 * Returns the viewer.
 */
public RMViewer getViewer()  { return getViewerPane().getViewer(); }

/**
 * Resets to UI.
 */
public void resetUI()  { }

/**
 * Responds to UI.
 */
public void respondUI(ViewEvent anEvent)
{
    // Handle SaveButton
    if(anEvent.equals("SaveButton"))
        getViewerPane().save();
    
    // Handle PrintButton
    if(anEvent.equals("PrintButton"))
        getViewerPane().print();
    
    // Handle CopyButton
    if(anEvent.equals("CopyButton"))
        getViewerPane().copy();
    
    // Handle File PreviewPDFButton
    if(anEvent.equals("PreviewPDFButton"))
        getViewerPane().previewPDF();
        
    // Handle MoveButton
    if(anEvent.equals("MoveButton"))
        getViewer().getEvents().setMode(RMViewerEvents.DEFAULT);
    
    // Handle TextButton
    if(anEvent.equals("TextButton"))
        getViewer().getEvents().setMode(RMViewerEvents.SELECT_TEXT);

    // Handle SelectButton
    if(anEvent.equals("SelectButton"))
        getViewer().getEvents().setMode(RMViewerEvents.SELECT_IMAGE);
}

}