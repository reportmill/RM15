/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import com.reportmill.shape.*;
import snap.util.*;
import snap.view.*;
import snap.viewx.DialogBox;
import snap.web.WebURL;

/**
 * This class is a container for a viewer and tool bars. The default tool bars add document controls (save,
 * print, copy), input controls (select, pan, text select, image select), zoom controls and page controls. 
 * 
 * You might use it like this to view in a Swing component hierarchy:
 * <p><blockquote><pre>
 *   RMViewerPane viewer = new RMViewerPane(); viewer.getViewer().setContent(new RMDocument(aSource));
 *   JComponent vcomp = viewer.getRootView().getNative(JComponent.class);
 *   myFrame.setContentPane(viewer);
 * </pre></blockquote>
 */
public class RMViewerPane extends ViewOwner {

    // The real viewer
    RMViewer          _viewer;
    
    // The ScrollView for this viewer
    ScrollView        _scrollView;
    
    // The BorderView for the ScrollView
    BorderView        _scrollBorderView;
    
    // The controls at the top of the document
    ViewOwner         _topToolBar;
    
    // The controls at the bottom of the document
    ViewOwner         _btmToolBar;
    
    // Listener for Viewer changes
    PropChangeListener  _viewLsnr = pc -> viewerDidPropChange(pc);
    
/**
 * Returns the viewer for this viewer pane.
 */
public RMViewer getViewer()  { if(_viewer==null) getUI(); return _viewer; }

/**
 * Sets the viewer for this viewer pane.
 */
protected void setViewer(RMViewer aViewer)
{
    // Stop listening to PropChanges on old
    if(_viewer!=null) _viewer.removePropChangeListener(_viewLsnr);
    
    // Set Viewer
    _viewer = aViewer;
    getScrollView().setContent(_viewer);
    
    // Start listening to PropChanges
    _viewer.addPropChangeListener(_viewLsnr);
}

/**
 * Creates the real viewer for this viewer plus.
 */
protected RMViewer createViewer()  { return new RMViewer(); }

/**
 * Returns the scroll view for this viewer plus.
 */
public ScrollView getScrollView()  { return _scrollView; }

/**
 * Returns the scroll view for this viewer plus.
 */
public BorderView getScrollBorderView()  { return _scrollBorderView; }

/**
 * Returns the viewer shape.
 */
public RMViewerShape getViewerShape()  { return getViewer().getViewerShape(); }

/**
 * Returns the content shape.
 */
public RMParentShape getContent()  { return getViewer().getContent(); }

/**
 * Returns the RMDocument associated with this viewer.
 */
public RMDocument getDocument()  { return getViewer().getDocument(); }

/**
 * Returns the document source.
 */
protected WebURL getSourceURL()  { return getViewer().getSourceURL(); }

/**
 * Returns the top controls.
 */
public ViewOwner getTopToolBar()  { return _topToolBar!=null? _topToolBar : (_topToolBar=createTopToolBar()); }

/**
 * Creates the top tool bar.
 */
public ViewOwner createTopToolBar()  { return new RMViewerTopToolBar(this); }

/**
 * Returns the bottom controls.
 */
public ViewOwner getBottomToolBar()  { return _btmToolBar!=null? _btmToolBar : (_btmToolBar=createBottomToolBar()); }

/**
 * Creates bottom tool bar.
 */
public ViewOwner createBottomToolBar()  { return new RMViewerBottomToolBar(this); }

/**
 * Saves the current viewer document.
 */
public void save()  { }

/**
 * Prints the current viewer document.
 */
public void print()  { getViewer().print(); }

/**
 * Copies the current viewer document selection.
 */
public void copy()  { getViewer().getEvents().copy(); }

/**
 * Runs a dialog panel to request a percentage zoom (which is then set with setZoomFactor).
 */
public void runZoomPanel()
{
    // Run input dialog to get zoom factor string
    DialogBox dbox = new DialogBox("Zoom Panel"); dbox.setQuestionMessage("Enter Percentage to Zoom to:");
    String string = dbox.showInputDialog(getUI(), "120");
    
    // If string is valid, set zoom factor to float value
    if(string!=null) {
        float factor = StringUtils.floatValue(string)/100;
        if(factor>0)
            getViewer().setZoomFactor(factor);
    }
    
    // Request focus
    requestFocus(getViewer());
}

/**
 * Previews the current viewer document as pdf.
 */
public void previewPDF()
{
    getDocument().writePDF(SnapUtils.getTempDir() + "RMPDFFile.pdf");
    FileUtils.openFile(SnapUtils.getTempDir() + "RMPDFFile.pdf");
}

/**
 * Initializes the UI.
 */
protected View createUI()
{
    // Create and configure viewer
    _viewer = createViewer();
    _viewer.addPropChangeListener(_viewLsnr); // Listen to PropertyChanges
    _scrollView = new ScrollView(); _scrollView.setFill(new snap.gfx.Color("#c0c0c0"));

    _scrollView.setContent(_viewer);
    
    // Create ScrollBorderView
    _scrollBorderView = new BorderView(); _scrollBorderView.setCenter(_scrollView);

    // Create BorderView and add TopToolBar, ScrollView/Viewer and BottomToolBar
    BorderView bpane = new BorderView();
    bpane.setTop(getTopToolBar().getUI());
    bpane.setCenter(_scrollBorderView);
    bpane.setBottom(getBottomToolBar().getUI());
    return bpane;
}

/**
 * Resets UI.
 */
protected void resetUI()
{
    if(!ViewUtils.isMouseDown()) getTopToolBar().resetLater();
    if(!ViewUtils.isMouseDown()) getBottomToolBar().resetLater();
}

/**
 * ResetUI on Viewer PropertyChange.
 */
protected void viewerDidPropChange(PropChange aPC)
{
    String pname = aPC.getPropertyName();
    if(pname==View.Cursor_Prop || pname==View.Width_Prop || pname==View.Height_Prop) return;
    resetLater();
}

}