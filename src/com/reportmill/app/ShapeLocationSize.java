/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import com.reportmill.shape.*;
import java.util.List;
import snap.util.MathUtils;
import snap.view.*;

/**
 * This class provides UI editing for the currently selected shapes location and size.
 */
public class ShapeLocationSize extends RMEditorPane.SupportPane {
    
/**
 * Creates a new ShapeLocationSize pane.
 */
public ShapeLocationSize(RMEditorPane anEP)  { super(anEP); }

/**
 * Updates UI controls from currently selected shape.
 */
public void resetUI()
{
    // Get currently selected shape
    RMShape shape = getEditor().getSelectedOrSuperSelectedShape();
    
    // Update XThumb & XText
    setViewValue("XThumb", getUnitsFromPoints(shape.getFrameX()));
    setViewValue("XText", getUnitsFromPoints(shape.getFrameX()));
    
    // Update YThumb & YText
    setViewValue("YThumb", getUnitsFromPoints(shape.getFrameY()));
    setViewValue("YText", getUnitsFromPoints(shape.getFrameY()));
    
    // Update WThumb & WText
    setViewValue("WThumb", getUnitsFromPoints(shape.width()));
    setViewValue("WText", getUnitsFromPoints(shape.width()));
    
    // Update HThumb & HText
    setViewValue("HThumb", getUnitsFromPoints(shape.height()));
    setViewValue("HText", getUnitsFromPoints(shape.height()));
    
    // Update MinWText and MinHText
    setViewValue("MinWText", shape.isMinWidthSet()? shape.getMinWidth() : "-");
    setViewValue("MinHText", shape.isMinHeightSet()? shape.getMinHeight() : "-");
    
    // Update PrefWText and PrefHText
    setViewValue("PrefWText", shape.isPrefWidthSet()? shape.getPrefWidth() : "-");
    setViewValue("PrefHText", shape.isPrefHeightSet()? shape.getPrefHeight() : "-");
    
    // Disable if document or page
    getUI().setEnabled(!(shape instanceof RMDocument || shape instanceof RMPage));
}

/**
 * Updates currently selected shape from UI controls.
 */
public void respondUI(ViewEvent anEvent)
{
    // Get currently selected editor, document and shapes
    RMEditor editor = getEditor();
    List <? extends RMShape> shapes = editor.getSelectedOrSuperSelectedShapes();
    
    // Handle X ThumbWheel and Text
    if(anEvent.equals("XThumb") || anEvent.equals("XText")) {
        editor.undoerSetUndoTitle("Location Change");
        double value = anEvent.getFloatValue(); value = getPointsFromUnits(value);
        for(RMShape shape : shapes) shape.setFrameX(value);
    }
    
    // Handle Y ThumbWheel and Text
    if(anEvent.equals("YThumb") || anEvent.equals("YText")) {
        editor.undoerSetUndoTitle("Location Change");
        double value = anEvent.getFloatValue(); value = getPointsFromUnits(value);
        for(RMShape shape : shapes) shape.setFrameY(value);
    }
    
    // Handle Width ThumbWheel and Text
    if(anEvent.equals("WThumb") || anEvent.equals("WText")) {
        editor.undoerSetUndoTitle("Size Change");
        double value = anEvent.getFloatValue(); value = getPointsFromUnits(value);
        if(Math.abs(value)<.1) value = MathUtils.sign(value)*.1f;
        for(RMShape shape : shapes) shape.setWidth(value);
    }
    
    // Handle Height ThumbWheel and Text
    if(anEvent.equals("HThumb") || anEvent.equals("HText")) {
        editor.undoerSetUndoTitle("Size Change");
        double value = anEvent.getFloatValue(); value = getPointsFromUnits(value);
        if(Math.abs(value)<.1) value = MathUtils.sign(value)*.1f;
        for(RMShape shape : shapes) shape.setHeight(value);
    }
    
    // Handle MinWText & MinHText
    if(anEvent.equals("MinWText"))
        for(RMShape shape : shapes) shape.setMinWidth(anEvent.getFloatValue());
    if(anEvent.equals("MinHText"))
        for(RMShape shape : shapes) shape.setMinHeight(anEvent.getFloatValue());
    
    // Handle MinWSyncButton & MinHSyncButton
    if(anEvent.equals("MinWSyncButton"))
        for(RMShape shape : shapes) shape.setMinWidth(shape.getWidth());
    if(anEvent.equals("MinHSyncButton"))
        for(RMShape shape : shapes) shape.setMinHeight(shape.getHeight());

    // Handle PrefWText & PrefHText
    if(anEvent.equals("PrefWText"))
        for(RMShape shape : shapes) shape.setPrefWidth(anEvent.getFloatValue());
    if(anEvent.equals("PrefHText"))
        for(RMShape shape : shapes) shape.setPrefHeight(anEvent.getFloatValue());
    
    // Handle PrefWSyncButton & PrefHSyncButton
    if(anEvent.equals("PrefWSyncButton"))
        for(RMShape shape : shapes) shape.setPrefWidth(shape.getWidth());
    if(anEvent.equals("PrefHSyncButton"))
        for(RMShape shape : shapes) shape.setPrefHeight(shape.getHeight());
}

/**
 * Converts from shape units to tool units.
 */
public double getUnitsFromPoints(double aValue)
{
    RMEditor editor = getEditor(); RMDocument doc = editor.getDoc();
    return doc!=null? doc.getUnitsFromPoints(aValue) : aValue;
}

/**
 * Converts from tool units to shape units.
 */
public double getPointsFromUnits(double aValue)
{
    RMEditor editor = getEditor(); RMDocument doc = editor.getDoc();
    return doc!=null? doc.getPointsFromUnits(aValue) : aValue;
}

/** Returns the name to be used in the inspector's window title. */
public String getWindowTitle()  { return "Location/Size Inspector"; }

}