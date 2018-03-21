/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import snap.gfx.Font;
import snap.view.*;

/**
 * This class provides UI editing for the currently selected shapes location and size.
 */
public class ShapePlacement extends RMEditorPane.SupportPane {
    
    // TabView for each inspector
    TabView             _tabView;
    
    // ShapeLocationSize inspector
    ShapeLocationSize   _locationSize = new ShapeLocationSize(getEditorPane());
    
    // ShapeRollScaleSkew inspector
    ShapeRollScaleSkew  _rollScaleSkew = new ShapeRollScaleSkew(getEditorPane());

    // ShapeLayout inspector
    ShapeLayout         _layout = new ShapeLayout(getEditorPane());

/**
 * Creates a new ShapePlacement pane.
 */
public ShapePlacement(RMEditorPane anEP)  { super(anEP); }

/**
 * Create UI panel for this inspector.
 */
protected View createUI()
{
    _tabView = new TabView();
    _tabView.setFont(Font.Arial12.deriveFont(11d));
    _tabView.addTab("Location/Size", _locationSize.getUI());
    _tabView.addTab("Roll/Scale", _rollScaleSkew.getUI());
    _tabView.addTab("Layout", _layout.getUI());
    return _tabView;
}

/**
 * Updates UI controls from current selection.
 */
public void resetUI()
{
    switch(_tabView.getSelIndex()) {
        case 0: _locationSize.resetLater(); break;
        case 1: _rollScaleSkew.resetLater(); break;
        case 2: _layout.resetLater(); break;
    }
}

/**
 * Returns the name to be used in the inspector's window title.
 */
public String getWindowTitle()  { return "Placement Inspector"; }

}