/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.apptools;
import com.reportmill.app.RMEditor;
import com.reportmill.graphics.*;
import com.reportmill.shape.RMShape;
import java.util.List;
import snap.view.ViewEvent;
import snap.viewx.ColorWell;

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
    
    // Update StrokeColorWell, StrokeWidthText, StrokeWidthThumb
    setViewValue("StrokeColorWell", stroke.getColor());
    setViewValue("StrokeWidthText", stroke.getWidth());
    setViewValue("StrokeWidthThumb", stroke.getWidth());
    
    // Update TopCheckBox, RightCheckBox, BottomCheckBox, LeftCheckBox
    setViewValue("TopCheckBox", bstroke.isShowTop());
    setViewValue("RightCheckBox", bstroke.isShowRight());
    setViewValue("BottomCheckBox", bstroke.isShowBottom());
    setViewValue("LeftCheckBox", bstroke.isShowLeft());
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
    
    // Handle StrokeColorWell - get color and set in selected shapes
    if(anEvent.equals("StrokeColorWell")) {
        ColorWell cwell = getView("StrokeColorWell", ColorWell.class);
        RMColor color = RMColor.get(cwell.getColor());
        for(RMShape s : shapes)
            s.setStrokeColor(color);
    }
    
    // Handle StrokeWidthText, StrokeWidthThumb
    if(anEvent.equals("StrokeWidthText") || anEvent.equals("StrokeWidthThumb")) {
        float width = anEvent.getFloatValue();
        for(RMShape s : shapes)
            s.setStrokeWidth(width);
    }
    
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
