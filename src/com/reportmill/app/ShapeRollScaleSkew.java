/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import com.reportmill.shape.*;
import java.util.List;
import snap.view.*;

/**
 * This class provides UI editing for advanced transforms such as rotation, scale and skew for the
 * currently selected shapes.
 */ 
public class ShapeRollScaleSkew extends RMEditorPane.SupportPane {

/**
 * Creates a new ShapeRollScaleSkew pane.
 */
public ShapeRollScaleSkew(RMEditorPane anEP)  { super(anEP); }

/**
 * Resets the UI controls from the currently selected shapes.
 */
public void resetUI()
{
    // Get currently selected shape
    RMShape shape = getEditor().getSelectedOrSuperSelectedShape();
    
    // Update RotationThumb and RotationText
    setViewValue("RotationThumb", shape.getRoll());
    setViewValue("RotationText", shape.getRoll());
    
    // Update ScaleXThumb and ScaleXText
    setViewValue("ScaleXThumb", shape.getScaleX());
    setViewValue("ScaleXText", shape.getScaleX());
    
    // Update ScaleYThumb and ScaleYText
    setViewValue("ScaleYThumb", shape.getScaleY());
    setViewValue("ScaleYText", shape.getScaleY());
    
    // Update SkewXThumb and SkewXText
    setViewValue("SkewXThumb", shape.getSkewX());
    setViewValue("SkewXText", shape.getSkewX());
    
    // Update SkewYThumb and SkewYText
    setViewValue("SkewYThumb", shape.getSkewY());
    setViewValue("SkewYText", shape.getSkewY());
    
    // Disable if document or page
    getUI().setEnabled(!(shape instanceof RMDocument || shape instanceof RMPage));
}

/**
 * Responds to changes from the UI panel's controls.
 */
public void respondUI(ViewEvent anEvent)
{
    // Get currently selected shape and shapes
    RMEditor editor = getEditor();
    RMShape shape = editor.getSelectedOrSuperSelectedShape();
    List <RMShape> shapes = editor.getSelectedOrSuperSelectedShapes();
    
    // Handle Rotation Thumb & Text
    if(anEvent.equals("RotationThumb") || anEvent.equals("RotationText")) {
        shape.undoerSetUndoTitle("Rotation Change");
        float value = anEvent.getFloatValue();
        for(RMShape s : shapes)
            s.setRoll(value);
    }

    // Handle ScaleX/ScaleY Thumb & Text
    if(anEvent.equals("ScaleXThumb") || anEvent.equals("ScaleXText") ||
        anEvent.equals("ScaleYThumb") || anEvent.equals("ScaleYText")) {
        shape.undoerSetUndoTitle("Scale Change");
        float value = anEvent.getFloatValue();
        boolean symmetrical = getViewBoolValue("ScaleSymetricCheckBox");
        
        // Handle ScaleX (and symmetrical)
        if(anEvent.equals("ScaleXThumb") || anEvent.equals("ScaleXText") || symmetrical)
            for(RMShape s : shapes)
                s.setScaleX(value);

        // Handle ScaleY (and symmetrical)
        if(anEvent.equals("ScaleYThumb") || anEvent.equals("ScaleYText") || symmetrical)
            for(RMShape s : shapes)
                s.setScaleY(value);
    }

    // Handle SkewX Thumb & Text
    if(anEvent.equals("SkewXThumb") || anEvent.equals("SkewXText")) {
        shape.undoerSetUndoTitle("Skew Change");
        float value = anEvent.getFloatValue();
        for(RMShape s : shapes)
            s.setSkewX(value);
    }

    // Handle SkewY Thumb & Text
    if(anEvent.equals("SkewYThumb") || anEvent.equals("SkewYText")) {
        shape.undoerSetUndoTitle("Skew Change");
        float value = anEvent.getFloatValue();
        for(RMShape s : shapes)
            s.setSkewY(value);
    }
}

/**
 * Returns the name to be used in the inspector panel window title.
 */
public String getWindowTitle()  { return "Roll/Scale/Skew Inspector"; }

}