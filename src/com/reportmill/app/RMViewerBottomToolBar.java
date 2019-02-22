/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import snap.gfx.*;
import snap.util.SnapUtils;
import snap.view.*;

/**
 * UI controls for RMViewerPane bottom.
 */
public class RMViewerBottomToolBar extends ViewOwner {

    // The viewer associated with this tool bar
    RMViewerPane    _viewerPane;

/**
 * Creates a new bottom ui.
 */
public RMViewerBottomToolBar(RMViewerPane aViewerPane)  { _viewerPane = aViewerPane; }

/**
 * Returns the viewer pane.
 */
public RMViewerPane getViewerPane()  { return _viewerPane; }

/**
 * Override to swap top level RowView for subclass to manually centers PageNavBox under viewer.
 */
protected View createUI()
{
    RowView rowView = (RowView)super.createUI();
    RowView rowView2 = new RMVRowView(rowView);
    return rowView2;
}

/**
 * Initialize UI panel.
 */
protected void initUI()
{
    // Set right arrow in PageForwardButton
    Polygon p1 = new Polygon(4, 5, 10, 11, 4, 17);
    getView("PageForwardButton", Button.class).setImage(getImage(p1));
    
    // Set left arrow in PageBackButton
    Polygon p2 = new Polygon(10, 5, 4, 11, 10, 17);
    getView("PageBackButton", Button.class).setImage(getImage(p2));
    
    // Set left arrow plus stop bar in PageBackAllButton
    Path p3 = new Path(); p3.append(p2.getPathIter(new Transform(2, 0)));
    p3.append(new Rect(2, 6, 2, 10));
    getView("PageBackAllButton", Button.class).setImage(getImage(p3));
    
    // Set right arrow plus stop bar in PageForwardAllButton
    Path p4 = new Path(); p4.append(p1.getPathIter(new Transform(-2, 0)));
    p4.append(new Rect(10, 6, 2, 10));
    getView("PageForwardAllButton", Button.class).setImage(getImage(p4));
}

/**
 * Resets UI.
 */
protected void resetUI()
{
    // Get viewer pane
    RMViewerPane viewerPane = getViewerPane();
    RMViewer viewer = viewerPane.getViewer();
    
    // Reset ZoomText
    setViewValue("ZoomText", Math.round(viewer.getZoomFactor()*100) + "%");
    
    // Reset PageText field
    String pageText = "" + (viewer.getSelPageIndex()+1) + " of " + viewer.getPageCount();
    setViewValue("PageText", pageText);
    
    // Reset PageNavBox.Visible and enabled states for page back/forward buttons 
    setViewEnabled("PageBackButton", viewer.getSelPageIndex()>0);
    setViewEnabled("PageBackAllButton", viewer.getSelPageIndex()>0);
    setViewEnabled("PageForwardButton", viewer.getSelPageIndex()<viewer.getPageCount()-1);
    setViewEnabled("PageForwardAllButton", viewer.getSelPageIndex()<viewer.getPageCount()-1);
    
    // Update PageNavBox.Visible
    getView("PageNavBox").setVisible(viewer.getDoc().getPageCount()>1);
}

/**
 * Responds to UI changes.
 */
protected void respondUI(ViewEvent anEvent)
{
    // Get viewer pane and viewer
    RMViewerPane viewerPane = getViewerPane();
    RMViewer viewer = viewerPane.getViewer();
    
    // Handle ZoomComboBox
    if(anEvent.equals("ZoomText"))
        viewer.setZoomFactor(anEvent.getFloatValue()/100);
    
    // Handle ZoomMenuButton
    if(anEvent.equals("ZoomMenuItem"))
        viewer.setZoomFactor(SnapUtils.floatValue(anEvent.getText())/100);
    
    // Handle ZoomToActualSizeMenuItem - use screen resolution to figure out zooming for actual size
    if(anEvent.equals("ZoomToActualSizeMenuItem"))
        viewer.setZoomFactor(viewer.getZoomToActualSizeFactor());
    
    // Handle ZoomToFitMenuItem
    if(anEvent.equals("ZoomToFitMenuItem"))
        viewer.setZoomMode(RMViewer.ZoomMode.ZoomToFit);
    
    // Handle ZoomAsNeededMenuItem
    if(anEvent.equals("ZoomAsNeededMenuItem"))
        viewer.setZoomMode(RMViewer.ZoomMode.ZoomAsNeeded);
    
    // Handle PageText
    if(anEvent.equals("PageText"))
        viewer.setSelPageIndex(anEvent.getIntValue()-1);
    
    // Handle PageBackButton
    if(anEvent.equals("PageBackButton"))
        viewer.pageBack();
    
    // Handle PageBackAllButton
    if(anEvent.equals("PageBackAllButton"))
        viewer.setSelPageIndex(0);
    
    // Handle PageForwardButton
    if(anEvent.equals("PageForwardButton"))
        viewer.pageForward();
    
    // Handle PageForwardAllButton
    if(anEvent.equals("PageForwardAllButton"))
        viewer.setSelPageIndex(viewer.getPageCount()-1);
    
    // Have viewer pane reset
    viewerPane.resetLater();
}

/**
 * Returns an image for given shape.
 */
private static Image getImage(Shape aShape)
{
    Image img = Image.get(14,22,true); Painter pntr = img.getPainter(); pntr.setColor(Color.DARKGRAY);
    pntr.fill(aShape); pntr.flush(); return img;
}

/**
 * A custom RowView subclass to center PageNavBox under viewer.
 */
public class RMVRowView extends RowView {
    
    /** Copy settings from given RowView. */
    public RMVRowView(RowView aRV) {
         setPrefHeight(aRV.getPrefHeight()); setPadding(aRV.getPadding()); setChildren(aRV.getChildren());
    }
    
    /** Override to center PageNavBox under Viewer. */
    protected void layoutImpl()
    {
        View pageNavBox = getView("PageNavBox");
        if(pageNavBox.isVisible()) {
            RMViewer viewer = getViewerPane().getViewer();
            View spacerLabel = getView("SpacerLabel"); if(spacerLabel.getX()==0) super.layoutImpl();
            double w0 = viewer.getParent().getWidth(), w1 = pageNavBox.getPrefWidth();
            double x0 = spacerLabel.getX(), x1 = Math.round((w0 - w1)/2); spacerLabel.setPrefWidth(x1 - x0);
        }
        super.layoutImpl();
    }
}

}