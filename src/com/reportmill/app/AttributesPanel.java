/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import snap.gfx.*;
import snap.util.ArrayUtils;
import snap.view.*;
import snap.viewx.ColorPanel;

/**
 * This class manages the attributes panel which holds the color panel, font panel, formatter panel and keys panel.
 */
public class AttributesPanel extends RMEditorPane.SupportPane {

    // The TabView
    private TabView  _tabView;

    // Inspectors
    private ViewOwner[]  _insprs;

    // The DrawerView
    private DrawerView  _drawer;

    // The DrawerView Effect
    private Effect  _drawerEffect;

    // Constants for tab selection
    public static final String KEYS = "Keys";
    public static final String COLOR = "Color";
    public static final String FONT = "Font";
    public static final String FORMAT = "Format";
    private static final String[] INSPECTOR_NAMES = { KEYS, COLOR, FONT, FORMAT };

    /**
     * Creates new AttributesPanel for EditorPane.
     */
    public AttributesPanel(RMEditorPane anEP)
    {
        super(anEP);
    }

    /**
     * Returns the inspector names.
     */
    public String[] getInspectorNames()  { return INSPECTOR_NAMES; }

    /**
     * Returns the inspectors.
     */
    public ViewOwner[] getInspectors()
    {
        if (_insprs != null) return _insprs;
        return _insprs = createInspectors();
    }

    /**
     * Creates the inspectors array.
     */
    protected ViewOwner[] createInspectors()
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
    public boolean isVisible()
    {
        return isUISet() && getUI().isShowing();
    }

    /**
     * Sets the attributes panel visible.
     */
    public void setVisible(boolean aValue)
    {
        // If requested visible and inspector is not visible, make visible
        if (aValue && !isVisible())
            setVisibleIndex(0);
    }

    /**
     * Returns the index of the currently visible tab (or -1 if attributes panel not visible).
     */
    public int getVisibleIndex()
    {
        return isVisible() ? _tabView.getSelIndex() : -1;
    }

    /**
     * Sets the attributes panel visible, specifying a specific tab by the given index.
     */
    public void setVisibleIndex(int anIndex)
    {
        // Get the UI
        getUI();

        // Set TabView to tab at given index
        _tabView.setSelIndex(anIndex);

        // If drawer is set, show drawer
        if (_drawer != null)
            showDrawer();

        // ResetUI
        resetLater();
    }

    /**
     * Sets the visible name.
     */
    public void setVisibleName(String aName)
    {
        setVisibleName(aName, false);
    }

    /**
     * Sets the visible name, with option to toggle if named panel already open.
     */
    public void setVisibleName(String aName, boolean doToggle)
    {
        String[] names = getInspectorNames();
        int oldVisIndex = getVisibleIndex();
        int newVisIndex = ArrayUtils.indexOf(names, aName);
        if (oldVisIndex != newVisIndex)
            setVisibleIndex(newVisIndex);
        else if (doToggle)
            setVisible(false);
    }

    /**
     * Returns the DrawerView.
     */
    public DrawerView getDrawer()
    {
        // If already set, just return
        if (_drawer != null) return _drawer;

        // Get/configure UI for drawer
        View attrUI = getUI();
        attrUI.setPrefSize(320, 320);
        DrawerView drawer = new DrawerView(attrUI);
        drawer.getDrawerLabel().setText("Attributes Panel");
        drawer.getTabLabel().setText("Keys");
        return _drawer = drawer;
    }

    /**
     * Returns the DrawerView.
     */
    public void showDrawer()
    {
        getDrawer().show();
    }

    /**
     * Hides the Attributes Drawer.
     */
    public void hideDrawer()
    {
        getDrawer().hide();
    }

    /**
     * Returns the UI panel for the attributes panel.
     */
    protected View createUI()
    {
        // Create/configure TabView
        _tabView = new TabView();
        _tabView.setGrowHeight(true);
        _tabView.setFont(Font.Arial12.deriveFont(11d));

        // Get inspectors and tab builder
        String[] names = getInspectorNames();
        ViewOwner[] inspectors = getInspectors();
        Tab.Builder tabBuilder = new Tab.Builder(_tabView.getTabBar());

        // Iterate over inspectors and create/add tabs
        for (int i = 0; i < names.length; i++)
            tabBuilder.title(names[i]).contentOwner(inspectors[i]).add();

        // Return
        return _tabView;
    }

    /**
     * Updates the attributes panel UI (forwards on to inspector at selected tab).
     */
    public void resetUI()
    {
        // Get selected inspector and reset
        Tab selTab = _tabView.getSelItem();
        ViewOwner inspector = selTab != null ? selTab.getContentOwner() : null;
        if (inspector != null)
            inspector.resetLater();
    }

    /**
     * Called when child starts dragging.
     */
    public void childDragStart()
    {
        // If no drawer, bail
        if (_drawer == null) return;

        // Make drawer invisible to mouse, and clear effect to speed up fade out anim
        _drawer.setPickable(false);
        _drawerEffect = _drawer.getEffect();
        _drawer.setEffect(null);

        // Configure drawer anim to fade out
        _drawer.getAnim(300).clear().setOpacity(.05).needsFinish().play();
    }

    /**
     * Called when child finishes dragging.
     */
    public void childDragStop()
    {
        // If no drawer, bail
        if (_drawer == null) return;

        // Make drawer pickage again
        _drawer.setPickable(true);

        // Configure drawer anim to fade drawer back in
        ViewAnim anim = _drawer.getAnim(0).clear();
        anim.getAnim(250).setOpacity(1).setOnFinish(() -> childDragStopDone()).needsFinish().play();
    }

    /**
     * Called when fade-in anim is done.
     */
    private void childDragStopDone()
    {
        _drawer.setEffect(_drawerEffect);
    }

    /**
     * Subclass ColorPanel to change setWindowVisible to show AttributesPanel.ColorPanel instead.
     */
    public class APColorPanel extends ColorPanel {

        /**
         * Overrides color panel behavior to order attributes panel visible instead.
         */
        public void setWindowVisible(boolean aValue)
        {
            setVisibleName(COLOR, true);
        }
    }
}