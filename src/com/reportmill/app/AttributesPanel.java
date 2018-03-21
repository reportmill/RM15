/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import com.reportmill.graphics.*;
import snap.gfx.*;
import snap.view.*;
import snap.viewx.ColorPanel;

/**
 * This class manages the attributes panel which holds the color panel, font panel, formatter panel and keys panel.
 */
public class AttributesPanel extends RMEditorPane.SupportPane {
    
    // The TabView
    TabView         _tabView;
    
    // Inspector names
    String          _inspNames[];
    
    // Inspectors
    ViewOwner       _insprs[];

    // Constants for tab selection
    public static final String KEYS = "Keys";
    public static final String COLOR = "Color";
    public static final String FONT = "Font";
    public static final String FORMAT = "Format";

/**
 * Creates new AttributesPanel for EditorPane.
 */
public AttributesPanel(RMEditorPane anEP)  { super(anEP); }

/**
 * Returns the inspector names.
 */
public String[] getInspectorNames()  { return _inspNames!=null? _inspNames : (_inspNames=createInspectorNames()); }

/**
 * Creates the inspector names array.
 */
public String[] createInspectorNames()  { return new String[] { KEYS, COLOR, FONT, FORMAT }; }

/**
 * Returns the inspectors.
 */
public ViewOwner[] getInspectors()  { return _insprs!=null? _insprs : (_insprs=createInspectors()); }

/**
 * Creates the inspectors array.
 */
public ViewOwner[] createInspectors()
{
    KeysPanel keys = new KeysPanel(getEditorPane());
    APColorPanel color = new APColorPanel();
    FontPanel font = new FontPanel(getEditorPane());
    FormatPanel format = new FormatPanel(getEditorPane());
    return new ViewOwner[] { keys, color, font, format };
}

/**
 * Returns whether the attributes panel is visible.
 */
public boolean isVisible()  { return isUISet() && getUI().isShowing(); }

/**
 * Sets the attributes panel visible.
 */
public void setVisible(boolean aValue)
{
    // If requested visible and inspector is not visible, make visible
    if(aValue && !isVisible())
        setVisible(0);
    
    // If setting not visible, propagate on to window
    //if(!aValue && isVisible()) setWindowVisible(false);
}

/**
 * Returns the index of the currently visible tab (or -1 if attributes panel not visible).
 */
public int getVisible()  { return isVisible()? _tabView.getSelIndex() : -1; }

/**
 * Sets the attributes panel visible, specifying a specific tab by the given index.
 */
public void setVisible(int anIndex)
{
    // Get the UI
    getUI();
    
    // Set TabView to tab at given index
    _tabView.setSelIndex(anIndex);
    
    // ResetUI
    resetLater();

    // If window isn't visible, set window visible
    //if(!isVisible()) { RMEditorPane epane = RMEditorPane.getMain(); if(epane==null) return;
    //    WindowView win = getWindow(); View eview = epane.getUI();
    //    Point pnt = win.getScreenLocation(eview, Pos.TOP_RIGHT, win.getWidth() + 20, 0);
    //    win.show(eview, pnt.x, pnt.y); }
}

/**
 * Returns the visible name.
 */
public String getVisibleName(String aName)  { return getInspectorNames()[getVisible()]; }

/**
 * Sets the visible name.
 */
public void setVisibleName(String aName)  { setVisibleName(aName, false); }

/**
 * Sets the visible name, with option to toggle if named panel already open.
 */
public void setVisibleName(String aName, boolean doToggle)
{
    String names[] = getInspectorNames();
    int vis = getVisible(), vis2 = -1; for(int i=0;i<names.length;i++) if(aName.equals(names[i])) vis2 = i;
    if(vis!=vis2)
        setVisible(vis2);
    else if(doToggle)
        setVisible(false);
}

/**
 * Returns the UI panel for the attributes panel.
 */
protected View createUI()
{
    _tabView = new TabView();
    _tabView.setFont(Font.Arial12.deriveFont(11d));
    String names[] = getInspectorNames(); ViewOwner inspectors[] = getInspectors();
    for(int i=0;i<names.length;i++)
        _tabView.addTab(names[i], new Label());
    _tabView.setPrefSize(275, 290);
    return _tabView;
}

/** Initializes the UI panel. */
//protected void initUI()  { getWindow().setAlwaysOnTop(true); getWindow().setHideOnDeactivate(true);
//    getWindow().setType(WindowView.TYPE_UTILITY); getWindow().setSaveName("AttributesPanel"); }

/**
 * Updates the attributes panel UI (forwards on to inspector at selected tab).
 */
public void resetUI()
{
    // Get inspector component from TabView
    ViewOwner inspector = getInspectors()[_tabView.getSelIndex()];

    // If inspector panel is JLabel, swap in real inspector UI
    if(_tabView.getContent() instanceof Label)
        _tabView.setTabContent(inspector.getUI(), _tabView.getSelIndex());
    
    // Set window title and reset inspector
    //getWindow().setTitle(Key.getStringValue(inspector, "getWindowTitle"));
    inspector.resetLater();
}

/**
 * This inner class is a ColorPanel suitable for manipulating colors in current RMEditor.
 */
public class APColorPanel extends ColorPanel {
    
    /** Overrides color panel behavior to order attributes panel visible instead. */
    public void setWindowVisible(boolean aValue)  { setVisibleName(COLOR, true); }

    /** Overrides normal implementation to get color from editor if no color well selected. */
    public Color getColor()
    {
        // If color panel has color well, just return normal
        if(getColorWell()!=null)
            return super.getColor();
        
        // Get color from editor
        return RMEditorShapes.getSelectedColor(getEditor());
    }

    /** Override to forward to editor. */
    public void setColor(Color aColor)
    {
        super.setColor(aColor);
        if(getColorWell()==null)
            RMEditorShapes.setSelectedColor(getEditor(), RMColor.get(aColor));
    }
    
    /** Returns the name for this panel. */
    public String getWindowTitle() { return "Color Panel"; }
}

}