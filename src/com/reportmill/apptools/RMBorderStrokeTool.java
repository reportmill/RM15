/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.apptools;
import com.reportmill.app.RMEditor;
import com.reportmill.graphics.RMBorderStroke;
import com.reportmill.graphics.RMStroke;
import com.reportmill.shape.RMShape;
import java.util.List;
import snap.view.ViewEvent;

public class RMBorderStrokeTool extends RMStrokeTool {

/**
 * Reset UI controls.
 */
public void resetUI()
{
    // Get currently selected shape
    RMShape shape = getEditor().getSelectedOrSuperSelectedShape();
    RMStroke stroke = shape.getStroke(); if(stroke==null) stroke = new RMStroke();
    RMBorderStroke bstroke = stroke instanceof RMBorderStroke? (RMBorderStroke)stroke : new RMBorderStroke();
    
    // Update TopCheckBox, RightCheckBox, BottomCheckBox, LeftCheckBox
    setViewValue("TopCheckBox", bstroke.getShowTop());
    setViewValue("RightCheckBox", bstroke.getShowRight());
    setViewValue("BottomCheckBox", bstroke.getShowBottom());
    setViewValue("LeftCheckBox", bstroke.getShowLeft());
}

/**
 * Respond to UI changes
 */
public void respondUI(ViewEvent anEvent)
{
    // Get editor selected shapes and selected shape
    RMEditor editor = getEditor();
    List <RMShape> shapes = editor.getSelectedOrSuperSelectedShapes();
    RMShape shape = editor.getSelectedOrSuperSelectedShape();
    
    // Handle TopCheckBox, RightCheckBox, BottomCheckBox, LeftCheckBox
    if(anEvent.equals("TopCheckBox")) {
        for(RMShape shp : shapes) { RMStroke str = shp.getStroke();
            RMBorderStroke bstr = str instanceof RMBorderStroke? (RMBorderStroke)str : new RMBorderStroke();
            shp.setStroke(bstr.deriveTop(anEvent.getBoolValue())); }}
    if(anEvent.equals("RightCheckBox")) {
        for(RMShape shp : shapes) { RMStroke str = shp.getStroke();
            RMBorderStroke bstr = str instanceof RMBorderStroke? (RMBorderStroke)str : new RMBorderStroke();
            shp.setStroke(bstr.deriveRight(anEvent.getBoolValue())); }}
    if(anEvent.equals("BottomCheckBox")) {
        for(RMShape shp : shapes) { RMStroke str = shp.getStroke();
            RMBorderStroke bstr = str instanceof RMBorderStroke? (RMBorderStroke)str : new RMBorderStroke();
            shp.setStroke(bstr.deriveBottom(anEvent.getBoolValue())); }}
    if(anEvent.equals("LeftCheckBox")) {
        for(RMShape shp : shapes) { RMStroke str = shp.getStroke();
            RMBorderStroke bstr = str instanceof RMBorderStroke? (RMBorderStroke)str : new RMBorderStroke();
            shp.setStroke(bstr.deriveLeft(anEvent.getBoolValue())); }}
}
    
}
