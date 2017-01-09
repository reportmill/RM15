/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.apptools;
import com.reportmill.shape.*;
import java.util.List;
import snap.gfx.*;
import snap.util.SnapUtils;
import snap.view.*;

/**
 * This class handles creation of lines.
 */
public class RMLineShapeTool <T extends RMLineShape> extends RMTool <T> {
    
    // Indicates whether line should try to be strictly horizontal or vertical
    boolean               _hysteresis = false;
    
    // The list of arrow head shapes
    List <RMLineShape>    _arrowShapes;

    // Constants for line segment points
    public static final byte HandleStartPoint = 0;
    public static final byte HandleEndPoint = 1;

/**
 * Returns the shape class that this tool is responsible for.
 */
public Class getShapeClass()  { return RMLineShape.class; }

/**
 * Returns the name of this tool to be displayed by inspector.
 */
public String getWindowTitle()  { return "Line Inspector"; }

/**
 * Event handling - overridden to install cross-hair cursor.
 */
public void mouseMoved(ViewEvent anEvent)  { getEditor().setCursor(Cursor.CROSSHAIR); }

/**
 * Handles mouse press for line creation.
 */
public void mousePressed(ViewEvent anEvent)
{
    super.mousePressed(anEvent);
    _hysteresis = true;
}

/**
 * Handles mouse drag for line creation.
 */
public void mouseDragged(ViewEvent anEvent)
{
    Point currentPoint = getEditorEvents().getEventPointInShape(true);
    double dx = currentPoint.getX() - _downPoint.getX();
    double dy = currentPoint.getY() - _downPoint.getY();
    double breakingPoint = 20f;
    
    if(_hysteresis) {
        if(Math.abs(dx) > Math.abs(dy)) {
            if(Math.abs(dy) < breakingPoint) dy = 0;
            else _hysteresis = false;
        }
        
        else if(Math.abs(dx) < breakingPoint) dx = 0;
        else _hysteresis = false;
    }
    
    // Register shape for repaint
    _shape.repaint();
    
    // Set adjusted bounds
    _shape.setBounds(_downPoint.getX(), _downPoint.getY(), dx, dy);
}

/**
 * Editor method (returns the number of handles).
 */
public int getHandleCount(T aShape)  { return 2; }

/**
 * Editor method.
 */
public Point getHandlePoint(T aShape, int anIndex, boolean isSuperSel)
{
    return super.getHandlePoint(aShape, anIndex==HandleEndPoint? HandleSE : anIndex, isSuperSel);
}

/**
 * Editor method.
 */
public void moveShapeHandle(T aShape, int aHandle, Point aPoint)
{
    super.moveShapeHandle(aShape, aHandle==HandleEndPoint? HandleSE : aHandle, aPoint);
}

/**
 * Loads the list of arrow shapes from a .rpt file.
 */
public List <RMLineShape> getArrows()
{
    if(_arrowShapes!=null) return _arrowShapes;
    RMDocument doc = new RMDocument(getClass().getResource("RMLineShapeToolArrowHeads.rpt"));
    return _arrowShapes = doc.getChildrenWithClass(getShapeClass());
}

/**
 * Initialize UI panel.
 */
protected void initUI()
{
    // Get arrows menu button
    MenuButton menuButton = getView("ArrowsMenuButton", MenuButton.class);
        
    // Add arrows menu button
    for(int i=0; i<getArrows().size(); i++) { RMLineShape line = getArrows().get(i);
        Image image = RMShapeUtils.createImage(line.getArrowHead(), null);
        MenuItem mitem = new MenuItem(); mitem.setImage(image);
        mitem.setName("ArrowsMenuButtonMenuItem" + i);
        menuButton.addItem(mitem);
    }
    
    // Add "None" menu item
    MenuItem mitem = new MenuItem(); mitem.setText("None"); mitem.setName("ArrowsMenuButtonMenuItem 999");
    menuButton.addItem(mitem);
}

/**
 * Update UI panel.
 */
public void resetUI()
{
    // Get selected line and arrow head
    RMLineShape line = getSelectedShape(); if(line==null) return;
    RMLineShape.ArrowHead ahead = line.getArrowHead();
    
    // Update ArrowsMenuButton
    Image image = ahead!=null? RMShapeUtils.createImage(line.getArrowHead(), null) : null;
    getView("ArrowsMenuButton", MenuButton.class).setImage(image);

    // Update ScaleText and ScaleThumbWheel
    setViewValue("ScaleText", ahead!=null? ahead.getScaleX() : 0);
    setViewValue("ScaleThumbWheel", ahead!=null? ahead.getScaleX() : 0);
    setViewEnabled("ScaleText", ahead!=null);
    setViewEnabled("ScaleThumbWheel", ahead!=null);
}

/**
 * Respond to UI change.
 */
public void respondUI(ViewEvent anEvent)
{
    // Get selected shape and arrow head
    RMLineShape line = getSelectedShape();
    RMLineShape.ArrowHead arrowHead = line.getArrowHead();

    // Handle ScaleText and ScaleThumbWheel
    if(anEvent.equals("ScaleText") || anEvent.equals("ScaleThumbWheel"))
        arrowHead.setScaleXY(anEvent.getFloatValue(), anEvent.getFloatValue());

    // Handle ArrowsMenuButtonMenuItem
    if(anEvent.getName().startsWith("ArrowsMenuButtonMenuItem")) {
        int index = SnapUtils.intValue(anEvent.getName());
        RMLineShape.ArrowHead ahead = index<getArrows().size()? getArrows().get(index).getArrowHead() : null;
        line.setArrowHead(ahead!=null? (RMLineShape.ArrowHead)ahead.clone() : null);
    }
}

}