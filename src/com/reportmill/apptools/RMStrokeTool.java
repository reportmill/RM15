/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.apptools;
import com.reportmill.app.RMEditor;
import com.reportmill.graphics.*;
import com.reportmill.shape.RMShape;
import java.util.*;
import snap.view.*;
import snap.viewx.ColorWell;

/**
 * UI editing for RMStroke.
 */
public class RMStrokeTool extends RMFillTool {

    /**
     * Reset UI controls.
     */
    public void resetUI()
    {
        // Get currently selected shape
        RMShape shape = getEditor().getSelectedOrSuperSelectedShape();
        RMStroke stroke = shape.getStroke();
        if (stroke == null)
            stroke = new RMStroke();

        // Update StrokeColorWell, StrokeWidthText, StrokeWidthThumb, DashArrayText, DashPhaseSpinner
        setViewValue("StrokeColorWell", stroke.getColor());
        setViewValue("StrokeWidthText", stroke.getWidth());
        setViewValue("StrokeWidthThumb", stroke.getWidth());
        setViewValue("DashArrayText", stroke.getDashArrayString());
        setViewValue("DashPhaseSpinner", stroke.getDashPhase());
    }

    /**
     * Respond to UI changes
     */
    public void respondUI(ViewEvent anEvent)
    {
        // Get editor selected shapes
        RMEditor editor = getEditor();
        List<RMShape> shapes = editor.getSelectedOrSuperSelectedShapes();

        // Handle StrokeColorWell - get color and set in selected shapes
        if (anEvent.equals("StrokeColorWell")) {
            ColorWell cwell = getView("StrokeColorWell", ColorWell.class);
            RMColor color = RMColor.get(cwell.getColor());
            for (RMShape s : shapes)
                s.setStrokeColor(color);
        }

        // Handle StrokeWidthText, StrokeWidthThumb
        if (anEvent.equals("StrokeWidthText") || anEvent.equals("StrokeWidthThumb")) {
            float width = anEvent.getFloatValue();
            for (RMShape s : shapes)
                s.setStrokeWidth(width);
        }

        // Handle DashArrayText
        if (anEvent.equals("DashArrayText")) {
            float darray[] = RMStroke.getDashArray(anEvent.getStringValue(), ",");
            for (RMShape shp : shapes) {
                RMStroke stroke = shp.getStroke();
                if (stroke == null) stroke = new RMStroke();
                shp.setStroke(stroke.deriveDashArray(darray));
            }
        }

        // Handle DashPhaseSpinner
        if (anEvent.equals("DashPhaseSpinner")) {
            float dphase = anEvent.getFloatValue();
            for (RMShape shp : shapes) {
                RMStroke stroke = shp.getStroke();
                if (stroke == null) stroke = new RMStroke();
                shp.setStroke(stroke.deriveDashPhase(dphase));
            }
        }
    }
}