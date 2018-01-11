/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.apptools;
import com.reportmill.app.*;
import com.reportmill.graphics.*;
import com.reportmill.shape.*;
import java.util.*;
import snap.gfx.Image;
import snap.util.*;
import snap.view.*;
import snap.viewx.ColorWell;

/**
 * Provides a tool for editing RMFills.
 */
public class RMFillTool extends RMEditorPane.SupportPane {

    // Map of tool instances by shape class
    Map                 _tools = new Hashtable();
    
    // List of known strokes
    static RMStroke     _strokes[] = { new RMStroke(), new RMBorderStroke() };
    
    // List of known fills
    static RMImageFill  _imageFill = new RMImageFill(Image.get(RMFillTool.class, "pkg.images/Clouds.jpg"), true);
    static RMFill       _fills[] = { new RMFill(), new RMGradientFill(), _imageFill };

/**
 * Creates a new RMFillTool panel.
 */
public RMFillTool()  { super(null); }

/**
 * Called to reset UI controls.
 */
protected void resetUI()
{
    // Get currently selected shape
    RMShape shape = getEditor().getSelectedOrSuperSelectedShape();
    
    // Update FillColorWell
    setViewValue("FillColorWell", shape.getColor());    
}

/**
 * Called to respond to UI controls
 */
protected void respondUI(ViewEvent anEvent)
{
    // Get the current editor and currently selected shape (just return if null)
    RMEditor editor = getEditor(); if(editor==null) return;
    RMShape shape = editor.getSelectedOrSuperSelectedShape(); if(shape==null) return;
    
    // Handle FillColorWell
    if(anEvent.equals("FillColorWell")) {
        
        // Get RMColor from color well
        ColorWell cwell = getView("FillColorWell", ColorWell.class);
        RMColor color = RMColor.get(cwell.getColor());
        
        // Iterate over selected shapes and set color
        for(RMShape s : editor.getSelectedOrSuperSelectedShapes()) {
            
            // If command-click, set gradient fill
            if(ViewUtils.isMetaDown()) {
                RMColor c1 = shape.getFill()!=null? shape.getColor() : RMColor.clearWhite;
                RMFill f = new RMGradientFill(c1, color, 0);
                s.setFill(f);
            }

            // If not command-click, just set color
            else s.setColor(color);
        }
    }
}

/**
 * Returns the number of known strokes.
 */
public int getStrokeCount()  { return _strokes.length; }

/**
 * Returns an individual stroke at given index.
 */
public RMStroke getStroke(int anIndex)  { return _strokes[anIndex]; }

/**
 * Returns the number of known fills.
 */
public int getFillCount()  { return _fills.length; }

/**
 * Returns an individual fill at given index.
 */
public RMFill getFill(int anIndex)  { return _fills[anIndex]; }

/**
 * Returns the currently selected shape's stroke.
 */
public RMStroke getSelectedStroke()
{
    RMShape shape = getEditor().getSelectedOrSuperSelectedShape();
    return shape.getStroke();
}

/**
 * Iterate over editor selected shapes and set stroke.
 */
public void setSelectedStroke(RMStroke aStroke)
{
    RMEditor editor = getEditor();
    for(int i=0, iMax=editor.getSelectedOrSuperSelectedShapeCount(); i<iMax; i++) {
        RMShape shape = editor.getSelectedOrSuperSelectedShape(i);
        shape.setStroke(i==0? aStroke : aStroke.clone());
    }
}

/**
 * Returns the currently selected shape's fill.
 */
public RMFill getSelectedFill()
{
    RMShape shape = getEditor().getSelectedOrSuperSelectedShape();
    return shape.getFill();
}

/**
 * Iterate over editor selected shapes and set fill.
 */
public void setSelectedFill(RMFill aFill)
{
    RMEditor editor = getEditor();
    for(int i=0, iMax=editor.getSelectedOrSuperSelectedShapeCount(); i<iMax; i++) {
        RMShape shape = editor.getSelectedOrSuperSelectedShape(i);
        shape.setFill(i==0? aFill : aFill.clone());
    }
}

/**
 * Returns the specific tool for a given fill.
 */
public RMFillTool getTool(Object anObj)
{
    // Get tool from tools map - just return if present
    Class cls = anObj instanceof Class? (Class)anObj : anObj.getClass();
    RMFillTool tool = (RMFillTool)_tools.get(cls);
    if(tool==null) {
        _tools.put(cls, tool=getToolImpl(cls));
        tool.setEditorPane(getEditorPane());
    }
    return tool;
}

/**
 * Returns the specific tool for a given fill.
 */
static RMFillTool getToolImpl(Class aClass)
{
    // Get class name
    String cname = aClass.getSimpleName();
    
    // Declare variable for tool class
    Class tclass = null;
    
    // If shape class starts with RM, check panels package for built-in fill tools
    if(cname.startsWith("RM"))
        tclass = ClassUtils.getClass("com.reportmill.apptools." + cname + "Tool");
    
    // If not found, try looking in same package for shape class plus "Tool"
    if(tclass==null)
        tclass = ClassUtils.getClass(aClass.getName() + "Tool", aClass);
    
    // If not found and class ends in "Fill", try looking in same package for class that ends with "Tool" instead
    if(tclass==null && cname.endsWith("Fill"))
        tclass = ClassUtils.getClass(StringUtils.replace(aClass.getName(), "Fill", "Tool"), aClass);
    
    // If not found, try looking for inner class named "Tool"
    if(tclass==null)
        tclass = ClassUtils.getClass(aClass.getName() + "$" + "Tool", aClass);
    
    // If tool class found, instantiate tool class
    if(tclass!=null)
        try { return (RMFillTool)tclass.newInstance(); }
        catch(Exception ie) { ie.printStackTrace(); }
        
    // Otherwise, get tool for super class
    return getToolImpl(aClass.getSuperclass());
}

}