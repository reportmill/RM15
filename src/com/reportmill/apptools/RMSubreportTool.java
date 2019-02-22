/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.apptools;
import com.reportmill.app.RMEditorPane;
import com.reportmill.shape.*;
import snap.util.ClassUtils;
import snap.view.ViewEvent;

/**
 * A tool implementation for RMSubreport.
 */
public class RMSubreportTool <T extends RMSubreport> extends RMTool <T> {

/** Override to declare window title. */
public String getWindowTitle()  { return "Subreport"; }

/** Override to declare shape class. */
public Class getShapeClass()  { return RMSubreport.class; }

/**
 * Reset UI.
 */
protected void resetUI()
{
    RMSubreport shape = getSelectedShape(); if(shape==null) return;

    // Update SubNameText
    setViewValue("SubNameText", shape.getSubreportName());
}

/**
 * Event handling from select tool for super selected shapes.
 */
protected void respondUI(ViewEvent anEvent)
{
    RMSubreport shape = getSelectedShape(); if(shape==null) return;

    // Handle SubNameText
    if(anEvent.equals("SubNameText")) shape.setSubreportName(anEvent.getStringValue());
    
    // Open doc
    if(anEvent.equals("OpenInEditorButton") && shape.getSubreportName()!=null) {
        RMDocument doc = shape.getDocument().getSubreport(shape.getSubreportName());
        if(doc!=null) {
            RMEditorPane epane = ClassUtils.newInstance(getEditorPane().getClass()).open(doc);
            epane.setWindowVisible(true);
        }
    }
}


}
