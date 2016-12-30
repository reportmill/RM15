/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import snap.view.*;

/**
 * This class provides UI editing for the currently selected shapes layout attributes (in parent).
 */
public class ShapeLayout extends RMEditorPane.SupportPane {
    
    // The SpringsLayoutPane
    SpringsLayoutPane        _slp = new SpringsLayoutPane(getEditorPane());

/**
 * Creates a new ShapeLayout pane.
 */
public ShapeLayout(RMEditorPane anEP)  { super(anEP); }

/**
 * Create UI.
 */
protected View createUI()
{
    SpringView spane = new SpringView(); spane.setSize(250,275);
    BorderView bpane = new BorderView(); bpane.setSize(250,275); bpane.setName("BorderView");
    spane.addChild(bpane);
    return spane;
}

/**
 * Updates UI controls from currently selected shape.
 */
public void resetUI()
{
    // Get currently selected shape and it's parent
    //RMShape shape = RMEditor.getMainEditor().getSelectedOrSuperSelectedShape(); if(shape==null) return;
    //RMParentShape parent = shape.getParent();
    
    // Set LayoutUI
    getView("BorderView", BorderView.class).setCenter(_slp.getUI());
    _slp.resetLater();
}

/** Returns the name to be used in the inspector's window title. */
public String getWindowTitle()  { return "Shape Layout Inspector"; }

}