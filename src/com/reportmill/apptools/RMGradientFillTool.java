/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.apptools;
import com.reportmill.graphics.*;
import com.reportmill.shape.*;
import snap.gfx.GradientPaint;
import snap.util.ClassUtils;
import snap.view.ViewEvent;

/**
 * UI editing for RMGradientFill.
 */
public class RMGradientFillTool extends RMFillTool {
    
/**
 * Initialize UI.
 */
protected void initUI()
{
    setViewItems("TypeComboBox", new String[] { "Linear", "Radial" });
}

/**
 * Updates the UI controls from the currently selected shape.
 */
protected void resetUI()
{
    // Get currently selected shape and shape gradient fill (just return if null)
    RMShape shape = getEditor().getSelectedOrSuperSelectedShape(); if(shape==null) return;
    RMGradientFill fill = getDefaultFill(shape);
    boolean isRadial = fill.isRadial();

    // Update ColorStopPicker
    GradientStopPicker picker = getView("ColorStopPicker", GradientStopPicker.class);
    picker.setStops(fill.getStops());
    
    // Update TypeComboBox, RadialPicker and LinearControls
    setViewSelIndex("TypeComboBox", isRadial? 1 : 0);
    getView("RadialPicker").setVisible(isRadial);
    getView("LinearControls").setVisible(!isRadial);
        
    // Update angle controls for a linear gradient
    if(!isRadial) {
        setViewValue("AngleThumb", fill.getRoll());
        setViewValue("AngleText", fill.getRoll());
    }
    
    // or the axis picker for a radial gradient
    else {
        GradientAxisPicker radialControl = getView("RadialPicker", GradientAxisPicker.class);
        radialControl.setStartPoint(fill.getStartX(), fill.getStartY());
        radialControl.setEndPoint(fill.getEndX(), fill.getEndY());
        radialControl.setStops(fill.getStops());
    }
}

/**
 * Updates the currently selected shape from the UI controls.
 */
protected void respondUI(ViewEvent anEvent)
{
    // Get currently selected shape and its fill (just return if null)
    RMShape shape = getEditor().getSelectedOrSuperSelectedShape(); if(shape==null) return;
    RMGradientFill oldfill = getDefaultFill(shape), newFill = null;
    
    // Handle ColorStopPicker
    if(anEvent.equals("ColorStopPicker")) {
        GradientStopPicker picker = anEvent.getView(GradientStopPicker.class);
        newFill = oldfill.copyForStops(picker.getStops());
    }
    
    // Handle ReverseStopsButton
    else if(anEvent.equals("ReverseStopsButton"))
        newFill = oldfill.copyForReverseStops();
    
    // Handle AngleThumb and AngleText
    else if(anEvent.equals("AngleThumb") || anEvent.equals("AngleText")) {
        double angle = anEvent.equals("AngleThumb")? anEvent.getIntValue() : anEvent.getFloatValue();
        newFill = oldfill.copyForRoll(angle);
    }
    
    // Handle linear/radial popup
    else if(anEvent.equals("TypeComboBox")) {
        GradientPaint.Type t = anEvent.getSelIndex()==1? GradientPaint.Type.RADIAL : GradientPaint.Type.LINEAR;
        newFill = oldfill.copyForType(t);
    }

    // Handle radial axis control
    else if(anEvent.equals("RadialPicker")) {
        GradientAxisPicker p = (GradientAxisPicker)anEvent.getView();
        newFill = oldfill.copyForPoints(p.getStartPoint(), p.getEndPoint());
    }
    
    // Reset fill of all selected shapes
    if(newFill!=null)
        setSelectedFill(newFill);
}

/**
 * Returns the gradient for the shape.  Creates one if the shape doesn't have a gradient fill.
 */
public RMGradientFill getDefaultFill(RMShape shape)
{
    // Get shape gradient fill, if present
    RMGradientFill fill = ClassUtils.getInstance(shape.getFill(), RMGradientFill.class);
    
    // If missing, create one - second color defaults to black, unless that would result in a black-black gradient
    if(fill==null) {
        RMColor c = shape.getColor();
        RMColor c2 = c.equals(RMColor.black)? RMColor.white : RMColor.black;
        fill = new RMGradientFill(c, c2, 0);
    }
    
    // Return fill
    return fill;
}

/**
 * Returns the string used for the inspector window title.
 */
public String getWindowTitle()  { return "Fill Inspector (Gradient)"; }

}