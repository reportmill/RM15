/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.apptools;
import com.reportmill.graphics.*;
import com.reportmill.shape.*;
import snap.view.*;
import snap.viewx.FilePanel;

/**
 * UI editing for RMImageFill.
 */
public class RMImageFillTool extends RMFillTool {

/**
 * Updates the UI controls from the currently selected shape.
 */
public void resetUI()
{
    // Get currently selected shape (just return if null) and image fill (if none, use default instance)
    RMShape shape = getEditor().getSelectedOrSuperSelectedShape(); if(shape==null) return;
    RMImageFill fill = shape.getFill() instanceof RMImageFill? (RMImageFill)shape.getFill() : _imageFill;
    
    // Update TiledCheckBox
    setViewValue("TiledCheckBox", fill.isTiled());
    
    // Update XSpinner, YSpinner, ScaleXSpinner and ScaleYSpinner
    setViewValue("XSpinner", fill.getX());
    setViewValue("YSpinner", fill.getY());
    setViewValue("ScaleXSpinner", fill.getScaleX());
    setViewValue("ScaleYSpinner", fill.getScaleY());
}

/**
 * Updates the currently selected shape from the UI controls.
 */
public void respondUI(ViewEvent anEvent)
{
    // Get currently selected shape (just return if null) and image fill (if none, use default instance)
    RMShape shape = getEditor().getSelectedOrSuperSelectedShape(); if(shape==null) return;
    RMImageFill fill = shape.getFill() instanceof RMImageFill? (RMImageFill)shape.getFill() : _imageFill;
    
    // Handle TiledCheckBox
    if(anEvent.equals("TiledCheckBox"))
        fill = fill.copyTiled(anEvent.getBoolValue());
    
    // Handle XSpinner, YSpinner, ScaleXSpinner, ScaleYSpinner
    if(anEvent.equals("XSpinner"))
        fill = fill.copyFor(anEvent.getFloatValue(), fill.getY(), fill.getWidth(), fill.getHeight(), fill.isAbsolute());
    if(anEvent.equals("YSpinner"))
        fill = fill.copyFor(fill.getX(), anEvent.getFloatValue(), fill.getWidth(), fill.getHeight(), fill.isAbsolute());
    if(anEvent.equals("ScaleXSpinner"))
        fill = fill.copyForScale(anEvent.getFloatValue(), fill.getScaleY());
    if(anEvent.equals("ScaleYSpinner"))
        fill = fill.copyForScale(fill.getScaleX(), anEvent.getFloatValue());
    
    // Handle ChooseButton
    if(anEvent.equals("ChooseButton")) {
        String path = FilePanel.showOpenPanel(getUI(), "Image File", "png", "jpg", "gif");
        if(path!=null)
            fill = new RMImageFill(path, true);
    }

    // Set new fill
    setSelectedFill(fill);
}

/**
 * Returns the string used for the inspector window title.
 */
public String getWindowTitle()  { return "Fill Inspector (Texture)"; }

}