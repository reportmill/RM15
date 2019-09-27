/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.apptools;
import com.reportmill.app.AttributesPanel;
import com.reportmill.app.RMEditor;
import com.reportmill.base.RMGrouping;
import com.reportmill.graphics.RMColor;
import com.reportmill.graphics.RMStroke;
import com.reportmill.shape.*;
import java.util.*;
import snap.gfx.Point;
import snap.util.*;
import snap.view.*;
import snap.viewx.ColorDock;

/**
 * This class provides RMEditor inspector support for RMGraph.
 */
public class RMGraphTool <T extends RMGraph> extends RMTool <T> implements RMSortPanel.Owner {
    
    // The sort panel
    RMSortPanel             _sortPanel;

    // Assistant tool for Bars
    RMGraphPartBarsTool     _barTool = new RMGraphPartBarsTool();
    
    // Assistant tool for Pie
    RMGraphPartPieTool      _pieTool = new RMGraphPartPieTool();
    
    // Assistant tool for 3D
    RMScene3DTool           _3dTool = new Scene3DTool();

/**
 * Override to forward to proxy tools.
 */
public void setEditor(RMEditor anEditor)
{
    super.setEditor(anEditor);
    _barTool.setEditor(anEditor);
    _pieTool.setEditor(anEditor);
    _3dTool.setEditor(anEditor);
}

/**
 * Initializes UI panel.
 */
protected void initUI()
{
    // Get SortPanel and configure (select TopN button, set UI bounds)
    _sortPanel = new RMSortPanel(this);
    _sortPanel.getUI().setBounds(4, 182, 248, 100);
    _sortPanel.setSelectedPane(1);
    
    // Add SortPanel to main tab content
    //BoxView sortBox = getView("SortPanelBox", BoxView.class);
    TitleView sortBox = getView("SortPanelBox", TitleView.class);
    _sortPanel.getView("SortingLabel").setVisible(false);
    sortBox.setContent(_sortPanel.getUI());
    
    // Add 3D UI
    TitleView thr3dBox = getView("3DBox", TitleView.class);
    thr3dBox.setContent(_3dTool.getUI());
    
    // Set values in SectionLayoutList and ItemsLayoutList
    setViewItems("SectionLayoutList", RMGraph.SectionLayout.values());
    setViewItems("ItemsLayoutList", RMGraph.ItemLayout.values());
    
    // Enable drop keys
    enableEvents("ListKeyText", DragDrop);
    enableEvents("FilterText", DragDrop);
    enableEvents("KeysText", DragDrop);
}

/**
 * Reset UI panel from currently selected graph.
 */
protected void resetUI()
{
    // Get currently selected graph (just return if null)
    RMGraph graph = getSelectedGraph(); if(graph==null) return;
    
    // Ensure Bar/Pie specific tab is installed
    boolean isPie = graph.getType()==RMGraph.Type.Pie;
    TitleView typeBox = getView("TypeBox", TitleView.class);
    View typeUI = isPie? _pieTool.getUI() : _barTool.getUI();
    if(typeBox.getContent()!=typeUI) {
        typeBox.setText(isPie? "Pie" : "Bar");
        typeBox.setContent(typeUI);
    }
    
    // Update ListKeyText, FilterText
    setViewValue("ListKeyText", graph.getDatasetKey());
    setViewValue("FilterText", graph.getFilterKey());
    
    // Update KeysText
    StringBuffer sb = new StringBuffer();
    for(int i=0; i<graph.getKeyCount();i++) { if(i>0) sb.append(", "); sb.append(graph.getKey(i)); }
    setViewValue("KeysText", sb.toString());
    
    // Set ValuesButton visible/invisible 
    _sortPanel.getView("ValuesButton").setVisible(graph.getGrouping().getKey()!=null);

    // Update BarVGraphButton, BarHGraphButton, PieGraphButton
    setViewValue("GraphTypeBarButton", graph.getType()==RMGraph.Type.Bar);
    setViewValue("GraphTypeBarHButton", graph.getType()==RMGraph.Type.BarH);
    setViewValue("GraphTypeAreaButton", graph.getType()==RMGraph.Type.Area);
    setViewValue("GraphTypeLineButton", graph.getType()==RMGraph.Type.Line);
    setViewValue("GraphTypeScatterButton", graph.getType()==RMGraph.Type.Scatter);
    setViewValue("GraphTypePieButton", graph.getType()==RMGraph.Type.Pie);
    
    // Reset SectionLayoutList and ItemsLayoutList
    setViewValue("SectionLayoutList", graph.getSectionLayout());
    setViewValue("ItemsLayoutList", graph.getItemsLayout());

    // Get the color dock, reset colors and update
    ColorDock colorDock = getView("ColorDock", ColorDock.class);
    List <RMColor> colors = graph.getColors();
    colorDock.resetColors();
    for(int i=0, iMax=Math.min(colors.size(), colorDock.getSwatchCount()); i<iMax; i++)
        colorDock.setColor(colors.get(i), i); 
    
    // Update ColorItemsCheckBox, ShowLegendCheckBox, Draw3DCheckBox
    setViewValue("ColorItemsCheckBox", graph.isColorItems());
    setViewValue("ShowLegendCheckBox", graph.isShowLegend());
    setViewValue("Draw3DCheckBox", graph.getDraw3D());
    getView("3DBox").setVisible(graph.getDraw3D());
    
    // Update SortPanel, 
    _sortPanel.resetLater();
    if(_barTool.isUISet() && _barTool.getUI().isShowing())
        _barTool.resetLater();
    else if(_pieTool.isUISet() && _pieTool.getUI().isShowing())
        _pieTool.resetLater();
    else if(_3dTool.isUISet() && _3dTool.getUI().isShowing())
        _3dTool.resetLater();
}

/**
 * Update currently selected graph from UI panel.
 */
public void respondUI(ViewEvent anEvent)
{
    // Get currently selected graph (just return if null)
    RMGraph graph = getSelectedGraph(); if(graph==null) return;
    
    // Register graph for repaint/revalidate
    graph.repaint();

    // Handle ListKeyText
    if(anEvent.equals("ListKeyText")) {
        graph.setDatasetKey(StringUtils.delete(anEvent.getStringValue(), "@"));
        if(anEvent.isDragDrop())
            anEvent.dropComplete();
    }
    
    // Handle FilterText
    if(anEvent.equals("FilterText"))
        graph.setFilterKey(StringUtils.delete(anEvent.getStringValue(), "@"));
        
    // Handle KeysMenuItem
    if(anEvent.equals("KeysMenuItem"))
        getEditorPane().getAttributesPanel().setVisibleName(AttributesPanel.KEYS);
    
    // Handle KeysText, Key2Text, Key3Text
    if(anEvent.equals("KeysText")) {
        
        // Get Key string and key strings
        String keysString = getViewStringValue("KeysText");
        if(anEvent.isDragDrop()) {
            if(keysString==null || keysString.length()==0) keysString = anEvent.getStringValue();
            else keysString += ',' + anEvent.getStringValue();
            anEvent.dropComplete();
        }
        String keyStrings[] = keysString.split(",");

        // Clear keys and (re)add
        graph.clearKeys();
        for(String ks : keyStrings) {
            String key = ks.trim().replace("@", "");
            if(key!=null && key.length()>0)
                graph.addKey(key);
        }
    }
    
    // Handle GraphTypeBarButton, GraphTypeBarHButton, GraphTypeAreaButton, GraphTypeLineButton, GraphTypeScatterButton
    if(anEvent.equals("GraphTypeBarButton")) graph.setType(RMGraph.Type.Bar);
    if(anEvent.equals("GraphTypeBarHButton")) graph.setType(RMGraph.Type.BarH);
    if(anEvent.equals("GraphTypeAreaButton")) graph.setType(RMGraph.Type.Area);
    if(anEvent.equals("GraphTypeLineButton")) graph.setType(RMGraph.Type.Line);
    if(anEvent.equals("GraphTypeScatterButton")) graph.setType(RMGraph.Type.Scatter);
    if(anEvent.equals("GraphTypePieButton")) graph.setType(RMGraph.Type.Pie);
    
    // Handle SectionLayoutList, ItemsLayoutList
    if(anEvent.equals("SectionLayoutList")) graph.setSectionLayout((RMGraph.SectionLayout)anEvent.getValue());
    if(anEvent.equals("ItemsLayoutList")) graph.setItemsLayout((RMGraph.ItemLayout)anEvent.getValue());
    
    // Handle ColorDock
    if(anEvent.equals("ColorDock")) {
        ColorDock cdock = getView("ColorDock", ColorDock.class);
        int index = cdock.getSelIndex(); // Get the selected index
        List <RMColor> colors = new ArrayList(graph.getColors()); // Get copy of graph colors
        while(colors.size()<index+1) colors.add(RMColor.white); // Make sure they are at least as long as selected index
        colors.set(index, RMColor.get(cdock.getColor(index))); // Set color of selected index, remove trailing whites
        while(colors.size()>1 && colors.get(colors.size()-1).equals(RMColor.white)) colors.remove(colors.size()-1);
        graph.setColors(colors); // Set new colors in graph
    }

    // Handle ColorItemsCheckBox, ShowLegendCheckBox, Draw3DCheckBox
    if(anEvent.equals("ColorItemsCheckBox")) graph.setColorItems(anEvent.getBoolValue());
    if(anEvent.equals("ShowLegendCheckBox")) graph.setShowLegend(anEvent.getBoolValue());
    if(anEvent.equals("Draw3DCheckBox")) graph.setDraw3D(anEvent.getBoolValue());
}

/**
 * Returns the selected graph.
 */
public RMGraph getSelectedGraph()  { return ClassUtils.getInstance(getSelectedShape(), RMGraph.class); }

/**
 * Returns the graph's grouping.
 */
public RMGrouping getGrouping()
{
    RMGraph graph = getSelectedGraph();
    return graph!=null? graph.getGrouping() : null;
}

/**
 * Returns the class this tool edits.
 */
public Class getShapeClass()  { return RMGraph.class; }

/**
 * Returns the name of the graph inspector.
 */
public String getWindowTitle()  { return "Graph Inspector"; }

/**
 * Overridden to make graph super-selectable.
 */
public boolean isSuperSelectable(RMShape aShape)  { return true; }

/**
 * Overridden to make graph accept children.
 */
public boolean getAcceptsChildren(RMShape aShape)  { return true; }

/**
 * Overridden to make graph not ungroupable.
 */
public boolean isUngroupable(RMShape aShape)  { return false; }

/**
 * Adds a new graph instance to the given editor with the given dataset key.
 */
public static void addGraph(RMEditor anEditor, String aKeyPath)
{
    // Get graph instance and set it's name and datasource
    RMGraph graph = createSampleGraph();
    graph.setName(aKeyPath);
    graph.setDatasetKey(aKeyPath);
    
    // Get graph parent and set location in middle of parent
    RMParentShape parent = anEditor.firstSuperSelectedShapeThatAcceptsChildren();
    graph.setXY(parent.getWidth()/2 - graph.getWidth()/2, parent.getHeight()/2 - graph.getHeight()/2);

    // Add graph
    anEditor.undoerSetUndoTitle("Add Bar Graph");
    parent.addChild(graph);

    // Select graph, select selectTool and redisplay
    anEditor.setCurrentToolToSelectTool();
    anEditor.setSelectedShape(graph);
}

/**
 * Creates a new sample graph instance.
 */
private static RMGraph createSampleGraph()
{
    // Create new 320x240(*90%) graph
    RMGraph graph = new RMGraph(); graph.setSize(288, 216);
    graph.setStroke(new RMStroke()); //setFill(newRMGradientFill(newRMColor(.85f,.88f,1),newRMColor(.58f,.65f,1),90));
    graph.setColor(RMColor.white);
    return graph;
}

/**
 * Handle MousePressed.
 */
public void mousePressed(T aGraph, ViewEvent anEvent)
{
    // See if point hit Graph part
    Point point = getEditor().convertToShape(anEvent.getX(), anEvent.getY(), aGraph);
    for(int i=0, iMax=aGraph.getPartCount(); i<iMax; i++)
        if(aGraph.getFontBox(i).contains(point.getX(), point.getY()))
            _hitChild = aGraph.getParts()[i];
    
    // See if 3D is available
    if(_hitChild==null && isSuperSelected(aGraph) && aGraph.getDraw3D()) {
        _hitChild = aGraph.get3D();
        _hitChild.processEvent(createShapeEvent(_hitChild, anEvent));
        getEditor().setSuperSelectedShape(aGraph);
    }
    
    if(_hitChild!=null)
        anEvent.consume();
}
RMShape _hitChild;

/**
 * Event handler for editing.
 */
public void mouseDragged(T aGraphArea, ViewEvent anEvent)
{
    // If child is scene3d, forward mouse event to child and consume event
    if(_hitChild instanceof RMScene3D)
        _hitChild.processEvent(createShapeEvent(_hitChild, anEvent));
    if(_hitChild!=null) anEvent.consume();
}

/**
 * Handle MouseReleased.
 */
public void mouseReleased(T aGraph, ViewEvent anEvent)
{
    if(_hitChild instanceof RMScene3D)
        _hitChild.processEvent(createShapeEvent(_hitChild, anEvent));
    else if(_hitChild!=null)
        getEditor().setSelectedShape(_hitChild);
    if(_hitChild!=null) anEvent.consume();
    _hitChild = null;
}

/**
 * The Scene3DTool.
 */
protected class Scene3DTool extends RMScene3DTool {

    /** Override to get RMScene3D from GraphArea. */
    public RMShape getSelectedShape()
    {
        RMGraph graph = RMGraphTool.this.getSelectedShape();
        return graph!=null? graph.get3D() : null;
    }
}

}