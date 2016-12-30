/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.apptools;
import com.reportmill.shape.*;
import snap.gfx.Image;
import snap.view.ViewEvent;

/**
 * This class is responsible for UI editing of nested document shape.
 */
public class RMNestedDocTool extends RMTool {
    
/**
 * Refreshes UI panel controls form the currently selected nested doc.
 */
public void resetUI()
{    
    RMNestedDoc edoc = (RMNestedDoc)getSelectedShape(); if(edoc==null) return;
    setViewValue("ScaleFactorText", edoc.getScaleFactor());
    setViewValue("InitialDelayText", edoc.getDelay());
    setViewValue("GapDelayText", edoc.getGapDelay());
}

/**
 * Handles changes from UI panel controls to currently selected nested doc.
 */
public void respondUI(ViewEvent anEvent)
{
    RMNestedDoc edoc = (RMNestedDoc)getSelectedShape(); if(edoc==null) return;
    if(anEvent.equals("ScaleFactorText")) edoc.setScaleFactor(anEvent.getFloatValue());
    if(anEvent.equals("InitialDelayText")) edoc.setDelay(anEvent.getFloatValue());
    if(anEvent.equals("GapDelayText")) edoc.setGapDelay(anEvent.getFloatValue());
}

/**
 * Returns the class that this tool is responsible for.
 */
public Class getShapeClass()  { return RMNestedDoc.class; }

/**
 * Returns the name to be presented to the user.
 */
public String getWindowTitle()  { return "Embedded Document Inspector";}

/**
 * Returns the image used to represent our shape class.
 */
public Image getImage()  { return getImage("RMPage.png"); }

}